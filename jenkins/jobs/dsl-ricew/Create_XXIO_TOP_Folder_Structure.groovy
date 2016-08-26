

// jenkins credentials id 
def scmCredentialsId = "svn-credentials-id"


def createJob = freeStyleJob ("${ENVIRONMENT} - Create XXIO TOP Folder Structure")

createJob.with {
  parameters {
    choiceParam('LOG_LEVEL', ['INFO', 'DEBUG'], '')
    choiceParam('APP_SERVER', ["${APP_SSH_USER}@${APP_SERVER}"], '')
    choiceParam('XXCU_TOP', ["${XXCU_TOP_DIRECTORY}"], 'Must point to the XXCU_TOP location')
  }
  
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
  
  steps {
    shell ('''#!/bin/bash
./scripts/pushFolders --target $APP_SERVER \\
      --src-root-uri "./" \\
      --trg-root-uri "$XXCU_TOP" \\
      --dist-dir-list "bin" \\
      --exclude ".svn" \\
      --exec-remote ". ~/.profile; cd $XXCU_TOP/bin; ./xxio_top_folder_structure_creation.sh"
    ''')
  }
  
}