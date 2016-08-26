

// jenkins credentials id 
def scmCredentialsId = "svn-credentials-id"


def createJob = freeStyleJob ("${ENVIRONMENT} - Install R12 OAF Files")

createJob.with {
  parameters {
    choiceParam('LOG_LEVEL', ['INFO', 'DEBUG'], '')
    choiceParam('APP_SERVER', ["${APP_SSH_USER}@${APP_SERVER}"], '')
    choiceParam('APPL_HOME', ["${APPL_HOME}"], 'Must point to the XXCU_TOP location')
    stringParam('EXECUTION_SCRIPTNAME', './xxcu_install_dynamic_oaf.ALL', '')
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
      location("${SCM_PROJECT_URL}/branches/${ENVIRONMENT}/appl_home/appl_top/xxcu/bin") {
        credentials(scmCredentialsId)
        directory('appl/xxcu/12.0.0/bin')
        depth(SvnDepth.INFINITY)
      }
      location("${SCM_PROJECT_URL}/branches/${ENVIRONMENT}/appl_home/common_top") {
        credentials(scmCredentialsId)
        directory('comn')
        depth(SvnDepth.INFINITY)
      }
      location("${SCM_PROJECT_URL}/branches/${ENVIRONMENT}/jenkins/bin") {
        credentials(scmCredentialsId)
        directory('scripts')
        depth(SvnDepth.INFINITY)
      }
      location("${SCM_PROJECT_URL}/branches/${ENVIRONMENT}/appl_home/appl_top/jdev") {
        credentials(scmCredentialsId)
        directory('appl/jdev')
        depth(SvnDepth.INFINITY)
	  }
    }
  }
  steps {
    shell ('''#!/bin/bash
./scripts/pushFolders --target $APP_SERVER \\
      --src-root-uri "./" \\
      --trg-root-uri "$APPL_HOME" \\
      --dist-dir-list "appl:comn" \\
      --exclude ".svn" \\
      --exec-remote ". ~/.profile; cd \$XXCU_TOP/bin; echo $APPS_PASSWORD | LOG_LEVEL=${LOG_LEVEL} ${EXECUTION_SCRIPTNAME} UPLOAD"
    ''')
  }
}