

// jenkins credentials id 
def scmCredentialsId = "svn-credentials-id"


def createJob = freeStyleJob ("${ENVIRONMENT} - Monitor LDAP")

createJob.with {
  disabled()
  configure { project ->
        project / 'triggers' / 'hudson.triggers.TimerTrigger' {
            'spec'('*/5 * * * *')
        }
  }
  
  steps {
    shell ('''RETCODE=0

ssh bxlp_r12prd@bxlpcn08 ". ~/.profile; ldapbind -h bxlpcn08.bos1.aesgdevapp -p 3060"
if [ $? -ne 0 ]; then
    RETCODE=1
fi

ssh bxlp_r12prd@bxlpcn16 ". ~/.profile; ldapbind -h bxlpcn16.bos1.aesgdevapp -p 3060"
if [ $? -ne 0 ]; then
    RETCODE=1
fi

ssh bxlp_r12prd@bxlpcn16 ". ~/.profile; ldapbind -h oidprd.aesgdevapp -p 3060"
if [ $? -ne 0 ]; then
    RETCODE=1
fi

exit $RETCODE		
    ''')
  }
  
  configure { project ->
        project / 'publishers' / 'hudson.tasks.Mailer' {
            'recipients'('becky_hall@aesgdevapp EBS_Support@aesgdevapp')
            'dontNotifyEveryUnstableBuild'('false')
            'sendToIndividuals'('false')
        }
  }
}