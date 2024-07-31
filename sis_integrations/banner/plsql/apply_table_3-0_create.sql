ACCEPT owner CHAR PROMPT 'Adaptor user: ';
ACCEPT tablespace CHAR DEFAULT 'DEVELOPMENT' PROMPT 'Tablespace owner: ';
 CREATE TABLE  "&owner".SZRAPLY
   (	APP_ID NUMBER,
	CCC_ID VARCHAR2(8 BYTE),
	STATUS VARCHAR2(50 BYTE),
	COLLEGE_ID VARCHAR2(3 BYTE),
	TERM_ID NUMBER,
	MAJOR_ID NUMBER,
	INTENDED_MAJOR VARCHAR2(30 BYTE),
	EDU_GOAL VARCHAR2(50 BYTE),
	HIGHEST_EDU_LEVEL VARCHAR2(5 BYTE),
	CONSENT_INDICATOR CHAR(1 BYTE),
	APP_LANG VARCHAR2(2 BYTE),
	ACK_FIN_AID CHAR(1 BYTE),
	FIN_AID_REF CHAR(1 BYTE),
	CONFIRMATION VARCHAR2(30 BYTE),
	SUP_PAGE_CODE VARCHAR2(30 BYTE),
	LAST_PAGE VARCHAR2(25 BYTE),
	STREETADDRESS1 VARCHAR2(50 BYTE),
	STREETADDRESS2 VARCHAR2(50 BYTE),
	CITY VARCHAR2(50 BYTE),
	POSTALCODE VARCHAR2(20 BYTE),
	STATE VARCHAR2(2 BYTE),
	NONUSAPROVINCE VARCHAR2(30 BYTE),
	COUNTRY VARCHAR2(2 BYTE),
	NON_US_ADDRESS CHAR(1 BYTE),
	EMAIL VARCHAR2(254 BYTE),
	PERM_STREETADDRESS1 VARCHAR2(50 BYTE),
	PERM_STREETADDRESS2 VARCHAR2(50 BYTE),
	PERM_CITY VARCHAR2(50 BYTE),
	PERM_POSTALCODE VARCHAR2(20 BYTE),
	PERM_STATE VARCHAR2(2 BYTE),
	PERM_NONUSAPROVINCE VARCHAR2(30 BYTE),
	PERM_COUNTRY VARCHAR2(2 BYTE),
	ADDRESS_SAME CHAR(1 BYTE),
	MAINPHONE VARCHAR2(19 BYTE),
	MAINPHONE_EXT VARCHAR2(4 BYTE),
	MAINPHONE_AUTH_TEXT CHAR(1 BYTE),
	SECONDPHONE VARCHAR2(19 BYTE),
	SECONDPHONE_EXT VARCHAR2(4 BYTE),
	SECONDPHONE_AUTH_TEXT CHAR(1 BYTE),
	ENROLL_STATUS VARCHAR2(50 BYTE),
	HS_EDU_LEVEL VARCHAR2(50 BYTE),
	HS_COMP_DATE DATE,
	HIGHER_EDU_LEVEL VARCHAR2(50 BYTE),
	HIGHER_COMP_DATE DATE,
	HS_NOT_ATTENDED CHAR(1 BYTE),
	CAHS_GRADUATED CHAR(1 BYTE),
	CAHS_3YEAR CHAR(1 BYTE),
	HS_NAME VARCHAR2(30 BYTE),
	HS_CITY VARCHAR2(20 BYTE),
	HS_STATE VARCHAR2(2 BYTE),
	HS_COUNTRY VARCHAR2(2 BYTE),
	HS_CDS VARCHAR2(6 BYTE),
	HS_CEEB VARCHAR2(7 BYTE),
	HS_NOT_LISTED CHAR(1 BYTE),
	HOME_SCHOOLED CHAR(1 BYTE),
	COLLEGE_COUNT NUMBER,
	HS_ATTENDANCE NUMBER,
	COENROLL_CONFIRM CHAR(1 BYTE),
	GENDER VARCHAR2(50 BYTE),
	PG_FIRSTNAME VARCHAR2(50 BYTE),
	PG_LASTNAME VARCHAR2(50 BYTE),
	PG_REL VARCHAR2(50 BYTE),
	PG1_EDU VARCHAR2(50 BYTE),
	PG2_EDU VARCHAR2(50 BYTE),
	PG_EDU_MIS VARCHAR2(2 BYTE),
	UNDER19_IND CHAR(1 BYTE),
	DEPENDENT_STATUS VARCHAR2(50 BYTE),
	RACE_ETHNIC VARCHAR2(50 BYTE),
	HISPANIC CHAR(1 BYTE),
	RACE_GROUP VARCHAR2(50 BYTE),
	RACE_ETHNIC_FULL VARCHAR2(1000 BYTE),
	SSN VARCHAR2(50 BYTE),
	BIRTHDATE DATE,
	FIRSTNAME VARCHAR2(50 BYTE),
	MIDDLENAME VARCHAR2(50 BYTE),
	LASTNAME VARCHAR2(50 BYTE),
	SUFFIX VARCHAR2(3 BYTE),
	OTHERFIRSTNAME VARCHAR2(50 BYTE),
	OTHERMIDDLENAME VARCHAR2(50 BYTE),
	OTHERLASTNAME VARCHAR2(50 BYTE),
	CITIZENSHIP_STATUS VARCHAR2(50 BYTE),
	ALIEN_REG_NUMBER VARCHAR2(20 BYTE),
	VISA_TYPE VARCHAR2(20 BYTE),
	NO_DOCUMENTS CHAR(1 BYTE),
	ALIEN_REG_ISSUE_DATE DATE,
	ALIEN_REG_EXPIRE_DATE DATE,
	ALIEN_REG_NO_EXPIRE CHAR(1 BYTE),
	MILITARY_STATUS VARCHAR2(50 BYTE),
	MILITARY_DISCHARGE_DATE DATE,
	MILITARY_HOME_STATE VARCHAR2(2 BYTE),
	MILITARY_HOME_COUNTRY VARCHAR2(2 BYTE),
	MILITARY_CA_STATIONED CHAR(1 BYTE),
	MILITARY_LEGAL_RESIDENCE VARCHAR2(2 BYTE),
	CA_RES_2_YEARS CHAR(1 BYTE),
	CA_DATE_CURRENT DATE,
	CA_NOT_ARRIVED CHAR(1 BYTE),
	CA_COLLEGE_EMPLOYEE CHAR(1 BYTE),
	CA_SCHOOL_EMPLOYEE CHAR(1 BYTE),
	CA_SEASONAL_AG CHAR(1 BYTE),
	CA_FOSTER_YOUTH CHAR(1 BYTE),
	CA_OUTSIDE_TAX CHAR(1 BYTE),
	CA_OUTSIDE_TAX_YEAR DATE,
	CA_OUTSIDE_VOTED CHAR(1 BYTE),
	CA_OUTSIDE_VOTED_YEAR DATE,
	CA_OUTSIDE_COLLEGE CHAR(1 BYTE),
	CA_OUTSIDE_COLLEGE_YEAR DATE,
	CA_OUTSIDE_LAWSUIT CHAR(1 BYTE),
	CA_OUTSIDE_LAWSUIT_YEAR DATE,
	RES_STATUS VARCHAR2(50 BYTE),
	RES_STATUS_CHANGE CHAR(1 BYTE),
	RES_PREV_DATE DATE,
	ADM_INELIGIBLE NUMBER,
	ELIG_AB540 CHAR(1 BYTE),
	RES_AREA_A NUMBER,
	RES_AREA_B NUMBER,
	RES_AREA_C NUMBER,
	RES_AREA_D NUMBER,
	EXPERIENCE NUMBER,
	RECOMMEND NUMBER,
	COMMENTS VARCHAR2(50 BYTE),
	COMFORTABLE_ENGLISH CHAR(1 BYTE),
	FINANCIAL_ASSISTANCE CHAR(1 BYTE),
	TANF_SSI_GA CHAR(1 BYTE),
	FOSTER_YOUTHS CHAR(1 BYTE),
	ATHLETIC_INTERCOLLEGIATE CHAR(1 BYTE),
	ATHLETIC_INTRAMURAL CHAR(1 BYTE),
	ATHLETIC_NOT_INTERESTED CHAR(1 BYTE),
	ACADEMIC_COUNSELING CHAR(1 BYTE),
	BASIC_SKILLS CHAR(1 BYTE),
	CALWORKS CHAR(1 BYTE),
	CAREER_PLANNING CHAR(1 BYTE),
	CHILD_CARE CHAR(1 BYTE),
	COUNSELING_PERSONAL CHAR(1 BYTE),
	DSPS CHAR(1 BYTE),
	EOPS CHAR(1 BYTE),
	ESL CHAR(1 BYTE),
	HEALTH_SERVICES CHAR(1 BYTE),
	HOUSING_INFO CHAR(1 BYTE),
	EMPLOYMENT_ASSISTANCE CHAR(1 BYTE),
	ONLINE_CLASSES CHAR(1 BYTE),
	REENTRY_PROGRAM CHAR(1 BYTE),
	SCHOLARSHIP_INFO CHAR(1 BYTE),
	STUDENT_GOVERNMENT CHAR(1 BYTE),
	TESTING_ASSESSMENT CHAR(1 BYTE),
	TRANSFER_INFO CHAR(1 BYTE),
	TUTORING_SERVICES CHAR(1 BYTE),
	VETERANS_SERVICES CHAR(1 BYTE),
	INTEGRITY_FG_01 CHAR(1 BYTE),
	INTEGRITY_FG_02 CHAR(1 BYTE),
	INTEGRITY_FG_03 CHAR(1 BYTE),
	INTEGRITY_FG_04 CHAR(1 BYTE),
	INTEGRITY_FG_11 CHAR(1 BYTE),
	INTEGRITY_FG_47 CHAR(1 BYTE),
	INTEGRITY_FG_48 CHAR(1 BYTE),
	INTEGRITY_FG_49 CHAR(1 BYTE),
	INTEGRITY_FG_50 CHAR(1 BYTE),
	INTEGRITY_FG_51 CHAR(1 BYTE),
	INTEGRITY_FG_52 CHAR(1 BYTE),
	INTEGRITY_FG_53 CHAR(1 BYTE),
	INTEGRITY_FG_54 CHAR(1 BYTE),
	INTEGRITY_FG_55 CHAR(1 BYTE),
	INTEGRITY_FG_56 CHAR(1 BYTE),
	INTEGRITY_FG_57 CHAR(1 BYTE),
	INTEGRITY_FG_58 CHAR(1 BYTE),
	INTEGRITY_FG_59 CHAR(1 BYTE),
	INTEGRITY_FG_60 CHAR(1 BYTE),
	INTEGRITY_FG_61 CHAR(1 BYTE),
	INTEGRITY_FG_62 CHAR(1 BYTE),
	INTEGRITY_FG_63 CHAR(1 BYTE),
	INTEGRITY_FG_70 CHAR(1 BYTE),
	INTEGRITY_FG_80 CHAR(1 BYTE),
	COL1_CEEB VARCHAR2(7 BYTE),
	COL1_CDS VARCHAR2(6 BYTE),
	COL1_NOT_LISTED CHAR(1 BYTE),
	COL1_NAME VARCHAR2(30 BYTE),
	COL1_CITY VARCHAR2(20 BYTE),
	COL1_STATE VARCHAR2(30 BYTE),
	COL1_COUNTRY VARCHAR2(2 BYTE),
	COL1_START_DATE DATE,
	COL1_END_DATE DATE,
	COL1_DEGREE_DATE DATE,
	COL1_DEGREE_OBTAINED VARCHAR2(50 BYTE),
	COL2_CEEB VARCHAR2(7 BYTE),
	COL2_CDS VARCHAR2(6 BYTE),
	COL2_NOT_LISTED CHAR(1 BYTE),
	COL2_NAME VARCHAR2(30 BYTE),
	COL2_CITY VARCHAR2(20 BYTE),
	COL2_STATE VARCHAR2(30 BYTE),
	COL2_COUNTRY VARCHAR2(2 BYTE),
	COL2_START_DATE DATE,
	COL2_END_DATE DATE,
	COL2_DEGREE_DATE DATE,
	COL2_DEGREE_OBTAINED VARCHAR2(50 BYTE),
	COL3_CEEB VARCHAR2(7 BYTE),
	COL3_CDS VARCHAR2(6 BYTE),
	COL3_NOT_LISTED CHAR(1 BYTE),
	COL3_NAME VARCHAR2(30 BYTE),
	COL3_CITY VARCHAR2(20 BYTE),
	COL3_STATE VARCHAR2(30 BYTE),
	COL3_COUNTRY VARCHAR2(2 BYTE),
	COL3_START_DATE DATE,
	COL3_END_DATE DATE,
	COL3_DEGREE_DATE DATE,
	COL3_DEGREE_OBTAINED VARCHAR2(50 BYTE),
	COL4_CEEB VARCHAR2(7 BYTE),
	COL4_CDS VARCHAR2(6 BYTE),
	COL4_NOT_LISTED CHAR(1 BYTE),
	COL4_NAME VARCHAR2(30 BYTE),
	COL4_CITY VARCHAR2(20 BYTE),
	COL4_STATE VARCHAR2(30 BYTE),
	COL4_COUNTRY VARCHAR2(2 BYTE),
	COL4_START_DATE DATE,
	COL4_END_DATE DATE,
	COL4_DEGREE_DATE DATE,
	COL4_DEGREE_OBTAINED VARCHAR2(50 BYTE),
	COLLEGE_NAME VARCHAR2(50 BYTE),
	DISTRICT_NAME VARCHAR2(50 BYTE),
	TERM_CODE VARCHAR2(15 BYTE),
	TERM_DESCRIPTION VARCHAR2(100 BYTE),
	MAJOR_CODE VARCHAR2(30 BYTE),
	MAJOR_DESCRIPTION VARCHAR2(100 BYTE),
	TSTMP_SUBMIT TIMESTAMP (6),
	TSTMP_CREATE TIMESTAMP (6),
	TSTMP_UPDATE TIMESTAMP (6),
	SSN_DISPLAY VARCHAR2(11 BYTE),
	FOSTER_YOUTH_STATUS VARCHAR2(50 BYTE),
	FOSTER_YOUTH_PREFERENCE CHAR(1 BYTE),
	FOSTER_YOUTH_MIS CHAR(1 BYTE),
	FOSTER_YOUTH_PRIORITY CHAR(1 BYTE),
	TSTMP_DOWNLOAD TIMESTAMP (6),
	ADDRESS_VALIDATION VARCHAR2(50 BYTE),
	ZIP4 VARCHAR2(4 BYTE),
	PERM_ADDRESS_VALIDATION VARCHAR2(50 BYTE),
	PERM_ZIP4 VARCHAR2(4 BYTE),
	DISCHARGE_TYPE VARCHAR2(50 BYTE),
	COLLEGE_EXPELLED_SUMMARY CHAR(1 BYTE),
	COL1_EXPELLED_STATUS CHAR(1 BYTE),
	COL2_EXPELLED_STATUS CHAR(1 BYTE),
	COL3_EXPELLED_STATUS CHAR(1 BYTE),
	COL4_EXPELLED_STATUS CHAR(1 BYTE),
	INTEGRITY_FLAGS VARCHAR2(255 BYTE),
	RDD DATE,
	SSN_TYPE VARCHAR2(50 BYTE),
	MILITARY_STATIONED_CA_ED CHAR(1 BYTE),
	INTEGRITY_FG_65 CHAR(1 BYTE),
	INTEGRITY_FG_64 CHAR(1 BYTE),
	IP_ADDRESS VARCHAR2(15 BYTE),
	CAMPAIGN1 VARCHAR2(255 BYTE),
	CAMPAIGN2 VARCHAR2(255 BYTE),
	CAMPAIGN3 VARCHAR2(255 BYTE),
	ORIENTATION_ENCRYPTED VARCHAR2(50 BYTE),
	TRANSGENDER_ENCRYPTED VARCHAR2(50 BYTE),
	SSN_EXCEPTION CHAR(1 BYTE),
	INTEGRITY_FG_71 CHAR(1 BYTE),
	PREFERRED_FIRSTNAME VARCHAR2(50 BYTE),
	PREFERRED_MIDDLENAME VARCHAR2(50 BYTE),
	PREFERRED_LASTNAME VARCHAR2(50 BYTE),
	PREFERRED_NAME CHAR(1 BYTE),
	SSN_NO CHAR(1 BYTE),
	COMPLETED_ELEVENTH_GRADE CHAR(1 BYTE),
	GRADE_POINT_AVERAGE VARCHAR2(5 BYTE),
	HIGHEST_ENGLISH_COURSE NUMBER,
	HIGHEST_ENGLISH_GRADE VARCHAR2(2 BYTE),
	HIGHEST_MATH_COURSE_TAKEN NUMBER,
	HIGHEST_MATH_TAKEN_GRADE VARCHAR2(2 BYTE),
	HIGHEST_MATH_COURSE_PASSED NUMBER,
	HIGHEST_MATH_PASSED_GRADE VARCHAR2(2 BYTE),
	INTEGRITY_FG_30 CHAR(1 BYTE),
	HS_CDS_FULL VARCHAR2(14 BYTE),
	COL1_CDS_FULL VARCHAR2(14 BYTE),
	COL2_CDS_FULL VARCHAR2(14 BYTE),
	COL3_CDS_FULL VARCHAR2(14 BYTE),
	COL4_CDS_FULL VARCHAR2(14 BYTE),
	SSID VARCHAR2(10 BYTE),
	NO_PERM_ADDRESS_HOMELESS CHAR(1 BYTE),
	NO_MAILING_ADDRESS_HOMELESS CHAR(1 BYTE),
	TERM_START DATE,
	TERM_END DATE,
	HOMELESS_YOUTH CHAR(1 BYTE),
	INTEGRITY_FG_40 CHAR(1 BYTE),
	CIP_CODE VARCHAR2(6 BYTE),
	MAJOR_CATEGORY VARCHAR2(100 BYTE),
	MAINPHONEINTL VARCHAR2(25 BYTE),
	SECONDPHONEINTL VARCHAR2(25 BYTE),
	SUPP_TEXT_01 VARCHAR2(250 BYTE),
	SUPP_TEXT_02 VARCHAR2(250 BYTE),
	SUPP_TEXT_03 VARCHAR2(250 BYTE),
	SUPP_TEXT_04 VARCHAR2(250 BYTE),
	SUPP_TEXT_05 VARCHAR2(250 BYTE),
	SUPP_TEXT_06 VARCHAR2(250 BYTE),
	SUPP_TEXT_07 VARCHAR2(250 BYTE),
	SUPP_TEXT_08 VARCHAR2(250 BYTE),
	SUPP_TEXT_09 VARCHAR2(250 BYTE),
	SUPP_TEXT_10 VARCHAR2(250 BYTE),
	SUPP_TEXT_11 VARCHAR2(250 BYTE),
	SUPP_TEXT_12 VARCHAR2(250 BYTE),
	SUPP_TEXT_13 VARCHAR2(250 BYTE),
	SUPP_TEXT_14 VARCHAR2(250 BYTE),
	SUPP_TEXT_15 VARCHAR2(250 BYTE),
	SUPP_TEXT_16 VARCHAR2(250 BYTE),
	SUPP_TEXT_17 VARCHAR2(250 BYTE),
	SUPP_TEXT_18 VARCHAR2(250 BYTE),
	SUPP_TEXT_19 VARCHAR2(250 BYTE),
	SUPP_TEXT_20 VARCHAR2(250 BYTE),
	SUPP_CHECK_01 CHAR(1 BYTE),
	SUPP_CHECK_02 CHAR(1 BYTE),
	SUPP_CHECK_03 CHAR(1 BYTE),
	SUPP_CHECK_04 CHAR(1 BYTE),
	SUPP_CHECK_05 CHAR(1 BYTE),
	SUPP_CHECK_06 CHAR(1 BYTE),
	SUPP_CHECK_07 CHAR(1 BYTE),
	SUPP_CHECK_08 CHAR(1 BYTE),
	SUPP_CHECK_09 CHAR(1 BYTE),
	SUPP_CHECK_10 CHAR(1 BYTE),
	SUPP_CHECK_11 CHAR(1 BYTE),
	SUPP_CHECK_12 CHAR(1 BYTE),
	SUPP_CHECK_13 CHAR(1 BYTE),
	SUPP_CHECK_14 CHAR(1 BYTE),
	SUPP_CHECK_15 CHAR(1 BYTE),
	SUPP_CHECK_16 CHAR(1 BYTE),
	SUPP_CHECK_17 CHAR(1 BYTE),
	SUPP_CHECK_18 CHAR(1 BYTE),
	SUPP_CHECK_19 CHAR(1 BYTE),
	SUPP_CHECK_20 CHAR(1 BYTE),
	SUPP_CHECK_21 CHAR(1 BYTE),
	SUPP_CHECK_22 CHAR(1 BYTE),
	SUPP_CHECK_23 CHAR(1 BYTE),
	SUPP_CHECK_24 CHAR(1 BYTE),
	SUPP_CHECK_25 CHAR(1 BYTE),
	SUPP_CHECK_26 CHAR(1 BYTE),
	SUPP_CHECK_27 CHAR(1 BYTE),
	SUPP_CHECK_28 CHAR(1 BYTE),
	SUPP_CHECK_29 CHAR(1 BYTE),
	SUPP_CHECK_30 CHAR(1 BYTE),
	SUPP_CHECK_31 CHAR(1 BYTE),
	SUPP_CHECK_32 CHAR(1 BYTE),
	SUPP_CHECK_33 CHAR(1 BYTE),
	SUPP_CHECK_34 CHAR(1 BYTE),
	SUPP_CHECK_35 CHAR(1 BYTE),
	SUPP_CHECK_36 CHAR(1 BYTE),
	SUPP_CHECK_37 CHAR(1 BYTE),
	SUPP_CHECK_38 CHAR(1 BYTE),
	SUPP_CHECK_39 CHAR(1 BYTE),
	SUPP_CHECK_40 CHAR(1 BYTE),
	SUPP_CHECK_41 CHAR(1 BYTE),
	SUPP_CHECK_42 CHAR(1 BYTE),
	SUPP_CHECK_43 CHAR(1 BYTE),
	SUPP_CHECK_44 CHAR(1 BYTE),
	SUPP_CHECK_45 CHAR(1 BYTE),
	SUPP_CHECK_46 CHAR(1 BYTE),
	SUPP_CHECK_47 CHAR(1 BYTE),
	SUPP_CHECK_48 CHAR(1 BYTE),
	SUPP_CHECK_49 CHAR(1 BYTE),
	SUPP_CHECK_50 CHAR(1 BYTE),
	SUPP_YESNO_01 CHAR(1 BYTE),
	SUPP_YESNO_02 CHAR(1 BYTE),
	SUPP_YESNO_03 CHAR(1 BYTE),
	SUPP_YESNO_04 CHAR(1 BYTE),
	SUPP_YESNO_05 CHAR(1 BYTE),
	SUPP_YESNO_06 CHAR(1 BYTE),
	SUPP_YESNO_07 CHAR(1 BYTE),
	SUPP_YESNO_08 CHAR(1 BYTE),
	SUPP_YESNO_09 CHAR(1 BYTE),
	SUPP_YESNO_10 CHAR(1 BYTE),
	SUPP_YESNO_11 CHAR(1 BYTE),
	SUPP_YESNO_12 CHAR(1 BYTE),
	SUPP_YESNO_13 CHAR(1 BYTE),
	SUPP_YESNO_14 CHAR(1 BYTE),
	SUPP_YESNO_15 CHAR(1 BYTE),
	SUPP_YESNO_16 CHAR(1 BYTE),
	SUPP_YESNO_17 CHAR(1 BYTE),
	SUPP_YESNO_18 CHAR(1 BYTE),
	SUPP_YESNO_19 CHAR(1 BYTE),
	SUPP_YESNO_20 CHAR(1 BYTE),
	SUPP_YESNO_21 CHAR(1 BYTE),
	SUPP_YESNO_22 CHAR(1 BYTE),
	SUPP_YESNO_23 CHAR(1 BYTE),
	SUPP_YESNO_24 CHAR(1 BYTE),
	SUPP_YESNO_25 CHAR(1 BYTE),
	SUPP_YESNO_26 CHAR(1 BYTE),
	SUPP_YESNO_27 CHAR(1 BYTE),
	SUPP_YESNO_28 CHAR(1 BYTE),
	SUPP_YESNO_29 CHAR(1 BYTE),
	SUPP_YESNO_30 CHAR(1 BYTE),
	SUPP_MENU_01 VARCHAR2(60 BYTE),
	SUPP_MENU_02 VARCHAR2(60 BYTE),
	SUPP_MENU_03 VARCHAR2(60 BYTE),
	SUPP_MENU_04 VARCHAR2(60 BYTE),
	SUPP_MENU_05 VARCHAR2(60 BYTE),
	SUPP_MENU_06 VARCHAR2(60 BYTE),
	SUPP_MENU_07 VARCHAR2(60 BYTE),
	SUPP_MENU_08 VARCHAR2(60 BYTE),
	SUPP_MENU_09 VARCHAR2(60 BYTE),
	SUPP_MENU_10 VARCHAR2(60 BYTE),
	SUPP_MENU_11 VARCHAR2(60 BYTE),
	SUPP_MENU_12 VARCHAR2(60 BYTE),
	SUPP_MENU_13 VARCHAR2(60 BYTE),
	SUPP_MENU_14 VARCHAR2(60 BYTE),
	SUPP_MENU_15 VARCHAR2(60 BYTE),
	SUPP_MENU_16 VARCHAR2(60 BYTE),
	SUPP_MENU_17 VARCHAR2(60 BYTE),
	SUPP_MENU_18 VARCHAR2(60 BYTE),
	SUPP_MENU_19 VARCHAR2(60 BYTE),
	SUPP_MENU_20 VARCHAR2(60 BYTE),
	SUPP_MENU_21 VARCHAR2(60 BYTE),
	SUPP_MENU_22 VARCHAR2(60 BYTE),
	SUPP_MENU_23 VARCHAR2(60 BYTE),
	SUPP_MENU_24 VARCHAR2(60 BYTE),
	SUPP_MENU_25 VARCHAR2(60 BYTE),
	SUPP_MENU_26 VARCHAR2(60 BYTE),
	SUPP_MENU_27 VARCHAR2(60 BYTE),
	SUPP_MENU_28 VARCHAR2(60 BYTE),
	SUPP_MENU_29 VARCHAR2(60 BYTE),
	SUPP_MENU_30 VARCHAR2(60 BYTE),
	SUPP_DATE_01 DATE,
	SUPP_DATE_02 DATE,
	SUPP_DATE_03 DATE,
	SUPP_DATE_04 DATE,
	SUPP_DATE_05 DATE,
	SUPP_STATE_01 VARCHAR2(2 BYTE),
	SUPP_STATE_02 VARCHAR2(2 BYTE),
	SUPP_STATE_03 VARCHAR2(2 BYTE),
	SUPP_STATE_04 VARCHAR2(2 BYTE),
	SUPP_STATE_05 VARCHAR2(2 BYTE),
	SUPP_COUNTRY_01 VARCHAR2(2 BYTE),
	SUPP_COUNTRY_02 VARCHAR2(2 BYTE),
	SUPP_COUNTRY_03 VARCHAR2(2 BYTE),
	SUPP_COUNTRY_04 VARCHAR2(2 BYTE),
	SUPP_COUNTRY_05 VARCHAR2(2 BYTE),
	SUPP_PHONENUMBER_01 VARCHAR2(25 BYTE),
	SUPP_PHONENUMBER_02 VARCHAR2(25 BYTE),
	SUPP_PHONENUMBER_03 VARCHAR2(25 BYTE),
	SUPP_PHONENUMBER_04 VARCHAR2(25 BYTE),
	SUPP_PHONENUMBER_05 VARCHAR2(25 BYTE),
	SUPP_SECRET_01 VARCHAR2(2000 BYTE),
	SUPP_SECRET_02 VARCHAR2(2000 BYTE),
	SUPP_SECRET_03 VARCHAR2(2000 BYTE),
	SUPP_SECRET_04 VARCHAR2(2000 BYTE),
	SUPP_SECRET_05 VARCHAR2(2000 BYTE),
	INTEGRITY_FG_81 CHAR(1 BYTE),
    NON_CREDIT CHAR(1 BYTE),
    SIS_PROCESSED_FLAG	VARCHAR2(1 BYTE),
    TSTMP_SIS_PROCESSED	 DATE,
    SIS_PROCESSED_NOTES	VARCHAR2(256 BYTE))
    TABLESPACE &tablespace;
	 /
CREATE PUBLIC SYNONYM SZRAPLY FOR "&owner".SZRAPLY;
/


