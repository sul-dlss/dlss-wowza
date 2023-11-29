pipeline {
  agent any

  environment {
    HOME = "${env.WORKSPACE}"
    XDG_CONFIG_HOME = "${env.WORKSPACE}/.config"
  }

  stages {
    stage('Checkout') {
      steps {
        checkout scm
      }
    }

    stage('Test') {
      steps {
        sh 'JAVA_OPTS= ./gradlew clean check'
      }
    }

    stage('Publish artifacts') {
      when {
        branch "master"
      }

      steps {
        sh 'JAVA_OPTS= ./gradlew distTar'
        sh 'cp ./build/distributions/dlss-wowza-v*.tar /ci/artifacts/dlss-wowza/'
      }
    }
  }
}
