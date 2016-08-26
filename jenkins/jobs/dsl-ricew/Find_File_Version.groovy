

// jenkins credentials id 
def scmCredentialsId = "svn-credentials-id"


def createJob = freeStyleJob ("${ENVIRONMENT} - Find File Version")

createJob.with {
  parameters {
    choiceParam('SHELL_TARGET', ['oracle@aesgdevapp', 'applmgr@bos1tr12l01.bos1.aesgdevapp'], 'Which application server to run on')
    stringParam('FILE_NAME_TO_SCAN', '', '''Put in the filename (e.g: CUSTOM.pll)
No paths or directories are needed''')
  }
  
  steps {
    shell ('''#!/bin/bash
ssh $SHELL_TARGET ". ~/.profile; \$XXCU_TOP/bin/xxcu_find_file_version.sh $FILE_NAME_TO_SCAN"
    ''')
  }
  
}