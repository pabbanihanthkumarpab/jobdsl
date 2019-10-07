folder('Folder1'){

pipelineJob('pipeline-job') {
  definition {
    cps {
      script('''
        pipeline {
            agent any
                stages {
                    stage('Stage 1') {
                        steps {
                            echo 'logic n'
                        }
                    }
                    stage('Stage 2') {
                        steps {
                            echo 'logic2'
                        }
                    }
                }
         
        }
      '''.stripIndent())
      sandbox()     
    }
  }
}
}
