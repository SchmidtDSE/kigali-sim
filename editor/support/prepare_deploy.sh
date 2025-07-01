mkdir deploy
cp -r intermediate deploy/intermediate
cp -r js deploy/js
cp -r style deploy/style
cp -r third_party deploy/third_party
cp -r wasm deploy/wasm
cp index.html deploy/index.html
cp privacy.html deploy/privacy.html
cp manifest.json deploy/manifest.json
cp service_worker.js deploy/service_worker.js
cp ../llms.txt deploy/llms.txt

# Keep only the specific font file needed
find deploy/third_party/publicsans -type f ! -path "*/fonts/otf/PublicSans-Regular.otf" -delete
find deploy/third_party/publicsans -type d -empty -delete