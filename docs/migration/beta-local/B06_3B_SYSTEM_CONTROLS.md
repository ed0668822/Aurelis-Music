<!-- Aurelis © 2026. GPL-3.0-or-later -->
# B06.3-b — foco, auriculares y controles del sistema

Base1a14927f937be762782143bf3c1ba98517007c70 confirmada por «Continúa». Sin descargar
ZIP previo. Misma rama/PR1, autorización B06, máximo30 archivos distintos por entrega.

Manifiesto previo (14): playback/LocalAudioPolicy.kt nuevo, AurelisPlaybackService.kt,
test/playback/LocalAudioPolicyTest.kt nuevo, androidTest/playback/RealSessionResilienceInstrumentedTest.kt,
.github/scripts/b05_case_results.py, b05-emulator-smoke.sh, test_b05_case_results.py,
b06_quality_readiness.py, test_b06_quality_readiness.py, .github/workflows/b05-android-emulator.yml,
NOTICE, LICENSE_AUDIT.md, PROGRESS_TRACKER.md y este documento.

Conservar foco/noisy de Media3 y único servicio/sesión. Extraer configuración común
para poder probarla con ExoPlayer real bajo Robolectric, sin simular hardware.
Emulador: nuevo octavo caso aislado usa token real de notificación para pause/play
y una petición de foco transitoria para observar supresión y recuperación del mismo
Player. No crea sesión adicional ni cambia ABI/SDK/audio sink/DSP.

Android15 declara AUDIO_BECOMING_NOISY protegido (AOSP core/res/AndroidManifest.xml,
tag android-15.0.0_r1, línea319 consultada): no emitirlo desde la app instrumentada ni
modificar permisos del sistema. Robolectric simula entrega del evento y comprueba
el receptor Media3; desconexión física y pantalla bloqueada siguen pendientes.
Marcadores/resultado global exigen los ocho casos; informes no equiparan emulador o
Robolectric con auriculares/lockscreen físicos. Dos revisiones del mismo agente.


## Resultado preparado y límites

LocalAudioPolicy extrae exactamente los atributos MEDIA/MUSIC, foco automático,
noisy y wake mode LOCAL que ya usaba Aurelis. Servicio conserva la intención durante
READY con supresión de foco, pero al quitar tarea deja de retenerlo en IDLE/ENDED
con playWhenReady todavía true, o con error terminal. Sin gestor de foco paralelo.
Notificación/lockscreen continúan siendo los de Media3; no se personaliza ni crea
otra sesión. La instrumentación controla el token Android que la notificación real
publica y exige que el controlador Media3 observe pausa/resume e identidad/cola intactas.
Foco Android15 se solicita desde Activity RESUMED; debe concederse, suprimir playback
transitoriamente y recuperar avance al abandonar la petición, sin prepare/seek extra.

Noisy unitario espera registro asíncrono del receptor real Media3 y bombea su looper;
no usa permiso para emitir broadcast protegido en Android real. No acredita unplug
físico, Bluetooth, visualización/acciones con dispositivo bloqueado ni audio audible.
La prueba Android usa WAV original30s y no exporta capturas nuevas.

CI pasa de siete a ocho invocaciones aisladas; todas se ejecutan aunque una falle.
Los marcadores antiguos se retiran antes de evaluar. Se conserva alias de compatibilidad
p3_all_seven, pero el marcador global exige ahora todos los registrados. El informe
admite evidencia histórica de7 para escenarios antiguos y exige8+identidad exacta,
exit0 y passed explícito para notificación/foco. Datos ausentes/malformados/otro método
no verifican nada. Escenarios físicos conservan NOT_VERIFIED y sin Release oficial.

## Dos revisiones y validación

Dos revisiones estáticas del mismo agente: primero propietario/foco/cancelación/
permisos/protección del broadcast; después firmas SDK/Media3/Robolectric, cleanup,
fixtures/selectores/identidad de evidencia y catorce rutas. Pasa git diff --check,
bash -n, parseo YAML y compilación sintáctica del Python embebido (sin ejecutarlo).
18 pruebas Python de test_b05_case_results/test_b06_quality_readiness aprobadas.
Dos nuevos casos Python comprueban falta del octavo caso y evidencia falsa/malformada.
Tres casos Android nuevos preparados para Actions: dos Robolectric, uno instrumentado.
No se ejecutaron Gradle/Lint/APK/tests Android localmente. CI nuevo y hardware pendientes.
B06.3-c/-d y B06.4 siguen pendientes; esta entrega no cierra B06 globalmente.


## Reparación CI139 — anotación de test

Run43658748680, commit92e82bf4dddcc9815cc1ae71d958f46c0ef2937c: B01/B03/B05 SUCCESS,
B02/B06 FAILURE, B04 SKIPPED. ZIP oficial de logs descargado y verificado; ZIP de
informes bloqueado en productionresultssa7. Lint identifica UseSdkSuppress en
RealSessionResilienceInstrumentedTest.kt:262: no usar RequiresApi en tests, usar
SdkSuppress. El método ya tenía SdkSuppress(minSdkVersion=26); se retira únicamente
RequiresApi redundante. Se conserva test completo, restricción de ejecución y guard
SDK33 del acceso Parcelable. No se cambia SDK, permisos, lógica ni puertas Lint/CI.

Tres archivos ya incluidos (test, este informe y tracker); acumulado14 de máximo30.
Dos revisiones del mismo agente: diagnóstico/alcance de anotaciones, después diff y
conservación del selector/cuerpo/restricción de API. git diff --check correcto.
Sin Gradle/Lint/Android locales. Nueva validación completa pendiente de Actions;
no transferir el éxito B03/B05 de92e82bf4 al SHA de reparación sin evidencia.
