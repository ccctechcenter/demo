ACCEPT owner CHAR PROMPT 'Adaptor user: ';
ALTER TABLE "&owner".SZRSAPP ADD (  IDME_CONFIRMATION_TSTMP TIMESTAMP (6) );
ALTER TABLE "&owner".SZRSAPP ADD (  IDME_OPTIN_TSTMP TIMESTAMP(6) );
ALTER TABLE "&owner".SZRSAPP ADD (  IDME_WORKFLOW_STATUS VARCHAR2(50 CHAR) );
/