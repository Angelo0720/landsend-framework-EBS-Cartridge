

// jenkins credentials id 
def scmCredentialsId = "svn-credentials-id"


def createJob = freeStyleJob ("${ENVIRONMENT} - Purge Audit")

createJob.with {
  parameters {
    choiceParam('DB_LOGIN1', ['bxdp_orar12@aesgdevdb'], '')
    choiceParam('DB_LOGIN2', ['bxdp_orar12@bxdpdb02.bos1.aesgdevapp'], '')
  }
  disabled()
  
  configure { project ->
        project / 'triggers' / 'hudson.triggers.TimerTrigger' {
            'spec'('0 0 * * *')
        }
  }
  
  steps {
    shell ('''ssh $DB_LOGIN1 '/usr/bin/find /u01/oracle/app/r12prd/11204/product/rdbms/audit  -name "*.aud*" -type f -mtime +7 -exec rm {} \\;'
ssh $DB_LOGIN2 '/usr/bin/find /u01/oracle/app/r12prd/11204/product/rdbms/audit  -name "*.aud*" -type f -mtime +7 -exec rm {} \\;'
    ''')
  }
  configure { project ->
        project / 'publishers' / 'hudson.tasks.Mailer' {
            'recipients'('becky_hall@aesgdevapp')
            'dontNotifyEveryUnstableBuild'('false')
            'sendToIndividuals'('false')
        }
  }

}