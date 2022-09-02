#!/bin/sh
set -e

cat <<EOF | crontab -
0 0 * * * sh /update_v2dat.sh > /dev/stdout
EOF

if [ ! -f /etc/mosdns/geoip.dat ]; then
    sh /update_v2dat.sh
fi

crond && mosdns start --dir /etc/mosdns
