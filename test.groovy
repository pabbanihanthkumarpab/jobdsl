String basePath = 'dsljobs'
String repo = 'git@repo:name/project'

folder(basePath) {
  description 'DSL generated folder.'
}

pipelineJob("$basePath/status") {
  description()
  definition {
    cpsScm {
      scm {
        git {
          remote {
            url(repo)
          }
          branch("*/dev")
        }
      }
      scriptPath("Jenkinsfile.status")
    }
  }
}
