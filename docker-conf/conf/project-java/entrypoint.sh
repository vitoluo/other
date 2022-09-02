#!/bin/sh
set -e

mkdir -p $JVM_LOG_PATH

JAVA_OPTS="
-Xms${APP_MEM}
-Xmx${APP_MEM}
-Xlog:gc:${JVM_LOG_PATH}/gc.log
-XX:ErrorFile=${JVM_LOG_PATH}/err.log
-XX:HeapDumpPath=${JVM_LOG_PATH}/heapdump.hprof
-XX:+HeapDumpOnOutOfMemoryError
-Dspring.profiles.active=${SPRING_PROFILE}
"

java $JAVA_OPTS $OTHER_JAVA_OPTS -jar ${APP_PATH} ${JAVA_ARGS}
