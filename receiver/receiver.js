/* Aurelis Music Custom Cast Web Receiver (TV03/TV04).
 * CAF controls media playback. This layer renders metadata and synced lyrics
 * against the actual receiver playback clock; the phone never streams a UI.
 */
(() => {
  'use strict';
  const NAMESPACE = 'urn:x-cast:com.music.aurelis.lyrics';
  const ctx = cast.framework.CastReceiverContext.getInstance();
  const player = ctx.getPlayerManager();
  const el = {
    title: document.getElementById('title'),
    artist: document.getElementById('artist'),
    artwork: document.getElementById('artwork'),
    placeholder: document.getElementById('art-placeholder'),
    ambient: document.getElementById('ambient'),
    lyrics: document.getElementById('lyrics'),
    status: document.getElementById('playback-status'),
    time: document.getElementById('time'),
    progress: document.getElementById('progress-fill'),
  };

  let mediaId = null;
  let mediaSignature = '';
  let latest = null;
  let pending = null;
  let renderedIndex = -999;
  let renderedRevision = -1;
  let activeWordSpans = [];

  function safeTime(value) {
    return Number.isFinite(value) && value >= 0 ? value : 0;
  }

  function formatTime(seconds) {
    const s = Math.floor(safeTime(seconds));
    return Math.floor(s / 60) + ':' + String(s % 60).padStart(2, '0');
  }

  function showEmpty(message) {
    const node = document.createElement('p');
    node.className = 'empty';
    node.textContent = message;
    el.lyrics.replaceChildren(node);
    activeWordSpans = [];
  }

  function titleAndArtwork(info) {
    const metadata = (info && info.metadata) || {};
    const title = typeof metadata.title === 'string' ? metadata.title : 'Ready to play';
    const artist = typeof metadata.artist === 'string' ? metadata.artist : 'Aurelis Music';
    const cover = Array.isArray(metadata.images) && metadata.images[0]
      ? metadata.images[0].url : '';
    const url = typeof cover === 'string' && /^https?:\/\//i.test(cover) ? cover : '';
    const signature = [mediaId || '', title, artist, url].join('\u0001');
    if (signature === mediaSignature) return;
    mediaSignature = signature;
    el.title.textContent = title;
    el.artist.textContent = artist;
    if (url) {
      el.artwork.src = url;
      el.artwork.classList.add('visible');
      el.ambient.style.backgroundImage = 'url(' + JSON.stringify(url) + ')';
      el.placeholder.style.display = 'none';
    } else {
      el.artwork.removeAttribute('src');
      el.artwork.classList.remove('visible');
      el.ambient.style.backgroundImage = '';
      el.placeholder.style.display = 'flex';
    }
  }

  function lineIndex(lines, songMs) {
    let low = 0;
    let high = lines.length - 1;
    let found = -1;
    while (low <= high) {
      const mid = (low + high) >>> 1;
      if (Number(lines[mid].t) <= songMs) {
        found = mid;
        low = mid + 1;
      } else {
        high = mid - 1;
      }
    }
    return found;
  }

  function appendWords(container, line) {
    const text = typeof line.x === 'string' ? line.x : '';
    const words = Array.isArray(line.w) ? line.w : [];
    if (!words.length) {
      container.textContent = text;
      return [];
    }
    const spans = [];
    let offset = 0;
    for (const item of words) {
      if (!Array.isArray(item) || item.length < 3 || typeof item[2] !== 'string') continue;
      const token = item[2];
      if (!token.length) continue;
      const start = text.indexOf(token, offset);
      if (start < offset) continue;
      if (start > offset) container.appendChild(document.createTextNode(text.slice(offset, start)));
      const span = document.createElement('span');
      span.className = 'word';
      span.textContent = token;
      container.appendChild(span);
      spans.push({ span, from: Number(item[0]), to: Number(item[1]) });
      offset = start + token.length;
    }
    if (offset < text.length) container.appendChild(document.createTextNode(text.slice(offset)));
    if (!spans.length) container.textContent = text;
    return spans;
  }

  function renderLines(lines, index) {
    const fragment = document.createDocumentFragment();
    const wordSpans = [];
    const first = Math.max(0, index - 2);
    const last = Math.min(lines.length - 1, Math.max(2, index + 2));
    for (let i = first; i <= last; i++) {
      const line = lines[i];
      const row = document.createElement('div');
      row.className = 'line ' + (i === index ? 'active' : i < index ? 'previous' : 'next');
      row.textContent = '';
      if (typeof line.x === 'string' && line.x.length) {
        if (i === index) wordSpans.push(...appendWords(row, line));
        else row.textContent = line.x;
      } else {
        row.textContent = '♪';
      }
      fragment.appendChild(row);
    }
    el.lyrics.replaceChildren(fragment);
    activeWordSpans = wordSpans;
  }

  function animateWords(songMs) {
    for (const { span, from, to } of activeWordSpans) {
      const duration = Math.max(1, to - from);
      const fraction = Math.min(1, Math.max(0, (songMs - from) / duration));
      span.style.setProperty('--fill', (fraction * 100).toFixed(1) + '%');
    }
  }

  function onLyricsMessage(event) {
    let data = event.data;
    if (typeof data === 'string') {
      try { data = JSON.parse(data); } catch (_) { return; }
    }
    if (!data || data.type !== 'lyrics' || typeof data.mediaId !== 'string') return;
    if (!Number.isInteger(data.total) || data.total < 1 || data.total > 96 ||
        !Number.isInteger(data.part) || data.part < 0 || data.part >= data.total ||
        !Number.isInteger(data.revision) || !Array.isArray(data.lines) ||
        data.lines.length > 1200) return;
    // Never replace the current revision with a late packet from an older lookup.
    if (latest && latest.mediaId === data.mediaId && data.revision < latest.revision) return;
    if (!pending || pending.mediaId !== data.mediaId || pending.revision !== data.revision ||
        pending.total !== data.total) {
      pending = {
        mediaId: data.mediaId, revision: data.revision, total: data.total,
        offsetMs: safeOffset(data.offsetMs), parts: new Array(data.total),
      };
    }
    pending.parts[data.part] = data.lines;
    if (pending.parts.filter(part => Array.isArray(part)).length === pending.total) {
      const lines = pending.parts.flat()
        .filter(row => row && Number.isFinite(row.t) && typeof row.x === 'string')
        .sort((a, b) => a.t - b.t);
      latest = {
        mediaId: pending.mediaId, revision: pending.revision,
        offsetMs: pending.offsetMs, lines,
      };
      pending = null;
      renderedIndex = -999;
      renderedRevision = -1;
    }
  }

  function safeOffset(value) {
    return Number.isInteger(value) && Math.abs(value) <= 120000 ? value : 0;
  }

  function tick() {
    const info = player.getMediaInformation();
    const id = info && info.customData && typeof info.customData.mediaId === 'string'
      ? info.customData.mediaId : null;
    if (id !== mediaId) {
      mediaId = id;
      renderedIndex = -999;
      renderedRevision = -1;
      // Old track lyrics cannot linger during transition or buffering.
      showEmpty('Finding synchronized lyrics…');
    }
    titleAndArtwork(info);
    const seconds = safeTime(player.getCurrentTimeSec());
    const duration = safeTime(player.getDurationSec());
    const state = player.getPlayerState();
    el.status.textContent = state === cast.framework.messages.PlayerState.PLAYING
      ? 'Playing on TV' : state === cast.framework.messages.PlayerState.BUFFERING
        ? 'Buffering…' : 'Paused';
    el.time.textContent = formatTime(seconds) + ' / ' + formatTime(duration);
    el.progress.style.width = (duration > 0
      ? (Math.min(1, seconds / duration) * 100).toFixed(2) : '0') + '%';

    const lyricState = latest && latest.mediaId === mediaId ? latest : null;
    if (!lyricState || !lyricState.lines.length) {
      if (renderedRevision !== (lyricState ? lyricState.revision : -1)) {
        showEmpty(lyricState ? 'No synchronized lyrics for this track.' : 'Finding synchronized lyrics…');
        renderedRevision = lyricState ? lyricState.revision : -1;
      }
      return;
    }
    const songMs = Math.max(0, seconds * 1000 - lyricState.offsetMs);
    const index = lineIndex(lyricState.lines, songMs);
    if (index !== renderedIndex || lyricState.revision !== renderedRevision) {
      renderedIndex = index;
      renderedRevision = lyricState.revision;
      renderLines(lyricState.lines, index);
    }
    animateWords(songMs);
  }

  ctx.addCustomMessageListener(NAMESPACE, onLyricsMessage);
  const options = new cast.framework.CastReceiverOptions();
  options.customNamespaces = { [NAMESPACE]: cast.framework.system.MessageType.JSON };
  ctx.start(options);
  window.setInterval(tick, 120);
  tick();
})();
