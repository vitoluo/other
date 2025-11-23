#!/usr/bin/env bash
set -e

curl -L https://testingcf.jsdelivr.net/gh/MetaCubeX/meta-rules-dat@release/geoip.dat -o /etc/mosdns/geoip.dat
curl -L https://testingcf.jsdelivr.net/gh/MetaCubeX/meta-rules-dat@release/geosite.dat -o /etc/mosdns/geosite.dat
mosdns v2dat unpack-ip -o /etc/mosdns /etc/mosdns/geoip.dat:private,cn,cloudflare
mosdns v2dat unpack-domain -o /etc/mosdns /etc/mosdns/geosite.dat:private,category-ads-all,cn,gfw

curl -L https://testingcf.jsdelivr.net/gh/platformbuilds/Akamai-ASN-and-IPs-List@master/akamai_ip_list.lst -o /etc/mosdns/tmp_akamai_ip4
curl -L https://testingcf.jsdelivr.net/gh/platformbuilds/Akamai-ASN-and-IPs-List@master/akamai_ipv6_list.lst -o /etc/mosdns/tmp_akamai_ip6
cat tmp_akamai_ip4 tmp_akamai_ip6 > ip_akamai.txt
rm -f tmp_akamai_ip4 tmp_akamai_ip6