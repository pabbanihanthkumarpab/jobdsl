    import net.sf.json.groovy.JsonSlurper 
    import hudson.FilePath
    import hudson.*
   
   
    def jsonSlurper = new JsonSlurper();

  //  def data = jsonSlurper.parseText(new File("/var/jenkins_home/data.json").text);
    def data = jsonSlurper.parseText(readFileFromWorkspace("data.json"));
  
       
    
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

  println "projectName --> " + projectName;
String basePath = 'CDAR-DEV/'+projectName+'/'+subModuleName;

folder(basePath)


pipelineJob("$basePath/pipeline-job") {
  definition {
    cps {
      script(script(readFileFromWorkspace('template_pipeline.groovy')))
      sandbox()     
    }
  }
}
}
