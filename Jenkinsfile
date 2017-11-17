node('docker') {
     stage('checkout') {
        checkout scm
     }
     stage('maven') {
        sh 'mvn clean package'
     }
     stage('archive') {
        archiveArtifacts 'target/docker-fixtures-1.2-SNAPSHOT.jar'
      }
     stage('surefire-report') {
        junit 'target/surefire-reports/*.xml'
     }
}