REMARK Fixing issue found in 4.9.0 with international applications
ACCEPT owner CHAR PROMPT 'Adaptor user: ';
ALTER TABLE "&owner".SZRINTL MODIFY (  MAJOR_DESCRIPTION VARCHAR2(100 CHAR) );
/