

// jenkins credentials id 
def scmCredentialsId = "svn-credentials-id"


def createJob = freeStyleJob ("${ENVIRONMENT} - Install COMMON TOP Files")

createJob.with {
  parameters {
    choiceParam('LOG_LEVEL', ['INFO', 'DEBUG'], '')
    choiceParam('APP_SERVER', ["${APP_SSH_USER}@${APP_SERVER}"], '')
    choiceParam('COMMON_TOP', ["${COMMON_TOP}"], 'Must point to the COMMON_TOP location')
  }
  
  multiscm {
    svn {
      location("${SCM_PROJECT_URL}/branches/${ENVIRONMENT}/appl_home/common_top") {
        credentials(scmCredentialsId)
        directory('admin/config/WFLOAD')
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
      --src-root-uri "./common_top" \\
      --trg-root-uri "$COMMON_TOP" \\
      --dist-dir-list "." \\
      --exclude ".svn"
    ''')
  }
  
}