[ ! -e test/qunit.css ] && wget https://cdnjs.cloudflare.com/ajax/libs/qunit/2.19.4/qunit.min.css -O test/qunit.css
[ ! -e test/qunit.min.js ] && wget https://cdnjs.cloudflare.com/ajax/libs/qunit/2.19.4/qunit.min.js -O test/qunit.min.js

[ ! -e third_party ] && mkdir third_party

[ ! -e third_party/d3.min.js ] && wget https://cdnjs.cloudflare.com/ajax/libs/d3/7.9.0/d3.min.js -O third_party/d3.min.js

[ ! -e third_party/publicsans ] && mkdir third_party/publicsans

[ ! -e third_party/publicsans/public-sans-v2.001.zip ] && wget https://github.com/uswds/public-sans/releases/download/v2.001/public-sans-v2.001.zip -O third_party/publicsans/public-sans-v2.001.zip

if [ ! -e third_party/publicsans/fonts/otf/PublicSans-Regular.otf ]; then
  cd third_party/publicsans
  unzip public-sans-v2.001.zip
  cd ../..
fi

[ ! -e third_party/ace.min.js ] && wget https://cdnjs.cloudflare.com/ajax/libs/ace/1.36.2/ace.min.js -O third_party/ace.min.js
[ ! -e third_party/theme-textmate.js ] && wget https://cdnjs.cloudflare.com/ajax/libs/ace/1.36.2/theme-textmate.min.js -O third_party/theme-textmate.js
[ ! -e third_party/theme-textmate-css.js ] && wget https://cdnjs.cloudflare.com/ajax/libs/ace/1.36.2/theme-textmate-css.min.js -O third_party/theme-textmate-css.js
[ ! -e third_party/ext-searchbox.js ] && wget https://cdnjs.cloudflare.com/ajax/libs/ace/1.36.2/ext-searchbox.js -O third_party/ext-searchbox.js
[ ! -e third_party/ext-options.js ] && wget https://cdnjs.cloudflare.com/ajax/libs/ace/1.36.2/ext-options.js -O third_party/ext-options.js
[ ! -e third_party/ext-prompt.js ] && wget https://cdnjs.cloudflare.com/ajax/libs/ace/1.36.2/ext-prompt.js -O third_party/ext-prompt.js
[ ! -e third_party/ext-lanaguge_tools.js ] && wget https://cdnjs.cloudflare.com/ajax/libs/ace/1.36.2/ext-language_tools.js -O third_party/ext-language_tools.js

[ ! -e third_party/chart.min.js ] && wget https://cdn.jsdelivr.net/npm/chart.js -O third_party/chart.min.js
