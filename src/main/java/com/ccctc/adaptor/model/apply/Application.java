package com.ccctc.adaptor.model.apply;

import com.ccctc.adaptor.model.PrimaryKey;
import com.ccctc.adaptor.util.BooleanDeserializer;
import com.fasterxml.jackson.databind.annotation.JsonDeserialize;
import lombok.Getter;
import lombok.Setter;

import java.io.Serializable;
import java.time.LocalDate;
import java.time.LocalDateTime;

@Getter
@Setter
@PrimaryKey({"collegeId", "appId"})
public class Application implements Serializable {

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

    //status character(1)
    private String status;

    //college_id character(3)
    private String collegeId;

    //term_id bigint
    private Long termId;

    //major_id bigint
    private Long majorId;

    //intended_major character varying(30)
    private String intendedMajor;

    //edu_goal character(1)
    private String eduGoal;

    //highest_edu_level character(5)
    private String highestEduLevel;

    //consent_indicator boolean DEFAULT false
    private Boolean consentIndicator;

    //app_lang character(2)
    private String appLang;

    //ack_fin_aid boolean DEFAULT false
    private Boolean ackFinAid;

    //fin_aid_ref boolean
    private Boolean finAidRef;

    //confirmation character varying(30)
    private String confirmation;

    //sup_page_code character varying(30)
    private String supPageCode;

    //last_page character varying(25)
    private String lastPage;

    //streetaddress1 character varying(50)
    private String streetaddress1;

    //streetaddress2 character varying(50)
    private String streetaddress2;

    //city character varying(50)
    private String city;

    //postalcode character varying(20)
    private String postalcode;

    //state character(2)
    private String state;

    //nonusaprovince character varying(30)
    private String nonusaprovince;

    //country character(2)
    private String country;

    //non_us_address boolean
    private Boolean nonUsAddress;

    //email character varying(254)
    private String email;

    //email_verified
    private Boolean emailVerified;

    //email_verified_timestamp
    private LocalDateTime emailVerifiedTimestamp;

    //perm_streetaddress1 character varying(50)
    private String permStreetaddress1;

    //perm_streetaddress2 character varying(50)
    private String permStreetaddress2;

    //perm_city character varying(50)
    private String permCity;

    //perm_postalcode character varying(20)
    private String permPostalcode;

    //perm_state character(2)
    private String permState;

    //perm_nonusaprovince character varying(30)
    private String permNonusaprovince;

    //perm_country character(2)
    private String permCountry;

    //mailing_address_validation_override
    private Boolean mailingAddressValidationOverride;

    //address_same boolean
    private Boolean addressSame;

    //mainphone character varying(19)
    private String mainphone;

    //mainphone_ext character varying(4)
    private String mainphoneExt;

    //mainphone_auth_text boolean
    private Boolean mainphoneAuthText;

    //mainphone_verified
    private Boolean mainphoneVerified;

    //phoneType enumerated Mobile/Landline varying (9)
    private String phoneType;

    //mainphone_verified_timestamp
    private LocalDateTime mainphoneVerifiedTimestamp;

    //secondphone character varying(19)
    private String secondphone;

    //secondphone_ext character varying(4)
    private String secondphoneExt;

    //secondphone_auth_text boolean
    private Boolean secondphoneAuthText;

    //preferred_method_of_contact character varying(30)
    private String preferredMethodOfContact;

    //enroll_status character(1)
    private String enrollStatus;

    //hs_edu_level character(1)
    private String hsEduLevel;

    //hs_comp_date date
    private LocalDate hsCompDate;

    //higher_edu_level character(1)
    private String higherEduLevel;

    //higher_comp_date date
    private LocalDate higherCompDate;

    //hs_not_attended boolean
    private Boolean hsNotAttended;

    //cahs_graduated boolean
    private Boolean cahsGraduated;

    //cahs_3year boolean
    private Boolean cahs3year;

    //hs_name character varying(30)
    private String hsName;

    //hs_city character varying(20)
    private String hsCity;

    //hs_state character(2)
    private String hsState;

    //hs_country character(2)
    private String hsCountry;

    //hs_cds character(6)
    private String hsCds;

    //hs_ceeb character(7)
    private String hsCeeb;

