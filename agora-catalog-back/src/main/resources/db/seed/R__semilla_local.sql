-- ============================================================
-- R — semilla de desarrollo local (ESPECIFICACION.md §9)
-- ============================================================
-- QUE HACE:    INSERT con ON CONFLICT DO NOTHING: 1 empresa con sus sellos y WhatsApp, 6 categorías,
--              25 productos, 3 colecciones y su administrador de prueba
-- DESTRUCTIVO: NO (nunca pisa filas que ya existan)
-- TIPOS:       no cambia el tipo de ninguna columna
-- POR QUE:     tener un catálogo realista para desarrollar sin cargar datos a mano
-- MEDIDO EN:   local — base recién migrada
-- ============================================================
-- Solo corre con settings.flyway.seed=true (perfil local y docker-compose); nunca en dev ni prod.
-- Es repetible: Flyway la vuelve a aplicar cuando cambia el archivo, por eso todo es idempotente.
-- Administrador de prueba: javier@agora.local, contraseña Agora123*.

INSERT INTO main.empresa (nombre, slug, public_hash, color_primario, color_secundario,
                          nombre_corto, lema,
                          portada_antetitulo, portada_titulo, portada_titulo_acento, portada_subtitulo,
                          portada_nota, portada_sello_texto,
                          catalogo_titulo, catalogo_acento, catalogo_subtitulo,
                          colecciones_nota, visita_nota, fachada)
VALUES ('Salsamentaria Javier', 'salsamentaria-javier', 'x7k2m9ab', '#24331F', '#DDB06C',
        'Javier', 'Tradición y buen sabor',
        'El sabor de reunirnos', 'Se ve rico.', E'Sabe a\nun buen plan.',
        'Embutidos, quesos y salsamentaria de la mejor calidad para tus momentos especiales.',
        E'Productos\nque hacen\nmejores momentos', 'Tradición · desde siempre',
        E'Tradición\nen cada', 'bocado.',
        'Productos seleccionados de la mejor calidad para tus momentos especiales.',
        E'Siempre\nqueda bien.', E'El buen sabor\nde siempre', 'SALSAMENTARIA')
ON CONFLICT (slug) DO NOTHING;

INSERT INTO main.empresa_whatsapp (empresa_id, numero, nombre, orden)
SELECT e.id, w.numero, w.nombre, w.orden
  FROM main.empresa e,
       (VALUES ('573163218276', 'Pedidos', 1), ('573015303994', NULL, 2)) AS w(numero, nombre, orden)
 WHERE e.slug = 'salsamentaria-javier'
ON CONFLICT (empresa_id, numero) DO NOTHING;

INSERT INTO main.empresa_sello (empresa_id, icono, texto, orden)
SELECT e.id, s.icono, s.texto, s.orden
  FROM main.empresa e,
       (VALUES ('CALIDAD', 'Productos de la mejor calidad', 1),
               ('DOMICILIO', 'Domicilios en el barrio', 2),
               ('WHATSAPP', 'Pide fácil por WhatsApp', 3),
               ('PRECIO', 'Precios justos todos los días', 4)) AS s(icono, texto, orden)
 WHERE e.slug = 'salsamentaria-javier'
   AND NOT EXISTS (SELECT 1 FROM main.empresa_sello x WHERE x.empresa_id = e.id);

INSERT INTO main.categoria (empresa_id, nombre, orden, banner_activo, banner_titulo, banner_frase, banner_nota)
SELECT e.id, c.nombre, c.orden, c.banner, c.titulo, c.frase, c.nota
  FROM main.empresa e,
       (VALUES ('Embutidos', 1, FALSE, NULL, NULL, NULL),
               ('Carnes frías', 2, FALSE, NULL, NULL, NULL),
               ('Quesos', 3, FALSE, NULL, NULL, NULL),
               ('Lácteos', 4, FALSE, NULL, NULL, NULL),
               ('Abarrotes', 5, FALSE, NULL, NULL, NULL),
               ('Combos', 6, TRUE, E'Combos\nde la casa', 'Todo listo para compartir, en un solo pedido.',
                E'Listos\npara disfrutar')) AS c(nombre, orden, banner, titulo, frase, nota)
 WHERE e.slug = 'salsamentaria-javier'
ON CONFLICT (empresa_id, nombre) DO NOTHING;

-- Cuatro ofertas vigentes, un agotado y tres nuevos (creados hoy; el resto hace 60 días).
INSERT INTO main.producto (empresa_id, categoria_id, codigo, nombre, descripcion, presentacion, unidad_venta,
                           precio, precio_oferta, oferta_desde, oferta_hasta, disponible, destacado, orden, created_at)
