package api.peoplesoft

import com.ccctc.adaptor.exception.EntityConflictException
import com.ccctc.adaptor.exception.EntityNotFoundException
import com.ccctc.adaptor.exception.InternalServerException
import com.ccctc.adaptor.exception.InvalidRequestException
import com.ccctc.adaptor.util.MisEnvironment
import org.apache.commons.lang.StringUtils
import org.slf4j.Logger
import org.slf4j.LoggerFactory
import org.springframework.core.env.Environment
import com.ccctc.adaptor.model.apply.CCPromiseGrant as CCPromiseGrantAppl

import java.time.LocalDate
import java.time.LocalDateTime
import java.time.format.DateTimeFormatter

/**
 * <h1>CCPromise Grant Application Peoplesoft Interface</h1>
 * <summary>
 *     <p>Provides a Wrapper around the native peoplesoft java API for:</p>
 *     <ol>
 *         <li>Storing and populating CCPromise Grant Applications (formerly known as BOG Fee Waiver) from CCPromise
 *             into a custom peoplesoft staging table</li>
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
class CCPromiseGrant {

    protected final static Logger log = LoggerFactory.getLogger(CCPromiseGrant.class)

    //****** Populated by injection in Groovy Service Implementation ****
    Environment environment

    /**
     * Default Constructor needed since we also specify an alternative Constructor
     */
    CCPromiseGrant(){
        // do nothing, except be available
    }

    /**
     * Alternative Constructor to set the environment during initialization.
     * Useful for when other groovy PS APIs need to use these Functions that depend on environment
     */
    CCPromiseGrant(Environment e) {
        this.environment = e
    }

    /**
     * Gets Standard Application Data from a Peoplesoft staging table using the CCTC_StandardApplication:getApplication
     * peoplesoft method as well as the CCTC_StandardApplication:getApplicationSupplementalQuestions method
     * @param misCode The College Code used for grabbing college specific settings from the environment
     * @param appId The application Id used when originally populating the data into the staging table
     */
    CCPromiseGrantAppl get(String misCode, Long appId) {

        log.debug("get: getting ccpromise grant application data")

        //****** Validate parameters ****
        if (!appId) {
            throw new InvalidRequestException(InvalidRequestException.Errors.invalidRequest, "Application Id cannot be null or blank")
        }
        if (!misCode) {
            throw new InvalidRequestException(InvalidRequestException.Errors.invalidSearchCriteria, "misCode cannot be null or blank")
        }

        log.debug("get: params ok; building method params")

        //****** Build parameters to send to our custom Peopletools API ****
        String packageName = "CCTC_CCPG_PKG"
        String className = "CCTC_PromiseGrantApplication"
        String methodName = "getApplication"

        String[] args = [appId]

        CCPromiseGrantAppl application = new CCPromiseGrantAppl()

        log.debug("get: attempting to get a new peoplesoft Session")
        //****** Create Connection to Peoplesoft and call remote method ******
        def peoplesoftSession = api.peoplesoft.PSConnection.getSession(environment, misCode)
        try {
            log.debug("get: calling the remote peoplesoft method")

            String[] appData = api.peoplesoft.PSConnection.callMethod(peoplesoftSession, packageName, className, methodName, "<", args)
            if(appData == null || appData.length <= 0) {
                throw new EntityNotFoundException("No Promise Grant Application found for given appId")
            }

            application = this.ConvertStringArrayToApplication(appData)
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
     * Gets CCPromise Grant Application Data from a Peoplesoft staging table using the CCTC_CCPromise:setApplication
     * peoplesoft method
     * @param misCode The College Code used for grabbing college specific settings from the environment
     * @param application The application data to set into the staging table
     */
    CCPromiseGrantAppl post(String misCode, CCPromiseGrantAppl application) {
        log.debug("post: setting ccpromise grant application data")

        //****** Validate parameters ****
        if (application == null) {
            throw new InvalidRequestException(InvalidRequestException.Errors.invalidRequest, "CCPromise Grant Application data cannot be null")
        }
        if (!application.appId) {
            throw new InvalidRequestException(InvalidRequestException.Errors.invalidRequest, "Application Id cannot be null or blank")
        }
        if (!misCode) {
            throw new InvalidRequestException(InvalidRequestException.Errors.invalidSearchCriteria, "misCode cannot be null or blank")
        }

        log.debug("post: params ok; building method params")
        //****** Build parameters to send to our custom Peopletools API ****
        String packageName = "CCTC_CCPG_PKG"
        String className = "CCTC_PromiseGrantApplication"
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

            packageName = "CCTC_CCPG_PKG"
            className = "CCTC_PromiseGrantApplication"
            methodName = "setApplication"

            args = this.ConvertApplicationToArgsArray(application)

            log.debug("post: calling the remote peoplesoft method to set the application")
            String[] rowsAffected = api.peoplesoft.PSConnection.callMethod(peoplesoftSession, packageName, className, methodName, "<", args)
            if(rowsAffected.length != 1) {
                throw new InternalServerException(InternalServerException.Errors.sisQueryError, "invalid number of rows affected. empty array")
            }
            if(!StringUtils.equalsIgnoreCase(rowsAffected[0], "1")) {
                throw new InternalServerException(InternalServerException.Errors.sisQueryError, "invalid number of rows affected [" + rowsAffected[0] + "]")
            }

            String dataEventsEnabled = MisEnvironment.getProperty(environment, misCode, "peoplesoft.dataEvents.enabled")
            if(StringUtils.equalsIgnoreCase(dataEventsEnabled, "true")) {
                try {
                    log.debug("post: triggering the remote peoplesoft event onPromiseGrantAppInserted")

                    args = [PSParameter.ConvertLongToCleanString(application.appId)]
                    api.peoplesoft.PSConnection.triggerEvent(peoplesoftSession, "onPromiseGrantAppInserted", "<", args)
                }
                catch (Exception exc) {
                    log.warn("post: Triggering the onPromiseGrantAppInserted event failed with message [" + exc.getMessage() + "]")
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
                        log.error("post: setting the record: [" + it + "]")
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
        return application
    }

    /**
     * Converts a CCPromise Grant Application Object into a string array
     * @param app The application to convert to a string array
     * @retuns a string array containing the string value of each field in the standard application object
     */
    protected String[] ConvertApplicationToArgsArray(CCPromiseGrantAppl app) {
        DateTimeFormatter dateFormat = DateTimeFormatter.ofPattern("yyyy-MM-dd")
        DateTimeFormatter dateTimeFormat = DateTimeFormatter.ofPattern("yyyy-MM-dd-HH.mm.ss.SSSSSS")
        String[] map = [
                PSParameter.ConvertLongToCleanString(app.appId),
                PSParameter.ConvertStringToCleanString(app.cccId),
                PSParameter.ConvertStringToCleanString(app.confirmationNumber),
                PSParameter.ConvertStringToCleanString(app.status),
                PSParameter.ConvertStringToCleanString(app.appLang),
                PSParameter.ConvertStringToCleanString(app.collegeId),
                PSParameter.ConvertLongToCleanString(app.yearCode),
                PSParameter.ConvertStringToCleanString(app.yearDescription),
                PSParameter.ConvertBoolToCleanString(app.determinedResidentca),
                PSParameter.ConvertBoolToCleanString(app.determinedAB540Eligible),
                PSParameter.ConvertBoolToCleanString(app.determinedNonResExempt),
                PSParameter.ConvertStringToCleanString(app.lastname),
                PSParameter.ConvertStringToCleanString(app.firstname),
                PSParameter.ConvertStringToCleanString(app.middlename),
                PSParameter.ConvertStringToCleanString(app.mainphone),
                PSParameter.ConvertStringToCleanString(app.mainphoneExt),
                PSParameter.ConvertBoolToCleanString(app.mainphoneAuthText),
                PSParameter.ConvertBoolToCleanString(app.mainphoneVerified),
                PSParameter.ConvertDateTimeToCleanString(app.mainphoneVerifiedTimestamp, dateTimeFormat),

                PSParameter.ConvertStringToCleanString(app.email),
                PSParameter.ConvertBoolToCleanString(app.emailVerified),
                PSParameter.ConvertDateTimeToCleanString(app.emailVerifiedTimestamp, dateTimeFormat),
                PSParameter.ConvertStringToCleanString(app.preferredMethodOfContact),

                PSParameter.ConvertBoolToCleanString(app.nonUsAddress),
                PSParameter.ConvertBoolToCleanString(app.addressValidationOverride),
                PSParameter.ConvertDateTimeToCleanString(app.addressValidationOverrideTimestamp, dateTimeFormat),
                PSParameter.ConvertStringToCleanString(app.streetaddress1),
                PSParameter.ConvertStringToCleanString(app.streetaddress2),
                PSParameter.ConvertStringToCleanString(app.city),
                PSParameter.ConvertStringToCleanString(app.state),
                PSParameter.ConvertStringToCleanString(app.province),
                PSParameter.ConvertStringToCleanString(app.country),
                PSParameter.ConvertStringToCleanString(app.postalcode),
                PSParameter.ConvertStringToCleanString(app.ssn),
                PSParameter.ConvertStringToCleanString(app.ssnType),
                PSParameter.ConvertStringToCleanString(app.studentCollegeId),
                PSParameter.ConvertDateToCleanString(app.birthdate, dateFormat),
                PSParameter.ConvertStringToCleanString(app.maritalStatus),
                PSParameter.ConvertBoolToCleanString(app.regDomPartner),
                PSParameter.ConvertBoolToCleanString(app.bornBefore23Year),
                PSParameter.ConvertBoolToCleanString(app.marriedOrRdp),
                PSParameter.ConvertBoolToCleanString(app.usVeteran),
                PSParameter.ConvertBoolToCleanString(app.dependents),
                PSParameter.ConvertBoolToCleanString(app.parentsDeceased),
                PSParameter.ConvertBoolToCleanString(app.emancipatedMinor),
                PSParameter.ConvertBoolToCleanString(app.legalGuardianship),
                PSParameter.ConvertBoolToCleanString(app.homelessYouthSchool),
                PSParameter.ConvertBoolToCleanString(app.homelessYouthHud),
                PSParameter.ConvertBoolToCleanString(app.homelessYouthOther),
                PSParameter.ConvertStringToCleanString(app.dependentOnParentTaxes),
                PSParameter.ConvertBoolToCleanString(app.livingWithParents),
                PSParameter.ConvertStringToCleanString(app.dependencyStatus),
                PSParameter.ConvertBoolToCleanString(app.certVeteranAffairs),
                PSParameter.ConvertBoolToCleanString(app.certNationalGuard),
                PSParameter.ConvertBoolToCleanString(app.eligMedalHonor),
                PSParameter.ConvertBoolToCleanString(app.eligSept11),
                PSParameter.ConvertBoolToCleanString(app.eligPoliceFire),
                PSParameter.ConvertBoolToCleanString(app.tanfCalworks),
                PSParameter.ConvertBoolToCleanString(app.ssiSsp),
                PSParameter.ConvertBoolToCleanString(app.generalAssistance),
                PSParameter.ConvertBoolToCleanString(app.parentsAssistance),
                PSParameter.ConvertIntegerToCleanString(app.depNumberHousehold),
                PSParameter.ConvertIntegerToCleanString(app.indNumberHousehold),
                PSParameter.ConvertIntegerToCleanString(app.depGrossIncome),
                PSParameter.ConvertIntegerToCleanString(app.indGrossIncome),
                PSParameter.ConvertIntegerToCleanString(app.depOtherIncome),
                PSParameter.ConvertIntegerToCleanString(app.indOtherIncome),
                PSParameter.ConvertIntegerToCleanString(app.depTotalIncome),
                PSParameter.ConvertIntegerToCleanString(app.indTotalIncome),
                PSParameter.ConvertBoolToCleanString(app.eligMethodA),
                PSParameter.ConvertBoolToCleanString(app.eligMethodB),
                PSParameter.ConvertStringToCleanString(app.eligBogfw),
                PSParameter.ConvertBoolToCleanString(app.confirmationParentGuardian),
                PSParameter.ConvertStringToCleanString(app.parentGuardianName),
                PSParameter.ConvertBoolToCleanString(app.ackFinAid),
                PSParameter.ConvertBoolToCleanString(app.confirmationApplicant),
                PSParameter.ConvertStringToCleanString(app.lastPage),
                PSParameter.ConvertStringToCleanString(app.ssnLast4),
                PSParameter.ConvertDateTimeToCleanString(app.tstmpSubmit, dateTimeFormat),
                PSParameter.ConvertDateTimeToCleanString(app.tstmpCreate, dateTimeFormat),
                PSParameter.ConvertDateTimeToCleanString(app.tstmpUpdate, dateTimeFormat),
                PSParameter.ConvertDateTimeToCleanString(app.tstmpDownload, dateTimeFormat),
                PSParameter.ConvertStringToCleanString(app.termCode),
                PSParameter.ConvertStringToCleanString(app.ipAddress),
                PSParameter.ConvertStringToCleanString(app.campaign1),
                PSParameter.ConvertStringToCleanString(app.campaign2),
                PSParameter.ConvertStringToCleanString(app.campaign3),
                PSParameter.ConvertBoolToCleanString(app.ssnException),
                PSParameter.ConvertStringToCleanString(app.collegeName),
                PSParameter.ConvertStringToCleanString(app.preferredFirstname),
                PSParameter.ConvertStringToCleanString(app.preferredMiddlename),
                PSParameter.ConvertStringToCleanString(app.preferredLastname),
                PSParameter.ConvertBoolToCleanString(app.preferredName),
                PSParameter.ConvertBoolToCleanString(app.ssnNo),
                PSParameter.ConvertBoolToCleanString(app.noPermAddressHomeless),
                PSParameter.ConvertBoolToCleanString(app.noMailingAddressHomeless),
                PSParameter.ConvertBoolToCleanString(app.determinedHomeless),
                PSParameter.ConvertBoolToCleanString(app.eligMethodD),
                PSParameter.ConvertStringToCleanString(app.mainphoneintl),
                PSParameter.ConvertBoolToCleanString(app.eligExoneratedCrime),
                PSParameter.ConvertBoolToCleanString(app.eligCovidDeath),

                PSParameter.ConvertBoolToCleanString(app.acceptedTerms),
                PSParameter.ConvertDateTimeToCleanString(app.acceptedTermsTimestamp, dateTimeFormat),

                PSParameter.ConvertStringToCleanString(app.phoneType),

                PSParameter.ConvertBoolToCleanString(app.mailingAddressValidationOverride),

                PSParameter.ConvertStringToCleanString(app.ipAddressAtAccountCreation),
                PSParameter.ConvertStringToCleanString(app.ipAddressAtAppCreation),

                PSParameter.ConvertBoolToCleanString(app.studentParent),

                PSParameter.ConvertDateTimeToCleanString(app.idmeConfirmationTimestamp, dateTimeFormat),
                PSParameter.ConvertDateTimeToCleanString(app.idmeOptinTimestamp, dateTimeFormat),
                PSParameter.ConvertStringToCleanString(app.idmeWorkflowStatus),

                PSParameter.ConvertIntegerToCleanString(app.studentDepsUnder18),
                PSParameter.ConvertIntegerToCleanString(app.studentDeps18Over)

        ]
        return map
    }

    /**
     * Converts a string array into an CCPromiseGrantAppl Object
     * @param results The string array to convert to an Application Object
     * @retuns a Application object with each field populated from the string array
     */
    protected CCPromiseGrantAppl ConvertStringArrayToApplication(String[] results) {
        if(results.length < 114) {
            return null
        }

        DateTimeFormatter dateFormat = DateTimeFormatter.ofPattern("yyyy-MM-dd")
        DateTimeFormatter dateTimeFormat = DateTimeFormatter.ofPattern("yyyy-MM-dd-HH.mm.ss.SSSSSS")

        Integer i = 0
        CCPromiseGrantAppl app = new CCPromiseGrantAppl()

        app.appId = results[i++].toLong()
        app.cccId = results[i++]
        app.confirmationNumber = results[i++]
        app.status = results[i++]
        app.appLang = results[i++]
        app.collegeId = results[i++]
        app.yearCode = results[i++].toLong()
        app.yearDescription = results[i++]
        app.determinedResidentca = (results[i++] == 'Y')
        app.determinedAB540Eligible = (results[i++] == 'Y')
        app.determinedNonResExempt = (results[i++] == 'Y')
        app.lastname = results[i++]
        app.firstname = results[i++]
        app.middlename = results[i++]
        app.mainphone = results[i++]
        app.mainphoneExt = results[i++]
        app.mainphoneAuthText = (results[i++] == 'Y')
        app.mainphoneVerified = (results[i++] == 'Y')
        app.mainphoneVerifiedTimestamp = (results[i] == null || results[i].trim().isEmpty() ? null : LocalDateTime.parse(results[i], dateTimeFormat))
        i++

        app.email = results[i++]
        app.emailVerified = (results[i++] == 'Y')
        app.emailVerifiedTimestamp = (results[i] == null || results[i].trim().isEmpty() ? null : LocalDateTime.parse(results[i], dateTimeFormat))
        i++
        app.preferredMethodOfContact = results[i++]

        app.nonUsAddress = (results[i++] == 'Y')
        app.addressValidationOverride = (results[i++] == 'Y')
        app.addressValidationOverrideTimestamp = (results[i] == null || results[i].trim().isEmpty() ? null : LocalDateTime.parse(results[i], dateTimeFormat))
        i++
        app.streetaddress1 = results[i++]
        app.streetaddress2 = results[i++]
        app.city = results[i++]
        app.state = results[i++]
        app.province = results[i++]
        app.country = results[i++]
        app.postalcode = results[i++]
        app.ssn = results[i++]
        app.ssnType = results[i++]
        app.studentCollegeId = results[i++]
        app.birthdate = (results[i] == null || results[i].trim().isEmpty() ? null : LocalDate.parse(results[i], dateFormat))
        i++
        app.maritalStatus = results[i++]
        app.regDomPartner = (results[i++] == 'Y')
        app.bornBefore23Year = (results[i++] == 'Y')
        app.marriedOrRdp = (results[i++] == 'Y')
        app.usVeteran = (results[i++] == 'Y')
        app.dependents = (results[i++] == 'Y')
        app.parentsDeceased = (results[i++] == 'Y')
        app.emancipatedMinor = (results[i++] == 'Y')
        app.legalGuardianship = (results[i++] == 'Y')
        app.homelessYouthSchool = (results[i++] == 'Y')
        app.homelessYouthHud = (results[i++] == 'Y')
        app.homelessYouthOther = (results[i++] == 'Y')
        app.dependentOnParentTaxes = results[i++]
        app.livingWithParents = (results[i++] == 'Y')
        app.dependencyStatus = results[i++]
        app.certVeteranAffairs = (results[i++] == 'Y')
        app.certNationalGuard = (results[i++] == 'Y')
        app.eligMedalHonor = (results[i++] == 'Y')
        app.eligSept11 = (results[i++] == 'Y')
        app.eligPoliceFire = (results[i++] == 'Y')
        app.tanfCalworks = (results[i++] == 'Y')
        app.ssiSsp = (results[i++] == 'Y')
        app.generalAssistance = (results[i++] == 'Y')
        app.parentsAssistance = (results[i++] == 'Y')
        app.depNumberHousehold = results[i++].toInteger()
        app.indNumberHousehold = results[i++].toInteger()
        app.depGrossIncome = results[i++].toInteger()
        app.indGrossIncome = results[i++].toInteger()
        app.depOtherIncome = results[i++].toInteger()
        app.indOtherIncome = results[i++].toInteger()
        app.depTotalIncome = results[i++].toInteger()
        app.indTotalIncome = results[i++].toInteger()
        app.eligMethodA = (results[i++] == 'Y')
        app.eligMethodB = (results[i++] == 'Y')
        app.eligBogfw = results[i++]
        app.confirmationParentGuardian = (results[i++] == 'Y')
        app.parentGuardianName = results[i++]
        app.ackFinAid = (results[i++] == 'Y')
        app.confirmationApplicant = (results[i++] == 'Y')
        app.lastPage = results[i++]
        app.ssnLast4 = results[i++]
        app.tstmpSubmit = (results[i] == null || results[i].trim().isEmpty() ? null : LocalDateTime.parse(results[i], dateTimeFormat))
        i++
        app.tstmpCreate = (results[i] == null || results[i].trim().isEmpty() ? null : LocalDateTime.parse(results[i], dateTimeFormat))
        i++
        app.tstmpUpdate = (results[i] == null || results[i].trim().isEmpty() ? null : LocalDateTime.parse(results[i], dateTimeFormat))
        i++
        app.tstmpDownload = (results[i] == null || results[i].trim().isEmpty() ? null : LocalDateTime.parse(results[i], dateTimeFormat))
        i++
        app.termCode = results[i++]
        app.ipAddress = results[i++]
        app.campaign1 = results[i++]
        app.campaign2 = results[i++]
        app.campaign3 = results[i++]
        app.ssnException = (results[i++] == 'Y')
        app.collegeName = results[i++]
        app.preferredFirstname = results[i++]
        app.preferredMiddlename = results[i++]
        app.preferredLastname = results[i++]
        app.preferredName = (results[i++] == 'Y')
        app.ssnNo = (results[i++] == 'Y')
        app.noPermAddressHomeless = (results[i++] == 'Y')
        app.noMailingAddressHomeless = (results[i++] == 'Y')
        app.determinedHomeless = (results[i++] == 'Y')
        app.eligMethodD = (results[i++] == 'Y')
        app.mainphoneintl = results[i++]
        app.eligExoneratedCrime = (results[i++] == 'Y')
        app.eligCovidDeath = (results[i++] == 'Y')

        app.acceptedTerms = (results[i++] == 'Y')
        app.acceptedTermsTimestamp = (results[i] == null || results[i].trim().isEmpty() ? null : LocalDateTime.parse(results[i], dateTimeFormat))
        i++

        app.phoneType = results[i++]

        app.mailingAddressValidationOverride = (results[i++] == 'Y')

        app.ipAddressAtAccountCreation = results[i++]
        app.ipAddressAtAppCreation = results[i++]

        app.studentParent = (results[i++] == 'Y')

        app.idmeConfirmationTimestamp = (results[i] == null || results[i].trim().isEmpty() ? null : LocalDateTime.parse(results[i], dateTimeFormat))
        i++
        
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

}
