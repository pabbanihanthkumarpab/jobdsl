    import net.sf.json.groovy.JsonSlurper 
    import hudson.FilePath
    import hudson.*
   
   
    def jsonSlurper = new JsonSlurper();

  
    def data = jsonSlurper.parseText(readFileFromWorkspace("data.json"));
  
       
    
    for (int i=0; i < data.size() ; i++ ){
        iterateEachProject ( data.get(i).projectName ,data.get(i).subModules , data.get(i).type);
    }

    def iterateEachProject (def projectName , def subModules , def type){
  
        def subProjects = subModules.split(',');
        for  ( int j = 0 ; j<subProjects.size() ; j++){  
		 
    		createPipelineJob(projectName,subProjects.getAt(j),type);
		createDeploymentJob(projectName,subProjects.getAt(j),type);
        }
        
    }
	
	

 def createDeploymentJob (def projectName , def subModuleName , def type){
      println "projectName --> " + projectName;
      String basePath = 'CDAR-CICD/'+projectName+'/'+subModuleName;
      folder('CDAR-CICD')
      folder("CDAR-CICD/$projectName")
      folder(basePath)
      
      job("$basePath/Deployment") {
           parameters {
             stringParam('environment', 'DEV', 'Name of the environemt that to be deployed DEV/UAT1/UAT3')
	     stringParam('version', '0.0.1-SNAPSHOT', 'version of the docker image that to be deployed')

           } 
	 
	      scm {
                git {
                  remote {
                           url('ssh://git@adlm.nielsen.com:7999/cm/azure-automation-nonprod.git')
			   credentials('87ba1f81-95e1-4ee0-b89a-e69f8b99cd70')
                         }
			
	           branches('*/master')		
                 
                  extensions {
                              relativeTargetDirectory('deployment-scripts')
                             }
                   }
	       }
	     
	      environmentVariables{
		env('repositoryName', projectName)
	        env('subModuleName', subModuleName)	      
		//propertiesFile('${workspace}/deployment-scripts/common-scripts/common-env.properties')      
	      }
	      steps { 
		   envInjectBuilder {
                         propertiesFilePath('deployment-scripts/common-scripts/common-env.properties')
			 propertiesContent('')
                      } 
	         ant {
                     target('Deploy_Service')        
                     props('env_pass' : '"ssh_${environment}_password"' , 'ssh_service_pwd' : '"${!env_pass}"' , 'ssh_user' : 'docker' , 'repositoryName' : '$repositoryName' , 'subModuleName' : '$subModuleName' , 'env' : '$environment' , 'version' : '$version')
                     buildFile('${workspace}/deployment-scripts/common-scripts/build.xml')
	         }
	      }
            
        }
     }

def createPipelineJob( def projectName , def subModuleName , def type){

  println "projectName --> " + projectName;
String basePath = 'CDAR-CICD/'+projectName+'/'+subModuleName;
folder('CDAR-CICD')
folder("CDAR-CICD/$projectName")
folder(basePath)


pipelineJob("$basePath/pipeline-job") {
parameters {
        stringParam('repositoryName', projectName, 'Repository Name')
	stringParam('subModuleName',  subModuleName,  'Sub Module Name')
	booleanParam('createRelease', false, 'Check to create release version')
	stringParam('releaseVersion', '', 'Fill the Release version')
	
 }
  definition {
    cps {
      script(readFileFromWorkspace('template_pipeline.groovy'))
      sandbox()     
    }
  }
}
}
