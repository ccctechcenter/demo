package com.ccctc.adaptor.model.apply;

import com.ccctc.adaptor.model.PrimaryKey;
import lombok.Getter;
import lombok.Setter;

import java.io.Serializable;
import java.time.LocalDate;
import java.time.LocalDateTime;

@Getter
@Setter
@PrimaryKey({"collegeId", "appId"})
public class InternationalApplication implements Serializable {

    //
    // NOTE: collegeId in this model is what we call "misCode", the 3-digit college code
    //


    //app_id bigint NOT NULL
    private Long appId;

    //accepted_terms Boolean
    private Boolean acceptedTerms;

    //accepted_terms_timestamp TIMESTAMP
    private LocalDateTime acceptedTermsTimestamp;

    //ccc_id character varying(8) NOT NULL
    private String cccId;

    //college_id character(3)
    private String collegeId;

    // address_verified boolean,
    private Boolean addressVerified;

    //international_agent_email
    private String agentEmail;

    // agent_phone_ext varchar(4),
    private String agentPhoneExt;

    // agent_phone_number varchar(25),
    private String agentPhoneNumber;

    // alt_non_us_phone_auth_txt boolean,
    private Boolean altNonUsPhoneAuthTxt;

    // alt_non_us_phone_ext varchar(4),
    private String altNonUsPhoneExt;

    // alt_non_us_phone_number varchar(25),
    private String altNonUsPhoneNumber;

    // app_lang char(2),
    private String appLang;

    // authorize_agent_info_release boolean,
    private Boolean authorizeAgentInfoRelease;

    // birthdate date,
    private LocalDate birthdate;

    // campaign1 varchar(255),
    private String campaign1;

    // campaign2 varchar(255),
    private String campaign2;

    // campaign3 varchar(255),
    private String campaign3;

    // citizenship_status varchar(1),
    private String citizenshipStatus;

    // city varchar(50),
    private String city;

    // coenroll_confirm char(1),
    private String coenrollConfirm;

    // col1_cds varchar(30),
    private String col1Cds;

    // col1_ceeb varchar(30),
    private String col1Ceeb;

    // col1_city varchar(50),
    private String col1City;

    // col1_college_attended_id bigint,
    private Long col1CollegeAttendedId;

    // col1_country char(2),
    private String col1Country;

    // col1_degree_date date,
    private LocalDate col1DegreeDate;

    // col1_degree_obtained char(1),
    private String col1DegreeObtained;

    // col1_end_date date,
    private LocalDate col1EndDate;

    // col1_expelled_status boolean,
    private Boolean col1ExpelledStatus;

    // col1_major varchar(50),
    private String col1Major;

    // col1_name varchar(30),
    private String col1Name;

    // col1_non_us_province varchar(50),
    private String col1NonUsProvince;

    // col1_not_listed boolean,
    private Boolean col1NotListed;

    // col1_primary_instruction_language varchar(5010),
    private String col1PrimaryInstructionLanguage;

    // col1_start_date date,
    private LocalDate col1StartDate;

    // col1_state varchar(2),
    private String col1State;

    // col2_cds varchar(30),
    private String col2Cds;

    // col2_ceeb varchar(30),
    private String col2Ceeb;

    // col2_city varchar(50),
    private String col2City;

    // col2_college_attended_id bigint,
    private Long col2CollegeAttendedId;

    // col2_country char(2),
    private String col2Country;

    // col2_degree_date date,
    private LocalDate col2DegreeDate;

    // col2_degree_obtained char(1),
    private String col2DegreeObtained;

    // col2_end_date date,
    private LocalDate col2EndDate;

    // col2_expelled_status boolean,
    private Boolean col2ExpelledStatus;

    // col2_major varchar(50),
    private String col2Major;

    // col2_name varchar(30),
    private String col2Name;

    // col2_non_us_province varchar(50),
    private String col2NonUsProvince;

    // col2_not_listed boolean,
    private Boolean col2NotListed;

    // col2_primary_instruction_language varchar(10),
    private String col2PrimaryInstructionLanguage;

    // col2_start_date date,
    private LocalDate col2StartDate;

    // col2_state varchar(2),
    private String col2State;

    // col3_cds varchar(30),
    private String col3Cds;

    // col3_ceeb varchar(30),
    private String col3Ceeb;

    // col3_city varchar(50),
    private String col3City;

