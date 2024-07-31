ACCEPT owner CHAR PROMPT 'Adaptor user: ';
ACCEPT tablespace CHAR DEFAULT 'DEVELOPMENT' PROMPT 'Tablespace owner: ';
 CREATE TABLE  "&owner".SZRPGRT
  (	"APP_ID" NUMBER,
 	"CCC_ID" VARCHAR2(8 CHAR) NOT NULL ENABLE,
 	"CONFIRMATION_NUMBER" VARCHAR2(25 CHAR),
 	"STATUS" VARCHAR2(1 CHAR),
 	"APP_LANG" VARCHAR2(2 CHAR),
 	"ACCEPTED_TERMS" VARCHAR2(1 CHAR),
    "ACCEPTED_TERMS_TSTMP" DATE,
    "STUDENT_PARENT" VARCHAR2(1 CHAR),
 	"COLLEGE_ID" VARCHAR2(3 CHAR),
 	"YEAR_CODE" NUMBER,
 	"YEAR_DESCRIPTION" VARCHAR2(100 CHAR),
 	"DETERMINED_RESIDENTCA" VARCHAR2(1 CHAR),
 	"DETERMINED_AB540_ELIGIBLE" VARCHAR2(1 CHAR),
 	"DETERMINED_NON_RES_EXEMPT" VARCHAR2(1 CHAR),
 	"LASTNAME" VARCHAR2(50 CHAR),
 	"FIRSTNAME" VARCHAR2(50 CHAR),
 	"MIDDLENAME" VARCHAR2(50 CHAR),
 	"MAINPHONE" VARCHAR2(14 CHAR),
 	"MAINPHONE_EXT" VARCHAR2(4 CHAR),
 	"MAINPHONE_AUTH_TEXT" VARCHAR2(1 CHAR),
 	"PHONE_VERIFIED" VARCHAR2(1 CHAR),
 	"PHONE_VERIFIED_TSTMP" DATE,
 	"PHONE_TYPE" VARCHAR2(10 CHAR),
 	"EMAIL" VARCHAR2(128 CHAR),
 	"EMAIL_VERIFIED" VARCHAR2(1 CHAR),
 	"EMAIL_VERIFIED_TSTMP" DATE,
 	"NON_US_ADDRESS" VARCHAR2(1 CHAR),
 	"STREETADDRESS1" VARCHAR2(50 CHAR),
 	"STREETADDRESS2" VARCHAR2(50 CHAR),
 	"CITY" VARCHAR2(50 CHAR),
 	"STATE" VARCHAR2(2 CHAR),
 	"PROVINCE" VARCHAR2(30 CHAR),
 	"COUNTRY" VARCHAR2(2 CHAR),
 	"POSTALCODE" VARCHAR2(20 CHAR),
 	"MAIL_ADDR_VALIDATION_OVR" VARCHAR2(1 CHAR),
 	"ADDRESS_VAL_OVERRIDE" VARCHAR2(1 CHAR),
 	"ADDRESS_VAL_OVER_TSTMP" DATE,
 	"PREF_CONTACT_METHOD" VARCHAR2(255 CHAR),
 	"SSN_TEXT" VARCHAR2(20 CHAR),
 	"SSN_TYPE" VARCHAR2(1 CHAR),
 	"STUDENT_COLLEGE_ID" VARCHAR2(20 CHAR),
 	"BIRTHDATE" DATE,
 	"MARITAL_STATUS" VARCHAR2(1 CHAR),
 	"REG_DOM_PARTNER" VARCHAR2(1 CHAR),
 	"BORN_BEFORE_23_YEAR" VARCHAR2(1 CHAR),
 	"MARRIED_OR_RDP" VARCHAR2(1 CHAR),
 	"US_VETERAN" VARCHAR2(1 CHAR),
 	"DEPENDENTS" VARCHAR2(1 CHAR),
 	"PARENTS_DECEASED" VARCHAR2(1 CHAR),
 	"EMANCIPATED_MINOR" VARCHAR2(1 CHAR),
 	"LEGAL_GUARDIANSHIP" VARCHAR2(1 CHAR),
 	"HOMELESS_YOUTH_SCHOOL" VARCHAR2(1 CHAR),
 	"HOMELESS_YOUTH_HUD" VARCHAR2(1 CHAR),
 	"HOMELESS_YOUTH_OTHER" VARCHAR2(1 CHAR),
 	"DEPENDENT_ON_PARENT_TAXES" VARCHAR2(1 CHAR),
 	"LIVING_WITH_PARENTS" VARCHAR2(1 CHAR),
 	"DEPENDENCY_STATUS" VARCHAR2(1 CHAR),
 	"CERT_VETERAN_AFFAIRS" VARCHAR2(1 CHAR),
 	"CERT_NATIONAL_GUARD" VARCHAR2(1 CHAR),
 	"ELIG_MEDAL_HONOR" VARCHAR2(1 CHAR),
 	"ELIG_SEPT_11" VARCHAR2(1 CHAR),
 	"ELIG_POLICE_FIRE" VARCHAR2(1 CHAR),
 	"TANF_CALWORKS" VARCHAR2(1 CHAR),
 	"SSI_SSP" VARCHAR2(1 CHAR),
 	"GENERAL_ASSISTANCE" VARCHAR2(1 CHAR),
 	"PARENTS_ASSISTANCE" VARCHAR2(1 CHAR),
 	"DEP_NUMBER_HOUSEHOLD" NUMBER,
 	"IND_NUMBER_HOUSEHOLD" NUMBER,
 	"DEP_GROSS_INCOME" NUMBER,
 	"IND_GROSS_INCOME" NUMBER,
 	"DEP_OTHER_INCOME" NUMBER,
 	"IND_OTHER_INCOME" NUMBER,
 	"DEP_TOTAL_INCOME" NUMBER,
 	"IND_TOTAL_INCOME" NUMBER,
 	"ELIG_METHOD_A" VARCHAR2(1 CHAR),
 	"ELIG_METHOD_B" VARCHAR2(1 CHAR),
 	"ELIG_BOGFW" VARCHAR2(1 CHAR),
 	"CONFIRMATION_PARENT_GUARDIAN" VARCHAR2(1 CHAR),
 	"PARENT_GUARDIAN_NAME" VARCHAR2(60 CHAR),
 	"ACK_FIN_AID" VARCHAR2(1 CHAR) DEFAULT 'F',
 	"CONFIRMATION_APPLICANT" VARCHAR2(1 CHAR) DEFAULT 'F',
 	"LAST_PAGE" VARCHAR2(25 CHAR),
 	"SSN_LAST4" VARCHAR2(4 CHAR),
 	"TSTMP_SUBMIT" TIMESTAMP (6),
 	"TSTMP_CREATE" TIMESTAMP (6) DEFAULT CURRENT_TIMESTAMP,
 	"TSTMP_UPDATE" TIMESTAMP (6),
 	"TSTMP_DOWNLOAD" TIMESTAMP (6),
 	"TERM_CODE" VARCHAR2(5 CHAR),
 	"IP_ADDRESS" VARCHAR2(15 CHAR),
 	"CAMPAIGN1" VARCHAR2(255 CHAR),
 	"CAMPAIGN2" VARCHAR2(255 CHAR),
 	"CAMPAIGN3" VARCHAR2(255 CHAR),
 	"SSN_EXCEPTION" VARCHAR2(1 CHAR) DEFAULT 'F',
 	"COLLEGE_NAME" VARCHAR2(50 CHAR),
 	"PREFERRED_FIRSTNAME" VARCHAR2(50 CHAR),
 	"PREFERRED_MIDDLENAME" VARCHAR2(50 CHAR),
 	"PREFERRED_LASTNAME" VARCHAR2(50 CHAR),
 	"PREFERRED_NAME" VARCHAR2(1 CHAR) DEFAULT 'F',
 	"SSN_NO" VARCHAR2(1 CHAR),
 	"NO_PERM_ADDRESS_HOMELESS" VARCHAR2(1 CHAR),
 	"NO_MAILING_ADDRESS_HOMELESS" VARCHAR2(1 CHAR),
 	"DETERMINED_HOMELESS" VARCHAR2(1 CHAR),
 	"ELIG_METHOD_D" VARCHAR2(1 CHAR),
 	"MAINPHONEINTL" VARCHAR2(25 CHAR),
 	"ELIG_EXONERATED_CRIME" VARCHAR2(1 CHAR),
 	"ELIG_COVID_DEATH" VARCHAR2(1 CHAR),
 	"IP_ADDR_ACCT_CREATE" VARCHAR2(40 CHAR),
    "IP_ADDR_APP_CREATE" VARCHAR2(40 CHAR),
    "IDME_CONFIRMATION_TSTMP" TIMESTAMP (6),
    "IDME_OPTIN_TSTMP" TIMESTAMP(6),
    "IDME_WORKFLOW_STATUS" VARCHAR2(50 CHAR)
 	) TABLESPACE &tablespace;
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
   COMMENT ON COLUMN "&owner"."SZRPGRT"."ELIG_EXONERATED_CRIME" IS 'eligExoneratedCrime';
   COMMENT ON COLUMN "&owner"."SZRPGRT"."ELIG_COVID_DEATH" IS 'eligCovidDeath';
   COMMENT ON COLUMN "&owner"."SZRPGRT"."PHONE_TYPE" IS 'phoneType';
   COMMENT ON COLUMN "&owner"."SZRPGRT"."ACCEPTED_TERMS" IS 'acceptedTerms';
   COMMENT ON COLUMN "&owner"."SZRPGRT"."ACCEPTED_TERMS_TSTMP" IS 'acceptedTermsTimestamp';
   COMMENT ON COLUMN "&owner"."SZRPGRT"."IP_ADDR_ACCT_CREATE" IS 'ipAddressAtAccountCreation';
   COMMENT ON COLUMN "&owner"."SZRPGRT"."IP_ADDR_APP_CREATE" IS 'ipAddressAtApplicationCreation';
   COMMENT ON COLUMN "&owner"."SZRPGRT"."MAIL_ADDR_VALIDATION_OVR" IS 'mailingAddressValidationOverride';
/
CREATE PUBLIC SYNONYM CC_SZRPGRT FOR "&owner".SZRPGRT;
/
CREATE PUBLIC SYNONYM SZRPGRT FOR "&owner".SZRPGRT;
/
