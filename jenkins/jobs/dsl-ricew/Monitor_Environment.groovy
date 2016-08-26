

// jenkins credentials id 
def scmCredentialsId = "svn-credentials-id"


def createJob = freeStyleJob ("${ENVIRONMENT} - Monitor Environment")

createJob.with {
  parameters {
    choiceParam('LOG_LEVEL', ['INFO', 'DEBUG'], '')
    choiceParam('APP_SERVER', ["${APP_SSH_USER}@${APP_SERVER}"], '')
    choiceParam('XXCU_TOP', ["${XXCU_TOP_DIRECTORY}"], 'Must point to the XXCU_TOP location')
    choiceParam('NOTIFY_MAIL', ['EBS_Support@aesgdevapp'], '')
  }
  configure { project ->
        project / 'properties' / 'hudson.model.ParametersDefinitionProperty' / 'parameterDefinitions' / 'hudson.model.PasswordParameterDefinition' {
            'name'('APPS_PASSWORD')
            'description'('')
            'defaultValue'('apps')
        }
  }
  
  disabled()
  
  multiscm {
        svn {
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
  
  configure { project ->
        project / 'triggers' / 'hudson.triggers.TimerTrigger' {
            'spec'('''# MINUTE HOUR DOM MONTH DOW
H * * * *
             ''')
        }
  }
  
  steps {
    shell ('''#!/bin/bash
./scripts/pushFolders --target $APP_SERVER \\
      --src-root-uri "./" \\
      --trg-root-uri "$XXCU_TOP" \\
      --dist-dir-list "bin" \\
      --exclude ".svn" \\
      --exec-remote ". ~/.profile; cd $XXCU_TOP/bin; echo $APPS_PASSWORD | NOTIFY_MAIL=$NOTIFY_MAIL ./xxcu_monitor_services.sh"
    ''')
  }
  
}