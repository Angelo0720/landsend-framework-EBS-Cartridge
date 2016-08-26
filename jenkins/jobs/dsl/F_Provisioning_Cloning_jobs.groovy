def scmProject = "git@gitlab:${WORKSPACE_NAME}/oracle-ebs-cloning-211.git"
def scmCredentialsId = "adop-jenkins-master"


def jobsList = [
                    [jobName:'Clone_App1', hostParam: '$APP1_HOST', id:'app1', downStreamJob: '', group: 'appserver1', tags: 'adcfgclone,xxcu_top_dir'],
                    [jobName:'Clone_Database', hostParam: '$DB_HOST', id:'db', downStreamJob: 'Clone_App1', group: 'dbserver', tags: 'adcfgclone']
                    ]

def containerFolder = "${PROJECT_NAME}/Environment_Cloning"         

jobsList.size().times { i -> //start Loop

  def createJob = freeStyleJob(containerFolder + '/' + jobsList[i].jobName)

  createJob.with { //start createJob
     concurrentBuild(true)
     logRotator(-1, 10)
     label('docker')
     environmentVariables {
          env('HOST', jobsList[i].hostParam)
          env('TYPE', jobsList[i].id)
          env('HOST_GROUP', jobsList[i].group)
          env('ANSIBLE_TAGS', jobsList[i].tags)
     }
     customWorkspace('$CUSTOM_WORKSPACE/' + jobsList[i].id)
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
        shell('''#!/bin/bash
export ANSIBLE_FORCE_COLOR=true

# Create a temporary env file for new environment
if [ ${NEW_ENV} == true ]; then
  cat group_vars/custom_env.template > group_vars/${ENVIRONMENT}.yml
  sed -i "s/###domain###/${AWS_DOMAIN}/g" group_vars/${ENVIRONMENT}.yml
  sed -i "s/###db_host###/${ENVIRONMENT}db/g" group_vars/${ENVIRONMENT}.yml
  sed -i "s/###app1_host###/${ENVIRONMENT}app1/g" group_vars/${ENVIRONMENT}.yml
  sed -i "s/###env###/${ENVIRONMENT}/g" group_vars/${ENVIRONMENT}.yml
fi

# Get Host Group accordingly
# NOTE: The variable host_group is not used in Database Download artifacts and Extract Artifacts Job.
case $TYPE in
 app1|app2)
   IDENTIFIER=appstier
   ;;
 db)
   IDENTIFIER=dbtier
   ;;
  *)
   echo 'VARIABLE $TYPE not defined properly'
   exit 1
   ;;
esac

# Create ansible vault file
VAULT_PASSWORD=${VAULT_PASSWORD:-pass123}
echo "$VAULT_PASSWORD" > .vault.pass && chmod 600 .vault.pass

wait_for_app() {
  echo "Wait for application service to go up.."
  ansible localhost -m wait_for -a "host=${ENVIRONMENT}${TYPE}.${AWS_DOMAIN} port=8091 delay=5 timeout=120"
}

ansible-playbook "${IDENTIFIER}.yml" -e "host_group=${HOST_GROUP}" -t "${ANSIBLE_TAGS}" -i ${CUSTOM_WORKSPACE}/jenkins_dynamic_inventory --vault-pass .vault.pass

if [[ $TYPE == *"app"* ]]; then
  
  wait_for_app
  
  if [[ $STATUS != '200' ]] || [[ -z $STATUS ]]; then
    echo "Reboot server and try to restart services.."
    ansible ${HOST_GROUP} -m shell -a "sudo reboot" -i ${CUSTOM_WORKSPACE}/jenkins_dynamic_inventory
    ansible localhost -m wait_for -a "host=${ENVIRONMENT}${TYPE}.${AWS_DOMAIN} port=22 delay=60 timeout=360"
  fi
  
  wait_for_app
  
  if [[ $? -eq 0 ]]; then
    echo "Cloning successful.."
    exit 0
  else
    echo "Cloning failure even after reboot fix.."
    exit 1
  fi
  
fi

        ''')
     }

     publishers {
      downstreamParameterized {
       trigger(jobsList[i].downStreamJob) {
      	 condition('SUCCESS')
         parameters {
           currentBuild()
         }
        }
      }
     }
 } // end createJob
} //end Loop
