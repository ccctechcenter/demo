
package api.peoplesoft

import com.ccctc.adaptor.util.MisEnvironment
import org.apache.commons.lang.StringUtils
import org.slf4j.Logger
import org.slf4j.LoggerFactory

import com.ccctc.adaptor.exception.EntityConflictException
import com.ccctc.adaptor.exception.EntityNotFoundException
import com.ccctc.adaptor.exception.InternalServerException
import com.ccctc.adaptor.exception.InvalidRequestException
import org.springframework.core.env.Environment
import com.ccctc.adaptor.model.apply.Application
import com.ccctc.adaptor.model.apply.SupplementalQuestions

import java.time.LocalDate
import java.time.LocalDateTime
import java.time.format.DateTimeFormatter

/**
 * <h1>CCCApply Standard Application Peoplesoft Interface</h1>
 * <summary>
 *     <p>Provides a Wrapper around the native peoplesoft java API for:</p>
 *     <ol>
 *         <li>Storing and populating Standard Applications from CCCApply into two custom peoplesoft staging tables</li>
 *         <li>Retrieving Standard Applications from the two custom peoplesoft staging tables</li>
 *     <ol>
 *     <p>Most logic for College Adaptor's Peoplesoft support is found within each Peoplesoft instance itself as
 *        this renders the highest level of transparency to each college for code-reviews and deployment authorization.
 *        See App Designer/PeopleTools projects latest API info
 *     </p>
 * </summary>
 *
 * @version CAv4.10.0
 *
 */
class Apply {

    protected final static Logger log = LoggerFactory.getLogger(Apply.class)

    //****** Populated by injection in Groovy Service Implementation ****
    Environment environment

    /**
     * Default Constructor needed since we also specify an alternative Constructor
     */
    Apply(){
        // do nothing, except be available
    }

    /**
     * Alternative Constructor to set the environment during initialization.
     * Useful for when other groovy PS APIs need to use these Functions that depend on environment
     */
    Apply(Environment e) {
        this.environment = e
    }

    /**
     * Gets Standard Application Data from a Peoplesoft staging table using the CCTC_StandardApplication:getApplication
     * peoplesoft method as well as the CCTC_StandardApplication:getApplicationSupplementalQuestions method
     * @param misCode The College Code used for grabbing college specific settings from the environment
     * @param appId The application Id used when originally populating the data into the staging table
     * @returns A matching Standard Application
     */
    Application get(String misCode, Long appId) {
        log.debug("get: getting standard application data")

        //****** Validate parameters ****
        if (!appId) {
            throw new InvalidRequestException(InvalidRequestException.Errors.invalidRequest, "Application Id cannot be null or blank")
        }
        if (!misCode) {
            throw new InvalidRequestException(InvalidRequestException.Errors.invalidSearchCriteria, "misCode cannot be null or blank")
        }

        log.debug("get: params ok; building method params")

        //****** Build parameters to send to our custom Peopletools API ****
        String packageName = "CCTC_APPLY_PKG"
        String className = "CCTC_StandardApplication"
        String methodName = "getApplication"

        String[] args = [appId]

        Application application = new Application()

        log.debug("get: attempting to get a new peoplesoft Session")
        //****** Create Connection to Peoplesoft and call remote method ******
        def peoplesoftSession = api.peoplesoft.PSConnection.getSession(environment, misCode)
        try {
            log.debug("get: calling the remote peoplesoft method")

            String[] appData = api.peoplesoft.PSConnection.callMethod(peoplesoftSession, packageName, className, methodName, "<", args)
            if(appData == null || appData.length <= 0) {
                throw new EntityNotFoundException("No Standard Application found for given appId")
            }

            application = this.ConvertStringArrayToApplication(appData)
            if(application != null) {
                log.debug("get: got application. now attempting to get supplemental data")
                methodName = "getSupplementalQuestions"

                String[] suppData = api.peoplesoft.PSConnection.callMethod(peoplesoftSession, packageName, className, methodName, "<", args)
                if (suppData != null && suppData.length > 0) {
                    application.supplementalQuestions = this.ConvertStringArrayToSupplementalQuestion(suppData)
                }
            }
        }
        catch(psft.pt8.joa.JOAException e) {
            String messageToThrow = e.getMessage()
            if(peoplesoftSession != null) {
                String[] msgs = api.peoplesoft.PSConnection.getErrorMessages(peoplesoftSession)
                if(msgs && msgs.length > 0) {
                    messageToThrow = msgs[0]
                    msgs.each {
                        log.error("get: retrieval failed: [" + it + "]")
                    }
                }
            }
            throw new InternalServerException(InternalServerException.Errors.internalServerError, messageToThrow)
        }
        finally {
            log.debug("get: disconnecting")
            if(peoplesoftSession != null) {
                peoplesoftSession.disconnect()
            }
        }

        log.debug("get: done")
        return application
    }

    /**
     * Sets Standard Application Data into a Peoplesoft staging table using the CCTC_StandardApplication:setApplication
     * peoplesoft method as well as the CCTC_StandardApplication:setApplicationSupplementalQuestions method
     * @param misCode The College Code used for grabbing college specific settings from the environment
     * @param application The application data to set into the staging table
     * @returns if successful, the newly inserted Record
     */
    Application post(String misCode, Application application) {

        log.debug("post: setting standard application data")

        //****** Validate parameters ****
        if (application == null) {
            throw new InvalidRequestException(InvalidRequestException.Errors.invalidRequest, "Standard Application data cannot be null")
        }
        if (!application.appId) {
            throw new InvalidRequestException(InvalidRequestException.Errors.invalidRequest, "Application Id cannot be null or blank")
        }
        if (!misCode) {
            throw new InvalidRequestException(InvalidRequestException.Errors.invalidSearchCriteria, "misCode cannot be null or blank")
        }

        log.debug("post: params ok; building method params")
        //****** Build parameters to send to our custom Peopletools API ****
        String packageName = "CCTC_APPLY_PKG"
        String className = "CCTC_StandardApplication"
        String methodName = "getApplication"

        String[] args = [application.appId]

        log.debug("post: attempting to get a new peoplesoft Session")
        //****** Create Connection to Peoplesoft and call remote method ******
        def peoplesoftSession = api.peoplesoft.PSConnection.getSession(environment, misCode)
        try {
            log.debug("post: calling the remote peoplesoft method to check if already exists")
            String[] applData = api.peoplesoft.PSConnection.callMethod(peoplesoftSession, packageName, className, methodName, "<", args)
            if(applData.length > 0) {
                throw new EntityConflictException("Application ID already exists")
            }

            packageName = "CCTC_APPLY_PKG"
            className = "CCTC_StandardApplication"
            methodName = "setApplication"

            args = this.ConvertApplicationToArgsArray(application)

            log.debug("post: calling the remote peoplesoft method to set the application")
            String[] rowsAffected = api.peoplesoft.PSConnection.callMethod(peoplesoftSession, packageName, className, methodName, "<", args)
            if(rowsAffected.length != 1) {
                log.error("post: invalid number of rows affected on reg app. empty array")
                throw new InternalServerException(InternalServerException.Errors.sisQueryError, "invalid number of rows affected. empty array")
            }
            if(!StringUtils.equalsIgnoreCase(rowsAffected[0], "1")) {
                log.error("post: invalid number of rows affected on reg app [" + rowsAffected[0] + "]")
                throw new InternalServerException(InternalServerException.Errors.sisQueryError, "invalid number of rows affected [" + rowsAffected[0] + "]")
            }

            if(application.supplementalQuestions != null){

                methodName = "setSupplementalQuestions"
                args = this.ConvertSupplementalQuestionToArgsArray(application.supplementalQuestions)

                log.debug("post: calling the remote peoplesoft method to set the supplemental questions")
                String[] sq_rowsAffected = api.peoplesoft.PSConnection.callMethod(peoplesoftSession, packageName, className, methodName, "<", args)
                if(sq_rowsAffected.length != 1) {
                    log.error("post: invalid number of rows affected on supp ques. empty array")
                    throw new InternalServerException(InternalServerException.Errors.sisQueryError, "invalid number of rows affected. empty array")
                }
                if(!StringUtils.equalsIgnoreCase(sq_rowsAffected[0], "1")) {
                    log.error("post: invalid number of rows affected on supp ques [" + sq_rowsAffected[0] + "]")
                    throw new InternalServerException(InternalServerException.Errors.sisQueryError, "invalid number of rows affected [" + sq_rowsAffected[0] + "]")
                }
            }
            String dataEventsEnabled = MisEnvironment.getProperty(environment, misCode, "peoplesoft.dataEvents.enabled")
            if(StringUtils.equalsIgnoreCase(dataEventsEnabled,"true")) {
                try {
                    log.debug("post: triggering the remote peoplesoft event onStandardAppInserted")

                    args = [application.appId]
                    api.peoplesoft.PSConnection.triggerEvent(peoplesoftSession, "onStandardAppInserted", "<", args)
                }
                catch (Exception exc) {
                    log.warn("post: Triggering the onStandardAppInserted event failed with message [" + exc.getMessage() + "]")
                }
            }
        }
        catch(psft.pt8.joa.JOAException e) {
            String messageToThrow = e.getMessage()
            if(peoplesoftSession != null) {
                String[] msgs = api.peoplesoft.PSConnection.getErrorMessages(peoplesoftSession)
                if(msgs && msgs.length > 0) {
                    messageToThrow = msgs[0]
                    msgs.each {
                        log.error("post: setting the record failed: [" + it + "]")
                    }
                }
            }
            throw new InternalServerException(InternalServerException.Errors.internalServerError, messageToThrow)
        }
        finally {
            log.debug("post: disconnecting")
            if(peoplesoftSession != null) {
                peoplesoftSession.disconnect()
            }
        }

        log.debug("post: done")
    }

