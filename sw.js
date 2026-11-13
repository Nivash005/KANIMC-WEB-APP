// KANIMC Offline Service Worker
const CACHE_NAME = 'kanimc-cache-v20261113_desktop_sync_v14';
const STATIC_ASSETS = [
    './',
    './index.html',
    './styles.css',
    './app.js',
    './gh_pages_adapter.js',
    './modal_logic.js',
    './logo.png',
    './favicon.ico',
    './favicon-32.png',
    './favicon-192.png',
    './apple-touch-icon.png',
    './data/accounts.json',
    './data/periods.json',
    './data/items.json',
    './data/places.json',
    './data/groups.json',
    './data/company.json',
    './data/settings.json'
];

self.addEventListener('install', event => {
    self.skipWaiting();
    event.waitUntil(
        caches.open(CACHE_NAME).then(cache => {
            console.log('[KANIMC SW] Pre-caching offline assets...');
            return cache.addAll(STATIC_ASSETS).catch(err => {
                console.warn('[KANIMC SW] Some assets failed pre-caching:', err);
            });
        })
    );
});

self.addEventListener('activate', event => {
    event.waitUntil(
        caches.keys().then(keys => {
            return Promise.all(
                keys.filter(k => k !== CACHE_NAME).map(k => caches.delete(k))
            );
        }).then(() => self.clients.claim())
    );
});

self.addEventListener('fetch', event => {
    // Only handle standard HTTP and HTTPS requests; ignore chrome-extension, file, etc.
    if (!event.request.url.startsWith('http://') && !event.request.url.startsWith('https://')) {
        return;
    }

    const url = new URL(event.request.url);

    // Bypass Service Worker for /api/ and external sync relay requests
    if (url.pathname.includes('/api/') || url.hostname.includes('ntfy.sh')) {
        return;
    }

    // NETWORK-FIRST FOR ALL HTML, JS, AND CSS: always load fresh code if online, fallback to cache if offline!
    event.respondWith(
        fetch(event.request).then(networkResponse => {
            if (networkResponse && networkResponse.status === 200 && event.request.method === 'GET') {
                const resClone = networkResponse.clone();
                caches.open(CACHE_NAME).then(cache => cache.put(event.request, resClone));
            }
            return networkResponse;
        }).catch(() => {
            return caches.match(event.request).then(cached => {
                if (cached) return cached;
                if (event.request.headers.get('accept')?.includes('text/html')) {
                    return caches.match('./index.html') || caches.match('./');
                }
            });
        })
    );
});
