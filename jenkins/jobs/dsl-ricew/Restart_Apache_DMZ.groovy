

// jenkins credentials id 
def scmCredentialsId = "svn-credentials-id"


def createJob = freeStyleJob ("${ENVIRONMENT} - Restart Apache DMZ")

createJob.with {
  parameters {
    choiceParam('SHELL_TARGET', ['applmgr@bos1pr12dmzl01.bos1.aesgdevapp'], 'Which application server to restart apache on')
    choiceParam('CLEAR_CACHE', ['N', 'Y'], '')
  }
  steps {
    shell ('''#!/bin/bash
ssh ${SHELL_TARGET} ". ~/.bash_profile; ASK_PASSWORD=N CLEAR_CACHE=${CLEAR_CACHE} \$XXCU_TOP/bin/xxcu_restart_apache.sh"
    ''')
  }
}