    /**
     * Converts a Standard Application Object into a string array
     * @param app The application to convert to a string array
     * @retuns a string array containing the string value of each field in the standard application object
     */
    protected String[] ConvertApplicationToArgsArray(Application app) {
        DateTimeFormatter dateFormat = DateTimeFormatter.ofPattern("yyyy-MM-dd")
        DateTimeFormatter dateTimeFormat = DateTimeFormatter.ofPattern("yyyy-MM-dd-HH.mm.ss.SSSSSS")
        String[] map = [
                //app_id bigint NOT NULL
                PSParameter.ConvertLongToCleanString(app.appId),

                //ccc_id character varying(8) NOT NULL
                PSParameter.ConvertStringToCleanString(app.cccId),

                //status character(1)
                PSParameter.ConvertStringToCleanString(app.status),

                //college_id character(3)
                PSParameter.ConvertStringToCleanString(app.collegeId),

                //term_id bigint
                PSParameter.ConvertLongToCleanString(app.termId),

                //major_id bigint
                PSParameter.ConvertLongToCleanString(app.majorId),

                //intended_major character varying(30)
                PSParameter.ConvertStringToCleanString(app.intendedMajor),

                //edu_goal character(1)
                PSParameter.ConvertStringToCleanString(app.eduGoal),

                //highest_edu_level character(5)
                PSParameter.ConvertStringToCleanString(app.highestEduLevel),

                //consent_indicator boolean DEFAULT false
                PSParameter.ConvertBoolToCleanString(app.consentIndicator),

                //app_lang character(2)
                PSParameter.ConvertStringToCleanString(app.appLang),

                //ack_fin_aid boolean DEFAULT false
                PSParameter.ConvertBoolToCleanString(app.ackFinAid),

                //fin_aid_ref boolean
                PSParameter.ConvertBoolToCleanString(app.finAidRef),

                //confirmation character varying(30)
                PSParameter.ConvertStringToCleanString(app.confirmation),

                //sup_page_code character varying(30)
                PSParameter.ConvertStringToCleanString(app.supPageCode),

                //last_page character varying(25)
                PSParameter.ConvertStringToCleanString(app.lastPage),

                //streetaddress1 character varying(50)
                PSParameter.ConvertStringToCleanString(app.streetaddress1),

                //streetaddress2 character varying(50)
                PSParameter.ConvertStringToCleanString(app.streetaddress2),

                //city character varying(50)
                PSParameter.ConvertStringToCleanString(app.city),

                //postalcode character varying(20)
                PSParameter.ConvertStringToCleanString(app.postalcode),

                //state character(2)
                PSParameter.ConvertStringToCleanString(app.state),

                //nonusaprovince character varying(30)
                PSParameter.ConvertStringToCleanString(app.nonusaprovince),

                //country character(2)
                PSParameter.ConvertStringToCleanString(app.country),

                //non_us_address boolean
                PSParameter.ConvertBoolToCleanString(app.nonUsAddress),

                //email character varying(254)
                PSParameter.ConvertStringToCleanString(app.email),

                //email_verified boolean
                PSParameter.ConvertBoolToCleanString(app.emailVerified),

                //email_verified_timestamp datetime
                PSParameter.ConvertDateTimeToCleanString(app.emailVerifiedTimestamp, dateTimeFormat),

                //perm_streetaddress1 character varying(50)
                PSParameter.ConvertStringToCleanString(app.permStreetaddress1),

                //perm_streetaddress2 character varying(50)
                PSParameter.ConvertStringToCleanString(app.permStreetaddress2),

                //perm_city character varying(50)
                PSParameter.ConvertStringToCleanString(app.permCity),

                //perm_postalcode character varying(20)
                PSParameter.ConvertStringToCleanString(app.permPostalcode),

                //perm_state character(2)
                PSParameter.ConvertStringToCleanString(app.permState),

                //perm_nonusaprovince character varying(30)
                PSParameter.ConvertStringToCleanString(app.permNonusaprovince),

                //perm_country character(2)
                PSParameter.ConvertStringToCleanString(app.permCountry),

                //address_same boolean
                PSParameter.ConvertBoolToCleanString(app.addressSame),

                //mainphone character varying(19)
                PSParameter.ConvertStringToCleanString(app.mainphone),

                //mainphone_ext character varying(4)
                PSParameter.ConvertStringToCleanString(app.mainphoneExt),

                //mainphone_auth_text boolean
                PSParameter.ConvertBoolToCleanString(app.mainphoneAuthText),

                //mainphone_verified boolean
                PSParameter.ConvertBoolToCleanString(app.mainphoneVerified),

                //mainphone_verified_timestamp datetime
                PSParameter.ConvertDateTimeToCleanString(app.mainphoneVerifiedTimestamp, dateTimeFormat),

                //secondphone character varying(19)
                PSParameter.ConvertStringToCleanString(app.secondphone),

                //secondphone_ext character varying(4)
                PSParameter.ConvertStringToCleanString(app.secondphoneExt),

                //secondphone_auth_text boolean
                PSParameter.ConvertBoolToCleanString(app.secondphoneAuthText),

                //preferred_method_of_contact character varying(30)
                PSParameter.ConvertStringToCleanString(app.preferredMethodOfContact),

                //enroll_status character(1)
                PSParameter.ConvertStringToCleanString(app.enrollStatus),

                //hs_edu_level character(1)
                PSParameter.ConvertStringToCleanString(app.hsEduLevel),

                //hs_comp_date date
                PSParameter.ConvertDateToCleanString(app.hsCompDate, dateFormat),

                //higher_edu_level character(1)
                PSParameter.ConvertStringToCleanString(app.higherEduLevel),

                //higher_comp_date date
                PSParameter.ConvertDateToCleanString(app.higherCompDate, dateFormat),

                //hs_not_attended boolean
                PSParameter.ConvertBoolToCleanString(app.hsNotAttended),

                //cahs_graduated boolean
                PSParameter.ConvertBoolToCleanString(app.cahsGraduated),

                //cahs_3year boolean
                PSParameter.ConvertBoolToCleanString(app.cahs3year),

                //hs_name character varying(30)
                PSParameter.ConvertStringToCleanString(app.hsName),

                //hs_city character varying(20)
                PSParameter.ConvertStringToCleanString(app.hsCity),

                //hs_state character(2)
                PSParameter.ConvertStringToCleanString(app.hsState),

                //hs_country character(2)
                PSParameter.ConvertStringToCleanString(app.hsCountry),

                //hs_cds character(6)
                PSParameter.ConvertStringToCleanString(app.hsCds),

                //hs_ceeb character(7)
                PSParameter.ConvertStringToCleanString(app.hsCeeb),

                //hs_not_listed boolean
                PSParameter.ConvertBoolToCleanString(app.hsNotListed),

                //home_schooled boolean
                PSParameter.ConvertBoolToCleanString(app.homeSchooled),

                //college_count smallint
                PSParameter.ConvertIntegerToCleanString(app.collegeCount),

                //hs_attendance smallint
                PSParameter.ConvertIntegerToCleanString(app.hsAttendance),

                //coenroll_confirm boolean
                PSParameter.ConvertBoolToCleanString(app.coenrollConfirm),

                //gender character(1)
                PSParameter.ConvertStringToCleanString(app.gender),

                //pg_firstname character varying(50)
                PSParameter.ConvertStringToCleanString(app.pgFirstname),

                //pg_lastname character varying(50)
                PSParameter.ConvertStringToCleanString(app.pgLastname),

                //pg_rel character(1)
                PSParameter.ConvertStringToCleanString(app.pgRel),

                //pg1_edu character(1)
                PSParameter.ConvertStringToCleanString(app.pg1Edu),

                //pg2_edu character(1)
                PSParameter.ConvertStringToCleanString(app.pg2Edu),

                //pg_edu_mis character(2)
                PSParameter.ConvertStringToCleanString(app.pgEduMis),

                //under19_ind boolean DEFAULT false
                PSParameter.ConvertBoolToCleanString(app.under19Ind),

                //dependent_status character(1)
                PSParameter.ConvertStringToCleanString(app.dependentStatus),

                //race_ethnic text
                PSParameter.ConvertStringToCleanString(app.raceEthnic),

                //hispanic boolean
                PSParameter.ConvertBoolToCleanString(app.hispanic),

                //race_aian_other_description text
                PSParameter.ConvertStringToCleanString(app.raceAIANOtherDescription),

                //race_group text
                PSParameter.ConvertStringToCleanString(app.raceGroup),

                //race_ethnic_full text
                PSParameter.ConvertStringToCleanString(app.raceEthnicFull),

                //ssn text
                PSParameter.ConvertStringToCleanString(app.ssn),

                //birthdate date
                PSParameter.ConvertDateToCleanString(app.birthdate, dateFormat),

                //firstname character varying(50)
                PSParameter.ConvertStringToCleanString(app.firstname),

                //middlename character varying(50)
                PSParameter.ConvertStringToCleanString(app.middlename),

                //lastname character varying(50)
                PSParameter.ConvertStringToCleanString(app.lastname),

                //suffix character varying(3)
                PSParameter.ConvertStringToCleanString(app.suffix),

                //otherfirstname character varying(50)
                PSParameter.ConvertStringToCleanString(app.otherfirstname),

                //othermiddlename character varying(50)
                PSParameter.ConvertStringToCleanString(app.othermiddlename),

                //otherlastname character varying(50)
                PSParameter.ConvertStringToCleanString(app.otherlastname),

                //citizenship_status character(1) NOT NULL
                PSParameter.ConvertStringToCleanString(app.citizenshipStatus),

                //alien_reg_number character varying(20)
                PSParameter.ConvertStringToCleanString(app.alienRegNumber),

                //visa_type character varying(20)
                PSParameter.ConvertStringToCleanString(app.visaType),

                //no_documents boolean
                PSParameter.ConvertBoolToCleanString(app.noDocuments),

                //alien_reg_issue_date date
                PSParameter.ConvertDateToCleanString(app.alienRegIssueDate, dateFormat),

                //alien_reg_expire_date date
                PSParameter.ConvertDateToCleanString(app.alienRegExpireDate, dateFormat),

                //alien_reg_no_expire boolean
                PSParameter.ConvertBoolToCleanString(app.alienRegNoExpire),

                //military_status character(1) NOT NULL
                PSParameter.ConvertStringToCleanString(app.militaryStatus),

                //military_discharge_date date
                PSParameter.ConvertDateToCleanString(app.militaryDischargeDate, dateFormat),

                //military_home_state character(2)
                PSParameter.ConvertStringToCleanString(app.militaryHomeState),

                //military_home_country character(2)
                PSParameter.ConvertStringToCleanString(app.militaryHomeCountry),

                //military_ca_stationed boolean
                PSParameter.ConvertBoolToCleanString(app.militaryCaStationed),

                //military_legal_residence character(2)
                PSParameter.ConvertStringToCleanString(app.militaryLegalResidence),

                //ca_res_2_years boolean
                PSParameter.ConvertBoolToCleanString(app.caRes2Years),

                //ca_date_current date
                PSParameter.ConvertDateToCleanString(app.caDateCurrent, dateFormat),

                //ca_not_arrived boolean
                PSParameter.ConvertBoolToCleanString(app.caNotArrived),

                //ca_college_employee boolean
                PSParameter.ConvertBoolToCleanString(app.caCollegeEmployee),

                //ca_school_employee boolean
                PSParameter.ConvertBoolToCleanString(app.caSchoolEmployee),

                //ca_seasonal_ag boolean
                PSParameter.ConvertBoolToCleanString(app.caSeasonalAg),

                //ca_foster_youth boolean
                PSParameter.ConvertBoolToCleanString(app.caFosterYouth),

                //ca_outside_tax boolean
                PSParameter.ConvertBoolToCleanString(app.caOutsideTax),

                //ca_outside_tax_year date
                PSParameter.ConvertDateToCleanString(app.caOutsideTaxYear, dateFormat),

                //ca_outside_voted boolean
                PSParameter.ConvertBoolToCleanString(app.caOutsideVoted),

                //ca_outside_voted_year date
                PSParameter.ConvertDateToCleanString(app.caOutsideVotedYear, dateFormat),

                //ca_outside_college boolean
                PSParameter.ConvertBoolToCleanString(app.caOutsideCollege),

                //ca_outside_college_year date
                PSParameter.ConvertDateToCleanString(app.caOutsideCollegeYear, dateFormat),

                //ca_outside_lawsuit boolean
                PSParameter.ConvertBoolToCleanString(app.caOutsideLawsuit),

                //ca_outside_lawsuit_year date
                PSParameter.ConvertDateToCleanString(app.caOutsideLawsuitYear, dateFormat),

                //res_status character(1)
                PSParameter.ConvertStringToCleanString(app.resStatus),

                //res_status_change boolean
                PSParameter.ConvertBoolToCleanString(app.resStatusChange),

                //res_prev_date date
                PSParameter.ConvertDateToCleanString(app.resPrevDate, dateFormat),

                //adm_ineligible smallint
                PSParameter.ConvertIntegerToCleanString(app.admIneligible),

                //elig_ab540 boolean
                PSParameter.ConvertBoolToCleanString(app.eligAb540),

                //res_area_a smallint
                PSParameter.ConvertIntegerToCleanString(app.resAreaA),

                //res_area_b smallint
                PSParameter.ConvertIntegerToCleanString(app.resAreaB),

                //res_area_c smallint
                PSParameter.ConvertIntegerToCleanString(app.resAreaC),

                //res_area_d smallint
                PSParameter.ConvertIntegerToCleanString(app.resAreaD),

                //experience integer
                PSParameter.ConvertIntegerToCleanString(app.experience),

                //recommend integer
                PSParameter.ConvertIntegerToCleanString(app.recommend),

                //comments text
                PSParameter.ConvertStringToCleanString(app.comments),

                //comfortable_english boolean
                PSParameter.ConvertBoolToCleanString(app.comfortableEnglish),

                //financial_assistance boolean
                PSParameter.ConvertBoolToCleanString(app.financialAssistance),

                //tanf_ssi_ga boolean
                PSParameter.ConvertBoolToCleanString(app.tanfSsiGa),

                //foster_youths boolean
                PSParameter.ConvertBoolToCleanString(app.fosterYouths),

                //athletic_intercollegiate boolean
                PSParameter.ConvertBoolToCleanString(app.athleticIntercollegiate),

                //athletic_intramural boolean
                PSParameter.ConvertBoolToCleanString(app.athleticIntramural),

                //athletic_not_interested boolean
                PSParameter.ConvertBoolToCleanString(app.athleticNotInterested),

                //academic_counseling boolean
                PSParameter.ConvertBoolToCleanString(app.academicCounseling),

                //basic_skills boolean
                PSParameter.ConvertBoolToCleanString(app.basicSkills),

                //calworks boolean
                PSParameter.ConvertBoolToCleanString(app.calworks),

                //career_planning boolean
                PSParameter.ConvertBoolToCleanString(app.careerPlanning),

                //child_care boolean
                PSParameter.ConvertBoolToCleanString(app.childCare),

                //counseling_personal boolean
                PSParameter.ConvertBoolToCleanString(app.counselingPersonal),

                //dsps boolean
                PSParameter.ConvertBoolToCleanString(app.dsps),

                //eops boolean
                PSParameter.ConvertBoolToCleanString(app.eops),

                //esl boolean
                PSParameter.ConvertBoolToCleanString(app.esl),

                //health_services boolean
                PSParameter.ConvertBoolToCleanString(app.healthServices),

                //housing_info boolean
                PSParameter.ConvertBoolToCleanString(app.housingInfo),

                //employment_assistance boolean
                PSParameter.ConvertBoolToCleanString(app.employmentAssistance),

                //online_classes boolean
                PSParameter.ConvertBoolToCleanString(app.onlineClasses),

                //reentry_program boolean
                PSParameter.ConvertBoolToCleanString(app.reentryProgram),

                //scholarship_info boolean
                PSParameter.ConvertBoolToCleanString(app.scholarshipInfo),

                //student_government boolean
                PSParameter.ConvertBoolToCleanString(app.studentGovernment),

                //testing_assessment boolean
                PSParameter.ConvertBoolToCleanString(app.testingAssessment),

                //transfer_info boolean
                PSParameter.ConvertBoolToCleanString(app.transferInfo),

                //tutoring_services boolean
                PSParameter.ConvertBoolToCleanString(app.tutoringServices),

                //veterans_services boolean
                PSParameter.ConvertBoolToCleanString(app.veteransServices),

                //integrity_fg_01 boolean
                PSParameter.ConvertBoolToCleanString(app.integrityFg01),

                //integrity_fg_02 boolean
                PSParameter.ConvertBoolToCleanString(app.integrityFg02),

                //integrity_fg_03 boolean
                PSParameter.ConvertBoolToCleanString(app.integrityFg03),

                //integrity_fg_04 boolean
                PSParameter.ConvertBoolToCleanString(app.integrityFg04),

                //integrity_fg_11 boolean
                PSParameter.ConvertBoolToCleanString(app.integrityFg11),

                //integrity_fg_47 boolean
                PSParameter.ConvertBoolToCleanString(app.integrityFg47),

                //integrity_fg_48 boolean
                PSParameter.ConvertBoolToCleanString(app.integrityFg48),

                //integrity_fg_49 boolean
                PSParameter.ConvertBoolToCleanString(app.integrityFg49),

                //integrity_fg_50 boolean
                PSParameter.ConvertBoolToCleanString(app.integrityFg50),

                //integrity_fg_51 boolean
                PSParameter.ConvertBoolToCleanString(app.integrityFg51),

                //integrity_fg_52 boolean
                PSParameter.ConvertBoolToCleanString(app.integrityFg52),

                //integrity_fg_53 boolean
                PSParameter.ConvertBoolToCleanString(app.integrityFg53),

                //integrity_fg_54 boolean
                PSParameter.ConvertBoolToCleanString(app.integrityFg54),

                //integrity_fg_55 boolean
                PSParameter.ConvertBoolToCleanString(app.integrityFg55),

                //integrity_fg_56 boolean
                PSParameter.ConvertBoolToCleanString(app.integrityFg56),

                //integrity_fg_57 boolean
                PSParameter.ConvertBoolToCleanString(app.integrityFg57),

                //integrity_fg_58 boolean
                PSParameter.ConvertBoolToCleanString(app.integrityFg58),

                //integrity_fg_59 boolean
                PSParameter.ConvertBoolToCleanString(app.integrityFg59),

                //integrity_fg_60 boolean
                PSParameter.ConvertBoolToCleanString(app.integrityFg60),

                //integrity_fg_61 boolean
                PSParameter.ConvertBoolToCleanString(app.integrityFg61),

                //integrity_fg_62 boolean
                PSParameter.ConvertBoolToCleanString(app.integrityFg62),

                //integrity_fg_63 boolean
                PSParameter.ConvertBoolToCleanString(app.integrityFg63),

                //integrity_fg_70 boolean
                PSParameter.ConvertBoolToCleanString(app.integrityFg70),

                //integrity_fg_80 boolean
                PSParameter.ConvertBoolToCleanString(app.integrityFg80),

                //col1_ceeb character(7)
                PSParameter.ConvertStringToCleanString(app.col1Ceeb),

                //col1_cds character(6)
                PSParameter.ConvertStringToCleanString(app.col1Cds),

                //col1_not_listed boolean
                PSParameter.ConvertBoolToCleanString(app.col1NotListed),

                //col1_name character varying(30)
                PSParameter.ConvertStringToCleanString(app.col1Name),

                //col1_city character varying(20)
                PSParameter.ConvertStringToCleanString(app.col1City),

                //col1_state character varying(30)
                PSParameter.ConvertStringToCleanString(app.col1State),

                //col1_country character(2)
                PSParameter.ConvertStringToCleanString(app.col1Country),

                //col1_start_date date
                PSParameter.ConvertDateToCleanString(app.col1StartDate, dateFormat),

                //col1_end_date date
                PSParameter.ConvertDateToCleanString(app.col1EndDate, dateFormat),

                //col1_degree_date date
                PSParameter.ConvertDateToCleanString(app.col1DegreeDate, dateFormat),

                //col1_degree_obtained character(1)
                PSParameter.ConvertStringToCleanString(app.col1DegreeObtained),

                //col2_ceeb character(7)
                PSParameter.ConvertStringToCleanString(app.col2Ceeb),

                //col2_cds character(6)
                PSParameter.ConvertStringToCleanString(app.col2Cds),

                //col2_not_listed boolean
                PSParameter.ConvertBoolToCleanString(app.col2NotListed),

                //col2_name character varying(30)
                PSParameter.ConvertStringToCleanString(app.col2Name),

                //col2_city character varying(20)
                PSParameter.ConvertStringToCleanString(app.col2City),

                //col2_state character varying(30)
                PSParameter.ConvertStringToCleanString(app.col2State),

                //col2_country character(2)
                PSParameter.ConvertStringToCleanString(app.col2Country),

                //col2_start_date date
                PSParameter.ConvertDateToCleanString(app.col2StartDate, dateFormat),

                //col2_end_date date
                PSParameter.ConvertDateToCleanString(app.col2EndDate, dateFormat),

                //col2_degree_date date
                PSParameter.ConvertDateToCleanString(app.col2DegreeDate, dateFormat),

                //col2_degree_obtained character(1)
                PSParameter.ConvertStringToCleanString(app.col2DegreeObtained),

                //col3_ceeb character(7)
                PSParameter.ConvertStringToCleanString(app.col3Ceeb),

                //col3_cds character(6)
                PSParameter.ConvertStringToCleanString(app.col3Cds),

                //col3_not_listed boolean
                PSParameter.ConvertBoolToCleanString(app.col3NotListed),

                //col3_name character varying(30)
                PSParameter.ConvertStringToCleanString(app.col3Name),

                //col3_city character varying(20)
                PSParameter.ConvertStringToCleanString(app.col3City),

                //col3_state character varying(30)
                PSParameter.ConvertStringToCleanString(app.col3State),

                //col3_country character(2)
                PSParameter.ConvertStringToCleanString(app.col3Country),

                //col3_start_date date
                PSParameter.ConvertDateToCleanString(app.col3StartDate, dateFormat),

                //col3_end_date date
                PSParameter.ConvertDateToCleanString(app.col3EndDate, dateFormat),

                //col3_degree_date date
                PSParameter.ConvertDateToCleanString(app.col3DegreeDate, dateFormat),

                //col3_degree_obtained character(1)
                PSParameter.ConvertStringToCleanString(app.col3DegreeObtained),

                //col4_ceeb character(7)
                PSParameter.ConvertStringToCleanString(app.col4Ceeb),

                //col4_cds character(6)
                PSParameter.ConvertStringToCleanString(app.col4Cds),

                //col4_not_listed boolean
                PSParameter.ConvertBoolToCleanString(app.col4NotListed),

                //col4_name character varying(30)
                PSParameter.ConvertStringToCleanString(app.col4Name),

                //col4_city character varying(20)
                PSParameter.ConvertStringToCleanString(app.col4City),

                //col4_state character varying(30)
                PSParameter.ConvertStringToCleanString(app.col4State),

                //col4_country character(2)
                PSParameter.ConvertStringToCleanString(app.col4Country),

                //col4_start_date date
                PSParameter.ConvertDateToCleanString(app.col4StartDate, dateFormat),

                //col4_end_date date
                PSParameter.ConvertDateToCleanString(app.col4EndDate, dateFormat),

                //col4_degree_date date
                PSParameter.ConvertDateToCleanString(app.col4DegreeDate, dateFormat),

                //col4_degree_obtained character(1)
                PSParameter.ConvertStringToCleanString(app.col4DegreeObtained),

                //college_name character varying(50)
                PSParameter.ConvertStringToCleanString(app.collegeName),

                //district_name character varying(50)
                PSParameter.ConvertStringToCleanString(app.districtName),

                //term_code character varying(15)
                PSParameter.ConvertStringToCleanString(app.termCode),

                //term_description character varying(100)
                PSParameter.ConvertStringToCleanString(app.termDescription),

                //major_code character varying(30)
                PSParameter.ConvertStringToCleanString(app.majorCode),

                //major_description character varying(100)
                PSParameter.ConvertStringToCleanString(app.majorDescription),

                //tstmp_submit timestamp with time zone
                PSParameter.ConvertDateTimeToCleanString(app.tstmpSubmit, dateTimeFormat),

                //tstmp_create timestamp with time zone DEFAULT now()
                PSParameter.ConvertDateTimeToCleanString(app.tstmpCreate, dateTimeFormat),

                //tstmp_update timestamp with time zone
                PSParameter.ConvertDateTimeToCleanString(app.tstmpUpdate, dateTimeFormat),

                //ssn_display character varying(11)
                PSParameter.ConvertStringToCleanString(app.ssnDisplay),

                //foster_youth_status character(1)
                PSParameter.ConvertStringToCleanString(app.fosterYouthStatus),

                //foster_youth_preference boolean
                PSParameter.ConvertBoolToCleanString(app.fosterYouthPreference),

                //foster_youth_mis boolean
                PSParameter.ConvertBoolToCleanString(app.fosterYouthMis),

                //foster_youth_priority boolean
                PSParameter.ConvertBoolToCleanString(app.fosterYouthPriority),

                //tstmp_download timestamp with time zone
                PSParameter.ConvertDateTimeToCleanString(app.tstmpDownload, dateTimeFormat),

                //address_validation character(1)
                PSParameter.ConvertStringToCleanString(app.addressValidation),

                //address_validation_override boolean
                PSParameter.ConvertBoolToCleanString(app.addressValidationOverride),

                //address_validation_override_timestamp datetime
                PSParameter.ConvertDateTimeToCleanString(app.addressValidationOverrideTimestamp, dateTimeFormat),

                //zip4 character(4)
                PSParameter.ConvertStringToCleanString(app.zip4),

                //perm_address_validation character(1)
                PSParameter.ConvertStringToCleanString(app.permAddressValidation),

                //perm_zip4 character(4)
                PSParameter.ConvertStringToCleanString(app.permZip4),

                //discharge_type character varying(1)
                PSParameter.ConvertStringToCleanString(app.dischargeType),

                //college_expelled_summary boolean
                PSParameter.ConvertBoolToCleanString(app.collegeExpelledSummary),

                //col1_expelled_status boolean
                PSParameter.ConvertBoolToCleanString(app.col1ExpelledStatus),

                //col2_expelled_status boolean
                PSParameter.ConvertBoolToCleanString(app.col2ExpelledStatus),

                //col3_expelled_status boolean
                PSParameter.ConvertBoolToCleanString(app.col3ExpelledStatus),

                //col4_expelled_status boolean
                PSParameter.ConvertBoolToCleanString(app.col4ExpelledStatus),

                //integrity_flags character varying(255)
                PSParameter.ConvertStringToCleanString(app.integrityFlags),

                //rdd date
                PSParameter.ConvertDateToCleanString(app.rdd, dateFormat),

                //ssn_type character(1)
                PSParameter.ConvertStringToCleanString(app.ssnType),

                //military_stationed_ca_ed boolean
                PSParameter.ConvertBoolToCleanString(app.militaryStationedCaEd),

                //integrity_fg_65 boolean
                PSParameter.ConvertBoolToCleanString(app.integrityFg65),

                //integrity_fg_64 boolean
                PSParameter.ConvertBoolToCleanString(app.integrityFg64),

                //ip_address character varying(15)
                PSParameter.ConvertStringToCleanString(app.ipAddress),

                //campaign1 character varying(255)
                PSParameter.ConvertStringToCleanString(app.campaign1),

                //campaign2 character varying(255)
                PSParameter.ConvertStringToCleanString(app.campaign2),

                //campaign3 character varying(255)
                PSParameter.ConvertStringToCleanString(app.campaign3),

                // unencrypted version of orientation_encrypted as a String
                PSParameter.ConvertStringToCleanString(app.orientation),

                // unencrypted version of transgender_encrypted as a String
                PSParameter.ConvertStringToCleanString(app.transgender),

                //ssn_exception boolean DEFAULT false
                PSParameter.ConvertBoolToCleanString(app.ssnException),

                //integrity_fg_71 boolean
                PSParameter.ConvertBoolToCleanString(app.integrityFg71),

                //preferred_firstname character varying(50)
                PSParameter.ConvertStringToCleanString(app.preferredFirstname),

                //preferred_middlename character varying(50)
                PSParameter.ConvertStringToCleanString(app.preferredMiddlename),

                //preferred_lastname character varying(50)
                PSParameter.ConvertStringToCleanString(app.preferredLastname),

                //preferred_name boolean DEFAULT false
                PSParameter.ConvertBoolToCleanString(app.preferredName),

                //ssn_no boolean
                PSParameter.ConvertBoolToCleanString(app.ssnNo),

                //completed_eleventh_grade boolean
                PSParameter.ConvertBoolToCleanString(app.completedEleventhGrade),

                //grade_point_average character varying(5)
                PSParameter.ConvertStringToCleanString(app.gradePointAverage),

                //highest_english_course integer
                PSParameter.ConvertIntegerToCleanString(app.highestEnglishCourse),

                //highest_english_grade character varying(2)
                PSParameter.ConvertStringToCleanString(app.highestEnglishGrade),

                //highest_math_course_taken integer
                PSParameter.ConvertIntegerToCleanString(app.highestMathCourseTaken),

                //highest_math_taken_grade character varying(2)
                PSParameter.ConvertStringToCleanString(app.highestMathTakenGrade),

                //highest_math_course_passed integer
                PSParameter.ConvertIntegerToCleanString(app.highestMathCoursePassed),

                //highest_math_passed_grade character varying(2)
                PSParameter.ConvertStringToCleanString(app.highestMathPassedGrade),

                //integrity_fg_30 boolean
                PSParameter.ConvertBoolToCleanString(app.integrityFg30),

                //hs_cds_full character varying(14)
                PSParameter.ConvertStringToCleanString(app.hsCdsFull),

                //col1_cds_full character varying(14)
                PSParameter.ConvertStringToCleanString(app.col1CdsFull),

                //col2_cds_full character varying(14)
                PSParameter.ConvertStringToCleanString(app.col2CdsFull),

                //col3_cds_full character varying(14)
                PSParameter.ConvertStringToCleanString(app.col3CdsFull),

                //col4_cds_full character varying(14)
                PSParameter.ConvertStringToCleanString(app.col4CdsFull),

                //ssid character varying(10)
                PSParameter.ConvertStringToCleanString(app.ssid),

                //no_perm_address_homeless boolean DEFAULT false
                PSParameter.ConvertBoolToCleanString(app.noPermAddressHomeless),

                //no_mailing_address_homeless boolean DEFAULT false
                PSParameter.ConvertBoolToCleanString(app.noMailingAddressHomeless),

                //term_start date
                PSParameter.ConvertDateToCleanString(app.termStart, dateFormat),

                //term_end date
                PSParameter.ConvertDateToCleanString(app.termEnd, dateFormat),

                //homeless_youth boolean
                PSParameter.ConvertBoolToCleanString(app.homelessYouth),

                //integrity_fg_40 boolean
                PSParameter.ConvertBoolToCleanString(app.integrityFg40),

                //cip_code character(6)
                PSParameter.ConvertStringToCleanString(app.cipCode),

                //major_category character varying(100)
                PSParameter.ConvertStringToCleanString(app.majorCategory),

                //mainphoneintl character varying(25)
                PSParameter.ConvertStringToCleanString(app.mainphoneintl),

                //secondphoneintl character varying(25)
                PSParameter.ConvertStringToCleanString(app.secondphoneintl),

                //non_credit boolean
                PSParameter.ConvertBoolToCleanString(app.nonCredit),

                //integrity_fg_81 boolean
                PSParameter.ConvertBoolToCleanString(app.integrityFg81),

                //highest_grade_completed character varying(2)
                PSParameter.ConvertStringToCleanString(app.highestGradeCompleted),

                //accepted_terms Boolean
                PSParameter.ConvertBoolToCleanString(app.acceptedTerms),

                //accepted_terms_timestamp TIMESTAMP
                PSParameter.ConvertDateTimeToCleanString(app.acceptedTermsTimestamp, dateTimeFormat),

                //mailing_address_validation_override
                PSParameter.ConvertBoolToCleanString(app.mailingAddressValidationOverride),

                //phoneType enumerated Mobile/Landline varying (9)
                PSParameter.ConvertStringToCleanString(app.phoneType),

                //ipAddressAtAccountCreation varying (15)
                PSParameter.ConvertStringToCleanString(app.ipAddressAtAccountCreation),

                //ipAddressAtAppCreation varying (15)
                PSParameter.ConvertStringToCleanString(app.ipAddressAtAppCreation),

                //fraud score double
                PSParameter.ConvertDoubleToCleanString(app.fraudScore),

                //fraud score status integer
                PSParameter.ConvertIntegerToCleanString(app.fraudStatus),

                //student_parent boolean
                PSParameter.ConvertBoolToCleanString(app.studentParent),

                //idme_confirmation_timestamp TIMESTAMP
                PSParameter.ConvertDateTimeToCleanString(app.idmeConfirmationTimestamp, dateTimeFormat),

                //idme_optin_timestamp TIMESTAMP
                PSParameter.ConvertDateTimeToCleanString(app.idmeOptinTimestamp, dateTimeFormat),

                //idme_workflow_status varying (50)
                PSParameter.ConvertStringToCleanString(app.idmeWorkflowStatus),

                //student_deps_under18 small int
                PSParameter.ConvertIntegerToCleanString(app.studentDepsUnder18),

                //student_deps_18over small int
                PSParameter.ConvertIntegerToCleanString(app.studentDeps18Over)
        ]
        return map
    }

