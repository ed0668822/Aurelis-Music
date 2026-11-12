# CI14 F3 - Benchmark real y cierre de meta

Medida: GitHub Actions REST, primer job started_at hasta ultimo job completed_at, dos runs completos success.

| Fase | Base #41 segundos | Actual #88 segundos |
|---|---:|---:|
| B01 | 237 | 256 |
| B02 | 246 | 301 |
| B03 | 304 | 336 |
| B04 | 550 | 321 |
| B05 | 395 | 311 |
| B06 | 13 | 20 |
| TIEMPO DE PARED | 29.25 min | 15.65 min |

Reduccion: (29.25-15.65)/29.25*100 = 46.5%. Meta >=30% cumplida en una ejecucion exitosa completa.
En #88 B02, B03 y B05 comenzaron al mismo tiempo tras B01. B04 espero B02 y B03; B06 requirio PASS total. Debug/instrumentation provienen de B01 con SHA256/procedencia, B04 construye Release una vez, B05 usa APK sin reconstruccion.
El tiempo de una ejecucion no garantiza duraciones futuras y no sustituye tests fisicos.
https://github.com/ed0668822/Aurelis-Music/actions/runs/43157520038
https://github.com/ed0668822/Aurelis-Music/actions/runs/43306095872