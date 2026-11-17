<!-- Aurelis © 2026. GPL-3.0-or-later -->
# Inventario funcional — bloques de 40/10 archivos

Organización solicitada el 2026-11-17 a partir del inventario F001–F200 que
proporcionó el usuario. Es planificación documental: no se ha vuelto a auditar
el código ni se han implementado funciones para preparar esta distribución.
Estos IDs no sustituyen B00–B13 ni las fases de Actions B01–B06.

## Límites y forma de contar

| Dificultad | Máximo de archivos distintos por bloque | Presupuesto orientativo |
|---|---:|---|
| Fácil | 40 | Hasta30 de implementación,6 de pruebas y4 de documentación/avisos |
| Media | 40 | Hasta30 de implementación,6 de pruebas y4 de documentación/avisos |
| Difícil | 10 | Hasta6 de implementación,2 de pruebas y2 de documentación/avisos |

El presupuesto es una reserva ajustable dentro del máximo; no exige modificar
esa cantidad. Código, recursos, configuración, pruebas, documentos, avisos,
eliminaciones y renombres cuentan. Los renombres se contabilizan por ambas rutas
para evitar ambigüedad. Una ruta repetida en varios commits del mismo bloque
cuenta una vez; todas las correcciones de ese bloque forman parte del acumulado.
No se reinicia el contador al fallar CI ni se cambia el nombre a una reparación
para eludir el máximo. La clasificación no se rebaja para disponer de más archivos.

Los límites40/10 rigen los próximos bloques; los60 de registros anteriores son
históricos como límite por bloque. Para una entrega conjunta se conserva además
el techo anterior de60 rutas distintas: dos bloques medios no autorizan80.
No existe obligación de agrupar dos bloques en un mensaje.

Antes de implementar se prepara una lista de rutas y criterios de aceptación.
Las cantidades de esta tabla son **topes**, no recuentos de cambios ya medidos.
Si una fila no cabe, se divide previamente en subbloques `.a`, `.b`, etc., con
listas y resultados verificables separados, conservando su ID funcional padre.
No se divide retrospectivamente una entrega que excedió el presupuesto.
Un subbloque debe dejar una integración coherente; no se activa una función
incompleta ni se declara terminada la función padre hasta verificarla completa.

## Estado y autorización

- Base de la organización original: Aurelis `9d203d13dc86bfddf90ac64dd39ee2e679ef1cdb`.
- CI183, run43970204682: B01–B06 SUCCESS, comprobado antes de esta planificación.
- F001 entregado:1 archivo documental. F002 entregado:18 rutas acumuladas,
  incluidas reparaciones. No se vuelven a implementar por reorganizar el plan.
- F003 se implementa en la entrega documentada en [F003_TOP_BAR](F003_TOP_BAR.md),
  con21 rutas acumuladas incluida la mejora de pruebas autorizada después de
  CI185 y la reparación de dependencias B02 trasCI191; CI192 global SUCCESS para3a015702.
  La mejora B03 ocupa10/10 y aprobó CI191. La reparación específica B02 ocupa5/10.
- F004 y F005–F006 entregados. CI197, run44034859111, SHA44d2fb5d,
  SUCCESS en las seis fases B01–B06. Corrige los timeouts de195 y el cierre de
  artefactos al reintentar196. La igualdad visual completa y el físico siguen pendientes.
- Entrega autorizada ahora: F007 y F008, base44d2fb5d. Recomendaciones locales
  con presentación Top Picks y Explorar con carátulas duotono de géneros reales.
  F00712/40, F00811/40,17 rutas distintas conjuntas contando avisos, pruebas y
  recursos compartidos. Nuevo CI PENDIENTE. No se inicia F009 ni B09.
- Las demás filas quedan pendientes de decisión individual. Una fila que reúne
  varias funciones solo se ejecuta cuando todas ellas estén autorizadas; si no,
  se limita a las aprobadas. Esta organización no aprueba el inventario completo.
- B09 no se inicia por aparecer en la tabla. DSP, salida exclusiva y mezcla
  requieren autorización específica y sus dependencias reales.

## Inicio, navegación y búsqueda

| Bloque / funciones | Alcance | Dificultad | Máximo | Estado |
|---|---|---|---:|---|
| F001 | Entradas, Atrás y restauración | Fácil | 40 | Entregado; conservar |
| F002 | Barra inferior y Buscar separado | Media | 40 | Entregado; conservar |
| F003 | Cabeceras, foto local y acceso a ajustes | Media | 40 | Entregado; CI192 SUCCESS |
| F004 | Bienvenida, nombre, permiso y finalización persistida | Difícil | 10 | Entregado; CI197 SUCCESS |
| F005–F006 | Cabecera, destacados, carruseles y estanterías de Inicio | Media | 40 | Entregado; CI197 SUCCESS |
| F007 | Presentación de recomendaciones locales existentes | Media | 40 | Implementado; CI nuevo pendiente |
| F008 | Explorar por géneros locales | Media | 40 | Implementado; CI nuevo pendiente |
| F009 | Búsqueda y filtros locales | Media | 40 | Pendiente |
| F010 | Conservar historial de búsqueda y sus acciones | Fácil | 40 | Pendiente |
| F011 | Resultado destacado y sugerencias locales | Media | 40 | Pendiente |
| F012 | Actualización, carga y recuperación del escaneo | Difícil | 10 | Pendiente |
| F013 | Conservar estados vacíos, errores y reintentos | Fácil | 40 | Pendiente |
| F014 | Teclado, foco, Atrás e insets | Difícil | 10 | Pendiente |

