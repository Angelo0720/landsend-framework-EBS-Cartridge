Closure hiddenParam(String paramName, String paramDescription, String paramDefaultValue) {
    return { project ->
        project / 'properties' / 'hudson.model.ParametersDefinitionProperty' / 'parameterDefinitions' << 'com.wangyin.parameter.WHideParameterDefinition' {
            'name'(paramName)
      		'description'(paramDescription)
        	'defaultValue'(paramDefaultValue)
        }
    }
}

// jenkins credentials id 
def scmCredentialsId = "svn-credentials-id"


def createJob = freeStyleJob ("${ENVIRONMENT} - Install RICEW")

createJob.with {
  concurrentBuild()
  parameters {
    stringParam('ERP_MANAGER_DB_HOST',"${ERP_MANAGER_DB_HOST}",'')
    stringParam('ERP_MANAGER_DB_PORT',"${ERP_MANAGER_DB_PORT}",'')
    choiceParam('XXCU_TOP', ["${XXCU_TOP_DIRECTORY}"], '')
    booleanParam('GENERATE_MASTER_FILES_ONLY', false, '')
    choiceParam('DATABASE_NAME', ["${ENVIRONMENT}"], '')
    choiceParam('PROTOCOL', ['http', 'https'], '')
    activeChoiceParam('LOG_LEVEL') {
            description('')
            choiceType('RADIO')
            groovyScript {
                script('''
def list = []
list.add("INFO:selected")
list.add("DEBUG")

return list
				''')
            }
	}
    activeChoiceParam('RICEW_APPLICATION') {
            description('')
            choiceType('SINGLE_SELECT')
            filterable()
            groovyScript {
                script('''
import groovy.sql.Sql
import java.sql.SQLException

def list = []
list.add("-- SELECT APPLICATION --")

try {

  this.class.classLoader.systemClassLoader.addURL(new URL("file:///usr/share/java/postgresql-jdbc.jar"))

  opdb_dbConn = Sql.newInstance(
"jdbc:postgresql://''' + "${ERP_MANAGER_DB_HOST}" + '''/acn_erp_manager",
        "acn_erp_manager","welcome1","org.postgresql.Driver")

  def sqlStmt = "SELECT DISTINCT APPLICATION_NAME FROM ERPAPPLICATION WHERE ENABLED = true ORDER BY APPLICATION_NAME"
  opdb_dbConn.eachRow(sqlStmt)
  {
       list.add(it.APPLICATION_NAME)
  }

} catch (Exception ex) {
   list.add(ex.message)
}

return list
				''')
            }
    }
    activeChoiceReactiveParam('RICEW_TO_INSTALL') {
            description('')
            filterable()
            choiceType('SINGLE_SELECT')
            groovyScript {
                script('''
import groovy.sql.Sql
import java.sql.SQLException

def list = []
list.add("-- SELECT RICEW --")
list.add("ALL")
list.add("ALL_DB")
list.add("ALL_FORM")
list.add("ALL_FND")
list.add("ALL_OAF")
list.add("ALL_REPORTS")
list.add("ALL_WORKFLOW")
list.add("ALL_XDO")

try {

  this.class.classLoader.systemClassLoader.addURL(new URL("file:///usr/share/java/postgresql-jdbc.jar"))

  opdb_dbConn = Sql.newInstance(
   "jdbc:postgresql://''' + "${ERP_MANAGER_DB_HOST}" + '''/acn_erp_manager",
        "acn_erp_manager","welcome1","org.postgresql.Driver")

  def sqlStmt = "SELECT DISTINCT RICEW_NAME FROM RICEW WHERE ENABLED = true  AND erp_app_id = (select id from erpapplication where application_name = '${RICEW_APPLICATION}') ORDER BY RICEW_NAME"
  opdb_dbConn.eachRow(sqlStmt)
  {
       list.add(it.RICEW_NAME)
  }

} catch (Exception ex) {
   list.add(ex.message)
}

return list

                ''')
            }
            referencedParameter('RICEW_APPLICATION')
    }
  }
  
  configure hiddenParam("SHELL_TARGET","","${APP_SSH_USER}@${APP_SERVER}")
  configure hiddenParam("USE_RICEW_MANAGER","","true")
  
  multiscm {
        svn {
          location("${SCM_PROJECT_URL}/branches/${ENVIRONMENT}/jenkins/bin") {
                credentials(scmCredentialsId)
                directory('jenkins')
                depth(SvnDepth.INFINITY)
            }
          location("${SCM_PROJECT_URL}/branches/${ENVIRONMENT}/appl_home/appl_top/xxcu/bin") {
                credentials(scmCredentialsId)
                directory('bin')
                depth(SvnDepth.INFINITY)
            }
          location("${SCM_PROJECT_URL}/branches/${ENVIRONMENT}/appl_home") {
                credentials(scmCredentialsId)
                directory('appl_home')
                depth(SvnDepth.INFINITY)
            }
        }
  }
  
  steps {
    shell ('''#!/bin/bash -e

export JENKINS_PORT='8080/jenkins'
export PGPASSWORD=${ERP_MANAGER_DB_PASSWORD:-welcome1}
chmod +x jenkins/install_ricew_object.sh

jenkins/install_ricew_object.sh
    ''')
  }
}