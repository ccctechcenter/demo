package com.ccctc.adaptor.model.apply;

import com.ccctc.adaptor.model.PrimaryKey;
import com.ccctc.adaptor.util.BooleanDeserializer;
import com.fasterxml.jackson.databind.annotation.JsonDeserialize;
import lombok.Getter;
import lombok.Setter;

import java.io.Serializable;
import java.time.LocalDate;
import java.time.LocalDateTime;

/**
 * California College Promise Grant data
 *
 * Note: BooleanDeserializer is used for values coming in from CCPD that may use a non-standard boolean value, ie "y" or "n"
 * instead of true/false, based on the data model of their database.
 */
@Getter
@Setter
@PrimaryKey({"collegeId", "appId"})
public class CCPromiseGrant implements Serializable {

    //
    // NOTE: collegeId in this model is what we call "misCode", the 3-digit college code
    //


    //app_id bigint NOT NULL DEFAULT nextval('bog_application_app_id_seq'::regclass)
    private Long appId;

    //accepted_terms Boolean
    private Boolean acceptedTerms;

    //accepted_terms_timestamp TIMESTAMP
    private LocalDateTime acceptedTermsTimestamp;

    //ccc_id character varying(8) NOT NULL
    private String cccId;

    //confirmation_number character varying(25)
    private String confirmationNumber;

    //status character(1)
    private String status;

    //app_lang character(2)
    private String appLang;

    //college_id character(3)
    private String collegeId;

    //year_code bigint
    private Long yearCode;

    //year_description character varying(100)
    private String yearDescription;

    //determined_residentca character(1)
    @JsonDeserialize(using = BooleanDeserializer.class)
    private Boolean determinedResidentca;

    //determined_ab540_eligible character(1)
    @JsonDeserialize(using = BooleanDeserializer.class)
    private Boolean determinedAB540Eligible;

    //determined_non_res_exempt character(1)
    @JsonDeserialize(using = BooleanDeserializer.class)
    private Boolean determinedNonResExempt;

    //lastname character varying(50)
    private String lastname;

    //firstname character varying(50)
    private String firstname;

    //middlename character varying(50)
    private String middlename;

    //mainphone character varying(14)
    private String mainphone;

    //mainphone_ext character varying(4)
    private String mainphoneExt;

    //mainphone_auth_text boolean
    private Boolean mainphoneAuthText;

    //mainphone_verified
    private Boolean mainphoneVerified;

    //mainphone_verified_timestamp
    private LocalDateTime mainphoneVerifiedTimestamp;

    //phoneType enumerated Mobile/Landline varying (9)
    private String phoneType;

    //email character varying(128)
    private String email;

    //email_verified
    private Boolean emailVerified;

    //email_verified_timestamp
    private LocalDateTime emailVerifiedTimestamp;

    //preferred_method_of_contact character varying(30)
    private String preferredMethodOfContact;

    //non_us_address boolean
    private Boolean nonUsAddress;

    //address_validation_override
    private Boolean addressValidationOverride;

    //address_validation_override_timestamp
    private LocalDateTime addressValidationOverrideTimestamp;

    //mailing_address_validation_override
    private Boolean mailingAddressValidationOverride;

    //streetaddress1 character varying(50)
    private String streetaddress1;

    //streetaddress2 character varying(50)
    private String streetaddress2;

    //city character varying(50)
    private String city;

    //state character varying(2)
    private String state;

    //province character varying(30)
    private String province;

    //country character varying(2)
    private String country;

    //postalcode character varying(20)
    private String postalcode;

    //ssn text - will come in unencrypted in xxx-xx-xxxx format
    private String ssn;

    //ssn_type character(1)
    private String ssnType;

    //student_college_id character varying(20)
    private String studentCollegeId;

    //birthdate date
    private LocalDate birthdate;

    //marital_status character(1)
    private String maritalStatus;

    //reg_dom_partner character(1)
    @JsonDeserialize(using = BooleanDeserializer.class)
    private Boolean regDomPartner;

    //born_before_23_year character(1)
    @JsonDeserialize(using = BooleanDeserializer.class)
    private Boolean bornBefore23Year;

    //married_or_rdp character(1)
    @JsonDeserialize(using = BooleanDeserializer.class)
    private Boolean marriedOrRdp;

    //us_veteran character(1)
    @JsonDeserialize(using = BooleanDeserializer.class)
    private Boolean usVeteran;

    //dependents character(1)
    @JsonDeserialize(using = BooleanDeserializer.class)
    private Boolean dependents;

    //parents_deceased character(1)
    @JsonDeserialize(using = BooleanDeserializer.class)
    private Boolean parentsDeceased;

    //emancipated_minor character(1)
    @JsonDeserialize(using = BooleanDeserializer.class)
    private Boolean emancipatedMinor;

    //legal_guardianship character(1)
    @JsonDeserialize(using = BooleanDeserializer.class)
    private Boolean legalGuardianship;

    //homeless_youth_school character(1)
    @JsonDeserialize(using = BooleanDeserializer.class)
    private Boolean homelessYouthSchool;

    //homeless_youth_hud character(1)
    @JsonDeserialize(using = BooleanDeserializer.class)
    private Boolean homelessYouthHud;

