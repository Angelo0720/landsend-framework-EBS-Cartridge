

// jenkins credentials id 
def scmCredentialsId = "svn-credentials-id"


def createJob = freeStyleJob ("${ENVIRONMENT} - Post Clone Steps")

createJob.with {
  parameters {
    choiceParam('LOG_LEVEL', ['INFO', 'DEBUG'], '')
    choiceParam('TNS_STRING', ["bxdp-scan.bos1.aesgdevapp:1521/AESGDEV"], '')
  }
  
  disabled()
  
  configure { project ->
        project / 'properties' / 'hudson.model.ParametersDefinitionProperty' / 'parameterDefinitions' / 'hudson.model.PasswordParameterDefinition' {
            'name'('APPS_PASSWORD')
            'description'('')
            'defaultValue'('apps')
        }
  }
  configure { project ->
        project / 'triggers' / 'hudson.triggers.TimerTrigger' {
            'spec'('H * * * *')
        }
  }
  
  steps {
    systemGroovyCommand('''import groovy.sql.Sql
import java.sql.SQLException

class Globals {
   static def build = Thread.currentThread().executable
   static def resolver = build.buildVariableResolver
   static String logLevel = resolver.resolve("LOG_LEVEL")
   static String appsPassword = resolver.resolve("APPS_PASSWORD")
   static String tnsString = resolver.resolve("TNS_STRING")
}

void debug(String p)
{
        if ( Globals.logLevel == "DEBUG" ) {
        println("[postCloneSteps][DEBUG]: "+p);
        }
}

void info(String p)
{

    println("[postCloneSteps][INFO]: "+p)
}

this.class.classLoader.systemClassLoader.addURL(new URL("file:///home/jenkins/mvn/apache-maven-2.2.1/lib/ojdbc6.jar"))

def OPDB_CONNECTION_URL      = "jdbc:oracle:thin:apps/"+Globals.appsPassword+"@"+Globals.tnsString
info("Setting up connection to Database") 
opdb_dbConn = Sql.newInstance(OPDB_CONNECTION_URL, "oracle.jdbc.OracleDriver");

def userDBNameStmt = "SELECT USER AS USER_NAME, GLOBAL_NAME as GLOBAL_NAME from GLOBAL_NAME"
opdb_dbConn.eachRow(userDBNameStmt)
{
    info("Connected to "+it.USER_NAME+"@"+it.GLOBAL_NAME)
}

// Get binds in all queries, so we can hardcode literals and make teststeps simpler to code
opdb_dbConn.execute("ALTER SESSION SET CURSOR_SHARING=SIMILAR")

info ("---> Setting workflow mailer ")
opdb_dbConn.execute("UPDATE FND_SVC_COMP_PARAM_VALS "+
"SET PARAMETER_VALUE = 'Workflow Mailer - Vertex' "+
"where parameter_value like 'Workflow%' "+
"and component_id = (  select component_id  from fnd_svc_components "+
"                      where component_name = 'Workflow Notification Mailer') "+
"and parameter_id = (                     "+
"                      SELECT PARAMETER_ID "+
"                      FROM fnd_svc_comp_params_b "+
"                      WHERE PARAMETER_NAME = 'FROM' AND COMPONENT_TYPE='WF_MAILER')")


info ("---> Setting Form Heading in webpage")
opdb_dbConn.execute("update fnd_form_functions_tl "+
" set user_function_name = 'EBS R12 Production' "+
" where function_id = (select function_id from fnd_form_functions "+
" where function_name = 'FWK_HOMEPAGE_BRAND')");


info ("---> Setting Form Heading in Java Forms")
opdb_dbConn.execute("UPDATE FND_PROFILE_OPTION_VALUES "+
" SET PROFILE_OPTION_VALUE = 'EBS R12 Production' "+
" WHERE PROFILE_OPTION_ID =(SELECT PROFILE_OPTION_ID  "+
" FROM FND_PROFILE_OPTIONS "+
" WHERE PROFILE_OPTION_NAME = 'SITENAME')");

info ("---> Setting WF Admin")
opdb_dbConn.execute("UPDATE WF_RESOURCES SET TEXT = 'FND_RESP|FND|FNDWF_ADMIN_WEB|STANDARD' WHERE TYPE = 'WFTKN' AND NAME = 'WF_ADMIN_ROLE'");                


info ("---> Enabling File Server (Tools / Copy Text) on Site Level")
opdb_dbConn.execute("UPDATE FND_PROFILE_OPTION_VALUES "+
" SET PROFILE_OPTION_VALUE = 'Y'  "+
" WHERE LEVEL_ID = 10001 "+
" AND PROFILE_OPTION_ID =(SELECT PROFILE_OPTION_ID  "+
" FROM FND_PROFILE_OPTIONS "+
" WHERE PROFILE_OPTION_NAME = 'FS_ENABLED')");

info "Closing connection to database"
opdb_dbConn.close()

return true
      ''')
  }
  
}