    // col3_college_attended_id bigint,
    private Long col3CollegeAttendedId;

    // col3_country char(2),
    private String col3Country;

    // col3_degree_date date,
    private LocalDate col3DegreeDate;

    // col3_degree_obtained char(1),
    private String col3DegreeObtained;

    // col3_end_date date,
    private LocalDate col3EndDate;

    // col3_expelled_status boolean,
    private Boolean col3ExpelledStatus;

    // col3_major varchar(50),
    private String col3Major;

    // col3_name varchar(30),
    private String col3Name;

    // col3_non_us_province varchar(50),
    private String col3NonUsProvince;

    // col3_not_listed boolean,
    private Boolean col3NotListed;

    // col3_primary_instruction_language varchar(10),
    private String col3PrimaryInstructionLanguage;

    // col3_start_date date,
    private LocalDate col3StartDate;

    // col3_state varchar(2),
    private String col3State;

    // col4_cds varchar(30),
    private String col4Cds;

    // col4_ceeb varchar(30),
    private String col4Ceeb;

    // col4_city varchar(50),
    private String col4City;

    // col4_college_attended_id bigint,
    private Long col4CollegeAttendedId;

    // col4_country char(2),
    private String col4Country;

    // col4_degree_date date,
    private LocalDate col4DegreeDate;

    // col4_degree_obtained char(1),
    private String col4DegreeObtained;

    // col4_end_date date,
    private LocalDate col4EndDate;

    // col4_expelled_status boolean,
    private Boolean col4ExpelledStatus;

    // col4_major varchar(50),
    private String col4Major;

    // col4_name varchar(30),
    private String col4Name;

    // col4_non_us_province varchar(50),
    private String col4NonUsProvince;

    // col4_not_listed boolean,
    private Boolean col4NotListed;

    // col4_primary_instruction_language varchar(10),
    private String col4PrimaryInstructionLanguage;

    // col4_start_date date,
    private LocalDate col4StartDate;

    // col4_state varchar(2),
    private String col4State;

    // college_comp_date date,
    private LocalDate collegeCompDate;

    // college_count smallint,
    private Integer collegeCount;

    // college_edu_level char(12),
    private String collegeEduLevel;

    // college_expelled_summary boolean,
    private Boolean collegeExpelledSummary;

    // college_name char(50),
    private String collegeName;

    // company varchar(50),
    private String company;

    // confirmation varchar(30),
    private String confirmation;

    // consent boolean,
    private Boolean consent;

    // contact varchar(50),
    private String contact;

    // country varchar(2),
    private String country;

    // country_of_birth char(2),
    private String countryOfBirth;

    // country_of_citizenship char(2),
    private String countryOfCitizenship;

    // cryptokeyid integer,
    private Integer cryptokeyid;

    // current_mailing_address_outside_us boolean,
    private Boolean currentMailingAddressOutsideUs;

    // current_mailing_address_verified boolean,
    private Boolean currentMailingAddressVerified;

    //address_validation_override
    private Boolean addressValidationOverride;

    //address_validation_override_timestamp
    private LocalDateTime addressValidationOverrideTimestamp;

    // current_mailing_city varchar(50),
    private String currentMailingCity;

    // current_mailing_country char(2),
    private String currentMailingCountry;

    // current_mailing_non_us_address boolean,
    private Boolean currentMailingNonUsAddress;

    // current_mailing_non_us_postal_code varchar(30),
    private String currentMailingNonUsPostalCode;

    // current_mailing_non_us_province varchar(50),
    private String currentMailingNonUsProvince;

    // current_mailing_same_as_permanent boolean,
    private Boolean currentMailingSameAsPermanent;

    // current_mailing_state char(2),
    private String currentMailingState;

    // current_mailing_street_1 varchar(50),
    private String currentMailingStreet1;

    // current_mailing_street_2 varchar(50),
    private String currentMailingStreet2;

    // current_mailing_zip_code varchar(20),
    private String currentMailingZipCode;

    // dep1_country_of_birth varchar(2),
    private String dep1CountryOfBirth;

    // dep1_date_of_birth date,
    private LocalDate dep1DateOfBirth;

    // dep1_dependent_id bigint,
    private Long dep1DependentId;

    // dep1_first_name varchar(25),
    private String dep1FirstName;

    // dep1_gender varchar(1),
    private String dep1Gender;

    // dep1_last_name varchar(25),
    private String dep1LastName;

