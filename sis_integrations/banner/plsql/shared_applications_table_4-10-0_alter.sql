ACCEPT owner CHAR PROMPT 'Adaptor user: ';
ALTER TABLE "&owner".SZRSAPP ADD (  RACE_AIAN_OTHER_DESC VARCHAR2(1024 CHAR) );
ALTER TABLE "&owner".SZRSAPP ADD (  STU_DEPS_UNDER_18 NUMBER );
ALTER TABLE "&owner".SZRSAPP ADD (  STU_DEPS_18_OVER NUMBER );
/