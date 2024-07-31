ACCEPT owner CHAR PROMPT 'Adaptor user: ';
ACCEPT tablespace CHAR DEFAULT 'DEVELOPMENT' PROMPT 'Tablespace owner: ';
create table "&owner".RZRENRL(RZRENRL_MIS_CODE            VARCHAR2(10),
                              RZRENRL_CCCID               VARCHAR2(50),
                              RZRENRL_SIS_TERM_ID         VARCHAR2(6),
			                  RZRENRL_SECTION_MIS_CODE    VARCHAR2(10),
                              RZRENRL_SECTION_SIS_TERM_ID VARCHAR2(6),
			                  RZRENRL_SECTION_ID      VARCHAR2(6),
                              RZRENRL_COLLEGE_NAME        VARCHAR2(50),
                              RZRENRL_SECTION_CID         VARCHAR2(10),
                              RZRENRL_SECTION_TITLE       VARCHAR2(100),
                              RZRENRL_SECTION_UNITS       NUMBER(7,3)) TABLESPACE &tablespace;
/
CREATE PUBLIC SYNONYM RZRENRL FOR "&owner".RZRENRL;
/