SELECT e.id, c.id, p.codigo, p.nombre, p.descripcion, p.presentacion, p.unidad, p.precio, p.oferta,
       CASE WHEN p.oferta IS NOT NULL THEN current_date - 3 END,
       CASE WHEN p.oferta IS NOT NULL THEN current_date + 30 END,
       p.disponible, p.destacado, p.orden,
       CASE WHEN p.nuevo THEN now() ELSE now() - INTERVAL '60 days' END
  FROM main.empresa e
  JOIN (VALUES
    ('EMB-001', 'Embutidos',    'Salchichón cervecero',   'Ahumado, ideal para la picada.',          'Barra x 500 g',   'UNIDAD',  14500, NULL::numeric, TRUE,  FALSE, FALSE, 1),
    ('EMB-002', 'Embutidos',    'Chorizo santarrosano',   'Receta tradicional, para asar.',          'Paquete x 5',     'PAQUETE', 16800, 14900,         TRUE,  TRUE,  FALSE, 2),
    ('EMB-003', 'Embutidos',    'Mortadela especial',     'Suave, tajada al gusto.',                 'Por libra',       'LIBRA',    9800, NULL,          TRUE,  FALSE, FALSE, 3),
    ('EMB-004', 'Embutidos',    'Butifarra soledeña',     'Para picar con limón.',                   'Paquete x 10',    'PAQUETE', 12500, NULL,          TRUE,  FALSE, FALSE, 4),
    ('EMB-005', 'Embutidos',    'Longaniza',              'Delgada, para fritar.',                   'Por libra',       'LIBRA',   13900, NULL,          TRUE,  FALSE, TRUE,  5),
    ('CF-001',  'Carnes frías', 'Jamón de cerdo tajado',  'Jamón cocido, tajado fino.',              'Tajado x 250 g',  'PAQUETE', 11900, NULL,          TRUE,  TRUE,  FALSE, 1),
    ('CF-002',  'Carnes frías', 'Jamón ahumado',          'Ahumado en leña.',                        'Tajado x 250 g',  'PAQUETE', 14200, 12500,         TRUE,  FALSE, FALSE, 2),
    ('CF-003',  'Carnes frías', 'Pechuga de pavo',        'Baja en grasa.',                          'Tajado x 250 g',  'PAQUETE', 15800, NULL,          TRUE,  FALSE, FALSE, 3),
    ('CF-004',  'Carnes frías', 'Tocineta ahumada',       'Para el desayuno.',                       'Paquete x 250 g', 'PAQUETE', 10900, NULL,          FALSE, FALSE, FALSE, 4),
    ('CF-005',  'Carnes frías', 'Salami italiano',        'Curado, de sabor intenso.',               'Tajado x 100 g',  'PAQUETE',  9500, NULL,          TRUE,  FALSE, TRUE,  5),
    ('QUE-001', 'Quesos',       'Queso campesino',        'Fresco, del día.',                        'Bloque',          'UNIDAD',  12000, NULL,          TRUE,  TRUE,  FALSE, 1),
    ('QUE-002', 'Quesos',       'Queso doble crema',      'El de las arepas.',                       'Por libra',       'LIBRA',   13500, NULL,          TRUE,  FALSE, FALSE, 2),
    ('QUE-003', 'Quesos',       'Queso costeño',          'Salado, para rallar o picar.',            'Por libra',       'LIBRA',   14800, 12900,         TRUE,  FALSE, FALSE, 3),
    ('QUE-004', 'Quesos',       'Queso mozzarella',       'Para pizzas y gratinados.',               'Bloque x 500 g',  'UNIDAD',  15500, NULL,          TRUE,  FALSE, FALSE, 4),
    ('QUE-005', 'Quesos',       'Cuajada',                'Fresca, con melao o sola.',               'Unidad x 400 g',  'UNIDAD',   7800, NULL,          TRUE,  FALSE, FALSE, 5),
    ('LAC-001', 'Lácteos',      'Kumis',                  'Cremoso y endulzado.',                    'Botella x 1 L',   'UNIDAD',   6900, NULL,          TRUE,  FALSE, FALSE, 1),
    ('LAC-002', 'Lácteos',      'Mantequilla',            'Con sal.',                                'Barra x 250 g',   'UNIDAD',   8900, NULL,          TRUE,  FALSE, FALSE, 2),
    ('LAC-003', 'Lácteos',      'Yogur de mora',          'Con trozos de fruta.',                    'Botella x 1 L',   'UNIDAD',   7900, NULL,          TRUE,  FALSE, FALSE, 3),
    ('LAC-004', 'Lácteos',      'Suero costeño',          'Para acompañar la arepa.',                'Vaso x 250 g',    'UNIDAD',   5200, NULL,          TRUE,  FALSE, FALSE, 4),
    ('ABA-001', 'Abarrotes',    'Arepas de maíz blanco',  'Listas para asar.',                       'Paquete x 10',    'PAQUETE',  6500, NULL,          TRUE,  FALSE, FALSE, 1),
    ('ABA-002', 'Abarrotes',    'Galletas de soda',       'Para la picada.',                         'Paquete x 6',     'PAQUETE',  4900, NULL,          TRUE,  FALSE, FALSE, 2),
    ('ABA-003', 'Abarrotes',    'Aceitunas verdes',       'Sin semilla.',                            'Frasco x 200 g',  'UNIDAD',   8500, NULL,          TRUE,  FALSE, TRUE,  3),
    ('COM-001', 'Combos',       'Combo picada familiar',  'Salchichón, chorizo, queso y galletas.',  'Para 6 personas', 'UNIDAD',  59000, 52000,         TRUE,  TRUE,  FALSE, 1),
    ('COM-002', 'Combos',       'Combo desayuno',         'Jamón, queso, arepas y kumis.',           'Para 4 personas', 'UNIDAD',  38000, NULL,          TRUE,  FALSE, FALSE, 2),
    ('COM-003', 'Combos',       'Combo parrillero',       'Chorizo, butifarra, longaniza y arepas.', 'Para 8 personas', 'UNIDAD',  72000, NULL,          TRUE,  FALSE, FALSE, 3)
  ) AS p(codigo, categoria, nombre, descripcion, presentacion, unidad, precio, oferta, disponible, destacado, nuevo, orden)
    ON TRUE
  JOIN main.categoria c ON c.empresa_id = e.id AND c.nombre = p.categoria
 WHERE e.slug = 'salsamentaria-javier'