    // dep1_no_first_name boolean,
    private Boolean dep1NoFirstName;

    // dep1_relationship varchar(20),
    private String dep1Relationship;

    // dep10_country_of_birth varchar(2),
    private String dep10CountryOfBirth;

    // dep10_date_of_birth date,
    private LocalDate dep10DateOfBirth;

    // dep10_dependent_id bigint,
    private Long dep10DependentId;

    // dep10_first_name varchar(25),
    private String dep10FirstName;

    // dep10_gender varchar(1),
    private String dep10Gender;

    // dep10_last_name varchar(25),
    private String dep10LastName;

    // dep10_no_first_name boolean,
    private Boolean dep10NoFirstName;

    // dep10_relationship varchar(20),
    private String dep10Relationship;

    // dep2_country_of_birth varchar(2),
    private String dep2CountryOfBirth;

    // dep2_date_of_birth date,
    private LocalDate dep2DateOfBirth;

    // dep2_dependent_id bigint,
    private Long dep2DependentId;

    // dep2_first_name varchar(25),
    private String dep2FirstName;

    // dep2_gender varchar(1),
    private String dep2Gender;

    // dep2_last_name varchar(25),
    private String dep2LastName;

    // dep2_no_first_name boolean,
    private Boolean dep2NoFirstName;

    // dep2_relationship varchar(20),
    private String dep2Relationship;

    // dep3_country_of_birth varchar(2),
    private String dep3CountryOfBirth;

    // dep3_date_of_birth date,
    private LocalDate dep3DateOfBirth;

    // dep3_dependent_id bigint,
    private Long dep3DependentId;

    // dep3_first_name varchar(25),
    private String dep3FirstName;

    // dep3_gender varchar(1),
    private String dep3Gender;

    // dep3_last_name varchar(25),
    private String dep3LastName;

    // dep3_no_first_name boolean,
    private Boolean dep3NoFirstName;

    // dep3_relationship varchar(20),
    private String dep3Relationship;

    // dep4_country_of_birth varchar(2),
    private String dep4CountryOfBirth;

    // dep4_date_of_birth date,
    private LocalDate dep4DateOfBirth;

    // dep4_dependent_id bigint,
    private Long dep4DependentId;

    // dep4_first_name varchar(25),
    private String dep4FirstName;

    // dep4_gender varchar(1),
    private String dep4Gender;

    // dep4_last_name varchar(25),
    private String dep4LastName;

    // dep4_no_first_name boolean,
    private Boolean dep4NoFirstName;

    // dep4_relationship varchar(20),
    private String dep4Relationship;

    // dep5_country_of_birth varchar(2),
    private String dep5CountryOfBirth;

    // dep5_date_of_birth date,
    private LocalDate dep5DateOfBirth;

    // dep5_dependent_id bigint,
    private Long dep5DependentId;

    // dep5_first_name varchar(25),
    private String dep5FirstName;

    // dep5_gender varchar(1),
    private String dep5Gender;

    // dep5_last_name varchar(25),
    private String dep5LastName;

    // dep5_no_first_name boolean,
    private Boolean dep5NoFirstName;

    // dep5_relationship varchar(20),
    private String dep5Relationship;

    // dep6_country_of_birth varchar(2),
    private String dep6CountryOfBirth;

    // dep6_date_of_birth date,
    private LocalDate dep6DateOfBirth;

    // dep6_dependent_id bigint,
    private Long dep6DependentId;

    // dep6_first_name varchar(25),
    private String dep6FirstName;

    // dep6_gender varchar(1),
    private String dep6Gender;

    // dep6_last_name varchar(25),
    private String dep6LastName;

    // dep6_no_first_name boolean,
    private Boolean dep6NoFirstName;

    // dep6_relationship varchar(20),
    private String dep6Relationship;

    // dep7_country_of_birth varchar(2),
    private String dep7CountryOfBirth;

    // dep7_date_of_birth date,
    private LocalDate dep7DateOfBirth;

    // dep7_dependent_id bigint,
    private Long dep7DependentId;

    // dep7_first_name varchar(25),
    private String dep7FirstName;

    // dep7_gender varchar(1),
    private String dep7Gender;

    // dep7_last_name varchar(25),
    private String dep7LastName;

    // dep7_no_first_name boolean,
    private Boolean dep7NoFirstName;

    // dep7_relationship varchar(20),
    private String dep7Relationship;

