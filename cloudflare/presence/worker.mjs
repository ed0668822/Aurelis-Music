// SPDX-FileCopyrightText: © 2026 Aurelis
// SPDX-License-Identifier: LGPL-3.0-or-later

import { DurableObject } from "cloudflare:workers";

const PRESENCE_PATH = "/v1/presence";
const STATS_PATH = "/v1/stats/live";
const STATS_CACHE_SECONDS = 20;
const MAX_CONNECTIONS = 4096;

export default {
  async fetch(request, env, ctx) {
    const url = new URL(request.url);
    if (url.pathname !== PRESENCE_PATH && url.pathname !== STATS_PATH) {
      return new Response(null, { status: 404 });
    }
    if (request.method !== "GET") {
      return new Response(null, { status: 405, headers: { Allow: "GET" } });
    }

    const presence = env.PRESENCE.getByName("aurelis-live");
    if (url.pathname === PRESENCE_PATH) {
      if (request.headers.get("Upgrade")?.toLowerCase() !== "websocket") {
        return new Response(null, { status: 426 });
      }
      // Browser pages only read the aggregate. Android's client has no Origin.
      if (request.headers.has("Origin")) {
        return new Response(null, { status: 403 });
      }
      const address = request.headers.get("CF-Connecting-IP") || "unknown";
      if (!(await env.PRESENCE_LIMIT.limit({ key: address })).success) {
        return new Response(null, { status: 429, headers: { "Retry-After": "60" } });
      }
      return presence.fetch(request);
    }

    // A single cache entry per host and edge location, regardless of query params.
    const cacheKey = new Request(new URL(STATS_PATH, url));
    const cached = await caches.default.match(cacheKey);
    if (cached) return cached;
    const response = Response.json(
      { activeSessions: await presence.activeSessions() },
      {
        headers: {
          "Access-Control-Allow-Origin": "*",
          "Cache-Control": `public, max-age=${STATS_CACHE_SECONDS}`,
        },
      },
    );
    ctx.waitUntil(caches.default.put(cacheKey, response.clone()));
    return response;
  },
};

export class AurelisPresence extends DurableObject {
  fetch() {
    if (this.ctx.getWebSockets().length >= MAX_CONNECTIONS) {
      return new Response(null, { status: 503, headers: { "Retry-After": "60" } });
    }

    const [client, server] = Object.values(new WebSocketPair());
    this.ctx.acceptWebSocket(server);
    return new Response(null, { status: 101, webSocket: client });
  }

  activeSessions() {
    return this.ctx.getWebSockets().filter((socket) => socket.readyState === WebSocket.OPEN).length;
  }

  webSocketMessage(socket) {
    // The runtime handles protocol pings. There is no application payload.
    socket.close(1008, "Presence sockets do not accept messages");
  }
}
