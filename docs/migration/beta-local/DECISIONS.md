# DECISIONS

Decisiones aprobadas en el prompt y consulta posterior. No reinterpretar un ID
interno de persistencia como permiso para mantener múltiples cuentas activas.

| ID | Decisión |
|---|---|
| D01 | Misma rama/PR; sin force push, merge o historial reescrito. |
| D02 | Aurelis, com.music.aurelis, LICENSE y firma intactos. |
| D03 | Un perfil local; sin selector ni autenticación Aurelis. |
| D04 | Conservar activo anterior y respaldo privado antes de consolidar; no mezclar/borrar silenciosamente. |
| D05 | Biblioteca/reproducción locales; metadatos no inventados. |
| D06 | Género MediaStore/etiquetas; ausente=Sin clasificar. |
| D07 | Paridad en flujo activo; interacción y estados reales. |
| D08 | Un servicio/sesión; dos decks internos coordinados permitidos. |
| D09 | Retirar Dolby y sección Audio Quality vacía; MiniPlayer conserva calidad real del archivo/salida. |
| D10 | Bit-perfect excluye mezcla, DSP y velocidad incompatibles. |
| D11 | AutoMix Beta1/2/3; Balanceado2 por defecto. |
| D12 | Sustituir analizadores vacíos por inferencia verificable. |
| D13 | Letras locales/caché primero; consulta explícita; LRCLIB prioritario. |
| D14 | Cliente de traducción configurable existente; no reactivar endpoints legacy. |
| D15 | Storage solo número/tamaño conocido de canciones; no cachés visibles ni desglose por formato en beta. |
| D16 | Eventos nuevos para Replay; totales previos sin fechas inventadas. |
| D17 | Respaldo versionado; importación validada/confirmada y recuperación. |
| D18 | Etiquetar copia por defecto; reemplazar solo autorizado y recuperable. |
| D19 | Carátulas remotas/scrobbling opt-in, no audio remoto. |
| D20 | Credenciales en Keystore; fuera de logs/APK publicado/exportaciones. |
| D21 | Roboto sistema por defecto/Vazirmatn OFL; sin SF Pro. |
| D22 | Exclusiones/aplazamientos de MASTER_PLAN; no eliminar funciones compatibles. |
| D23 | Mantener plataforma actual; cambios por incompatibilidad demostrada. |
| D24 | Dos revisiones por unidad; evidencia antes de declarar éxito. |

## Nombres investigados

Miscellaneous es una sección real de SettingsSheet y strings upstream. No se encontró
literal «Miss Jan News»: no crear función con ese nombre. Mask aparece en máscaras
visuales/de bits y máscara vocal de AutoMix; no es una opción independiente acreditada.
Play Next insertará una ocurrencia inmediatamente después de la actual, preservando
resto de cola/posición. No equivale a append al final.

## Dependencias externas pendientes

Modelos: procedencia/exportación. Binarios: fuentes/configuración correspondientes.
BetterLyrics/BiniLyrics/LRC Red: condiciones individuales. Last.fm/ListenBrainz:
credenciales y autorización oficial; no embeber secreto ni crear backend excluido.
Dispositivos: USB/DAC, Bluetooth y audio largo. Fidelidad: referencia visual
verificable. Formato de cabeceras: CSV pendiente histórico. Son puertas de los
componentes afectados/B13, no excusa para simularlos. No quedan preguntas de producto
imprescindibles para B00. B01 espera autorización después de entregar B00.


## Ejecución autorizada hasta B02

«autorización concedida continúa hasta B02» permite B01.2/.3 y B02.1/.2/.3 tras
B01.1 existente. Se conserva rama/PR/título e identidad Git local; B03 no iniciado.
B02.3 incluye componentes funcionales, no certificación de paridad del panel:
ancla/snapshot/retorno upstream se integrarán en consumidores B03/B04/B05.
No requiere cambiar decisiones de producto ni retirar funciones del plan.
