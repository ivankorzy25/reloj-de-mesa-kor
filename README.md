# Reloj de Mesa KOR

Modo mesa de luz para Android, al estilo del StandBy del iPhone. Dejás el teléfono cargando
y acostado de costado, se apaga la pantalla y en vez del bloqueo aparece un reloj grande.
Cuando lo levantás, desaparece solo.

Es un fork en español de [android-custom-always-on](https://github.com/realfamousbae/android-custom-always-on)
(MIT), con las mejoras que pidió Iván (ver `PLAN-KOR.md`).

Probado en un Samsung Galaxy S25 Ultra (Android 17, One UI 9). Necesita Android 12 o más nuevo.

## Qué hace

- **Aparece solo.** Se apagó la pantalla y el teléfono está apoyado sobre el lado largo:
  el reloj se pone por encima del bloqueo. Acostado boca arriba, o mientras lo estás usando,
  no aparece.
- **Cuatro caras de reloj**, que se pasan con el dedo de izquierda a derecha y se recuerdan:
  - **Digital**, con tipografía condensada fina a todo el ancho y dígitos de un solo ancho.
  - **Analógica** minimalista, sin números.
  - **Flip clock**, con el giro de ficha de los carteles viejos.
  - **Fotos**: carrusel de tu Immich, con la hora en una esquina.
- **Tarjeta al costado**, que se pasa con el dedo de arriba a abajo:
  - **Clima** de [Open-Meteo](https://open-meteo.com) (sin clave ni cuenta), del lugar que elijas.
  - **Agenda**: los próximos eventos del calendario del teléfono.
  - **Alarma y batería**: la próxima alarma del sistema y cuánta carga queda.
  - **Reproduciendo**: carátula, título y play/pausa de lo que esté sonando.
  - **Widget del sistema**: cualquier widget de escritorio, con los bordes desvanecidos.
- **Avisos estilo iPhone.** Las notificaciones nuevas aparecen arriba en una burbuja grande,
  se van solas a los 8 segundos, se tocan para abrir la app y se deslizan para arriba para
  sacarlas. Se elige de qué apps se muestran, y el texto se tapa con las mismas reglas que
  usa la pantalla de bloqueo.
- **Modo noche.** De noche (por horario, y si querés también cuando el sensor ve el cuarto a
  oscuras) todo pasa a rojo tenue y el brillo no supera el 25 %.
- **Personalización.** Color del reloj, cuatro tipografías, tamaño, segundos y 12/24 horas,
  con previsualización en vivo en Ajustes.
- **Sin publicidad, sin cuentas y sin telemetría.** Solo sale a internet para el clima y,
  si la configurás, para tus fotos de Immich.
- **Brillo automático** según la luz del ambiente, suave, sin saltos.
- **Guía de la primera vez** en criollo, un paso por pantalla, para dejarlo andando sin saber
  nada de Android.

## Instalar

El APK firmado se arma con Gradle y se instala a mano (no está en Google Play):

```bash
./gradlew assembleRelease           # queda en app/build/outputs/apk/release/
adb install -r app/build/outputs/apk/release/app-release.apk
```

Para que el release salga firmado hay que dejar un `keystore.properties` en la raíz (está en
`.gitignore`):

```properties
storeFile=/ruta/a/tu.keystore
storePassword=...
keyAlias=...
keyPassword=...
```

La primera vez, la app abre sola la guía de permisos. Los que hacen falta son: aparecer sobre
otras apps, batería sin restricciones, la notificación del servicio, acceso a notificaciones
y el protector de pantalla de Samsung apagado.

## Fotos de Immich

En Ajustes → Fotos de Immich se pone la dirección del servidor y una clave de API (Immich →
Cuenta → Claves de API), con botón para probar la conexión. La clave queda solo en el
teléfono.

Para no tipearla en la pantalla chica, también se puede empujar por adb y la app la toma en
el próximo arranque (y después borra el archivo):

```bash
printf 'url=https://tu-immich\nkey=TU_CLAVE\n' > immich.txt
adb push immich.txt /sdcard/Android/data/ar.kor.reloj/files/immich.txt
```

## Probar el modo sin tocar el teléfono

```bash
adb shell am broadcast -a com.realfamousbae.standby.dock.SHOW_NOW -p ar.kor.reloj
adb shell am broadcast -a com.realfamousbae.standby.dock.HIDE_NOW -p ar.kor.reloj
```

Está protegido con el permiso DUMP, que solo tienen adb y el sistema.

## Cómo está armado

- `:core` — Kotlin/JVM puro, sin Android: cuándo mostrar el modo (`TriggerEngine`), el brillo
  automático, las reglas de las notificaciones, el modo noche y los ajustes. Todo con unit tests.
- `:app` — todo lo que toca Android: servicio, Activity del modo, caras del reloj, tarjetas,
  Ajustes y la guía de la primera vez.

```bash
./gradlew :core:test        # pruebas de la lógica
./gradlew assembleRelease   # APK firmado y achicado con R8
```

## Licencia

MIT, igual que el proyecto original (ver `LICENSE`). Las tipografías que vienen incluidas son
SIL OFL 1.1 (ver `licenses/`).

El plan de trabajo original del autor está en `docs/PLAN.md`, en ruso.
