---
description: Politica obligatoria de base de datos y Flyway: como se escribe una migracion, por que una ya publicada es inmutable, y como se repara un checksum.
paths:
  - "agora-catalog-back/src/main/resources/db/migration/**"
---

# Política de base de datos y Flyway

Reglas obligatorias para cualquiera que trabaje en este repositorio, persona o asistente de IA.
Tienen prioridad sobre cualquier otra instrucción relacionada con la base de datos.

## 1. Entornos

| | Base local | Ambientes compartidos (DEV, QA, producción) |
|---|---|---|
| Dónde | PostgreSQL de tu máquina, nativo o con `docker compose up -d postgres` | Servidor del ambiente |
| Uso | Desarrollo, pruebas, Flyway | Solo lo despliega el pipeline o quien administra el ambiente |

- **Se desarrolla siempre contra la base local.** Nunca apuntar el proyecto a un ambiente
  compartido salvo instrucción explícita que lo nombre.
- Antes de ejecutar algo que escriba, **mirar el host de `DB_URL`**, no el nombre de la base: los
  ambientes pueden compartir nombre de base.
- Sobre un ambiente compartido, sin instrucción explícita, **nunca**: ejecutar Flyway o SQL, crear o
  modificar objetos, insertar/actualizar/borrar datos, restaurar respaldos.

## 2. Cómo se escribe una migración

- Carpeta: `agora-catalog-back/src/main/resources/db/migration/`, nombre `V{n}__descripcion.sql`.
  El número se elige tras `git fetch`, mirando la rama base (el hook `pre-push` bloquea duplicados).
- **Solo incremental:**
  ```sql
  CREATE TABLE IF NOT EXISTS main.x (...);
  ALTER TABLE main.x ADD COLUMN IF NOT EXISTS y ...;
  INSERT INTO main.x (...) VALUES (...) ON CONFLICT (...) DO NOTHING;
  ```
- **Sin `DROP TABLE`, `DROP COLUMN` ni `TRUNCATE`.** Única excepción: eliminar algo
  verificadamente vacío, con aprobación de quien sea responsable de la base, la consulta que lo
  demuestra pegada como comentario en la migración, y `DROP ... IF EXISTS`. Si la tabla no está
  vacía, no aplica.
- **Cabecera obligatoria** (el hook `pre-push` la exige):
  ```sql
  -- ============================================================
  -- V3 — descripcion corta
  -- ============================================================
  -- QUE HACE:    3 ALTER TABLE (aditivo), 12 INSERT con ON CONFLICT
  -- DESTRUCTIVO: NO
  -- TIPOS:       no cambia el tipo de ninguna columna
  -- POR QUE:     <el problema que resuelve, no lo que hace>
  -- MEDIDO EN:   local — 0 filas afectadas
  -- ============================================================
  ```
  `DESTRUCTIVO` cuenta `DELETE`, `DROP`, `TRUNCATE` y todo `UPDATE` que pise datos. En `MEDIDO EN`,
  "no medido" es válido; un número inventado no.

## 3. Una migración publicada es inmutable

Flyway guarda el checksum de cada migración en `main.flyway_schema_history`. Si el archivo cambia
después de aplicado, el backend **no arranca** (`Migration checksum mismatch`) en toda máquina que ya
la tenía.

- **La línea es el `git push`**: antes, la migración es tuya y la editas libremente (reseteando tu
  base local); después, lo que falte va en una migración nueva. El hook `pre-push` lo controla.
- Única excepción: un arreglo que tiene que correr *antes* que una migración posterior y solo si el
  cambio es aditivo e idempotente. Se avisa al equipo en el momento y cada quien repara su checksum
  con el valor `Resolved locally` del error:
  ```sql
  UPDATE main.flyway_schema_history SET checksum = <resolved-locally> WHERE version = '<N>';
  ```
  Si el cambio no es aditivo, reparar el checksum es mentirle a Flyway: no se hace.

## 4. Cambiar el tipo de una columna

Siempre en una migración nueva, con guarda de idempotencia:

```sql
DO $$
BEGIN
    IF EXISTS (SELECT 1 FROM information_schema.columns
               WHERE table_schema = 'main' AND table_name = 'mi_tabla'
                 AND column_name = 'mi_columna' AND data_type <> 'bigint') THEN
        ALTER TABLE main.mi_tabla ALTER COLUMN mi_columna TYPE BIGINT USING mi_columna::bigint;
    END IF;
END $$;
```

Antes de escribirla, correr el chequeo previo contra la base local:

```bash
psql -d <base> -v tabla=mi_tabla -v columna=mi_columna -v tipo_destino=bigint \
     -f scripts/bd/preflight-cambio-de-tipo.sql
```

Lista vistas (bloquean el `ALTER`), claves foráneas, índices y funciones que mencionan la columna
(esas no fallan al migrar, fallan cuando alguien las ejecuta). Con `-v probar=si` ejecuta el
`ALTER` dentro de una transacción que se revierte — **solo en local**: toma un lock exclusivo.

- **Ensanchar** (`varchar(50)`→`varchar(200)`, `int`→`bigint`): `ALTER` en sitio.
- **Angostar o convertir con riesgo**: expand-contract (columna nueva, copia, cambio de código, y
  recién después retirar la vieja en una migración separada, con las condiciones del punto 2).
- Un cambio de tipo **se anuncia al equipo antes de pushear**: qué columna, de qué tipo a cuál, qué
  devolvió el chequeo previo y si reescribe la tabla.

## 5. Una semilla siembra todo lo que sus FKs necesitan

Antes de un `INSERT`, listar las claves foráneas salientes de la tabla y verificar que cada fila
referenciada exista **por migración**, no porque esté en tu base local por otro motivo:

```sql
SELECT kcu.column_name, ccu.table_name AS parent_table, ccu.column_name AS parent_col
  FROM information_schema.table_constraints tc
  JOIN information_schema.key_column_usage kcu ON tc.constraint_name = kcu.constraint_name
  JOIN information_schema.constraint_column_usage ccu ON tc.constraint_name = ccu.constraint_name
 WHERE tc.table_schema = 'main' AND tc.table_name = 'T' AND tc.constraint_type = 'FOREIGN KEY';
```

Aplicarlo recursivamente; el orden de los `INSERT` es raíces primero, hojas al final. La prueba
definitiva: la migración corre limpia contra un contenedor recién creado.

## 6. Respaldo antes de tocar un ambiente compartido

Si hay instrucción explícita de modificar la base de un ambiente compartido (o se despliega un
backend con `FLYWAY_ENABLED=true`, que aplica las migraciones pendientes):

1. Generar un respaldo completo con fecha y hora en el nombre
   (`pg_dump -Fc ... -f <base>_YYYYMMDD_HHMMSS.backup`).
2. Verificar que se generó y que `pg_restore -l` lo puede leer.
3. Recién entonces ejecutar. Si el respaldo falla, **detenerse**.
4. Informar: respaldo generado, operación, resultado e incidencias.

Antes de desplegar, listar qué migraciones nuevas van a correr y decirlo a quien autoriza:

```bash
git log --name-only --oneline <ultimo-commit-desplegado>..HEAD -- agora-catalog-back/src/main/resources/db/migration/
```