    /**
     * Converts a Standard Application's Supplemental Questions Object into a string array
     * @param ques The supplemental question to convert to a string array
     * @retuns a string array containing the string value of each field in the supplemental question object
     */
    protected String[] ConvertSupplementalQuestionToArgsArray(SupplementalQuestions ques) {
        DateTimeFormatter dateFormat = DateTimeFormatter.ofPattern("yyyy-MM-dd")
        String[] map = [
                //app_id bigint NOT NULL
                PSParameter.ConvertLongToCleanString(ques.appId),

                //supp_text_01 character varying(250)
                PSParameter.ConvertStringToCleanString(ques.suppText01),

                //supp_text_02 character varying(250)
                PSParameter.ConvertStringToCleanString(ques.suppText02),

                //supp_text_03 character varying(250)
                PSParameter.ConvertStringToCleanString(ques.suppText03),

                //supp_text_04 character varying(250)
                PSParameter.ConvertStringToCleanString(ques.suppText04),

                //supp_text_05 character varying(250)
                PSParameter.ConvertStringToCleanString(ques.suppText05),

                //supp_text_06 character varying(250)
                PSParameter.ConvertStringToCleanString(ques.suppText06),

                //supp_text_07 character varying(250)
                PSParameter.ConvertStringToCleanString(ques.suppText07),

                //supp_text_08 character varying(250)
                PSParameter.ConvertStringToCleanString(ques.suppText08),

                //supp_text_09 character varying(250)
                PSParameter.ConvertStringToCleanString(ques.suppText09),

                //supp_text_10 character varying(250)
                PSParameter.ConvertStringToCleanString(ques.suppText10),

                //supp_text_11 character varying(250)
                PSParameter.ConvertStringToCleanString(ques.suppText11),

                //supp_text_12 character varying(250)
                PSParameter.ConvertStringToCleanString(ques.suppText12),

                //supp_text_13 character varying(250)
                PSParameter.ConvertStringToCleanString(ques.suppText13),

                //supp_text_14 character varying(250)
                PSParameter.ConvertStringToCleanString(ques.suppText14),

                //supp_text_15 character varying(250)
                PSParameter.ConvertStringToCleanString(ques.suppText15),

                //supp_text_16 character varying(250)
                PSParameter.ConvertStringToCleanString(ques.suppText16),

                //supp_text_17 character varying(250)
                PSParameter.ConvertStringToCleanString(ques.suppText17),

                //supp_text_18 character varying(250)
                PSParameter.ConvertStringToCleanString(ques.suppText18),

                //supp_text_19 character varying(250)
                PSParameter.ConvertStringToCleanString(ques.suppText19),

                //supp_text_20 character varying(250)
                PSParameter.ConvertStringToCleanString(ques.suppText20),

                //supp_check_01 boolean
                PSParameter.ConvertBoolToCleanString(ques.suppCheck01),

                //supp_check_02 boolean
                PSParameter.ConvertBoolToCleanString(ques.suppCheck02),

                //supp_check_03 boolean
                PSParameter.ConvertBoolToCleanString(ques.suppCheck03),

                //supp_check_04 boolean
                PSParameter.ConvertBoolToCleanString(ques.suppCheck04),

                //supp_check_05 boolean
                PSParameter.ConvertBoolToCleanString(ques.suppCheck05),

                //supp_check_06 boolean
                PSParameter.ConvertBoolToCleanString(ques.suppCheck06),

                //supp_check_07 boolean
                PSParameter.ConvertBoolToCleanString(ques.suppCheck07),

                //supp_check_08 boolean
                PSParameter.ConvertBoolToCleanString(ques.suppCheck08),

                //supp_check_09 boolean
                PSParameter.ConvertBoolToCleanString(ques.suppCheck09),

                //supp_check_10 boolean
                PSParameter.ConvertBoolToCleanString(ques.suppCheck10),

                //supp_check_11 boolean
                PSParameter.ConvertBoolToCleanString(ques.suppCheck11),

                //supp_check_12 boolean
                PSParameter.ConvertBoolToCleanString(ques.suppCheck12),

                //supp_check_13 boolean
                PSParameter.ConvertBoolToCleanString(ques.suppCheck13),

                //supp_check_14 boolean
                PSParameter.ConvertBoolToCleanString(ques.suppCheck14),

                //supp_check_15 boolean
                PSParameter.ConvertBoolToCleanString(ques.suppCheck15),

                //supp_check_16 boolean
                PSParameter.ConvertBoolToCleanString(ques.suppCheck16),

                //supp_check_17 boolean
                PSParameter.ConvertBoolToCleanString(ques.suppCheck17),

                //supp_check_18 boolean
                PSParameter.ConvertBoolToCleanString(ques.suppCheck18),

                //supp_check_19 boolean
                PSParameter.ConvertBoolToCleanString(ques.suppCheck19),

                //supp_check_20 boolean
                PSParameter.ConvertBoolToCleanString(ques.suppCheck20),

                //supp_check_21 boolean
                PSParameter.ConvertBoolToCleanString(ques.suppCheck21),

                //supp_check_22 boolean
                PSParameter.ConvertBoolToCleanString(ques.suppCheck22),

                //supp_check_23 boolean
                PSParameter.ConvertBoolToCleanString(ques.suppCheck23),

                //supp_check_24 boolean
                PSParameter.ConvertBoolToCleanString(ques.suppCheck24),

                //supp_check_25 boolean
                PSParameter.ConvertBoolToCleanString(ques.suppCheck25),

                //supp_check_26 boolean
                PSParameter.ConvertBoolToCleanString(ques.suppCheck26),

                //supp_check_27 boolean
                PSParameter.ConvertBoolToCleanString(ques.suppCheck27),

                //supp_check_28 boolean
                PSParameter.ConvertBoolToCleanString(ques.suppCheck28),

                //supp_check_29 boolean
                PSParameter.ConvertBoolToCleanString(ques.suppCheck29),

                //supp_check_30 boolean
                PSParameter.ConvertBoolToCleanString(ques.suppCheck30),

                //supp_check_31 boolean
                PSParameter.ConvertBoolToCleanString(ques.suppCheck31),

                //supp_check_32 boolean
                PSParameter.ConvertBoolToCleanString(ques.suppCheck32),

                //supp_check_33 boolean
                PSParameter.ConvertBoolToCleanString(ques.suppCheck33),

                //supp_check_34 boolean
                PSParameter.ConvertBoolToCleanString(ques.suppCheck34),

                //supp_check_35 boolean
                PSParameter.ConvertBoolToCleanString(ques.suppCheck35),

                //supp_check_36 boolean
                PSParameter.ConvertBoolToCleanString(ques.suppCheck36),

                //supp_check_37 boolean
                PSParameter.ConvertBoolToCleanString(ques.suppCheck37),

                //supp_check_38 boolean
                PSParameter.ConvertBoolToCleanString(ques.suppCheck38),

                //supp_check_39 boolean
                PSParameter.ConvertBoolToCleanString(ques.suppCheck39),

                //supp_check_40 boolean
                PSParameter.ConvertBoolToCleanString(ques.suppCheck40),

                //supp_check_41 boolean
                PSParameter.ConvertBoolToCleanString(ques.suppCheck41),

                //supp_check_42 boolean
                PSParameter.ConvertBoolToCleanString(ques.suppCheck42),

                //supp_check_43 boolean
                PSParameter.ConvertBoolToCleanString(ques.suppCheck43),

                //supp_check_44 boolean
                PSParameter.ConvertBoolToCleanString(ques.suppCheck44),

                //supp_check_45 boolean
                PSParameter.ConvertBoolToCleanString(ques.suppCheck45),

                //supp_check_46 boolean
                PSParameter.ConvertBoolToCleanString(ques.suppCheck46),

                //supp_check_47 boolean
                PSParameter.ConvertBoolToCleanString(ques.suppCheck47),

                //supp_check_48 boolean
                PSParameter.ConvertBoolToCleanString(ques.suppCheck48),

                //supp_check_49 boolean
                PSParameter.ConvertBoolToCleanString(ques.suppCheck49),

                //supp_check_50 boolean
                PSParameter.ConvertBoolToCleanString(ques.suppCheck50),

                //supp_yesno_01 boolean
                PSParameter.ConvertBoolToCleanString(ques.suppYesno01),

                //supp_yesno_02 boolean
                PSParameter.ConvertBoolToCleanString(ques.suppYesno02),

                //supp_yesno_03 boolean
                PSParameter.ConvertBoolToCleanString(ques.suppYesno03),

                //supp_yesno_04 boolean
                PSParameter.ConvertBoolToCleanString(ques.suppYesno04),

                //supp_yesno_05 boolean
                PSParameter.ConvertBoolToCleanString(ques.suppYesno05),

                //supp_yesno_06 boolean
                PSParameter.ConvertBoolToCleanString(ques.suppYesno06),

                //supp_yesno_07 boolean
                PSParameter.ConvertBoolToCleanString(ques.suppYesno07),

                //supp_yesno_08 boolean
                PSParameter.ConvertBoolToCleanString(ques.suppYesno08),

                //supp_yesno_09 boolean
                PSParameter.ConvertBoolToCleanString(ques.suppYesno09),

                //supp_yesno_10 boolean
                PSParameter.ConvertBoolToCleanString(ques.suppYesno10),

                //supp_yesno_11 boolean
                PSParameter.ConvertBoolToCleanString(ques.suppYesno11),

                //supp_yesno_12 boolean
                PSParameter.ConvertBoolToCleanString(ques.suppYesno12),

                //supp_yesno_13 boolean
                PSParameter.ConvertBoolToCleanString(ques.suppYesno13),

                //supp_yesno_14 boolean
                PSParameter.ConvertBoolToCleanString(ques.suppYesno14),

                //supp_yesno_15 boolean
                PSParameter.ConvertBoolToCleanString(ques.suppYesno15),

                //supp_yesno_16 boolean
                PSParameter.ConvertBoolToCleanString(ques.suppYesno16),

                //supp_yesno_17 boolean
                PSParameter.ConvertBoolToCleanString(ques.suppYesno17),

                //supp_yesno_18 boolean
                PSParameter.ConvertBoolToCleanString(ques.suppYesno18),

                //supp_yesno_19 boolean
                PSParameter.ConvertBoolToCleanString(ques.suppYesno19),

                //supp_yesno_20 boolean
                PSParameter.ConvertBoolToCleanString(ques.suppYesno20),

                //supp_yesno_21 boolean
                PSParameter.ConvertBoolToCleanString(ques.suppYesno21),

                //supp_yesno_22 boolean
                PSParameter.ConvertBoolToCleanString(ques.suppYesno22),

                //supp_yesno_23 boolean
                PSParameter.ConvertBoolToCleanString(ques.suppYesno23),

                //supp_yesno_24 boolean
                PSParameter.ConvertBoolToCleanString(ques.suppYesno24),

                //supp_yesno_25 boolean
                PSParameter.ConvertBoolToCleanString(ques.suppYesno25),

                //supp_yesno_26 boolean
                PSParameter.ConvertBoolToCleanString(ques.suppYesno26),

                //supp_yesno_27 boolean
                PSParameter.ConvertBoolToCleanString(ques.suppYesno27),

                //supp_yesno_28 boolean
                PSParameter.ConvertBoolToCleanString(ques.suppYesno28),

                //supp_yesno_29 boolean
                PSParameter.ConvertBoolToCleanString(ques.suppYesno29),

                //supp_yesno_30 boolean
                PSParameter.ConvertBoolToCleanString(ques.suppYesno30),

                //supp_menu_01 character varying(60)
                PSParameter.ConvertStringToCleanString(ques.suppMenu01),

                //supp_menu_02 character varying(60)
                PSParameter.ConvertStringToCleanString(ques.suppMenu02),

                //supp_menu_03 character varying(60)
                PSParameter.ConvertStringToCleanString(ques.suppMenu03),

                //supp_menu_04 character varying(60)
                PSParameter.ConvertStringToCleanString(ques.suppMenu04),

                //supp_menu_05 character varying(60)
                PSParameter.ConvertStringToCleanString(ques.suppMenu05),

                //supp_menu_06 character varying(60)
                PSParameter.ConvertStringToCleanString(ques.suppMenu06),

                //supp_menu_07 character varying(60)
                PSParameter.ConvertStringToCleanString(ques.suppMenu07),

                //supp_menu_08 character varying(60)
                PSParameter.ConvertStringToCleanString(ques.suppMenu08),

                //supp_menu_09 character varying(60)
                PSParameter.ConvertStringToCleanString(ques.suppMenu09),

                //supp_menu_10 character varying(60)
                PSParameter.ConvertStringToCleanString(ques.suppMenu10),

                //supp_menu_11 character varying(60)
                PSParameter.ConvertStringToCleanString(ques.suppMenu11),

                //supp_menu_12 character varying(60)
                PSParameter.ConvertStringToCleanString(ques.suppMenu12),

                //supp_menu_13 character varying(60)
                PSParameter.ConvertStringToCleanString(ques.suppMenu13),

                //supp_menu_14 character varying(60)
                PSParameter.ConvertStringToCleanString(ques.suppMenu14),

                //supp_menu_15 character varying(60)
                PSParameter.ConvertStringToCleanString(ques.suppMenu15),

                //supp_menu_16 character varying(60)
                PSParameter.ConvertStringToCleanString(ques.suppMenu16),

                //supp_menu_17 character varying(60)
                PSParameter.ConvertStringToCleanString(ques.suppMenu17),

                //supp_menu_18 character varying(60)
                PSParameter.ConvertStringToCleanString(ques.suppMenu18),

                //supp_menu_19 character varying(60)
                PSParameter.ConvertStringToCleanString(ques.suppMenu19),

                //supp_menu_20 character varying(60)
                PSParameter.ConvertStringToCleanString(ques.suppMenu20),

                //supp_menu_21 character varying(60)
                PSParameter.ConvertStringToCleanString(ques.suppMenu21),

                //supp_menu_22 character varying(60)
                PSParameter.ConvertStringToCleanString(ques.suppMenu22),

                //supp_menu_23 character varying(60)
                PSParameter.ConvertStringToCleanString(ques.suppMenu23),

                //supp_menu_24 character varying(60)
                PSParameter.ConvertStringToCleanString(ques.suppMenu24),

                //supp_menu_25 character varying(60)
                PSParameter.ConvertStringToCleanString(ques.suppMenu25),

                //supp_menu_26 character varying(60)
                PSParameter.ConvertStringToCleanString(ques.suppMenu26),

                //supp_menu_27 character varying(60)
                PSParameter.ConvertStringToCleanString(ques.suppMenu27),

                //supp_menu_28 character varying(60)
                PSParameter.ConvertStringToCleanString(ques.suppMenu28),

                //supp_menu_29 character varying(60)
                PSParameter.ConvertStringToCleanString(ques.suppMenu29),

                //supp_menu_30 character varying(60)
                PSParameter.ConvertStringToCleanString(ques.suppMenu30),

                //supp_date_01 date
                PSParameter.ConvertDateToCleanString(ques.suppDate01, dateFormat),

                //supp_date_02 date
                PSParameter.ConvertDateToCleanString(ques.suppDate02, dateFormat),

                //supp_date_03 date
                PSParameter.ConvertDateToCleanString(ques.suppDate03, dateFormat),

                //supp_date_04 date
                PSParameter.ConvertDateToCleanString(ques.suppDate04, dateFormat),

                //supp_date_05 date
                PSParameter.ConvertDateToCleanString(ques.suppDate05, dateFormat),

                //supp_state_01 character(2)
                PSParameter.ConvertStringToCleanString(ques.suppState01),

                //supp_state_02 character(2)
                PSParameter.ConvertStringToCleanString(ques.suppState02),

                //supp_state_03 character(2)
                PSParameter.ConvertStringToCleanString(ques.suppState03),

                //supp_state_04 character(2)
                PSParameter.ConvertStringToCleanString(ques.suppState04),

                //supp_state_05 character(2)
                PSParameter.ConvertStringToCleanString(ques.suppState05),

                //supp_country_01 character(2)
                PSParameter.ConvertStringToCleanString(ques.suppCountry01),

                //supp_country_02 character(2)
                PSParameter.ConvertStringToCleanString(ques.suppCountry02),

                //supp_country_03 character(2)
                PSParameter.ConvertStringToCleanString(ques.suppCountry03),

                //supp_country_04 character(2)
                PSParameter.ConvertStringToCleanString(ques.suppCountry04),

                //supp_country_05 character(2)
                PSParameter.ConvertStringToCleanString(ques.suppCountry05),

                //supp_phonenumber_01 character varying(25)
                PSParameter.ConvertStringToCleanString(ques.suppPhonenumber01),

                //supp_phonenumber_02 character varying(25)
                PSParameter.ConvertStringToCleanString(ques.suppPhonenumber02),

                //supp_phonenumber_03 character varying(25)
                PSParameter.ConvertStringToCleanString(ques.suppPhonenumber03),

                //supp_phonenumber_04 character varying(25)
                PSParameter.ConvertStringToCleanString(ques.suppPhonenumber04),

                //supp_phonenumber_05 character varying(25)
                PSParameter.ConvertStringToCleanString(ques.suppPhonenumber05),

                //supp_secret_01 text
                PSParameter.ConvertStringToCleanString(ques.suppSecret01),

                //supp_secret_02 text
                PSParameter.ConvertStringToCleanString(ques.suppSecret02),

                //supp_secret_03 text
                PSParameter.ConvertStringToCleanString(ques.suppSecret03),

                //supp_secret_04 text
                PSParameter.ConvertStringToCleanString(ques.suppSecret04),

                //supp_secret_05 text
                PSParameter.ConvertStringToCleanString(ques.suppSecret05)
        ]

        return map
    }

