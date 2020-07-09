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
        sh './gradlew'
      }
    }
    
    stage('Publish artifacts') {
      steps {
        sh 'cp ./build/distributions/dlss-wowza-v*.tar /ci/artifacts/dlss-wowza/'
      }
    }
  }
}
