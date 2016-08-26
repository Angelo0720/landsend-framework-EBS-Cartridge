

// jenkins credentials id 
def scmCredentialsId = "svn-credentials-id"


def createJob = freeStyleJob ("${ENVIRONMENT} - Promote Single File to PROD")

createJob.with {
  parameters {
    choiceParam('PF_SOURCE_SVN_URL', ["${SCM_PROJECT_URL}/branches/${ENVIRONMENT}/QA"], '')
    choiceParam('PF_TARGET_SVN_URL', ["${SCM_PROJECT_URL}/branches/${ENVIRONMENT}/${ENVIRONMENT}"], '')
    choiceParam('LOG_LEVEL', ['INFO', 'DEBUG'], '')
    choiceParam('SVN_USER', ['oebs_r12_prod'], 'Release Manager for QA branch')
    stringParam('PF_PROMOTE_FILE', '', '''Used to promote a single file. Use relative path below .../branches/QA

Example:
appl_home/appl_top/xxcu/admin/sql/my_sql_file.sql''')
    
    booleanParam('PF_CLEAN_SVN', false, 'Useful if there are conflicts in the files locally on the Jenkins workspace. This option is a lot slower and should only be used when things are broken.')
    stringParam('APPROVAL_REFERENCE', '', 'Mandatory Spitfire ID or Ticket No.')
  }
  configure { project ->
        project / 'properties' / 'hudson.model.ParametersDefinitionProperty' / 'parameterDefinitions' / 'hudson.model.PasswordParameterDefinition' {
            'name'('SVN_PASSWORD')
            'description'('AD Account Password for: $SVN_USER')
            'defaultValue'('apps')
        }
  }
  scm {
      svn {
          location("${SCM_PROJECT_URL}/branches/${ENVIRONMENT}/${ENVIRONMENT}") {
              directory('scripts')
              depth(SvnDepth.INFINITY)
          }
      }
  }
  steps {
    shell ('''#!/bin/bash
./scripts/bin/promoteFilesAndCommit.sh \
      --source-svn-url "$PF_SOURCE_SVN_URL" \
      --target-svn-url "$PF_TARGET_SVN_URL" \
      --commit-comment "Updated from $JOB_NAME via Jenkins. Requested by: ${BUILD_USER_ID} - Approval Ref: ${APPROVAL_REFERENCE}"
    ''')
  }

}