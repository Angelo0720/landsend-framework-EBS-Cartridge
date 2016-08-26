

// jenkins credentials id 
def scmCredentialsId = "svn-credentials-id"


def createJob = freeStyleJob ("${ENVIRONMENT} - Assign Responsibilities")

createJob.with {
  configure { project ->
        project / 'properties' / 'jenkins.model.BuildDiscarderProperty' / 'strategy' {
            'daysToKeep'('10')
            'numToKeep'('-1')
            'artifactDaysToKeep'('-1')
            'artifactNumToKeep'('-1')
        }
  }
  configure { project ->
        project / 'properties' / 'hudson.model.ParametersDefinitionProperty' / 'parameterDefinitions' / 'hudson.model.PasswordParameterDefinition' {
            'name'('APPS_PASSWORD')
            'description'('')
            'defaultValue'('apps')
        }
  }
  parameters {
    choiceParam('LOG_LEVEL', ['INFO', 'DEBUG'], '')
    choiceParam('TNS_STRING', ['bxdp-scan.bos1.develapp2.cmt-eagle.domain:1521/DBdevel'], '')
    choiceParam('USER_LIST_FILE', ["${HOME}/jobs/AESGDEV - Assign Responsibilities/workspace/scripts/PRD_POWER_USER_RESPONSIBILITIES.dat"], '')
    choiceParam('OJDBC_LOCATION', ["${HOME}/apache-maven-2.2.1/lib/ojdbc6.jar"], '')
  }
  
  multiscm {
        svn {
          location("${SCM_PROJECT_URL}/branches/${ENVIRONMENT}/jenkins/bin") {
                credentials(scmCredentialsId)
                directory('scripts')
                depth(SvnDepth.INFINITY)
            }
            location("${SCM_PROJECT_URL}/branches/${ENVIRONMENT}/jenkins/groovy") {
                credentials(scmCredentialsId)
                directory('groovy')
                depth(SvnDepth.INFINITY)
            }
        }
  }
  
  steps {
    systemGroovyScriptFile('groovy/assign_prod_responsibilities.groovy') {
    }
  }
  
}