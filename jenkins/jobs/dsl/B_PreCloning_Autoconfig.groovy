def scmProject = "git@gitlab:${WORKSPACE_NAME}/oracle-ebs-cloning-211.git"
def scmCredentialsId = "adop-jenkins-master"
def downStreamJob = 'PreCloning'
def createJob = freeStyleJob("${PROJECT_NAME}/Environment_PreCloning/PreCloning_AutoConfig") 

createJob.with { //start createJob
   concurrentBuild(true)
   logRotator(-1, 10)
   label('docker')
   customWorkspace('$CUSTOM_WORKSPACE')
   properties {
     rebuild {
       autoRebuild(true)
       rebuildDisabled(false)
      }
   }

   scm {
     git {
       remote {
         url(scmProject)
         credentials(scmCredentialsId)
       }
       branch('*/master')
     }
   }
   
   wrappers {
     sshAgent('ansible-user-key')
     colorizeOutput('css')
   }
   
   steps {
      shell('''#!/bin/bash -e
export ANSIBLE_FORCE_COLOR=true

# Create ansible vault file
VAULT_PASSWORD=${VAULT_PASSWORD:-pass123}
echo "$VAULT_PASSWORD" > .vault.pass && chmod 600 .vault.pass

ansible-playbook preclone_autoconfig.yml -i hosts -e "host=$APPSERVER_TO_CLONE" -i jenkins_dynamic_inventory --vault-pass .vault.pass

rm -f .vault.pass

        ''')
     }

     publishers {
      downstreamParameterized {
       trigger(downStreamJob) {
      	 condition('SUCCESS')
         parameters {
           currentBuild()
         }
        }
      }
    }
 } // end createJob
