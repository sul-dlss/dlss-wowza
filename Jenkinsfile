pipeline {
  agent any

  stages {
    stage('Checkout') {
      steps {
        checkout scm
      }
    }
    
    stage('Build') {
      steps {
        sh "pwd -P"
        dir("${env.WORKSPACE}") {
          sh "pwd -P"
          sh './gradlew distTar'
        }
        sh "pwd -P"
      }
    }
    
    stage('Publish artifacts') {
      steps {
        sh 'cp ./build/distributions/dlss-wowza-v*.tar /ci/artifacts/dlss-wowza/'
      }
    }
  }
}
