epoch=$(date +%s)

if ! sed -i "s/EPOCH/${epoch}/g" deploy/guide/*.html; then
    exit 1
fi

if ! sed -i "s/EPOCH/${epoch}/g" deploy/index.html; then
    exit 2
fi
