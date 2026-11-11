/* Service worker: mode network-first + hapus cache lama saat instalasi. */
self.addEventListener("install", (e) => {
    e.waitUntil(
        caches.keys()
            .then((keys) => Promise.all(keys.map((k) => caches.delete(k))))
            .then(() => self.skipWaiting())
    );
});

self.addEventListener("fetch", (e) => {
    // Jangan intersep API (selalu butuh token terbaru)
    if (e.request.url.includes("/api/")) return;
    e.respondWith(
        fetch(e.request).catch(() => caches.match(e.request))
    );
});

self.addEventListener("activate", (e) => e.waitUntil(clients.claim()));
