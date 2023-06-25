#!/bin/sh
set -e

curl -L https://ghproxy.com/https://github.com/Loyalsoldier/v2ray-rules-dat/releases/latest/download/geoip.dat -o /etc/mosdns/geoip.dat
curl -L https://ghproxy.com/https://github.com/Loyalsoldier/v2ray-rules-dat/releases/latest/download/geosite.dat -o /etc/mosdns/geosite.dat
mosdns v2dat unpack-ip -o /etc/mosdns /etc/mosdns/geoip.dat:private
mosdns v2dat unpack-ip -o /etc/mosdns /etc/mosdns/geoip.dat:cn
mosdns v2dat unpack-domain -o /etc/mosdns /etc/mosdns/geosite.dat:category-ads-all
mosdns v2dat unpack-domain -o /etc/mosdns /etc/mosdns/geosite.dat:cn
mosdns v2dat unpack-domain -o /etc/mosdns /etc/mosdns/geosite.dat:geolocation-!cn
