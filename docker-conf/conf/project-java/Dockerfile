FROM openjdk:11-slim

ENV APP_HOME=/app \
    APP_PATH=${APP_HOME}/app.jar \
    JVM_LOG_PATH=${APP_HOME}/logs/jvm \
    APP_MEM=512M \
    SPRING_PROFILE=prod
# 其他虚拟机参数放在环境变量 OTHER_JAVA_OPTS 里面
# java 参数放在环境变量 JAVA_ARGS 里面

COPY entrypoint.sh /
RUN chmod +x /entrypoint.sh \
    && useradd -d ${APP_HOME} -m app

RUN sed -i 's#deb.debian.org#mirrors.ustc.edu.cn#g' /etc/apt/sources.list \
    && sed -i 's#security.debian.org#mirrors.ustc.edu.cn#g' /etc/apt/sources.list \
    && apt-get update \
    && apt-get install -y fonts-dejavu fontconfig \
    && rm -rf /var/lib/apt/lists/*

USER app
WORKDIR ${APP_HOME}
VOLUME ${APP_HOME}
EXPOSE 8080
ENTRYPOINT ["/entrypoint.sh"]
