<!-- Aurelis © 2026. GPL-3.0-or-later -->
# Plan de implementación — cierre UI de B05 y continuación B07–B13

## Instrucción vigente

El usuario aclara que pueden modificarse todos los archivos necesarios y que el
límite es60 archivos distintos POR MENSAJE/ENTREGA. Sustituye para este trabajo la
interpretación de63 rutas acumuladas de toda la corrección. No se necesita ampliar
a64 para incluir NowPlayingScreen.kt. Contar código, tests, recursos, documentación,
configuración, renombres y eliminaciones de la entrega; varios commits del mismo
mensaje no reinician su presupuesto. Objetivo habitual20–35, reservando margen de
corrección hasta60. Publicar inventario real antes de cada entrega.

La primera entrega de evidencia/gates se publicó en18950a40. El usuario autoriza
ahora terminar MiniPlayer y pantalla de reproducción, vertical/horizontal, juntos
dentro de60 archivos. El resto del plan conserva su orden y autorización por entrega.
Planificar B09 no autoriza iniciarlo. Avisar tras cada push y registrar resultados reales.

## Continuación autorizada y forma de entrega

MiniPlayer/reproductor se publicaron y corrigieron en e0fc7cad/0f639624. Base de
esta continuación:0f639624, Main170 SUCCESS B01–B06,429 pruebas por variante;
UI26 conserva fallo de referencia y revisión visual/Redmi pendientes. El usuario
indica «Continúa»: se ejecuta entrega5, cápsulas y textos confirmados del ZIP.
Después del push avisar y esperar SU resultado de GitHub antes de avanzar, sin
vigilar conclusiones ni descargar ZIP de un «Continúa» por iniciativa propia.

## Base y referencia

Aurelis HEAD publicado a50897411f26f43c163100cffd3481f6343b5e5d.
Main167/43788887521: B01–B06 SUCCESS,424 tests por variante y cobertura90,24%.
UI23/43788887503: regresiones nativas PASS API35/API36; comparación FAILURE.
Glass ZIP recibido y validado:18 capturas Aurelis y6 BitChord revisadas,24/36;
faltan referencia player/letras/horizontal y referencia clara. Plain registró33
estados según logs, pero sus imágenes todavía no se han abierto aquí.
No usar REACHED como prueba de fidelidad ni transporte visible.

BitChord local solo lectura /workspace/BitChord, commit fijo
b690dc146009d4815825fa4a20815ef2e4ee8898, no actualizar a main.
Fuentes principales reales:
- app/src/main/java/com/music/bitchord/ui/components/MiniPlayer.kt
- sharedUi/src/jvmSharedMain/kotlin/com/music/bitchord/ui/player/NowPlayingScreen.kt
- sharedUi/src/jvmSharedMain/kotlin/com/music/bitchord/ui/player/PlayerControls.kt
- sharedUi/src/jvmSharedMain/kotlin/com/music/bitchord/ui/player/LandscapePlayer.kt
- sharedUi/src/jvmSharedMain/kotlin/com/music/bitchord/ui/player/PlayerDock.kt
Consultar FILE_MAPPING para dependencias adicionales y registrar adaptación/procedencia.

## Qué se conserva

Nombre Aurelis, com.music.aurelis, un perfil local, servicio AurelisPlaybackService,
un estado coordinado de reproducción y funciones locales reales. Liquid Glass y
sus correcciones de RenderNode. Firma/ABI/SDK/modelos no se alteran para pasar CI.
Mantener atribuciones y licencias aplicables; no copiar módulos TV/Wear/Auto ni
streaming/Cast/WebDAV/SMB/escuchar juntos/JS/servidor compartido.
No reimplementar parsers/temporización ni LRCLIB/traducción ya entregados.
No eliminar funciones de Aurelis para aproximar una captura. Menús/herramientas
locales siguen accesibles aunque se reorganice su ubicación.

## Entregas de cierre B05 y hallazgos UI23, en orden

### 1. Completar evidencia y corregir el criterio de captura (estimación6–12 archivos)

- Obtener plain23 desde Actions; si el proxy sigue bloqueando su host, usar ZIP
  adjuntado por el usuario. Link estable:
  https://github.com/ed0668822/Aurelis-Music/actions/runs/43788887503/artifacts/14192186886
- Comparar código fijo y todas las parejas disponibles: posición, tamaño, iconos,
  materiales, contraste, tipografía autorizada, carátulas y estados locales.
- Reforzar ui_parity.py: expansión exige transporte real visible y correctamente
  habilitado según sesión/capacidades. Comprobar Play/Pause mediante cambio real de
  sesión; Next/Previous con cola de fixtures conocida. Separar abrir panel de operar
  transporte y de aprobar revisión visual. No aceptar solo título/cola/letras.
- Añadir regresión que reproduzca el XML aportado: player abierto sin transporte
  visible debe quedar incompleto, aunque haya título y botón de letras.
