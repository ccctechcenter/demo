ACCEPT owner CHAR PROMPT 'Adaptor user: ';
ACCEPT tablespace CHAR DEFAULT 'USERS' PROMPT 'Tablespace owner: ';
ACCEPT index_tablespace CHAR DEFAULT 'USERS' PROMPT 'Index Tablespace owner: ';
-- Placement View table.
create table "&owner".SZRPLMT   (	"SZRPLMT_SEQ_NO" NUMBER(15,0) NOT NULL ENABLE,
                               	"SZRPLMT_PIDM" NUMBER(8,0),
                               	"SZRPLMT_CCCID" VARCHAR2(320 CHAR) NOT NULL ENABLE,
                               	"SZRPLMT_STATEWIDE_STUDENT_ID" VARCHAR2(160 CHAR),
                               	"SZRPLMT_MIS_CODE" VARCHAR2(320 CHAR) NOT NULL ENABLE,
                               	"SZRPLMT_DATA_SOURCE" NUMBER,
                               	"SZRPLMT_ENGLISH" NUMBER,
                               	"SZRPLMT_SLAM_SUPPORT" NUMBER,
                               	"SZRPLMT_STEM_SUPPORT" NUMBER,
                               	"SZRPLMT_IS_ALG_I" VARCHAR2(16 CHAR),
                               	"SZRPLMT_IS_ALG_II" VARCHAR2(16 CHAR),
                               	"SZRPLMT_TRIGONOMETRY" VARCHAR2(16 CHAR),
                               	"SZRPLMT_PRE_CALCULUS" VARCHAR2(16 CHAR),
                               	"SZRPLMT_CALCULUS" VARCHAR2(16 CHAR),
                               	"SZRPLMT_COMPLETED_11TH_GRADE" VARCHAR2(16 CHAR),
                               	"SZRPLMT_CUMULATIVE_GPA" NUMBER(3,2),
                               	"SZRPLMT_ENG_COMP_COURSE_ID" NUMBER,
                               	"SZRPLMT_ENG_COMP_COURSE_GRADE" VARCHAR2(32 CHAR),
                               	"SZRPLMT_MATH_COMP_COURSE_ID" NUMBER,
                               	"SZRPLMT_MATH_COMP_COURSE_GRADE" VARCHAR2(32 CHAR),
                               	"SZRPLMT_MATH_PASS_COURSE_ID" NUMBER,
                               	"SZRPLMT_MATH_PASS_COURSE_GRADE" VARCHAR2(32 CHAR),
                               	"SZRPLMT_ACTIVITY_DATE" DATE NOT NULL ENABLE,
                               	"SZRPLMT_USER_ID" VARCHAR2(480 CHAR),
                               	"SZRPLMT_VPDI_CODE" VARCHAR2(96 CHAR),
                               	"SZRPLMT_STATUS" VARCHAR2(80 CHAR),
                               	"SZRPLMT_TSTMP_ERP_TRANSMIT" DATE,
                               	"SZRPLMT_TSTMP_SIS_TRANSMIT" DATE,
                               	"SZRPLMT_SIS_PROCESSED_FLAG" VARCHAR2(4 CHAR),
                               	"SZRPLMT_TSTMP_SIS_PROCESSED" DATE,
                               	"SZRPLMT_SIS_PROCESSED_NOTES" VARCHAR2(1024 CHAR),
                               	"SZRPLMT_APP_ID" NUMBER,
                               "SZRPLMT_HIGST_GRADE_COMPLETED" VARCHAR2(2 CHAR),
                               constraint PK_SZRPLMT primary key (SZRPLMT_SEQ_NO)
                             ) TABLESPACE &tablespace
/
create index "&owner".IDX_SZRPLMT_ADAPTOR ON "&owner".SZRPLMT ( SZRPLMT_CCCID, SZRPLMT_MIS_CODE ) TABLESPACE &index_tablespace
/
create sequence "&owner".szrplmt_seq_no minvalue 1 maxvalue 999999999999999 start with 1 increment by 1 cache 10
/
create public synonym SZRPLMT for "&owner".SZRPLMT
/
create public synonym SZRPLMT_SEQ_NO for "&owner".SZRPLMT_SEQ_NO
/