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
        sh 'printenv | sort'
        sh 'java -version' // print the java version being used, to aid in debugging build issues

        // if the build breaks, and it's necessary to temporarily pin to a known working gradle
        // version, can do e.g. --gradle-version=8.4
        sh 'JAVA_OPTS= ./gradlew wrapper --gradle-version=latest --distribution-type=bin'
        sh './gradlew --version' // print some basic gradle info, to aid in debugging build issues

        // make extra sure that no stale build artifacts are confounding things -- can revisit if builds are
        // unpleasantly slow
        sh 'JAVA_OPTS= ./gradlew --no-build-cache --no-configuration-cache clean check'
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
