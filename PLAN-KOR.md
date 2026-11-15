# StandBy KOR — plan de mejoras (para Claude Code) — 14/11/2026

Base: fork de realfamousbae/android-custom-always-on (Kotlin + Compose, MIT), ya traducido al español y compilando en PRO.
Build: `cd ~/android-build/aon && ./gradlew --no-daemon -q assembleRelease` (SDK en ~/android-sdk, local.properties ya está).
Firmar: zipalign + apksigner con ~/android-build/kor-release.keystore → standby-es.apk.
Instalar: `adb install -r standby-es.apk`.
Objetivo de Iván: "todo lo que tiene el StandBy de iPhone, tal cual, de base en esta app". Sin publicidad, sin cuentas, sin internet salvo clima/Immich.
Uso real: cargador magnético que lo deja a ~45°, pantalla apagada → aparece solo. Umbral de inclinación alto.

## Features a agregar (en este orden)
1. Caras de reloj deslizables (swipe horizontal), recordar la última:
   - Digital grande (actual, mejorar tipografía: fina/condensada, ocupa todo el ancho, dígitos tabulares)
   - Analógico minimalista
   - Flip clock retro (animación de cambio de dígito)
   - Fotos: carrusel de Immich (fotos.generadores.ar, API key en ~/.config/kor/immich_key si existe; si no, pedirla) con hora chica en esquina
2. Widgets laterales (layout "reloj + tarjeta", swipe vertical en la tarjeta para cambiar):
   - Clima (Open-Meteo, sin key; ubicación Quilmes por defecto)
   - Calendario: próximo evento (CalendarContract)
   - Próxima alarma + batería grande
   - Now playing: MediaSession (carátula, título, play/pausa)
3. Notificaciones estilo iPhone: burbuja grande arriba con ícono y texto, elegir apps (WhatsApp, llamadas, SMS); tocar = abrir; se oculta sola a los 8 s.
4. Modo noche: rojo tenue, con sensor de luz (ya hay autobrillo: integrar), horario configurable.
5. Personalización: color del reloj (paleta + elegir), fuente (3-4 opciones), tamaño, mostrar segundos, 12/24 h.
6. Pantalla de ajustes limpia en español con previsualización en vivo.

## Reglas
- Todo en español rioplatense. Sin publicidad ni telemetría.
- No romper lo que ya anda (disparo por inclinación + carga, overlay sobre bloqueo, DND).
- Cada feature que compile e instale OK: commit en git local del repo con mensaje claro.
- Al terminar cada bloque, dejar instalado en el S25 y anotar en este archivo qué quedó hecho.

## 7. Distribuible para amigos (IMPORTANTE, pedido de Iván 14/11)
- Iván se lo quiere pasar a amigos (gente grande, no técnica, con Samsung). Debe instalarse y configurarse SIN adb.
- Renombrar app a "Reloj de Mesa KOR" (applicationId ar.kor.standby2 o similar para no chocar), ícono propio simple (reloj), nombre en el launcher.
- Primera apertura: asistente guiado en español, un paso por pantalla, con botón grande que abra el ajuste del sistema correspondiente: Sobre otras apps, Batería sin restricciones, Notificaciones, Acceso a notificaciones, Protector de pantalla Samsung apagado. Explicar en criollo por qué.
- Valores por defecto ya buenos: umbral de inclinación alto (soporta ~45°), solo cargando ON, modo noche automático.
- Generar APK firmado final en ~/android-build/aon/RelojDeMesaKOR.apk y copiarlo a /home/kor/kor/apk/ (carpeta de APKs de KOR). Tamaño chico (minify/shrink resources).

---

# HECHO — 14/11/2026 (Claude Code, sesión de la noche)

Las 7 features están hechas, compiladas, firmadas, instaladas en el S25 y commiteadas una
por una. Siete commits, del `fd3b31f` al `c351cc4`.

## 1. Caras de reloj deslizables · `fd3b31f`
- Carrusel horizontal con las 4 caras; la última queda guardada y vuelve sola al otro día.
- **Digital**: IBM Plex Sans Condensed ExtraLight a todo el ancho, dígitos tabulares, y un
  techo de tamaño para que la fecha no se quede afuera en pantallas bajas.
- **Analógica**: marcas y agujas, sin números, cuadrada y centrada.
- **Flip**: cada dígito gira en dos tiempos (cae la mitad de arriba del viejo y después
  entra la de abajo del nuevo), con ficha y ranura al medio.
