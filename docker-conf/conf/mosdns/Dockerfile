FROM irinesistiana/mosdns:latest

RUN sed -i 's/dl-cdn.alpinelinux.org/mirrors.tuna.tsinghua.edu.cn/g' /etc/apk/repositories \
    && apk update \
    && apk add --no-cache curl
COPY docker-entrypoint.sh update_v2dat.sh /
RUN chmod +x docker-entrypoint.sh update_v2dat.sh
ENTRYPOINT ["/docker-entrypoint.sh"]