    // dep8_country_of_birth varchar(2),
    private String dep8CountryOfBirth;

    // dep8_date_of_birth date,
    private LocalDate dep8DateOfBirth;

    // dep8_dependent_id bigint,
    private Long dep8DependentId;

    // dep8_first_name varchar(25),
    private String dep8FirstName;

    // dep8_gender varchar(1),
    private String dep8Gender;

    // dep8_last_name varchar(25),
    private String dep8LastName;

    // dep8_no_first_name boolean,
    private Boolean dep8NoFirstName;

    // dep8_relationship varchar(20),
    private String dep8Relationship;

    // dep9_country_of_birth varchar(2),
    private String dep9CountryOfBirth;

    // dep9_date_of_birth date,
    private LocalDate dep9DateOfBirth;

    // dep9_dependent_id bigint,
    private Long dep9DependentId;

    // dep9_first_name varchar(25),
    private String dep9FirstName;

    // dep9_gender varchar(1),
    private String dep9Gender;

    // dep9_last_name varchar(25),
    private String dep9LastName;

    // dep9_no_first_name boolean,
    private Boolean dep9NoFirstName;

    // dep9_relationship varchar(20),
    private String dep9Relationship;

    // educational_goal char(1),
    private String educationalGoal;

    // email varchar(254),
    private String email;

    //email_verified
    private Boolean emailVerified;

    //email_verified_timestamp
    private LocalDateTime emailVerifiedTimestamp;

    //preferred_method_of_contact character varying(30)
    private String preferredMethodOfContact;

    // emergency_contact_address_verified boolean,
    private Boolean emergencyContactAddressVerified;

    // emergency_contact_city varchar(50),
    private String emergencyContactCity;

    // emergency_contact_country char(2),
    private String emergencyContactCountry;

    // emergency_contact_email varchar(50),
    private String emergencyContactEmail;

    // emergency_contact_first_name varchar(25),
    private String emergencyContactFirstName;

    // emergency_contact_last_name varchar(25),
    private String emergencyContactLastName;

    // emergency_contact_no_first_name boolean,
    private Boolean emergencyContactNoFirstName;

    // emergency_contact_non_us_address boolean,
    private Boolean emergencyContactNonUsAddress;

    // emergency_contact_non_us_postal_code varchar(30),
    private String emergencyContactNonUsPostalCode;

    // emergency_contact_non_us_province varchar(50),
    private String emergencyContactNonUsProvince;

    // emergency_contact_phone_auth_txt boolean,
    private Boolean emergencyContactPhoneAuthTxt;

    // emergency_contact_phone_ext varchar(4),
    private String emergencyContactPhoneExt;

    // emergency_contact_phone_number varchar(25),
    private String emergencyContactPhoneNumber;

    // emergency_contact_relationship varchar(20),
    private String emergencyContactRelationship;

    // emergency_contact_state char(2),
    private String emergencyContactState;

    // emergency_contact_street_1 varchar(50),
    private String emergencyContactStreet1;

    // emergency_contact_street_2 varchar(50),
    private String emergencyContactStreet2;

    // emergency_contact_zip_code varchar(20),
    private String emergencyContactZipCode;

    // eng_months_studied smallint,
    private Integer engMonthsStudied;

    // eng_proficiency_date date,
    private LocalDate engProficiencyDate;

    // eng_proficiency_other varchar(50),
    private String engProficiencyOther;

    // eng_proficiency_prerequisite boolean,
    private Boolean engProficiencyPrerequisite;

    // eng_proficiency_req text,
    private String engProficiencyReq;

    // eng_proficiency_score char(6),
    private String engProficiencyScore;

    // eng_proficiency_show_score boolean,
    private Boolean engProficiencyShowScore;

    // eng_proficiency_type char(2),
    private String engProficiencyType;

    // enroll_status char(1),
    private String enrollStatus;

    // enroll_term_code varchar(15),
    private String enrollTermCode;

    // enroll_term_description varchar(100),
    private String enrollTermDescription;

    // esignature boolean,
    private Boolean esignature;

    // expiration_date date,
    private LocalDate expirationDate;

    // fax_number_number varchar(25),
    private String faxNumberNumber;

    // firstname varchar(50),
    private String firstname;

    // gender char(1),
    private String gender;

    // higher_lang varchar(10),
    private String higherLang;

    // highest_comp_date date,
    private LocalDate highestCompDate;