F003 conserva el recorrido foto→ajustes y la captura aislada del cuerpo.
F004 utiliza el perfil único existente, conserva nombres/datos y trata rechazo,
cancelación, recreación y perfil en solo lectura. No incorpora Google ni otro
repositorio de perfiles. Si su integración supera10 rutas, se separarán antes
de implementar la presentación y la conexión persistida, sin activar un flujo
incompleto. La subdivisión y las listas de esta entrega están al final. F004 no
rediseña Inicio o Buscar; F005–F006 adapta únicamente el Inicio aprobado.

## Biblioteca local

| Bloque / funciones | Alcance | Dificultad | Máximo |
|---|---|---|---:|
| F015–F017 | Conservar catálogo, metadatos y observación existentes | Difícil | 10 |
| F018 | Carpetas seleccionadas y permisos persistidos | Difícil | 10 |
| F019 | Conservar agrupaciones por álbum/artista | Fácil | 40 |
| F020 | Pestañas y vistas de Música local | Media | 40 |
| F021 | Conservar géneros reales y desconocidos | Fácil | 40 |
| F022 | Página principal de Biblioteca | Media | 40 |
| F023–F024 | Presentación local de álbum y artista | Media | 40 |
| F025–F027 | Carpetas, selección múltiple y ordenación | Media | 40 |
| F028 | Revocación de permisos y medios inaccesibles | Difícil | 10 |
| F029 | Menús de colecciones con acciones reales | Media | 40 |

## MiniPlayer y reproductor

| Bloque / funciones | Alcance | Dificultad | Máximo |
|---|---|---|---:|
| F030 | Presentación del MiniPlayer | Media | 40 |
| F031 | Conservar gestos, eje y cancelación | Difícil | 10 |
| F032 | Calidad real e información accesible en MiniPlayer | Media | 40 |
| F033 | Reproductor vertical sobre el servicio existente | Difícil | 10 |
| F034 | Disposición horizontal y continuidad | Difícil | 10 |
| F035 | Expansión y retorno de portada | Difícil | 10 |
| F036 | Conservar permanencia al cambiar de canción | Difícil | 10 |
| F037–F039 | Título, créditos, favorito y menú local | Media | 40 |
| F040 | Progreso y gesto de seek | Difícil | 10 |
| F041–F042 | Transporte y volumen existentes | Media | 40 |
| F043 | Verso sincronizado sobre el progreso | Difícil | 10 |
| F044–F045 | Modos de reproducción y contexto local de origen | Media | 40 |
| F046 | Cola como panel y continuidad de navegación | Difícil | 10 |
| F047 | Selector de salida conectado a ruta efectiva | Difícil | 10 |
| F048–F049 | Información técnica y dispositivo efectivo | Difícil | 10 |

## Animaciones, componentes y recursos

| Bloque / funciones | Alcance | Dificultad | Máximo |
|---|---|---|---:|
| F050 | Paleta de portada y respuestas antiguas | Difícil | 10 |
| F051 | Fondo mesh y coste gráfico | Difícil | 10 |
| F052 | Conservar transición y retención de carátulas | Difícil | 10 |
| F053 | Conservar Liquid Glass y aislamiento del backdrop | Difícil | 10 |
| F054 | Menús mantenidos, captura, elevación y retorno | Difícil | 10 |
| F055–F057 | Iconos, botones y tipografías autorizadas | Media | 40 |
| F058 | Conservar política y fallbacks hápticos | Difícil | 10 |
| F059–F060 | Switches, segmentos y placeholders reales | Media | 40 |
| F061 | Desvanecidos y blur progresivo | Difícil | 10 |
| F062 | Overscroll y coordinación de gestos | Difícil | 10 |
| F063 | Barras del sistema y contraste | Difícil | 10 |
| F064 | Pantallas grandes, orientación y recreación | Difícil | 10 |
| F065 | Conservar movimiento reducido en consumidores | Difícil | 10 |

## Letras, proveedores, traducción y compartir

| Bloque / funciones | Alcance | Dificultad | Máximo |
|---|---|---|---:|
| F066–F067 | Conservar importación e incrustadas | Difícil | 10 |
| F068–F070 | Conservar parsers de texto, LRC y TTML | Difícil | 10 |
| F071–F072 | Reloj, seek, offset, voces y dirección | Difícil | 10 |
| F073 | Anticipación y desplazamiento del verso | Difícil | 10 |
| F074 | Barrido y efectos de notas sostenidas | Difícil | 10 |
| F075 | Pausas instrumentales y temporización | Difícil | 10 |
| F076 | Lectura manual y reanudación de seguimiento | Difícil | 10 |
| F077 | Pantalla y controles de letras | Difícil | 10 |
| F078 | Efectos de traducción y cancelación | Difícil | 10 |
| F079 | Romanización con servicio permitido | Difícil | 10 |
| F080 | Conservar multiselección por ocurrencia | Difícil | 10 |
| F081 | Presentación de tarjeta para compartir | Media | 40 |
| F082–F083 | Conservar registro de proveedores y LRCLIB | Difícil | 10 |
| F084 | BetterLyrics: condiciones antes de activación | Difícil | 10 |
| F085 | BiniLyrics: condiciones antes de activación | Difícil | 10 |
| F086 | lrc.red: condiciones antes de activación | Difícil | 10 |
| F087 | LyricsPlus: condiciones y contrato | Difícil | 10 |
| F088 | PaxSenix: condiciones y credencial | Difícil | 10 |
| F089 | Unison: condiciones y adaptación | Difícil | 10 |
| F090 | Megalobiz: condiciones, parsing y límites | Difícil | 10 |
| F091 | KuGou: condiciones y matching | Difícil | 10 |
| F092 | Musixmatch: acceso y derechos de contenido | Difícil | 10 |
| F093 | Genius: condiciones y formato real | Difícil | 10 |
| F095–F096 | Conservar traducción HTTPS, caché y cancelación | Difícil | 10 |