    /**
     * Converts a string array into an Application Object
     * Note: Assumes that the order of the string array is the same order as the fields in the Application object
     * @param results The string array to convert to an Application Object
     * @retuns a Application object with each field populated from the string array
     */
    protected Application ConvertStringArrayToApplication(String[] results) {
        if(results.length < 313) {
            return null
        }

        DateTimeFormatter dateFormat = DateTimeFormatter.ofPattern("yyyy-MM-dd")
        DateTimeFormatter dateTimeFormat = DateTimeFormatter.ofPattern("yyyy-MM-dd-HH.mm.ss.SSSSSS")

        Integer i = 0
        Application app = new Application()


        //app_id bigint NOT NULL
        app.appId = results[i++].toBigInteger()

        //ccc_id character varying(8) NOT NULL
        app.cccId = results[i++]

        //status character(1)
        app.status = results[i++]

        //college_id character(3)
        app.collegeId = results[i++]

        //term_id bigint
        app.termId = results[i++].toBigInteger()

        //major_id bigint
        app.majorId = results[i++].toBigInteger()

        //intended_major character varying(30)
        app.intendedMajor = results[i++]

        //edu_goal character(1)
        app.eduGoal = results[i++]

        //highest_edu_level character(5)
        app.highestEduLevel = results[i++]

        //consent_indicator boolean DEFAULT false
        app.consentIndicator = (results[i++] == 'Y')

        //app_lang character(2)
        app.appLang = results[i++]

        //ack_fin_aid boolean DEFAULT false
        app.ackFinAid = (results[i++] == 'Y')

        //fin_aid_ref boolean
        app.finAidRef = (results[i++] == 'Y')

        //confirmation character varying(30)
        app.confirmation = results[i++]

        //sup_page_code character varying(30)
        app.supPageCode = results[i++]

        //last_page character varying(25)
        app.lastPage = results[i++]

        //streetaddress1 character varying(50)
        app.streetaddress1 = results[i++]

        //streetaddress2 character varying(50)
        app.streetaddress2 = results[i++]

        //city character varying(50)
        app.city = results[i++]

        //postalcode character varying(20)
        app.postalcode = results[i++]

        //state character(2)
        app.state = results[i++]

        //nonusaprovince character varying(30)
        app.nonusaprovince = results[i++]

        //country character(2)
        app.country = results[i++]

        //non_us_address boolean
        app.nonUsAddress = (results[i++] == 'Y')

        //email character varying(254)
        app.email = results[i++]

        //email_verified boolean
        app.emailVerified = (results[i++] == 'Y')

        //email_verified_timestamp datetime
        app.emailVerifiedTimestamp = (results[i] == null || results[i].trim().isEmpty() ? null : LocalDateTime.parse(results[i], dateTimeFormat))
        i++

        //perm_streetaddress1 character varying(50)
        app.permStreetaddress1 = results[i++]

        //perm_streetaddress2 character varying(50)
        app.permStreetaddress2 = results[i++]

        //perm_city character varying(50)
        app.permCity = results[i++]

        //perm_postalcode character varying(20)
        app.permPostalcode = results[i++]

        //perm_state character(2)
        app.permState = results[i++]

        //perm_nonusaprovince character varying(30)
        app.permNonusaprovince = results[i++]

        //perm_country character(2)
        app.permCountry = results[i++]

        //address_same boolean
        app.addressSame = (results[i++] == 'Y')

        //mainphone character varying(19)
        app.mainphone = results[i++]

        //mainphone_ext character varying(4)
        app.mainphoneExt = results[i++]

        //mainphone_auth_text boolean
        app.mainphoneAuthText = (results[i++] == 'Y')

        //mainphone_verified boolean
        app.mainphoneVerified = (results[i++] == 'Y')

        //mainphone_verified_timestamp datetime
        app.mainphoneVerifiedTimestamp = (results[i] == null || results[i].trim().isEmpty() ? null : LocalDateTime.parse(results[i], dateTimeFormat))
        i++

        //secondphone character varying(19)
        app.secondphone = results[i++]

        //secondphone_ext character varying(4)
        app.secondphoneExt = results[i++]

        //secondphone_auth_text boolean
        app.secondphoneAuthText = (results[i++] == 'Y')

        //preferred_method_of_contact character varying(30)
        app.preferredMethodOfContact = results[i++]

        //enroll_status character(1)
        app.enrollStatus = results[i++]

        //hs_edu_level character(1)
        app.hsEduLevel = results[i++]

        //hs_comp_date date
        app.hsCompDate = (results[i] == null || results[i].trim().isEmpty() ? null : LocalDate.parse(results[i], dateFormat))
        i++

        //higher_edu_level character(1)
        app.higherEduLevel = results[i++]

        //higher_comp_date date
        app.higherCompDate = (results[i] == null || results[i].trim().isEmpty() ? null : LocalDate.parse(results[i], dateFormat))
        i++

        //hs_not_attended boolean
        app.hsNotAttended = (results[i++] == 'Y')

        //cahs_graduated boolean
        app.cahsGraduated = (results[i++] == 'Y')

        //cahs_3year boolean
        app.cahs3year = (results[i++] == 'Y')

        //hs_name character varying(30)
        app.hsName = results[i++]

        //hs_city character varying(20)
        app.hsCity = results[i++]

        //hs_state character(2)
        app.hsState = results[i++]

        //hs_country character(2)
        app.hsCountry = results[i++]

        //hs_cds character(6)
        app.hsCds = results[i++]

        //hs_ceeb character(7)
        app.hsCeeb = results[i++]

        //hs_not_listed boolean
        app.hsNotListed = (results[i++] == 'Y')

        //home_schooled boolean
        app.homeSchooled = (results[i++] == 'Y')

        //college_count smallint
        app.collegeCount = results[i++].toInteger()

        //hs_attendance smallint
        app.hsAttendance = results[i++].toInteger()

        //coenroll_confirm boolean
        app.coenrollConfirm = (results[i++] == 'Y')

        //gender character(1)
        app.gender = results[i++]

        //pg_firstname character varying(50)
        app.pgFirstname = results[i++]

        //pg_lastname character varying(50)
        app.pgLastname = results[i++]

        //pg_rel character(1)
        app.pgRel = results[i++]

        //pg1_edu character(1)
        app.pg1Edu = results[i++]

        //pg2_edu character(1)
        app.pg2Edu = results[i++]

        //pg_edu_mis character(2)
        app.pgEduMis = results[i++]

        //under19_ind boolean DEFAULT false
        app.under19Ind = (results[i++] == 'Y')

        //dependent_status character(1)
        app.dependentStatus = results[i++]

        //race_ethnic text
        app.raceEthnic = results[i++]

        //hispanic boolean
        app.hispanic = (results[i++] == 'Y')

        //race_aian_other_description text
        app.raceAIANOtherDescription = results[i++]

        //race_group text
        app.raceGroup = results[i++]

        //race_ethnic_full text
        app.raceEthnicFull = results[i++]

        //ssn text
        app.ssn = results[i++]

        //birthdate date
        app.birthdate = (results[i] == null || results[i].trim().isEmpty() ? null : LocalDate.parse(results[i], dateFormat))
        i++

        //firstname character varying(50)
        app.firstname = results[i++]

        //middlename character varying(50)
        app.middlename = results[i++]

        //lastname character varying(50)
        app.lastname = results[i++]

        //suffix character varying(3)
        app.suffix = results[i++]

        //otherfirstname character varying(50)
        app.otherfirstname = results[i++]

        //othermiddlename character varying(50)
        app.othermiddlename = results[i++]

        //otherlastname character varying(50)
        app.otherlastname = results[i++]

        //citizenship_status character(1) NOT NULL
        app.citizenshipStatus = results[i++]

        //alien_reg_number character varying(20)
        app.alienRegNumber = results[i++]

        //visa_type character varying(20)
        app.visaType = results[i++]

        //no_documents boolean
        app.noDocuments = (results[i++] == 'Y')

        //alien_reg_issue_date date
        app.alienRegIssueDate = (results[i] == null || results[i].trim().isEmpty() ? null : LocalDate.parse(results[i], dateFormat))
        i++

        //alien_reg_expire_date date
        app.alienRegExpireDate = (results[i] == null || results[i].trim().isEmpty() ? null : LocalDate.parse(results[i], dateFormat))
        i++

        //alien_reg_no_expire boolean
        app.alienRegNoExpire = (results[i++] == 'Y')

        //military_status character(1) NOT NULL
        app.militaryStatus = results[i++]

        //military_discharge_date date
        app.militaryDischargeDate = (results[i] == null || results[i].trim().isEmpty() ? null : LocalDate.parse(results[i], dateFormat))
        i++

        //military_home_state character(2)
        app.militaryHomeState = results[i++]

        //military_home_country character(2)
        app.militaryHomeCountry = results[i++]

        //military_ca_stationed boolean
        app.militaryCaStationed = (results[i++] == 'Y')

        //military_legal_residence character(2)
        app.militaryLegalResidence = results[i++]

        //ca_res_2_years boolean
        app.caRes2Years = (results[i++] == 'Y')

        //ca_date_current date
        app.caDateCurrent = (results[i] == null || results[i].trim().isEmpty() ? null : LocalDate.parse(results[i], dateFormat))
        i++

        //ca_not_arrived boolean
        app.caNotArrived = (results[i++] == 'Y')

        //ca_college_employee boolean
        app.caCollegeEmployee = (results[i++] == 'Y')

        //ca_school_employee boolean
        app.caSchoolEmployee = (results[i++] == 'Y')

        //ca_seasonal_ag boolean
        app.caSeasonalAg = (results[i++] == 'Y')

        //ca_foster_youth boolean
        app.caFosterYouth = (results[i++] == 'Y')

        //ca_outside_tax boolean
        app.caOutsideTax = (results[i++] == 'Y')

        //ca_outside_tax_year date
        app.caOutsideTaxYear = (results[i] == null || results[i].trim().isEmpty() ? null : LocalDate.parse(results[i], dateFormat))
        i++

        //ca_outside_voted boolean
        app.caOutsideVoted = (results[i++] == 'Y')

        //ca_outside_voted_year date
        app.caOutsideVotedYear = (results[i] == null || results[i].trim().isEmpty() ? null : LocalDate.parse(results[i], dateFormat))
        i++

        //ca_outside_college boolean
        app.caOutsideCollege = (results[i++] == 'Y')

        //ca_outside_college_year date
        app.caOutsideCollegeYear = (results[i] == null || results[i].trim().isEmpty() ? null : LocalDate.parse(results[i], dateFormat))
        i++

        //ca_outside_lawsuit boolean
        app.caOutsideLawsuit = (results[i++] == 'Y')

        //ca_outside_lawsuit_year date
        app.caOutsideLawsuitYear = (results[i] == null || results[i].trim().isEmpty() ? null : LocalDate.parse(results[i], dateFormat))
        i++

        //res_status character(1)
        app.resStatus = results[i++]

        //res_status_change boolean
        app.resStatusChange = (results[i++] == 'Y')

        //res_prev_date date
        app.resPrevDate = (results[i] == null || results[i].trim().isEmpty() ? null : LocalDate.parse(results[i], dateFormat))
        i++

        //adm_ineligible smallint
        app.admIneligible = results[i++].toInteger()

        //elig_ab540 boolean
        app.eligAb540 = (results[i++] == 'Y')

        //res_area_a smallint
        app.resAreaA = results[i++].toInteger()

        //res_area_b smallint
        app.resAreaB = results[i++].toInteger()

        //res_area_c smallint
        app.resAreaC = results[i++].toInteger()

        //res_area_d smallint
        app.resAreaD = results[i++].toInteger()

        //experience integer
        app.experience = results[i++].toInteger()

        //recommend integer
        app.recommend = results[i++].toInteger()

        //comments text
        app.comments = results[i++]

        //comfortable_english boolean
        app.comfortableEnglish = (results[i++] == 'Y')

        //financial_assistance boolean
        app.financialAssistance = (results[i++] == 'Y')

        //tanf_ssi_ga boolean
        app.tanfSsiGa = (results[i++] == 'Y')

        //foster_youths boolean
        app.fosterYouths = (results[i++] == 'Y')

        //athletic_intercollegiate boolean
        app.athleticIntercollegiate = (results[i++] == 'Y')

        //athletic_intramural boolean
        app.athleticIntramural = (results[i++] == 'Y')

        //athletic_not_interested boolean
        app.athleticNotInterested = (results[i++] == 'Y')

        //academic_counseling boolean
        app.academicCounseling = (results[i++] == 'Y')

        //basic_skills boolean
        app.basicSkills = (results[i++] == 'Y')

        //calworks boolean
        app.calworks = (results[i++] == 'Y')

        //career_planning boolean
        app.careerPlanning = (results[i++] == 'Y')

        //child_care boolean
        app.childCare = (results[i++] == 'Y')

        //counseling_personal boolean
        app.counselingPersonal = (results[i++] == 'Y')

        //dsps boolean
        app.dsps = (results[i++] == 'Y')

        //eops boolean
        app.eops = (results[i++] == 'Y')

        //esl boolean
        app.esl = (results[i++] == 'Y')

        //health_services boolean
        app.healthServices = (results[i++] == 'Y')

        //housing_info boolean
        app.housingInfo = (results[i++] == 'Y')

        //employment_assistance boolean
        app.employmentAssistance = (results[i++] == 'Y')

        //online_classes boolean
        app.onlineClasses = (results[i++] == 'Y')

        //reentry_program boolean
        app.reentryProgram = (results[i++] == 'Y')

        //scholarship_info boolean
        app.scholarshipInfo = (results[i++] == 'Y')

        //student_government boolean
        app.studentGovernment = (results[i++] == 'Y')

        //testing_assessment boolean
        app.testingAssessment = (results[i++] == 'Y')

        //transfer_info boolean
        app.transferInfo = (results[i++] == 'Y')

        //tutoring_services boolean
        app.tutoringServices = (results[i++] == 'Y')

        //veterans_services boolean
        app.veteransServices = (results[i++] == 'Y')

        //integrity_fg_01 boolean
        app.integrityFg01 = (results[i++] == 'Y')

        //integrity_fg_02 boolean
        app.integrityFg02 = (results[i++] == 'Y')

        //integrity_fg_03 boolean
        app.integrityFg03 = (results[i++] == 'Y')

        //integrity_fg_04 boolean
        app.integrityFg04 = (results[i++] == 'Y')

        //integrity_fg_11 boolean
        app.integrityFg11 = (results[i++] == 'Y')

        //integrity_fg_47 boolean
        app.integrityFg47 = (results[i++] == 'Y')

        //integrity_fg_48 boolean
        app.integrityFg48 = (results[i++] == 'Y')

        //integrity_fg_49 boolean
        app.integrityFg49 = (results[i++] == 'Y')

        //integrity_fg_50 boolean
        app.integrityFg50 = (results[i++] == 'Y')

        //integrity_fg_51 boolean
        app.integrityFg51 = (results[i++] == 'Y')

        //integrity_fg_52 boolean
        app.integrityFg52 = (results[i++] == 'Y')

        //integrity_fg_53 boolean
        app.integrityFg53 = (results[i++] == 'Y')

        //integrity_fg_54 boolean
        app.integrityFg54 = (results[i++] == 'Y')

        //integrity_fg_55 boolean
        app.integrityFg55 = (results[i++] == 'Y')

        //integrity_fg_56 boolean
        app.integrityFg56 = (results[i++] == 'Y')

        //integrity_fg_57 boolean
        app.integrityFg57 = (results[i++] == 'Y')

        //integrity_fg_58 boolean
        app.integrityFg58 = (results[i++] == 'Y')

        //integrity_fg_59 boolean
        app.integrityFg59 = (results[i++] == 'Y')

        //integrity_fg_60 boolean
        app.integrityFg60 = (results[i++] == 'Y')

        //integrity_fg_61 boolean
        app.integrityFg61 = (results[i++] == 'Y')

        //integrity_fg_62 boolean
        app.integrityFg62 = (results[i++] == 'Y')

        //integrity_fg_63 boolean
        app.integrityFg63 = (results[i++] == 'Y')

        //integrity_fg_70 boolean
        app.integrityFg70 = (results[i++] == 'Y')

        //integrity_fg_80 boolean
        app.integrityFg80 = (results[i++] == 'Y')

        //col1_ceeb character(7)
        app.col1Ceeb = results[i++]

        //col1_cds character(6)
        app.col1Cds = results[i++]

        //col1_not_listed boolean
        app.col1NotListed = (results[i++] == 'Y')

        //col1_name character varying(30)
        app.col1Name = results[i++]

        //col1_city character varying(20)
        app.col1City = results[i++]

        //col1_state character varying(30)
        app.col1State = results[i++]

        //col1_country character(2)
        app.col1Country = results[i++]

        //col1_start_date date
        app.col1StartDate = (results[i] == null || results[i].trim().isEmpty() ? null : LocalDate.parse(results[i], dateFormat))
        i++

        //col1_end_date date
        app.col1EndDate = (results[i] == null || results[i].trim().isEmpty() ? null : LocalDate.parse(results[i], dateFormat))
        i++

        //col1_degree_date date
        app.col1DegreeDate = (results[i] == null || results[i].trim().isEmpty() ? null : LocalDate.parse(results[i], dateFormat))
        i++

        //col1_degree_obtained character(1)
        app.col1DegreeObtained = results[i++]

        //col2_ceeb character(7)
        app.col2Ceeb = results[i++]

        //col2_cds character(6)
        app.col2Cds = results[i++]

        //col2_not_listed boolean
        app.col2NotListed = (results[i++] == 'Y')

        //col2_name character varying(30)
        app.col2Name = results[i++]

        //col2_city character varying(20)
        app.col2City = results[i++]

        //col2_state character varying(30)
        app.col2State = results[i++]

        //col2_country character(2)
        app.col2Country = results[i++]

        //col2_start_date date
        app.col2StartDate = (results[i] == null || results[i].trim().isEmpty() ? null : LocalDate.parse(results[i], dateFormat))
        i++

        //col2_end_date date
        app.col2EndDate = (results[i] == null || results[i].trim().isEmpty() ? null : LocalDate.parse(results[i], dateFormat))
        i++

        //col2_degree_date date
        app.col2DegreeDate = (results[i] == null || results[i].trim().isEmpty() ? null : LocalDate.parse(results[i], dateFormat))
        i++

        //col2_degree_obtained character(1)
        app.col2DegreeObtained = results[i++]

        //col3_ceeb character(7)
        app.col3Ceeb = results[i++]

        //col3_cds character(6)
        app.col3Cds = results[i++]

        //col3_not_listed boolean
        app.col3NotListed = (results[i++] == 'Y')

        //col3_name character varying(30)
        app.col3Name = results[i++]

        //col3_city character varying(20)
        app.col3City = results[i++]

        //col3_state character varying(30)
        app.col3State = results[i++]

        //col3_country character(2)
        app.col3Country = results[i++]

        //col3_start_date date
        app.col3StartDate = (results[i] == null || results[i].trim().isEmpty() ? null : LocalDate.parse(results[i], dateFormat))
        i++

        //col3_end_date date
        app.col3EndDate = (results[i] == null || results[i].trim().isEmpty() ? null : LocalDate.parse(results[i], dateFormat))
        i++

        //col3_degree_date date
        app.col3DegreeDate = (results[i] == null || results[i].trim().isEmpty() ? null : LocalDate.parse(results[i], dateFormat))
        i++

        //col3_degree_obtained character(1)
        app.col3DegreeObtained = results[i++]

        //col4_ceeb character(7)
        app.col4Ceeb = results[i++]

        //col4_cds character(6)
        app.col4Cds = results[i++]

        //col4_not_listed boolean
        app.col4NotListed = (results[i++] == 'Y')

        //col4_name character varying(30)
        app.col4Name = results[i++]

        //col4_city character varying(20)
        app.col4City = results[i++]

        //col4_state character varying(30)
        app.col4State = results[i++]

        //col4_country character(2)
        app.col4Country = results[i++]

        //col4_start_date date
        app.col4StartDate = (results[i] == null || results[i].trim().isEmpty() ? null : LocalDate.parse(results[i], dateFormat))
        i++

        //col4_end_date date
        app.col4EndDate = (results[i] == null || results[i].trim().isEmpty() ? null : LocalDate.parse(results[i], dateFormat))
        i++

        //col4_degree_date date
        app.col4DegreeDate = (results[i] == null || results[i].trim().isEmpty() ? null : LocalDate.parse(results[i], dateFormat))
        i++

        //col4_degree_obtained character(1)
        app.col4DegreeObtained = results[i++]

        //college_name character varying(50)
        app.collegeName = results[i++]

        //district_name character varying(50)
        app.districtName = results[i++]

        //term_code character varying(15)
        app.termCode = results[i++]

        //term_description character varying(100)
        app.termDescription = results[i++]

        //major_code character varying(30)
        app.majorCode = results[i++]

        //major_description character varying(100)
        app.majorDescription = results[i++]

        //tstmp_submit timestamp with time zone
        app.tstmpSubmit = (results[i] == null || results[i].trim().isEmpty() ? null : LocalDateTime.parse(results[i], dateTimeFormat))
        i++

        //tstmp_create timestamp with time zone DEFAULT now()
        app.tstmpCreate = (results[i] == null || results[i].trim().isEmpty() ? null : LocalDateTime.parse(results[i], dateTimeFormat))
        i++

        //tstmp_update timestamp with time zone
        app.tstmpUpdate = (results[i] == null || results[i].trim().isEmpty() ? null : LocalDateTime.parse(results[i], dateTimeFormat))
        i++

        //ssn_display character varying(11)
        app.ssnDisplay = results[i++]

        //foster_youth_status character(1)
        app.fosterYouthStatus = results[i++]

        //foster_youth_preference boolean
        app.fosterYouthPreference = (results[i++] == 'Y')

        //foster_youth_mis boolean
        app.fosterYouthMis = (results[i++] == 'Y')

        //foster_youth_priority boolean
        app.fosterYouthPriority = (results[i++] == 'Y')

        //tstmp_download timestamp with time zone
        app.tstmpDownload = (results[i] == null || results[i].trim().isEmpty() ? null : LocalDateTime.parse(results[i], dateTimeFormat))
        i++

        //address_validation character(1)
        app.addressValidation = results[i++]

        //address_validation_override boolean
        app.addressValidationOverride = (results[i++] == 'Y')

        //address_validation_override_timestamp datetime
        app.addressValidationOverrideTimestamp = (results[i] == null || results[i].trim().isEmpty() ? null : LocalDateTime.parse(results[i], dateTimeFormat))
        i++

        //zip4 character(4)
        app.zip4 = results[i++]

        //perm_address_validation character(1)
        app.permAddressValidation = results[i++]

        //perm_zip4 character(4)
        app.permZip4 = results[i++]

        //discharge_type character varying(1)
        app.dischargeType = results[i++]

        //college_expelled_summary boolean
        app.collegeExpelledSummary = (results[i++] == 'Y')

        //col1_expelled_status boolean
        app.col1ExpelledStatus = (results[i++] == 'Y')

        //col2_expelled_status boolean
        app.col2ExpelledStatus = (results[i++] == 'Y')

        //col3_expelled_status boolean
        app.col3ExpelledStatus = (results[i++] == 'Y')

        //col4_expelled_status boolean
        app.col4ExpelledStatus = (results[i++] == 'Y')

        //integrity_flags character varying(255)
        app.integrityFlags = results[i++]

        //rdd date
        app.rdd = (results[i] == null || results[i].trim().isEmpty() ? null : LocalDate.parse(results[i], dateFormat))
        i++

        //ssn_type character(1)
        app.ssnType = results[i++]

        //military_stationed_ca_ed boolean
        app.militaryStationedCaEd = (results[i++] == 'Y')

        //integrity_fg_65 boolean
        app.integrityFg65 = (results[i++] == 'Y')

        //integrity_fg_64 boolean
        app.integrityFg64 = (results[i++] == 'Y')

        //ip_address character varying(15)
        app.ipAddress = results[i++]

        //campaign1 character varying(255)
        app.campaign1 = results[i++]

        //campaign2 character varying(255)
        app.campaign2 = results[i++]

        //campaign3 character varying(255)
        app.campaign3 = results[i++]

        // unencrypted version of orientation_encrypted as a String
        app.orientation = results[i++]

        // unencrypted version of transgender_encrypted as a String
        app.transgender = results[i++]

        //ssn_exception boolean DEFAULT false
        app.ssnException = (results[i++] == 'Y')

        //integrity_fg_71 boolean
        app.integrityFg71 = (results[i++] == 'Y')

        //preferred_firstname character varying(50)
        app.preferredFirstname = results[i++]

        //preferred_middlename character varying(50)
        app.preferredMiddlename = results[i++]

        //preferred_lastname character varying(50)
        app.preferredLastname = results[i++]

        //preferred_name boolean DEFAULT false
        app.preferredName = (results[i++] == 'Y')

        //ssn_no boolean
        app.ssnNo = (results[i++] == 'Y')

        //completed_eleventh_grade boolean
        app.completedEleventhGrade = (results[i++] == 'Y')

        //grade_point_average character varying(5)
        app.gradePointAverage = results[i++]

        //highest_english_course integer
        app.highestEnglishCourse = results[i++].toInteger()

        //highest_english_grade character varying(2)
        app.highestEnglishGrade = results[i++]

        //highest_math_course_taken integer
        app.highestMathCourseTaken = results[i++].toInteger()

        //highest_math_taken_grade character varying(2)
        app.highestMathTakenGrade = results[i++]

        //highest_math_course_passed integer
        app.highestMathCoursePassed = results[i++].toInteger()

        //highest_math_passed_grade character varying(2)
        app.highestMathPassedGrade = results[i++]

        //integrity_fg_30 boolean
        app.integrityFg30 = results[i++]

        //hs_cds_full character varying(14)
        app.hsCdsFull = results[i++]

        //col1_cds_full character varying(14)
        app.col1CdsFull = results[i++]

        //col2_cds_full character varying(14)
        app.col2CdsFull = results[i++]

        //col3_cds_full character varying(14)
        app.col3CdsFull = results[i++]

        //col4_cds_full character varying(14)
        app.col4CdsFull = results[i++]

        //ssid character varying(10)
        app.ssid = results[i++]

        //no_perm_address_homeless boolean DEFAULT false
        app.noPermAddressHomeless = (results[i++] == 'Y')

        //no_mailing_address_homeless boolean DEFAULT false
        app.noMailingAddressHomeless = (results[i++] == 'Y')

        //term_start date
        app.termStart = (results[i] == null || results[i].trim().isEmpty() ? null : LocalDate.parse(results[i], dateFormat))
        i++

        //term_end date
        app.termEnd = (results[i] == null || results[i].trim().isEmpty() ? null : LocalDate.parse(results[i], dateFormat))
        i++

        //homeless_youth boolean
        app.homelessYouth = (results[i++] == 'Y')

        //integrity_fg_40 boolean
        app.integrityFg40 = (results[i++] == 'Y')

        //cip_code character(6)
        app.cipCode = results[i++]

        //major_category character varying(100)
        app.majorCategory = results[i++]

        //mainphoneintl character varying(25)
        app.mainphoneintl = results[i++]

        //secondphoneintl character varying(25)
        app.secondphoneintl = results[i++]

        //non_credit boolean
        app.nonCredit = (results[i++] == 'Y')

        //integrity_fg_81 boolean
        app.integrityFg81 = (results[i++] == 'Y')

        //highest_grade_completed character varying(2)
        app.highestGradeCompleted = results[i++]

        //accepted_terms Boolean
        app.acceptedTerms = (results[i++] == 'Y')

        //accepted_terms_timestamp TIMESTAMP
        app.acceptedTermsTimestamp = (results[i] == null || results[i].trim().isEmpty() ? null : LocalDateTime.parse(results[i], dateTimeFormat))
        i++

        //mailing_address_validation_override Boolean
        app.mailingAddressValidationOverride = (results[i++] == 'Y')

        //phoneType enumerated Mobile/Landline varying (9)
        app.phoneType = results[i++]

        //ipAddressAtAccountCreation varying (15)
        app.ipAddressAtAccountCreation = results[i++]

        //ipAddressAtAppCreation varying (15)
        app.ipAddressAtAppCreation = results[i++]

        //fraud score double
        app.fraudScore = results[i++].toDouble()

        //fraud score status integer
        app.fraudStatus = results[i++].toInteger()

        //student_parent boolean
        app.studentParent = (results[i++] == 'Y')

        //idme_confirmation_timestamp timestamp
        app.idmeConfirmationTimestamp = (results[i] == null || results[i].trim().isEmpty() ? null : LocalDateTime.parse(results[i], dateTimeFormat))
        i++

        //idme_optin_timestamp timestamp
        app.idmeOptinTimestamp = (results[i] == null || results[i].trim().isEmpty() ? null : LocalDateTime.parse(results[i], dateTimeFormat))
        i++

        app.idmeWorkflowStatus = results[i++]

        app.studentDepsUnder18 = results[i++].toInteger()

        app.studentDeps18Over = results[i++].toInteger()

        app.sisProcessedFlag = results[i++]

        app.tstmpSISProcessed = (results[i] == null || results[i].trim().isEmpty() ? null : LocalDateTime.parse(results[i], dateTimeFormat))
        i++
        app.sisProcessedNotes = results[i]

        return app
    }