    // highest_edu_level varchar(5),
    private String highestEduLevel;

    // hispanic boolean,
    private Boolean hispanic;

    // hs_address_verified boolean,
    private Boolean hsAddressVerified;

    // hs_cds char(30),
    private String hsCds;

    // hs_ceeb char(30),
    private String hsCeeb;

    // hs_city varchar(50),
    private String hsCity;

    // hs_comp_date date,
    private LocalDate hsCompDate;

    // hs_country char(2),
    private String hsCountry;

    // hs_diploma_cert char(12),
    private String hsDiplomaCert;

    // hs_diploma_cert_date date,
    private LocalDate hsDiplomaCertDate;

    // hs_edu_level char(1),
    private String hsEduLevel;

    // hs_end_date date,
    private LocalDate hsEndDate;

    // hs_lang varchar(10),
    private String hsLang;

    // hs_name varchar(40),
    private String hsName;

    // hs_non_us_address boolean,
    private Boolean hsNonUsAddress;

    // hs_non_us_postal_code varchar(30),
    private String hsNonUsPostalCode;

    // hs_non_us_province varchar(50),
    private String hsNonUsProvince;

    // hs_not_listed boolean,
    private Boolean hsNotListed;

    // hs_start_date date,
    private LocalDate hsStartDate;

    // hs_state char(2),
    private String hsState;

    // hs_street_1 varchar(50),
    private String hsStreet1;

    // hs_street_2 varchar(50),
    private String hsStreet2;

    // hs_type char(1),
    private String hsType;

    // hs_zip_code varchar(20),
    private String hsZipCode;

    // i20_expiration_date date,
    private LocalDate i20ExpirationDate;

    // i20_issuing_school_name varchar(20),
    private String i20IssuingSchoolName;

    // i94_admission_number varchar(32),
    private String i94AdmissionNumber;

    // i94_expiration_date date,
    private LocalDate i94ExpirationDate;

    // intended_4_year_major varchar(255),
    private String intended4YearMajor;

    // ip_address varchar(15),
    private String ipAddress;

    //ipAddressAtAccountCreation varying (15)
    private String ipAddressAtAccountCreation;

    //ipAddressAtAppCreation varying (15)
    private String ipAddressAtAppCreation;

    // issue_date date,
    private LocalDate issueDate;

    // last_page varchar(30),
    private String lastPage;

    // lastname varchar(50),
    private String lastname;

    // main_phone_auth_txt boolean,
    private Boolean mainPhoneAuthTxt;

    // main_phone_ext varchar(4),
    private String mainPhoneExt;

    // main_phone_number varchar(25),
    private String mainPhoneNumber;

    //phoneType enumerated Mobile/Landline varying (9)
    private String phoneType;

    //mainphone_verified
    private Boolean mainphoneVerified;

    //mainphone_verified_timestamp
    private LocalDateTime mainphoneVerifiedTimestamp;

    // major_code varchar(30),
    private String majorCode;

    // major_description varchar(100),
    private String majorDescription;

    // middlename varchar(50),
    private String middlename;

    // no_i94_expiration_date boolean,
    private Boolean noI94ExpirationDate;

    // no_visa boolean,
    private Boolean noVisa;

    // non_us_address boolean,
    private Boolean nonUsAddress;

    // non_us_permanent_home_address_same_as_permanent boolean,
    private Boolean nonUsPermanentHomeAddressSameAsPermanent;

    // non_us_permanent_home_address_verified boolean,
    private Boolean nonUsPermanentHomeAddressVerified;

    // non_us_permanent_home_city varchar(50),
    private String nonUsPermanentHomeCity;

    // non_us_permanent_home_country char(2),
    private String nonUsPermanentHomeCountry;

    // non_us_permanent_home_non_us_address boolean,
    private Boolean nonUsPermanentHomeNonUsAddress;

    // non_us_permanent_home_non_us_postal_code varchar(30),
    private String nonUsPermanentHomeNonUsPostalCode;

    // non_us_permanent_home_non_us_province varchar(50),
    private String nonUsPermanentHomeNonUsProvince;

    // non_us_permanent_home_street_1 varchar(50),
    private String nonUsPermanentHomeStreet1;

    // non_us_permanent_home_street_2 varchar(50),
    private String nonUsPermanentHomeStreet2;

    // non_us_phone_auth_txt boolean,
    private Boolean nonUsPhoneAuthTxt;

