#!/bin/sh
set -e

if [ -z "$(apk list --installed curl)" ]
then
    sed -i 's/dl-cdn.alpinelinux.org/mirrors.tuna.tsinghua.edu.cn/g' /etc/apk/repositories
    apk update && apk add --no-cache curl
fi

cat <<EOF | crontab -
0 0 * * * sh /etc/mosdns/update_v2dat.sh > /dev/stdout
EOF

if [ ! -f /etc/mosdns/geoip.dat ]; then
    sh /etc/mosdns/update_v2dat.sh
fi

crond && mosdns start --dir /etc/mosdns