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
