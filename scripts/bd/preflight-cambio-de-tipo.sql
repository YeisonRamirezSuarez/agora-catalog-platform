-- =====================================================================
-- Chequeo previo a un cambio de tipo de columna
-- =====================================================================
-- Responde, ANTES de escribir el ALTER, las cuatro preguntas que si no se
-- contestan terminan en una migracion que falla a mitad o en una funcion que
-- se rompe en silencio:
--
--   1. Que tipo tiene hoy la columna
--   2. Que vistas dependen de ella  -> bloquean el ALTER
--   3. Que claves foraneas e indices la tocan  -> se revalidan o reconstruyen
--   4. Que funciones la mencionan  -> NO fallan al migrar, fallan al ejecutarse
--
-- El punto 4 es el que muerde. Una funcion con el nombre de la columna escrito
-- adentro sigue existiendo despues del ALTER y nadie se entera hasta que
-- alguien la llama.
--
-- USO:
--   psql -d <base> -v tabla=mi_tabla -v columna=mi_columna \
--        -v tipo_destino=bigint -f scripts/bd/preflight-cambio-de-tipo.sql
--
-- La PARTE A es de solo lectura y se puede correr contra cualquier base,
-- incluida la de un ambiente compartido. La PARTE B modifica y esta apagada por
-- defecto: usarla solo en local. Ver .claude/rules/migraciones.md §4.
-- =====================================================================

\set ON_ERROR_STOP on
\pset pager off

\echo ''
\echo '=== Columna bajo analisis ==='

SELECT table_name  AS tabla,
       column_name AS columna,
       data_type   AS tipo_actual,
       CASE WHEN is_nullable = 'YES' THEN 'nulable' ELSE 'NOT NULL' END AS nulabilidad,
       COALESCE(column_default, '(sin default)') AS valor_por_defecto
  FROM information_schema.columns
 WHERE table_schema = 'main'
   AND table_name   = :'tabla'
   AND column_name  = :'columna';

\echo ''
\echo '=== 1. Vistas que dependen de la columna (BLOQUEAN el ALTER) ==='
\echo '    Si aparece alguna, hay que bajarla y recrearla en la misma migracion.'

SELECT DISTINCT dependiente.relname AS vista,
       CASE dependiente.relkind WHEN 'v' THEN 'vista' WHEN 'm' THEN 'vista materializada' END AS tipo
  FROM pg_depend d
  JOIN pg_rewrite r        ON r.oid = d.objid
  JOIN pg_class dependiente ON dependiente.oid = r.ev_class
  JOIN pg_class origen      ON origen.oid = d.refobjid
  JOIN pg_namespace n       ON n.oid = origen.relnamespace
  JOIN pg_attribute a       ON a.attrelid = origen.oid AND a.attnum = d.refobjsubid
 WHERE n.nspname   = 'main'
   AND origen.relname = :'tabla'
   AND a.attname   = :'columna'
   AND dependiente.relkind IN ('v', 'm');

\echo ''
\echo '=== 2. Claves foraneas que involucran la columna (se revalidan) ==='

SELECT c.conname AS restriccion,
       c.conrelid::regclass::text  AS tabla_origen,
       c.confrelid::regclass::text AS tabla_referenciada
  FROM pg_constraint c
 WHERE c.contype = 'f'
   AND (
        (c.conrelid = ('main.' || :'tabla')::regclass
         AND :'columna' = ANY (SELECT a.attname FROM pg_attribute a
                                WHERE a.attrelid = c.conrelid AND a.attnum = ANY (c.conkey)))
     OR (c.confrelid = ('main.' || :'tabla')::regclass
         AND :'columna' = ANY (SELECT a.attname FROM pg_attribute a
                                WHERE a.attrelid = c.confrelid AND a.attnum = ANY (c.confkey)))
   );

\echo ''
\echo '=== 3. Indices sobre la columna (se reconstruyen solos) ==='

SELECT indexname AS indice, indexdef AS definicion
  FROM pg_indexes
 WHERE schemaname = 'main'
   AND tablename  = :'tabla'
   AND indexdef ILIKE '%' || :'columna' || '%';

\echo ''
\echo '=== 4. Funciones y procedimientos que mencionan la columna ==='
\echo '    OJO: estos NO fallan al migrar. Fallan cuando alguien los ejecuta.'
\echo '    Revisar cada uno a mano: el nombre puede aparecer como parametro'
\echo '    (in_columna) y en ese caso no es una referencia real.'

SELECT p.proname AS rutina,
       CASE p.prokind WHEN 'p' THEN 'procedimiento' ELSE 'funcion' END AS clase
  FROM pg_proc p
  JOIN pg_namespace n ON n.oid = p.pronamespace
 WHERE n.nspname = 'main'
   AND p.prosrc ILIKE '%' || :'columna' || '%'
 ORDER BY 1;

\echo ''
\echo '=== 5. Cuantas filas tiene la tabla (cuanto va a costar la reescritura) ==='
\echo '    Ensanchar (varchar(n)->varchar(m) mayor, varchar->text) NO reescribe.'
\echo '    Cualquier otro cambio reescribe la tabla entera con lock exclusivo.'

SELECT n_live_tup AS filas_aproximadas
  FROM pg_stat_user_tables
 WHERE schemaname = 'main' AND relname = :'tabla';

-- =====================================================================
-- PARTE B - Prueba real del casteo. SOLO EN LA BASE LOCAL.
-- =====================================================================
-- Es la unica prueba que vale: ejecuta el ALTER de verdad dentro de una
-- transaccion y la revierte. Si pasa aca, pasa en el despliegue.
--
-- ⚠️ NO CORRER CONTRA UN AMBIENTE COMPARTIDO. Aunque revierta, toma un lock
-- ACCESS EXCLUSIVE y reescribe la tabla mientras dura: bloquea a todo el
-- mundo. En una tabla grande eso es una caida.
--
-- Para habilitarla, agregar -v probar=si a la linea de psql.
-- =====================================================================

\if :{?probar}
\echo ''
\echo '=== PRUEBA REAL DEL CASTEO (transaccion revertida) ==='

BEGIN;

SELECT format('ALTER TABLE main.%I ALTER COLUMN %I TYPE %s USING %I::%s',
              :'tabla', :'columna', :'tipo_destino', :'columna', :'tipo_destino') AS sentencia_probada
\gset
\echo :sentencia_probada
:sentencia_probada ;

\echo '--> El casteo funciona. Revirtiendo, la base queda como estaba.'
ROLLBACK;
\else
\echo ''
\echo '(Prueba real del casteo omitida. Para ejecutarla EN LOCAL: -v probar=si)'
\endif

\echo ''
\echo '=== Fin del chequeo ==='