    //hs_not_listed boolean
    private Boolean hsNotListed;

    //home_schooled boolean
    private Boolean homeSchooled;

    //college_count smallint
    private Integer collegeCount;

    //hs_attendance smallint
    private Integer hsAttendance;

    //coenroll_confirm boolean
    private Boolean coenrollConfirm;

    //gender character(1)
    private String gender;

    //pg_firstname character varying(50)
    private String pgFirstname;

    //pg_lastname character varying(50)
    private String pgLastname;

    //pg_rel character(1)
    private String pgRel;

    //pg1_edu character(1)
    private String pg1Edu;

    //pg2_edu character(1)
    private String pg2Edu;

    //pg_edu_mis character(2)
    private String pgEduMis;

    //under19_ind boolean DEFAULT false
    private Boolean under19Ind;

    //dependent_status character(1)
    private String dependentStatus;

    //race_ethnic text
    private String raceEthnic;

    //hispanic boolean
    private Boolean hispanic;

    //race_group text
    private String raceGroup;

    //race_ethnic_full text
    private String raceEthnicFull;

    //ssn text
    private String ssn;

    //birthdate date
    private LocalDate birthdate;

    //firstname character varying(50)
    private String firstname;

    //middlename character varying(50)
    private String middlename;

    //lastname character varying(50)
    private String lastname;

    //suffix character varying(3)
    private String suffix;

    //otherfirstname character varying(50)
    private String otherfirstname;

    //othermiddlename character varying(50)
    private String othermiddlename;

    //otherlastname character varying(50)
    private String otherlastname;

    //citizenship_status character(1) NOT NULL
    private String citizenshipStatus;

    //alien_reg_number character varying(20)
    private String alienRegNumber;

    //visa_type character varying(20)
    private String visaType;

    //no_documents boolean
    private Boolean noDocuments;

    //alien_reg_issue_date date
    private LocalDate alienRegIssueDate;

    //alien_reg_expire_date date
    private LocalDate alienRegExpireDate;

    //alien_reg_no_expire boolean
    private Boolean alienRegNoExpire;

    //military_status character(1) NOT NULL
    private String militaryStatus;

    //military_discharge_date date
    private LocalDate militaryDischargeDate;

    //military_home_state character(2)
    private String militaryHomeState;

    //military_home_country character(2)
    private String militaryHomeCountry;

    //military_ca_stationed boolean
    private Boolean militaryCaStationed;

    //military_legal_residence character(2)
    private String militaryLegalResidence;

    //ca_res_2_years boolean
    private Boolean caRes2Years;

    //ca_date_current date
    private LocalDate caDateCurrent;

    //ca_not_arrived boolean
    private Boolean caNotArrived;

    //ca_college_employee boolean
    private Boolean caCollegeEmployee;

    //ca_school_employee boolean
    private Boolean caSchoolEmployee;

    //ca_seasonal_ag boolean
    private Boolean caSeasonalAg;

    //ca_foster_youth boolean
    private Boolean caFosterYouth;

    //ca_outside_tax boolean
    private Boolean caOutsideTax;

    //ca_outside_tax_year date
    private LocalDate caOutsideTaxYear;

    //ca_outside_voted boolean
    private Boolean caOutsideVoted;

    //ca_outside_voted_year date
    private LocalDate caOutsideVotedYear;

    //ca_outside_college boolean
    private Boolean caOutsideCollege;

    //ca_outside_college_year date
    private LocalDate caOutsideCollegeYear;

    //ca_outside_lawsuit boolean
    private Boolean caOutsideLawsuit;

    //ca_outside_lawsuit_year date
    private LocalDate caOutsideLawsuitYear;

    //res_status character(1)
    private String resStatus;

    //res_status_change boolean
    private Boolean resStatusChange;

    //res_prev_date date
    private LocalDate resPrevDate;

    //adm_ineligible smallint
    private Integer admIneligible;

    //elig_ab540 boolean
    private Boolean eligAb540;

    //res_area_a smallint
    private Integer resAreaA;

    //res_area_b smallint
    private Integer resAreaB;

    //res_area_c smallint
    private Integer resAreaC;

    //res_area_d smallint
    private Integer resAreaD;

    //experience integer
    private Integer experience;

