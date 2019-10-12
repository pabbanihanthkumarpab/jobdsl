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
      String basePath = 'CDAR-CI-CD/'+projectName+'/'+subModuleName;
      folder(basePath)
      
      job("$basePath/Deployment") {
           parameters {
             stringParam('environment', 'DEV', 'Name of the environemt that to be deployed DEV/UAT1/UAT3')
	     stringParam('version', '0.0.1-SNAPSHOT', 'version of the docker image that to be deployed')
             stringParam('repositoryName',  projectName , '')
	     stringParam('subModuleName', subModuleName , '')
           } 
	 
	      scm {
                git {
                  remote {
                           url('ssh://git@adlm.nielsen.com:7999/cm/azure-automation-nonprod.git')
			   credentials('uatbuild')
                         }
			
	           branches('*/master')		
                 
                  extensions {
                              relativeTargetDirectory('deployment-scripts')
                             }
                   }
	       }
	      
	      ant {
                   target('Deploy_Service')        
                   props('ssh_service_pwd': 'oi2l(3UWk', 'ssh_user': 'qabuild' , 'BuildHostServices' : 'dayrhecdad008.enterprisenet.org')
                   buildFile('${workspace}/deployment-scripts/${repositoryName}/build/build.xml')
	      }
            
        }
     }
}

def createPipelineJob( def projectName , def subModuleName , def type){

  println "projectName --> " + projectName;
String basePath = 'CDAR-CI-CD/'+projectName+'/'+subModuleName;

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