    /**
     * Converts a string array into a Standard Application's Supplement Questions Object
     * Note: Assumes that the order of the string array is the same order as the fields in the Supplemental Questions Object
     * @param results The string array to convert to an Supplemental Questions Object
     * @retuns a Supplemental Questions object with each field populated from the string array
     */
    protected SupplementalQuestions ConvertStringArrayToSupplementalQuestion(String[] results) {
        if(results.length < 161) {
            return null
        }

        DateTimeFormatter dateFormat = DateTimeFormatter.ofPattern("yyyy-MM-dd")
        DateTimeFormatter dateTimeFormat = DateTimeFormatter.ofPattern("yyyy-MM-dd-HH.mm.ss.SSSSSS")

        Integer i = 0
        SupplementalQuestions ques = new SupplementalQuestions()

        //app_id bigint NOT NULL
        ques.appId = results[i++].toBigInteger()

        //supp_text_01 character varying(250)
        ques.suppText01 = results[i++]

        //supp_text_02 character varying(250)
        ques.suppText02 = results[i++]

        //supp_text_03 character varying(250)
        ques.suppText03 = results[i++]

        //supp_text_04 character varying(250)
        ques.suppText04 = results[i++]

        //supp_text_05 character varying(250)
        ques.suppText05 = results[i++]

        //supp_text_06 character varying(250)
        ques.suppText06 = results[i++]

        //supp_text_07 character varying(250)
        ques.suppText07 = results[i++]

        //supp_text_08 character varying(250)
        ques.suppText08 = results[i++]

        //supp_text_09 character varying(250)
        ques.suppText09 = results[i++]

        //supp_text_10 character varying(250)
        ques.suppText10 = results[i++]

        //supp_text_11 character varying(250)
        ques.suppText11 = results[i++]

        //supp_text_12 character varying(250)
        ques.suppText12 = results[i++]

        //supp_text_13 character varying(250)
        ques.suppText13 = results[i++]

        //supp_text_14 character varying(250)
        ques.suppText14 = results[i++]

        //supp_text_15 character varying(250)
        ques.suppText15 = results[i++]

        //supp_text_16 character varying(250)
        ques.suppText16 = results[i++]

        //supp_text_17 character varying(250)
        ques.suppText17 = results[i++]

        //supp_text_18 character varying(250)
        ques.suppText18 = results[i++]

        //supp_text_19 character varying(250)
        ques.suppText19 = results[i++]

        //supp_text_20 character varying(250)
        ques.suppText20 = results[i++]

        //supp_check_01 boolean
        ques.suppCheck01 = (results[i++] == 'Y')

        //supp_check_02 boolean
        ques.suppCheck02 = (results[i++] == 'Y')

        //supp_check_03 boolean
        ques.suppCheck03 = (results[i++] == 'Y')

        //supp_check_04 boolean
        ques.suppCheck04 = (results[i++] == 'Y')

        //supp_check_05 boolean
        ques.suppCheck05 = (results[i++] == 'Y')

        //supp_check_06 boolean
        ques.suppCheck06 = (results[i++] == 'Y')

        //supp_check_07 boolean
        ques.suppCheck07 = (results[i++] == 'Y')

        //supp_check_08 boolean
        ques.suppCheck08 = (results[i++] == 'Y')

        //supp_check_09 boolean
        ques.suppCheck09 = (results[i++] == 'Y')

        //supp_check_10 boolean
        ques.suppCheck10 = (results[i++] == 'Y')

        //supp_check_11 boolean
        ques.suppCheck11 = (results[i++] == 'Y')

        //supp_check_12 boolean
        ques.suppCheck12 = (results[i++] == 'Y')

        //supp_check_13 boolean
        ques.suppCheck13 = (results[i++] == 'Y')

        //supp_check_14 boolean
        ques.suppCheck14 = (results[i++] == 'Y')

        //supp_check_15 boolean
        ques.suppCheck15 = (results[i++] == 'Y')

        //supp_check_16 boolean
        ques.suppCheck16 = (results[i++] == 'Y')

        //supp_check_17 boolean
        ques.suppCheck17 = (results[i++] == 'Y')

        //supp_check_18 boolean
        ques.suppCheck18 = (results[i++] == 'Y')

        //supp_check_19 boolean
        ques.suppCheck19 = (results[i++] == 'Y')

        //supp_check_20 boolean
        ques.suppCheck20 = (results[i++] == 'Y')

        //supp_check_21 boolean
        ques.suppCheck21 = (results[i++] == 'Y')

        //supp_check_22 boolean
        ques.suppCheck22 = (results[i++] == 'Y')

        //supp_check_23 boolean
        ques.suppCheck23 = (results[i++] == 'Y')

        //supp_check_24 boolean
        ques.suppCheck24 = (results[i++] == 'Y')

        //supp_check_25 boolean
        ques.suppCheck25 = (results[i++] == 'Y')

        //supp_check_26 boolean
        ques.suppCheck26 = (results[i++] == 'Y')

        //supp_check_27 boolean
        ques.suppCheck27 = (results[i++] == 'Y')

        //supp_check_28 boolean
        ques.suppCheck28 = (results[i++] == 'Y')

        //supp_check_29 boolean
        ques.suppCheck29 = (results[i++] == 'Y')

        //supp_check_30 boolean
        ques.suppCheck30 = (results[i++] == 'Y')

        //supp_check_31 boolean
        ques.suppCheck31 = (results[i++] == 'Y')

        //supp_check_32 boolean
        ques.suppCheck32 = (results[i++] == 'Y')

        //supp_check_33 boolean
        ques.suppCheck33 = (results[i++] == 'Y')

        //supp_check_34 boolean
        ques.suppCheck34 = (results[i++] == 'Y')

        //supp_check_35 boolean
        ques.suppCheck35 = (results[i++] == 'Y')

        //supp_check_36 boolean
        ques.suppCheck36 = (results[i++] == 'Y')

        //supp_check_37 boolean
        ques.suppCheck37 = (results[i++] == 'Y')

        //supp_check_38 boolean
        ques.suppCheck38 = (results[i++] == 'Y')

        //supp_check_39 boolean
        ques.suppCheck39 = (results[i++] == 'Y')

        //supp_check_40 boolean
        ques.suppCheck40 = (results[i++] == 'Y')

        //supp_check_41 boolean
        ques.suppCheck41 = (results[i++] == 'Y')

        //supp_check_42 boolean
        ques.suppCheck42 = (results[i++] == 'Y')

        //supp_check_43 boolean
        ques.suppCheck43 = (results[i++] == 'Y')

        //supp_check_44 boolean
        ques.suppCheck44 = (results[i++] == 'Y')

        //supp_check_45 boolean
        ques.suppCheck45 = (results[i++] == 'Y')

        //supp_check_46 boolean
        ques.suppCheck46 = (results[i++] == 'Y')

        //supp_check_47 boolean
        ques.suppCheck47 = (results[i++] == 'Y')

        //supp_check_48 boolean
        ques.suppCheck48 = (results[i++] == 'Y')

        //supp_check_49 boolean
        ques.suppCheck49 = (results[i++] == 'Y')

        //supp_check_50 boolean
        ques.suppCheck50 = (results[i++] == 'Y')

        //supp_yesno_01 boolean
        ques.suppYesno01 = (results[i++] == 'Y')

        //supp_yesno_02 boolean
        ques.suppYesno02 = (results[i++] == 'Y')

        //supp_yesno_03 boolean
        ques.suppYesno03 = (results[i++] == 'Y')

        //supp_yesno_04 boolean
        ques.suppYesno04 = (results[i++] == 'Y')

        //supp_yesno_05 boolean
        ques.suppYesno05 = (results[i++] == 'Y')

        //supp_yesno_06 boolean
        ques.suppYesno06 = (results[i++] == 'Y')

        //supp_yesno_07 boolean
        ques.suppYesno07 = (results[i++] == 'Y')

        //supp_yesno_08 boolean
        ques.suppYesno08 = (results[i++] == 'Y')

        //supp_yesno_09 boolean
        ques.suppYesno09 = (results[i++] == 'Y')

        //supp_yesno_10 boolean
        ques.suppYesno10 = (results[i++] == 'Y')

        //supp_yesno_11 boolean
        ques.suppYesno11 = (results[i++] == 'Y')

        //supp_yesno_12 boolean
        ques.suppYesno12 = (results[i++] == 'Y')

        //supp_yesno_13 boolean
        ques.suppYesno13 = (results[i++] == 'Y')

        //supp_yesno_14 boolean
        ques.suppYesno14 = (results[i++] == 'Y')

        //supp_yesno_15 boolean
        ques.suppYesno15 = (results[i++] == 'Y')

        //supp_yesno_16 boolean
        ques.suppYesno16 = (results[i++] == 'Y')

        //supp_yesno_17 boolean
        ques.suppYesno17 = (results[i++] == 'Y')

        //supp_yesno_18 boolean
        ques.suppYesno18 = (results[i++] == 'Y')

        //supp_yesno_19 boolean
        ques.suppYesno19 = (results[i++] == 'Y')

        //supp_yesno_20 boolean
        ques.suppYesno20 = (results[i++] == 'Y')

        //supp_yesno_21 boolean
        ques.suppYesno21 = (results[i++] == 'Y')

        //supp_yesno_22 boolean
        ques.suppYesno22 = (results[i++] == 'Y')

        //supp_yesno_23 boolean
        ques.suppYesno23 = (results[i++] == 'Y')

        //supp_yesno_24 boolean
        ques.suppYesno24 = (results[i++] == 'Y')

        //supp_yesno_25 boolean
        ques.suppYesno25 = (results[i++] == 'Y')

        //supp_yesno_26 boolean
        ques.suppYesno26 = (results[i++] == 'Y')

        //supp_yesno_27 boolean
        ques.suppYesno27 = (results[i++] == 'Y')

        //supp_yesno_28 boolean
        ques.suppYesno28 = (results[i++] == 'Y')

        //supp_yesno_29 boolean
        ques.suppYesno29 = (results[i++] == 'Y')

        //supp_yesno_30 boolean
        ques.suppYesno30 = (results[i++] == 'Y')

        //supp_menu_01 character varying(60)
        ques.suppMenu01 = results[i++]

        //supp_menu_02 character varying(60)
        ques.suppMenu02 = results[i++]

        //supp_menu_03 character varying(60)
        ques.suppMenu03 = results[i++]

        //supp_menu_04 character varying(60)
        ques.suppMenu04 = results[i++]

        //supp_menu_05 character varying(60)
        ques.suppMenu05 = results[i++]

        //supp_menu_06 character varying(60)
        ques.suppMenu06 = results[i++]

        //supp_menu_07 character varying(60)
        ques.suppMenu07 = results[i++]

        //supp_menu_08 character varying(60)
        ques.suppMenu08 = results[i++]

        //supp_menu_09 character varying(60)
        ques.suppMenu09 = results[i++]

        //supp_menu_10 character varying(60)
        ques.suppMenu10 = results[i++]

        //supp_menu_11 character varying(60)
        ques.suppMenu11 = results[i++]

        //supp_menu_12 character varying(60)
        ques.suppMenu12 = results[i++]

        //supp_menu_13 character varying(60)
        ques.suppMenu13 = results[i++]

        //supp_menu_14 character varying(60)
        ques.suppMenu14 = results[i++]

        //supp_menu_15 character varying(60)
        ques.suppMenu15 = results[i++]

        //supp_menu_16 character varying(60)
        ques.suppMenu16 = results[i++]

        //supp_menu_17 character varying(60)
        ques.suppMenu17 = results[i++]

        //supp_menu_18 character varying(60)
        ques.suppMenu18 = results[i++]

        //supp_menu_19 character varying(60)
        ques.suppMenu19 = results[i++]

        //supp_menu_20 character varying(60)
        ques.suppMenu20 = results[i++]

        //supp_menu_21 character varying(60)
        ques.suppMenu21 = results[i++]

        //supp_menu_22 character varying(60)
        ques.suppMenu22 = results[i++]

        //supp_menu_23 character varying(60)
        ques.suppMenu23 = results[i++]

        //supp_menu_24 character varying(60)
        ques.suppMenu24 = results[i++]

        //supp_menu_25 character varying(60)
        ques.suppMenu25 = results[i++]

        //supp_menu_26 character varying(60)
        ques.suppMenu26 = results[i++]

        //supp_menu_27 character varying(60)
        ques.suppMenu27 = results[i++]

        //supp_menu_28 character varying(60)
        ques.suppMenu28 = results[i++]

        //supp_menu_29 character varying(60)
        ques.suppMenu29 = results[i++]

        //supp_menu_30 character varying(60)
        ques.suppMenu30 = results[i++]

        //supp_date_01 date
        ques.suppDate01 = (results[i] == null || results[i].trim().isEmpty() ? null : LocalDate.parse(results[i], dateFormat))
        i++

        //supp_date_02 date
        ques.suppDate02 = (results[i] == null || results[i].trim().isEmpty() ? null : LocalDate.parse(results[i], dateFormat))
        i++

        //supp_date_03 date
        ques.suppDate03 = (results[i] == null || results[i].trim().isEmpty() ? null : LocalDate.parse(results[i], dateFormat))
        i++

        //supp_date_04 date
        ques.suppDate04 = (results[i] == null || results[i].trim().isEmpty() ? null : LocalDate.parse(results[i], dateFormat))
        i++

        //supp_date_05 date
        ques.suppDate05 = (results[i] == null || results[i].trim().isEmpty() ? null : LocalDate.parse(results[i], dateFormat))
        i++

        //supp_state_01 character(2)
        ques.suppState01 = results[i++]

        //supp_state_02 character(2)
        ques.suppState02 = results[i++]

        //supp_state_03 character(2)
        ques.suppState03 = results[i++]

        //supp_state_04 character(2)
        ques.suppState04 = results[i++]

        //supp_state_05 character(2)
        ques.suppState05 = results[i++]

        //supp_country_01 character(2)
        ques.suppCountry01 = results[i++]

        //supp_country_02 character(2)
        ques.suppCountry02 = results[i++]

        //supp_country_03 character(2)
        ques.suppCountry03 = results[i++]

        //supp_country_04 character(2)
        ques.suppCountry04 = results[i++]

        //supp_country_05 character(2)
        ques.suppCountry05 = results[i++]

        //supp_phonenumber_01 character varying(25)
        ques.suppPhonenumber01 = results[i++]

        //supp_phonenumber_02 character varying(25)
        ques.suppPhonenumber02 = results[i++]

        //supp_phonenumber_03 character varying(25)
        ques.suppPhonenumber03 = results[i++]

        //supp_phonenumber_04 character varying(25)
        ques.suppPhonenumber04 = results[i++]

        //supp_phonenumber_05 character varying(25)
        ques.suppPhonenumber05 = results[i++]

        //supp_secret_01 text
        ques.suppSecret01 = results[i++]

        //supp_secret_02 text
        ques.suppSecret02 = results[i++]

        //supp_secret_03 text
        ques.suppSecret03 = results[i++]

        //supp_secret_04 text
        ques.suppSecret04 = results[i++]

        //supp_secret_05 text
        ques.suppSecret05 = results[i++]

        ques.sisProcessedFlag = results[i++]

        ques.tstmpSISProcessed = (results[i] == null || results[i].trim().isEmpty() ? null : LocalDateTime.parse(results[i], dateTimeFormat))
        i++
        ques.sisProcessedNotes = results[i]

        return ques
    }
}
