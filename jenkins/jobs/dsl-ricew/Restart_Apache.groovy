

// jenkins credentials id 
def scmCredentialsId = "svn-credentials-id"


def createJob = freeStyleJob ("${ENVIRONMENT} - Restart Apache")

createJob.with {
  parameters {
    choiceParam('SHELL_TARGET', ['oracle@aesgdevapp'], 'Which application server to restart apache on')
    choiceParam('CLEAR_CACHE', ['N', 'Y'], '')
    choiceParam('SHELL_TARGET2', ['bxlp_r12prd@bxlpcn16.bos1.aesgdevapp'], '')
  }
  configure { project ->
        project / 'properties' / 'hudson.model.ParametersDefinitionProperty' / 'parameterDefinitions' / 'hudson.model.PasswordParameterDefinition' {
            'name'('APPS_PASSWORD')
            'description'('')
            'defaultValue'('apps')
        }
  }
  steps {
    shell ('''#!/bin/bash
ssh ${SHELL_TARGET} ". ~/.profile; echo $APPS_PASSWORD | CLEAR_CACHE=${CLEAR_CACHE} \$XXCU_TOP/bin/xxcu_restart_apache.sh"
ssh ${SHELL_TARGET2} ". ~/.profile; echo $APPS_PASSWORD | CLEAR_CACHE=${CLEAR_CACHE} \$XXCU_TOP/bin/xxcu_restart_apache.sh"
    ''')
  }
  
}