    //homeless_youth_other character(1)
    @JsonDeserialize(using = BooleanDeserializer.class)
    private Boolean homelessYouthOther;

    //dependent_on_parent_taxes character(1) - note, this is not a boolean, it can be 0, 1 or 2
    private String dependentOnParentTaxes;

    //living_with_parents character(1)
    @JsonDeserialize(using = BooleanDeserializer.class)
    private Boolean livingWithParents;

    //dependency_status character(1) - I or D
    private String dependencyStatus;

    //cert_veteran_affairs character(1)
    @JsonDeserialize(using = BooleanDeserializer.class)
    private Boolean certVeteranAffairs;

    //cert_national_guard character(1)
    @JsonDeserialize(using = BooleanDeserializer.class)
    private Boolean certNationalGuard;

    //elig_medal_honor character(1)
    @JsonDeserialize(using = BooleanDeserializer.class)
    private Boolean eligMedalHonor;

    //elig_sept_11 character(1)
    @JsonDeserialize(using = BooleanDeserializer.class)
    private Boolean eligSept11;

    //elig_police_fire character(1)
    @JsonDeserialize(using = BooleanDeserializer.class)
    private Boolean eligPoliceFire;

    //tanf_calworks character(1)
    @JsonDeserialize(using = BooleanDeserializer.class)
    private Boolean tanfCalworks;

    //ssi_ssp character(1)
    @JsonDeserialize(using = BooleanDeserializer.class)
    private Boolean ssiSsp;

    //general_assistance character(1)
    @JsonDeserialize(using = BooleanDeserializer.class)
    private Boolean generalAssistance;

    //parents_assistance character(1)
    @JsonDeserialize(using = BooleanDeserializer.class)
    private Boolean parentsAssistance;

    //dep_number_household integer
    private Integer depNumberHousehold;

    //ind_number_household integer
    private Integer indNumberHousehold;

    //dep_gross_income integer
    private Integer depGrossIncome;

    //ind_gross_income integer
    private Integer indGrossIncome;

    //dep_other_income integer
    private Integer depOtherIncome;

    //ind_other_income integer
    private Integer indOtherIncome;

    //dep_total_income integer
    private Integer depTotalIncome;

    //ind_total_income integer
    private Integer indTotalIncome;

    //elig_method_a boolean
    private Boolean eligMethodA;

    //elig_method_b boolean
    private Boolean eligMethodB;

    //elig_bogfw character(1)
    private String eligBogfw;

    //confirmation_parent_guardian boolean
    private Boolean confirmationParentGuardian;

    //parent_guardian_name character varying(60)
    private String parentGuardianName;

    //ack_fin_aid boolean DEFAULT false
    private Boolean ackFinAid;

    //confirmation_applicant boolean DEFAULT false
    private Boolean confirmationApplicant;

    //last_page character varying(25)
    private String lastPage;

    //ssn_last4 character(4)
    private String ssnLast4;

    //tstmp_submit timestamp with time zone
    private LocalDateTime tstmpSubmit;

    //tstmp_create timestamp with time zone DEFAULT now()
    private LocalDateTime tstmpCreate;

    //tstmp_update timestamp with time zone
    private LocalDateTime tstmpUpdate;

    //tstmp_download timestamp with time zone
    private LocalDateTime tstmpDownload;

    //term_code character varying(5)
    private String termCode;

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

    //ssn_exception boolean DEFAULT false
    private Boolean ssnException;

    //college_name character varying(50)
    private String collegeName;

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

    //no_perm_address_homeless character(1)
    @JsonDeserialize(using = BooleanDeserializer.class)
    private Boolean noPermAddressHomeless;

    //no_mailing_address_homeless character(1)
    @JsonDeserialize(using = BooleanDeserializer.class)
    private Boolean noMailingAddressHomeless;

    //determined_homeless character(1)
    @JsonDeserialize(using = BooleanDeserializer.class)
    private Boolean determinedHomeless;

    //elig_method_d boolean
    private Boolean eligMethodD;

    //mainphoneintl character varying(25)
    private String mainphoneintl;

    //sisprocessedflag character varying(1)
    private String sisProcessedFlag;

    //tstmpsisprocessed timestamp with time zone
    private LocalDateTime tstmpSISProcessed;

    //sisprocessednotes character varying(256)
    private String sisProcessedNotes;

    //elig_exonerated_crime boolean
    @JsonDeserialize(using = BooleanDeserializer.class)
    private Boolean eligExoneratedCrime;

    //elig_covid_death boolean
    @JsonDeserialize(using = BooleanDeserializer.class)
    private Boolean eligCovidDeath;

    //student_parent boolean
    @JsonDeserialize(using = BooleanDeserializer.class)
    private Boolean studentParent;

    //idme_confirmation_timestamp date
    private LocalDateTime idmeConfirmationTimestamp;

    //idme_optin_timestamp date
    private LocalDateTime idmeOptinTimestamp;
    
    //idme_workflow_status character varying(50) values (expired/verified/staff_verified)
    private String idmeWorkflowStatus;

    //student_deps_under18 smallint
    private Integer studentDepsUnder18;

    //student_deps_18over smallint
    private Integer studentDeps18Over;
}
