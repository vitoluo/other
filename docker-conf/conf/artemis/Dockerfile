# Licensed to the Apache Software Foundation (ASF) under one
# or more contributor license agreements.  See the NOTICE file
# distributed with this work for additional information
# regarding copyright ownership.  The ASF licenses this file
# to you under the Apache License, Version 2.0 (the
# "License"); you may not use this file except in compliance
# with the License.  You may obtain a copy of the License at
#
#   http://www.apache.org/licenses/LICENSE-2.0
#
# Unless required by applicable law or agreed to in writing,
# software distributed under the License is distributed on an
# "AS IS" BASIS, WITHOUT WARRANTIES OR CONDITIONS OF ANY
# KIND, either express or implied.  See the License for the
# specific language governing permissions and limitations
# under the License.

# ActiveMQ Artemis

FROM openjdk:11
LABEL maintainer="Apache ActiveMQ Team"

ENV ARTEMIS_USER artemis \
    ARTEMIS_PASSWORD artemis \
    ANONYMOUS_LOGIN false \
    EXTRA_ARGS --http-host 0.0.0.0 --relax-jolokia

# add user and group for artemis
RUN groupadd -g 1000 -r artemis \
    && useradd -r -u 1000 -g artemis artemis \
    && sed -i 's#deb.debian.org#mirrors.ustc.edu.cn#g' /etc/apt/sources.list \
    && sed -i 's#security.debian.org#mirrors.ustc.edu.cn#g' /etc/apt/sources.list \
    && apt-get update \
    && apt-get install -y libaio1 \
    && rm -rf /var/lib/apt/lists/*

# Web Server
EXPOSE 8161 \
# JMX Exporter
    9404 \
# Port for CORE,MQTT,AMQP,HORNETQ,STOMP,OPENWIRE
    61616 \
# Port for HORNETQ,STOMP
    5445 \
# Port for AMQP
    5672 \
# Port for MQTT
    1883 \
#Port for STOMP
    61613

USER root

COPY ./download.sh ./docker-run.sh /

RUN mkdir /var/lib/artemis-instance \
    && chown -R artemis:artemis /var/lib/artemis-instance \
    && chmod +x /docker-run.sh \
    && bash /download.sh

USER artemis

# Expose some outstanding folders
VOLUME ["/var/lib/artemis-instance"]
WORKDIR /var/lib/artemis-instance

ENTRYPOINT ["/docker-run.sh"]
CMD ["run"]
