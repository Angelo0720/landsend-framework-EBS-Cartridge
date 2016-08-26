Closure passwordParam(String paramName, String paramDescription, String paramDefaultValue) {
    return { project ->
        project / 'properties' / 'hudson.model.ParametersDefinitionProperty' / 'parameterDefinitions' << 'hudson.model.PasswordParameterDefinition' {
            'name'(paramName)
      		'description'(paramDescription)
        	'defaultValue'(paramDefaultValue)
        }
    }
}



// jenkins credentials id 
def scmCredentialsId = "svn-credentials-id"


def createJob = mavenJob ("${ENVIRONMENT} - Install R12 Database Objects")

createJob.with {
  parameters {
    choiceParam('TARGET_JDBC_URL', ["jdbc:oracle:thin:@${DB_SERVER}:${DB_PORT}/${DB_NAME}"], '')
    stringParam('TARGET_AIFO_FILE', "${HOME}/install/xxcu_upload_dynamic.aifo.ALL", '') 
  }
  configure passwordParam("TARGET_APPS_PASSWORD","","apps")
  configure passwordParam("TARGET_XXCU_PASSWORD","","apps")
  
  multiscm {
    svn {
      location("${SCM_PROJECT_URL}/branches/${ENVIRONMENT}/appl_home/appl_top/xxcu/install") {
        credentials(scmCredentialsId)
        directory('install')
        depth(SvnDepth.INFINITY)
      }
      location("${SCM_PROJECT_URL}/branches/${ENVIRONMENT}/appl_home/appl_top/xxcu/admin/sql") {
        credentials(scmCredentialsId)
        directory('admin/sql')
        depth(SvnDepth.INFINITY)
      }
	}
  }
  
  triggers {
        snapshotDependencies(true)
  }
  
  rootPOM('xxcu_pom.xml')
  goals('--offline com.accenture.maven.plugin:oracle-installation-framework:execute')
  
}