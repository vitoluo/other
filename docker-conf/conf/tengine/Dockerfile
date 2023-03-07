FROM alpine:3.16 as builder

RUN sed -i 's/dl-cdn.alpinelinux.org/mirrors.ustc.edu.cn/g' /etc/apk/repositories \
    && apk update \
    && apk add alpine-sdk pcre-dev openssl-dev zlib-dev

ARG TENGINE_VERSION=2.4.0

RUN mkdir -p /opt/tengine \
    && curl -L "https://ghproxy.com/https://github.com/alibaba/tengine/archive/$TENGINE_VERSION.tar.gz" -o /tengine.tar.gz \
    && tar axf /tengine.tar.gz --strip-components=1 -C /opt/tengine

ARG CONFIG="--prefix=/etc/nginx \
--sbin-path=/usr/sbin/nginx \
--modules-path=/usr/lib/nginx/modules \
--conf-path=/etc/nginx/nginx.conf \
--error-log-path=/var/log/nginx/error.log \
--http-log-path=/var/log/nginx/access.log \
--pid-path=/var/run/nginx.pid \
--lock-path=/var/run/nginx.lock \
--http-client-body-temp-path=/var/cache/nginx/client_temp \
--http-proxy-temp-path=/var/cache/nginx/proxy_temp \
--http-fastcgi-temp-path=/var/cache/nginx/fastcgi_temp \
--http-uwsgi-temp-path=/var/cache/nginx/uwsgi_temp \
--http-scgi-temp-path=/var/cache/nginx/scgi_temp \
--with-http_gzip_static_module \
--add-module=modules/ngx_http_upstream_dynamic_module \
"
RUN cd /opt/tengine \
    && ./configure $CONFIG \
    && make -j$(getconf _NPROCESSORS_ONLN) \
    && make install \
    && rm -rf /etc/nginx/html/ \
    && strip /usr/sbin/nginx*

FROM alpine:3.16

RUN sed -i 's/dl-cdn.alpinelinux.org/mirrors.ustc.edu.cn/g' /etc/apk/repositories \
    && apk update \
    && apk add pcre openssl zlib \
    && addgroup -g 101 -S nginx \
    && adduser -S -D -u 101 -h /var/cache/nginx -s /sbin/nologin -G nginx -g nginx nginx \
    && mkdir -p /var/log/nginx \
    && touch /var/log/nginx/access.log /var/log/nginx/error.log \
    && chown -R nginx:nginx /var/log/nginx \
    && ln -sf /dev/stdout /var/log/nginx/access.log \
    && ln -sf /dev/stderr /var/log/nginx/error.log

COPY --from=builder /etc/nginx /etc/nginx
COPY --from=builder /usr/sbin/nginx /usr/sbin/nginx
COPY nginx.conf /etc/nginx

EXPOSE 80 443
CMD ["nginx", "-g", "daemon off;"]