    //recommend integer
    private Integer recommend;

    //comments text
    private String comments;

    //comfortable_english boolean
    private Boolean comfortableEnglish;

    //financial_assistance boolean
    private Boolean financialAssistance;

    //tanf_ssi_ga boolean
    private Boolean tanfSsiGa;

    //foster_youths boolean
    private Boolean fosterYouths;

    //athletic_intercollegiate boolean
    private Boolean athleticIntercollegiate;

    //athletic_intramural boolean
    private Boolean athleticIntramural;

    //athletic_not_interested boolean
    private Boolean athleticNotInterested;

    //academic_counseling boolean
    private Boolean academicCounseling;

    //basic_skills boolean
    private Boolean basicSkills;

    //calworks boolean
    private Boolean calworks;

    //career_planning boolean
    private Boolean careerPlanning;

    //child_care boolean
    private Boolean childCare;

    //counseling_personal boolean
    private Boolean counselingPersonal;

    //dsps boolean
    private Boolean dsps;

    //eops boolean
    private Boolean eops;

    //esl boolean
    private Boolean esl;

    //health_services boolean
    private Boolean healthServices;

    //housing_info boolean
    private Boolean housingInfo;

    //employment_assistance boolean
    private Boolean employmentAssistance;

    //online_classes boolean
    private Boolean onlineClasses;

    //reentry_program boolean
    private Boolean reentryProgram;

    //scholarship_info boolean
    private Boolean scholarshipInfo;

    //student_government boolean
    private Boolean studentGovernment;

    //testing_assessment boolean
    private Boolean testingAssessment;

    //transfer_info boolean
    private Boolean transferInfo;

    //tutoring_services boolean
    private Boolean tutoringServices;

    //veterans_services boolean
    private Boolean veteransServices;

    //integrity_fg_01 boolean
    private Boolean integrityFg01;

    //integrity_fg_02 boolean
    private Boolean integrityFg02;

    //integrity_fg_03 boolean
    private Boolean integrityFg03;

    //integrity_fg_04 boolean
    private Boolean integrityFg04;

    //integrity_fg_11 boolean
    private Boolean integrityFg11;

    //integrity_fg_47 boolean
    private Boolean integrityFg47;

    //integrity_fg_48 boolean
    private Boolean integrityFg48;

    //integrity_fg_49 boolean
    private Boolean integrityFg49;

    //integrity_fg_50 boolean
    private Boolean integrityFg50;

    //integrity_fg_51 boolean
    private Boolean integrityFg51;

    //integrity_fg_52 boolean
    private Boolean integrityFg52;

    //integrity_fg_53 boolean
    private Boolean integrityFg53;

    //integrity_fg_54 boolean
    private Boolean integrityFg54;

    //integrity_fg_55 boolean
    private Boolean integrityFg55;

    //integrity_fg_56 boolean
    private Boolean integrityFg56;

    //integrity_fg_57 boolean
    private Boolean integrityFg57;

    //integrity_fg_58 boolean
    private Boolean integrityFg58;

    //integrity_fg_59 boolean
    private Boolean integrityFg59;

    //integrity_fg_60 boolean
    private Boolean integrityFg60;

    //integrity_fg_61 boolean
    private Boolean integrityFg61;

    //integrity_fg_62 boolean
    private Boolean integrityFg62;

    //integrity_fg_63 boolean
    private Boolean integrityFg63;

    //integrity_fg_70 boolean
    private Boolean integrityFg70;

    //integrity_fg_80 boolean
    private Boolean integrityFg80;

    //col1_ceeb character(7)
    private String col1Ceeb;

    //col1_cds character(6)
    private String col1Cds;

    //col1_not_listed boolean
    private Boolean col1NotListed;

    //col1_name character varying(30)
    private String col1Name;

    //col1_city character varying(20)
    private String col1City;

    //col1_state character varying(30)
    private String col1State;

    //col1_country character(2)
    private String col1Country;

    //col1_start_date date
    private LocalDate col1StartDate;

    //col1_end_date date
    private LocalDate col1EndDate;

    //col1_degree_date date
    private LocalDate col1DegreeDate;

    //col1_degree_obtained character(1)
    private String col1DegreeObtained;

    //col2_ceeb character(7)
    private String col2Ceeb;

