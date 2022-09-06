pipeline {
    agent any
    environment {
        GIT_URL = ''
        GIT_CRED = ''
        S_PATH = "${env.WORKSPACE}/build/*"
        T_PATH = "${env.JENKINS_HOME}/docker_data/project"
        APP_NAME = 'project'
    }
    parameters {
        gitParameter(name: 'BRANCH',
                type: 'PT_BRANCH_TAG',
                branchFilter: 'origin/(.*)',
                defaultValue: 'master')
    }
    stages {
        stage('拉取代码') {
            steps {
                checkout([$class           : 'GitSCM',
                          branches         : [[name: "${params.BRANCH}"]],
                          // git 子模块
                          extensions       : [[$class             : 'SubmoduleOption',
                                               parentCredentials  : true,
                                               recursiveSubmodules: true,
                                              ]],
                          userRemoteConfigs: [[url          : "${env.GIT_URL}",
                                               credentialsId: "${env.GIT_CRED}"]]
                ])
            }
        }
        stage('构建项目') {
            steps {
                sh 'npm i && npm run build'
            }
        }
        stage('发布项目') {
            steps {
                sh """
                rm -rf ${T_PATH} \
                && mkdir -p ${T_PATH} \
                && mv ${S_PATH} ${T_PATH} \
                && docker restart ${APP_NAME}
                """
            }
        }
    }
}