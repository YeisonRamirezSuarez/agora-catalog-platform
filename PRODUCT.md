# Product

<!-- impeccable:product-schema 1 -->

## Platform

web

## Stack

Producto final: Spring Boot 4 + Angular 21 (CSR) según `ESPECIFICACION.md`. El prototipo de diseño (`docs/prototipo/index.html`) es HTML, CSS y JS sin dependencias, desechable una vez aprobado.

## Users

- **Visitante del catálogo** (anónimo, sin login). Mezcla de dos públicos: clientes del barrio que reciben el link por WhatsApp, miran desde el celular y piden domicilio o pasan a la tienda; y negocios (tiendas, restaurantes, asaderos) que compran por volumen y comparan precios. El catálogo debe servir a los dos.
- **Administrador de la empresa** (`ADMIN_EMPRESA`): el dueño o un empleado del negocio. Carga productos, precios, ofertas y fotos, muchas veces desde el celular y también desde Excel.
- **Super admin** (`SUPER_ADMIN`): opera la plataforma; crea y desactiva empresas.

## Product Purpose

Cada negocio publica su catálogo en un link público, permanente y sin login, que comparte por WhatsApp. El visitante ve qué se vende, a qué precio y qué está en oferta, arma un pedido y lo envía por WhatsApp con cantidades y total. El pedido vive solo en su navegador: no hay pagos, cuentas ni pedidos guardados en el servidor. Éxito: el visitante encuentra el producto rápido y envía su pedido ya escrito; el dueño mantiene el catálogo al día sin ayuda.

## Positioning

Agorá Catalog es una plataforma SaaS multiempresa con **identidad propia visible**: cada catálogo se reconoce como un catálogo de Agorá Catalog, y la marca de cada negocio (logo, colores, nombre) vive dentro de ese sistema. Mecanismo propio: un link que nunca expira (resuelto por un hash inmutable), carga masiva desde Excel y fotos por ZIP según el código del producto, y precios u ofertas que el negocio puede ocultar.

## Operating Context

- El link se comparte en chats y estados de WhatsApp; la vista previa (Open Graph) es la primera impresión.
- El visitante lo abre en celulares de gama media o baja, muchas veces con datos móviles.
- El administrador trabaja entre ventas, desde el celular; la carga grande la hace con Excel en un computador.
- Precios en pesos colombianos sin decimales (`$12.500`), unidades de venta por libra, kilo, unidad, paquete o caja.

## Capabilities and Constraints

- Funciones confirmadas en `ESPECIFICACION.md` §6–§8: portada con titular configurable, sellos, colecciones ("¿Cuál es el plan?"), favoritos por pestañas, corazones de favoritos guardados en el navegador, sección de contacto a la que lleva "Contáctanos", franja de una categoría, página de catálogo con búsqueda, filtros y orden, pedido por WhatsApp, etiquetas Agotado / Promo / Más vendido / Nuevo, compartir, toggles de precios, pedido y agotados.
- Un solo diseño de catálogo para todos los negocios; cada uno configura portada, frase, sellos, colecciones, franja y dos colores. El color de botones debe leerse con texto blanco y el de acento sobre la portada oscura: el panel no guarda colores sin contraste.
- Fuera de alcance: pagos, pedidos guardados en el servidor, inventario, login de clientes finales, favoritos guardados en el servidor.

## Brand Commitments

- Agorá Catalog tiene marca propia visible en todos los catálogos públicos.
- Decisión del cliente (2026-10-16): el catálogo público sigue el diseño que pidió Salsamentaria Javier (imagen "Salsamentaria Javier: Tradición y Buen Sabor"): portada fotográfica oscura con titular serif y segunda línea en cursiva dorada, mostrador crema con tarjetas de producto, notas manuscritas y pedido por WhatsApp. Reemplaza la referencia anterior a las apps de domicilios.
- Salsamentaria Javier tiene logo propio (el emblema de la vaca, en `docs/prototipo/img/cliente/`) y números de WhatsApp reales. Colores del semilla: botones `#24331F`, acento `#DDB06C`, tomados del boceto. Aún no tiene fotos reales de sus productos.
- Texto para el usuario en español neutro con tuteo.

## Evidence on Hand

- Lista de productos de ejemplo en `ESPECIFICACION.md` §9 (embutidos, carnes frías, quesos, lácteos, abarrotes).
- Logo, nombre, dueño y números de WhatsApp reales del negocio. No hay fotos de productos ni testimonios reales. Las fotos del prototipo son imágenes de prueba de Wikimedia Commons con su fuente y licencia registradas; no deben presentarse como fotos del negocio. Las fotos del boceto del cliente son generadas por IA y no se usan.

## Product Principles

1. El producto y su precio primero: el visitante viene a ver qué hay y cuánto cuesta.
2. Todo termina en una conversación de WhatsApp, idealmente con el pedido ya escrito; ninguna pantalla deja al visitante sin ese camino.
3. Funciona en el celular más modesto y con datos lentos.
4. El dueño no necesita ayuda para mantener su catálogo.
5. La marca Agorá Catalog acompaña sin tapar la del negocio.

## Accessibility & Inclusion

Público general de barrio, con celulares de pantalla pequeña: texto legible sin zoom, contraste suficiente con cualquier color del negocio y botones del tamaño del dedo.