    //col2_cds character(6)
    private String col2Cds;

    //col2_not_listed boolean
    private Boolean col2NotListed;

    //col2_name character varying(30)
    private String col2Name;

    //col2_city character varying(20)
    private String col2City;

    //col2_state character varying(30)
    private String col2State;

    //col2_country character(2)
    private String col2Country;

    //col2_start_date date
    private LocalDate col2StartDate;

    //col2_end_date date
    private LocalDate col2EndDate;

    //col2_degree_date date
    private LocalDate col2DegreeDate;

    //col2_degree_obtained character(1)
    private String col2DegreeObtained;

    //col3_ceeb character(7)
    private String col3Ceeb;

    //col3_cds character(6)
    private String col3Cds;

    //col3_not_listed boolean
    private Boolean col3NotListed;

    //col3_name character varying(30)
    private String col3Name;

    //col3_city character varying(20)
    private String col3City;

    //col3_state character varying(30)
    private String col3State;

    //col3_country character(2)
    private String col3Country;

    //col3_start_date date
    private LocalDate col3StartDate;

    //col3_end_date date
    private LocalDate col3EndDate;

    //col3_degree_date date
    private LocalDate col3DegreeDate;

    //col3_degree_obtained character(1)
    private String col3DegreeObtained;

    //col4_ceeb character(7)
    private String col4Ceeb;

    //col4_cds character(6)
    private String col4Cds;

    //col4_not_listed boolean
    private Boolean col4NotListed;

    //col4_name character varying(30)
    private String col4Name;

    //col4_city character varying(20)
    private String col4City;

    //col4_state character varying(30)
    private String col4State;

    //col4_country character(2)
    private String col4Country;

    //col4_start_date date
    private LocalDate col4StartDate;

    //col4_end_date date
    private LocalDate col4EndDate;

    //col4_degree_date date
    private LocalDate col4DegreeDate;

    //col4_degree_obtained character(1)
    private String col4DegreeObtained;

    //college_name character varying(50)
    private String collegeName;

    //district_name character varying(50)
    private String districtName;

    //term_code character varying(15)
    private String termCode;

    //term_description character varying(100)
    private String termDescription;

    //major_code character varying(30)
    private String majorCode;

    //major_description character varying(100)
    private String majorDescription;

    //tstmp_submit timestamp with time zone
    private LocalDateTime tstmpSubmit;

    //tstmp_create timestamp with time zone DEFAULT now()
    private LocalDateTime tstmpCreate;

    //tstmp_update timestamp with time zone
    private LocalDateTime tstmpUpdate;

    //ssn_display character varying(11)
    private String ssnDisplay;

    //foster_youth_status character(1)
    private String fosterYouthStatus;

    //foster_youth_preference boolean
    private Boolean fosterYouthPreference;

    //foster_youth_mis boolean
    private Boolean fosterYouthMis;

    //foster_youth_priority boolean
    private Boolean fosterYouthPriority;

    //tstmp_download timestamp with time zone
    private LocalDateTime tstmpDownload;

    //address_validation character(1)
    private String addressValidation;

    //address_validation_override
    private Boolean addressValidationOverride;

    //address_validation_override_timestamp
    private LocalDateTime addressValidationOverrideTimestamp;

    //zip4 character(4)
    private String zip4;

    //perm_address_validation character(1)
    private String permAddressValidation;

    //perm_zip4 character(4)
    private String permZip4;

    //discharge_type character varying(1)
    private String dischargeType;

    //college_expelled_summary boolean
    private Boolean collegeExpelledSummary;

    //col1_expelled_status boolean
    private Boolean col1ExpelledStatus;

    //col2_expelled_status boolean
    private Boolean col2ExpelledStatus;

    //col3_expelled_status boolean
    private Boolean col3ExpelledStatus;

    //col4_expelled_status boolean
    private Boolean col4ExpelledStatus;

    //integrity_flags character varying(255)
    private String integrityFlags;

    //rdd date
    private LocalDate rdd;

    //ssn_type character(1)
    private String ssnType;

    //military_stationed_ca_ed boolean
    private Boolean militaryStationedCaEd;

    //integrity_fg_65 boolean
    private Boolean integrityFg65;

