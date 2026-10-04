# Surge

![Logo de Surge](recursos/vista-previa.png)

[Mihon](https://github.com/mihonapp/mihon) con el scroll del modo **tira larga** (manhwa/webtoon) más fluido en tablets.

Cada día, GitHub Actions comprueba si salió una versión nueva de Mihon, le aplica los parches de `patches/`,
la compila y publica el APK en [Releases](../../releases). Así el arreglo se mantiene aunque Mihon se actualice.

## Qué cambia

Todo pensado para que el modo tira larga vaya fluido en tablets:

- `0001-reader-sin-capa-hardware.patch`: el lector siempre dibujaba todo a través de una capa extra de la GPU
  (una "hardware layer"), aunque solo hace falta para los filtros de escala de grises o colores invertidos.
  Esa capa obliga a redibujar la pantalla entera dos veces en cada fotograma del scroll, y en pantallas grandes
  como las de las tablets eso provoca tirones. Ahora la capa solo se usa cuando uno de esos filtros está activo.
- `0002-reader-maxima-tasa-de-refresco.patch`: mientras lees, la app pide a la pantalla su tasa de refresco
  más alta (90/120 Hz en muchas tablets). Si en los ajustes de Android limitaste la pantalla a 60 Hz, se respeta.
- `0003-tira-larga-precarga-mas-paginas.patch`: las páginas se preparan hasta dos pantallas por delante
  (antes, tres cuartos de pantalla). En una tablet cada página ocupa varias pantallas de alto, así que la
  siguiente suele estar lista antes de que llegues a ella, sin esperas ni saltos.
- `0004-tira-larga-inercia-segun-pantalla.patch`: al soltar el dedo, la tira sigue deslizándose con inercia
  proporcional al tamaño de la pantalla. Android calcula esa inercia en centímetros reales pensando en un móvil,
  así que en una tablet grande el deslizamiento se paraba demasiado pronto. En móviles no cambia nada.

La app tiene su propio logo (en `recursos/drawable/`) y se instala como **Surge** (`app.mihon.surge`), al lado de la Mihon oficial, porque está firmada con otra
clave. Para pasar tu biblioteca: en Mihon *Más → Copia de seguridad y restauración → Crear copia*, y en Surge
*Restaurar copia*.

## Actualizaciones automáticas en la tablet

Instala [Obtainium](https://github.com/ImranR98/Obtainium), toca *Añadir app* y pega la URL de este repo.
Obtainium te avisará e instalará cada versión nueva.

## Si una compilación falla

Se abre un issue en este repo. Suele significar que una versión nueva de Mihon cambió el código que toca el parche
y hay que adaptarlo. Al cambiar un parche, sube el número de `REVISION` para que se publique una release nueva.
