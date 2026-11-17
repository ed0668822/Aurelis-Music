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

- Base actual: Aurelis `9d203d13dc86bfddf90ac64dd39ee2e679ef1cdb`.
- CI183, run43970204682: B01–B06 SUCCESS, comprobado antes de esta planificación.
- F001 entregado:1 archivo documental. F002 entregado:18 rutas acumuladas,
  incluidas reparaciones. No se vuelven a implementar por reorganizar el plan.
- F003 se implementa en la entrega documentada en [F003_TOP_BAR](F003_TOP_BAR.md),
  con14 rutas previstas y CI nuevo pendiente. F004 está autorizado y sin implementar.
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
| F003 | Cabeceras, foto local y acceso a ajustes | Media | 40 | Implementado; CI nuevo pendiente |
| F004 | Bienvenida, nombre, permiso y finalización persistida | Difícil | 10 | Autorizado; después de F003 |
| F005–F006 | Cabecera, destacados, carruseles y estanterías de Inicio | Media | 40 | Pendiente |
| F007 | Presentación de recomendaciones locales existentes | Media | 40 | Pendiente |
| F008 | Explorar por géneros locales | Media | 40 | Pendiente |
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
incompleto. No se rediseñan Inicio o Buscar como parte de estas dos funciones.

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