- **Fotos**: carrusel de Immich (`fotos.generadores.ar`), foto nueva cada 20 s con fundido y
  la hora abajo a la izquierda sobre una sombra.
- La clave de Immich se carga desde Ajustes **o** empujando un archivo por adb
  (`/sdcard/Android/data/ar.kor.reloj/files/immich.txt`); la app la importa y borra el
  archivo. Para esta prueba se usó la clave de Iván de (clave privada, fuera del repo)
  (no está en el repo ni en el APK).
- Salir del modo ahora es solo con un toque de verdad: arrastrar para pasar de cara ya no
  lo cierra.
- Orden por adb para probar sin tocar el teléfono (protegida con el permiso DUMP):
  `adb shell am broadcast -a com.realfamousbae.standby.dock.SHOW_NOW -p ar.kor.reloj`.

## 2. Tarjetas laterales · `bae190c`
- Carrusel vertical en la tarjeta de la derecha, con la última elección guardada.
- **Clima**: Open-Meteo sin clave, Quilmes por defecto y lugar configurable; dibujos del
  tiempo hechos a mano (sol, luna, nubes, lluvia, chaparrón, nieve, tormenta, niebla).
- **Agenda**: próximos eventos del calendario, con "Hoy / Mañana / Lun 17".
- **Alarma y batería**: próxima alarma del sistema, porcentaje grande y barra de carga.
- **Reproduciendo**: carátula, título, app y botones de play/pausa y siguiente.
- El widget del sistema pasó a ser una tarjeta más del mismo carrusel.

## 3. Avisos estilo iPhone · `b35dada`
- Burbuja grande arriba: ícono de la app, título y texto, 8 segundos en pantalla.
- Tocar el aviso abre la app (si está bloqueado, el sistema pide desbloquear primero).
- Deslizar el aviso para arriba lo saca sin abrir nada.
- De qué apps se muestran se elige en Ajustes → Notificaciones → Apps (ya estaba).

## 4. Modo noche · `b35dada`
- Todo en rojo tenue: reloj, tarjetas, avisos y hasta las fotos (velo rojo).
- Por horario configurable (22:00 a 07:00 de fábrica) y, si se quiere, también cuando el
  sensor de luz ve el cuarto a oscuras (menos de 3 lux).
- Con el modo noche prendido el brillo no pasa del 25 %.

## 5. Personalización · `c863995`
- Color del reloj (8 colores), 4 tipografías (condensada fina, redonda gruesa, geométrica y
  monoespaciada), tamaño del 60 % al 130 %, mostrar segundos y 12/24 horas.

## 6. Ajustes limpios con previsualización en vivo · `c863995`
- Arriba de todo se ve cómo va a quedar el reloj, con los cambios al toque.
- Secciones en orden: Funcionando · El reloj · Pantalla del modo y tarjetas (clima, agenda,
  Immich, widget) · Modo noche · Notificaciones · Cuándo aparece · Permisos y diagnóstico.

## 7. Distribuible para amigos · `4faefd1`
- Se llama **Reloj de Mesa KOR**, applicationId **`ar.kor.reloj`** (no `ar.kor.standby2`:
  en el teléfono ya había un `ar.kor.standby` de otra prueba, así no se pisa nada).
- Ícono propio: un reloj de mesa, adaptable y con versión monocroma.
- Guía de la primera vez, 7 pasos, en criollo, con botón grande que abre cada ajuste de
  Android y dice si ya está listo. Se puede volver a abrir desde Ajustes.
- Valores de fábrica: inclinación hasta 55°, solo cargando y modo noche prendido.
- R8 + shrink de recursos: el APK pasó de **27 MB a 3,6 MB**.
- APK firmado en `~/android-build/aon/RelojDeMesaKOR.apk` y copiado a `/home/kor/kor/apk/`.

## Extra · `c351cc4`
- Traducido al español todo lo que quedaba en ruso: comentarios de `:core` y `:app`, Gradle,
  manifiestos y nombres de las pruebas. README nuevo contando lo que hace la app.
- Los APK ya no se versionan en git (se generan con `./gradlew assembleRelease`).

## Cómo quedó el teléfono
- Instalado **Reloj de Mesa KOR** (`ar.kor.reloj`) con todos los permisos dados por adb
  (sobre otras apps, batería, notificaciones, acceso a notificaciones, No molestar y
  calendario) y la clave de Immich ya cargada.
- **Desinstalado** el viejo `com.realfamousbae.standby.dock`, para que no haya dos apps
  peleando por el mismo disparo. Sigue instalado `ar.kor.standby` (versión 1.2, de otra
  prueba del 14/11 20:28): no se tocó, pero si también muestra un reloj conviene sacarlo.

