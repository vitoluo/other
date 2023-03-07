pipeline {
  agent any
  options {
    buildDiscarder logRotator(numToKeepStr: '5')
    parallelsAlwaysFailFast()
  }
  tools {
    maven 'maven'
  }
  environment {
    GIT_URL = 'http://www.example.com/java.git'
    GIT_CONF_URL = 'http://www.example.com/java.git'
    GIT_CRED = 'jenkins_git'
    BUILD_JAR_NAME = 'app.jar'
    PRODUCT_BASE_NAME = 'example'
    DOCKER_PRIVREPO = credentials('docker_priv_repo')
    DOCKER_ACCT = credentials('docker_deploy_acct')
    BUILD_SERVER = '192.168.1.2'
    DEPLOY_SERVERS = "[dev: '192.168.1.3', test: '192.168.1.4']"
  }
  parameters {
    gitParameter name: 'branch', description: 'git 分支', type: 'PT_BRANCH_TAG', defaultValue: 'test', selectedValue: 'DEFAULT', branchFilter: 'origin/(.*)', useRepository: '^.*java.git$'

    booleanParam name: 'updatePipeline', description: '更新流水线配置', defaultValue: false

    choice name: 'confType', description: '配置选择', choices: ['dev', 'test']
    
    choice name: 'deployServer', description: '发布服务器', choices: ['dev', 'test', 'buildOnly']
  }
  stages {
    stage('拉取代码') {
      steps {
        checkout scmGit(branches: [[name: params.branch]], extensions: [cloneOption(shallow: true, depth: 1), [$class: 'RelativeTargetDirectory', relativeTargetDir: 'code']], userRemoteConfigs: [[url: GIT_URL, credentialsId: GIT_CRED]])

        checkout scmGit(branches: [[name: 'master']], extensions: [cloneOption(shallow: true, depth: 1), [$class: 'RelativeTargetDirectory', relativeTargetDir: 'conf']], userRemoteConfigs: [[url: GIT_CONF_URL, credentialsId: GIT_CRED]])

        script {
          if(BUILD_NUMBER == '1'
          || params.updatePipeline == true) {
            echo '流水线初始化完成'
          }
        }
      }
    }
    stage('项目部署') {
      when {
        expression { BUILD_NUMBER != '1' }
        expression { params.updatePipeline == false }
      }
      stages {
        stage('打包') {
          steps {
            dir('code') {
              configFileProvider([configFile(fileId: 'GlobalMavenSettings', variable: 'MAVEN_SETTINGS')]) {
                sh 'mvn -s $MAVEN_SETTINGS clean package -Dmaven.test.skip=true'
              }
            }

            script {
              def docker_img_version = "${DOCKER_PRIVREPO}/${PRODUCT_BASE_NAME}/${JOB_NAME}-${params.branch}:${BUILD_NUMBER}"
              def docker_img_latest = "${DOCKER_PRIVREPO}/${PRODUCT_BASE_NAME}/${JOB_NAME}-${params.branch}:latest"

              def remote = [:]
              remote.name = 'build'
              remote.host = DOCKER_REMOTE_HOST
              remote.user = 'deploy'
              remote.allowAnyHosts = true

              withCredentials([sshUserPrivateKey(credentialsId: 'jenkins_ssh', keyFileVariable: 'identity')]) {
                remote.identityFile = identity
                sshPut remote: remote, from: "${WORKSPACE}/code/target/${BUILD_JAR_NAME}", into: "./${JOB_NAME}.jar"
                sshPut remote: remote, from: "${WORKSPACE}/conf/${params.confType}/${JOB_NAME}/application-docker.yaml", into: "./${JOB_NAME}-conf.yaml"
                writeFile encoding: 'utf-8', file: 'build.sh', text: """\
#!/usr/bin/env bash
set -e

mkdir -p \$HOME/docker-build/${JOB_NAME}
cd \$HOME/docker-build/${JOB_NAME}
mv \$HOME/${JOB_NAME}.jar ./app.jar
mv \$HOME/${JOB_NAME}-conf.yaml ./application-docker.yaml

cat >entrypoint.sh<<EOF
#!/bin/sh
set -e

jvmLogPath=\\\${APP_HOME}/logs/jvm
mkdir -p \\\$jvmLogPath

JAVA_OPTS="
-Xms\\\${APP_MEM}
-Xmx\\\${APP_MEM}
-Xlog:gc:\\\${jvmLogPath}/gc.log
-XX:ErrorFile=\\\${jvmLogPath}/err.log
-XX:HeapDumpPath=\\\${jvmLogPath}/heapdump.hprof
-XX:+HeapDumpOnOutOfMemoryError
-Dspring.profiles.active=\\\${SPRING_PROFILE}
\\\${JAVA_OPTS}
"

java \\\$JAVA_OPTS -jar \\\${APP_HOME}/app.jar \\\$JAVA_ARGS
EOF

cat >Dockerfile<<EOF
FROM ibm-semeru-runtimes:open-11-jdk-jammy

ARG GID=1000
ARG UID=1000

ENV APP_HOME=/app \
  APP_MEM=512M \
  SPRING_PROFILE=docker
# 虚拟机参数放在环境变量 JAVA_OPTS 里面
# java 参数放在环境变量 JAVA_ARGS 里面

RUN groupadd -g \\\${GID} app \
  && useradd -d \\\${APP_HOME} -g \\\${GID} -u \\\${UID} -m app

RUN sed -i 's#//.*archive.ubuntu.com#//mirrors.ustc.edu.cn#g' /etc/apt/sources.list \
  && sed -i 's#security.ubuntu.com#mirrors.ustc.edu.cn#g' /etc/apt/sources.list \
  && apt-get update \
  && apt-get install -y fonts-dejavu fontconfig \
  && rm -rf /var/lib/apt/lists/*

COPY entrypoint.sh /
RUN chmod +x /entrypoint.sh

USER app
WORKDIR \\\${APP_HOME}
EXPOSE 8080
ENTRYPOINT ["/entrypoint.sh"]

COPY app.jar application-docker.yaml ./
EOF

docker build -t ${docker_img_version} -t ${docker_img_latest} .
docker login ${DOCKER_PRIVREPO} -u${DOCKER_ACCT_USR} -p${DOCKER_ACCT_PSW}
docker push ${docker_img_version}
docker push ${docker_img_latest}
docker rmi ${docker_img_version} ${docker_img_latest}
"""
                sshScript remote: remote, script: 'build.sh'
              }
            }
          }
        }
        stage('部署') {
          when {
            expression { params.deployServer != 'buildOnly' }
          }
          steps {
            script {
              def servers = evaluate(DEPLOY_SERVERS)

              def remote = [:]
              remote.name = 'deploy'
              remote.user = 'deploy'
              remote.allowAnyHosts = true
              remote.host = servers[deployServer]

              withCredentials([sshUserPrivateKey(credentialsId: 'jenkins_ssh', keyFileVariable: 'identity')]) {
                remote.identityFile = identity
                writeFile encoding: 'utf-8', file: 'run.sh', text: """\
#!/usr/bin/env bash
set -e

mkdir -p \$HOME/docker/${PRODUCT_BASE_NAME}
cd \$HOME/docker/${PRODUCT_BASE_NAME}

if [[ -z \$(docker network ls | grep ${PRODUCT_BASE_NAME}) ]]
then
  docker network create ${PRODUCT_BASE_NAME}
fi

cat >${JOB_NAME}.yaml<<EOF
services:
  ${JOB_NAME}:
    container_name: ${JOB_NAME}
    restart: on-failure:1
    image: ${PRODUCT_BASE_NAME}/${JOB_NAME}-${params.branch}:${BUILD_NUMBER}
    networks:
      - ${PRODUCT_BASE_NAME}
    volumes:
      - /etc/timezone:/etc/timezone:ro
      - /etc/localtime:/etc/localtime:ro
      - ./data/share_files:/data/files
    environment:
      APP_MEM: 512M

networks:
  ${PRODUCT_BASE_NAME}:
    external: true
EOF

docker compose -f ${JOB_NAME}.yaml up -d
sleep 5

if [[ -z \$(docker ps | grep ${JOB_NAME} | awk -F "[ ]{2,}" '{print \$5}' | grep 'Up') ]]
then
  echo '部署失败'
  docker logs -n 100 ${JOB_NAME}
  exit 1
fi

echo '部署完成'
"""
                sshScript remote: remote, script: 'run.sh'
              }
            }
          }
        }
      }
    }
  }
}