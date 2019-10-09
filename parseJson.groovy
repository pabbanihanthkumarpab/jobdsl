    import net.sf.json.groovy.JsonSlurper 
    import hudson.FilePath
    import hudson.*
    def jobFun = load 'pipelinejob.groovy'

   // evaluate(new File("pipelinejob.groovy"))
    //evaluate(new File("./pipelinejob.groovy"))

    def jsonSlurper = new JsonSlurper();

//    def data = jsonSlurper.parseText(new File("/var/jenkins_home/data.json").text);
     def data = jsonSlurper.parseText(readFileFromWorkspace("/var/jenkins_home/data.json").text);
       
    
    for (int i=0; i < data.size() ; i++ ){
        iterateEachProject ( data.get(i).projectName ,data.get(i).subModules , data.get(i).type);
    }

    def iterateEachProject (def projectName , def subModules , def type){
  
        def subProjects = subModules.split(',');
        for  ( int j = 0 ; j<subProjects.size() ; j++){    
    		  jobFun.createPipelineJob(projectName,subProjects.getAt(j),type);
        }
        
    }
	
	


