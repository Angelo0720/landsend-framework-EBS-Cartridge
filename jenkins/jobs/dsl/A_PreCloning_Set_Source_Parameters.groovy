folder("${PROJECT_NAME}/Environment_PreCloning") {
  configure { folder ->
    folder / icon(class: 'org.example.MyFolderIcon')
  }
}

def scmProject = "git@gitlab:${WORKSPACE_NAME}/oracle-ebs-cloning-211.git"
def scmCredentialsId = "adop-jenkins-master"
def downStreamJob = 'PreCloning_AutoConfig'
def createJob = freeStyleJob("${PROJECT_NAME}/Environment_PreCloning/Set_PreCloning_Source_Parameters") 

createJob.with {
  concurrentBuild(true)
  logRotator(-1, 10)
  label('docker')
 customWorkspace('/workspace/${JOB_NAME}_${ENVIRONMENT}')
  parameters {
    choiceParam('ENVIRONMENT', ['devel', 'stage', 'prod'], 'Environment Name')
    stringParam('AWS_S3_BUCKET', 'pdc-oracle-ebs-bucket', 'PreClone Artifacts Bucket. This is where your backups will be stored.')
    choiceParam('APPSERVER_TO_CLONE', ['appserver1', 'appserver2'], 'Select the Target Appserver to clone')
    nonStoredPasswordParam('VAULT_PASSWORD', 'Ansible Vault Password. Default to pass123 if unset')
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
    preBuildCleanup() 
	  colorizeOutput('css')
	}
    
  steps {
    shell('''#!/bin/bash -e
 
 # Create props file
DOMAIN=$(cat group_vars/${ENVIRONMENT}.yml | grep domain: | cut -d' ' -f2 | tr -d '"')

HOST=$(cat group_vars/${ENVIRONMENT}.yml | grep db_host: | cut -d' ' -f2 | tr -d '"')
DB_HOST="$HOST.$DOMAIN"

HOST=$(cat group_vars/${ENVIRONMENT}.yml | grep app1_host: | cut -d' ' -f2 | tr -d '"')
APP1_HOST="$HOST.$DOMAIN"

HOST=$(cat group_vars/${ENVIRONMENT}.yml | grep app2_host: | cut -d' ' -f2 | tr -d '"')
APP2_HOST="$HOST.$DOMAIN"

echo "CUSTOM_WORKSPACE=$WORKSPACE" > props
echo "DB_HOST=$DB_HOST" >> props
echo "APP1_HOST=$APP1_HOST" >> props
echo "APP2_HOST=$APP2_HOST" >> props

echo "[$ENVIRONMENT]" > jenkins_dynamic_inventory
echo "$DB_HOST" >> jenkins_dynamic_inventory
echo "$APP1_HOST" >> jenkins_dynamic_inventory
echo "$APP2_HOST" >> jenkins_dynamic_inventory
echo "[appserver1]" >> jenkins_dynamic_inventory
echo "$APP1_HOST" >> jenkins_dynamic_inventory
echo "[appserver2]" >> jenkins_dynamic_inventory
echo "$APP2_HOST" >> jenkins_dynamic_inventory
echo "[dbserver]" >> jenkins_dynamic_inventory
echo "$DB_HOST" >> jenkins_dynamic_inventory

	''')
  }
  publishers {
    downstreamParameterized {
      trigger(downStreamJob) {
 		    condition('SUCCESS')
        parameters {
          currentBuild()
 				  propertiesFile('props', true)
        }
 	    }
 	  }
  }
  
}