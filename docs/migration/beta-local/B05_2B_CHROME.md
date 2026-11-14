<!-- Aurelis © 2026. GPL-3.0-or-later -->
# B05.2-b — contraste de transporte

Segundo bloque expresamente autorizado junto a B05.2-a. Base668daa36,
misma rama/PR1; ambos commits se envían juntos, sin atribuir CI intermedio.
Máximo30 rutas por bloque. Manifiesto de ocho archivos:
- app/src/main/java/com/music/aurelis/ui/player/PlayerChrome.kt
- app/src/main/java/com/music/aurelis/ui/components/PlayerControls.kt
- app/src/main/java/com/music/aurelis/ui/screens/NowPlayingScreen.kt
- app/src/test/java/com/music/aurelis/ui/PlayerChromeTest.kt
- NOTICE
- docs/migration/beta-local/LICENSE_AUDIT.md
- docs/migration/beta-local/B05_2B_CHROME.md
- docs/migration/beta-local/PROGRESS_TRACKER.md

Excepción autorizada a la base BitChord: acf219a7e4a7f52f026d4336d5c7ac5cf7bfafcc,
sharedUi/src/jvmSharedMain/kotlin/com/music/bitchord/ui/player/PlayerChrome.kt,
SHA256 6dad18b561bf3b8dbf5a61c4c8cbf390883525ac921b158f2980e1b67b376c78.
Se adapta solo additiveChrome y las intensidades blancas0,73 menos0,15 del origen.
No se importa ColorDodge, la infraestructura completa del player ni otras novedades.

La mezcla se limita a anterior/siguiente del transporte, bajo hardware y sin
reduceBlur. Con software/reducir desenfoque se conserva tinta sólida del tema.
El botón central conserva su contenedor/onPrimary de Material. El tint explícito
anterior de skip ignoraba enabled: ahora se atenúa cuando falta capacidad o conexión.
Sin cambiar callbacks, objetivos táctiles, layout, servicio, shaders, ABI o SDK.

Dos regresiones para Actions: contraste calculado de tinta activa/deshabilitada
sobre fondos sólidos claro/oscuro y tinta neutral sobre negro; controles sin
capacidad rechazan comandos y play conserva su callback. La alternativa sin efecto
no crea modifier de dibujo. Estas pruebas no certifican píxeles/GPU ni el contraste
sobre todas las carátulas; no se generan capturas nuevas.

Dos revisiones del mismo agente: fuente exacta del delta, aplicación limitada,
hardware/reduceBlur y estados enabled/capacidad; después API/imports, diff y ocho
rutas, callbacks/targets intactos y atribuciones. git diff --check local; sin
Gradle/Lint/APK/tests Android locales. Las dos entregas suman once rutas distintas
(tres documentos compartidos), sin superar30 ni separarlas para eludir el límite.
Se publican en un push con dos commits; Actions del SHA final validará el conjunto.
Pendiente CI y fidelidad física/GPU; no se declara B05 completo ni se inicia B05.3,
B06 o B09. Tras envío avisar y esperar resultado comunicado por el usuario.