- Investigar ADB closed conservando logs del emulador/host y su estado ANTES de
  terminarlo. No recoger dumps de memoria/secretos. Mantener fallos reales y fallos
  de app; recuperar solo el caso de launcher ya estrictamente permitido.
- La causa de ADB sigue sin demostrarse: JIT, memoria posterior y presencia de apport
  no acreditan causa. No cambiar GPU/ABI ni referencia por conjetura.

Rutas previstas: .github/scripts/ui_parity.py, test_ui_parity.py,
.github/workflows/ui-parity-comparison.yml si requiere diagnóstico, registros de
comparación/progreso y pruebas pertinentes. Puede avanzarse revisión estática de
los componentes mientras se obtiene el ZIP; no certificar parejas ausentes.
Aceptación: fixtures distinguen transporte fuera de vista; diagnóstico no oculta
fallos; imágenes y límites de evidencia identificados por SHA/tema/material.

### 2. MiniPlayer completo (estimación14–22 archivos)

- Adaptar dimensiones, posición sobre navegación, carátula/radio, textos, iconos,
  botones, progreso y materiales a la referencia, con datos locales reales.
- Revisar flecha y línea de formato añadidas respecto a la referencia; situar calidad
  en el lugar adecuado sin perder información/accesibilidad de apertura.
- Conectar toque/gestos y transición a player con dock/carátula coordinados.
- Cubrir pausa, carga, error, ausencia de carátula, títulos largos y cola de1 o varias
  canciones. No habilitar acciones que el controlador no soporta.
Rutas candidatas: MiniPlayer.kt, MiniPlayerGestures.kt, PlayerDock.kt,
DockedArtworkOverlay.kt, PlayerSheetMotion.kt, PlayerHost/FullScreenPlayerHost,
MainActivity y AppNavigationHost solo en sus conexiones activas, recursos/tests/docs.
Aceptación: botones accionan la misma sesión; abrir/cerrar/Atrás no cambia canción,
posición ni reproducción; comparación visual dark/light/plain/glass revisada.

### 3. Reproductor vertical completo (estimación16–28 archivos)

- Adaptar distribución completa: cabecera/menús, carátula, título/artista, calidad,
  favorito si existe en referencia, progreso/tiempos, transporte, shuffle/repeat,
  cola y acceso a letras. Adaptar acciones locales conservando funciones existentes.
- Reservar espacio al transporte; dimensionar carátula por alto y ancho disponible.
  Herramientas secundarias no deben desplazar Play/Pause fuera de la vista inicial.
- Conservar opciones de velocidad/temporizador/etc. en entradas accesibles; no
  convertirlas en requisitos de desplazamiento para pausar.
- Textos localizados y accesibilidad, reducción de animación/blur, errores/carga.
Rutas candidatas: ui/screens/NowPlayingScreen.kt, PlayerControls.kt, PlayerSlider.kt,
AnimatedArtwork.kt, ArtworkBackdrop/ArtworkMeshBackdrop, PlayerChrome/PlayerText,
SourceQualityBadge.kt y hosts que realmente lo consumen; recursos/tests/docs.
Aceptación: transporte visible sin scroll en tamaños objetivo y fuente ampliada
(con adaptación accesible); botones/seek/cola funcionan; capturas contrastadas.

### 4. Reproductor horizontal, paneles y continuidad (estimación14–24 archivos)

- Adaptar LandscapePlayer, dock, paneles de cola/letras y jerarquía de controles a
  la referencia con transporte prioritario visible y carátula proporcional.
- Un único estado/controller para MiniPlayer, vertical, horizontal, cola y letras.
- Validar abrir en horizontal, girar con player abierto, recreación, Atrás, retorno,
  cola/posición y reproducción preservados. Separar estas pruebas de capturas
  estáticas; el protocolo actual NO acredita rotación continua ni inicio frío.
Rutas candidatas: NowPlayingScreen.kt, LandscapePlayer.kt, PlayerSidePanels.kt,
PlayerDrawer.kt, PlayerQueue.kt, PlayerHost/FullScreenPlayerHost, QueueScreen y
conexiones existentes; pruebas de navegación/reproducción y recursos/docs.
Aceptación: dos layouts realmente operables; sin transporte recortado ni segunda
sesión, sin perder estado y sin regresión de Glass/RenderNode.

### 5. Resto confirmado del ZIP (estimación12–24 archivos)

- LocalDetailHeader: Play relleno, Shuffle con borde, cápsulas, tamaños y separación
  como referencia, manteniendo callbacks/capacidades.
- LocalMusicContent/SearchScreen y textos implicados: recursos ES/EN, plurales y
  semántica. Resolver mezcla de idioma en el locale configurado.
- Revisar Home/Biblioteca/Ajustes, iconos y barra inferior contra imágenes completas.
  Distinguir errores geométricos de cambios aprobados por perfil/música local.
