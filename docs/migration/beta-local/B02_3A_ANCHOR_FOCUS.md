<!-- Aurelis © 2026. GPL-3.0-or-later -->
# B02.3-a — anclaje de menús y devolución del foco

## Autorización, base y entrega

El usuario pidió empezar B02–B05. Se ejecuta la primera entrega acotada de
B02.3-a, anclaje y devolución del foco, sobre la integración existente. Su
autorización incluye commit/push a feature/bitchord-local-migration, mismo PR1.
Base: ed9771836bb3aabb644c54f1fb06412d62b270a2; checkout limpio y remoto
coincidente tras fetch. Identidad local ed0668822 / ed0668822@gmail.com; cuenta
y correo verificado comprobados por API antes de crear el commit.

Entrega difícil: máximo seis archivos distintos contando código, pruebas y docs.

| Archivo | Cambio |
|---|---|
| app/src/main/java/com/music/aurelis/ui/components/HeldContextMenu.kt | Ancla del gesto y ciclo de vida/foco por host. |
| app/src/main/java/com/music/aurelis/ui/components/IosMenu.kt | Posición relativa al origen, ajuste a ventana y cierre. |
| app/src/main/java/com/music/aurelis/ui/theme/AurelisTheme.kt | Estado de menús instalado en el host real. |
| app/src/test/java/com/music/aurelis/ui/theme/SharedGesturesTest.kt | Regresiones geométricas y Compose/Robolectric. |
| docs/migration/beta-local/B02_3A_ANCHOR_FOCUS.md | Alcance, evidencia y límites de esta entrega. |
| docs/migration/beta-local/PROGRESS_TRACKER.md | Seguimiento y puerta entre bloques. |

## Referencia y problema concreto

BitChord, solo lectura, commit fijo b690dc146009d4815825fa4a20815ef2e4ee8898:
app/src/main/java/com/music/bitchord/ui/components/IosMenu.kt y HeldContextMenu.kt.
Su popover utiliza la geometría del origen y el menú mantenido conserva el
elemento levantado para su retorno visual. Se consultaron ambos archivos; esta
entrega implementa el contrato local de geometría/foco sin trasplantar sus
implementaciones. Se mantienen los avisos y atribuciones anteriores de Aurelis.

Antes: el gesto ya abría SongActionsDialog sin reproducir, pero IosMenu usaba
Alignment.Center y no recibía ni conservaba el origen. Pulsar una canción cerca
de un borde abría el mismo panel centrado. Tampoco había devolución explícita del
foco al elemento después del cierre.

## Comportamiento implementado y conexiones

AurelisTheme instala un estado de menús por host de composición. El modificador
heldContextMenuInteractions, ya consumido por SongRow y HomeCarousels, registra
los límites en ventana y un FocusRequester del origen. El gesto conserva la
separación entre toque/reproducción y pulsación larga/acciones. Los callbacks
existentes de Home/biblioteca/búsqueda siguen abriendo el mismo SongActionsDialog;
este utiliza HeldContextMenu e IosMenu sin cambiar sus firmas ni acciones.

El menú consume el ancla una sola vez al abrirse. Un PopupPositionProvider usa
la ventana y el tamaño medido del panel: alinea el extremo lógico, admite RTL,
intenta colocarlo debajo, después encima y finalmente limita sus coordenadas.
Mantiene margen de16dp cuando cabe y separación de8dp. Sin origen visible conserva
la posición centrada, incluida la apertura independiente del reproductor. Las
medidas limitadas y el scroll existentes siguen gestionando contenido largo.

El cierre por Atrás, exterior o acción mantiene el callback propietario. Al salir
el menú se programa la devolución del foco para el siguiente frame del host,
después de retirar el popup. La retirada de la fila invalida el ancla; si el host
desaparece se cancela su efecto. El estado no es global ni guarda Activity,
LocalSong, URI o capturas de imagen. El ancla consumida no se reutiliza en el
siguiente menú abierto mediante otro control.

## Dos revisiones y validación

Revisión1 del mismo agente: consumidores reales, contrato de callbacks, geometría
LTR/RTL/bordes, exclusión entre reproducción y acciones, y ciclo de vida del foco.
Revisión2 del mismo agente: API Compose disponible, rutas del cambio, pruebas,
cabeceras, sintaxis y diff; conservación de firma, ABI, reproducción y workflows.
No son revisiones independientes ni resultados de ejecución Android.

Comprobaciones locales: diff y presupuesto; lectura estática de las APIs Compose
en los artefactos de dependencias existentes, sin invocar compilador/Gradle.
No se crean fixtures que simulen una ejecución de la app ni se compila localmente.

Pruebas preparadas en SharedGesturesTest para GitHub:

- Posición relativa LTR/RTL, inversión sobre una fila inferior y límites laterales.
- Origen ausente/invisible, ventana pequeña y falta de espacio vertical.
- Consumo único, separación entre hosts y ancla retirada.
- Pulsación larga, cierre y devolución del foco sin reproducir.
- Retirada del origen con el menú abierto y cierre posterior.
- Atrás y evento exterior cierran el popup real y devuelven el foco.
- Regresiones existentes de cola/favoritos/playlists y contenido desplazable.

La compilación y las pruebas nuevas quedan pendientes de GitHub. Los cambios en
app/** activan automáticamente el pipeline unificado B01–B06 al hacer push; no
se altera el filtro ni se solicita ejecución manual. Un resultado de compilación
no acredita TalkBack, tacto/foco OEM ni fidelidad visual en dispositivo físico.

## Pendientes y siguiente puerta

B02.3-a permanece parcial: snapshot del elemento, elevación/retorno visual y
comparación física con BitChord aún pendientes; esta entrega devuelve el foco,
sin declarar implementada la animación de retorno. B02.3-b (hápticos/accesibilidad)
y B03–B05 se ejecutan en sus entregas posteriores, con autorización. CI12.3 y las
cinco aclaraciones históricas de CI16 siguen abiertas; el usuario priorizó ahora
B02–B05. B06/B07 no se cierran retrospectivamente y B09 continúa sin iniciar.

Tras publicar, comunicar commit e inicio del CI y esperar el resultado del
usuario y su autorización antes del siguiente bloque. Si falla, conservar el
alcance y revisar los informes aportados antes de continuar el plan.
