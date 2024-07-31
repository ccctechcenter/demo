ACCEPT owner CHAR PROMPT 'Adaptor user: ';

DROP SEQUENCE "&owner".SZRFRAD_SEQ;
/
DROP TABLE "&owner".SZRFRAD;
/
drop public synonym SZRFRAD;
/