    //integrity_fg_64 boolean
    private Boolean integrityFg64;

    //ip_address character varying(15)
    private String ipAddress;

    //ipAddressAtAccountCreation varying (15)
    private String ipAddressAtAccountCreation;

    //ipAddressAtAppCreation varying (15)
    private String ipAddressAtAppCreation;

    //campaign1 character varying(255)
    private String campaign1;

    //campaign2 character varying(255)
    private String campaign2;

    //campaign3 character varying(255)
    private String campaign3;

    // unencrypted version of orientation_encrypted as a String
    private String orientation;

    // unencrypted version of transgender_encrypted as a String
    private String transgender;

    //ssn_exception boolean DEFAULT false
    private Boolean ssnException;

    //integrity_fg_71 boolean
    private Boolean integrityFg71;

    //preferred_firstname character varying(50)
    private String preferredFirstname;

    //preferred_middlename character varying(50)
    private String preferredMiddlename;

    //preferred_lastname character varying(50)
    private String preferredLastname;

    //preferred_name boolean DEFAULT false
    private Boolean preferredName;

    //ssn_no boolean
    private Boolean ssnNo;

    //completed_eleventh_grade boolean
    private Boolean completedEleventhGrade;

    //grade_point_average character varying(5)
    private String gradePointAverage;

    //highest_english_course integer
    private Integer highestEnglishCourse;

    //highest_english_grade character varying(2)
    private String highestEnglishGrade;

    //highest_math_course_taken integer
    private Integer highestMathCourseTaken;

    //highest_math_taken_grade character varying(2)
    private String highestMathTakenGrade;

    //highest_math_course_passed integer
    private Integer highestMathCoursePassed;

    //highest_math_passed_grade character varying(2)
    private String highestMathPassedGrade;

    //integrity_fg_30 boolean
    private Boolean integrityFg30;

    //hs_cds_full character varying(14)
    private String hsCdsFull;

    //col1_cds_full character varying(14)
    private String col1CdsFull;

    //col2_cds_full character varying(14)
    private String col2CdsFull;

    //col3_cds_full character varying(14)
    private String col3CdsFull;

    //col4_cds_full character varying(14)
    private String col4CdsFull;

    //ssid character varying(10)
    private String ssid;

    //no_perm_address_homeless boolean DEFAULT false
    private Boolean noPermAddressHomeless;

    //no_mailing_address_homeless boolean DEFAULT false
    private Boolean noMailingAddressHomeless;

    //term_start date
    private LocalDate termStart;

    //term_end date
    private LocalDate termEnd;

    //homeless_youth boolean
    private Boolean homelessYouth;

    //integrity_fg_40 boolean
    private Boolean integrityFg40;

    //cip_code character(6)
    private String cipCode;

    //major_category character varying(100)
    private String majorCategory;

    //mainphoneintl character varying(25)
    private String mainphoneintl;

    //secondphoneintl character varying(25)
    private String secondphoneintl;

    //non_credit boolean
    private Boolean nonCredit;

    //integrity_fg_81 boolean
    private Boolean integrityFg81;

    //highest_grade_completed character varying(2)
    private String highestGradeCompleted;

    //fraud score
    private Double fraudScore;

    //fraud score status
    private Integer fraudStatus;

    //student_parent boolean
    @JsonDeserialize(using = BooleanDeserializer.class)
    private Boolean studentParent;

    //idme_confirmation_timestamp date
    private LocalDateTime idmeConfirmationTimestamp;

    //idme_optin_timestamp date
    private LocalDateTime idmeOptinTimestamp;

    //idme_workflow_status character varying(50) (expired/verified/staff_verified)
    private String idmeWorkflowStatus;

    //race_aian_other_description varying(1024)
    private String raceAIANOtherDescription;

    //student_deps_under18 smallint
    private Integer studentDepsUnder18;

    //student_deps_18over smallint
    private Integer studentDeps18Over;

    //sisprocessedflag character varying(1)
    private String sisProcessedFlag;

    //tstmpsisprocessed timestamp with time zone
    private LocalDateTime tstmpSISProcessed;

    //sisprocessednotes character varying(256)
    private String sisProcessedNotes;
    private SupplementalQuestions supplementalQuestions;

}
