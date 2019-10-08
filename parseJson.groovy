    import net.sf.json.groovy.JsonSlurper  

    def jsonSlurper = new JsonSlurper();
    def data = jsonSlurper.parseText(new File("/var/jenkins_home/data.json").text);
 
    
    for (int i=0; i < data.size() ; i++ ){
        iterateEachProject ( data.get(i).projectName ,data.get(i).subModules , data.get(i).type);
    }

    def iterateEachProject (def projectName , def subModules , def type){
  
        def subProjects = subModules.split(',');
        for  ( int j = 0 ; j<subProjects.size() ; j++){    
    		  createPipelineJob(projectName,subProjects.getAt(j),type);
        }
        
    }
	
	
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

