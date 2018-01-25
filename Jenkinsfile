pipeline {
    options {
        buildDiscarder(logRotator(numToKeepStr: '20'))
        timeout(time: 1, unit: 'HOURS')
    }
    // cf. https://github.com/jenkins-infra/documentation/blob/master/ci.adoc
    agent {
        label 'docker'
    }
    tools {
        jdk 'jdk8'
        maven 'mvn'
    }
    stages {
        stage('main') {
            steps {
                sh 'mvn -B clean verify'
            }
            post {
                success {
                    junit '**/target/surefire-reports/TEST-*.xml'
                }
            }
        }
    }
}
