epoch=$(date +%s)
sed -i "s/EPOCH/${epoch}/g" deploy/index.html