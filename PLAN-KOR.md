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