F094 permanece excluido. Ninguna fila de proveedor equivale a permiso de
activarlo: se respetan disponibilidad, condiciones y aprobación específica.

## Reproducción, DSP y AutoMix

| Bloque / funciones | Alcance | Dificultad | Máximo |
|---|---|---|---:|
| F097–F098 | Conservar servicio, sesión y controlador únicos | Difícil | 10 |
| F099–F100 | Conservar foco, segundo plano y restauración | Difícil | 10 |
| F101–F103 | Velocidad, temporizador y autoplay local | Difícil | 10 |
| F104 | Crossfade audible y handoff coordinado | Difícil | 10 |
| F105 | Conservar ecualización DSP existente | Difícil | 10 |
| F106 | Interfaz original sobre filtros existentes | Difícil | 10 |
| F107 | Normalización real de sonoridad | Difícil | 10 |
| F108–F109 | Conservar espacialización y puente PCM | Difícil | 10 |
| F110–F111 | Conservar decodificación común y DSD/DST | Difícil | 10 |
| F112 | Negociación efectiva de salida | Difícil | 10 |
| F113 | Salida exclusiva bit-perfect | Difícil | 10 |
| F114 | Transporte USB directo y volumen del DAC | Difícil | 10 |
| F115 | Seguimiento efectivo de Bluetooth/rutas | Difícil | 10 |
| F116 | Análisis real de ritmo | Difícil | 10 |
| F117 | Análisis real de voz | Difícil | 10 |
| F118 | Pipeline cancelable y caché de análisis | Difícil | 10 |
| F119 | Planificación real de transiciones | Difícil | 10 |
| F120 | Mezcla, estiramiento y LoopMix | Difícil | 10 |
| F121 | Modos efectivos1/2/3; Balanceado=2 | Difícil | 10 |
| F122 | Vista de mezcla alimentada por análisis real | Difícil | 10 |
| F123 | Alineación de versiones locales | Difícil | 10 |

El orden del inventario se conserva. Una dependencia no aprobada deja el bloque
en espera, no autoriza adelantarla. F104 y F112–F123 pueden requerir varios
subbloques de10 antes de quedar funcionales. No se modifica SDK, ABI, firma o
secretos para pasar CI; ni la presencia de clases/modelos acredita funcionamiento.

## Playlists, favoritos y cola

| Bloque / funciones | Alcance | Dificultad | Máximo |
|---|---|---|---:|
| F124 | Conservar favoritos y su propietario | Fácil | 40 |
| F125–F126 | Diálogos y acciones de playlists existentes | Media | 40 |
| F127 | Reordenación persistida de playlists | Difícil | 10 |
| F128–F129 | Fijación, orden de Biblioteca y selector | Media | 40 |
| F130–F131 | Conservar Play Next y secciones de cola | Difícil | 10 |
| F132 | Arrastre y autoscroll de cola | Difícil | 10 |
| F133–F134 | Conservar limpieza e identidad por ocurrencia | Difícil | 10 |
| F135–F136 | Conservar adaptadores, ventanas y continuación | Difícil | 10 |

## Ajustes, perfil y personalización

| Bloque / funciones | Alcance | Dificultad | Máximo |
|---|---|---|---:|
| F137 | Jerarquía y orden de ajustes compatibles | Media | 40 |
| F138–F139 | Conservar búsqueda en ajustes y perfil único | Fácil | 40 |
| F140 | Conservar Photo Picker, copia privada y cancelación | Difícil | 10 |
| F141–F143 | Tema, fuente y opciones con consumidor real | Media | 40 |
| F144 | Filas de AutoMix/calidad con destinos reales | Difícil | 10 |
| F145 | Rendimiento y frecuencia de pantalla | Difícil | 10 |
| F146 | Idiomas, literales y plurales de la UI | Media | 40 |
| F147 | Conservar recuperación de preferencias inválidas | Difícil | 10 |
| F148–F149 | Almacenamiento conocido, versión y enlaces Aurelis | Fácil | 40 |

## Estadísticas, Replay, conexiones y widgets

