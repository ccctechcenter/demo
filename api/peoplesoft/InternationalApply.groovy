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
import com.ccctc.adaptor.model.apply.InternationalApplication
import com.ccctc.adaptor.model.apply.SupplementalQuestions

import java.time.LocalDate
import java.time.LocalDateTime
import java.time.format.DateTimeFormatter

/**
 * <h1>CCCApply International Application Peoplesoft Interface</h1>
 * <summary>
 *     <p>Provides a Wrapper around the native peoplesoft java API for:</p>
 *     <ol>
 *         <li>Storing and populating International Applications from CCCApply into a custom peoplesoft staging tables</li>
 *     <ol>
 *     <p>Most logic for College Adaptor's Peoplesoft support is found within each Peoplesoft instance itself as
 *        this renders the highest level of transparency to each college for code-reviews and deployment authorization.
 *        See App Designer/PeopleTools projects latest API info
 *     </p>
 * </summary>
 *
 * @version CAv4.9.0
 *
 */
class InternationalApply {

    protected final static Logger log = LoggerFactory.getLogger(InternationalApply.class)

    //****** Populated by injection in Groovy Service Implementation ****
    Environment environment

    /**
     * Default Constructor needed since we also specify an alternative Constructor
     */
    InternationalApply(){
        // do nothing, except be available
    }

    /**
     * Alternative Constructor to set the environment during initialization.
     * Useful for when other groovy PS APIs need to use these Functions that depend on environment
     */
    InternationalApply(Environment e) {
        this.environment = e
    }

