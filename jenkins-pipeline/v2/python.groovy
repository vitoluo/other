pipeline {
  agent any
  options {
    buildDiscarder logRotator(numToKeepStr: '5')
    parallelsAlwaysFailFast()
  }
  environment {
    GIT_URL = 'http://www.example.com/python.git'
    GIT_DATA_URL = 'http://www.example.com/data.git'
    GIT_CRED = 'jenkins_git'
    PRODUCT_BASE_NAME = 'example'
    DOCKER_PRIVREPO = credentials('docker_priv_repo')
    DOCKER_ACCT = credentials('docker_deploy_acct')
    BUILD_SERVER = '192.168.1.2'
    DEPLOY_SERVERS = "[dev: '192.168.1.3', test: '192.168.1.4']"
  }
  parameters {
    gitParameter name: 'branch', description: 'git 分支', type: 'PT_BRANCH_TAG', defaultValue: 'test', selectedValue: 'DEFAULT', branchFilter: 'origin/(.*)', useRepository: '^.*python.git$'

    booleanParam name: 'updatePipeline', description: '更新流水线配置', defaultValue: false

    choice name: 'confType', description: '配置选择', choices: ['dev', 'test']
    
    choice name: 'deployServer', description: '发布服务器', choices: ['dev', 'test', 'buildOnly']
  }
  stages {
    stage('拉取代码') {
      steps {
        checkout scmGit(branches: [[name: params.branch]], extensions: [cloneOption(shallow: true, depth: 1), [$class: 'RelativeTargetDirectory', relativeTargetDir: 'code']], userRemoteConfigs: [[url: GIT_URL, credentialsId: GIT_CRED]])

        checkout scmGit(branches: [[name: 'master']], extensions: [cloneOption(shallow: true, depth: 1), [$class: 'RelativeTargetDirectory', relativeTargetDir: 'data']], userRemoteConfigs: [[url: GIT_DATA_URL, credentialsId: GIT_CRED]])

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
        stage('生成 docker 镜像') {
          steps {
            sh 'mkdir -p extra_files/cur extra_files/synonyms'
            sh "cp -f data/${params.dataType}/crf_bqitem.pkl extra_files/cur/"
            sh "cp -f data/${params.dataType}/crf_bqtitle.pkl extra_files/cur/"
            sh "cp -f data/${params.dataType}/crf_pre_bqitem.pkl extra_files/cur/"
            sh "cp -f data/${params.dataType}/post_synonyms.txt extra_files/synonyms/"
            sh "cp -f data/${params.dataType}/pre_synonyms.txt extra_files/synonyms/"

            sh 'tar --exclude=code/.git -acf code.tar.gz code/'
            sh 'tar -acf data.tar.gz extra_files/ --remove-files'

            script {
              def docker_img_version = "${DOCKER_PRIVREPO}/${PRODUCT_BASE_NAME}/${JOB_NAME}-${params.branch}:${BUILD_NUMBER}"
              def docker_img_latest = "${DOCKER_PRIVREPO}/${PRODUCT_BASE_NAME}/${JOB_NAME}-${params.branch}:latest"

              def remote = [:]
              remote.name = 'build'
              remote.host = BUILD_SERVER
              remote.user = 'deploy'
              remote.allowAnyHosts = true

              withCredentials([sshUserPrivateKey(credentialsId: 'jenkins_ssh', keyFileVariable: 'identity')]) {
                remote.identityFile = identity
                sshPut remote: remote, from: "code.tar.gz", into: "./${JOB_NAME}-code.tar.gz"
                sshPut remote: remote, from: "data.tar.gz", into: "./${JOB_NAME}-data.tar.gz"
                writeFile encoding: 'utf-8', file: 'build.sh', text: """\
#!/usr/bin/env bash
set -e

mkdir -p \$HOME/docker-build/${JOB_NAME}
cd \$HOME/docker-build/${JOB_NAME}
mv \$HOME/${JOB_NAME}-code.tar.gz ./code.tar.gz
mv \$HOME/${JOB_NAME}-data.tar.gz ./data.tar.gz

cat >entrypoint.sh<<EOF
#!/bin/sh
set -e

/app/main.bin \\\${UVICORN_APP} --host \\\${HOST} --port \\\${PORT} \\\${UVICORN_OPTIONS}
EOF

cat >Dockerfile<<EOF
FROM python:3.10-alpine3.16 as builder

RUN --mount=type=cache,target=/root/pipcache,sharing=locked \
    sed -i 's/dl-cdn.alpinelinux.org/mirrors.ustc.edu.cn/g' /etc/apk/repositories \
    && apk update \
    && apk add alpine-sdk ccache patchelf \
    && pip config set global.index-url https://mirrors.cloud.tencent.com/pypi/simple \
    && pip config set global.cache-dir /root/pipcache \
    && pip install nuitka ordered_set zstandard

ADD code.tar.gz /
ENV NUITKA_CACHE_DIR=/root/nuitkacache
RUN --mount=type=cache,target=/root/pipcache,sharing=locked \
    --mount=type=cache,target=/root/nuitkacache,sharing=locked \
     cd /code \
    && pip install -r requirements.txt \
    && python -m nuitka --standalone --include-package=app --include-module=pycrfsuite._dumpparser --include-module=pycrfsuite._logparser --output-dir=dist main.py

FROM alpine:3.16

ARG GID=1000
ARG UID=1000
ARG APP_HOME=/app
ENV HOST=0.0.0.0
ENV PORT=8000

RUN addgroup -g \\\${GID} app \
    && adduser -g app -h \\\${APP_HOME} -H -G app -u \\\${UID} -D app

USER app
WORKDIR \\\${APP_HOME}

COPY --chown=app:app entrypoint.sh ./
RUN chmod +x entrypoint.sh 

EXPOSE \\\${PORT}
ENTRYPOINT ["./entrypoint.sh"]

COPY --chown=app:app --from=builder /code/dist/main.dist \\\${APP_HOME}
ADD --chown=app:app data.tar.gz \\\${APP_HOME}
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
    environment:
      UVICORN_APP: app.api:app
    networks:
      - ${PRODUCT_BASE_NAME}
    volumes:
      - /etc/timezone:/etc/timezone:ro
      - /etc/localtime:/etc/localtime:ro

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