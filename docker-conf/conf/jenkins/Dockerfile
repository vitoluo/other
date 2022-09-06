FROM jenkins/jenkins:2.346.3-2-lts-slim

RUN jenkins-plugin-cli -p "localization-zh-cn workflow-aggregator git-parameter"

USER root

RUN curl https://mirrors.ustc.edu.cn/apache/maven/maven-3/3.8.6/binaries/apache-maven-3.8.6-bin.tar.gz -o /opt/maven.tar.gz \
    && mkdir /opt/maven \
    && tar axf /opt/maven.tar.gz -C /opt/maven --strip 1 \
    && rm -f /opt/maven.tar.gz
COPY settings.xml /opt/maven/conf/settings.xml

RUN curl https://mirrors.ustc.edu.cn/node/latest-v14.x/node-v14.20.0-linux-x64.tar.gz -o /opt/nodejs.tar.gz \
    && mkdir /opt/nodejs \
    && tar axf /opt/nodejs.tar.gz -C /opt/nodejs --strip 1 \
    && rm -f /opt/nodejs.tar.gz

ARG DOCKER_GID=900
ARG GID=1000
ARG UID=1000
RUN groupadd -g ${DOCKER_GID} docker && usermod -a -G docker jenkins \
    && groupmod -g ${GID} jenkins && usermod -u ${UID} jenkins

USER jenkins
ENV PATH="/opt/maven/bin:${PATH}"
ENV PATH="/opt/nodejs/bin:${PATH}"