def scmProject = "git@gitlab:${WORKSPACE_NAME}/oracle-ebs-cloning-211.git"
def scmCredentialsId = "adop-jenkins-master"
def containerFolder = "${PROJECT_NAME}/Environment_Cloning"  
def downStreamJob = 'Clone_Database' 
def createJob = freeStyleJob(containerFolder + '/Configure_ContextFiles_HostFiles')

createJob.with { //start createJob
   concurrentBuild(true)
   logRotator(-1, 10)
   label('docker')
   customWorkspace('$CUSTOM_WORKSPACE/prepare')
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

# Prepare the context and Host files
ORACLE_EBS_HTTP_PORT=${ORACLE_EBS_HTTP_PORT-:8091}

ansible-playbook "common.yml" -e "target_host=$ENVIRONMENT  domain=${AWS_DOMAIN}" -t "host_file,setup_users" -i ${CUSTOM_WORKSPACE}/jenkins_dynamic_inventory
ansible-playbook "dbtier.yml" -e " domain=${AWS_DOMAIN}"  -t "context_file" -i ${CUSTOM_WORKSPACE}/jenkins_dynamic_inventory
ansible-playbook "appstier.yml" -e "host_group=appserver1  domain=${AWS_DOMAIN} web_port=${ORACLE_EBS_HTTP_PORT}" -t "context_file" -i ${CUSTOM_WORKSPACE}/jenkins_dynamic_inventory

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
