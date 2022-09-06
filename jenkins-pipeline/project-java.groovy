pipeline {
    agent any
    environment {
        GIT_URL = ''
        GIT_CRED = ''
        S_PATH = "${env.WORKSPACE}/target/app.jar"
        T_PATH = "${env.JENKINS_HOME}/docker_data/project/app.jar"
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
                          userRemoteConfigs: [[url          : "${env.GIT_URL}",
                                               credentialsId: "${env.GIT_CRED}"]]
                ])
            }
        }
        stage('构建项目') {
            steps {
                sh 'mvn clean package -Dmaven.test.skip=true'
            }
        }
        stage('发布项目') {
            steps {
                sh """
                mv ${S_PATH} ${T_PATH} \
                && docker restart ${APP_NAME}
                """
            }
        }
    }
}