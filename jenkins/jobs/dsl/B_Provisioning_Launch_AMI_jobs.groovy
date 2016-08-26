def scmProject = "git@gitlab:${WORKSPACE_NAME}/ec2-create.git"
def scmCredentialsId = "adop-jenkins-master"
def jobsList = [
                    [jobName:'Create_App1_EC2', hostParam: '$APP1_HOST', id:'app1', downStreamJob: 'Download_App1_Archives'] ,
                    [jobName:'Create_Database_EC2', hostParam: '$DB_HOST', id:'db', downStreamJob: 'Download_Database_Archives']
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
     	  usernamePassword("EC2_ACCESS_KEY","EC2_SECRET_KEY","aws-environment-provisioning")
       } 
       colorizeOutput('css')
     }
     
     steps {
        shell('''#!/bin/bash
export ANSIBLE_FORCE_COLOR=true

# Provision
ansible-playbook "launch_ami.yml" -e "env=${ENVIRONMENT} type=${TYPE} workspace=${CUSTOM_WORKSPACE} ami_id=${AWS_AMI_ID} instance_name=${HOST} custom_hostname=${HOST} aws_region=${AWS_REGION} security_group_id=${AWS_SECURITY_GROUP_ID} key_name=${AWS_KEY_PAIR} vpc_subnet_id=${AWS_SUBNET_ID} vpc_id=${AWS_VPC_ID} domain=${AWS_DOMAIN} hosted_zone=${AWS_DOMAIN} assign_public_ip=${ASSIGN_PUBLIC_IP}"

# Check if inventory file was properly tokenized
if [ $(cat "${CUSTOM_WORKSPACE}/jenkins_dynamic_inventory" | grep "###${TYPE}_host###" | wc -l) -eq 1 ]; then
  echo "###${TYPE}_host### was not properly tokenized."
  exit 1
fi

cat ${CUSTOM_WORKSPACE}/jenkins_dynamic_inventory

# Error handling
if [ $? -gt 0 ]
then
ansible-playbook "terminate_instances.yml"
exit 1
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
