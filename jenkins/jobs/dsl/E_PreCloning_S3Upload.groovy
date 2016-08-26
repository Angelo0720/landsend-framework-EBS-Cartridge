def scmProject = "git@gitlab:${WORKSPACE_NAME}/oracle-ebs-cloning-211.git"
def scmCredentialsId = "adop-jenkins-master"

def jobsList = [
                    [jobName:'S3_Upload_Application_Archive', tags: 'appstier,upload_s3_artifacts', host: '$APPSERVER_TO_CLONE'] ,
                    [jobName:'S3_Upload_Database_Archive', tags: 'dbtier,upload_s3_artifacts', host: 'dbserver']
                    ]

def containerFolder = "${PROJECT_NAME}/Environment_PreCloning"         

jobsList.size().times { i -> //start Loop

  def createJob = freeStyleJob(containerFolder + '/' + jobsList[i].jobName)

  createJob.with { //start createJob
     concurrentBuild(true)
     logRotator(-1, 10)
     label('docker')
     environmentVariables {
          env('ANSIBLE_TAGS', jobsList[i].tags)
          env('ANSIBLE_HOST', jobsList[i].host)
     }
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
       credentialsBinding {
     	  usernamePassword("S3_ADMIN_ACCESS_KEY","S3_ADMIN_SECRET_KEY","aws-s3-access")
       } 
       colorizeOutput('css')
     }
     
     steps {
        shell('''#!/bin/bash -e
export ANSIBLE_FORCE_COLOR=true

ansible-playbook -i hosts preclone.yml -e "env=${ENVIRONMENT} host=${ANSIBLE_HOST} s3cmd_bucket_url=s3://${AWS_S3_BUCKET}" -t "${ANSIBLE_TAGS}" -i jenkins_dynamic_inventory

        ''')
     }

 } // end createJob
} //end Loop