| Bloque / funciones | Alcance | Dificultad | Máximo |
|---|---|---|---:|
| F150–F151 | Conservar registro de escucha y eventos recuperables | Difícil | 10 |
| F152 | Presentación de minutos y tarjetas de Biblioteca | Media | 40 |
| F153 | Conservar apertura de Replay y aislamiento Glass | Difícil | 10 |
| F154 | Períodos calculados con eventos disponibles | Difícil | 10 |
| F155 | Presentación del ranking de canciones existente | Media | 40 |
| F156 | Rankings de artistas, álbumes y géneros | Difícil | 10 |
| F157 | Agregaciones de hábitos y zona horaria | Difícil | 10 |
| F158 | Historias, póster y compartir resúmenes reales | Media | 40 |
| F159 | Coherencia de historial, borrado y eventos | Difícil | 10 |
| F160 | Fecha local real de incorporación | Difícil | 10 |
| F161 | Exportación, restauración y recuperación de respaldo | Difícil | 10 |
| F162 | Scrobbling opcional y envío recuperable | Difícil | 10 |
| F163–F164 | Presentación de widgets cuadrado, ancho y cápsula | Media | 40 |
| F165 | Conservar resistencia y acciones de widgets | Difícil | 10 |

## Metadatos y gestión de archivos

| Bloque / funciones | Alcance | Dificultad | Máximo |
|---|---|---|---:|
| F166 | Presentación de información local real | Media | 40 |
| F167 | Carátulas opcionales, matching y caché | Difícil | 10 |
| F168 | Fotos/datos opcionales de artistas y procedencia | Difícil | 10 |
| F169 | Escritura verificable de etiquetas | Difícil | 10 |
| F170 | Escritura y conservación por contenedor | Difícil | 10 |
| F171 | Guardar letras dentro del audio | Difícil | 10 |
| F172 | Copiar, sustituir o eliminar música con recuperación | Difícil | 10 |
| F173 | Compartir audio/información mediante URI limitada | Difícil | 10 |
| F174 | Conservar actualización desde Aurelis | Difícil | 10 |

F170 se subdivide por formato antes de implementar. No se modifica el audio
original para probar una adaptación visual ni se confunde importar letras a la
app con escribirlas dentro del archivo.

## Exclusiones y aplazamientos: sin bloques de implementación

| Funciones | Estado que se mantiene |
|---|---|
| F094 | SimpMusic/letras dependientes de YouTube: excluido |
| F175–F186 | Streaming/autenticación, fuentes JS, red compartida, Cast, TV, Wear, Auto y web: excluidos |
| F187 | Discord: aplazado |
| F188 | Carátulas animadas externas: aplazado |
| F189 | Videoclips: aplazado |
| F190–F192 | Firmas de artistas, selector de icono y Dolby visible: excluidos/retirados |

No tienen presupuesto de implementación ni se activan por estar inventariados.
Solo se conserva la exclusión; una nueva autorización explícita requeriría su
propio plan y clasificación antes de editar.

## Infraestructura y contratos

| Bloque / funciones | Alcance | Dificultad | Máximo |
|---|---|---|---:|
| F193 | Conservar plataforma Gradle compatible | Difícil | 10 |
| F194 | Contratos JNI, procedencia y carga de modelos | Difícil | 10 |
| F195 | Conservar secuencia y evidencia B01–B06 | Difícil | 10 |
| F196 | Capturas y comparación UI con evidencia válida | Difícil | 10 |
| F197 | Clasificar y corregir comentarios/documentos legacy | Media | 40 |
| F198 | Mantener licencias y procedencia por componente | Media | 40 |
| F199 | Conservar HTTPS, límites, cancelación y modo local | Difícil | 10 |
| F200 | Adaptadores UI/modelos sin duplicar propietarios | Difícil | 10 |

Las tareas transversales de licencia, integración y regresión necesarias para
una función se cuentan en su bloque desde el principio; no se aplazan a estas
filas para aparentar que el bloque principal utiliza menos archivos.

## Entrega y verificación

1. Confirmar autorización, lista de rutas, dependencias y máximo de la fila.
2. Conservar implementación funcional, fuente BitChord fijada y atribuciones.
3. Realizar dos revisiones, indicando si las hace el mismo agente.
4. Localmente: lectura, sintaxis, diff --check y pruebas Python pertinentes.
   Gradle, Android Lint, pruebas Android y APK exclusivamente en GitHub Actions.
5. Entregar solo en feature/bitchord-local-migration y PR1; sin force push,
   rebase, merge automático, otra rama remota ni Release oficial.
6. Avisar al enviar archivos, informar SHA y resultado real y esperar al usuario
   antes de avanzar. Si falla, corregir el mismo bloque dentro de su acumulado.

La validación física en Redmi Note14 Pro5G, Android16/HyperOS3, sigue pendiente.
Una compilación o captura software no acredita fidelidad visual ni audio real.

## Entrega F004 — bienvenida con perfil local

Autorizada junto al bloque siguiente mediante «Continúa con los siguientes dos
bloques». Base3a01570239c384d9808d813d8c56e13ccfc16d9d, CI192 SUCCESS en
B01–B06 y CodeQL. No se hereda ese verde para el código nuevo.

Se reutilizan NamePage/NameField, cápsulas56dp, título40/44sp, campo64dp,
transición horizontal420ms y salida vertical560ms del origen fijado. Se conservan
icono y paletas Aurelis existentes. La segunda página solicita permiso de música
local y permite omitirlo; no hay Google, conexión remota obligatoria o perfiles
adicionales. El límite del nombre sigue los60 UTF-16 del propietario local,
sin cortar un par sustituto; la fuente fijada limitaba su nombre de fiesta a40.

