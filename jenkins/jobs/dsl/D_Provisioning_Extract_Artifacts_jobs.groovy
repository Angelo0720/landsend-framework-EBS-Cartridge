def scmProject = "git@gitlab:${WORKSPACE_NAME}/oracle-ebs-cloning-211.git"
def scmCredentialsId = "adop-jenkins-master"
def jobsList = [
                    [jobName:'Extract_App1_Archives', hostParam: '$APP1_HOST', id:'app1', downStreamJob: '', group: 'appserver1'] ,
                    [jobName:'Extract_Database_Archives', hostParam: '$DB_HOST', id:'db', downStreamJob: 'Configure_ContextFiles_HostFiles', group: 'dbserver']
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

ansible-playbook "${IDENTIFIER}.yml" -e "host_group=${HOST_GROUP}" -t "extract" -i ${CUSTOM_WORKSPACE}/jenkins_dynamic_inventory
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
