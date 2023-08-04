#!/usr/bin/env bash
set -e

curl -L https://ghproxy.com/https://github.com/Loyalsoldier/v2ray-rules-dat/releases/latest/download/geoip.dat -o /etc/mosdns/geoip.dat
curl -L https://ghproxy.com/https://github.com/Loyalsoldier/v2ray-rules-dat/releases/latest/download/geosite.dat -o /etc/mosdns/geosite.dat
mosdns v2dat unpack-ip -o /etc/mosdns /etc/mosdns/geoip.dat:private,cn
mosdns v2dat unpack-domain -o /etc/mosdns /etc/mosdns/geosite.dat:category-ads-all,cn,geolocation-!cn