    /**
     * Gets International Application Data from a Peoplesoft staging table using the CCTC_InternationalApplication:getApplication
     * peoplesoft method
     * @param misCode The College Code used for grabbing college specific settings from the environment
     * @param appId The application Id used when originally populating the data into the staging table
     */
    InternationalApplication get(String misCode, Long appId) {
        log.debug("get: getting international application data")

        //****** Validate parameters ****
        if (!appId) {
            throw new InvalidRequestException(InvalidRequestException.Errors.invalidRequest, "Application Id cannot be null or blank")
        }
        if (!misCode) {
            throw new InvalidRequestException(InvalidRequestException.Errors.invalidSearchCriteria, "misCode cannot be null or blank")
        }

        log.debug("get: params ok; building method params")

        //****** Build parameters to send to our custom Peopletools API ****
        String packageName = "CCTC_INTL_PKG"
        String className = "CCTC_InternationalApplication"
        String methodName = "getApplication"

        String[] args = [appId]

        InternationalApplication application = new InternationalApplication()

        log.debug("get: attempting to get a new peoplesoft Session")
        //****** Create Connection to Peoplesoft and call remote method ******
        def peoplesoftSession = api.peoplesoft.PSConnection.getSession(environment, misCode)
        try {
            log.debug("get: calling the remote peoplesoft method")

            String[] appData = api.peoplesoft.PSConnection.callMethod(peoplesoftSession, packageName, className, methodName, "<", args)
            if(appData == null || appData.length <= 0) {
                throw new EntityNotFoundException("No International Application found for given Id")
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
            throw new InternalServerException(InternalServerException.Errors.internalServerError, "Peoplesoft Error- " + messageToThrow)
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
     * Sets International Application Data into a Peoplesoft staging table
     * using the CCTC_InternationalApplication:setApplication peoplesoft method
     * @param misCode The College Code used for grabbing college specific settings from the environment
     * @param application The application data to set into the staging table
     */
    InternationalApplication post(String misCode, InternationalApplication application) {
        log.debug("post: setting international application data")

        //****** Validate parameters ****
        if (application == null) {
            throw new InvalidRequestException(InvalidRequestException.Errors.invalidRequest, "International Application data cannot be null")
        }
        if (!application.appId) {
            throw new InvalidRequestException(InvalidRequestException.Errors.invalidRequest, "Application Id cannot be null or blank")
        }
        if (!misCode) {
            throw new InvalidRequestException(InvalidRequestException.Errors.invalidSearchCriteria, "misCode cannot be null or blank")
        }

        log.debug("post: params ok; building method params")
        //****** Build parameters to send to our custom Peopletools API ****
        String packageName = "CCTC_INTL_PKG"
        String className = "CCTC_InternationalApplication"
        String methodName = "getApplication"

        String[] args = [application.appId]

        log.debug("post: attempting to get a new peoplesoft Session")
        //****** Create Connection to Peoplesoft and call remote method ******
        def peoplesoftSession = api.peoplesoft.PSConnection.getSession(environment, misCode)
        try {
            log.debug("post: calling the remote peoplesoft method to check if already exists")
            String[] applData = api.peoplesoft.PSConnection.callMethod(peoplesoftSession, packageName, className, methodName, "<", args)
            if(applData.length > 0) {
                throw new EntityConflictException("International Application ID already exists")
            }

            packageName = "CCTC_INTL_PKG"
            className = "CCTC_InternationalApplication"
            methodName = "setApplication"

            args = this.ConvertApplicationToArgsArray(application)

            log.debug("post: calling the remote peoplesoft method set set the application")
            String[] rowsAffected = api.peoplesoft.PSConnection.callMethod(peoplesoftSession, packageName, className, methodName, "<", args)
            if(rowsAffected.length != 1) {
                throw new InternalServerException(InternalServerException.Errors.sisQueryError, "invalid number of rows affected. empty array")
            }
            if(!StringUtils.equalsIgnoreCase(rowsAffected[0], "1")) {
                throw new InternalServerException(InternalServerException.Errors.sisQueryError, "invalid number of rows affected [" + rowsAffected[0] + "]")
            }

            if(application.supplementalQuestions != null) {

                methodName = "setSupplementalQuestions"
                args = this.ConvertSupplementalQuestionToArgsArray(application.supplementalQuestions)

                log.debug("post: calling the remote peoplesoft method to set the supplemental questions")
                String[] sq_rowsAffected = api.peoplesoft.PSConnection.callMethod(peoplesoftSession, packageName, className, methodName, "<", args)
                if (sq_rowsAffected.length != 1) {
                    log.error("post: invalid number of rows affected on supp ques. empty array")
                    throw new InternalServerException(InternalServerException.Errors.sisQueryError, "invalid number of rows affected. empty array")
                }
                if (!StringUtils.equalsIgnoreCase(sq_rowsAffected[0], "1")) {
                    log.error("post: invalid number of rows affected on supp ques [" + sq_rowsAffected[0] + "]")
                    throw new InternalServerException(InternalServerException.Errors.sisQueryError, "invalid number of rows affected on supp ques [" + sq_rowsAffected[0] + "]")
                }
            }

            String dataEventsEnabled = MisEnvironment.getProperty(environment, misCode, "peoplesoft.dataEvents.enabled")
            if(StringUtils.equalsIgnoreCase(dataEventsEnabled,"true")) {
                try {
                    log.debug("post: triggering the remote peoplesoft event onInternationalAppInserted")

                    args = [application.appId]
                    api.peoplesoft.PSConnection.triggerEvent(peoplesoftSession, "onInternationalAppInserted", "<", args)
                }
                catch (Exception exc) {
                    String messageToThrow = exc.getMessage()
                    if(peoplesoftSession != null) {
                        String[] msgs = api.peoplesoft.PSConnection.getErrorMessages(peoplesoftSession)
                        if(msgs && msgs.length > 0) {
                            messageToThrow = msgs[0]
                            msgs.each {
                                log.debug("post: Triggering the onInternationalAppInserted event failed with message: [" + it + "]")
                            }
                        }
                    }
                    log.warn("post: Triggering the onInternationalAppInserted event failed with message: [" + messageToThrow + "]")
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
                        log.error("post: setting international application failed: [" + it + "]")
                    }
                }
            }
            throw new InternalServerException(InternalServerException.Errors.internalServerError, "Peoplesoft Error- " + messageToThrow)
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
     * Converts a International Application Object into a string array
     * @param app The application to convert to a string array
     * @retuns a string array containing the string value of each field in the international application object
     */
    protected String[] ConvertApplicationToArgsArray(InternationalApplication app) {
        DateTimeFormatter dateFormat = DateTimeFormatter.ofPattern("yyyy-MM-dd")
        DateTimeFormatter dateTimeFormat = DateTimeFormatter.ofPattern("yyyy-MM-dd-HH.mm.ss.SSSSSS")
        String[] map = [
                PSParameter.ConvertLongToCleanString(app.appId),
                PSParameter.ConvertStringToCleanString(app.cccId),
                PSParameter.ConvertStringToCleanString(app.collegeId),
                PSParameter.ConvertBoolToCleanString(app.addressVerified),
                PSParameter.ConvertStringToCleanString(app.agentEmail),
                PSParameter.ConvertStringToCleanString(app.agentPhoneExt),
                PSParameter.ConvertStringToCleanString(app.agentPhoneNumber),
                PSParameter.ConvertBoolToCleanString(app.altNonUsPhoneAuthTxt),
                PSParameter.ConvertStringToCleanString(app.altNonUsPhoneExt),
                PSParameter.ConvertStringToCleanString(app.altNonUsPhoneNumber),
                PSParameter.ConvertStringToCleanString(app.appLang),
                PSParameter.ConvertBoolToCleanString(app.authorizeAgentInfoRelease),
                PSParameter.ConvertDateToCleanString(app.birthdate, dateFormat),
                PSParameter.ConvertStringToCleanString(app.campaign1),
                PSParameter.ConvertStringToCleanString(app.campaign2),
                PSParameter.ConvertStringToCleanString(app.campaign3),
                PSParameter.ConvertStringToCleanString(app.citizenshipStatus),
                PSParameter.ConvertStringToCleanString(app.city),
                PSParameter.ConvertStringToCleanString(app.coenrollConfirm),

                PSParameter.ConvertStringToCleanString(app.col1Cds),
                PSParameter.ConvertStringToCleanString(app.col1CdsFull),
                PSParameter.ConvertStringToCleanString(app.col1Ceeb),
                PSParameter.ConvertStringToCleanString(app.col1City),
                PSParameter.ConvertLongToCleanString(app.col1CollegeAttendedId),
                PSParameter.ConvertStringToCleanString(app.col1Country),
                PSParameter.ConvertDateToCleanString(app.col1DegreeDate, dateFormat),
                PSParameter.ConvertStringToCleanString(app.col1DegreeObtained),
                PSParameter.ConvertDateToCleanString(app.col1EndDate, dateFormat),
                PSParameter.ConvertBoolToCleanString(app.col1ExpelledStatus),
                PSParameter.ConvertStringToCleanString(app.col1Major),
                PSParameter.ConvertStringToCleanString(app.col1Name),
                PSParameter.ConvertStringToCleanString(app.col1NonUsProvince),
                PSParameter.ConvertBoolToCleanString(app.col1NotListed),
                PSParameter.ConvertStringToCleanString(app.col1PrimaryInstructionLanguage),
                PSParameter.ConvertDateToCleanString(app.col1StartDate, dateFormat),
                PSParameter.ConvertStringToCleanString(app.col1State),

                PSParameter.ConvertStringToCleanString(app.col2Cds),
                PSParameter.ConvertStringToCleanString(app.col2CdsFull),
                PSParameter.ConvertStringToCleanString(app.col2Ceeb),
                PSParameter.ConvertStringToCleanString(app.col2City),
                PSParameter.ConvertLongToCleanString(app.col2CollegeAttendedId),
                PSParameter.ConvertStringToCleanString(app.col2Country),
                PSParameter.ConvertDateToCleanString(app.col2DegreeDate, dateFormat),
                PSParameter.ConvertStringToCleanString(app.col2DegreeObtained),
                PSParameter.ConvertDateToCleanString(app.col2EndDate, dateFormat),
                PSParameter.ConvertBoolToCleanString(app.col2ExpelledStatus),
                PSParameter.ConvertStringToCleanString(app.col2Major),
                PSParameter.ConvertStringToCleanString(app.col2Name),
                PSParameter.ConvertStringToCleanString(app.col2NonUsProvince),
                PSParameter.ConvertBoolToCleanString(app.col2NotListed),
                PSParameter.ConvertStringToCleanString(app.col2PrimaryInstructionLanguage),
                PSParameter.ConvertDateToCleanString(app.col2StartDate, dateFormat),
                PSParameter.ConvertStringToCleanString(app.col2State),

                PSParameter.ConvertStringToCleanString(app.col3Cds),
                PSParameter.ConvertStringToCleanString(app.col3CdsFull),
                PSParameter.ConvertStringToCleanString(app.col3Ceeb),
                PSParameter.ConvertStringToCleanString(app.col3City),
                PSParameter.ConvertLongToCleanString(app.col3CollegeAttendedId),
                PSParameter.ConvertStringToCleanString(app.col3Country),
                PSParameter.ConvertDateToCleanString(app.col3DegreeDate, dateFormat),
                PSParameter.ConvertStringToCleanString(app.col3DegreeObtained),
                PSParameter.ConvertDateToCleanString(app.col3EndDate, dateFormat),
                PSParameter.ConvertBoolToCleanString(app.col3ExpelledStatus),
                PSParameter.ConvertStringToCleanString(app.col3Major),
                PSParameter.ConvertStringToCleanString(app.col3Name),
                PSParameter.ConvertStringToCleanString(app.col3NonUsProvince),
                PSParameter.ConvertBoolToCleanString(app.col3NotListed),
                PSParameter.ConvertStringToCleanString(app.col3PrimaryInstructionLanguage),
                PSParameter.ConvertDateToCleanString(app.col3StartDate, dateFormat),
                PSParameter.ConvertStringToCleanString(app.col3State),

                PSParameter.ConvertStringToCleanString(app.col4Cds),
                PSParameter.ConvertStringToCleanString(app.col4CdsFull),
                PSParameter.ConvertStringToCleanString(app.col4Ceeb),
                PSParameter.ConvertStringToCleanString(app.col4City),
                PSParameter.ConvertLongToCleanString(app.col4CollegeAttendedId),
                PSParameter.ConvertStringToCleanString(app.col4Country),
                PSParameter.ConvertDateToCleanString(app.col4DegreeDate, dateFormat),
                PSParameter.ConvertStringToCleanString(app.col4DegreeObtained),
                PSParameter.ConvertDateToCleanString(app.col4EndDate, dateFormat),
                PSParameter.ConvertBoolToCleanString(app.col4ExpelledStatus),
                PSParameter.ConvertStringToCleanString(app.col4Major),
                PSParameter.ConvertStringToCleanString(app.col4Name),
                PSParameter.ConvertStringToCleanString(app.col4NonUsProvince),
                PSParameter.ConvertBoolToCleanString(app.col4NotListed),
                PSParameter.ConvertStringToCleanString(app.col4PrimaryInstructionLanguage),
                PSParameter.ConvertDateToCleanString(app.col4StartDate, dateFormat),
                PSParameter.ConvertStringToCleanString(app.col4State),

                PSParameter.ConvertDateToCleanString(app.collegeCompDate, dateFormat),
                PSParameter.ConvertIntegerToCleanString(app.collegeCount),
                PSParameter.ConvertStringToCleanString(app.collegeEduLevel),
                PSParameter.ConvertBoolToCleanString(app.collegeExpelledSummary),
                PSParameter.ConvertStringToCleanString(app.collegeName),
                PSParameter.ConvertStringToCleanString(app.company),
                PSParameter.ConvertStringToCleanString(app.confirmation),
                PSParameter.ConvertBoolToCleanString(app.consent),
                PSParameter.ConvertStringToCleanString(app.contact),
                PSParameter.ConvertStringToCleanString(app.country),
                PSParameter.ConvertStringToCleanString(app.countryOfBirth),
                PSParameter.ConvertStringToCleanString(app.countryOfCitizenship),


                PSParameter.ConvertBoolToCleanString(app.currentMailingAddressVerified),
                PSParameter.ConvertBoolToCleanString(app.addressValidationOverride),
                PSParameter.ConvertDateTimeToCleanString(app.addressValidationOverrideTimestamp, dateTimeFormat),
                PSParameter.ConvertStringToCleanString(app.currentMailingCity),
                PSParameter.ConvertStringToCleanString(app.currentMailingCountry),
                PSParameter.ConvertBoolToCleanString(app.currentMailingNonUsAddress),
                PSParameter.ConvertStringToCleanString(app.currentMailingNonUsPostalCode),
                PSParameter.ConvertStringToCleanString(app.currentMailingNonUsProvince),
                PSParameter.ConvertBoolToCleanString(app.currentMailingSameAsPermanent),
                PSParameter.ConvertStringToCleanString(app.currentMailingState),
                PSParameter.ConvertStringToCleanString(app.currentMailingStreet1),
                PSParameter.ConvertStringToCleanString(app.currentMailingStreet2),
                PSParameter.ConvertStringToCleanString(app.currentMailingZipCode),
                PSParameter.ConvertBoolToCleanString(app.noMailingAddressHomeless),

                PSParameter.ConvertStringToCleanString(app.dep1CountryOfBirth),
                PSParameter.ConvertDateToCleanString(app.dep1DateOfBirth, dateFormat),
                PSParameter.ConvertStringToCleanString(app.dep1FirstName),
                PSParameter.ConvertStringToCleanString(app.dep1Gender),
                PSParameter.ConvertStringToCleanString(app.dep1LastName),
                PSParameter.ConvertBoolToCleanString(app.dep1NoFirstName),
                PSParameter.ConvertStringToCleanString(app.dep1Relationship),
                PSParameter.ConvertStringToCleanString(app.dep10CountryOfBirth),
                PSParameter.ConvertDateToCleanString(app.dep10DateOfBirth, dateFormat),

                PSParameter.ConvertStringToCleanString(app.dep10FirstName),
                PSParameter.ConvertStringToCleanString(app.dep10Gender),
                PSParameter.ConvertStringToCleanString(app.dep10LastName),
                PSParameter.ConvertBoolToCleanString(app.dep10NoFirstName),
                PSParameter.ConvertStringToCleanString(app.dep10Relationship),
                PSParameter.ConvertStringToCleanString(app.dep2CountryOfBirth),
                PSParameter.ConvertDateToCleanString(app.dep2DateOfBirth, dateFormat),

                PSParameter.ConvertStringToCleanString(app.dep2FirstName),
                PSParameter.ConvertStringToCleanString(app.dep2Gender),
                PSParameter.ConvertStringToCleanString(app.dep2LastName),
                PSParameter.ConvertBoolToCleanString(app.dep2NoFirstName),
                PSParameter.ConvertStringToCleanString(app.dep2Relationship),
                PSParameter.ConvertStringToCleanString(app.dep3CountryOfBirth),
                PSParameter.ConvertDateToCleanString(app.dep3DateOfBirth, dateFormat),

                PSParameter.ConvertStringToCleanString(app.dep3FirstName),
                PSParameter.ConvertStringToCleanString(app.dep3Gender),
                PSParameter.ConvertStringToCleanString(app.dep3LastName),
                PSParameter.ConvertBoolToCleanString(app.dep3NoFirstName),
                PSParameter.ConvertStringToCleanString(app.dep3Relationship),
                PSParameter.ConvertStringToCleanString(app.dep4CountryOfBirth),
                PSParameter.ConvertDateToCleanString(app.dep4DateOfBirth, dateFormat),

                PSParameter.ConvertStringToCleanString(app.dep4FirstName),
                PSParameter.ConvertStringToCleanString(app.dep4Gender),
                PSParameter.ConvertStringToCleanString(app.dep4LastName),
                PSParameter.ConvertBoolToCleanString(app.dep4NoFirstName),
                PSParameter.ConvertStringToCleanString(app.dep4Relationship),
                PSParameter.ConvertStringToCleanString(app.dep5CountryOfBirth),
                PSParameter.ConvertDateToCleanString(app.dep5DateOfBirth, dateFormat),

                PSParameter.ConvertStringToCleanString(app.dep5FirstName),
                PSParameter.ConvertStringToCleanString(app.dep5Gender),
                PSParameter.ConvertStringToCleanString(app.dep5LastName),
                PSParameter.ConvertBoolToCleanString(app.dep5NoFirstName),
                PSParameter.ConvertStringToCleanString(app.dep5Relationship),
                PSParameter.ConvertStringToCleanString(app.dep6CountryOfBirth),
                PSParameter.ConvertDateToCleanString(app.dep6DateOfBirth, dateFormat),

                PSParameter.ConvertStringToCleanString(app.dep6FirstName),
                PSParameter.ConvertStringToCleanString(app.dep6Gender),
                PSParameter.ConvertStringToCleanString(app.dep6LastName),
                PSParameter.ConvertBoolToCleanString(app.dep6NoFirstName),
                PSParameter.ConvertStringToCleanString(app.dep6Relationship),
                PSParameter.ConvertStringToCleanString(app.dep7CountryOfBirth),
                PSParameter.ConvertDateToCleanString(app.dep7DateOfBirth, dateFormat),

                PSParameter.ConvertStringToCleanString(app.dep7FirstName),
                PSParameter.ConvertStringToCleanString(app.dep7Gender),
                PSParameter.ConvertStringToCleanString(app.dep7LastName),
                PSParameter.ConvertBoolToCleanString(app.dep7NoFirstName),
                PSParameter.ConvertStringToCleanString(app.dep7Relationship),
                PSParameter.ConvertStringToCleanString(app.dep8CountryOfBirth),
                PSParameter.ConvertDateToCleanString(app.dep8DateOfBirth, dateFormat),

                PSParameter.ConvertStringToCleanString(app.dep8FirstName),
                PSParameter.ConvertStringToCleanString(app.dep8Gender),
                PSParameter.ConvertStringToCleanString(app.dep8LastName),
                PSParameter.ConvertBoolToCleanString(app.dep8NoFirstName),
                PSParameter.ConvertStringToCleanString(app.dep8Relationship),
                PSParameter.ConvertStringToCleanString(app.dep9CountryOfBirth),
                PSParameter.ConvertDateToCleanString(app.dep9DateOfBirth, dateFormat),

                PSParameter.ConvertStringToCleanString(app.dep9FirstName),
                PSParameter.ConvertStringToCleanString(app.dep9Gender),
                PSParameter.ConvertStringToCleanString(app.dep9LastName),
                PSParameter.ConvertBoolToCleanString(app.dep9NoFirstName),
                PSParameter.ConvertStringToCleanString(app.dep9Relationship),
                PSParameter.ConvertStringToCleanString(app.educationalGoal),
                PSParameter.ConvertStringToCleanString(app.email),
                PSParameter.ConvertBoolToCleanString(app.emailVerified),
                PSParameter.ConvertDateTimeToCleanString(app.emailVerifiedTimestamp, dateTimeFormat),
                PSParameter.ConvertStringToCleanString(app.preferredMethodOfContact),
                PSParameter.ConvertBoolToCleanString(app.emergencyContactAddressVerified),
                PSParameter.ConvertStringToCleanString(app.emergencyContactCity),
                PSParameter.ConvertStringToCleanString(app.emergencyContactCountry),
                PSParameter.ConvertStringToCleanString(app.emergencyContactEmail),
                PSParameter.ConvertStringToCleanString(app.emergencyContactFirstName),
                PSParameter.ConvertStringToCleanString(app.emergencyContactLastName),
                PSParameter.ConvertBoolToCleanString(app.emergencyContactNoFirstName),
                PSParameter.ConvertBoolToCleanString(app.emergencyContactNonUsAddress),
                PSParameter.ConvertStringToCleanString(app.emergencyContactNonUsPostalCode),
                PSParameter.ConvertStringToCleanString(app.emergencyContactNonUsProvince),
                PSParameter.ConvertBoolToCleanString(app.emergencyContactPhoneAuthTxt),
                PSParameter.ConvertStringToCleanString(app.emergencyContactPhoneExt),
                PSParameter.ConvertStringToCleanString(app.emergencyContactPhoneNumber),
                PSParameter.ConvertStringToCleanString(app.emergencyContactRelationship),
                PSParameter.ConvertStringToCleanString(app.emergencyContactState),
                PSParameter.ConvertStringToCleanString(app.emergencyContactStreet1),
                PSParameter.ConvertStringToCleanString(app.emergencyContactStreet2),
                PSParameter.ConvertStringToCleanString(app.emergencyContactZipCode),
                PSParameter.ConvertIntegerToCleanString(app.engMonthsStudied),
                PSParameter.ConvertDateToCleanString(app.engProficiencyDate, dateFormat),
                PSParameter.ConvertStringToCleanString(app.engProficiencyOther),
                PSParameter.ConvertBoolToCleanString(app.engProficiencyPrerequisite),

                PSParameter.ConvertStringToCleanString(app.engProficiencyScore),
                PSParameter.ConvertBoolToCleanString(app.engProficiencyShowScore),
                PSParameter.ConvertStringToCleanString(app.engProficiencyType),
                PSParameter.ConvertStringToCleanString(app.enrollStatus),
                PSParameter.ConvertStringToCleanString(app.enrollTermCode),
                PSParameter.ConvertStringToCleanString(app.enrollTermDescription),
                PSParameter.ConvertBoolToCleanString(app.esignature),
                PSParameter.ConvertDateToCleanString(app.expirationDate, dateFormat),
                PSParameter.ConvertStringToCleanString(app.faxNumberNumber),
                PSParameter.ConvertStringToCleanString(app.firstname),
                PSParameter.ConvertStringToCleanString(app.gender),

                PSParameter.ConvertDateToCleanString(app.highestCompDate, dateFormat),
                PSParameter.ConvertStringToCleanString(app.highestEduLevel),
                PSParameter.ConvertBoolToCleanString(app.hispanic),
                PSParameter.ConvertBoolToCleanString(app.hsAddressVerified),
                PSParameter.ConvertStringToCleanString(app.hsCds),
                PSParameter.ConvertStringToCleanString(app.hsCdsFull),
                PSParameter.ConvertStringToCleanString(app.hsCeeb),
                PSParameter.ConvertStringToCleanString(app.hsCity),
                PSParameter.ConvertDateToCleanString(app.hsCompDate, dateFormat),
                PSParameter.ConvertStringToCleanString(app.hsCountry),
                PSParameter.ConvertStringToCleanString(app.hsDiplomaCert),
                PSParameter.ConvertDateToCleanString(app.hsDiplomaCertDate, dateFormat),
                PSParameter.ConvertStringToCleanString(app.hsEduLevel),
                PSParameter.ConvertDateToCleanString(app.hsEndDate, dateFormat),
                PSParameter.ConvertStringToCleanString(app.hsLang),
                PSParameter.ConvertStringToCleanString(app.hsName),
                PSParameter.ConvertBoolToCleanString(app.hsNonUsAddress),
                PSParameter.ConvertStringToCleanString(app.hsNonUsPostalCode),
                PSParameter.ConvertStringToCleanString(app.hsNonUsProvince),
                PSParameter.ConvertBoolToCleanString(app.hsNotListed),
                PSParameter.ConvertDateToCleanString(app.hsStartDate, dateFormat),
                PSParameter.ConvertStringToCleanString(app.hsState),
                PSParameter.ConvertStringToCleanString(app.hsStreet1),
                PSParameter.ConvertStringToCleanString(app.hsStreet2),
                PSParameter.ConvertStringToCleanString(app.hsType),
                PSParameter.ConvertStringToCleanString(app.hsZipCode),
                PSParameter.ConvertDateToCleanString(app.i20ExpirationDate, dateFormat),
                PSParameter.ConvertStringToCleanString(app.i20IssuingSchoolName),
                PSParameter.ConvertStringToCleanString(app.i94AdmissionNumber),
                PSParameter.ConvertDateToCleanString(app.i94ExpirationDate, dateFormat),
                PSParameter.ConvertStringToCleanString(app.intended4YearMajor),
                PSParameter.ConvertStringToCleanString(app.ipAddress),
                PSParameter.ConvertDateToCleanString(app.issueDate, dateFormat),
                PSParameter.ConvertStringToCleanString(app.lastPage),
                PSParameter.ConvertStringToCleanString(app.lastname),
                PSParameter.ConvertBoolToCleanString(app.mainPhoneAuthTxt),
                PSParameter.ConvertStringToCleanString(app.mainPhoneExt),
                PSParameter.ConvertStringToCleanString(app.mainPhoneNumber),
                PSParameter.ConvertBoolToCleanString(app.mainphoneVerified),
                PSParameter.ConvertDateTimeToCleanString(app.mainphoneVerifiedTimestamp, dateTimeFormat),

                PSParameter.ConvertStringToCleanString(app.majorCode),
                PSParameter.ConvertStringToCleanString(app.majorDescription),
                PSParameter.ConvertStringToCleanString(app.majorCategory),
                PSParameter.ConvertStringToCleanString(app.cipCode),

                PSParameter.ConvertStringToCleanString(app.middlename),
                PSParameter.ConvertBoolToCleanString(app.noI94ExpirationDate),
                PSParameter.ConvertBoolToCleanString(app.noVisa),

                PSParameter.ConvertBoolToCleanString(app.residesInUs),
                PSParameter.ConvertBoolToCleanString(app.nonUsAddress),
                PSParameter.ConvertBoolToCleanString(app.nonUsPermanentHomeAddressSameAsPermanent),
                PSParameter.ConvertBoolToCleanString(app.nonUsPermanentHomeAddressVerified),
                PSParameter.ConvertStringToCleanString(app.nonUsPermanentHomeCity),
                PSParameter.ConvertStringToCleanString(app.nonUsPermanentHomeCountry),
                PSParameter.ConvertBoolToCleanString(app.nonUsPermanentHomeNonUsAddress),
                PSParameter.ConvertStringToCleanString(app.nonUsPermanentHomeNonUsPostalCode),
                PSParameter.ConvertStringToCleanString(app.nonUsPermanentHomeNonUsProvince),
                PSParameter.ConvertStringToCleanString(app.nonUsPermanentHomeStreet1),
                PSParameter.ConvertStringToCleanString(app.nonUsPermanentHomeStreet2),
                PSParameter.ConvertBoolToCleanString(app.noNonUsaPermAddressHomeless),

                PSParameter.ConvertBoolToCleanString(app.nonUsPhoneAuthTxt),
                PSParameter.ConvertStringToCleanString(app.nonUsPhoneExt),
                PSParameter.ConvertStringToCleanString(app.nonUsPhoneNumber),
                PSParameter.ConvertStringToCleanString(app.nonUsPostalCode),
                PSParameter.ConvertStringToCleanString(app.nonUsProvince),
                PSParameter.ConvertIntegerToCleanString(app.numberOfDependents),
                PSParameter.ConvertIntegerToCleanString(app.numberOfPracticalTraining),
                PSParameter.ConvertStringToCleanString(app.otherfirstname),
                PSParameter.ConvertStringToCleanString(app.otherlastname),
                PSParameter.ConvertStringToCleanString(app.othermiddlename),
                PSParameter.ConvertBoolToCleanString(app.parentGuardianAddressVerified),
                PSParameter.ConvertStringToCleanString(app.parentGuardianCity),
                PSParameter.ConvertStringToCleanString(app.parentGuardianCountry),
                PSParameter.ConvertStringToCleanString(app.parentGuardianEmail),
                PSParameter.ConvertStringToCleanString(app.parentGuardianFirstName),
                PSParameter.ConvertStringToCleanString(app.parentGuardianLastName),
                PSParameter.ConvertBoolToCleanString(app.parentGuardianNoFirstName),
                PSParameter.ConvertBoolToCleanString(app.parentGuardianNonUsAddress),
                PSParameter.ConvertStringToCleanString(app.parentGuardianNonUsPostalCode),
                PSParameter.ConvertStringToCleanString(app.parentGuardianNonUsProvince),
                PSParameter.ConvertBoolToCleanString(app.parentGuardianPhoneAuthTxt),
                PSParameter.ConvertStringToCleanString(app.parentGuardianPhoneExt),
                PSParameter.ConvertStringToCleanString(app.parentGuardianPhoneNumber),
                PSParameter.ConvertStringToCleanString(app.parentGuardianRelationship),
                PSParameter.ConvertStringToCleanString(app.parentGuardianState),
                PSParameter.ConvertStringToCleanString(app.parentGuardianStreet1),
                PSParameter.ConvertStringToCleanString(app.parentGuardianStreet2),
                PSParameter.ConvertStringToCleanString(app.parentGuardianZipCode),
                PSParameter.ConvertStringToCleanString(app.passportCountryOfIssuance),
                PSParameter.ConvertDateToCleanString(app.passportExpirationDate, dateFormat),
                PSParameter.ConvertBoolToCleanString(app.passportNotYet),
                PSParameter.ConvertStringToCleanString(app.passportNumber),
                PSParameter.ConvertBoolToCleanString(app.permAddrAddressVerified),
                PSParameter.ConvertStringToCleanString(app.permAddrCity),
                PSParameter.ConvertStringToCleanString(app.permAddrCountry),
                PSParameter.ConvertBoolToCleanString(app.permAddrNonUsAddress),
                PSParameter.ConvertStringToCleanString(app.permAddrNonUsPostalCode),
                PSParameter.ConvertStringToCleanString(app.permAddrNonUsProvince),
                PSParameter.ConvertStringToCleanString(app.permAddrState),
                PSParameter.ConvertStringToCleanString(app.permAddrStreet1),
                PSParameter.ConvertStringToCleanString(app.permAddrStreet2),
                PSParameter.ConvertStringToCleanString(app.permAddrZipCode),
                PSParameter.ConvertBoolToCleanString(app.noPermAddressHomeless),

                PSParameter.ConvertBoolToCleanString(app.phoneAuthTxt),
                PSParameter.ConvertStringToCleanString(app.phoneExt),
                PSParameter.ConvertStringToCleanString(app.phoneNumber),

                PSParameter.ConvertStringToCleanString(app.preferredFirstname),
                PSParameter.ConvertStringToCleanString(app.preferredLastname),
                PSParameter.ConvertStringToCleanString(app.preferredMiddlename),
                PSParameter.ConvertBoolToCleanString(app.preferredName),
                PSParameter.ConvertBoolToCleanString(app.presentlyStudyingInUs),
                PSParameter.ConvertStringToCleanString(app.primaryLanguage),
                PSParameter.ConvertStringToCleanString(app.pt1AuthorizingSchool),
                PSParameter.ConvertDateToCleanString(app.pt1EndDate, dateFormat),

                PSParameter.ConvertDateToCleanString(app.pt1StartDate, dateFormat),
                PSParameter.ConvertStringToCleanString(app.pt1Type),
                PSParameter.ConvertStringToCleanString(app.pt2AuthorizingSchool),
                PSParameter.ConvertDateToCleanString(app.pt2EndDate, dateFormat),

                PSParameter.ConvertDateToCleanString(app.pt2StartDate, dateFormat),
                PSParameter.ConvertStringToCleanString(app.pt2Type),
                PSParameter.ConvertStringToCleanString(app.pt3AuthorizingSchool),
                PSParameter.ConvertDateToCleanString(app.pt3EndDate, dateFormat),

                PSParameter.ConvertDateToCleanString(app.pt3StartDate, dateFormat),
                PSParameter.ConvertStringToCleanString(app.pt3Type),
                PSParameter.ConvertStringToCleanString(app.pt4AuthorizingSchool),
                PSParameter.ConvertDateToCleanString(app.pt4EndDate, dateFormat),

                PSParameter.ConvertDateToCleanString(app.pt4StartDate, dateFormat),
                PSParameter.ConvertStringToCleanString(app.pt4Type),
                PSParameter.ConvertStringToCleanString(app.raceEthnicity),
                PSParameter.ConvertStringToCleanString(app.raceEthnicFull),
                PSParameter.ConvertStringToCleanString(app.raceGroup),

                PSParameter.ConvertBoolToCleanString(app.secondPhoneAuthTxt),
                PSParameter.ConvertStringToCleanString(app.secondPhoneExt),
                PSParameter.ConvertStringToCleanString(app.secondPhoneNumber),
                PSParameter.ConvertStringToCleanString(app.sevisIdNumber),

                PSParameter.ConvertStringToCleanString(app.ssn),
                PSParameter.ConvertBoolToCleanString(app.ssnNo),
                PSParameter.ConvertBoolToCleanString(app.ssnException),
                PSParameter.ConvertStringToCleanString(app.ssnLast4),
                PSParameter.ConvertStringToCleanString(app.ssnType),
                PSParameter.ConvertStringToCleanString(app.state),
                PSParameter.ConvertStringToCleanString(app.status),
                PSParameter.ConvertStringToCleanString(app.street1),
                PSParameter.ConvertStringToCleanString(app.street2),
                PSParameter.ConvertStringToCleanString(app.suffix),
                PSParameter.ConvertStringToCleanString(app.supPageCode),
                PSParameter.ConvertDateToCleanString(app.termEndDate, dateFormat),
                PSParameter.ConvertDateToCleanString(app.termStartDate, dateFormat),
                PSParameter.ConvertDateTimeToCleanString(app.tstmpCreate, dateTimeFormat),
                PSParameter.ConvertDateTimeToCleanString(app.tstmpDownload, dateTimeFormat),
                PSParameter.ConvertDateTimeToCleanString(app.tstmpSubmit, dateTimeFormat),
                PSParameter.ConvertDateTimeToCleanString(app.tstmpUpdate, dateTimeFormat),
                PSParameter.ConvertStringToCleanString(app.visaType),
                PSParameter.ConvertStringToCleanString(app.zipCode),

                PSParameter.ConvertBoolToCleanString(app.acceptedTerms),
                PSParameter.ConvertDateTimeToCleanString(app.acceptedTermsTimestamp, dateTimeFormat),

                PSParameter.ConvertStringToCleanString(app.phoneType),

                PSParameter.ConvertStringToCleanString(app.ipAddressAtAccountCreation),
                PSParameter.ConvertStringToCleanString(app.ipAddressAtAppCreation),

                PSParameter.ConvertDateTimeToCleanString(app.idmeConfirmationTimestamp, dateTimeFormat),
                PSParameter.ConvertDateTimeToCleanString(app.idmeOptinTimestamp, dateTimeFormat),
                PSParameter.ConvertStringToCleanString(app.idmeWorkflowStatus)
        ]
        return map
    }

    /**
     * Converts a International Application's Supplemental Questions Object into a string array
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
     * Converts a string array into an International Application Object
     * @param results The string array to convert to an International Application Object
     * @retuns an International Application object with each field populated from the string array
     */
    protected InternationalApplication ConvertStringArrayToApplication(String[] results) {
        if(results.length < 386) {
            return null
        }

        DateTimeFormatter dateFormat = DateTimeFormatter.ofPattern("yyyy-MM-dd")
        DateTimeFormatter dateTimeFormat = DateTimeFormatter.ofPattern("yyyy-MM-dd-HH.mm.ss.SSSSSS")

        Integer i = 0
        InternationalApplication app = new InternationalApplication()

        app.appId = results[i++].toBigInteger()
        app.cccId = results[i++]
        app.collegeId = results[i++]
        app.addressVerified = (results[i++] == 'Y')
        app.agentEmail = results[i++]
        app.agentPhoneExt = results[i++]
        app.agentPhoneNumber = results[i++]
        app.altNonUsPhoneAuthTxt = (results[i++] == 'Y')
        app.altNonUsPhoneExt = results[i++]
        app.altNonUsPhoneNumber = results[i++]
        app.appLang = results[i++]
        app.authorizeAgentInfoRelease = (results[i++] == 'Y')
        app.birthdate = (results[i] == null || results[i].trim().isEmpty() ? null : LocalDate.parse(results[i], dateFormat))
        i++
        app.campaign1 = results[i++]
        app.campaign2 = results[i++]
        app.campaign3 = results[i++]
        app.citizenshipStatus = results[i++]
        app.city = results[i++]
        app.coenrollConfirm = results[i++]

        app.col1Cds = results[i++]
        app.col1CdsFull = results[i++]
        app.col1Ceeb = results[i++]
        app.col1City = results[i++]
        app.col1CollegeAttendedId = results[i++].toBigInteger()
        app.col1Country = results[i++]
        app.col1DegreeDate = (results[i] == null || results[i].trim().isEmpty() ? null : LocalDate.parse(results[i], dateFormat))
        i++
        app.col1DegreeObtained = results[i++]
        app.col1EndDate = (results[i] == null || results[i].trim().isEmpty() ? null : LocalDate.parse(results[i], dateFormat))
        i++
        app.col1ExpelledStatus = (results[i++] == 'Y')
        app.col1Major = results[i++]
        app.col1Name = results[i++]
        app.col1NonUsProvince = results[i++]
        app.col1NotListed = (results[i++] == 'Y')
        app.col1PrimaryInstructionLanguage = results[i++]
        app.col1StartDate = (results[i] == null || results[i].trim().isEmpty() ? null : LocalDate.parse(results[i], dateFormat))
        i++
        app.col1State = results[i++]

        app.col2Cds = results[i++]
        app.col2CdsFull = results[i++]
        app.col2Ceeb = results[i++]
        app.col2City = results[i++]
        app.col2CollegeAttendedId = results[i++].toBigInteger()
        app.col2Country = results[i++]
        app.col2DegreeDate = (results[i] == null || results[i].trim().isEmpty() ? null : LocalDate.parse(results[i], dateFormat))
        i++
        app.col2DegreeObtained = results[i++]
        app.col2EndDate = (results[i] == null || results[i].trim().isEmpty() ? null : LocalDate.parse(results[i], dateFormat))
        i++
        app.col2ExpelledStatus = (results[i++] == 'Y')
        app.col2Major = results[i++]
        app.col2Name = results[i++]
        app.col2NonUsProvince = results[i++]
        app.col2NotListed = (results[i++] == 'Y')
        app.col2PrimaryInstructionLanguage = results[i++]
        app.col2StartDate = (results[i] == null || results[i].trim().isEmpty() ? null : LocalDate.parse(results[i], dateFormat))
        i++
        app.col2State = results[i++]

        app.col3Cds = results[i++]
        app.col3CdsFull = results[i++]
        app.col3Ceeb = results[i++]
        app.col3City = results[i++]
        app.col3CollegeAttendedId = results[i++].toBigInteger()
        app.col3Country = results[i++]
        app.col3DegreeDate = (results[i] == null || results[i].trim().isEmpty() ? null : LocalDate.parse(results[i], dateFormat))
        i++
        app.col3DegreeObtained = results[i++]
        app.col3EndDate = (results[i] == null || results[i].trim().isEmpty() ? null : LocalDate.parse(results[i], dateFormat))
        i++
        app.col3ExpelledStatus = (results[i++] == 'Y')
        app.col3Major = results[i++]
        app.col3Name = results[i++]
        app.col3NonUsProvince = results[i++]
        app.col3NotListed = (results[i++] == 'Y')
        app.col3PrimaryInstructionLanguage = results[i++]
        app.col3StartDate = (results[i] == null || results[i].trim().isEmpty() ? null : LocalDate.parse(results[i], dateFormat))
        i++
        app.col3State = results[i++]

        app.col4Cds = results[i++]
        app.col4CdsFull = results[i++]
        app.col4Ceeb = results[i++]
        app.col4City = results[i++]
        app.col4CollegeAttendedId = results[i++].toBigInteger()
        app.col4Country = results[i++]
        app.col4DegreeDate = (results[i] == null || results[i].trim().isEmpty() ? null : LocalDate.parse(results[i], dateFormat))
        i++
        app.col4DegreeObtained = results[i++]
        app.col4EndDate = (results[i] == null || results[i].trim().isEmpty() ? null : LocalDate.parse(results[i], dateFormat))
        i++
        app.col4ExpelledStatus = (results[i++] == 'Y')
        app.col4Major = results[i++]
        app.col4Name = results[i++]
        app.col4NonUsProvince = results[i++]
        app.col4NotListed = (results[i++] == 'Y')
        app.col4PrimaryInstructionLanguage = results[i++]
        app.col4StartDate = (results[i] == null || results[i].trim().isEmpty() ? null : LocalDate.parse(results[i], dateFormat))
        i++
        app.col4State = results[i++]

        app.collegeCompDate = (results[i] == null || results[i].trim().isEmpty() ? null : LocalDate.parse(results[i], dateFormat))
        i++
        app.collegeCount = results[i++].toInteger()
        app.collegeEduLevel = results[i++]
        app.collegeExpelledSummary = (results[i++] == 'Y')
        app.collegeName = results[i++]
        app.company = results[i++]
        app.confirmation = results[i++]
        app.consent = (results[i++] == 'Y')
        app.contact = results[i++]
        app.country = results[i++]
        app.countryOfBirth = results[i++]
        app.countryOfCitizenship = results[i++]


        app.currentMailingAddressVerified = (results[i++] == 'Y')
        app.addressValidationOverride = (results[i++] == 'Y')
        app.addressValidationOverrideTimestamp = (results[i] == null || results[i].trim().isEmpty() ? null : LocalDateTime.parse(results[i], dateTimeFormat))
        i++
        app.currentMailingCity = results[i++]
        app.currentMailingCountry = results[i++]
        app.currentMailingNonUsAddress = (results[i++] == 'Y')
        app.currentMailingNonUsPostalCode = results[i++]
        app.currentMailingNonUsProvince = results[i++]
        app.currentMailingSameAsPermanent = (results[i++] == 'Y')
        app.currentMailingState = results[i++]
        app.currentMailingStreet1 = results[i++]
        app.currentMailingStreet2 = results[i++]
        app.currentMailingZipCode = results[i++]
        app.noMailingAddressHomeless = results[i++]

        app.dep1CountryOfBirth = results[i++]
        app.dep1DateOfBirth = (results[i] == null || results[i].trim().isEmpty() ? null : LocalDate.parse(results[i], dateFormat))
        i++

        app.dep1FirstName = results[i++]
        app.dep1Gender = results[i++]
        app.dep1LastName = results[i++]
        app.dep1NoFirstName = (results[i++] == 'Y')
        app.dep1Relationship = results[i++]
        app.dep10CountryOfBirth = results[i++]
        app.dep10DateOfBirth = (results[i] == null || results[i].trim().isEmpty() ? null : LocalDate.parse(results[i], dateFormat))
        i++

        app.dep10FirstName = results[i++]
        app.dep10Gender = results[i++]
        app.dep10LastName = results[i++]
        app.dep10NoFirstName = (results[i++] == 'Y')
        app.dep10Relationship = results[i++]
        app.dep2CountryOfBirth = results[i++]
        app.dep2DateOfBirth = (results[i] == null || results[i].trim().isEmpty() ? null : LocalDate.parse(results[i], dateFormat))
        i++

        app.dep2FirstName = results[i++]
        app.dep2Gender = results[i++]
        app.dep2LastName = results[i++]
        app.dep2NoFirstName = (results[i++] == 'Y')
        app.dep2Relationship = results[i++]
        app.dep3CountryOfBirth = results[i++]
        app.dep3DateOfBirth = (results[i] == null || results[i].trim().isEmpty() ? null : LocalDate.parse(results[i], dateFormat))
        i++

        app.dep3FirstName = results[i++]
        app.dep3Gender = results[i++]
        app.dep3LastName = results[i++]
        app.dep3NoFirstName = (results[i++] == 'Y')
        app.dep3Relationship = results[i++]
        app.dep4CountryOfBirth = results[i++]
        app.dep4DateOfBirth = (results[i] == null || results[i].trim().isEmpty() ? null : LocalDate.parse(results[i], dateFormat))
        i++

        app.dep4FirstName = results[i++]
        app.dep4Gender = results[i++]
        app.dep4LastName = results[i++]
        app.dep4NoFirstName = (results[i++] == 'Y')
        app.dep4Relationship = results[i++]
        app.dep5CountryOfBirth = results[i++]
        app.dep5DateOfBirth = (results[i] == null || results[i].trim().isEmpty() ? null : LocalDate.parse(results[i], dateFormat))
        i++

        app.dep5FirstName = results[i++]
        app.dep5Gender = results[i++]
        app.dep5LastName = results[i++]
        app.dep5NoFirstName = (results[i++] == 'Y')
        app.dep5Relationship = results[i++]
        app.dep6CountryOfBirth = results[i++]
        app.dep6DateOfBirth = (results[i] == null || results[i].trim().isEmpty() ? null : LocalDate.parse(results[i], dateFormat))
        i++

        app.dep6FirstName = results[i++]
        app.dep6Gender = results[i++]
        app.dep6LastName = results[i++]
        app.dep6NoFirstName = (results[i++] == 'Y')
        app.dep6Relationship = results[i++]
        app.dep7CountryOfBirth = results[i++]
        app.dep7DateOfBirth = (results[i] == null || results[i].trim().isEmpty() ? null : LocalDate.parse(results[i], dateFormat))
        i++

        app.dep7FirstName = results[i++]
        app.dep7Gender = results[i++]
        app.dep7LastName = results[i++]
        app.dep7NoFirstName = (results[i++] == 'Y')
        app.dep7Relationship = results[i++]
        app.dep8CountryOfBirth = results[i++]
        app.dep8DateOfBirth = (results[i] == null || results[i].trim().isEmpty() ? null : LocalDate.parse(results[i], dateFormat))
        i++

        app.dep8FirstName = results[i++]
        app.dep8Gender = results[i++]
        app.dep8LastName = results[i++]
        app.dep8NoFirstName = (results[i++] == 'Y')
        app.dep8Relationship = results[i++]
        app.dep9CountryOfBirth = results[i++]
        app.dep9DateOfBirth = (results[i] == null || results[i].trim().isEmpty() ? null : LocalDate.parse(results[i], dateFormat))
        i++

        app.dep9FirstName = results[i++]
        app.dep9Gender = results[i++]
        app.dep9LastName = results[i++]
        app.dep9NoFirstName = (results[i++] == 'Y')
        app.dep9Relationship = results[i++]
        app.educationalGoal = results[i++]
        app.email = results[i++]
        app.emailVerified = (results[i++] == 'Y')
        app.emailVerifiedTimestamp = (results[i] == null || results[i].trim().isEmpty() ? null : LocalDateTime.parse(results[i], dateTimeFormat))
        i++
        app.preferredMethodOfContact = results[i++]

        return continueParsing(app, i, results, dateFormat, dateTimeFormat)
    }

    // THe JVM has a max code-length of 65k per function. Which we somehow manage to reach.
    // The simple way to get around doing a massive refactor is to split the parsing into multiple functions.
    private InternationalApplication continueParsing(InternationalApplication app, Integer i, String[] results, DateTimeFormatter dateFormat, DateTimeFormatter dateTimeFormat) {

        app.emergencyContactAddressVerified = (results[i++] == 'Y')
        app.emergencyContactCity = results[i++]
        app.emergencyContactCountry = results[i++]
        app.emergencyContactEmail = results[i++]
        app.emergencyContactFirstName = results[i++]
        app.emergencyContactLastName = results[i++]
        app.emergencyContactNoFirstName = (results[i++] == 'Y')
        app.emergencyContactNonUsAddress = (results[i++] == 'Y')
        app.emergencyContactNonUsPostalCode = results[i++]
        app.emergencyContactNonUsProvince = results[i++]
        app.emergencyContactPhoneAuthTxt = (results[i++] == 'Y')
        app.emergencyContactPhoneExt = results[i++]
        app.emergencyContactPhoneNumber = results[i++]
        app.emergencyContactRelationship = results[i++]
        app.emergencyContactState = results[i++]
        app.emergencyContactStreet1 = results[i++]
        app.emergencyContactStreet2 = results[i++]
        app.emergencyContactZipCode = results[i++]
        app.engMonthsStudied = results[i++].toInteger()
        app.engProficiencyDate = (results[i] == null || results[i].trim().isEmpty() ? null : LocalDate.parse(results[i], dateFormat))
        i++
        app.engProficiencyOther = results[i++]
        app.engProficiencyPrerequisite = (results[i++] == 'Y')

        app.engProficiencyScore = results[i++]
        app.engProficiencyShowScore = (results[i++] == 'Y')
        app.engProficiencyType = results[i++]
        app.enrollStatus = results[i++]
        app.enrollTermCode = results[i++]
        app.enrollTermDescription = results[i++]
        app.esignature = (results[i++] == 'Y')
        app.expirationDate = (results[i] == null || results[i].trim().isEmpty() ? null : LocalDate.parse(results[i], dateFormat))
        i++
        app.faxNumberNumber = results[i++]
        app.firstname = results[i++]
        app.gender = results[i++]

        app.highestCompDate = (results[i] == null || results[i].trim().isEmpty() ? null : LocalDate.parse(results[i], dateFormat))
        i++
        app.highestEduLevel = results[i++]
        app.hispanic = (results[i++] == 'Y')
        app.hsAddressVerified = (results[i++] == 'Y')
        app.hsCds = results[i++]
        app.hsCdsFull = results[i++]
        app.hsCeeb = results[i++]
        app.hsCity = results[i++]
        app.hsCompDate = (results[i] == null || results[i].trim().isEmpty() ? null : LocalDate.parse(results[i], dateFormat))
        i++
        app.hsCountry = results[i++]
        app.hsDiplomaCert = results[i++]
        app.hsDiplomaCertDate = (results[i] == null || results[i].trim().isEmpty() ? null : LocalDate.parse(results[i], dateFormat))
        i++
        app.hsEduLevel = results[i++]
        app.hsEndDate = (results[i] == null || results[i].trim().isEmpty() ? null : LocalDate.parse(results[i], dateFormat))
        i++
        app.hsLang = results[i++]
        app.hsName = results[i++]
        app.hsNonUsAddress = (results[i++] == 'Y')
        app.hsNonUsPostalCode = results[i++]
        app.hsNonUsProvince = results[i++]
        app.hsNotListed = (results[i++] == 'Y')
        app.hsStartDate = (results[i] == null || results[i].trim().isEmpty() ? null : LocalDate.parse(results[i], dateFormat))
        i++
        app.hsState = results[i++]
        app.hsStreet1 = results[i++]
        app.hsStreet2 = results[i++]
        app.hsType = results[i++]
        app.hsZipCode = results[i++]
        app.i20ExpirationDate = (results[i] == null || results[i].trim().isEmpty() ? null : LocalDate.parse(results[i], dateFormat))
        i++
        app.i20IssuingSchoolName = results[i++]
        app.i94AdmissionNumber = results[i++]
        app.i94ExpirationDate = (results[i] == null || results[i].trim().isEmpty() ? null : LocalDate.parse(results[i], dateFormat))
        i++
        app.intended4YearMajor = results[i++]
        app.ipAddress = results[i++]
        app.issueDate = (results[i] == null || results[i].trim().isEmpty() ? null : LocalDate.parse(results[i], dateFormat))
        i++
        app.lastPage = results[i++]
        app.lastname = results[i++]
        app.mainPhoneAuthTxt = (results[i++] == 'Y')
        app.mainPhoneExt = results[i++]
        app.mainPhoneNumber = results[i++]
        app.mainphoneVerified = (results[i++] == 'Y')
        app.mainphoneVerifiedTimestamp = (results[i] == null || results[i].trim().isEmpty() ? null : LocalDateTime.parse(results[i], dateTimeFormat))
        i++

        app.majorCode = results[i++]
        app.majorDescription = results[i++]
        app.majorCategory = results[i++]
        app.cipCode = results[i++]

        app.middlename = results[i++]
        app.noI94ExpirationDate = (results[i++] == 'Y')
        app.noVisa = (results[i++] == 'Y')

        app.residesInUs = (results[i++] == 'Y')
        app.nonUsAddress = (results[i++] == 'Y')
        app.nonUsPermanentHomeAddressSameAsPermanent = (results[i++] == 'Y')
        app.nonUsPermanentHomeAddressVerified = (results[i++] == 'Y')
        app.nonUsPermanentHomeCity = results[i++]
        app.nonUsPermanentHomeCountry = results[i++]
        app.nonUsPermanentHomeNonUsAddress = (results[i++] == 'Y')
        app.nonUsPermanentHomeNonUsPostalCode = results[i++]
        app.nonUsPermanentHomeNonUsProvince = results[i++]
        app.nonUsPermanentHomeStreet1 = results[i++]
        app.nonUsPermanentHomeStreet2 = results[i++]
        app.noNonUsaPermAddressHomeless = (results[i++] == 'Y')

        app.nonUsPhoneAuthTxt = (results[i++] == 'Y')
        app.nonUsPhoneExt = results[i++]
        app.nonUsPhoneNumber = results[i++]
        app.nonUsPostalCode = results[i++]
        app.nonUsProvince = results[i++]
        app.numberOfDependents = results[i++].toInteger()
        app.numberOfPracticalTraining = results[i++].toInteger()
        app.otherfirstname = results[i++]
        app.otherlastname = results[i++]
        app.othermiddlename = results[i++]
        app.parentGuardianAddressVerified = (results[i++] == 'Y')
        app.parentGuardianCity = results[i++]
        app.parentGuardianCountry = results[i++]
        app.parentGuardianEmail = results[i++]
        app.parentGuardianFirstName = results[i++]
        app.parentGuardianLastName = results[i++]
        app.parentGuardianNoFirstName = (results[i++] == 'Y')
        app.parentGuardianNonUsAddress = (results[i++] == 'Y')
        app.parentGuardianNonUsPostalCode = results[i++]
        app.parentGuardianNonUsProvince = results[i++]
        app.parentGuardianPhoneAuthTxt = (results[i++] == 'Y')
        app.parentGuardianPhoneExt = results[i++]
        app.parentGuardianPhoneNumber = results[i++]
        app.parentGuardianRelationship = results[i++]
        app.parentGuardianState = results[i++]
        app.parentGuardianStreet1 = results[i++]
        app.parentGuardianStreet2 = results[i++]
        app.parentGuardianZipCode = results[i++]
        app.passportCountryOfIssuance = results[i++]
        app.passportExpirationDate = (results[i] == null || results[i].trim().isEmpty() ? null : LocalDate.parse(results[i], dateFormat))
        i++
        app.passportNotYet = (results[i++] == 'Y')
        app.passportNumber = results[i++]
        app.permAddrAddressVerified = (results[i++] == 'Y')
        app.permAddrCity = results[i++]
        app.permAddrCountry = results[i++]
        app.permAddrNonUsAddress = (results[i++] == 'Y')
        app.permAddrNonUsPostalCode = results[i++]
        app.permAddrNonUsProvince = results[i++]
        app.permAddrState = results[i++]
        app.permAddrStreet1 = results[i++]
        app.permAddrStreet2 = results[i++]
        app.permAddrZipCode = results[i++]
        app.noPermAddressHomeless = (results[i++] == 'Y')

        app.phoneAuthTxt = (results[i++] == 'Y')
        app.phoneExt = results[i++]
        app.phoneNumber = results[i++]
        app.preferredFirstname = results[i++]
        app.preferredLastname = results[i++]
        app.preferredMiddlename = results[i++]
        app.preferredName = (results[i++] == 'Y')
        app.presentlyStudyingInUs = (results[i++] == 'Y')
        app.primaryLanguage = results[i++]
        app.pt1AuthorizingSchool = results[i++]
        app.pt1EndDate = (results[i] == null || results[i].trim().isEmpty() ? null : LocalDate.parse(results[i], dateFormat))
        i++

        app.pt1StartDate = (results[i] == null || results[i].trim().isEmpty() ? null : LocalDate.parse(results[i], dateFormat))
        i++
        app.pt1Type = results[i++]
        app.pt2AuthorizingSchool = results[i++]
        app.pt2EndDate = (results[i] == null || results[i].trim().isEmpty() ? null : LocalDate.parse(results[i], dateFormat))
        i++

        app.pt2StartDate = (results[i] == null || results[i].trim().isEmpty() ? null : LocalDate.parse(results[i], dateFormat))
        i++
        app.pt2Type = results[i++]
        app.pt3AuthorizingSchool = results[i++]
        app.pt3EndDate = (results[i] == null || results[i].trim().isEmpty() ? null : LocalDate.parse(results[i], dateFormat))
        i++

        app.pt3StartDate = (results[i] == null || results[i].trim().isEmpty() ? null : LocalDate.parse(results[i], dateFormat))
        i++
        app.pt3Type = results[i++]
        app.pt4AuthorizingSchool = results[i++]
        app.pt4EndDate = (results[i] == null || results[i].trim().isEmpty() ? null : LocalDate.parse(results[i], dateFormat))
        i++

        app.pt4StartDate = (results[i] == null || results[i].trim().isEmpty() ? null : LocalDate.parse(results[i], dateFormat))
        i++
        app.pt4Type = results[i++]
        app.raceEthnicity = results[i++]
        app.raceEthnicFull = results[i++]
        app.raceGroup = results[i++]

        app.secondPhoneAuthTxt = (results[i++] == 'Y')
        app.secondPhoneExt = results[i++]
        app.secondPhoneNumber = results[i++]
        app.sevisIdNumber = results[i++]

        app.ssn = results[i++]
        app.ssnNo = (results[i++] == 'Y')
        app.ssnException = (results[i++] == 'Y')
        app.ssnLast4 = results[i++]
        app.ssnType = results[i++]

        app.state = results[i++]
        app.status = results[i++]
        app.street1 = results[i++]
        app.street2 = results[i++]
        app.suffix = results[i++]
        app.supPageCode = results[i++]
        app.termEndDate = (results[i] == null || results[i].trim().isEmpty() ? null : LocalDate.parse(results[i], dateFormat))
        i++
        app.termStartDate = (results[i] == null || results[i].trim().isEmpty() ? null : LocalDate.parse(results[i], dateFormat))
        i++
        app.tstmpCreate = (results[i] == null || results[i].trim().isEmpty() ? null : LocalDateTime.parse(results[i], dateTimeFormat))
        i++
        app.tstmpDownload = (results[i] == null || results[i].trim().isEmpty() ? null : LocalDateTime.parse(results[i], dateTimeFormat))
        i++
        app.tstmpSubmit = (results[i] == null || results[i].trim().isEmpty() ? null : LocalDateTime.parse(results[i], dateTimeFormat))
        i++
        app.tstmpUpdate = (results[i] == null || results[i].trim().isEmpty() ? null : LocalDateTime.parse(results[i], dateTimeFormat))
        i++
        app.visaType = results[i++]
        app.zipCode = results[i++]

        app.acceptedTerms = (results[i++] == 'Y')
        app.acceptedTermsTimestamp = (results[i] == null || results[i].trim().isEmpty() ? null : LocalDateTime.parse(results[i], dateTimeFormat))
        i++

        app.phoneType = results[i++]

        app.ipAddressAtAccountCreation = results[i++]
        app.ipAddressAtAppCreation = results[i++]

        app.idmeConfirmationTimestamp = (results[i] == null || results[i].trim().isEmpty() ? null : LocalDateTime.parse(results[i], dateTimeFormat))
        i++

        app.idmeOptinTimestamp = (results[i] == null || results[i].trim().isEmpty() ? null : LocalDateTime.parse(results[i], dateTimeFormat))
        i++

        app.idmeWorkflowStatus = results[i++]

        app.sisProcessedFlag = results[i++]

        app.tstmpSISProcessed = (results[i] == null || results[i].trim().isEmpty() ? null : LocalDateTime.parse(results[i], dateTimeFormat))
        i++
        app.sisProcessedNotes = results[i]

        return app
    }

    /**
     * Converts a string array into an International Application's Supplement Questions Object
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