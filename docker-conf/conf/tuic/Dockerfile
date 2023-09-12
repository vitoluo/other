FROM ubuntu:22.04

ARG UID
ARG PASSWD
WORKDIR /app
ADD https://github.com/EAimTY/tuic/releases/download/tuic-server-1.0.0/tuic-server-1.0.0-x86_64-unknown-linux-gnu /app/tuic
RUN chmod +x tuic \
    && echo "{\n\
  \"log_level\": \"warn\",\n\
  \"server\": \"0.0.0.0:1230\",\n\
  \"users\": {\n\
    \"$UID\": \"$PASSWD\"\n\
  },\n\
  \"certificate\": \"/app/cert.crt\",\n\
  \"private_key\": \"/app/key.pem\",\n\
  \"congestion_control\": \"bbr\",\n\
  \"alpn\": [\"h3\"]\n\
}" >/app/config.json
EXPOSE 1230/udp
CMD [ "/app/tuic", "-c", "config.json" ]