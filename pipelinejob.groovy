
def createPipelineJob( def projectName , def subModuleName , def type){

  println "projectName --> " + projectName;
String basePath = 'CDAR-DEV/'+projectName;

folder(basePath)


pipelineJob("$basePath/pipeline-job") {
  definition {
    cps {
      script('''
        pipeline {
            agent any
                stages {
                    stage('Stage 1') {
                        steps {
                            echo 'logic new'
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