    // non_us_phone_ext varchar(4),
    private String nonUsPhoneExt;

    // non_us_phone_number varchar(25),
    private String nonUsPhoneNumber;

    // non_us_postal_code varchar(30),
    private String nonUsPostalCode;

    // non_us_province varchar(50),
    private String nonUsProvince;

    // number_of_dependents smallint,
    private Integer numberOfDependents;

    // number_of_practical_training smallint,
    private Integer numberOfPracticalTraining;

    // otherfirstname varchar(50),
    private String otherfirstname;

    // otherlastname varchar(50),
    private String otherlastname;

    // othermiddlename varchar(50),
    private String othermiddlename;

    // parent_guardian_address_verified boolean,
    private Boolean parentGuardianAddressVerified;

    // parent_guardian_city varchar(50),
    private String parentGuardianCity;

    // parent_guardian_country char(2),
    private String parentGuardianCountry;

    // parent_guardian_email varchar(50),
    private String parentGuardianEmail;

    // parent_guardian_first_name varchar(25),
    private String parentGuardianFirstName;

    // parent_guardian_last_name varchar(25),
    private String parentGuardianLastName;

    // parent_guardian_no_first_name boolean,
    private Boolean parentGuardianNoFirstName;

    // parent_guardian_non_us_address boolean,
    private Boolean parentGuardianNonUsAddress;

    // parent_guardian_non_us_postal_code varchar(30),
    private String parentGuardianNonUsPostalCode;

    // parent_guardian_non_us_province varchar(50),
    private String parentGuardianNonUsProvince;

    // parent_guardian_phone_auth_txt boolean,
    private Boolean parentGuardianPhoneAuthTxt;

    // parent_guardian_phone_ext varchar(4),
    private String parentGuardianPhoneExt;

    // parent_guardian_phone_number varchar(25),
    private String parentGuardianPhoneNumber;

    // parent_guardian_relationship char(1),
    private String parentGuardianRelationship;

    // parent_guardian_state char(2),
    private String parentGuardianState;

    // parent_guardian_street_1 varchar(50),
    private String parentGuardianStreet1;

    // parent_guardian_street_2 varchar(50),
    private String parentGuardianStreet2;

    // parent_guardian_zip_code varchar(20),
    private String parentGuardianZipCode;

    // passport_country_of_issuance char(2),
    private String passportCountryOfIssuance;

    // passport_expiration_date date,
    private LocalDate passportExpirationDate;

    // passport_not_yet boolean,
    private Boolean passportNotYet;

    // passport_number varchar(20),
    private String passportNumber;

    // perm_addr_address_verified boolean,
    private Boolean permAddrAddressVerified;

    // perm_addr_city varchar(50),
    private String permAddrCity;

    // perm_addr_country char(2),
    private String permAddrCountry;

    // perm_addr_non_us_address boolean,
    private Boolean permAddrNonUsAddress;

    // perm_addr_non_us_postal_code varchar(30),
    private String permAddrNonUsPostalCode;

    // perm_addr_non_us_province varchar(50),
    private String permAddrNonUsProvince;

    // perm_addr_state char(2),
    private String permAddrState;

    // perm_addr_street_1 varchar(50),
    private String permAddrStreet1;

    // perm_addr_street_2 varchar(50),
    private String permAddrStreet2;

    // perm_addr_zip_code varchar(20),
    private String permAddrZipCode;

    // phone_auth_txt boolean,
    private Boolean phoneAuthTxt;

    // phone_ext varchar(4),
    private String phoneExt;

    // phone_number varchar(25),
    private String phoneNumber;

    // preferred_firstname varchar(50),
    private String preferredFirstname;

    // preferred_lastname varchar(50),
    private String preferredLastname;

    // preferred_middlename varchar(50),
    private String preferredMiddlename;

    // preferred_name boolean,
    private Boolean preferredName;

    // presently_studying_in_us boolean,
    private Boolean presentlyStudyingInUs;

    // primary_language varchar(10),
    private String primaryLanguage;

    // pt1_authorizing_school varchar(32),
    private String pt1AuthorizingSchool;

    // pt1_end_date date,
    private LocalDate pt1EndDate;

    // pt1_practical_training_id bigint,
    private Long pt1PracticalTrainingId;

    // pt1_start_date date,
    private LocalDate pt1StartDate;