ON CONFLICT (empresa_id, codigo) DO NOTHING;

INSERT INTO main.coleccion (empresa_id, nombre, descripcion, orden)
SELECT e.id, k.nombre, k.descripcion, k.orden
  FROM main.empresa e,
       (VALUES ('Una buena picada', 'Para compartir con los amigos.', 1),
               ('Desayunar sin afán', 'Lo de siempre para empezar el día.', 2),
               ('Me dejo antojar', 'Algo distinto para darse gusto.', 3)) AS k(nombre, descripcion, orden)
 WHERE e.slug = 'salsamentaria-javier'
   AND NOT EXISTS (SELECT 1 FROM main.coleccion x WHERE x.empresa_id = e.id);

INSERT INTO main.coleccion_producto (coleccion_id, producto_id, orden)
SELECT k.id, p.id, cp.orden
  FROM main.empresa e
  JOIN main.coleccion k ON k.empresa_id = e.id
  JOIN (VALUES ('Una buena picada', 'EMB-001', 1), ('Una buena picada', 'EMB-002', 2),
               ('Una buena picada', 'QUE-003', 3), ('Una buena picada', 'ABA-002', 4),
               ('Una buena picada', 'COM-001', 5),
               ('Desayunar sin afán', 'CF-001', 1), ('Desayunar sin afán', 'QUE-002', 2),
               ('Desayunar sin afán', 'LAC-001', 3), ('Desayunar sin afán', 'ABA-001', 4),
               ('Desayunar sin afán', 'COM-002', 5),
               ('Me dejo antojar', 'CF-005', 1), ('Me dejo antojar', 'ABA-003', 2),
               ('Me dejo antojar', 'QUE-005', 3), ('Me dejo antojar', 'LAC-003', 4)
       ) AS cp(coleccion, codigo, orden) ON cp.coleccion = k.nombre
  JOIN main.producto p ON p.empresa_id = e.id AND p.codigo = cp.codigo
 WHERE e.slug = 'salsamentaria-javier'
ON CONFLICT (coleccion_id, producto_id) DO NOTHING;

-- Contraseña Agora123* (BCrypt). Solo para la base local. El SUPER_ADMIN no va en la semilla:
-- lo crea SuperAdminBootstrap con los datos de SUPERADMIN_* del .env.
INSERT INTO main.usuario (empresa_id, nombre, email, password_hash, rol)
SELECT e.id, 'Javier Barón', 'javier@agora.local',
       '$2a$10$/gAsXoNT68ZmFyb1UYN5j.Q.EKMERhZZxdg.YfERq5iNv0F9QH7QO', 'ADMIN_EMPRESA'
  FROM main.empresa e
 WHERE e.slug = 'salsamentaria-javier'
ON CONFLICT ((lower(email))) DO NOTHING;