## Pendientes / para mirar
- `docs/PLAN.md` sigue en ruso: es el plan del autor original, no se tradujo.
- La cara de fotos pide la clave de Immich por app; si Iván le pasa el APK a un amigo, esa
  cara queda con un cartel explicando qué falta (no se cuelga).
- El clima usa el lugar elegido a mano (no pide permiso de ubicación, a propósito).
- Probado todo en el S25 en horizontal; en vertical el modo también entra, pero las caras
  están pensadas para apaisado, que es como queda en el cargador.

## Fase 2 — paridad con el StandBy de iOS actual (pedido 15/11/2026, ANTES del video de YouTube)
Orden sugerido. Cada ítem: compilar, instalar en el S25 por adb, commit, y anotar acá qué quedó.
1. **Dos columnas de widgets apilados** (layout "Widgets"): cada columna con swipe vertical propio; reutilizar las tarjetas existentes (Clima, Calendario, Alarma/Batería, Música, Widget del sistema) y sumar Recordatorios/Próxima alarma grande.
2. **Más caras**: Cuadrícula de dígitos (cada dígito en una celda, color pleno), Solar (el color de fondo/dígitos sigue la hora del día: alba, día, atardecer, noche), Flotante (números redondos gordos con color de acento) y Reloj mundial (2-3 ciudades configurables).
3. **Actividades en vivo a pantalla completa**: llamada entrante (nombre, atender/colgar), timer/cronómetro del sistema, música con carátula grande y controles.
4. **Memoria por cargador**: recordar cara/tarjeta por ubicación (Wi-Fi SSID + nombre del cargador si lo expone), ej. mesa de luz vs escritorio.
5. **Fotos**: efecto Ken Burns suave, "recuerdos" por fecha (mismo día otros años), respetar solo favoritas.
6. **Siempre encendido con atenuación**: en vez de apagar la pantalla, bajar a mínimo brillo con reloj chico tras N minutos, y volver al tocar; cuidar batería (solo cargando).
7. El comando adb DEMO debe poder fijar también las caras/layouts nuevos (para capturas y video).
Reglas: todo en español rioplatense, sin romper lo que anda, sin secretos en el repo, README y galería actualizados al terminar.

---

# HECHO — Fase 2, 15/11/2026 (Claude Code, sesión de la tarde)

Los siete ítems están hechos, compilados, firmados, instalados en el S25 y commiteados uno
por uno: `7e75b83`, `dbabab6`, `cbf257f`, `76dfc3a`, `f8c611b`, `ad6dca5` y el de las
capturas. Todo probado en el teléfono salvo lo que se aclara abajo.

## 1. Dos columnas de tarjetas · `7e75b83`
- Layout nuevo **"Dos columnas"**: dos columnas de tarjetas apiladas, cada una con su propio
  swipe vertical y su elección guardada.
- El **reloj pasó a ser una tarjeta más**, así la hora puede ir en una columna (si no, el
  reloj de mesa se quedaba sin hora).
- Tarjeta nueva **"Próxima alarma"**: la hora bien grande, el día y cuánto falta.
- El widget del sistema solo se dibuja en la primera columna: Android no deja tener dos
  vistas del mismo widget.

## 2. Cuatro caras nuevas · `dbabab6`
- **Cuadrícula** (cada dígito en su celda de color pleno), **Flotante** (hora arriba, minutos
  abajo, redonda gorda), **Solar** (el color de los números sigue la hora del día) y
  **Reloj mundial** (la hora de acá más hasta tres ciudades, con "mañana"/"ayer").
- Las ciudades se eligen en Ajustes cuando la cara puesta es la mundial.
- Se arregló algo que venía de antes: las caras que sacan el tamaño de la letra del espacio
  que tienen usaban `sp`, y el ajuste "tamaño de letra" del sistema (1,15 en el S25) las
  agrandaba hasta que no entraban. Ahora pasan de dp a sp con `dpAsSp`.
