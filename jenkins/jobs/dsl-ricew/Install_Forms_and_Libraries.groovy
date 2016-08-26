

// jenkins credentials id 
def scmCredentialsId = "svn-credentials-id"


def createJob = freeStyleJob ("${ENVIRONMENT} - Install R12 Forms and libraries")

createJob.with {
  parameters {
    choiceParam('LOG_LEVEL', ['INFO', 'DEBUG'], '')
    choiceParam('APP_SERVER', ["${APP_SSH_USER}@${APP_SERVER}"], '')
    choiceParam('STOP_ON_ERROR', ['Y', 'N'], '')
    choiceParam('XXCU_TOP', ["${XXCU_TOP_DIRECTORY}"], 'Must point to the XXCU_TOP location')
    stringParam('EXECUTION_SCRIPTNAME', 'xxcu_install_dynamic_forms.ALL', '')
  }
  configure { project ->
        project / 'properties' / 'hudson.model.ParametersDefinitionProperty' / 'parameterDefinitions' / 'hudson.model.PasswordParameterDefinition' {
            'name'('APPS_PASSWORD')
            'description'('')
            'defaultValue'('apps')
        }
  }
  
  multiscm {
    svn {
      location("${SCM_PROJECT_URL}/branches/${ENVIRONMENT}/appl_home/appl_top/xxcu/forms") {
        credentials(scmCredentialsId)
        directory('forms')
        depth(SvnDepth.INFINITY)
      }
      location("${SCM_PROJECT_URL}/branches/${ENVIRONMENT}/appl_home/appl_top/xxcu/bin") {
        credentials(scmCredentialsId)
        directory('bin')
        depth(SvnDepth.INFINITY)
      }
      location("${SCM_PROJECT_URL}/branches/${ENVIRONMENT}/jenkins/bin") {
        credentials(scmCredentialsId)
        directory('scripts')
        depth(SvnDepth.INFINITY)
      }
      location("${SCM_PROJECT_URL}/branches/${ENVIRONMENT}/appl_home/appl_top/xxcu/resource") {
        credentials(scmCredentialsId)
        directory('resource')
        depth(SvnDepth.INFINITY)
	  }
    }
  }
  
  steps {
    shell ('''#!/bin/bash
./scripts/pushFolders --target $APP_SERVER \\
      --src-root-uri "./" \\
      --trg-root-uri "$XXCU_TOP" \\
      --dist-dir-list "forms:resource:bin" \\
      --exclude ".svn" \\
      --exec-remote ". ~/.profile; cd $XXCU_TOP/bin; echo $APPS_PASSWORD | STOP_ON_ERROR=${STOP_ON_ERROR} SKIPCOUNT=${SKIPCOUNT} LOG_LEVEL=${LOG_LEVEL} ${EXECUTION_SCRIPTNAME}"
    ''')
  }
}