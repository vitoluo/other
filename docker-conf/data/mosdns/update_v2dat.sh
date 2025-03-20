#!/usr/bin/env bash
set -e

curl -L https://testingcf.jsdelivr.net/gh/MetaCubeX/meta-rules-dat@release/geoip.dat -o /etc/mosdns/geoip.dat
curl -L https://testingcf.jsdelivr.net/gh/MetaCubeX/meta-rules-dat@release/geosite.dat -o /etc/mosdns/geosite.dat
mosdns v2dat unpack-ip -o /etc/mosdns /etc/mosdns/geoip.dat:private,cn
mosdns v2dat unpack-domain -o /etc/mosdns /etc/mosdns/geosite.dat:private,category-ads-all,cn,gfw
