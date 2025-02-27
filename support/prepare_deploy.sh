mkdir deploy
cp -r intermediate deploy/intermediate
cp -r js deploy/js
cp -r style deploy/style
cp -r third_party deploy/third_party
cp index.html deploy/index.html
cp privacy.html deploy/privacy.html

rm -r deploy/third_party/publicsans