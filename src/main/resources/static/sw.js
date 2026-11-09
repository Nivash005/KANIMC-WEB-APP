// KANIMC Offline Service Worker
const CACHE_NAME = 'kanimc-cache-v20261109_2';
const STATIC_ASSETS = [
    './',
    './index.html',
    './styles.css',
    './app.js',
    './gh_pages_adapter.js',
    './modal_logic.js',
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
    const url = new URL(event.request.url);

    // Bypass Service Worker for /api/ requests so gh_pages_adapter can handle them
    if (url.pathname.includes('/api/')) {
        return;
    }

    // Cache-first strategy for static assets
    event.respondWith(
        caches.match(event.request).then(cachedResponse => {
            if (cachedResponse) {
                // Fetch fresh in background
                fetch(event.request).then(networkResponse => {
                    if (networkResponse && networkResponse.status === 200) {
                        caches.open(CACHE_NAME).then(cache => cache.put(event.request, networkResponse));
                    }
                }).catch(() => {});
                return cachedResponse;
            }
            return fetch(event.request).then(networkResponse => {
                if (networkResponse && networkResponse.status === 200 && event.request.method === 'GET') {
                    const resClone = networkResponse.clone();
                    caches.open(CACHE_NAME).then(cache => cache.put(event.request, resClone));
                }
                return networkResponse;
            }).catch(() => {
                // If offline and requesting an HTML page, serve index.html
                if (event.request.headers.get('accept')?.includes('text/html')) {
                    return caches.match('./index.html') || caches.match('./');
                }
            });
        })
    );
});
