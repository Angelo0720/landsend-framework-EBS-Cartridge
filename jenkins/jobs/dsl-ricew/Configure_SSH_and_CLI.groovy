def createJob = freeStyleJob("Configure_SSH_and_CLI") 

createJob.with {
  steps {
    shell('''#!/bin/bash
    
set -e

echo "Generating SSH Key for Jenkins in ${HOME}/userContent/"
mkdir -p ${HOME}/.ssh && chmod 700 ${HOME}/.ssh
cd ${HOME}/.ssh
rm -fr id_rsa*
ssh-keygen -t rsa -f 'id_rsa' -C "jenkins@adopcore" -N '';
echo "Copy key to userContent folder"
mkdir -p ${HOME}/userContent
cp id_rsa.pub ${HOME}/userContent/id_rsa.pub

echo "Downloading CLI jar to ${HOME}"
cd /var/lib/jenkins; wget http://localhost:8080/jenkins/jnlpJars/jenkins-cli.jar

    ''')
  }
}