- También se arregló el `DEMO` por adb: usaba `runBlocking` en el hilo principal y con el
  proceso recién arrancado se trababa 60 s con la primera lectura de DataStore ("la app no
  responde"). Ahora va con `goAsync`.

## 3. Actividades en vivo a pantalla completa · `cbf257f`
- **Llamada**: el nombre grande y los botones redondos de atender y colgar, sacados de las
  intenciones del CallStyle de la notificación.
- **Temporizador y cronómetro** con sus botones de verdad. El reloj de Samsung **no** prende
  la bandera de cronómetro de Android: mete un `Chronometer` adentro de las vistas de la
  notificación, así que si la bandera no está se arman las vistas y se busca el reloj adentro.
  Probado con el temporizador del S25: contó bien y el botón "Cancelar" canceló de verdad.
- **Música**: carátula grande, título y anterior/play/siguiente.
- Un toque que se quedó un botón ya no cierra el modo.
- Al conectarse el oyente se miran las notificaciones que ya estaban (si el teléfono se
  reinició en medio de una llamada, eso sigue pasando).

## 4. Memoria por cargador · `76dfc3a`
- El lugar se reconoce por **cómo está cargando** (inalámbrico, pared, USB, sin cargador) y
  por los **voltios de carga**, que delatan al cargador. Hasta ocho lugares.
- Al aparecer en un lugar conocido vuelve la cara y las tarjetas de la última vez; al
  cambiarlas, se anotan ahí. En Ajustes se ve en qué lugar estás y se pueden olvidar todos.
- **No se pide el permiso de ubicación**, aunque el plan hablaba del nombre de la red wifi:
  desde Android 10 leer el SSID exige ubicación, y una app de reloj que pide ubicación
  asusta, más todavía para pasársela a un amigo. El cargador alcanza para lo que Iván quería
  distinguir (mesa de luz contra escritorio).

## 5. Fotos · `f8c611b`
- **Ken Burns**: cada foto entra un poco acercada y durante sus 20 s se acerca y se corre
  despacio; el lado al que se corre sale del id de la foto. De noche se queda quieta.
- **Recuerdos**: las fotos del mismo día de los últimos doce años, con el cartel "Recuerdos
  de un 15 de noviembre". Si ese día nunca pasó nada, sigue con fotos al azar.
- **Solo las favoritas** ahora también vale para los recuerdos.
- Probado contra el Immich de Iván: las fotos se mueven y los recuerdos del 15 de noviembre
  salieron.

## 6. Siempre encendido con atenuación · `ad6dca5`
- A los N minutos sin tocar nada (1 a 30, de fábrica 5) y **solo en el cargador**, la pantalla
  baja al brillo mínimo y queda la hora chiquita. Un toque la vuelve a encender y no cierra
  el modo.
- El reloj chiquito se corre de lugar cada minuto siguiendo vueltas de 7 y 5: tarda 35
  minutos en repetir el mismo punto, para no quemar el OLED.
- A batería no se atenúa: la pantalla se apaga como siempre.
- La pantalla atenuada se probó forzándola por adb (`--es dimnow 1`), porque mientras se
  probaba el teléfono estaba fuera del cargador. La regla de "solo cargando" es justamente la
  que impidió probarla sola, y está cubierta por los tests de `:core`.

## 7. El DEMO por adb llega a todo lo nuevo · (commit de capturas)
- `face` acepta las ocho caras, `layout` acepta `Widgets`, y se suman `card2`, `cities`,
  `live`, `dim`, `dimminutes`, `dimnow`, `memories`, `favorites`, `charging`, `places` y `on`.
- `--es dump 1` devuelve el registro de diagnóstico y qué está puesto ahora, como resultado
  del broadcast: en los Samsung, `logcat` no muestra lo que escriben las apps de uno, así que
  era la única forma de mirar el registro sin tocar el teléfono.
- Lo que fija el DEMO también queda anotado como "lo de este lugar", para que la memoria por
  cargador no pise una captura recién armada.

## Pendientes / para mirar
- **Se sacó del repositorio la captura de la agenda** (`06-tarjeta-Calendario`): mostraba un
  turno personal de Iván ("Limpieza dental…") y el repositorio es público. Si ya se publicó
  alguna vez, conviene saber que estuvo ahí; en la galería nueva no hay ninguna captura con
  datos personales ni fotos de Immich.
- La cara de fotos sigue necesitando la clave de Immich; sin ella queda el cartel de siempre.
- La música a pantalla completa se probó con la actividad de mentira del DEMO: no se puso
  música de verdad en el teléfono de Iván para no hacerle sonar nada. Usa la misma fuente que
  la tarjeta "Reproduciendo", que sí anda.
- `README.en.md` sigue siendo el del autor original (habla de una app en ruso, del S24 y de
  funciones que ya no son estas). El README en español está al día; el inglés habría que
  rehacerlo entero o sacarlo.
- "Recordatorios" (del ítem 1) quedó como "Próxima alarma": Android no tiene una lista de
  recordatorios que una app pueda leer, solo alarmas y calendario, y las dos ya están.