LocalOnboardingGate vive junto a la pantalla y recibe app_meta del arranque,
no crea otro ProfileRepository. El nombre se guarda con ProfileRepository.rename
al confirmar la primera página, como el patrón original; un nombre sin cambios
no se reescribe. El perfil de solo lectura permite seguir conservando sus datos.
No se serializan canciones, servicios, callbacks o repositorios en el estado UI.

La finalización usa local_onboarding_complete en metadatos de esta instalación.
Solo descubre el contenido tras un commit confirmado fuera del hilo UI. Un fallo
restaura la bandera en memoria, conserva los demás datos y ofrece reintento.
La recreación preserva borrador/página; futuras aperturas omiten la bienvenida
tras completar. Instalar esta función sobre una versión sin bandera muestra la
bienvenida una vez, con el nombre actual prellenado y sin crear otro perfil.

Antes de terminar no se compone la navegación ni sus modales, para que no roben
foco, accesibilidad o Atrás. El navegador recordado, conexión de reproducción y
ciclo de biblioteca permanecen en su propietario existente; una intención de
abrir el reproductor conserva su destino pendiente. El permiso deja de abrirse
automáticamente al primer frame y se pide desde la bienvenida o recuperación.
Se actualiza al volver del diálogo/ajustes de Android. La pantalla respeta IME,
texto grande, scroll en ventanas cortas y movimiento reducido. El controlador
SystemBarIcons existente muestra iconos blancos en bienvenida y restaura el
contraste del tema al terminar; no se añade un gestor de ventanas paralelo.

### Subdivisión previa a ampliar el ámbito

La lista inicial de10 rutas no incluía el capturador comparativo. Antes de editar
esas rutas adicionales o crear commits, se detectó que su reset dejaría la nueva
bienvenida bloqueando capturas de Inicio. Se divide la función antes de superar
su presupuesto, conforme a la regla inicial del plan. No es una división de
correcciones para reiniciar contadores.

F004.a — presentación y persistencia,8 rutas de10:

- app/src/main/java/com/music/aurelis/ui/onboarding/OnboardingScreen.kt
- app/src/test/java/com/music/aurelis/ui/OnboardingTest.kt
- app/src/main/res/values/strings.xml
- app/src/main/res/values-en/strings.xml
- NOTICE
- docs/migration/beta-local/LICENSE_AUDIT.md
- docs/migration/beta-local/FUNCTION_BLOCKS_40_10.md
- docs/migration/beta-local/PROGRESS_TRACKER.md

F004.b — conexión activa y harness,9 rutas acumuladas de10 con la reparación CI195:

- app/src/main/java/com/music/aurelis/MainActivity.kt
- app/src/androidTest/java/com/music/aurelis/playback/RealSettingsNavigationInstrumentedTest.kt
- .github/scripts/ui_parity.py
- .github/scripts/test_ui_parity.py
- .github/scripts/b05-emulator-smoke.sh
- Los mismos cuatro registros/avisos de F004.a, contados también en este subbloque.

El padre F004 ocupa13 rutas distintas, declaradas expresamente; cada subbloque
mantiene su acumulado independiente para sus futuras correcciones. La primera
parte queda compilable pero sin activación hasta conectar la segunda. La prueba
instrumentada existente atraviesa la bienvenida real, comprueba la bandera y
conservación del perfil y sigue con ajustes/recreación/reproducción. El capturador
solo prepara la bandera en el emulador desechable de comparación, igual que ya
omitía la bienvenida de BitChord. Su informe declara esa preparación y la prueba
nativa no llama a seed_preferences. No se modifica el APK de referencia.

Se añaden10 métodos Robolectric para SDK33/35: guardar/omitir permiso; restaurar
borrador y página; nombre vacío/límite/Unicode; fallo al guardar perfil; perfil de
solo lectura; fallo y reintento al finalizar; persistencia ya completada; rollback
de memoria tras fallo de disco; bandera inválida; texto grande/ventana corta.
La comprobación de iconos del sistema forma parte del recorrido de finalización.
El emulador API36 ejercitará la entrada activa, no una pantalla aislada.

## Entrega F005–F006 — presentación de Inicio y carruseles

Se conservan la cabecera/levantamiento28dp y el título pequeño oculto del origen,
gutter16dp, estanterías150dp, recents de cuatro filas, las listas locales y sus
índices/colas, pulsación mantenida, favoritos, playlists y AutoMix existente.
No se inicia análisis musical, DSP, streaming o otra función pendiente.

La tarjeta principal pasa de60%/280dp y proporción0,75 a heroCardWidth original:
70%, techo320dp, proporción0,92, esquinas18dp y scrim/caption con padding16/34/14dp.
Se adapta la función HeroCard real: texto titleLarge, artista a dos líneas y
barras centrales solo en reproducción. El gran botón superpuesto ajeno a esa
variante desaparece; el toque y la pulsación mantenida mantienen sus callbacks.
La variante legacy no vertical de HomeHero conserva su implementación anterior.
El contexto local Escuchar/Continuar permanece en la semántica de la tarjeta.

ShelfRow incorpora el código original como una sobrecarga compatible. Todos los
carruseles Home lo usan con sus claves/listas previas y rememberSnapFlingBehavior;
no se elimina el ShelfRow legacy de filas ni su sobrecarga GlassNavBar. Se leyó
el JAR fuente oficial de foundation1.7.6 sin compilar: su posición por defecto es
Center, por lo que la prueba no asume que el comentario upstream de alineación
inicial equivale a un SnapPosition.Start que el código nunca solicita.

