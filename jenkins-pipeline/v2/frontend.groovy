pipeline {
  agent any
  options {
    buildDiscarder logRotator(numToKeepStr: '5')
    parallelsAlwaysFailFast()
  }
  tools {
    nodejs 'nodejs'
  }
  environment {
    GIT_URL = 'http://www.example.com/client.git'
    GIT_CONF_URL = 'http://www.example.com/project-config.git'
    GIT_CRED = 'jenkins_git'
    PRODUCT_BASE_NAME = 'example'
    DOCKER_PRIVREPO = credentials('docker_priv_repo')
    DOCKER_ACCT = credentials('docker_deploy_acct')
    BUILD_SERVER = '192.168.1.2'
    DEPLOY_SERVERS = "[dev: '192.168.1.3', test: '192.168.1.4']"
  }
  parameters {
    gitParameter name: 'branch', description: 'git 分支', type: 'PT_BRANCH_TAG', defaultValue: 'master', selectedValue: 'DEFAULT', branchFilter: 'origin/(.*)', useRepository: '^.*client.git$'

    booleanParam name: 'updatePipeline', description: '更新流水线配置', defaultValue: false

    choice name: 'confType', description: '配置选择', choices: ['dev', 'test']
    
    choice name: 'deployServer', description: '发布服务器', choices: ['dev', 'test', 'buildOnly']
  }
  stages {
    stage('拉取代码') {
      steps {
        checkout scmGit(branches: [[name: params.branch]], extensions: [submodule(recursiveSubmodules: true, parentCredentials: true, shallow: true, depth: 1), cloneOption(shallow: true, depth: 1), [$class: 'RelativeTargetDirectory', relativeTargetDir: 'code']], userRemoteConfigs: [[url: GIT_URL, credentialsId: GIT_CRED]])

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
        stage('编译项目') {
          steps {
            dir('code') {
              sh 'bash build.sh'
            }
          }
        }
        stage('生成 docker 镜像') {
          steps {
            sh 'mv code/build/ client/'
            sh 'tar acf data.tar.gz client/ --remove-files'

            sh 'mkdir conf.d'
            sh 'cp conf/nginx/* conf.d/'
            sh "cp conf/${params.confType}/${JOB_NAME}/* conf.d/"
            sh 'tar acf conf.tar.gz conf.d --remove-files'

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
                sshPut remote: remote, from: "data.tar.gz", into: "./${JOB_NAME}-data.tar.gz"
                sshPut remote: remote, from: "conf.tar.gz", into: "./${JOB_NAME}-conf.tar.gz"
                writeFile encoding: 'utf-8', file: 'build.sh', text: """\
#!/usr/bin/env bash
set -e

mkdir -p \$HOME/docker-build/${JOB_NAME}
cd \$HOME/docker-build/${JOB_NAME}
mv \$HOME/${JOB_NAME}-data.tar.gz ./data.tar.gz
mv \$HOME/${JOB_NAME}-conf.tar.gz ./conf.tar.gz

cat >Dockerfile<<EOF
FROM yematech/tengine:2.4.0

ADD conf.tar.gz /etc/nginx/
ADD data.tar.gz /var/www/
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
    ports:
    - 4017:4017
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