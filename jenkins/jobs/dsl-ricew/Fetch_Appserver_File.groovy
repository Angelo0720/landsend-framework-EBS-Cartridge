

// jenkins credentials id 
def scmCredentialsId = "svn-credentials-id"


def createJob = freeStyleJob ("${ENVIRONMENT} - Fetch Appserver File")

createJob.with {
  parameters {
    choiceParam('REMOTE_LOGIN', ["${APP_SSH_USER}@${APP_SERVER}", 'bxlt_r12tst2@bxldcn06.bos1.aesgdevapp'], '')
    stringParam('DIRECTORY_NAME', '', '''Directory to fetch file from:<br>
<br>
/u01/oracle/app/prod/r12prd/xxio_top/archive<br>
/u01/oracle/app/prod/r12prd/inst_bxlpcn08/apps/AESGDEV_bxlpcn08/logs/appl/conc/log<br>
/u01/oracle/app/prod/r12prd/inst_bxlpcn08/apps/AESGDEV_bxlpcn08/logs/appl/conc/out<br>''')
    stringParam('FILE_NAME', '', '''File to fetch.
Specify blank if you want a directory listing instead''')
  }
  
  steps {
    shell ('''#!/bin/bash
if [ "$FILE_NAME" == "" ]; then
    echo "Directory listing of: ${DIRECTORY_NAME}"
    ssh $REMOTE_LOGIN "cd ${DIRECTORY_NAME}; ls -altr"
else
    echo "Fetching: ${DIRECTORY_NAME}/${FILE_NAME} to workspace"
    scp $REMOTE_LOGIN:${DIRECTORY_NAME}/${FILE_NAME} .

    echo "###############################"
    echo "To see file, look in workspace"
    echo "###############################"
fi
    ''')
  }
  
}