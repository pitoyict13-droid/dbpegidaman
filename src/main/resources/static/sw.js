/* Service worker sederhana: mode network-first agar data selalu terbaru. */
self.addEventListener("install", (e) => self.skipWaiting());

self.addEventListener("fetch", (e) => {
    // Jangan intersep API (selalu butuh token terbaru)
    if (e.request.url.includes("/api/")) return;
    e.respondWith(
        fetch(e.request).catch(() => caches.match(e.request))
    );
});

self.addEventListener("activate", (e) => e.waitUntil(clients.claim()));
