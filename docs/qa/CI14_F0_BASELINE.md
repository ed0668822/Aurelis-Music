# CI14 F0 — línea base y decisiones verificadas

Fecha UTC: 2026-11-12. Rama: `feature/bitchord-local-migration`. Sin cambios en la aplicación ni en CI al medir.
Fuente: GitHub REST, endpoints `/actions/runs/{id}` y `/actions/runs/{id}/jobs?per_page=100`; tiempos desde `started_at` a `completed_at` en los seis jobs, sin sumar tiempos paralelos.
No se confunde un verde de un SHA antiguo con una ejecución posterior.

| Run / SHA | Evento | Estado | Tiempo de pared B01–B06 |
|---|---|---|---|
| #40 / `c5ff687` | push | success | 30,4 min |
| #41 / `09b848d` | push | success | **29,3 min** — línea base F0 |
| #50 / `cded1ae` | push | success | 27,4 min |
| #51 / `cded1ae` | pull_request | failure B05; B06 falló correctamente por dependencia | 30,8 min |

## Línea base por etapa — run #41

| Etapa | Inicio UTC | Fin UTC | Duración |
|---|---|---|---:|
| B01 | 04:51:42 | 04:55:39 | 4,0 min |
| B02 | 04:55:41 | 04:59:47 | 4,1 min |
| B03 | 04:59:49 | 05:04:53 | 5,1 min |
| B04 | 05:04:55 | 05:14:05 | 9,2 min |
| B05 | 05:14:07 | 05:20:42 | 6,6 min |
| B06 | 05:20:44 | 05:20:57 | 0,2 min |

Referencia: https://github.com/ed0668822/Aurelis-Music/actions/runs/43157520038

## Ejecución reciente — run #50

| Etapa | Resultado | Duración |
|---|---|---:|
| B01 | success | 3,4 min |
| B02 | success | 5,0 min |
| B03 | success | 5,0 min |
| B04 | success | 5,8 min |
| B05 | success | 7,7 min |
| B06 | success | 0,3 min |

Referencia: https://github.com/ed0668822/Aurelis-Music/actions/runs/43262158349
Las diferencias de tiempo entre runs **no acreditan** una optimización F3.

## Decisiones para F0–F1

- D1: B01–B06 es la ruta canónica; `android.yml` permanece retirado.
- D2: publicación oficial Release ARM64 solo desde `main`/tags, con aprobación exigida en el environment `release`; no publicar en PR ni ramas feature.
- D3: VirusTotal por hash en CI ordinario. Cualquier subida pública requiere acción manual, consentimiento explícito y environment protegido.
- D4: la solicitud de completar F1 autoriza exclusivamente la restricción de firma Release en `app/build.gradle.kts` (H3); los cambios de dependencias/targetSdk de F7 **no** están aprobados.
- D5: eliminación de archivos Media3 FFmpeg diferida; requiere verificación técnica y autorización independiente.
- D6: reducción del 30 % respecto a #41, equivalente a <=20,5 min de tiempo de pared. Se mide en F3, no en F0.

## Deudas verificadas antes de F1

- El run #51 falló por el test real de Media3 `repeatAllCyclesFromSecondOriginalWavBackToFirst`, con segundo WAV en estado IDLE. Esta falla no autoriza retries silenciosos ni la eliminación del test.
- La configuración de environments, revisores, variables y secretos del repositorio debe comprobarse desde GitHub; los workflows por sí solos no prueban que existan reglas de aprobación.
- F0 se cierra como medición y alcance. Validación Android física y confiabilidad repetida de B05 continúan pendientes.