- Mantener álbumes/listas reales, creación de listas, Ajustes por foto y PhotoPicker.
  No añadir login remoto, importación Spotify ni escuchar juntos.
Rutas: LocalDetailHeader.kt, LocalMusicContent.kt, SearchScreen.kt, Home/Library/
Settings y componentes solo cuando haya brecha reproducida; values/strings.xml,
values-en/strings.xml; pruebas existentes y registros de adaptación.
Aceptación: ningún hallazgo confirmado queda sin resolver o pendiente explícito;
ES/EN coherentes y accesos locales preservados. No declarar igualdad global aún.

### 6. Cierre de B05 y puerta de UI (estimación8–16 archivos)

- Ejecutar B01–B06 para el SHA entregado y revisar suites reales de ambas variantes;
  no confundir continue-on-error/FROM-CACHE con ejecución aprobada.
- Comparar dark/light/plain/glass con estado musical y dispositivo equivalentes;
  completar las36 capturas por material o registrar qué bloquea cada una.
- Revisar imágenes humanamente por pantalla; captura completa no es paridad.
- Probar menús, gestos y transiciones además de PNG; widgets en launcher conservan
  validación específica de B12 y B13, no se cierran por screenshots del player.
- Proporcionar APK ARM64 verificado y prueba en Redmi Note14 Pro5G Android16/HyperOS3:
  Perfil, Biblioteca/foto, galería, listas, Buscar, MiniPlayer, player/rotación/cola,
  con y sin música. La respuesta previa de preferencias NO acreditaba PASS físico.
- Actualizar checklist/progreso/contexto con evidencia, diferencias autorizadas,
  pendientes y rutas realmente cambiadas. B05 no cerrado por compilar.

## Continuación principal después del cierre UI

| Bloque | Implementación que queda | Evidencia de aceptación |
|---|---|---|
|B07|Efectos/foco/blur/pausas; gestos/offset; lectura manual y retorno; selección/composición/compartir letras con URI segura. Conservar parsers y sincronización.|Audio continúa, seek correcto, lectura manual estable, reducción de movimiento y contenido compartido real.|
|B08|Evaluación individual de proveedores y condiciones; habilitar solo los verificados y autorizados. Conservar LRCLIB explícito y traducción HTTPS configurable.|Prioridad local/importado/incrustado/caché; límites/cancelación/respuestas tardías; proveedores no acreditados deshabilitados.|
|B09|DSP y audio avanzado, SIN INICIAR; implementación solo con autorización específica de ese bloque.|Mediciones PCM y rutas reales; hardware donde corresponde; no afirmar bit-perfect/modelos por compilar.|
|B10|AutoMix Beta real: análisis, planificación, modos1/2/3 hilos y transiciones; Balanceado2 por defecto.|Análisis/transiciones medidos, cancelación y recursos controlados; sin simulación.|
|B11|Completar ajustes, Replay y respaldo recuperable según registros.|Datos reales, períodos veraces, restauración válida, errores/cancelación y continuidad.|
|B12|Widgets, metadatos opcionales, Last.fm/ListenBrainz y gestión local de archivos/etiquetas.|Widgets operables en launcher; integraciones opcionales y archivos editados de forma recuperable.|
|B13|Validación final por función, dispositivos reales y entrega verificada.|Registros reales de instalación/reproducción/UI/datos; ABI/firma/checksums/versiones; limitaciones y bloqueos explícitos.|

Conservar plan complementario CI12.3 y revisión ui/audit/components/haptics por
variante donde sigan pendientes. No cerrar retrospectivamente por presencia DEX.
Android Auto/B06.4 excluido por el usuario; no añadirlo como dependencia de B07.
Las filas anteriores se subdividen según PLAN_CONTINUATION, hasta60 por entrega;
ninguna nueva autorización se deriva de planificar los bloques posteriores.

## Protocolo por entrega

1. Leer estado/docs y comparar origen fijo; preservar cambios ajenos.
2. Definir alcance e inventario previsto, pruebas y aceptación; máximo60.
3. Adaptar componentes reales conservando sesión local y licencias/procedencia.
4. Dos revisiones; indicar si las realiza el mismo agente.
5. Local solo lectura/comprobaciones estáticas, Python y git diff --check.
6. Compilar/Lint/APK/tests Android exclusivamente GitHub Actions.
7. Verificar cuenta, correo verificado, permisos, identidad Git local antes de commit.
8. Commit/push normal a feature/bitchord-local-migration, PR1 y título conservados;
   sin main, ramas remotas nuevas, rebase, force push, merge ni Release oficial.
9. Avisar al usuario tras subir; comprobar resultados reales del SHA y corregir
   fallos antes de avanzar. Esperar la autorización del siguiente bloque/entrega.

Los conteos estimados son orientativos e incluyen tests/docs; confirmar inventario
real por entrega. Primera entrega de comparador implementada; CI del nuevo SHA
pendiente al preparar el commit. No se cierra B05 con este cambio de verificación.
