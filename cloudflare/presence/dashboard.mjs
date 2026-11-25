// SPDX-FileCopyrightText: © 2026 Aurelis
// SPDX-License-Identifier: LGPL-3.0-or-later

export function dashboard(nonce) {
  return `<!doctype html>
<html lang="es">
<head>
  <meta charset="utf-8">
  <meta name="viewport" content="width=device-width,initial-scale=1">
  <meta name="theme-color" content="#10121b">
  <title>Sesiones activas · Aurelis Music</title>
  <style nonce="${nonce}">
    :root { color-scheme: dark; font: 16px system-ui, sans-serif; background: #10121b; color: #f3f2fa; }
    * { box-sizing: border-box; }
    body { margin: 0; min-width: 260px; }
    main { width: min(100% - 2rem, 50rem); margin: 0 auto; padding: clamp(1.75rem, 6vw, 4rem) 0; }
    header, .row, .actions { display: flex; align-items: center; gap: .75rem; flex-wrap: wrap; }
    header { justify-content: space-between; margin-bottom: 1.5rem; }
    .brand { display: flex; align-items: center; gap: .65rem; font-weight: 720; }
    .mark { display: grid; place-items: center; width: 2rem; height: 2rem; border-radius: .6rem; color: #14152a; background: #c9bcff; font-size: 1.35rem; }
    .card { padding: clamp(1.15rem, 4vw, 2rem); border: 1px solid #34374c; border-radius: 1.25rem; background: #191c29; }
    .counter { background: radial-gradient(ellipse at top right, #31294b, transparent 70%), #191c29; margin-bottom: 1rem; }
    h1, h2, p { margin: 0; }
    h1 { font-size: clamp(1.15rem, 4vw, 1.5rem); font-weight: 640; }
    h2 { font-size: 1.05rem; }
    .value { display: block; margin: .25rem 0 .5rem; color: #e0d8ff; font-size: clamp(3.8rem, 16vw, 7.3rem); line-height: 1; font-weight: 730; letter-spacing: -.06em; font-variant-numeric: tabular-nums; }
    .muted { color: #bcbed1; font-size: .9rem; line-height: 1.55; }
    .row { justify-content: space-between; }
    .actions { margin-top: 1.25rem; }
    button { min-height: 2.75rem; padding: .55rem .85rem; border: 1px solid #55546e; border-radius: .65rem; background: #232539; color: inherit; font: inherit; font-size: .9rem; cursor: pointer; }
    button:hover { background: #32334a; }
    button:disabled { opacity: .55; cursor: default; }
    button:focus-visible, input:focus-visible { outline: 2px solid #c9bcff; outline-offset: 3px; }
    .status { color: #d2d1e4; font-size: .88rem; }
    .status::before { content: ""; display: inline-block; width: .55rem; height: .55rem; margin-right: .45rem; border-radius: 50%; background: #a6a8bb; }
    .status[data-tone="live"]::before { background: #72e5bc; }
    .status[data-tone="error"]::before { background: #ffb387; }
    .plot { display: flex; align-items: end; gap: clamp(2px, 1vw, 7px); height: 8.5rem; margin: 1rem 0 .5rem; padding: .4rem .25rem 0; border-bottom: 1px solid #4a4c65; background: repeating-linear-gradient(to top, transparent 0, transparent calc(25% - 1px), #303349 25%); }
    .bar { flex: 1; max-width: 2.5rem; min-width: 0; height: var(--height); border-radius: .2rem .2rem 0 0; background: #8070c3; transition: height .23s ease; }
    .bar[data-selected] { background: #d3c5ff; }
    .inspect { display: grid; gap: .25rem; margin-top: .75rem; }
    .inspect input { width: 100%; min-height: 1.75rem; margin: 0; accent-color: #c9bcff; }
    footer { margin-top: 1.2rem; }
    @media (prefers-reduced-motion: reduce) { .bar { transition: none; } }
  </style>
</head>
<body>
  <main>
    <header>
      <div class="brand"><span class="mark" aria-hidden="true">♪</span>Aurelis Music</div>
      <span class="status" id="status" role="status">Conectando</span>
    </header>
    <section class="card counter" aria-labelledby="heading">
      <h1 id="heading">Sesiones activas</h1>
      <output class="value" id="count" aria-live="off">—</output>
      <p class="muted">Sesiones de Aurelis Music con la app visible y el contador activado.</p>
      <div class="actions">
        <button type="button" id="refresh">Actualizar ahora</button>
        <button type="button" id="pause">Pausar actualización</button>
      </div>
      <p class="muted" id="updated">Esperando la primera lectura.</p>
    </section>
    <section class="card" aria-labelledby="history">
      <div class="row"><h2 id="history">Actividad reciente</h2><span class="muted" id="peak"></span></div>
      <div class="plot" id="plot" aria-hidden="true"></div>
      <p class="muted" id="detail">Los datos aparecerán aquí al recibir la primera lectura.</p>
      <div class="inspect">
        <label for="sample" class="muted">Explorar las lecturas del gráfico</label>
        <input id="sample" type="range" min="0" max="0" value="0" disabled>
      </div>
    </section>
    <footer class="muted">El panel consulta cada 20 segundos mientras está visible. El gráfico se construye desde que abres esta página.</footer>
  </main>
  <script nonce="${nonce}">
    (() => {
      const interval = 20000;
      const samples = [];
      const number = new Intl.NumberFormat('es');
      const clock = new Intl.DateTimeFormat('es', {hour: '2-digit', minute: '2-digit', second: '2-digit'});
      const el = (id) => document.getElementById(id);
      const count = el('count'), status = el('status'), updated = el('updated');
      const plot = el('plot'), peak = el('peak'), sample = el('sample'), detail = el('detail');
      const refresh = el('refresh'), pause = el('pause');
      let timer;
      let busy = false, paused = false, failed = false;
      let lastAttempt = -Infinity;
      let inspected = null;

      function showStatus() {
        status.textContent = paused ? 'En pausa' : failed ? 'Sin conexión' : samples.length ? 'En vivo' : 'Conectando';
        status.dataset.tone = paused ? 'idle' : failed ? 'error' : samples.length ? 'live' : 'idle';
      }

      function draw() {
        const selected = samples.findIndex((item) => item === inspected);
        const selectedIndex = selected < 0 ? samples.length - 1 : selected;
        if (selected < 0) inspected = null;
        const maximum = Math.max(...samples.map((item) => item.count));
        peak.textContent = 'Máximo observado: ' + number.format(maximum);
        plot.replaceChildren(...samples.map((item, index) => {
          const bar = document.createElement('span');
          bar.className = 'bar';
          bar.dataset.index = index;
          bar.toggleAttribute('data-selected', index === selectedIndex);
          bar.style.setProperty('--height', Math.max(3, item.count / Math.max(1, maximum) * 100) + '%');
          bar.title = clock.format(item.time) + ' · ' + number.format(item.count);
          return bar;
        }));
        sample.max = samples.length - 1;
        sample.value = selectedIndex;
        sample.disabled = samples.length < 2;
        const item = samples[selectedIndex];
        const label = clock.format(item.time) + ' · ' + number.format(item.count) + (item.count === 1 ? ' sesión' : ' sesiones');
        sample.setAttribute('aria-valuetext', label);
        detail.textContent = label;
      }

      function schedule() {
        clearTimeout(timer);
        if (!paused && !document.hidden) timer = setTimeout(load, interval);
      }

      async function load() {
        if (busy || document.hidden) return;
        if (performance.now() - lastAttempt < 5000) {
          schedule();
          return;
        }
        clearTimeout(timer);
        busy = true;
        lastAttempt = performance.now();
        refresh.disabled = true;
        try {
          const response = await fetch('/v1/stats/live', { signal: AbortSignal.timeout(9000) });
          if (!response.ok) throw new Error();
          const { activeSessions } = await response.json();
          if (!Number.isSafeInteger(activeSessions) || activeSessions < 0) throw new Error();
          const item = { count: activeSessions, time: new Date() };
          samples.push(item);
          if (samples.length > 30) samples.shift();
          count.textContent = number.format(activeSessions);
          updated.textContent = 'Última lectura a las ' + clock.format(item.time);
          failed = false;
          draw();
        } catch {
          failed = true;
          const previous = samples.at(-1);
          updated.textContent = previous
            ? 'Error de actualización. Última lectura a las ' + clock.format(previous.time)
            : 'No se pudo cargar el total. Puedes volver a intentarlo.';
        } finally {
          busy = false;
          refresh.disabled = false;
          showStatus();
          schedule();
        }
      }

      function inspect(index) {
        inspected = index === samples.length - 1 ? null : samples[index];
        draw();
      }

      refresh.addEventListener('click', load);
      pause.addEventListener('click', () => {
        paused = !paused;
        pause.textContent = paused ? 'Reanudar actualización' : 'Pausar actualización';
        showStatus();
        schedule();
        if (!paused) load();
      });
      sample.addEventListener('input', () => inspect(Number(sample.value)));
      plot.addEventListener('click', (event) => {
        if (event.target.dataset.index !== undefined) inspect(Number(event.target.dataset.index));
      });
      document.addEventListener('visibilitychange', () => {
        schedule();
        if (!paused && !document.hidden) load();
      });
      load();
    })();
  </script>
</body>
</html>`;
}