Las tarjetas de artista usan la geometría cuadrada150dp del ShelfCard original
y un contador local pluralizado; se retira el literal español del consumidor.
El título de canción mantiene la tinta del original, con estado de reproducción
en las barras existentes. Se conserva el banner local funcional y sus dos
destinos, en lugar del login remoto; se ajusta su padding inferior a18dp. Su mesh
estable y los motores de imagen/Glass no se sustituyen. No se añaden botones
Mostrar todo a Home: la referencia los reserva a los consumidores de Biblioteca.

Lista acumulada de11 rutas (máximo40), incluidos avisos, pruebas y reparación CI193:

- app/src/main/java/com/music/aurelis/ui/screens/HomeScreen.kt
- app/src/main/java/com/music/aurelis/ui/components/home/HomeHero.kt
- app/src/main/java/com/music/aurelis/ui/components/home/HomeCarousels.kt
- app/src/main/java/com/music/aurelis/ui/components/ShelfRow.kt
- app/src/test/java/com/music/aurelis/ui/HomeShelvesIntegrationTest.kt
- app/src/test/java/com/music/aurelis/ui/HomePresentationTest.kt
- app/src/test/java/com/music/aurelis/ui/HomeLocalTest.kt
- NOTICE
- docs/migration/beta-local/LICENSE_AUDIT.md
- docs/migration/beta-local/FUNCTION_BLOCKS_40_10.md
- docs/migration/beta-local/PROGRESS_TRACKER.md

Se añaden cinco métodos de presentación (ancho/proporción/tap/hold en teléfono,
techo en ventana ancha, encaje tras fling, RTL/click y artista/contador), y uno de
integración que abre el reproductor actual sin sustituir su cola ni posición.
Todos se destinan a SDK33/35. Las pruebas anteriores no se eliminan.

## Evidencia de esta entrega conjunta y procedencia

Actualización CI196, SHA8da645ba, intento2: B01–B05 SUCCESS;506/506 Debug,
506/506 Release, ambos recorridos de navegación API36 y8/8 casos de audio API35
PASS. Los tres timeouts195 quedan superados; el primer intento196 no los probó
porque Maven Central respondió429 en configuración. El reintento conserva el SHA.
La prueba física y la fidelidad visual completa siguen pendientes.

B06 consolidó/subió informes y aprobó su puerta de evidencia; falló su limpieza
por dos nombres duplicados (b01-report-196 y Aurelis-Informes-B01-B06-196) de
los dos intentos. Se mantiene la validación estricta del limpiador. La corrección
configura overwrite:true en las12 subidas de B01–B06 para reemplazar únicamente
artefactos homónimos de la misma ejecución. No se añaden sufijos por intento,
se cambian nombres entregables ni se mezclan evidencias de distintas ejecuciones.
La acción fijada upload-artifact@b7c566a confirma soporte de esta opción.

Ámbito de infraestructura fijado antes de editar,9 rutas de10:

- .github/workflows/b01-compile-debug.yml
- .github/workflows/b02-android-lint.yml
- .github/workflows/b03-unit-tests.yml
- .github/workflows/b04-verify-apks.yml
- .github/workflows/b05-android-emulator.yml
- .github/workflows/b06-unified-pipeline.yml
- .github/scripts/test_b06_delivery_contract.py
- docs/migration/beta-local/FUNCTION_BLOCKS_40_10.md
- docs/migration/beta-local/PROGRESS_TRACKER.md

Los ámbitos funcionales no se reinician ni amplían: F004.a8/10, F004.b9/10,
F005–F00611/40. Sumando la corrección de infraestructura y documentos compartidos
hay27 rutas propias distintas,30 con los tres iconos externos conservados.
Una regresión simula dos intentos desde los seis workflows reales y ejecuta el
limpiador original. Sin overwrite reproduce el rechazo anterior; con él conserva
solo los tres artefactos finales recientes.15 casos de contrato y9 de grafo CI
PASS, actionlint y diff --check PASS. Dos revisiones del mismo agente; nueva
validación global pendiente. No se inicia F007 ni se publica una Release oficial.

Actualización CI195 (ZIP original del usuario,311 entradas válidas): B01 y B02
SUCCESS;506 pruebas Debug y506 Release PASS, reparaciones CI193 validadas.
El primer recorrido Android16 agota150s y el segundo PASS; B05 tiene6/8 PASS,
local y continuous agotan125s (exit124). Continuous sí registra12s de audio,
seek, reanudación y stop final, antes de quedar sin resultado JUnit. No se
demuestra un error de audio ni un deadlock nativo con estas trazas.

Se verificó el fuente oficial ActivityScenario1.6.1: launch/onActivity/transición
y cierre esperan waitForIdleSync. La bienvenida animada aún estaba activa en
la preparación de estos tests. El recorrido de ajustes ahora completa la
bienvenida real por Intent/accesibilidad antes de entrar a ActivityScenario,
verificando perfil y persistencia; mantiene todas las recreaciones y recorridos.
El fixture de audio B05 completa solo la bandera de bienvenida mientras la app
está detenida, preserva las demás claves y exige readback booleano exacto.
No prueba ni certifica la UI de bienvenida: esa cobertura permanece en B03.
No se modifica producción, Glass, movimiento, audio focus o los límites de tiempo.
Si otro problema persiste, los casos fallidos imprimen sus logs y etapas reales.

