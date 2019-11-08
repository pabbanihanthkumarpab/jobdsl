pipeline {
    agent any
    parameters{string(defaultValue: 'develop', description: 'The repository branch to be built', name: 'branch_name', trim: false)}
    environment {
        // Adding stuff to the env
        _JDK='java'
        _ANT='ant_1101'

        _JENKINS_AGENT='master'

        _SCM_MASTER_BRANCH='master'
        _SCM_CODE_BRANCH="${params.branch_name}"
        _SCM_REPO_NAME="${params.repositoryName}"
        _SCM_SUB_MODULE_NAME="${params.subModuleName}"
        _SCM_REPO_URL='ssh://git@adlm.nielsen.com:7999/cm/'+"${_SCM_REPO_NAME}" +'.git'
        _SCM_BUILD_URL='ssh://git@adlm.nielsen.com:7999/cm/azure-automation-nonprod.git'
        
        _SCM_CREDENTIALS_ID='perfbuild-KEY'
        _SCM_CREDENTIALS_USER=''                    // To be programatically setup
        _SCM_CREDENTIALS_KEY=''                     // To be programatically setup

	    _IMAGE_NAME='buycdarcreg.azurecr.io/cdar/'+  "${params.repositoryName}" + '/' +  "${params.subModuleName}" 
        _AUTOBUILD_FOLDER='master'
        _DOCKERBUILD_FOLDER='dockerBuild'
        _BUILD_XML_FOLDER="${_DOCKERBUILD_FOLDER}/common-scripts"
        _BUILD_VERSION=''
        _JAR_QUALIFIER='exec' // ---> what is this?
        _JAR_CONFIG_BASENAME='common-config-services'
        
        _JAR_CONFIG=''
       
        _JAR_BASE_FOLDER="${_AUTOBUILD_FOLDER}/" + "${params.repositoryName}"
        _JAR_FOLDER_COMMON_CFG="${_JAR_BASE_FOLDER}/" +  "${params.subModuleName}"+ "/build/libs"
        _BUILD_SERVER_CREDENTIALS_ID='qabuild'
        _BUILD_SERVER_CREDENTIALS_USERNAME=''       // To be programatically setup
        _BUILD_SERVER_CREDENTIALS_PASSWORD=''       // To be programatically setup

       /* _ARTIFACTORY_SERVER='ADLM_Artifactory'
        _ARTIFACTORY_REPO=''
        _SNAPSHOT_REPO='CDAR_datascience_tmv_services_Snapshot'
        _RELEASE_REPO='CDAR_datascience_tmv_services_Release'*/
        
        boolean testPassed = true
    }
    tools {
        jdk "${_JDK}"
        ant "${_ANT}"
    }
    options {
        buildDiscarder(logRotator(numToKeepStr: '20' ))
        timeout(time: 30, unit: 'MINUTES' )
        disableConcurrentBuilds()
        timestamps()
    }
    triggers{
        // This is required to enable webhooks to trigger builds
        pollSCM ''
    }
    /*post {
        success {
            build job: 'CDAR_Backoffice_Export_Deployment_Promotion',parameters: [string(name: 'Build_Version', value: "${_BUILD_VERSION}")],wait:false
            echo 'Pipeline completed successfully'
        }
        failure {
            echo 'Pipeline completed with errors'
        }
        always {
            echo 'Cleaning workspace...'
            //    deleteDir()
            echo 'All steps executed'
        }
    } */

    stages {

        stage('Code Checkout') {
            agent { label 'master' }
            options {
                skipDefaultCheckout()
            }
            steps {
                cleanWs()
                // Create folders to checkout branches into:
                // dockerbuild <== automation scriots branch
                // autobuild <== code branch
                echo "Code Checkout stage starting..."
                sh 'mkdir "${_AUTOBUILD_FOLDER}"'
                sh 'mkdir "${_DOCKERBUILD_FOLDER}"'
                dir ("${_AUTOBUILD_FOLDER}") {
                    //git branch: "${_SCM_CODE_BRANCH}", credentialsId: "${_SCM_CREDENTIALS_ID}", url: "${_SCM_REPO_URL}"
                    sshagent(['perfbuild-KEY']) {
                        sh 'git clone "${_SCM_REPO_URL}";cd "${repositoryName}";git checkout "${branch_name}"'
                    }
                }
                dir ("${_DOCKERBUILD_FOLDER}") {
                    git branch: "${_SCM_MASTER_BRANCH}", credentialsId: "${_SCM_CREDENTIALS_ID}", url: "${_SCM_BUILD_URL}"
                }
            }
            post {
                success {
                    echo "Code Checkout stage completed successfully."
                }
                failure {
                    echo "Code Checkout stage failed."
                }
            }
        }

        stage('Build') {
            agent { label 'master' }
            options {
                skipDefaultCheckout()
            }
            steps {
                echo "Build stage starting..."
                dir("${_AUTOBUILD_FOLDER}") {
                  script {
                    
                    if (params.CreateRelease) {
                      sshagent(['perfbuild-KEY']) {
                      sh """
                          ls -ltr;
                          echo "started build";
                          cd ${repositoryName}/${subModuleName} ; 
                         
                          ls -lart;
                          if [ -x gradle.properties ]
                          then
                            echo "File has execute permission"
                          else
                            chmod 755 gradle.properties
                            git update-index --chmod=+x gradle.properties;
                            git commit -m "Setting gradle.properties as executable";
                            git push;
                            echo "Execute permission has been set";
                          fi
                          ./gradlew release -Prelease.useAutomaticVersion=true -Prelease.releaseVersion="${ReleaseVersion}" -Prelease.newVersion=${NewVersion}
                          echo "Release created";
                          
                          sleep 2
                          git checkout develop
                          echo "Building artifacts..."
                          ./gradlew clean build --debug
                          
                          git checkout "${Branch_Name}";
                          git pull;
                          git checkout develop;
                          git pull;
                          git merge "${Branch_Name}";
                          git push;
                          git checkout master;
                          git merge "${Branch_Name}";
                          git push;
                          git push --delete origin ${Branch_Name}
                      """
                      }
                      _BUILD_VERSION = "${ReleaseVersion}"
                      echo "1:${ReleaseVersion}-2:${_BUILD_VERSION}" 
                    }
                
                    else{
                        sh "ls -lrt; chmod -R 755 *;./gradlew ${subModuleName} clean build ;ls -lart;"
                        _BUILD_VERSION = sh (script: 'cat ${repositoryName}/${subModuleName}/gradle.properties | cut -d= -f 2', returnStdout: true).trim()
                    }
                    
                    _JAR_CFG = "${_JAR_CONFIG_BASENAME}-${_BUILD_VERSION}-${_JAR_QUALIFIER}" 
                  }
                }
            }
            post {
                success {
                    echo "Build stage completed successfuly."
                }
                failure {
                    echo "Build stage Failed."
                }
            }
        }

        stage('Push images to ACR') {
            agent { label 'master' }
            options {
                skipDefaultCheckout()
            }
            steps {
                echo "Building and pushing docker images..."
                
                withAnt(installation: "${_ANT}") {
                    withCredentials ([usernamePassword(
                        credentialsId: "${_BUILD_SERVER_CREDENTIALS_ID}",
                        usernameVariable: '_BUILD_SERVER_CREDENTIALS_USERNAME',
                        passwordVariable: '_BUILD_SERVER_CREDENTIALS_PASSWORD')])
                    {
                        dir("${_BUILD_XML_FOLDER}") {
                            //sh "echo ${_BUILD_VERSION}>build_version;cat build_version"
                            sh 'ant -Dtag="${_BUILD_VERSION}" Build_N_Push_Docker_Image'
                        }
                    }
                }
                
            }
            post {
                success {
                    echo "Push to ACR stage completed successfuly."
                }
                failure {
                    echo "Push to ACR stage Failed."
                }
            }
        }
        
		
		// what about the props file in azure repo 
		//what about gateway code in repo?
		// stop gsr service in build.xml?
		
		/*
        stage('Push to ADLM JFrog Artifactory') {
            agent { label 'master' }
            options {
                skipDefaultCheckout()
            }
            steps {
                echo "Push to Artifactory stage starting..."
                script {
                    def server = Artifactory.server "${_ARTIFACTORY_SERVER}"
                    
                    if (params.CreateRelease) {
                        _ARTIFACTORY_REPO="${_RELEASE_REPO}"
                    }
                    else {
                        _ARTIFACTORY_REPO="${_SNAPSHOT_REPO}"
                    }
                        
                    def uploadSpec = """{
                         "files": [
                          {
                            "pattern": "${_JAR_FOLDER_COMMON_CFG}/${_JAR_CFG}.jar",
                            "target": "${_ARTIFACTORY_REPO}"
                          }
                          ]
                        }"""
                    
                    server.upload(uploadSpec)
                    
               // sh "ls -alh ${_JAR_FOLDER_DLVRY};echo ${_JAR_CFG}"
                }
            }
            post {
                success {
                    echo "Push to Artifactory stage completed successfuly."
                }
                failure {
                    echo "Push to Artifactory stage failed."
                }
            }
        }*/

    }
}
