#!/usr/bin/env bash
set -e

curl -L https://cdn.jsdelivr.net/gh/Loyalsoldier/geoip@release/text/private.txt -o /etc/mosdns/ip_private.txt
curl -L https://cdn.jsdelivr.net/gh/Loyalsoldier/geoip@release/text/cn.txt -o /etc/mosdns/ip_cn.txt
curl -L https://cdn.jsdelivr.net/gh/Loyalsoldier/domain-list-custom@release/private.txt -o /etc/mosdns/site_private.txt
curl -L https://cdn.jsdelivr.net/gh/Loyalsoldier/v2ray-rules-dat@release/reject-list.txt -o /etc/mosdns/site_reject.txt
curl -L https://cdn.jsdelivr.net/gh/Loyalsoldier/v2ray-rules-dat@release/direct-list.txt -o /etc/mosdns/site_direct.txt
curl -L https://cdn.jsdelivr.net/gh/Loyalsoldier/v2ray-rules-dat@release/proxy-list.txt -o /etc/mosdns/site_proxy.txt