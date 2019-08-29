@Library('jenkins_lib')_
pipeline {
  agent any
 
  environment {
    // Define global environment variables in this section
    SLACK_CHANNEL = 'jenkins-misc-alerts'
    ARCHIVE_PATH = "target/**/*.tar.gz"
    SONAR_PATH = './'

 
  }
  stages {
    stage("Define Release version"){
      steps {
      script {
       //Global Lib for Environment Versions Definition
        versionDefine('pom.xml')
        }
      }
    }
    stage("Compile, Build and Test") {
      steps {
      script {
        echo "Running Build and Test"
        sh 'mvn clean install -Ppackage'
             }
      }
    }
    stage('SonarQube analysis') {
    steps {
      script {
        //Global Lib for Sonarqube runnner JAVA
        sonarqube(env.SONAR_PATH)
      }
    }
    }

    
 
}
  post {
       always {
 
          postBuild(env.ARCHIVE_PATH)
         //Global Lib for post build actions eg: artifacts archive
 
          slackalert(env.SLACK_CHANNEL)
         //Global Lib for slack alerts
 
      }
    }
}
