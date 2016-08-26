

// jenkins credentials id 
def scmCredentialsId = "svn-credentials-id"


def createJob = freeStyleJob ("${ENVIRONMENT} - Install DataLoad Professional Files")

createJob.with {
  parameters {
    choiceParam('LOG_LEVEL', ['INFO', 'DEBUG'], '')
    choiceParam('APP_SERVER', ["${APP_SSH_USER}@${APP_SERVER}"], '')
    choiceParam('FORMS_TRACE_DIR', ["${FORMS_TRACE_DIR}"], 'Must point to the FORMS_TRACE_DIR location')
  }
  
  multiscm {
    svn {
      location("${SCM_PROJECT_URL}/branches/${ENVIRONMENT}/appl_home/appl_top/xxcu/admin/config/DATALOAD/staging") {
        credentials(scmCredentialsId)
        directory('staging')
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
      --src-root-uri "./staging" \\
      --trg-root-uri "$FORMS_TRACE_DIR" \\
      --dist-dir-list "." \\
      --exclude ".svn" \\
      --exec-remote ". ~/.profile; cd $FORMS_TRACE_DIR; cd ..; chmod -R g+rw forms"
    ''')
  }

}