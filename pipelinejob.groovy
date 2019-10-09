def createPipelineJob( def projectName , def subModuleName , def type){

//String basePath = 'CDAR-DEV/$projectName'

//folder("$basePath")
 println "projectName --> " + projectName;
String projectPath = 'CDAR-DEV/'+projectName;
String basePath = 'CDAR-DEV/'+projectName+'/'+subModuleName;
folder("CDAR-DEV")
folder(projectPath)
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
                            echo 'logic new 1'
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
