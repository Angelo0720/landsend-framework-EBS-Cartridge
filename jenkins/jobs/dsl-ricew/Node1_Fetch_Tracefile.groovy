

// jenkins credentials id 
def scmCredentialsId = "svn-credentials-id"


def createJob = freeStyleJob ("${ENVIRONMENT} - Node1 Fetch Tracefile")

createJob.with {
  parameters {
    choiceParam('USER_DUMP_DEST', ['/u01/oracle/app/r12prd/11204/diag/rdbms/r12prd/AESGDEV1/trace'], '')
    stringParam('TRACE_FILENAME', '', '''Insert your tracefile name here.
If you don't specify the filename, it will fetch the directory listing instead.''')
    choiceParam('DB_LOGIN', ['bxdp_orar12@aesgdevdb'], '')
    booleanParam('TKPROF', false, 'Check if you also want a TKProf Version (Filename will be same, with .tkprof as suffix)')
    choiceParam('TKPROF_SORTING', ['None', 'Elapsed Time Fetching (fchela)', 'Elapsed Time Executing(exeela)'], '')
    choiceParam('PROFILE_FILE_NAME', ['AESGDEV1_bxdpdb01.env'], '')
  }
  
  steps {
    shell ('''#!/bin/bash
if [ "$TRACE_FILENAME" == "" ]; then
    echo "Directory listing of: ${USER_DUMP_DEST}"
    ssh $DB_LOGIN "cd ${USER_DUMP_DEST}; ls -altr"
else
    echo "Fetching: ${USER_DUMP_DEST}/${TRACE_FILENAME} to workspace"
    scp $DB_LOGIN:${USER_DUMP_DEST}/${TRACE_FILENAME} .

    if [ "$TKPROF" == "true" ]; then
        if [ "$TKPROF_SORTING" == "Elapsed Time Fetching (fchela)" ]; then
            SORT_OPT="sort=fchela"
        elif [ "$TKPROF_SORTING" == "Elapsed Time Executing(exeela)" ]; then
            SORT_OPT="sort=exeela"
        else
            SORT_OPT=""
        fi
        echo "Running TKPROF"


        ssh $DB_LOGIN ". ${PROFILE_FILE_NAME}; tkprof ${USER_DUMP_DEST}/${TRACE_FILENAME} $SORT_OPT output=${USER_DUMP_DEST}/${TRACE_FILENAME}${SORT_OPT}.tkprof"
        scp $DB_LOGIN:${USER_DUMP_DEST}/${TRACE_FILENAME}${SORT_OPT}.tkprof .

        echo "Content of TKPROF file:"
        cat ${TRACE_FILENAME}${SORT_OPT}.tkprof

        echo "########################################"
        echo "To see raw tracefile, look in workspace"
        echo "########################################"

    fi
fi
    ''')
  }
  
}