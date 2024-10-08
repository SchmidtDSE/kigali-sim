mkdir deploy
cp -r intermediate deploy/intermediate
cp -r js deploy/js
cp -r style deploy/style
cp -r third_party deploy/third_party
cp index.html deploy/index.html

rm deploy/third_party/publicsans