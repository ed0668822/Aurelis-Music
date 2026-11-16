<!-- Aurelis © 2026. GPL-3.0-or-later -->
# B05 — cápsulas locales y presentación ES/EN

Entrega 5 de B05_UI_EVIDENCE_PLAN.md, autorizada por «Continúa». Base publicada:
0f6396242fc1906442e01972453f486ec2559519. MiniPlayer y reproductor vertical/horizontal
ya fueron implementados en e0fc7cad y corregidos en esa base; no se reaplican aquí.
El límite vigente es 60 archivos distintos por entrega, incluidas correcciones.

## Cambios y procedencia

LocalDetailHeader adapta ActionPill de DetailScreen.kt de BitChord, referencia fija
b690dc146009d4815825fa4a20815ef2e4ee8898. SHA256 del archivo consultado:
21b88b74aff957481e54795b1ca069a07322aa4f28494d3b800cde5f61a66855.
Reproducir usa cápsula blanca y Aleatorio una cápsula con borde: alto 50 dp,
fila máxima 420 dp, separación 14 dp, icono 18 dp y separación de texto 8 dp.
Se conserva el feedback Resume y los callbacks originales. El relleno interior
se adapta a 16 dp para los textos españoles; el borde sigue el contraste del tema.
No se añade muestreo del propio backdrop. NOTICE y LICENSE_AUDIT conservan la
procedencia GPLv3 de BitChord y distinguen las aportaciones de Aurelis.

Música local y Buscar usan recursos ES/EN para títulos, pestañas, acciones,
ordenación, instrucciones, vacíos, historial y accesibilidad. Inicio usa plurales
reales y localiza etiquetas de carga/hero. Son 39 claves nuevas por idioma,
incluido un plural; se reutilizan recursos existentes donde corresponde.
Los títulos/artistas y consultas originales no se traducen ni reformatean.
Los nombres de enum persistidos, rutas, permisos y almacenamiento no cambian.
La suma de duración de la cabecera ignora duraciones negativas y satura antes
de desbordar Long. Se conservan listas locales, acceso por foto y PhotoPicker.

## Validación preparada

Tres regresiones nuevas para GitHub Actions:

1. Música local en inglés: singular, cápsulas de 50 dp, Play/Shuffle reales,
   ordenación y títulos/artistas originales.
2. Buscar en inglés: categorías, consulta con acento y porcentaje preservada,
   selección del historial, eliminación individual y borrado completo.
3. Inicio en español: cambio de singular a plural y acceso a Favoritos.

Los dos casos existentes de LocalUiParityTest que consultaban «Local Music»
usan ahora el recurso localizado; se conservan sus comprobaciones de navegación
y desplazamiento. No se ejecutaron Gradle, Lint ni pruebas Android localmente.
Las tres pruebas nuevas siguen PENDIENTES de CI; el resultado anterior de 429
por variante no acredita esta entrega ni permite afirmar que ahora pasan 432.

Se realizaron dos revisiones estáticas por el mismo agente, no independientes:
la primera de APIs, imports, recursos y ámbitos Compose; la segunda de llamadas,
enum persistido, acciones reales, límites de la evidencia y alcance de la entrega.
Sin defectos adicionales confirmados en esas revisiones. El parser Kotlin pasó
en los 11 archivos Kotlin implicados; XML válido, claves únicas, correspondencia
ES/EN y contratos de formato comprobados. git diff --check pasó.

## Inventario: 18 archivos

- app/src/main/java/com/music/aurelis/MainActivity.kt
- app/src/main/java/com/music/aurelis/ui/components/LocalDetailHeader.kt
- app/src/main/java/com/music/aurelis/ui/components/LocalUiText.kt (nuevo)
- app/src/main/java/com/music/aurelis/ui/components/SortMenu.kt
- app/src/main/java/com/music/aurelis/ui/components/home/HomeHero.kt
- app/src/main/java/com/music/aurelis/ui/screens/HomeScreen.kt
- app/src/main/java/com/music/aurelis/ui/screens/LocalMusicContent.kt
- app/src/main/java/com/music/aurelis/ui/screens/SearchScreen.kt
- app/src/main/res/values/strings.xml
- app/src/main/res/values-en/strings.xml
- app/src/test/java/com/music/aurelis/ui/HomeLocalTest.kt
- app/src/test/java/com/music/aurelis/ui/LocalUiParityTest.kt
- app/src/test/java/com/music/aurelis/ui/LocalUiLocalizationTest.kt (nuevo)
- NOTICE
- docs/migration/beta-local/LICENSE_AUDIT.md
- docs/migration/beta-local/PROGRESS_TRACKER.md
- docs/migration/beta-local/B05_UI_EVIDENCE_PLAN.md
- docs/migration/beta-local/B05_LOCAL_UI_FOLLOWUP.md (este informe)

## Evidencia y siguientes pasos

La base Main170/43800278588 sí aprobó B01–B06; UI26/43800312810 terminó en
FAILURE en la referencia. Aurelis alcanzó sus 18 estados por material y los casos
nativos pasaron en API35/36; eso no demuestra identidad visual. Los ZIP actuales
de comparación siguen pendientes de lectura por el bloqueo de descarga registrado.
Las capturas originales UI23 y el código fijo justifican las diferencias corregidas.
La causa de pérdida de ADB de la referencia no está demostrada.

Tras publicar esta entrega, avisar al usuario y esperar a que comunique si GitHub
compiló correctamente o falló. Se puede iniciar Actions y ofrecer sus enlaces;
no vigilar conclusiones ni avanzar a otra entrega por iniciativa propia.
Si falla, analizar evidencia real y reparar dentro del mismo presupuesto.
El cierre visual B05 y la prueba física en Redmi Note 14 Pro 5G, Android 16 /
HyperOS 3 siguen pendientes. B07 no se abre aquí y B09 continúa sin iniciar.
No se crea una Release oficial ni se certifica toda la UI por compilación.
