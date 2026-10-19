---
version: 1
slug: "docs-prototipo-index-html"
primary_target: "docs/prototipo/index.html"
related_targets: []
---

## Scope

Prototipo navegable `docs/prototipo/index.html`: catálogo público (modo Persuade, prioridad) rediseñado al diseño que pidió el cliente (imagen `Salsamentaria Javier_ Tradición y Buen Sabor.png`), y panel de administración + super admin (modo Operate) que suman los campos que el catálogo nuevo necesita. El panel, el login y «Catálogo no disponible» llevan la marca Agorá Catalog (tinta cálida y dorado del sello, foco dorado oscuro, Playfair solo en títulos de página, Onest en lo demás); el catálogo público no hereda esos valores.

## Audience and job

Clientes del barrio y compradores de negocios que abren el link desde WhatsApp en el celular. Éxito: armar un pedido (o preguntar por un producto) y enviarlo por WhatsApp con todo ya escrito. El administrador mantiene precios, fotos, ofertas, la portada con su frase, los sellos, las colecciones («¿Cuál es el plan?») y la franja de combos desde el panel.

## Constraints

- Decisiones del usuario (2026-10-16): pedido por WhatsApp guardado solo en el navegador, sin pagos ni cuentas; un solo diseño para todos los negocios, con portada, frase, sellos, colecciones y colores configurables; secciones: portada con frase grande, ¿Cuál es el plan?, Combos de la casa, filtros en computador.
- Fotos de prueba de Wikimedia Commons con créditos en `docs/prototipo/img/CREDITOS.md`; nunca presentadas como fotos del negocio.
- Colores del negocio variables: primario oscuro (botones, debe leer texto blanco) y acento claro (cursiva y botón de portada, debe leerse sobre el fondo noche).
- Fidelidad al boceto del cliente por indicación explícita del usuario (2026-10-16: «conservar el diseño del cliente sin cambios»): se restauraron los antetítulos en mayúsculas espaciadas («El sabor de reunirnos», «Nuestro catálogo»), los corazones de favorito en las tarjetas y las notas manuscritas. Los antetítulos quedan solo en esta superficie; no son patrón del sistema para superficies nuevas.

## Direction contract

THESIS: La vitrina de una salsamentaria de barrio contada como una mesa servida de noche: portada fotográfica oscura con frase serif grande, y debajo un mostrador crema donde cada producto se suma a un pedido que sale por WhatsApp. Rechaza la app de domicilios genérica y la grilla de tarjetas iguales sin voz.

OWN-WORLD: Fondo noche #15120D en portada, sellos y pie; mostrador crema #F7F1E6 con tarjetas papel #FFFDF9; acento dorado del negocio en la cursiva, el botón de portada y la pestaña activa; botones de acción en el primario oscuro del negocio; precios carmesí; insignias Más vendido (miel), Promo (amarillo), Nuevo (menta). Playfair Display para titulares (cursiva para el acento), Ms Madi para notas manuscritas, Onest para todo lo funcional.

STORY: El visitante entiende en una pantalla qué vende el negocio y que puede pedir desde ahí; entra por un plan o por los favoritos, suma productos con el selector de cantidad, revisa «Mi pedido» y lo envía por WhatsApp al número que elija.

FIRST VIEWPORT: Encabezado oscuro sobre la foto: logo y nombre a la izquierda, menú al centro (escritorio), buscar y «Mi pedido $total» con contador a la derecha. Portada a sangre con degradado noche desde la izquierda; titular serif de 84 px (escritorio) en dos tonos, blanco y cursiva dorada; subtítulo; botón dorado «Explorar el catálogo»; sello circular con el nombre del negocio abajo a la derecha. Franja de sellos al pie de la portada. En el celular: titular de 40 a 50 px y buscador dentro de la portada; sin barra inferior: «Mi pedido» vive en el encabezado. «Contáctanos» lleva a la sección #contacto (sin ventana emergente).

FORM: Boceto del cliente fijado por el brief (gana sobre la tirada); semilla registrada 98d08d1d; construcción guiada por código, sin generación de imágenes; el boceto del cliente es la referencia de crítica.

FINISH: unreviewed and undocumented is unfinished; this build ends with the finish review, the verdict, DESIGN.md, and every shipping raster carrying its provenance

## Memorable moment

Sumar un producto: el selector de cantidad y el botón oscuro de carrito, el total de «Mi pedido» que sube en el encabezado, y el pedido que llega a WhatsApp ya escrito, con cantidades y total.

## Unresolved

- Las fotos del boceto son generadas por IA; el prototipo usa fotos de prueba reales de Commons hasta tener las del negocio.
- La ilustración de la fachada del boceto («Nos vemos en Javier») no existe; se omite hasta tener una del negocio.
