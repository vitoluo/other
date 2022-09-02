FROM jenkins/jenkins:2.346.1-lts-slim

RUN jenkins-plugin-cli -p "localization-zh-cn workflow-aggregator git-parameter"

USER root

RUN sed -i 's#deb.debian.org#mirrors.ustc.edu.cn#g' /etc/apt/sources.list \
    && sed -i 's#security.debian.org#mirrors.ustc.edu.cn#g' /etc/apt/sources.list \
    && apt-get update && apt-get install -y docker-compose \
    && rm -rf /var/lib/apt/lists/*

RUN curl https://mirrors.ustc.edu.cn/apache/maven/maven-3/3.8.6/binaries/apache-maven-3.8.6-bin.tar.gz -o /opt/maven.tar.gz \
    && mkdir /opt/maven \
    && tar axf /opt/maven.tar.gz -C /opt/maven --strip 1 \
    && rm -f /opt/maven.tar.gz
COPY settings.xml /opt/maven/conf/settings.xml

ARG DOCKER_GID=900
ARG GID=1000
ARG UID=1000
RUN groupmod -g ${DOCKER_GID} docker && usermod -a -G docker jenkins \
    && groupmod -g ${GID} jenkins && usermod -u ${UID} jenkins

USER jenkins
ENV PATH="/opt/maven/bin:${PATH}"
