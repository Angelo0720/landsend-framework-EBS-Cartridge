

// jenkins credentials id 
def scmCredentialsId = "svn-credentials-id"


def createJob = freeStyleJob ("${ENVIRONMENT} - Install R12 XDO files")

createJob.with {
  parameters {
    choiceParam('LOG_LEVEL', ['INFO', 'DEBUG'], '')
    choiceParam('APP_SERVER', ["${APP_SSH_USER}@${APP_SERVER}"], '')
    stringParam('SKIPCOUNT', '0', 'Number of files to skip (Useful if processing large amount of files)')
    choiceParam('STOP_ON_ERROR', ['Y', 'N'], '')
    choiceParam('XXCU_TOP', ["${XXCU_TOP_DIRECTORY}"], 'Must point to the XXCU_TOP location')
    stringParam('EXECUTION_SCRIPTNAME', './xxcu_upload_dynamic_xdo.sh.ALL', '')
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
      location("${SCM_PROJECT_URL}/branches/${ENVIRONMENT}/appl_home/appl_top/xxcu/admin/config") {
        credentials(scmCredentialsId)
        directory('admin/config/')
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
	}
  }
  
  steps {
    shell ('''#!/bin/bash

chmod -R 755 scripts

./scripts/pushFolders --target $APP_SERVER \\
  --src-root-uri "./" \\
  --trg-root-uri "$XXCU_TOP" \\
  --dist-dir-list "bin:admin/config" \\
  --exclude "svn" \\
  --exec-remote ". ~/.bash_profile; chmod -R 755 $XXCU_TOP/bin; cd $XXCU_TOP/bin; echo $APPS_PASSWORD | STOP_ON_ERROR=${STOP_ON_ERROR} SKIPCOUNT=${SKIPCOUNT} LOG_LEVEL=${LOG_LEVEL}" ${EXECUTION_SCRIPTNAME} UPLOAD"
    ''')
  }
}