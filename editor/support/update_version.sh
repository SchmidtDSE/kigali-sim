epoch=$(date +%s)

if ! sed -i "s/EPOCH/${epoch}/g" deploy/guide/*.html; then
    exit 1
fi

if ! sed -i "s/EPOCH/${epoch}/g" deploy/index.html; then
    exit 2
fi

if ! sed -i "s/EPOCH/${epoch}/g" deploy/privacy.html; then
    exit 2
fi

if ! sed -i "s/EPOCH/${epoch}/g" deploy/js/wasm.worker.js; then
    exit 3
fi

if ! sed -i "s/EPOCH/${epoch}/g" deploy/js/wasm_backend.js; then
    exit 4
fi

# PWA cache invalidation - forces service worker to update and clear old caches
if ! sed -i "s/EPOCH/${epoch}/g" deploy/service_worker.js; then
    exit 5
fi

# Create version.txt file for update detection
# This file is intentionally NOT cached by the service worker
if ! echo "${epoch}" > deploy/version.txt; then
    exit 6
fi
