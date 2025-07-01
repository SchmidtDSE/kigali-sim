/**
 * Simple service worker for the PWA which caches application resources.
 *
 * See https://developer.mozilla.org/en-US/docs/Web/Progressive_web_apps/Tutorials/js13kGames/
 *     Offline_Service_workers
 * See https://developer.mozilla.org/en-US/docs/Web/Progressive_web_apps/Guides/Caching
 *
 * Adapted from Global Plastics AI Policy Tool by Josh (https://joshsim.org/)
 * and the Global Plastics AI Policy Tool (https://global-plastics-tool.org/)
 * Original under BSD-3-Clause license.
 *
 * @license BSD
 */

const OLD_CACHES = [
    "KigaliSimOffline_v1",
    "KigaliSimOffline_v2",
    "KigaliSimOffline_v3",
];
const CACHE_NAME = "KigaliSimOffline_EPOCH";
const ESSENTIAL_FILES = [
    // Main application files
    "/index.html",
    "/privacy.html",
    "/manifest.json",
    
    // JavaScript files (from importmap and script tags)
    "/js/ace-mode-qubectalk.js",
    "/js/code_editor.js",
    "/js/engine_const.js",
    "/js/engine_number.js",
    "/js/engine_struct.js",
    "/js/main.js",
    "/js/report_data.js",
    "/js/results.js",
    "/js/ui_editor.js",
    "/js/ui_translator.js",
    "/js/user_config.js",
    "/js/wasm.worker.js",
    "/js/wasm_backend.js",
    "/js/year_matcher.js",
    
    // Intermediate files (generated language files)
    "/intermediate/index.js",
    "/intermediate/static/qubectalk.js",
    "/intermediate/QubecTalkLexer.js",
    "/intermediate/QubecTalkListener.js",
    "/intermediate/QubecTalkParser.js",
    "/intermediate/QubecTalkVisitor.js",
    "/intermediate/QubecTalk.interp",
    "/intermediate/QubecTalk.tokens",
    "/intermediate/QubecTalkLexer.interp",
    "/intermediate/QubecTalkLexer.tokens",
    
    // WASM files (essential for computation)
    "/wasm/KigaliSim.js",
    "/wasm/KigaliSim.wasm",
    "/wasm/KigaliSim.wasm-runtime.js",
    
    // Stylesheets and assets
    "/style/style.css",
    "/style/90-ring.svg",
    "/style/icon.png",
    
    // Third party libraries (from script tags in HTML)
    "/third_party/ace.min.js",
    "/third_party/chart.min.js",
    "/third_party/d3.min.js",
    "/third_party/ext-language_tools.js",
    "/third_party/ext-options.js",
    "/third_party/ext-prompt.js",
    "/third_party/ext-searchbox.js",
    "/third_party/tabby.min.js",
    "/third_party/tabby-ui.min.css",
    "/third_party/theme-textmate.js",
    "/third_party/theme-textmate-css.js",
    "/third_party/prism-autoloader.min.js",
    "/third_party/prism-core.min.js",
    "/third_party/prism-tomorrow.min.css",
    
    // Fonts (from CSS @font-face)
    "/third_party/publicsans/fonts/otf/PublicSans-Regular.otf",
    "/third_party/publicsans/fonts/webfonts/PublicSans-Regular.woff2",
    "/third_party/publicsans/fonts/webfonts/PublicSans-Bold.woff2",
    "/third_party/publicsans/fonts/webfonts/PublicSans-Medium.woff2",
    "/third_party/publicsans/fonts/webfonts/PublicSans-Light.woff2",
    
    // Example files (workshop sample)
    "/examples/workshop.qta",
    "/examples/basic.qta",
    "/examples/policies.qta",
    "/examples/ui/sim.qta",
    "/examples/ui/bau_single.qta",
    "/examples/ui/policy_single.qta",
    
    // Service worker files
    "/service_worker.js",
    "/js/sw_load.js",
];

/**
 * Determine if the resource is allowed to be cached.
 *
 * @returns True if cachable. False otherwise.
 */
function isCacheable(request) {
    const url = new URL(request.url);
    const isSentry = url.host.indexOf("sentry-cdn.com") != -1;
    const isTestFile = url.pathname.indexOf("version.txt") != -1;
    const isGuide = url.pathname.startsWith("/guide/");
    const nonCachable = isSentry || isTestFile || isGuide;
    const isCacheable = !nonCachable;
    return isCacheable;
}

/**
 * Make a request and update cache in background.
 *
 * @param request The request to make after which the internal cache will be updated.
 * @returns Response
 */
async function cacheFirstWithRefresh(request) {
    const url = new URL(request.url);
    const currentHost = self.location.hostname;

    const fetchResponsePromise = fetch(request).then(async (networkResponse) => {
        if (url.hostname === currentHost && networkResponse.ok && request.method === "GET") {
            const cache = await caches.open(CACHE_NAME);
            cache.put(url.pathname, networkResponse.clone());
        }
        return networkResponse;
    });

    if (currentHost === url.hostname) {
        return (await caches.match(url.pathname)) || (await fetchResponsePromise);
    } else {
        return (await fetchResponsePromise);
    }
}


/**
 * Intercept fetch
 */
self.addEventListener("fetch", (event) => {
    const request = event.request;
    if (isCacheable(request)) {
        event.respondWith(cacheFirstWithRefresh(request));
    }
});


// Thanks https://developer.mozilla.org/en-US/docs/Web/API/Cache
self.addEventListener("activate", (event) => {
    const expectedCacheNamesSet = new Set(Object.values(CACHE_NAME));
    event.waitUntil(
        caches.keys().then((cacheNames) =>
        Promise.all(
            cacheNames.map((cacheName) => {
            if (!expectedCacheNamesSet.has(cacheName)) {
                console.log("Deleting out of date cache:", cacheName);
                return caches.delete(cacheName);
            }
            }),
        ),
        ),
    );
});


/**
 * Schedule a cache fill after install.
 */
self.addEventListener("install", (e) => {
    const preloadCache = () => {
        const requests = ESSENTIAL_FILES.map((url) => new Request(url));
        requests.forEach((request) => cacheFirstWithRefresh(request));
        console.log("[Service Worker] Cache Loaded");
    };

    console.log("[Service Worker] Cache Queued");

    // In case someone is bouncing, don't add to download size
    setTimeout(preloadCache, 4000);
});