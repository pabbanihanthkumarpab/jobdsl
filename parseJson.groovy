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