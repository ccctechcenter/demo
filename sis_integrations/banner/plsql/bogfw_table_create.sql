ACCEPT owner CHAR PROMPT 'Adaptor user: ';
ACCEPT tablespace CHAR DEFAULT 'DEVELOPMENT' PROMPT 'Tablespace owner: ';
CREATE TABLE "&owner".RZRSAAP (
      RZRSAAP_CCCID 		      varchar(30),
      RZRSAAP_PIDM                number,
      RZRSAAP_MIS_CODE 		      varchar(3),
      RZRSAAP_TERM_CODE           varchar2(6),
      RZRSAAP_MARITAL_STATUS 	  varchar(25),
      RZRSAAP_SRC_MIS_CODE 	      varchar(3),
      RZRSAAP_REG_DOM_PARTNER_IND varchar2(1),
      RZRSAAP_BORN_B4_DT          varchar2(1),
      RZRSAAP_MARRIED_OR_RDP      varchar2(1),
      RZRSAAP_VETERAN_IND         varchar2(1),
      RZRSAAP_DEPENDENTS_IND      varchar2(1),
      RZRSAAP_ORPHAN_WARD_IND     varchar2(1),
      RZRSAAP_EMAN_MINOR_IND      varchar2(1),
      RZRSAAP_LEGAL_GUARD_IND     varchar2(1),
      RZRSAAP_HOMLS_SCHL_IND      varchar2(1),
      RZRSAAP_HOMLS_SHLTR_IND     varchar2(1),
      RZRSAAP_HOMLS_CNTR_PRG_IND  varchar2(1),
      RZRSAAP_PARENTS_TAX         varchar2(1),
      RZRSAAP_LIVE_PARENTS_IND    varchar2(1),
      RZRSAAP_DEPENDENCY_STAT     varchar2(1),
      RZRSAAP_CERT_VET_AFFAIR     varchar2(1),
      RZRSAAP_CERT_NATL_GUARD     varchar2(1),
      RZRSAAP_ELIG_MEDAL_HONOR    varchar2(1),
      RZRSAAP_ELIG_SEPT11         varchar2(1),
      RZRSAAP_ELIG_POLICE_FIRE    varchar2(1),
      RZRSAAP_TANF_CALWORKS       varchar2(1),
      RZRSAAP_SSI_SSP             varchar2(1),
      RZRSAAP_GENERAL_ASSISTANCE  varchar2(1),
      RZRSAAP_PARENTS_ASSISTANCE  varchar2(1),
      RZRSAAP_DEP_NO_HOUSE        varchar2(1),
      RZRSAAP_IND_NO_HOUSE        varchar2(1),
      RZRSAAP_DEP_AGI             number(8,2),
      RZRSAAP_IND_AGI             number(8,2),
      RZRSAAP_DEP_OTHER_INC       number(8,2),
      RZRSAAP_IND_OTHER_INC       number(8,2),
      RZRSAAP_DEP_TOTAL_INC       number(8,2),
      RZRSAAP_IND_TOTAL_INC       number(8,2),
      RZRSAAP_ELIGIBILITY         varchar2(15),
      RZRSAAP_DET_RES_CA          varchar2(1),
      RZRSAAP_DET_AB540_ELIG      varchar2(1),
      RZRSAAP_DET_NON_RES_EXMT    varchar2(1),
      RZRSAAP_ERROR_MESSAGE       varchar2(100),
      RZRSAAP_USER_ID             varchar2(50),
      RZRSAAP_ACTIVITY_DATE       date
) TABLESPACE &tablespace;
/
create public synonym rzrsaap for "&owner".rzrsaap;
/