Cinco regresiones Python nuevas verifican que el fixture no borra metadatos y
falla ante errores de dispositivo, XML o readback.86 fixtures Python PASS;
Android nuevo pendiente exclusivamente de Actions. Dos revisiones del mismo
agente. Esta reparación usa6 rutas; solo b05-emulator-smoke.sh es nueva frente
al acumulado anterior. F004.a8/10, F004.b9/10, F005–F00611/40,20 propias y23
con iconos externos. No se reinician contadores ni se inicia F007.

Actualización CI193: B01 compiló correctamente. Debug y Release ejecutaron506
casos cada uno:503 aprobados y3 fallidos. HomeLocalTest buscaba el rótulo y botón
legacy retirados de la variante portrait aprobada; se actualiza para comprobar
la semántica Continuar y tocar la tarjeta, desplazando realmente la lista y el
carrusel para acceder a Favoritos y AutoMix. Los SDK33/35 de la prueba de retorno
al reproductor exigían12345ms exactos después de reanudar, pero el reloj avanzó
a12354ms. Ahora se comprueba12345ms mientras está pausado y, tras reanudar,
igualdad de cola y de contadores reales de reemplazo/prepare/seek, además de
posición no retrocedida y playWhenReady. No se cambia el reproductor ni se
permite una tolerancia arbitraria o un reinicio encubierto de la reproducción.

El push externo8f7e1b1a de tres iconos inició194 y canceló193 por la concurrencia
existente. B02/B03/B05 quedaron CANCELLED, B04 SKIPPED y B06 FAILURE; no se
atribuyen a Lint/emulador errores no demostrados. Se conservan los tres iconos.
La reparación ocupa cuatro rutas (dos pruebas y estos dos registros), una nueva
respecto de la entrega propia:19 acumuladas, F005–F00611/40, F004.a8/10 y
F004.b8/10. El intervalo completo con cambios externos contiene22 rutas.
Dos revisiones estáticas del mismo agente; Android del nuevo SHA queda pendiente
exclusivamente de Actions. No se inicia F007.

Evidencia histórica de preparación anterior a CI193:

Son18 rutas distintas en conjunto (máximo60), con los cuatro avisos/registros
compartidos contados en cada subbloque.16 métodos Android nuevos, pendientes de
Actions; no se ejecuta Gradle, Lint ni Android localmente. Validación local:
parser de10 Kotlin, XML/recursos, diff --check y fixtures Python del capturador.
Pasaron74 pruebas Python del capturador y sus informes (57+17), actionlint de
los workflows que consumen esas evidencias, XML sin claves duplicadas y sintaxis
de los diez Kotlin. No se ha ejecutado todavía ninguno de los16 métodos Android.
El parser verifica sintaxis, no tipos/compilación. Se verificaron estáticamente
las APIs Compose1.7.6 usadas (autoCorrectEnabled y snap Center).

Dos revisiones del mismo agente: (1) flujo, persistencia/rollback, permisos,
readOnly, ciclo de vida y conservación de propietarios; (2) procedencia/APIs,
geometría, gestos, índices de colas, capturas, pruebas, ámbitos y diff. El nuevo
resultado B01–B06 y la comparación visual quedan PENDIENTES. La prueba física
en Redmi Note14 Pro5G, Android16/HyperOS3, y la fidelidad completa de píxeles
siguen pendientes. Avisar después del push y esperar al usuario antes de F007.

BitChord se mantiene en b690dc146009d4815825fa4a20815ef2e4ee8898, solo lectura.
GPLv3 y autorías originales conservadas; la cabecera Aurelis identifica su
adaptación sin reemplazar los avisos aplicables. No se copian fuentes SF,
recursos Google ni código de funciones excluidas.

| Fuente BitChord | SHA256 |
|---|---|
| app/src/main/java/com/music/bitchord/ui/onboarding/OnboardingScreen.kt | d8dfbce86c9ef6a31fb99932aa09a81fa23c4676be1e51159a5fd7f223c7ffc6 |
| app/src/main/java/com/music/bitchord/MainActivity.kt | 90e254be457f57c70166321ca9bac3bc5e42eba4e6bdf577b48235ea75c200e2 |
| sharedUi/src/jvmSharedMain/kotlin/com/music/bitchord/ui/screens/HomeScreen.kt | d6f6a11a322eb968823fd6abd861f3e8ebff381e71e629a2a77a55d38d66e017 |
| sharedUi/src/jvmSharedMain/kotlin/com/music/bitchord/ui/components/ShelfRow.kt | 5478cc6b81502bc206045ae8798b991fd649f2662128b866920cee99a13d3fa5 |
| sharedUi/src/jvmSharedMain/kotlin/com/music/bitchord/ui/components/Common.kt | 9e6847c462b52f4c94a53b24a297fe9aba1fa5a0f29accde2746e7a6c075b04e |

## Entrega F007/F008 — recomendaciones y géneros (2026-11-17)