    // pt1_type varchar(3),
    private String pt1Type;

    // pt2_authorizing_school varchar(32),
    private String pt2AuthorizingSchool;

    // pt2_end_date date,
    private LocalDate pt2EndDate;

    // pt2_practical_training_id bigint,
    private Long pt2PracticalTrainingId;

    // pt2_start_date date,
    private LocalDate pt2StartDate;

    // pt2_type varchar(3),
    private String pt2Type;

    // pt3_authorizing_school varchar(32),
    private String pt3AuthorizingSchool;

    // pt3_end_date date,
    private LocalDate pt3EndDate;

    // pt3_practical_training_id bigint,
    private Long pt3PracticalTrainingId;

    // pt3_start_date date,
    private LocalDate pt3StartDate;

    // pt3_type varchar(3),
    private String pt3Type;

    // pt4_authorizing_school varchar(32),
    private String pt4AuthorizingSchool;

    // pt4_end_date date,
    private LocalDate pt4EndDate;

    // pt4_practical_training_id bigint,
    private Long pt4PracticalTrainingId;

    // pt4_start_date date,
    private LocalDate pt4StartDate;

    // pt4_type varchar(3),
    private String pt4Type;

    // race_ethnicity text,
    private String raceEthnicity;

    // race_ethnic_full text(805),
    private String raceEthnicFull;

    // race_group text,
    private String raceGroup;

    // salt text,
    private String salt;

    // second_phone_auth_txt boolean,
    private Boolean secondPhoneAuthTxt;

    // second_phone_ext varchar(4),
    private String secondPhoneExt;

    // second_phone_number varchar(25),
    private String secondPhoneNumber;

    // sevis_id_number varchar(11),
    private String sevisIdNumber;

    // signature text,
    private String signature;

    // ssn text,
    private String ssn;

    // ssn_exception boolean,
    private Boolean ssnException;

    // ssn_last4 char(4),
    private String ssnLast4;

    // ssn_type char(1),
    private String ssnType;

    // state varchar(2),
    private String state;

    // status char,
    private String status;

    // street_1 varchar(50),
    private String street1;

    // street_2 varchar(50),
    private String street2;

    // suffix varchar(3),
    private String suffix;

    // sup_page_code varchar(30),
    private String supPageCode;

    // term_end_date date,
    private LocalDate termEndDate;

    // term_start_date date,
    private LocalDate termStartDate;

    // tstmp_create timestamp with time zone(()),
    private LocalDateTime tstmpCreate;

    // tstmp_download timestamp with time zone,
    private LocalDateTime tstmpDownload;

    // tstmp_submit timestamp with time zone,
    private LocalDateTime tstmpSubmit;

    // tstmp_update timestamp with time zone,
    private LocalDateTime tstmpUpdate;

    // visa_type varchar(4),
    private String visaType;

    // zip_code varchar(20),
    private String zipCode;

    //sisprocessedflag character varying(1)
    private String sisProcessedFlag;

    //tstmpsisprocessed timestamp with time zone
    private LocalDateTime tstmpSISProcessed;

    //sisprocessednotes character varying(256)
    private String sisProcessedNotes;

    // cip_code varchar(6),
    private String cipCode;

    //col1_cds_full varchar(30),
    private String col1CdsFull;

    //col2_cds_full varchar(30),
    private String col2CdsFull;

    //col3_cds_full varchar(30),
    private String col3CdsFull;

    //col4_cds_full varchar(30),
    private String col4CdsFull;

    // hs_cds_full varchar(30),
    private String hsCdsFull;

    // major_category varchar(100),
    private String majorCategory;

    //no_mailing_address_homeless boolean,
    private Boolean noMailingAddressHomeless;

    //no_non_usa_perm_address_homeless boolean,
    private Boolean noNonUsaPermAddressHomeless;

    //no_perm_address_homeless boolean,
    private Boolean noPermAddressHomeless;

    //ssn_no boolean,
    private Boolean ssnNo;

    //residesInUs boolean,
    private Boolean residesInUs;

    //idme_confirmation_timestamp date
    private LocalDateTime idmeConfirmationTimestamp;
    
    //idme_optin_timestamp date
    private LocalDateTime idmeOptinTimestamp;

    //idme_workflow_status character varying(50) values (expired/verified/staff_verified)
    private String idmeWorkflowStatus;

    private SupplementalQuestions supplementalQuestions;

}
