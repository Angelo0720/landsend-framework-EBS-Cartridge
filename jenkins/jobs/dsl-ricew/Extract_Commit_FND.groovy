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


def createJob = freeStyleJob ("${ENVIRONMENT} - Extract and Commit FND")

createJob.with {
  parameters {
    choiceParam('PUSH_TARGET', ["${APP_SSH_USER}@${APP_SERVER}"], '')
    choiceParam('COMMIT_TARGET', ['FNDLOAD'], 'SVN Path to commit to')
    choiceParam('LOG_LEVEL', ['INFO', 'DEBUG'], '')
    choiceParam('XXCU_TOP', ["${XXCU_TOP_DIRECTORY}"], 'Must point to the APP_TOP for the XXCU application')
    choiceParam('SCM_USER', ['admin'], '')
    choiceParam('TWO_TASK', ["${DB_NAME}"], '')
    choiceParam('FND_TYPE_TO_EXTRACT', ['Alert', 'Audit Group', 'Concurrent Program', 'Form Personalization', 'Descriptive Flexfield', 'Lookup Type', 'Menu', 'Message', 'Printer', 'Profile Option', 'Responsibility', 'Request Group', 'Request Set', 'Value Set'], '')
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

  def sqlStmt = "SELECT DISTINCT APPLICATION_NAME FROM ERPAPPLICATION WHERE ENABLED = true ORDER BY APPLICATION_NAME DESC"
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
    activeChoiceReactiveParam('SCRIPT_ENTRY_1') {
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

  def sqlStmt = "SELECT 'INVALID FND TYPE' AS ENTRY_VALUE FROM DUAL"

  if ( FND_TYPE_TO_EXTRACT.equals("Value Set") ) {

    sqlStmt = "SELECT * FROM (SELECT 'fnd_load_value_set \"'||FLEX_VALUE_SET_NAME||'\" \"'||"+
                   "SUBSTR(description,1,80)||'\"' AS ENTRY_VALUE "+
                   "FROM APPS.FND_FLEX_VALUE_SETS "+
                   "WHERE CREATION_DATE > TRUNC(SYSDATE) - 1000 "+
                   "AND LAST_UPDATED_BY NOT BETWEEN 118 AND 122 "+
                   "ORDER BY LAST_UPDATE_DATE DESC ) WHERE ROWNUM < 2000";
  }

  if ( FND_TYPE_TO_EXTRACT.equals("Lookup Type" ) ) {
    sqlStmt = "SELECT * FROM (SELECT 'fnd_load_lookups \"'||b.APPLICATION_SHORT_NAME||'\" \"'|| "+
                   "a.lookup_type||'\" \"'||SUBSTR(a.meaning,1,80)||'\"' AS ENTRY_VALUE "+
                   "FROM APPS.FND_LOOKUP_TYPES_VL a, APPS.FND_APPLICATION b "+
                   "WHERE a.application_id = b.application_id "+
                   "AND a.CREATION_DATE > TRUNC(SYSDATE) - 1000 "+
                   "AND a.LAST_UPDATED_BY NOT BETWEEN 118 AND 122 "+
                   "ORDER BY a.LAST_UPDATE_DATE DESC ) WHERE ROWNUM < 2000";

  }

  if ( FND_TYPE_TO_EXTRACT.equals("Descriptive Flexfield" ) ) {

sqlStmt = "SELECT * FROM (select 'fnd_load_desc_flexfield \"'||b.application_short_name ||"+
"'\" \"'||a.descriptive_flexfield_name||'\" \"'||SUBSTR(a.description,1,80)AS ENTRY_VALUE "+
"from apps.FND_DESCR_FLEX_CONTEXTS_VL a, apps.fnd_application b "+
"where a.application_id = b.application_id "+
"and a.CREATION_DATE > TRUNC(SYSDATE) - 1000 "+
"AND a.LAST_UPDATED_BY NOT BETWEEN 118 AND 122 "+
"ORDER BY a.LAST_UPDATE_DATE DESC ) WHERE ROWNUM < 2000"

  }

  if ( FND_TYPE_TO_EXTRACT.equals("Menu" ) ) {

sqlStmt = "SELECT * FROM (select 'fnd_load_menus \"'||menu_name||'\" \"'||SUBSTR(a.description,1,80)||'\"' as entry_value "+
"from apps.fnd_menus_vl a "+
"where a.CREATION_DATE > TRUNC(SYSDATE) - 1000 "+
"AND a.LAST_UPDATED_BY NOT BETWEEN 118 AND 122 "+
"ORDER BY a.LAST_UPDATE_DATE DESC) WHERE ROWNUM < 2000"

  }

  if ( FND_TYPE_TO_EXTRACT.equals("Responsibility" ) ) {

sqlStmt = "SELECT * FROM (select 'fnd_load_responsibility \"'||responsibility_key||'\" \"'||responsibility_name||'\"' as entry_value "+
"from apps.fnd_responsibility_vl a "+
"where a.CREATION_DATE > TRUNC(SYSDATE) - 1000 "+
"AND a.LAST_UPDATED_BY NOT BETWEEN 118 AND 122 "+
"ORDER BY a.LAST_UPDATE_DATE DESC) WHERE ROWNUM < 2000"

  }

  if ( FND_TYPE_TO_EXTRACT.equals("Form Personalization" ) ) {

sqlStmt = "SELECT * FROM (select distinct 'fnd_load_forms_pers \"'||function_name||'\"' as entry_value "+
"from apps.fnd_form_custom_rules a "+
"where a.CREATION_DATE > TRUNC(SYSDATE) - 1000 "+
"AND a.LAST_UPDATED_BY NOT BETWEEN 118 AND 122 ) WHERE ROWNUM < 2000 "

  }

  if ( FND_TYPE_TO_EXTRACT.equals("Profile Option" ) ) {

sqlStmt = "SELECT * FROM (select distinct 'fnd_load_profile_option \"'||profile_option_name||'\" \"'||user_profile_option_name||'\"' as entry_value "+
"from apps.fnd_profile_options_vl a "+
"where a.CREATION_DATE > TRUNC(SYSDATE) - 1000 "+
"AND a.LAST_UPDATED_BY NOT BETWEEN 118 AND 122 ) WHERE ROWNUM < 2000 "

  }

  if ( FND_TYPE_TO_EXTRACT.equals("Concurrent Program" ) ) {

sqlStmt = "SELECT * FROM (select 'fnd_load_conc_program \"'||application_short_name||'\" \"' "+
"||concurrent_program_name||'\" \"'||user_concurrent_program_name||'\"' as ENTRY_VALUE "+
"from apps.fnd_concurrent_programs_vl a, apps.fnd_application b "+
"where a.application_id = b.application_id "+
"and a.CREATION_DATE > TRUNC(SYSDATE) - 1000  "+
"ORDER BY a.LAST_UPDATE_DATE DESC ) WHERE ROWNUM < 2000"

  }

  if ( FND_TYPE_TO_EXTRACT.equals("Printer" ) ) {

sqlStmt = "SELECT * FROM (select 'fnd_load_printer \"'||printer_name||'\" \"'||description||'\"' as ENTRY_VALUE "+
"from apps.fnd_printer_vl a "+
"where a.CREATION_DATE > TRUNC(SYSDATE) - 1000  "+
"ORDER BY a.LAST_UPDATE_DATE DESC ) WHERE ROWNUM < 2000"

  }

  if ( FND_TYPE_TO_EXTRACT.equals("Request Set" ) ) {

sqlStmt = "SELECT * FROM (select 'fnd_load_request_sets \"'||b.application_short_name||'\" \"'||a.request_set_name|| "+
"'\" \"'||a.user_request_set_name||'\"' as ENTRY_VALUE "+
"from apps.fnd_request_sets_vl a "+
", apps.fnd_application b "+
"where a.application_id = b.application_id "+
"and a.CREATION_DATE > TRUNC(SYSDATE) - 1000  "+
"ORDER BY a.LAST_UPDATE_DATE DESC ) WHERE ROWNUM < 2000"

  }

  if ( FND_TYPE_TO_EXTRACT.equals("Request Group" ) ) {

sqlStmt = "SELECT * FROM (select 'fnd_load_request_group \"'||request_group_name||'\" \"'||description||'\"' as ENTRY_VALUE "+
"from apps.fnd_request_groups a "+
"where a.CREATION_DATE > TRUNC(SYSDATE) - 1000  "+
"ORDER BY a.LAST_UPDATE_DATE DESC) WHERE ROWNUM < 2000 "

  }

  if ( FND_TYPE_TO_EXTRACT.equals("Alert" ) ) {

sqlStmt = "SELECT * FROM (select 'fnd_load_alert \"'||application_short_name||'\" \"'||alert_name||'\" \"'||description||'\"' AS ENTRY_VALUE "+
"from apps.alr_alerts a "+
", apps.fnd_application b "+
"where a.application_id = b.application_id "+
"AND a.CREATION_DATE > TRUNC(SYSDATE) - 1000  "+
"ORDER BY a.LAST_UPDATE_DATE DESC) WHERE ROWNUM < 2000"

  }

  if ( FND_TYPE_TO_EXTRACT.equals("Message" ) ) {

sqlStmt = "SELECT * FROM (select 'fnd_load_messages \"'||application_short_name||'\" \"'||"+
"message_name||'\" \"'||substr(description,1,80)||'\"' AS ENTRY_VALUE "+
"from apps.fnd_new_messages a "+
", apps.fnd_application b "+
"where a.application_id = b.application_id "+
"AND a.CREATION_DATE > TRUNC(SYSDATE) - 1000  "+
"ORDER BY a.LAST_UPDATE_DATE DESC) WHERE ROWNUM < 2000 "

  }

  if ( FND_TYPE_TO_EXTRACT.equals("Audit Group" ) ) {

sqlStmt = "SELECT * FROM (select 'fnd_load_audit_group \"'||b.application_short_name||"+
"'\" \"'||a.group_name||'\" \"'||description||'\"' AS ENTRY_VALUE "+
"from apps.fnd_audit_groups a "+
", apps.fnd_application b "+
"where a.application_id = b.application_id "+
"ORDER BY a.LAST_UPDATE_DATE DESC) WHERE ROWNUM < 2000"

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
        referencedParameter('FND_TYPE_TO_EXTRACT')
        referencedParameter('RICEW_TO_EXTRACT')
    }
    activeChoiceReactiveParam('SCRIPT_ENTRY_2') {
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

  def sqlStmt = "SELECT 'INVALID FND TYPE' AS ENTRY_VALUE FROM DUAL"

  if ( FND_TYPE_TO_EXTRACT.equals("Value Set") ) {

    sqlStmt = "SELECT * FROM (SELECT 'fnd_load_value_set \"'||FLEX_VALUE_SET_NAME||'\" \"'||"+
                   "SUBSTR(description,1,80)||'\"' AS ENTRY_VALUE "+
                   "FROM APPS.FND_FLEX_VALUE_SETS "+
                   "WHERE CREATION_DATE > TRUNC(SYSDATE) - 1000 "+
                   "AND LAST_UPDATED_BY NOT BETWEEN 118 AND 122 "+
                   "ORDER BY LAST_UPDATE_DATE DESC ) WHERE ROWNUM < 2000";
  }

  if ( FND_TYPE_TO_EXTRACT.equals("Lookup Type" ) ) {
    sqlStmt = "SELECT * FROM (SELECT 'fnd_load_lookups \"'||b.APPLICATION_SHORT_NAME||'\" \"'|| "+
                   "a.lookup_type||'\" \"'||SUBSTR(a.meaning,1,80)||'\"' AS ENTRY_VALUE "+
                   "FROM APPS.FND_LOOKUP_TYPES_VL a, APPS.FND_APPLICATION b "+
                   "WHERE a.application_id = b.application_id "+
                   "AND a.CREATION_DATE > TRUNC(SYSDATE) - 1000 "+
                   "AND a.LAST_UPDATED_BY NOT BETWEEN 118 AND 122 "+
                   "ORDER BY a.LAST_UPDATE_DATE DESC ) WHERE ROWNUM < 2000";

  }

  if ( FND_TYPE_TO_EXTRACT.equals("Descriptive Flexfield" ) ) {

sqlStmt = "SELECT * FROM (select 'fnd_load_desc_flexfield \"'||b.application_short_name ||"+
"'\" \"'||a.descriptive_flexfield_name||'\" \"'||SUBSTR(a.description,1,80)AS ENTRY_VALUE "+
"from apps.FND_DESCR_FLEX_CONTEXTS_VL a, apps.fnd_application b "+
"where a.application_id = b.application_id "+
"and a.CREATION_DATE > TRUNC(SYSDATE) - 1000 "+
"AND a.LAST_UPDATED_BY NOT BETWEEN 118 AND 122 "+
"ORDER BY a.LAST_UPDATE_DATE DESC ) WHERE ROWNUM < 2000"

  }

  if ( FND_TYPE_TO_EXTRACT.equals("Menu" ) ) {

sqlStmt = "SELECT * FROM (select 'fnd_load_menus \"'||menu_name||'\" \"'||SUBSTR(a.description,1,80)||'\"' as entry_value "+
"from apps.fnd_menus_vl a "+
"where a.CREATION_DATE > TRUNC(SYSDATE) - 1000 "+
"AND a.LAST_UPDATED_BY NOT BETWEEN 118 AND 122 "+
"ORDER BY a.LAST_UPDATE_DATE DESC) WHERE ROWNUM < 2000"

  }

  if ( FND_TYPE_TO_EXTRACT.equals("Responsibility" ) ) {

sqlStmt = "SELECT * FROM (select 'fnd_load_responsibility \"'||responsibility_key||'\" \"'||responsibility_name||'\"' as entry_value "+
"from apps.fnd_responsibility_vl a "+
"where a.CREATION_DATE > TRUNC(SYSDATE) - 1000 "+
"AND a.LAST_UPDATED_BY NOT BETWEEN 118 AND 122 "+
"ORDER BY a.LAST_UPDATE_DATE DESC) WHERE ROWNUM < 2000"

  }

  if ( FND_TYPE_TO_EXTRACT.equals("Form Personalization" ) ) {

sqlStmt = "SELECT * FROM (select distinct 'fnd_load_forms_pers \"'||function_name||'\"' as entry_value "+
"from apps.fnd_form_custom_rules a "+
"where a.CREATION_DATE > TRUNC(SYSDATE) - 1000 "+
"AND a.LAST_UPDATED_BY NOT BETWEEN 118 AND 122 ) WHERE ROWNUM < 2000 "

  }


  if ( FND_TYPE_TO_EXTRACT.equals("Profile Option" ) ) {

sqlStmt = "SELECT * FROM (select distinct 'fnd_load_profile_option \"'||profile_option_name||'\" \"'||user_profile_option_name||'\"' as entry_value "+
"from apps.fnd_profile_options_vl a "+
"where a.CREATION_DATE > TRUNC(SYSDATE) - 1000 "+
"AND a.LAST_UPDATED_BY NOT BETWEEN 118 AND 122 ) WHERE ROWNUM < 2000 "
  }

  if ( FND_TYPE_TO_EXTRACT.equals("Concurrent Program" ) ) {

sqlStmt = "SELECT * FROM (select 'fnd_load_conc_program \"'||application_short_name||'\" \"' "+
"||concurrent_program_name||'\" \"'||user_concurrent_program_name||'\"' as ENTRY_VALUE "+
"from apps.fnd_concurrent_programs_vl a, apps.fnd_application b "+
"where a.application_id = b.application_id "+
"and a.CREATION_DATE > TRUNC(SYSDATE) - 1000  "+
"ORDER BY a.LAST_UPDATE_DATE DESC ) WHERE ROWNUM < 2000"

  }

  if ( FND_TYPE_TO_EXTRACT.equals("Printer" ) ) {

sqlStmt = "SELECT * FROM (select 'fnd_load_printer \"'||printer_name||'\" \"'||description||'\"' as ENTRY_VALUE "+
"from apps.fnd_printer_vl a "+
"where a.CREATION_DATE > TRUNC(SYSDATE) - 1000  "+
"ORDER BY a.LAST_UPDATE_DATE DESC ) WHERE ROWNUM < 2000"

  }

  if ( FND_TYPE_TO_EXTRACT.equals("Request Set" ) ) {

sqlStmt = "SELECT * FROM (select 'fnd_load_request_sets \"'||b.application_short_name||'\" \"'||a.request_set_name|| "+
"'\" \"'||a.user_request_set_name||'\"' as ENTRY_VALUE "+
"from apps.fnd_request_sets_vl a "+
", apps.fnd_application b "+
"where a.application_id = b.application_id "+
"and a.CREATION_DATE > TRUNC(SYSDATE) - 1000  "+
"ORDER BY a.LAST_UPDATE_DATE DESC ) WHERE ROWNUM < 2000"

  }


  if ( FND_TYPE_TO_EXTRACT.equals("Request Group" ) ) {

sqlStmt = "SELECT * FROM (select 'fnd_load_request_group \"'||request_group_name||'\" \"'||description||'\"' as ENTRY_VALUE "+
"from apps.fnd_request_groups a "+
"where a.CREATION_DATE > TRUNC(SYSDATE) - 1000  "+
"ORDER BY a.LAST_UPDATE_DATE DESC) WHERE ROWNUM < 2000 "

  }

  if ( FND_TYPE_TO_EXTRACT.equals("Alert" ) ) {

sqlStmt = "SELECT * FROM (select 'fnd_load_alert \"'||application_short_name||'\" \"'||alert_name||'\" \"'||description||'\"' AS ENTRY_VALUE "+
"from apps.alr_alerts a "+
", apps.fnd_application b "+
"where a.application_id = b.application_id "+
"AND a.CREATION_DATE > TRUNC(SYSDATE) - 1000  "+
"ORDER BY a.LAST_UPDATE_DATE DESC) WHERE ROWNUM < 2000"

  }

  if ( FND_TYPE_TO_EXTRACT.equals("Message" ) ) {

sqlStmt = "SELECT * FROM (select 'fnd_load_messages \"'||application_short_name||'\" \"'||"+
"message_name||'\" \"'||substr(description,1,80)||'\"' AS ENTRY_VALUE "+
"from apps.fnd_new_messages a "+
", apps.fnd_application b "+
"where a.application_id = b.application_id "+
"AND a.CREATION_DATE > TRUNC(SYSDATE) - 1000  "+
"ORDER BY a.LAST_UPDATE_DATE DESC) WHERE ROWNUM < 2000 "

}


  if ( FND_TYPE_TO_EXTRACT.equals("Audit Group" ) ) {

sqlStmt = "SELECT * FROM (select 'fnd_load_audit_group \"'||b.application_short_name||"+
"'\" \"'||a.group_name||'\" \"'||description||'\"' AS ENTRY_VALUE "+
"from apps.fnd_audit_groups a "+
", apps.fnd_application b "+
"where a.application_id = b.application_id "+
"ORDER BY a.LAST_UPDATE_DATE DESC) WHERE ROWNUM < 2000"

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
        referencedParameter('FND_TYPE_TO_EXTRACT')
        referencedParameter('RICEW_TO_EXTRACT')
    }
    activeChoiceReactiveParam('SCRIPT_ENTRY_3') {
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

  def sqlStmt = "SELECT 'INVALID FND TYPE' AS ENTRY_VALUE FROM DUAL"

  if ( FND_TYPE_TO_EXTRACT.equals("Value Set") ) {

    sqlStmt = "SELECT * FROM (SELECT 'fnd_load_value_set \"'||FLEX_VALUE_SET_NAME||'\" \"'||"+
                   "SUBSTR(description,1,80)||'\"' AS ENTRY_VALUE "+
                   "FROM APPS.FND_FLEX_VALUE_SETS "+
                   "WHERE CREATION_DATE > TRUNC(SYSDATE) - 1000 "+
                   "AND LAST_UPDATED_BY NOT BETWEEN 118 AND 122 "+
                   "ORDER BY LAST_UPDATE_DATE DESC ) WHERE ROWNUM < 2000";
  }

  if ( FND_TYPE_TO_EXTRACT.equals("Lookup Type" ) ) {
    sqlStmt = "SELECT * FROM (SELECT 'fnd_load_lookups \"'||b.APPLICATION_SHORT_NAME||'\" \"'|| "+
                   "a.lookup_type||'\" \"'||SUBSTR(a.meaning,1,80)||'\"' AS ENTRY_VALUE "+
                   "FROM APPS.FND_LOOKUP_TYPES_VL a, APPS.FND_APPLICATION b "+
                   "WHERE a.application_id = b.application_id "+
                   "AND a.CREATION_DATE > TRUNC(SYSDATE) - 1000 "+
                   "AND a.LAST_UPDATED_BY NOT BETWEEN 118 AND 122 "+
                   "ORDER BY a.LAST_UPDATE_DATE DESC ) WHERE ROWNUM < 2000";

  }

  if ( FND_TYPE_TO_EXTRACT.equals("Descriptive Flexfield" ) ) {

sqlStmt = "SELECT * FROM (select 'fnd_load_desc_flexfield \"'||b.application_short_name ||"+
"'\" \"'||a.descriptive_flexfield_name||'\" \"'||SUBSTR(a.description,1,80)AS ENTRY_VALUE "+
"from apps.FND_DESCR_FLEX_CONTEXTS_VL a, apps.fnd_application b "+
"where a.application_id = b.application_id "+
"and a.CREATION_DATE > TRUNC(SYSDATE) - 1000 "+
"AND a.LAST_UPDATED_BY NOT BETWEEN 118 AND 122 "+
"ORDER BY a.LAST_UPDATE_DATE DESC ) WHERE ROWNUM < 2000"

  }

  if ( FND_TYPE_TO_EXTRACT.equals("Menu" ) ) {

sqlStmt = "SELECT * FROM (select 'fnd_load_menus \"'||menu_name||'\" \"'||SUBSTR(a.description,1,80)||'\"' as entry_value "+
"from apps.fnd_menus_vl a "+
"where a.CREATION_DATE > TRUNC(SYSDATE) - 1000 "+
"AND a.LAST_UPDATED_BY NOT BETWEEN 118 AND 122 "+
"ORDER BY a.LAST_UPDATE_DATE DESC) WHERE ROWNUM < 2000"

  }

  if ( FND_TYPE_TO_EXTRACT.equals("Responsibility" ) ) {

sqlStmt = "SELECT * FROM (select 'fnd_load_responsibility \"'||responsibility_key||'\" \"'||responsibility_name||'\"' as entry_value "+
"from apps.fnd_responsibility_vl a "+
"where a.CREATION_DATE > TRUNC(SYSDATE) - 1000 "+
"AND a.LAST_UPDATED_BY NOT BETWEEN 118 AND 122 "+
"ORDER BY a.LAST_UPDATE_DATE DESC) WHERE ROWNUM < 2000"

  }

  if ( FND_TYPE_TO_EXTRACT.equals("Form Personalization" ) ) {

sqlStmt = "SELECT * FROM (select distinct 'fnd_load_forms_pers \"'||function_name||'\"' as entry_value "+
"from apps.fnd_form_custom_rules a "+
"where a.CREATION_DATE > TRUNC(SYSDATE) - 1000 "+
"AND a.LAST_UPDATED_BY NOT BETWEEN 118 AND 122 ) WHERE ROWNUM < 2000 "

  }


  if ( FND_TYPE_TO_EXTRACT.equals("Profile Option" ) ) {

sqlStmt = "SELECT * FROM (select distinct 'fnd_load_profile_option \"'||profile_option_name||'\" \"'||user_profile_option_name||'\"' as entry_value "+
"from apps.fnd_profile_options_vl a "+
"where a.CREATION_DATE > TRUNC(SYSDATE) - 1000 "+
"AND a.LAST_UPDATED_BY NOT BETWEEN 118 AND 122 ) WHERE ROWNUM < 2000 "
  }

  if ( FND_TYPE_TO_EXTRACT.equals("Concurrent Program" ) ) {

sqlStmt = "SELECT * FROM (select 'fnd_load_conc_program \"'||application_short_name||'\" \"' "+
"||concurrent_program_name||'\" \"'||user_concurrent_program_name||'\"' as ENTRY_VALUE "+
"from apps.fnd_concurrent_programs_vl a, apps.fnd_application b "+
"where a.application_id = b.application_id "+
"and a.CREATION_DATE > TRUNC(SYSDATE) - 1000  "+
"ORDER BY a.LAST_UPDATE_DATE DESC ) WHERE ROWNUM < 2000"

  }

  if ( FND_TYPE_TO_EXTRACT.equals("Printer" ) ) {

sqlStmt = "SELECT * FROM (select 'fnd_load_printer \"'||printer_name||'\" \"'||description||'\"' as ENTRY_VALUE "+
"from apps.fnd_printer_vl a "+
"where a.CREATION_DATE > TRUNC(SYSDATE) - 1000  "+
"ORDER BY a.LAST_UPDATE_DATE DESC ) WHERE ROWNUM < 2000"

  }

  if ( FND_TYPE_TO_EXTRACT.equals("Request Set" ) ) {

sqlStmt = "SELECT * FROM (select 'fnd_load_request_sets \"'||b.application_short_name||'\" \"'||a.request_set_name|| "+
"'\" \"'||a.user_request_set_name||'\"' as ENTRY_VALUE "+
"from apps.fnd_request_sets_vl a "+
", apps.fnd_application b "+
"where a.application_id = b.application_id "+
"and a.CREATION_DATE > TRUNC(SYSDATE) - 1000  "+
"ORDER BY a.LAST_UPDATE_DATE DESC ) WHERE ROWNUM < 2000"

  }


  if ( FND_TYPE_TO_EXTRACT.equals("Request Group" ) ) {

sqlStmt = "SELECT * FROM (select 'fnd_load_request_group \"'||request_group_name||'\" \"'||description||'\"' as ENTRY_VALUE "+
"from apps.fnd_request_groups a "+
"where a.CREATION_DATE > TRUNC(SYSDATE) - 1000  "+
"ORDER BY a.LAST_UPDATE_DATE DESC) WHERE ROWNUM < 2000 "

  }

  if ( FND_TYPE_TO_EXTRACT.equals("Alert" ) ) {

sqlStmt = "SELECT * FROM (select 'fnd_load_alert \"'||application_short_name||'\" \"'||alert_name||'\" \"'||description||'\"' AS ENTRY_VALUE "+
"from apps.alr_alerts a "+
", apps.fnd_application b "+
"where a.application_id = b.application_id "+
"AND a.CREATION_DATE > TRUNC(SYSDATE) - 1000  "+
"ORDER BY a.LAST_UPDATE_DATE DESC) WHERE ROWNUM < 2000"

  }

  if ( FND_TYPE_TO_EXTRACT.equals("Message" ) ) {

sqlStmt = "SELECT * FROM (select 'fnd_load_messages \"'||application_short_name||'\" \"'||"+
"message_name||'\" \"'||substr(description,1,80)||'\"' AS ENTRY_VALUE "+
"from apps.fnd_new_messages a "+
", apps.fnd_application b "+
"where a.application_id = b.application_id "+
"AND a.CREATION_DATE > TRUNC(SYSDATE) - 1000  "+
"ORDER BY a.LAST_UPDATE_DATE DESC) WHERE ROWNUM < 2000 "

}


  if ( FND_TYPE_TO_EXTRACT.equals("Audit Group" ) ) {

sqlStmt = "SELECT * FROM (select 'fnd_load_audit_group \"'||b.application_short_name||"+
"'\" \"'||a.group_name||'\" \"'||description||'\"' AS ENTRY_VALUE "+
"from apps.fnd_audit_groups a "+
", apps.fnd_application b "+
"where a.application_id = b.application_id "+
"ORDER BY a.LAST_UPDATE_DATE DESC) WHERE ROWNUM < 2000"

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
        referencedParameter('FND_TYPE_TO_EXTRACT')
        referencedParameter('RICEW_TO_EXTRACT')
    }
    activeChoiceReactiveParam('SCRIPT_ENTRY_4') {
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

  def sqlStmt = "SELECT 'INVALID FND TYPE' AS ENTRY_VALUE FROM DUAL"

  if ( FND_TYPE_TO_EXTRACT.equals("Value Set") ) {

    sqlStmt = "SELECT * FROM (SELECT 'fnd_load_value_set \"'||FLEX_VALUE_SET_NAME||'\" \"'||"+
                   "SUBSTR(description,1,80)||'\"' AS ENTRY_VALUE "+
                   "FROM APPS.FND_FLEX_VALUE_SETS "+
                   "WHERE CREATION_DATE > TRUNC(SYSDATE) - 1000 "+
                   "AND LAST_UPDATED_BY NOT BETWEEN 118 AND 122 "+
                   "ORDER BY LAST_UPDATE_DATE DESC ) WHERE ROWNUM < 2000";
  }

  if ( FND_TYPE_TO_EXTRACT.equals("Lookup Type" ) ) {
    sqlStmt = "SELECT * FROM (SELECT 'fnd_load_lookups \"'||b.APPLICATION_SHORT_NAME||'\" \"'|| "+
                   "a.lookup_type||'\" \"'||SUBSTR(a.meaning,1,80)||'\"' AS ENTRY_VALUE "+
                   "FROM APPS.FND_LOOKUP_TYPES_VL a, APPS.FND_APPLICATION b "+
                   "WHERE a.application_id = b.application_id "+
                   "AND a.CREATION_DATE > TRUNC(SYSDATE) - 1000 "+
                   "AND a.LAST_UPDATED_BY NOT BETWEEN 118 AND 122 "+
                   "ORDER BY a.LAST_UPDATE_DATE DESC ) WHERE ROWNUM < 2000";

  }

  if ( FND_TYPE_TO_EXTRACT.equals("Descriptive Flexfield" ) ) {

sqlStmt = "SELECT * FROM (select 'fnd_load_desc_flexfield \"'||b.application_short_name ||"+
"'\" \"'||a.descriptive_flexfield_name||'\" \"'||SUBSTR(a.description,1,80)AS ENTRY_VALUE "+
"from apps.FND_DESCR_FLEX_CONTEXTS_VL a, apps.fnd_application b "+
"where a.application_id = b.application_id "+
"and a.CREATION_DATE > TRUNC(SYSDATE) - 1000 "+
"AND a.LAST_UPDATED_BY NOT BETWEEN 118 AND 122 "+
"ORDER BY a.LAST_UPDATE_DATE DESC ) WHERE ROWNUM < 2000"

  }

  if ( FND_TYPE_TO_EXTRACT.equals("Menu" ) ) {

sqlStmt = "SELECT * FROM (select 'fnd_load_menus \"'||menu_name||'\" \"'||SUBSTR(a.description,1,80)||'\"' as entry_value "+
"from apps.fnd_menus_vl a "+
"where a.CREATION_DATE > TRUNC(SYSDATE) - 1000 "+
"AND a.LAST_UPDATED_BY NOT BETWEEN 118 AND 122 "+
"ORDER BY a.LAST_UPDATE_DATE DESC) WHERE ROWNUM < 2000"

  }

  if ( FND_TYPE_TO_EXTRACT.equals("Responsibility" ) ) {

sqlStmt = "SELECT * FROM (select 'fnd_load_responsibility \"'||responsibility_key||'\" \"'||responsibility_name||'\"' as entry_value "+
"from apps.fnd_responsibility_vl a "+
"where a.CREATION_DATE > TRUNC(SYSDATE) - 1000 "+
"AND a.LAST_UPDATED_BY NOT BETWEEN 118 AND 122 "+
"ORDER BY a.LAST_UPDATE_DATE DESC) WHERE ROWNUM < 2000"

  }

  if ( FND_TYPE_TO_EXTRACT.equals("Form Personalization" ) ) {

sqlStmt = "SELECT * FROM (select distinct 'fnd_load_forms_pers \"'||function_name||'\"' as entry_value "+
"from apps.fnd_form_custom_rules a "+
"where a.CREATION_DATE > TRUNC(SYSDATE) - 1000 "+
"AND a.LAST_UPDATED_BY NOT BETWEEN 118 AND 122 ) WHERE ROWNUM < 2000 "

  }


  if ( FND_TYPE_TO_EXTRACT.equals("Profile Option" ) ) {

sqlStmt = "SELECT * FROM (select distinct 'fnd_load_profile_option \"'||profile_option_name||'\" \"'||user_profile_option_name||'\"' as entry_value "+
"from apps.fnd_profile_options_vl a "+
"where a.CREATION_DATE > TRUNC(SYSDATE) - 1000 "+
"AND a.LAST_UPDATED_BY NOT BETWEEN 118 AND 122 ) WHERE ROWNUM < 2000 "
  }

  if ( FND_TYPE_TO_EXTRACT.equals("Concurrent Program" ) ) {

sqlStmt = "SELECT * FROM (select 'fnd_load_conc_program \"'||application_short_name||'\" \"' "+
"||concurrent_program_name||'\" \"'||user_concurrent_program_name||'\"' as ENTRY_VALUE "+
"from apps.fnd_concurrent_programs_vl a, apps.fnd_application b "+
"where a.application_id = b.application_id "+
"and a.CREATION_DATE > TRUNC(SYSDATE) - 1000  "+
"ORDER BY a.LAST_UPDATE_DATE DESC ) WHERE ROWNUM < 2000"

  }

  if ( FND_TYPE_TO_EXTRACT.equals("Printer" ) ) {

sqlStmt = "SELECT * FROM (select 'fnd_load_printer \"'||printer_name||'\" \"'||description||'\"' as ENTRY_VALUE "+
"from apps.fnd_printer_vl a "+
"where a.CREATION_DATE > TRUNC(SYSDATE) - 1000  "+
"ORDER BY a.LAST_UPDATE_DATE DESC ) WHERE ROWNUM < 2000"

  }

  if ( FND_TYPE_TO_EXTRACT.equals("Request Set" ) ) {

sqlStmt = "SELECT * FROM (select 'fnd_load_request_sets \"'||b.application_short_name||'\" \"'||a.request_set_name|| "+
"'\" \"'||a.user_request_set_name||'\"' as ENTRY_VALUE "+
"from apps.fnd_request_sets_vl a "+
", apps.fnd_application b "+
"where a.application_id = b.application_id "+
"and a.CREATION_DATE > TRUNC(SYSDATE) - 1000  "+
"ORDER BY a.LAST_UPDATE_DATE DESC ) WHERE ROWNUM < 2000"

  }


  if ( FND_TYPE_TO_EXTRACT.equals("Request Group" ) ) {

sqlStmt = "SELECT * FROM (select 'fnd_load_request_group \"'||request_group_name||'\" \"'||description||'\"' as ENTRY_VALUE "+
"from apps.fnd_request_groups a "+
"where a.CREATION_DATE > TRUNC(SYSDATE) - 1000  "+
"ORDER BY a.LAST_UPDATE_DATE DESC) WHERE ROWNUM < 2000 "

  }

  if ( FND_TYPE_TO_EXTRACT.equals("Alert" ) ) {

sqlStmt = "SELECT * FROM (select 'fnd_load_alert \"'||application_short_name||'\" \"'||alert_name||'\" \"'||description||'\"' AS ENTRY_VALUE "+
"from apps.alr_alerts a "+
", apps.fnd_application b "+
"where a.application_id = b.application_id "+
"AND a.CREATION_DATE > TRUNC(SYSDATE) - 1000  "+
"ORDER BY a.LAST_UPDATE_DATE DESC) WHERE ROWNUM < 2000"

  }

  if ( FND_TYPE_TO_EXTRACT.equals("Message" ) ) {

sqlStmt = "SELECT * FROM (select 'fnd_load_messages \"'||application_short_name||'\" \"'||"+
"message_name||'\" \"'||substr(description,1,80)||'\"' AS ENTRY_VALUE "+
"from apps.fnd_new_messages a "+
", apps.fnd_application b "+
"where a.application_id = b.application_id "+
"AND a.CREATION_DATE > TRUNC(SYSDATE) - 1000  "+
"ORDER BY a.LAST_UPDATE_DATE DESC) WHERE ROWNUM < 2000 "

}


  if ( FND_TYPE_TO_EXTRACT.equals("Audit Group" ) ) {

sqlStmt = "SELECT * FROM (select 'fnd_load_audit_group \"'||b.application_short_name||"+
"'\" \"'||a.group_name||'\" \"'||description||'\"' AS ENTRY_VALUE "+
"from apps.fnd_audit_groups a "+
", apps.fnd_application b "+
"where a.application_id = b.application_id "+
"ORDER BY a.LAST_UPDATE_DATE DESC) WHERE ROWNUM < 2000"

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
        referencedParameter('FND_TYPE_TO_EXTRACT')
        referencedParameter('RICEW_TO_EXTRACT')
    }
    activeChoiceReactiveParam('SCRIPT_ENTRY_5') {
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

  def sqlStmt = "SELECT 'INVALID FND TYPE' AS ENTRY_VALUE FROM DUAL"

  if ( FND_TYPE_TO_EXTRACT.equals("Value Set") ) {

    sqlStmt = "SELECT * FROM (SELECT 'fnd_load_value_set \"'||FLEX_VALUE_SET_NAME||'\" \"'||"+
                   "SUBSTR(description,1,80)||'\"' AS ENTRY_VALUE "+
                   "FROM APPS.FND_FLEX_VALUE_SETS "+
                   "WHERE CREATION_DATE > TRUNC(SYSDATE) - 1000 "+
                   "AND LAST_UPDATED_BY NOT BETWEEN 118 AND 122 "+
                   "ORDER BY LAST_UPDATE_DATE DESC ) WHERE ROWNUM < 2000";
  }

  if ( FND_TYPE_TO_EXTRACT.equals("Lookup Type" ) ) {
    sqlStmt = "SELECT * FROM (SELECT 'fnd_load_lookups \"'||b.APPLICATION_SHORT_NAME||'\" \"'|| "+
                   "a.lookup_type||'\" \"'||SUBSTR(a.meaning,1,80)||'\"' AS ENTRY_VALUE "+
                   "FROM APPS.FND_LOOKUP_TYPES_VL a, APPS.FND_APPLICATION b "+
                   "WHERE a.application_id = b.application_id "+
                   "AND a.CREATION_DATE > TRUNC(SYSDATE) - 1000 "+
                   "AND a.LAST_UPDATED_BY NOT BETWEEN 118 AND 122 "+
                   "ORDER BY a.LAST_UPDATE_DATE DESC ) WHERE ROWNUM < 2000";

  }

  if ( FND_TYPE_TO_EXTRACT.equals("Descriptive Flexfield" ) ) {

sqlStmt = "SELECT * FROM (select 'fnd_load_desc_flexfield \"'||b.application_short_name ||"+
"'\" \"'||a.descriptive_flexfield_name||'\" \"'||SUBSTR(a.description,1,80)AS ENTRY_VALUE "+
"from apps.FND_DESCR_FLEX_CONTEXTS_VL a, apps.fnd_application b "+
"where a.application_id = b.application_id "+
"and a.CREATION_DATE > TRUNC(SYSDATE) - 1000 "+
"AND a.LAST_UPDATED_BY NOT BETWEEN 118 AND 122 "+
"ORDER BY a.LAST_UPDATE_DATE DESC ) WHERE ROWNUM < 2000"

  }

  if ( FND_TYPE_TO_EXTRACT.equals("Menu" ) ) {

sqlStmt = "SELECT * FROM (select 'fnd_load_menus \"'||menu_name||'\" \"'||SUBSTR(a.description,1,80)||'\"' as entry_value "+
"from apps.fnd_menus_vl a "+
"where a.CREATION_DATE > TRUNC(SYSDATE) - 1000 "+
"AND a.LAST_UPDATED_BY NOT BETWEEN 118 AND 122 "+
"ORDER BY a.LAST_UPDATE_DATE DESC) WHERE ROWNUM < 2000"

  }

  if ( FND_TYPE_TO_EXTRACT.equals("Responsibility" ) ) {

sqlStmt = "SELECT * FROM (select 'fnd_load_responsibility \"'||responsibility_key||'\" \"'||responsibility_name||'\"' as entry_value "+
"from apps.fnd_responsibility_vl a "+
"where a.CREATION_DATE > TRUNC(SYSDATE) - 1000 "+
"AND a.LAST_UPDATED_BY NOT BETWEEN 118 AND 122 "+
"ORDER BY a.LAST_UPDATE_DATE DESC) WHERE ROWNUM < 2000"

  }

  if ( FND_TYPE_TO_EXTRACT.equals("Form Personalization" ) ) {

sqlStmt = "SELECT * FROM (select distinct 'fnd_load_forms_pers \"'||function_name||'\"' as entry_value "+
"from apps.fnd_form_custom_rules a "+
"where a.CREATION_DATE > TRUNC(SYSDATE) - 1000 "+
"AND a.LAST_UPDATED_BY NOT BETWEEN 118 AND 122 ) WHERE ROWNUM < 2000 "

  }


  if ( FND_TYPE_TO_EXTRACT.equals("Profile Option" ) ) {

sqlStmt = "SELECT * FROM (select distinct 'fnd_load_profile_option \"'||profile_option_name||'\" \"'||user_profile_option_name||'\"' as entry_value "+
"from apps.fnd_profile_options_vl a "+
"where a.CREATION_DATE > TRUNC(SYSDATE) - 1000 "+
"AND a.LAST_UPDATED_BY NOT BETWEEN 118 AND 122 ) WHERE ROWNUM < 2000 "
  }

  if ( FND_TYPE_TO_EXTRACT.equals("Concurrent Program" ) ) {

sqlStmt = "SELECT * FROM (select 'fnd_load_conc_program \"'||application_short_name||'\" \"' "+
"||concurrent_program_name||'\" \"'||user_concurrent_program_name||'\"' as ENTRY_VALUE "+
"from apps.fnd_concurrent_programs_vl a, apps.fnd_application b "+
"where a.application_id = b.application_id "+
"and a.CREATION_DATE > TRUNC(SYSDATE) - 1000  "+
"ORDER BY a.LAST_UPDATE_DATE DESC ) WHERE ROWNUM < 2000"

  }

  if ( FND_TYPE_TO_EXTRACT.equals("Printer" ) ) {

sqlStmt = "SELECT * FROM (select 'fnd_load_printer \"'||printer_name||'\" \"'||description||'\"' as ENTRY_VALUE "+
"from apps.fnd_printer_vl a "+
"where a.CREATION_DATE > TRUNC(SYSDATE) - 1000  "+
"ORDER BY a.LAST_UPDATE_DATE DESC ) WHERE ROWNUM < 2000"

  }

  if ( FND_TYPE_TO_EXTRACT.equals("Request Set" ) ) {

sqlStmt = "SELECT * FROM (select 'fnd_load_request_sets \"'||b.application_short_name||'\" \"'||a.request_set_name|| "+
"'\" \"'||a.user_request_set_name||'\"' as ENTRY_VALUE "+
"from apps.fnd_request_sets_vl a "+
", apps.fnd_application b "+
"where a.application_id = b.application_id "+
"and a.CREATION_DATE > TRUNC(SYSDATE) - 1000  "+
"ORDER BY a.LAST_UPDATE_DATE DESC ) WHERE ROWNUM < 2000"

  }


  if ( FND_TYPE_TO_EXTRACT.equals("Request Group" ) ) {

sqlStmt = "SELECT * FROM (select 'fnd_load_request_group \"'||request_group_name||'\" \"'||description||'\"' as ENTRY_VALUE "+
"from apps.fnd_request_groups a "+
"where a.CREATION_DATE > TRUNC(SYSDATE) - 1000  "+
"ORDER BY a.LAST_UPDATE_DATE DESC) WHERE ROWNUM < 2000 "

  }

  if ( FND_TYPE_TO_EXTRACT.equals("Alert" ) ) {

sqlStmt = "SELECT * FROM (select 'fnd_load_alert \"'||application_short_name||'\" \"'||alert_name||'\" \"'||description||'\"' AS ENTRY_VALUE "+
"from apps.alr_alerts a "+
", apps.fnd_application b "+
"where a.application_id = b.application_id "+
"AND a.CREATION_DATE > TRUNC(SYSDATE) - 1000  "+
"ORDER BY a.LAST_UPDATE_DATE DESC) WHERE ROWNUM < 2000"

  }

  if ( FND_TYPE_TO_EXTRACT.equals("Message" ) ) {

sqlStmt = "SELECT * FROM (select 'fnd_load_messages \"'||application_short_name||'\" \"'||"+
"message_name||'\" \"'||substr(description,1,80)||'\"' AS ENTRY_VALUE "+
"from apps.fnd_new_messages a "+
", apps.fnd_application b "+
"where a.application_id = b.application_id "+
"AND a.CREATION_DATE > TRUNC(SYSDATE) - 1000  "+
"ORDER BY a.LAST_UPDATE_DATE DESC) WHERE ROWNUM < 2000 "

}


  if ( FND_TYPE_TO_EXTRACT.equals("Audit Group" ) ) {

sqlStmt = "SELECT * FROM (select 'fnd_load_audit_group \"'||b.application_short_name||"+
"'\" \"'||a.group_name||'\" \"'||description||'\"' AS ENTRY_VALUE "+
"from apps.fnd_audit_groups a "+
", apps.fnd_application b "+
"where a.application_id = b.application_id "+
"ORDER BY a.LAST_UPDATE_DATE DESC) WHERE ROWNUM < 2000"

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
            referencedParameter('FND_TYPE_TO_EXTRACT')
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
            location("${SCM_PROJECT_URL}/branches/${ENVIRONMENT}/appl_home/appl_top/xxcu/admin/config/FNDLOAD") {
                credentials(scmCredentialsId)
                directory('FNDLOAD')
                depth(SvnDepth.INFINITY)
            }
        }
  }
  
  steps {
    shell ('''
#!/bin/bash

chmod -R 755 scripts

ssh $PUSH_TARGET "mkdir -p $XXCU_TOP"
./scripts/pushFolders --target $PUSH_TARGET \\
      --src-root-uri "./" \\
      --trg-root-uri "$XXCU_TOP" \\
      --dist-dir-list "bin" \\
      --exclude ".svn" \\
      --exec-remote ". ~/.bash_profile; . ~/APP_PROFILE run; chmod -R 755 $XXCU_TOP/bin; cd $XXCU_TOP/bin; echo $APPS_PASSWORD | LOG_LEVEL=${LOG_LEVEL} RICEW_APPLICATION=${RICEW_APPLICATION} RICEW_TO_EXTRACT=${RICEW_TO_EXTRACT} ./xxcu_extract_dynamic_fnddata.sh /tmp/jenkins_extract_${TWO_TASK}_$BUILD_NUMBER '$SCRIPT_ENTRY_1' '$SCRIPT_ENTRY_2' '$SCRIPT_ENTRY_3' '$SCRIPT_ENTRY_4' '$SCRIPT_ENTRY_5'"  
if [ $? -eq 0 ]; then
./scripts/fetchFilesAndCommit.sh --target $PUSH_TARGET \\
      --target-dir "/tmp/jenkins_extract_${TWO_TASK}_$BUILD_NUMBER" \\
      --commit-target "$COMMIT_TARGET" \\
      --commit-comment "Updated from $JOB_NAME via Jenkins. Requested by: $BUILD_USER_ID"
else
  exit 1
fi
    ''')
  }

}