    import net.sf.json.groovy.JsonSlurper 
    import hudson.FilePath
    import hudson.*
    import tools.JobStructure
   
    def jsonSlurper = new JsonSlurper();
    GroovyShell shell = new GroovyShell()
    def js = new JobStructure() 
//    def data = jsonSlurper.parseText(new File("/var/jenkins_home/data.json").text);
     def data = jsonSlurper.parseText(readFileFromWorkspace("data.json"));
     println data;
       
    
    for (int i=0; i < data.size() ; i++ ){
        iterateEachProject ( data.get(i).projectName ,data.get(i).subModules , data.get(i).type);
    }

    def iterateEachProject (def projectName , def subModules , def type){
  
        def subProjects = subModules.split(',');
        for  ( int j = 0 ; j<subProjects.size() ; j++){  
		 
    		 js.createPipelineJob(projectName,subProjects.getAt(j),type);
        }
        
    }
	
	


