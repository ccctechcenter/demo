ACCEPT owner CHAR PROMPT 'Adaptor user: ';
ALTER TABLE "&owner".SZRAPLY ADD (  INTEGRITY_FG_81 CHAR(1) );
/
ALTER TABLE "&owner".SZRAPLY ADD (  NON_CREDIT CHAR(1) );
/
ALTER TABLE "&owner".SZRAPLY ADD (  SIS_PROCESSED_FLAG VARCHAR2(1) );
/
ALTER TABLE "&owner".SZRAPLY ADD (  TSTMP_SIS_PROCESSED DATE );
/
ALTER TABLE "&owner".SZRAPLY ADD (  SIS_PROCESSED_NOTES VARCHAR2(256) );
/