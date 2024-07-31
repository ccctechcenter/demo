ACCEPT owner CHAR PROMPT 'Adaptor user: ';
ACCEPT tablespace CHAR DEFAULT 'DEVELOPMENT' PROMPT 'Tablespace owner: ';
 CREATE TABLE  "&owner".SZRPGRT
 (	"APP_ID" NUMBER,
        	"CCC_ID" VARCHAR2(8 BYTE) NOT NULL ENABLE,
        	"CONFIRMATION_NUMBER" VARCHAR2(25 BYTE),
        	"STATUS" VARCHAR2(1 BYTE),
        	"APP_LANG" VARCHAR2(2 BYTE),
        	"COLLEGE_ID" VARCHAR2(3 BYTE),
        	"YEAR_CODE" NUMBER,
        	"YEAR_DESCRIPTION" VARCHAR2(100 BYTE),
        	"DETERMINED_RESIDENTCA" VARCHAR2(1 BYTE),
        	"DETERMINED_AB540_ELIGIBLE" VARCHAR2(1 BYTE),
        	"DETERMINED_NON_RES_EXEMPT" VARCHAR2(1 BYTE),
        	"LASTNAME" VARCHAR2(50 BYTE),
        	"FIRSTNAME" VARCHAR2(50 BYTE),
        	"MIDDLENAME" VARCHAR2(50 BYTE),
        	"MAINPHONE" VARCHAR2(14 BYTE),
        	"MAINPHONE_EXT" VARCHAR2(4 BYTE),
        	"MAINPHONE_AUTH_TEXT" VARCHAR2(1 BYTE),
        	"EMAIL" VARCHAR2(128 BYTE),
        	"NON_US_ADDRESS" VARCHAR2(1 BYTE),
        	"STREETADDRESS1" VARCHAR2(50 BYTE),
        	"STREETADDRESS2" VARCHAR2(50 BYTE),
        	"CITY" VARCHAR2(50 BYTE),
        	"STATE" VARCHAR2(2 BYTE),
        	"PROVINCE" VARCHAR2(30 BYTE),
        	"COUNTRY" VARCHAR2(2 BYTE),
        	"POSTALCODE" VARCHAR2(20 BYTE),
        	"SSN_TEXT" VARCHAR2(20 BYTE),
        	"SSN_TYPE" VARCHAR2(1 BYTE),
        	"STUDENT_COLLEGE_ID" VARCHAR2(20 BYTE),
        	"BIRTHDATE" DATE,
        	"MARITAL_STATUS" VARCHAR2(1 BYTE),
        	"REG_DOM_PARTNER" VARCHAR2(1 BYTE),
        	"BORN_BEFORE_23_YEAR" VARCHAR2(1 BYTE),
        	"MARRIED_OR_RDP" VARCHAR2(1 BYTE),
        	"US_VETERAN" VARCHAR2(1 BYTE),
        	"DEPENDENTS" VARCHAR2(1 BYTE),
        	"PARENTS_DECEASED" VARCHAR2(1 BYTE),
        	"EMANCIPATED_MINOR" VARCHAR2(1 BYTE),
        	"LEGAL_GUARDIANSHIP" VARCHAR2(1 BYTE),
        	"HOMELESS_YOUTH_SCHOOL" VARCHAR2(1 BYTE),
        	"HOMELESS_YOUTH_HUD" VARCHAR2(1 BYTE),
        	"HOMELESS_YOUTH_OTHER" VARCHAR2(1 BYTE),
        	"DEPENDENT_ON_PARENT_TAXES" VARCHAR2(1 BYTE),
        	"LIVING_WITH_PARENTS" VARCHAR2(1 BYTE),
        	"DEPENDENCY_STATUS" VARCHAR2(1 BYTE),
        	"CERT_VETERAN_AFFAIRS" VARCHAR2(1 BYTE),
        	"CERT_NATIONAL_GUARD" VARCHAR2(1 BYTE),
        	"ELIG_MEDAL_HONOR" VARCHAR2(1 BYTE),
        	"ELIG_SEPT_11" VARCHAR2(1 BYTE),
        	"ELIG_POLICE_FIRE" VARCHAR2(1 BYTE),
        	"TANF_CALWORKS" VARCHAR2(1 BYTE),
        	"SSI_SSP" VARCHAR2(1 BYTE),
        	"GENERAL_ASSISTANCE" VARCHAR2(1 BYTE),
        	"PARENTS_ASSISTANCE" VARCHAR2(1 BYTE),
        	"DEP_NUMBER_HOUSEHOLD" NUMBER,
        	"IND_NUMBER_HOUSEHOLD" NUMBER,
        	"DEP_GROSS_INCOME" NUMBER,
        	"IND_GROSS_INCOME" NUMBER,
        	"DEP_OTHER_INCOME" NUMBER,
        	"IND_OTHER_INCOME" NUMBER,
        	"DEP_TOTAL_INCOME" NUMBER,
        	"IND_TOTAL_INCOME" NUMBER,
        	"ELIG_METHOD_A" VARCHAR2(1 BYTE),
        	"ELIG_METHOD_B" VARCHAR2(1 BYTE),
        	"ELIG_BOGFW" VARCHAR2(1 BYTE),
        	"CONFIRMATION_PARENT_GUARDIAN" VARCHAR2(1 BYTE),
        	"PARENT_GUARDIAN_NAME" VARCHAR2(60 BYTE),
        	"ACK_FIN_AID" VARCHAR2(1 BYTE) DEFAULT 'F',
        	"CONFIRMATION_APPLICANT" VARCHAR2(1 BYTE) DEFAULT 'F',
        	"LAST_PAGE" VARCHAR2(25 BYTE),
        	"SSN_LAST4" VARCHAR2(4 BYTE),
        	"TSTMP_SUBMIT" TIMESTAMP (6),
        	"TSTMP_CREATE" TIMESTAMP (6) DEFAULT CURRENT_TIMESTAMP,
        	"TSTMP_UPDATE" TIMESTAMP (6),
        	"TSTMP_DOWNLOAD" TIMESTAMP (6),
        	"TERM_CODE" VARCHAR2(5 BYTE),
        	"IP_ADDRESS" VARCHAR2(15 BYTE),
        	"CAMPAIGN1" VARCHAR2(255 BYTE),
        	"CAMPAIGN2" VARCHAR2(255 BYTE),
        	"CAMPAIGN3" VARCHAR2(255 BYTE),
        	"SSN_EXCEPTION" VARCHAR2(1 BYTE) DEFAULT 'F',
        	"COLLEGE_NAME" VARCHAR2(50 BYTE),
        	"PREFERRED_FIRSTNAME" VARCHAR2(50 BYTE),
        	"PREFERRED_MIDDLENAME" VARCHAR2(50 BYTE),
        	"PREFERRED_LASTNAME" VARCHAR2(50 BYTE),
        	"PREFERRED_NAME" VARCHAR2(1 BYTE) DEFAULT 'F',
        	"SSN_NO" VARCHAR2(1 BYTE),
        	"NO_PERM_ADDRESS_HOMELESS" VARCHAR2(1 BYTE),
        	"NO_MAILING_ADDRESS_HOMELESS" VARCHAR2(1 BYTE),
        	"DETERMINED_HOMELESS" VARCHAR2(1 BYTE),
        	"ELIG_METHOD_D" VARCHAR2(1 BYTE),
        	"MAINPHONEINTL" VARCHAR2(25 BYTE)) TABLESPACE &tablespace;
	 /


   COMMENT ON COLUMN "&owner"."SZRPGRT"."APP_ID" IS 'appId';
   COMMENT ON COLUMN "&owner"."SZRPGRT"."CCC_ID" IS 'cccId';
   COMMENT ON COLUMN "&owner"."SZRPGRT"."CONFIRMATION_NUMBER" IS 'confirmationNumber';
   COMMENT ON COLUMN "&owner"."SZRPGRT"."STATUS" IS 'status';
   COMMENT ON COLUMN "&owner"."SZRPGRT"."APP_LANG" IS 'appLang';
   COMMENT ON COLUMN "&owner"."SZRPGRT"."COLLEGE_ID" IS 'collegeId';
   COMMENT ON COLUMN "&owner"."SZRPGRT"."YEAR_CODE" IS 'yearCode';
   COMMENT ON COLUMN "&owner"."SZRPGRT"."YEAR_DESCRIPTION" IS 'yearDescription';
   COMMENT ON COLUMN "&owner"."SZRPGRT"."DETERMINED_RESIDENTCA" IS 'determinedResidentca';
   COMMENT ON COLUMN "&owner"."SZRPGRT"."DETERMINED_AB540_ELIGIBLE" IS 'determinedAB540Eligible';
   COMMENT ON COLUMN "&owner"."SZRPGRT"."DETERMINED_NON_RES_EXEMPT" IS 'determinedNonResExempt';
   COMMENT ON COLUMN "&owner"."SZRPGRT"."LASTNAME" IS 'lastname';
   COMMENT ON COLUMN "&owner"."SZRPGRT"."FIRSTNAME" IS 'firstname';
   COMMENT ON COLUMN "&owner"."SZRPGRT"."MIDDLENAME" IS 'middlename';
   COMMENT ON COLUMN "&owner"."SZRPGRT"."MAINPHONE" IS 'mainphone';
   COMMENT ON COLUMN "&owner"."SZRPGRT"."MAINPHONE_EXT" IS 'mainphoneExt';
   COMMENT ON COLUMN "&owner"."SZRPGRT"."MAINPHONE_AUTH_TEXT" IS 'mainphoneAuthText';
   COMMENT ON COLUMN "&owner"."SZRPGRT"."EMAIL" IS 'email';
   COMMENT ON COLUMN "&owner"."SZRPGRT"."NON_US_ADDRESS" IS 'nonUsAddress';
   COMMENT ON COLUMN "&owner"."SZRPGRT"."STREETADDRESS1" IS 'streetaddress1';
   COMMENT ON COLUMN "&owner"."SZRPGRT"."STREETADDRESS2" IS 'streetaddress2';
   COMMENT ON COLUMN "&owner"."SZRPGRT"."CITY" IS 'city';
   COMMENT ON COLUMN "&owner"."SZRPGRT"."STATE" IS 'state';
   COMMENT ON COLUMN "&owner"."SZRPGRT"."PROVINCE" IS 'province';
   COMMENT ON COLUMN "&owner"."SZRPGRT"."COUNTRY" IS 'country';
   COMMENT ON COLUMN "&owner"."SZRPGRT"."POSTALCODE" IS 'postalcode';
   COMMENT ON COLUMN "&owner"."SZRPGRT"."SSN_TEXT" IS 'ssn';
   COMMENT ON COLUMN "&owner"."SZRPGRT"."SSN_TYPE" IS 'ssnType';
   COMMENT ON COLUMN "&owner"."SZRPGRT"."STUDENT_COLLEGE_ID" IS 'studentCollegeId';
   COMMENT ON COLUMN "&owner"."SZRPGRT"."BIRTHDATE" IS 'birthdate';
   COMMENT ON COLUMN "&owner"."SZRPGRT"."MARITAL_STATUS" IS 'maritalStatus';
   COMMENT ON COLUMN "&owner"."SZRPGRT"."REG_DOM_PARTNER" IS 'regDomPartner';
   COMMENT ON COLUMN "&owner"."SZRPGRT"."BORN_BEFORE_23_YEAR" IS 'bornBefore23Year';
   COMMENT ON COLUMN "&owner"."SZRPGRT"."MARRIED_OR_RDP" IS 'marriedOrRdp';
   COMMENT ON COLUMN "&owner"."SZRPGRT"."US_VETERAN" IS 'usVeteran';
   COMMENT ON COLUMN "&owner"."SZRPGRT"."DEPENDENTS" IS 'dependents';
   COMMENT ON COLUMN "&owner"."SZRPGRT"."PARENTS_DECEASED" IS 'parentsDeceased';
   COMMENT ON COLUMN "&owner"."SZRPGRT"."EMANCIPATED_MINOR" IS 'emancipatedMinor';
   COMMENT ON COLUMN "&owner"."SZRPGRT"."LEGAL_GUARDIANSHIP" IS 'legalGuardianship';
   COMMENT ON COLUMN "&owner"."SZRPGRT"."HOMELESS_YOUTH_SCHOOL" IS 'homelessYouthSchool';
   COMMENT ON COLUMN "&owner"."SZRPGRT"."HOMELESS_YOUTH_HUD" IS 'homelessYouthHud';
   COMMENT ON COLUMN "&owner"."SZRPGRT"."HOMELESS_YOUTH_OTHER" IS 'homelessYouthOther';
   COMMENT ON COLUMN "&owner"."SZRPGRT"."DEPENDENT_ON_PARENT_TAXES" IS 'dependentOnParentTaxes';
   COMMENT ON COLUMN "&owner"."SZRPGRT"."LIVING_WITH_PARENTS" IS 'livingWithParents';
   COMMENT ON COLUMN "&owner"."SZRPGRT"."DEPENDENCY_STATUS" IS 'dependencyStatus';
   COMMENT ON COLUMN "&owner"."SZRPGRT"."CERT_VETERAN_AFFAIRS" IS 'certVeteranAffairs';
   COMMENT ON COLUMN "&owner"."SZRPGRT"."CERT_NATIONAL_GUARD" IS 'certNationalGuard';
   COMMENT ON COLUMN "&owner"."SZRPGRT"."ELIG_MEDAL_HONOR" IS 'eligMedalHonor';
   COMMENT ON COLUMN "&owner"."SZRPGRT"."ELIG_SEPT_11" IS 'eligSept11';
   COMMENT ON COLUMN "&owner"."SZRPGRT"."ELIG_POLICE_FIRE" IS 'eligPoliceFire';
   COMMENT ON COLUMN "&owner"."SZRPGRT"."TANF_CALWORKS" IS 'tanfCalworks';
   COMMENT ON COLUMN "&owner"."SZRPGRT"."SSI_SSP" IS 'ssiSsp';
   COMMENT ON COLUMN "&owner"."SZRPGRT"."GENERAL_ASSISTANCE" IS 'generalAssistance';
   COMMENT ON COLUMN "&owner"."SZRPGRT"."PARENTS_ASSISTANCE" IS 'parentsAssistance';
   COMMENT ON COLUMN "&owner"."SZRPGRT"."DEP_NUMBER_HOUSEHOLD" IS 'depNumberHousehold';
   COMMENT ON COLUMN "&owner"."SZRPGRT"."IND_NUMBER_HOUSEHOLD" IS 'indNumberHousehold';
   COMMENT ON COLUMN "&owner"."SZRPGRT"."DEP_GROSS_INCOME" IS 'depGrossIncome';
   COMMENT ON COLUMN "&owner"."SZRPGRT"."IND_GROSS_INCOME" IS 'indGrossIncome';
   COMMENT ON COLUMN "&owner"."SZRPGRT"."DEP_OTHER_INCOME" IS 'depOtherIncome';
   COMMENT ON COLUMN "&owner"."SZRPGRT"."IND_OTHER_INCOME" IS 'indOtherIncome';
   COMMENT ON COLUMN "&owner"."SZRPGRT"."DEP_TOTAL_INCOME" IS 'depTotalIncome';
   COMMENT ON COLUMN "&owner"."SZRPGRT"."IND_TOTAL_INCOME" IS 'indTotalIncome';
   COMMENT ON COLUMN "&owner"."SZRPGRT"."ELIG_METHOD_A" IS 'eligMethodA';
   COMMENT ON COLUMN "&owner"."SZRPGRT"."ELIG_METHOD_B" IS 'eligMethodB';
   COMMENT ON COLUMN "&owner"."SZRPGRT"."ELIG_BOGFW" IS 'eligBogfw';
   COMMENT ON COLUMN "&owner"."SZRPGRT"."CONFIRMATION_PARENT_GUARDIAN" IS 'confirmationParentGuardian';
   COMMENT ON COLUMN "&owner"."SZRPGRT"."PARENT_GUARDIAN_NAME" IS 'parentGuardianName';
   COMMENT ON COLUMN "&owner"."SZRPGRT"."ACK_FIN_AID" IS 'ackFinAid';
   COMMENT ON COLUMN "&owner"."SZRPGRT"."CONFIRMATION_APPLICANT" IS 'confirmationApplicant';
   COMMENT ON COLUMN "&owner"."SZRPGRT"."LAST_PAGE" IS 'lastPage';
   COMMENT ON COLUMN "&owner"."SZRPGRT"."SSN_LAST4" IS 'ssnLast4';
   COMMENT ON COLUMN "&owner"."SZRPGRT"."TSTMP_SUBMIT" IS 'tstmpSubmit';
   COMMENT ON COLUMN "&owner"."SZRPGRT"."TSTMP_CREATE" IS 'tstmpCreate';
   COMMENT ON COLUMN "&owner"."SZRPGRT"."TSTMP_UPDATE" IS 'tstmpUpdate';
   COMMENT ON COLUMN "&owner"."SZRPGRT"."TSTMP_DOWNLOAD" IS 'tstmpDownload';
   COMMENT ON COLUMN "&owner"."SZRPGRT"."TERM_CODE" IS 'termCode';
   COMMENT ON COLUMN "&owner"."SZRPGRT"."IP_ADDRESS" IS 'ipAddress';
   COMMENT ON COLUMN "&owner"."SZRPGRT"."CAMPAIGN1" IS 'campaign1';
   COMMENT ON COLUMN "&owner"."SZRPGRT"."CAMPAIGN2" IS 'campaign2';
   COMMENT ON COLUMN "&owner"."SZRPGRT"."CAMPAIGN3" IS 'campaign3';
   COMMENT ON COLUMN "&owner"."SZRPGRT"."SSN_EXCEPTION" IS 'ssnException';
   COMMENT ON COLUMN "&owner"."SZRPGRT"."COLLEGE_NAME" IS 'collegeName';
   COMMENT ON COLUMN "&owner"."SZRPGRT"."PREFERRED_FIRSTNAME" IS 'preferredFirstname';
   COMMENT ON COLUMN "&owner"."SZRPGRT"."PREFERRED_MIDDLENAME" IS 'preferredMiddlename';
   COMMENT ON COLUMN "&owner"."SZRPGRT"."PREFERRED_LASTNAME" IS 'preferredLastname';
   COMMENT ON COLUMN "&owner"."SZRPGRT"."PREFERRED_NAME" IS 'preferredName';
   COMMENT ON COLUMN "&owner"."SZRPGRT"."SSN_NO" IS 'ssnNo';
   COMMENT ON COLUMN "&owner"."SZRPGRT"."NO_PERM_ADDRESS_HOMELESS" IS 'noPermAddressHomeless';
   COMMENT ON COLUMN "&owner"."SZRPGRT"."NO_MAILING_ADDRESS_HOMELESS" IS 'noMailingAddressHomeless';
   COMMENT ON COLUMN "&owner"."SZRPGRT"."DETERMINED_HOMELESS" IS 'determinedHomeless';
   COMMENT ON COLUMN "&owner"."SZRPGRT"."ELIG_METHOD_D" IS 'eligMethodD';
   COMMENT ON COLUMN "&owner"."SZRPGRT"."MAINPHONEINTL" IS 'mainphoneintl';
/
CREATE PUBLIC SYNONYM CC_SZRPGRT FOR "&owner".SZRPGRT;
/