Autorización: «Continúa con los dos siguientes bloques F007 and F008».
Base44d2fb5d4c35998e26a7e7606fa2ba3c4f1e2d04, CI197 B01–B06 SUCCESS.
Referencia BitChord b690dc146009d4815825fa4a20815ef2e4ee8898, clon solo lectura.

F007 adapta TopPicksShelf/TopPickCard/CardCaption/ReleaseArt: ancho60% con
techo280dp, proporción0,75, esquinas12dp, gutter16dp, separación14dp y caption
13/17/13sp. Conserva la posición tras el banner y antes del feed como el origen.
La portada cuadrada se funde con una franja inferior cacheada del mismo archivo;
se adapta el blur Android original a48px, altura4..96 y solicitud160px, con
cancelación y sin reciclar la imagen propiedad de Coil. La caché se invalida con
su propia clave. No se incorpora vídeo de artista ni canvas externo.

Las tarjetas reciben las recomendaciones actuales de LocalHomeData sin cambiar
el algoritmo. El toque usa la cola real del estante y su índice; mantener pulsado
abre las acciones de esa canción. La canción actual conserva semántica de
selección y barras de reproducción. La leyenda De tu biblioteca sustituye
etiquetas remotas de novedad/estación que no serían verdaderas para esos datos.
No se crean estados de ánimo, inferencia musical, streaming ni mezclas DSP.
HeroShelf y la tarjeta principal F005 se conservan para sus otros consumidores.

F008 completa GenreSleeve con MoodGenreSleeve/duotone originales: franja28%,
carátula72%, proporción1,72, esquinas18dp y separación12dp; rejilla2..6 columnas
según ventana, fila incompleta del mismo tamaño. La portada viene de la primera
carátula no vacía de las canciones del grupo actual, sin red añadida. Los géneros
se siguen agrupando únicamente por etiquetas reales; unknown conserva su clave
independiente y su nombre se localiza también en el detalle. El contador real
pluralizado se ofrece semánticamente, sin el rótulo extra ausente del origen.

Explorar conecta el skeleton, error, permiso, vacío y reintento a LibraryState y
al escáner existente; no inventa géneros durante carga/error ni tras vaciar el
catálogo. No se inicia F012 ni se añade otro observador o gesto de actualización.
Se preservan nombre Aurelis, MediaStore, ProfileRepository, servicio y sesión,
modelos y filtros ABI, firmas, Gradle, todos los workflows y los iconos ajenos.
Nuevos textos ES/EN; traducciones adicionales quedan en F146.

Se añaden14 métodos Android de regresión destinados a SDK33/35: geometría de
recomendaciones y géneros en teléfono/ventana ancha, RTL/fuente grande, selección,
cola e índices al reordenar, tap/hold, eliminación del estante vacío, bounds/color
sin mutar la portada original, errores/permisos/reintentos y navegación real de
Explore a un género y vuelta. Se actualiza el desplazamiento de las pruebas Home
para el carrusel adicional y se conserva su comprobación de cola/seek/retorno.
Android, Lint, Gradle y APK nuevos solo en Actions, pendientes. La fidelidad de
píxeles y la prueba física en Redmi Android16/HyperOS3 siguen pendientes.

Dos revisiones del mismo agente: (1) propiedad de datos/cola/permisos/caché y
contratos Compose/Coil3.0.4 verificados en su fuente; (2) medidas/RTL/accesibilidad,
recorridos de pruebas, procedencia/licencias, lista de rutas y diff. Sin agentes
independientes. Los SHA256 de fuente están en LICENSE_AUDIT. Tras push avisar y
esperar al usuario; el siguiente bloque F009 no está autorizado en esta entrega.

Listas fijadas antes de editar; cada corrección futura cuenta en estos acumulados:

F007: 12 rutas de40:

- NOTICE
- docs/migration/beta-local/LICENSE_AUDIT.md
- docs/migration/beta-local/FUNCTION_BLOCKS_40_10.md
- docs/migration/beta-local/PROGRESS_TRACKER.md
- app/src/main/res/values/strings.xml
- app/src/main/res/values-en/strings.xml
- app/src/main/java/com/music/aurelis/ui/components/home/LocalRecommendationsShelf.kt
- app/src/main/java/com/music/aurelis/ui/components/home/RecommendationBackdrop.kt
- app/src/main/java/com/music/aurelis/ui/screens/HomeScreen.kt
- app/src/test/java/com/music/aurelis/ui/RecommendationsPresentationTest.kt
- app/src/test/java/com/music/aurelis/ui/RecommendationBackdropTest.kt
- app/src/test/java/com/music/aurelis/ui/HomeShelvesIntegrationTest.kt

F008: 11 rutas de40:

- NOTICE
- docs/migration/beta-local/LICENSE_AUDIT.md
- docs/migration/beta-local/FUNCTION_BLOCKS_40_10.md
- docs/migration/beta-local/PROGRESS_TRACKER.md
- app/src/main/res/values/strings.xml
- app/src/main/res/values-en/strings.xml
- app/src/main/java/com/music/aurelis/ui/screens/ExploreScreen.kt
- app/src/main/java/com/music/aurelis/MainActivity.kt
- app/src/main/java/com/music/aurelis/ui/screens/LocalCollectionScreen.kt
- app/src/test/java/com/music/aurelis/ui/ExplorePresentationTest.kt
- app/src/test/java/com/music/aurelis/ui/ExploreIntegrationTest.kt
