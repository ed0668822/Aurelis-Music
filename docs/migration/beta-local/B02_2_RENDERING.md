# B02.2 — cristal y desenfoque

Referencia F03: LiquidGlass, backdrop y floatingtabbar ya presentes, con avisos
Kyant0/Apache-2.0 y Ahmed Gamil/MIT conservados. No se importa otro motor ni se
cambian dependencias. La entrada antigua de ui/theme delega al componente activo:
ambos imports consumen el mismo estado y renderizado.

El host captura solo la página, dejando barra/overlays fuera para evitar muestreo
recursivo. La preferencia Liquid Glass conecta blur, vibrancy, lente/dispersión,
reflejo y sombra de los componentes existentes. Reducir desenfoque produce una
superficie sólida; sin aceleración se usa sólida; si falla la compilación de
shaders se permite Haze cuando hay fuente compatible. La píldora tiene fallback
plano si no existe fondo o capacidad. No se afirma fidelidad completa del nav de
B03 ni del reproductor de B05.

La capacidad de la View y la del Canvas se comprueban por separado. Un Canvas de
captura/impresión por software no admite drawRenderNode aunque su View viva en
una ventana acelerada: los nodos fuente/destino/reflejo evitan esas operaciones.
El destino mantiene sus callbacks de superficie/contenido sin efecto GPU.

## Revisiones y pruebas

Revisión1, misma persona/agente: estado único, imports, fuente fuera de overlays,
nullabilidad/píldora, flags de accesibilidad, licencias y renderizador software.
Se corrigió incompatibilidad de nullabilidad y el crash real de las capturas.
Revisión2 separada: programas exactos (cuatro), fallback, tests del reproductor,
ajustes/navegación y suite U; resultado final en B02_2_EVIDENCE.

Robolectric4.14.1 usa Skia de Android S al emular API33: incluso un shader mínimo
con child.eval falla. Fuente verificable:
https://github.com/robolectric/robolectric/blob/robolectric-4.14.1/shadows/framework/src/main/java/org/robolectric/shadows/ShadowNativeRuntimeShader.java
El loader selecciona android_runtime nativo de V para SDK35. GlassRenderingTest
usa SDK35 y compila los cuatro programas exactos sin traducción ni suprimir
aserciones; 13 casos seleccionados correctos (BUILD SUCCESSFUL in28s).
La propuesta de adaptar sintaxis solo en tests fue rechazada por revisión
automática y no se ejecutó; se resolvió usando el runtime compatible existente.

Sin dispositivo conectado: D, aspecto/fps/GPU OEM y API33 real quedan pendientes.
Compilar AGSL no demuestra por sí solo la fidelidad visual ni coste del efecto.
