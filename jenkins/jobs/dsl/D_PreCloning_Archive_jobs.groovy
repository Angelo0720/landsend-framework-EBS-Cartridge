def scmProject = "git@gitlab:${WORKSPACE_NAME}/oracle-ebs-cloning-211.git"
def scmCredentialsId = "adop-jenkins-master"
def jobsList = [
                    [jobName:'Archive_Application_Files', downStreamJob: 'S3_Upload_Application_Archive', tags: 'appstier,app_archive', host: '$APPSERVER_TO_CLONE'] ,
                    [jobName:'Archive_Database_Files', downStreamJob: 'S3_Upload_Database_Archive', tags: 'db_archive', host: 'dbserver']
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
       colorizeOutput('css')
     }
     
     steps {
        shell('''#!/bin/bash -e
export ANSIBLE_FORCE_COLOR=true

ansible-playbook -i hosts preclone.yml -e "host=${ANSIBLE_HOST}" -t "${ANSIBLE_TAGS}" -i jenkins_dynamic_inventory 

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
