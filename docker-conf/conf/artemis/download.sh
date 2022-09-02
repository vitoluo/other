#!/bin/sh
ARTEMIS_VERSION="2.20.0"
ARTEMIS_URL="https://mirrors.ustc.edu.cn/apache/activemq/activemq-artemis/${ARTEMIS_VERSION}/apache-artemis-${ARTEMIS_VERSION}-bin.tar.gz"

curl ${ARTEMIS_URL} -o /opt/artemis.tar.gz
mkdir -p /opt/activemq-artemis
tar zxf /opt/artemis.tar.gz -C /opt/activemq-artemis --strip 1
rm -f /opt/artemis.tar.gz
