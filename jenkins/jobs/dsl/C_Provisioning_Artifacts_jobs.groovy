def scmProject = "git@gitlab:${WORKSPACE_NAME}/oracle-ebs-cloning-211.git"
def scmCredentialsId = "adop-jenkins-master"

def jobsList = [
                    [jobName:'Download_App1_Archives', hostParam: '$APP1_HOST', id:'app1', downStreamJob: 'Extract_App1_Archives', group: 'appserver1'] ,
                    [jobName:'Download_Database_Archives', hostParam: '$DB_HOST', id:'db', downStreamJob: 'Extract_Database_Archives', group: 'dbserver']
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
       credentialsBinding {
     	  usernamePassword("S3_ADMIN_ACCESS_KEY","S3_ADMIN_SECRET_KEY","aws-s3-access")
       } 
       colorizeOutput('css')
     }
     
     steps {
        shell('''#!/bin/bash -e
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
   ARTIFACT=appsTier.tar.gz
   ;;
 db)
   IDENTIFIER=dbtier
   ARTIFACT=dbTier.tar.gz
   ;;
  *)
   echo 'VARIABLE $TYPE not defined properly'
   exit 1
   ;;
esac

# Wait for Route 53 host availability 
ansible localhost -m wait_for -a "port=22 host=${HOST} delay=1"  -i ${CUSTOM_WORKSPACE}/jenkins_dynamic_inventory

if [ ${PRIVATE_BUCKET} == false ]; then
  # Run adhoc command to download S3 artifacts
  echo "Downloading Oracle EBS 2.1.1 artifacts from ADOP Public S3 endpoint.."
  ansible ${HOST_GROUP} -m shell -a "wget -O /tmp/${ARTIFACT} https://pdc-oracle-ebs-bucket.s3-accelerate.amazonaws.com/${AWS_S3_TAG}/${ARTIFACT}" -i ${CUSTOM_WORKSPACE}/jenkins_dynamic_inventory
else
  # Execute playbook  to download S3 Artifacts
  ansible-playbook  "${IDENTIFIER}.yml" -e "s3_tag=${AWS_S3_TAG} host_group=${HOST_GROUP}  s3cmd_bucket_url=s3://${AWS_S3_BUCKET}" -t "${IDENTIFIER},get_s3_artifacts" -i ${CUSTOM_WORKSPACE}/jenkins_dynamic_inventory
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
