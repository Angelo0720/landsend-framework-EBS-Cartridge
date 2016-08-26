folder("${PROJECT_NAME}/Environment_Cloning") {
  configure { folder ->
    folder / icon(class: 'org.example.MyFolderIcon')
  }
}
def scmProject1 = "git@gitlab:${WORKSPACE_NAME}/ec2-create.git"
def scmProject2 = "git@gitlab:${WORKSPACE_NAME}/oracle-ebs-cloning-211.git"
def scmCredentialsId = "adop-jenkins-master"
def downStreamJobNames = 'Create_App1_EC2,Create_Database_EC2'
def createJob = freeStyleJob("${PROJECT_NAME}/Environment_Cloning/Set_Provisioning_Parameters") 

createJob.with {
  description("\nThis job serves as the Kickoff build for the Environment Provisioning Pipeline. Define your parameters accordingly. You will be able to access the  successfully provisioned environments by its app2 server.  \nEXAMPLE: http://demoapp1.adop.com:8091")
  parameters {
    stringParam('ENVIRONMENT', 'demo', 'This will use environment default provisioning values if it does not have a corresponding environment file in Ansible group variables')
    stringParam('AWS_DOMAIN', 'adop.com', 'Route 53 Private Domain Name. AWS will charge you with each unique Private domain created so reuse already created domains.')
    stringParam('AWS_AMI_ID', "ami-808401f3", 'Oracle EBS Base AMI')
    stringParam('AWS_REGION', "${AWS_REGION}", 'Current Region')
    stringParam('AWS_SECURITY_GROUP_ID', "${DEFAULT_SECURITY_GROUP_ID}", 'Security Group')
    stringParam('AWS_KEY_PAIR', "${KEY_PAIR}", 'SSH Key Pair Name')
    stringParam('AWS_VPC_ID', "${VPC_ID}", 'VPC ID')
    stringParam('AWS_SUBNET_ID', "${DEFAULT_PUBLIC_SUBNET_ID}", 'Subnet ID. Default value is a Public subnet from CloudFormation')
    booleanParam('ASSIGN_PUBLIC_IP', true, 'Uncheck if AWS_SUBNET_ID is on a private subnet.')
    stringParam('ORACLE_EBS_HTTP_PORT','8091','OPTIONAL. The HTTP port for the newly provisioned Application Server. This must not conflict with other used ports.')
    stringParam('AWS_S3_TAG', 'prod_04-06-2016', 'PreClone Build Tag')
    stringParam('AWS_S3_BUCKET', 'pdc-oracle-ebs-bucket', 'PreClone Artifacts Bucket')
    booleanParam('PRIVATE_BUCKET', false, 'Uncheck if Source Bucket is secured and Jenkins will use plain download commands to get the Artifacts.')
    nonStoredPasswordParam('VAULT_PASSWORD', 'Ansible Vault Password. Defaults to pass123 if unset for DEMO Purposes')
  	}

  concurrentBuild(true)
  logRotator(-1, 10)
  label('docker')
  customWorkspace('/workspace/${BUILD_TAG}')
 
  multiscm {
    git {
      remote {
        url(scmProject1)
        credentials(scmCredentialsId)
        branch("*/master")
      }
      extensions {
        relativeTargetDirectory('$WORKSPACE/ec2-create')
      }
    }
    git {     
      remote {
        url(scmProject2)
        credentials(scmCredentialsId)
        branch("*/master")
      }
      extensions {
        relativeTargetDirectory('$WORKSPACE/oracle-ebs-cloning-211')
      }
    }
  }
  
	wrappers {
    preBuildCleanup() 
	  colorizeOutput('css')
	}
  
  steps {
    shell('''#!/bin/bash -e
 
 # Parameter Validation
 if [ ${#ENVIRONMENT}  -gt 10 ]; then
  echo "ENVIRONMENT parameter should only be 10 character or less"
  exit 1
fi
 
 # Create props file 
 if [ ! -f "oracle-ebs-cloning-211/group_vars/${ENVIRONMENT}.yml"  ]; then
  echo "No existing environment file found. Using the default values."
  echo "NEW_ENV=true"  > props
  APP1_HOST="${ENVIRONMENT}app1.${AWS_DOMAIN}"
  DB_HOST="${ENVIRONMENT}db.${AWS_DOMAIN}" 
else
  echo "NEW_ENV=false"  > props
  DOMAIN=$(cat oracle-ebs-cloning-211/group_vars/${ENVIRONMENT}.yml | grep domain: | cut -d' ' -f2 | tr -d '"')
  HOST=$(cat oracle-ebs-cloning-211/group_vars/${ENVIRONMENT}.yml | grep db_host: | cut -d' ' -f2 | tr -d '"')
  DB_HOST="${HOST}.${AWS_DOMAIN}"
  HOST=$(cat oracle-ebs-cloning-211/group_vars/${ENVIRONMENT}.yml | grep app1_host: | cut -d' ' -f2 | tr -d '"')
  APP1_HOST="${HOST}.${AWS_DOMAIN}"
fi 

if [ ${APP1_HOST} == ".${AWS_DOMAIN}" ] || [ ${APP2_HOST} == ".${AWS_DOMAIN}" ] || [ ${DB_HOST} == ".${AWS_DOMAIN}" ]; then
 echo "Some parameters was not defined correctly"
 echo "APP1=${APP1_HOST}"
 echo "DB=${DB_HOST}"
 exit 1
fi

echo "CUSTOM_WORKSPACE=${WORKSPACE}" >> props
echo "DB_HOST=${DB_HOST}" >> props
echo "APP1_HOST=${APP1_HOST}" >> props

# Copy the playbook hosts inventory template
rm -f jenkins_dynamic_inventory
cp ec2-create/jenkins_dynamic_inventory.template .

	''')
  }
  publishers {
    downstreamParameterized {
      trigger(downStreamJobNames) {
 		    condition('SUCCESS')
        parameters {
          currentBuild()
 				  propertiesFile('props', true)
        }
 	    }
 	  }
  }
  
}