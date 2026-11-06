# CI05 — ciclo de vida de Dispatchers.Main en PlaybackIntegrationTest

Base3128d9c, misma rama feature/bitchord-local-migration y PR1.
Run138: https://github.com/ed0668822/Aurelis-Music/actions/runs/42239098568
Job127964358176, commit3128d9c89d2f4c9d4a991f43129a413d546ad1bd, failure.

## Evidencia confirmada

Se reintentó gh run list: la API sigue devolviendo Forbidden. Las páginas públicas
sí permitieron consultar run/job y las anotaciones JUnit añadidas en CI04.
Dos casos de release aparecen con IllegalStateException:
- PlaybackIntegrationTest.latestTapWinsAndEmptySelectionDoesNotClearTheQueue
- PlaybackIntegrationTest.editingSingleProfileDoesNotCancelPendingPlaybackOrChangeTheQueue
Mensaje: Dispatchers.Main is used concurrently with setting it.
No se asume acceso a logs completos, ni fallo de compilación Kotlin por este mensaje.
No se atribuyen al nuevo run los errores R.string/maxWidth ni el assert global anterior.

## Análisis y corrección localizada

El harness sustituye Main en Before y ejecutaba store.clear(); resetMain() en After
sin esperar finalización. clear cancela scopes, pero cancelación no equivale a join.
PlaybackViewModel puede tener búsqueda de letras en LocalLyricsRepository.find,
que usa withContext(Dispatchers.IO) y vuelve al contexto Main. Algunas pruebas
reemplazan ViewModels en el mismo store; sus scopes también deben terminar.

Se contrastó el guard de concurrencia de TestMainDispatcher con javap del JAR
kotlinx-coroutines-test1.9.0 almacenado: el mensaje corresponde a lectura/escritura
concurrente del dispatcher delegado. Las anotaciones no proporcionan toda la pila;
la ruta insegura identificada y corregida es la restauración antes de terminar
trabajo propio. Su suficiencia debe confirmarse con el nuevo run, sin atribuir
esta excepción a funcionamiento de Android real o descartar otras fuentes sin datos.

Una factory de pruebas conserva el Job de cada viewModelScope creado, incluyendo
los reemplazados. After cancela mediante ViewModelStore y usa runTest con el mismo
StandardTestDispatcher para esperar joinAll, con límite de10s. Solo restaura Main
si todos esos scopes terminaron; timeout sigue siendo fallo y no se disimula.
No sleeps arbitrarios, reintentos, sleeps de estabilización, ignores o cambio de
paralelismo global. No se cambian producción/dispatchers de la app para pasar tests.

Nueva regresión: crea trabajo IO suspendido, reemplaza el ViewModel y comprueba
que su finalizador NonCancellable que vuelve a Main termina junto con el scope
viejo. La factory conserva también el scope actual, que se cierra en After.
Los diez casos anteriores y todas sus líneas de aserción se conservan; solo
utilizan la factory en lugar del constructor directo. Once casos tras la adición.

## Revisiones y límites

Revisión1 estática: registros/commit, origen de Main mutable, cancelación asíncrona,
scopes reemplazados, factory y scheduler único. Revisión2, pasada separada del mismo
agente: conservación de los diez casos/asserts, timeout, restauración condicionada,
regresión IO→Main, firmas del constructor y alcance protegido. git diff --check
correcto. No se presenta como revisor independiente externo o prueba ejecutada.

Modificado únicamente PlaybackIntegrationTest.kt y documentación. App principal,
servicio/reproductor/Glass/QA01, workflow/gates/reportes, firma/ABI, dependencias,
LICENSE/NOTICE/LICENSES y plan intactos. Sin permisos o credenciales nuevos.

No Gradle, tests Android ni APK locales por preferencia del usuario. Ejecución de
la regresión, suite debug/release, empaquetado y validación de la corrección:
PENDIENTES DE GITHUB. No se afirma que una carrera intermitente esté eliminada por
revisión estática. Publicar mismo PR, avisar y esperar resultado. No B06 ni QA02.
