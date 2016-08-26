

// jenkins credentials id 
def scmCredentialsId = "svn-credentials-id"


def createJob = freeStyleJob ("${ENVIRONMENT} - Gather Stale Statistics")

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
    choiceParam('TNS_STRING', ["bxdp-scan.bos1.aesgdevapp:${DB_PORT}/AESGDEV"], '')
    choiceParam('OJDBC_LOCATION', ["${HOME}/apache-maven-2.2.1/lib/ojdbc6.jar"], '')
  }
  
  disabled()
  
  scm {
        svn {
            location("${SCM_PROJECT_URL}/branches/${ENVIRONMENT}/jenkins/groovy") {
                directory('groovy')
                depth(SvnDepth.INFINITY)
            }
        }
  }
    
  configure { project ->
        project / 'triggers' / 'hudson.triggers.TimerTrigger' {
            'spec'('H 2 * * *')
        }
  }
  
  steps {
    systemGroovyScriptFile('groovy/gather_stale_statistics.groovy') {
    }
  }
  
}