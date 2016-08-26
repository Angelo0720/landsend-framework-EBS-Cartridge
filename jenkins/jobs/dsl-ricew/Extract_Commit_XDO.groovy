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


def createJob = freeStyleJob ("${ENVIRONMENT} - Extract and Commit XDO")

createJob.with {
  parameters {
    choiceParam('PUSH_TARGET', ["${APP_SSH_USER}@${APP_SERVER}"], '')
    choiceParam('COMMIT_TARGET', ['XDOLOAD'], 'SVN Path to commit to')
    choiceParam('LOG_LEVEL', ['INFO', 'DEBUG'], '')
    choiceParam('XXCU_TOP', ["${XXCU_TOP_DIRECTORY}"], 'Must point to the APP_TOP for the XXCU application')
    choiceParam('SCM_USER', ['admin'], '')
    choiceParam('TWO_TASK', ["${DB_NAME}"], '')
    choiceParam('XDO_TYPE_TO_EXTRACT', ['Data Template', 'RTF Template'], '')
    choiceParam('XDO_LANGUAGES', ['en_US', 'en_00'], '')
    activeChoiceReactiveParam('RICEW_APPLICATION') {
            description('')
            choiceType('SINGLE_SELECT')
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

    activeChoiceReactiveParam('RICEW_TO_EXTRACT') {
            description('The FNDLOAD .ldt files will be auto-associated with this RICEW in the RICEW Manager repository')
            filterable()
            choiceType('SINGLE_SELECT')
            groovyScript {
                script('''
import groovy.sql.Sql
import java.sql.SQLException

def list = []
list.add("-- SELECT RICEW --")

try {
  this.class.classLoader.systemClassLoader.addURL(new URL("file:///usr/share/java/postgresql-jdbc.jar"))

  opdb_dbConn = Sql.newInstance(
   "jdbc:postgresql://''' + "${ERP_MANAGER_DB_HOST}" + '''/acn_erp_manager",
        "acn_erp_manager","welcome1","org.postgresql.Driver")

  def sqlStmt = "SELECT DISTINCT RICEW_NAME FROM RICEW WHERE ENABLED = true  AND erp_app_id = (select id from erpapplication where application_name = ${RICEW_APPLICATION}) ORDER BY RICEW_NAME"
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
            referencedParameter('APPS_PASSWORD')
    }
  
    activeChoiceReactiveParam('SCRIPT_ENTRY') {
            description('')
            filterable()
            choiceType('SINGLE_SELECT')
            groovyScript {
                script('''
import groovy.sql.Sql
import java.sql.SQLException

def list = []
list.add(" ")

try {
  this.class.classLoader.systemClassLoader.addURL(new URL("file://''' + "${HOME}" + '''/apache-maven-2.2.1/lib/ojdbc6.jar"))

  if ( RICEW_TO_EXTRACT.equals("-- SELECT RICEW --") ) { return ["Select Application / RICEW first" ] }

  opdb_dbConn = Sql.newInstance(
"jdbc:oracle:thin:xxcu_read/''' + "${DB_READ_USER}@${DB_SERVER}:${DB_PORT}/${DB_NAME}" + '''", "oracle.jdbc.OracleDriver")

  def sqlStmt = "SELECT 'INVALID XDO TYPE' AS ENTRY_VALUE FROM DUAL"

  if ( XDO_TYPE_TO_EXTRACT.equals("Data Template") ) {
    sqlStmt = "select * from ( "+
                "select 'xdo_load_data_templ \"'||APPLICATION_SHORT_NAME||'\" \"'||LOB_CODE||'\"' as ENTRY_VALUE "+
                "from apps.xdo_lobs a "+
                "where xdo_file_type = 'XML-DATA-TEMPLATE' "+
                "and lob_type = 'DATA_TEMPLATE' "+
                "order by last_update_date desc "+
                ") where rownum < 2000"
  }

  if ( XDO_TYPE_TO_EXTRACT.equals("RTF Template") ) {
    sqlStmt = "select * from ( "+
                "select 'xdo_load_rtf_templ \"'||APPLICATION_SHORT_NAME||'\" \"'||LOB_CODE||'\"' as ENTRY_VALUE "+
                "from apps.xdo_lobs a "+
                "where xdo_file_type = 'RTF' "+
                "and lob_type = 'TEMPLATE_SOURCE' "+
                "order by last_update_date desc "+
                ") where rownum < 2000"
  }
  
  opdb_dbConn.eachRow(sqlStmt)
  {
       list.add(it.ENTRY_VALUE)
  }

} catch (Exception ex) {
   list.add(ex.message)
}

return list
				''')
            }
            referencedParameter('XDO_TYPE_TO_EXTRACT')
            referencedParameter('RICEW_TO_EXTRACT')
    }
  }
  configure passwordParam("SCM_PASSWORD","jenkinsci account","apps")
  configure passwordParam("APPS_PASSWORD","","apps")

  multiscm {
        svn {
          location("${SCM_PROJECT_URL}/branches/${ENVIRONMENT}/jenkins/bin") {
                credentials(scmCredentialsId)
                directory('scripts')
                depth(SvnDepth.INFINITY)
            }
            location("${SCM_PROJECT_URL}/branches/${ENVIRONMENT}/appl_home/appl_top/xxcu/bin") {
                credentials(scmCredentialsId)
                directory('bin')
                depth(SvnDepth.INFINITY)
            }
            location("${SCM_PROJECT_URL}/branches/${ENVIRONMENT}/appl_home/appl_top/xxcu/admin/config/XDOLOAD") {
                credentials(scmCredentialsId)
                directory('XDOLOAD')
                depth(SvnDepth.INFINITY)
            }
        }
  }
  
  steps {
    shell ('''#!/bin/bash

chmod -R 755 scripts

./scripts/pushFolders --target $PUSH_TARGET \\
      --src-root-uri "./" \\
      --trg-root-uri "$XXCU_TOP" \\
      --dist-dir-list "bin" \\
      --exclude ".svn" \\
      --exec-remote ". ~/.bash_profile; . ~/APP_PROFILE run; chmod -R 755  $XXCU_TOP/bin; cd $XXCU_TOP/bin; echo $APPS_PASSWORD | LOG_LEVEL=${LOG_LEVEL} RICEW_APPLICATION=${RICEW_APPLICATION} RICEW_TO_EXTRACT=${RICEW_TO_EXTRACT} ./xxcu_extract_dynamic_xdodata.sh '$SCRIPT_ENTRY' /tmp/jenkins_xdo_extract_$BUILD_NUMBER $XDO_LANGUAGES"
if [ $? -eq 0 ]; then
./scripts/fetchFilesAndCommit.sh --target $PUSH_TARGET \\
      --target-dir "/tmp/jenkins_xdo_extract_$BUILD_NUMBER" \\
      --commit-target "$COMMIT_TARGET" \\
      --commit-comment "Updated from $JOB_NAME via Jenkins. Requested by: $BUILD_USER_ID"
else
  exit 1
fi
    ''')
  }
}