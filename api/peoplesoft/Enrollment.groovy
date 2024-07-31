package api.peoplesoft

import com.ccctc.adaptor.exception.EntityNotFoundException
import com.ccctc.adaptor.exception.InternalServerException
import com.ccctc.adaptor.exception.InvalidRequestException
import com.ccctc.adaptor.model.PrerequisiteStatus
import com.ccctc.adaptor.model.PrerequisiteStatusEnum
import com.ccctc.adaptor.util.MisEnvironment
import org.apache.commons.lang.StringUtils
import org.slf4j.Logger
import org.slf4j.LoggerFactory
import org.springframework.core.env.Environment
import com.ccctc.adaptor.model.EnrollmentStatus
import java.text.SimpleDateFormat
import java.time.ZoneId
import java.time.format.DateTimeFormatter

/**
 * <h1>Student Enrollment Peoplesoft Interface</h1>
 * <summary>
 *     <p>Provides a Wrapper around the native peoplesoft java API for:</p>
 *     <ol>
 *         <li>Submitting Enrollment Requests into legit peoplesoft tables</li>
 *         <li>Dropping Enrollments from legit peoplesoft tables</li>
 *         <li>Retrieving Current Enrollments from legit peoplesoft tables</li>
 *         <li>Retrieving Enrollment Prerequisite Status for a given class and date</li>
 *     <ol>
 *     <p>Most logic for College Adaptor's Peoplesoft support is found within each Peoplesoft instance itself as
 *        this renders the highest level of transparency to each college for code-reviews and deployment authorization.
 *        See App Designer/PeopleTools projects latest API info
 *     </p>
 * </summary>
 *
 * @version 3.3.0
 *
 */
class Enrollment {

    protected final static Logger log = LoggerFactory.getLogger(Enrollment.class)

    //****** Populated by injection in Groovy Service Implementation ****
    Environment environment

    /**
     * Default Constructor needed since we also specify an alternative Constructor
     */
    Enrollment(){
        // do nothing, except be available
    }

    /**
     * Alternative Constructor to set the environment during initialization.
     * Useful for when other groovy PS APIs need to use these Functions that depend on environment
     */
    Enrollment(Environment e) {
        this.environment = e
    }

    /**
     * Gets all the enrollment records from the legit Peoplesoft table using the CCTC_StudentEnrollment:getEnrollments
     * peoplesoft method
     * @param misCode The College Code used for grabbing college specific settings from the environment. (required)
     * @param sisTermId The internal/College-specific Term Id (aka strm) to limit results to. (optional)
     * @param cccId The California Community College Student Id to limit results to. This value is translated
     * to a sisPersonId as enrollments are based on sisPersonId. (required)
     * @param sisSectionId The internal/College-specific Class Section Id (aka class_nbr) to limit results to. (optional)
     * @return List of Enrollments matching the person and then term/section
     */
    List<com.ccctc.adaptor.model.Enrollment> getStudent(String misCode, String sisTermId, String cccid, String sisSectionId) {

        //****** Validate parameters ****
        if (!misCode) {
            throw new InvalidRequestException(InvalidRequestException.Errors.invalidSearchCriteria, "misCode cannot be null or blank")
        }
        if (!cccid) {
            throw new InvalidRequestException(InvalidRequestException.Errors.invalidSearchCriteria, "cccid cannot be null or blank")
        }

        api.peoplesoft.Identity identityApi = new api.peoplesoft.Identity(this.environment)
        String sisPersonId = identityApi.translateCCCIdToSisPersonId(misCode, cccid)
        if (!sisPersonId) {
            throw new InvalidRequestException(InvalidRequestException.Errors.invalidSearchCriteria, "student not found for given cccid")
        }
        return this.getEnrollments(misCode, sisTermId, sisPersonId, sisSectionId)
    }

    /**
     * Updates the enrollment record in the legit Peoplesoft table to Drop the students enrollment in the given class for the given term.
     * Uses the CCTC_StudentEnrollment:dropEnrollment peoplesoft method
     * @param misCode The College Code used for grabbing college specific settings from the environment. (required)
     * @param cccId The California Community College Student Id to limit results to. This value is translated
     * to a sisPersonId as enrollments are based on sisPersonId. (required)
     * @param sisSectionId The internal/College-specific Class Section Id (aka class_nbr) to limit results to. (required)
     * @param sisTermId The internal/College-specific Term Id (aka strm) to limit results to. (required)
     * @param enrollment The full enrollment object to validate the other (section, term, person) params against (required)
     * @return The updated Enrollment record; null if dropped
     */
    com.ccctc.adaptor.model.Enrollment put(String misCode, String cccid, String sisSectionId, String sisTermId, com.ccctc.adaptor.model.Enrollment enrollment) {

        log.debug("put: dropping enrollment. validating parameters")

        //****** Validate parameters ****
        if (!misCode) {
            throw new InvalidRequestException(InvalidRequestException.Errors.invalidSearchCriteria, "misCode cannot be null or blank")
        }
        if (!sisTermId) {
            throw new InvalidRequestException(InvalidRequestException.Errors.invalidSearchCriteria, "term cannot be null or blank")
        }
        if (!sisSectionId) {
            throw new InvalidRequestException(InvalidRequestException.Errors.invalidSearchCriteria, "section cannot be null or blank")
        }

        if (!enrollment) {
            throw new InvalidRequestException(InvalidRequestException.Errors.invalidSearchCriteria, "enrollment cannot be null or blank")
        }

        if (enrollment.getEnrollmentStatus() && enrollment.getEnrollmentStatus() != EnrollmentStatus.Dropped) {
            throw new InvalidRequestException(InvalidRequestException.Errors.generalEnrollmentError, "Enrollment Status not supported")
        }

        if ((enrollment.getCccid() && cccid != enrollment.getCccid()) ||
                (enrollment.getSisTermId() && sisTermId != enrollment.getSisTermId()) ||
                (enrollment.getSisSectionId() && sisSectionId != enrollment.getSisSectionId())) {
            throw new InvalidRequestException(InvalidRequestException.Errors.generalEnrollmentError, "You may not update cccid or sisTermId or sisSectionId")
        }

        String sisPersonId = enrollment.getSisPersonId()
        if (!sisPersonId) {
            log.debug("put: sisPersonId not provided; attempting to translate from cccid")
            if(!cccid) {
                throw new InvalidRequestException(InvalidRequestException.Errors.generalEnrollmentError, "cccid cannot be null or blank if sisPersonId not also provided")
            }
            api.peoplesoft.Identity identityApi = new api.peoplesoft.Identity(this.environment)
            sisPersonId = identityApi.translateCCCIdToSisPersonId(misCode, cccid)
            if(!sisPersonId) {
                throw new InvalidRequestException(InvalidRequestException.Errors.generalEnrollmentError, "No Student found for cccid")
            }
        }

        log.debug("put: params ok; building method params")
        //****** Build parameters to send to our custom Peopletools API ****
        String packageName = "CCTC_ENROLLMENT_PKG"
        String className = "CCTC_StudentEnrollment"
        String methodName = "dropEnrollment"

        String[] args = [
                PSParameter.ConvertStringToCleanString(sisPersonId),
                PSParameter.ConvertStringToCleanString(sisTermId),
                PSParameter.ConvertStringToCleanString(sisSectionId),
                PSParameter.ConvertStringToCleanString(MisEnvironment.getProperty(environment, misCode,"peoplesoft.global.institution")),
                PSParameter.ConvertStringToCleanString(MisEnvironment.getProperty(environment, misCode,"peoplesoft.global.acad_career")),
                PSParameter.ConvertStringToCleanString(MisEnvironment.getProperty(environment, misCode,"peoplesoft.enrollment.drop.action_reason"))
        ]

        log.debug("put: attempting to get a new peoplesoft Session")
        //****** Create Connection to Peoplesoft and call remote method ******
        def peoplesoftSession = api.peoplesoft.PSConnection.getSession(environment, misCode)
        try {
            log.debug("put: calling the remote peoplesoft method")
            String[] results = api.peoplesoft.PSConnection.callMethod(peoplesoftSession, packageName, className, methodName, "<", args)

            if(results.length != 2) {
                log.error("put: invalid results length")
                throw new InternalServerException(InternalServerException.Errors.sisQueryError, "invalid results length")
            }
            String requestId = results[0]
            if(!requestId) {
                log.error("put: invalid requestId when submitting drop enrl req [" + requestId + "]")
                throw new InternalServerException(InternalServerException.Errors.sisQueryError, "invalid requestId [" + requestId + "]")
            }

            String statusCode = results[1]
            if(!statusCode){
                log.error("put: empty Enrollment engine request status")
                throw new InternalServerException(InternalServerException.Errors.sisQueryError, "empty Enrollment engine request status")
            }

            Integer max_tries = 15
            while(statusCode.equalsIgnoreCase("P") && max_tries > 0){
                //enrollment request still processing
                // wait a half a second and check the status again
                Thread.sleep(500)

                methodName = "checkEnrollmentRequest"
                args = [
                        PSParameter.ConvertStringToCleanString(sisPersonId),
                        PSParameter.ConvertStringToCleanString(sisTermId),
                        PSParameter.ConvertStringToCleanString(sisSectionId),
                        PSParameter.ConvertStringToCleanString(MisEnvironment.getProperty(environment, misCode,"peoplesoft.global.institution")),
                        PSParameter.ConvertStringToCleanString(MisEnvironment.getProperty(environment, misCode,"peoplesoft.global.acad_career")),
                        PSParameter.ConvertStringToCleanString(requestId)
                ]
                String[] statusResults = api.peoplesoft.PSConnection.callMethod(peoplesoftSession, packageName, className, methodName, "<", args)
                statusCode = statusResults[0]

                if(!statusCode){
                    log.error("put: empty Enrollment engine request status")
                    throw new InternalServerException(InternalServerException.Errors.sisQueryError, "empty Enrollment engine request status")
                }
                max_tries = max_tries - 1
            }

            if(statusCode == "E") {
                // enrollment engine reports an error
                // lets find out what the error was

                methodName = "getEnrollmentErrorMessage"
                args = [
                        PSParameter.ConvertStringToCleanString(sisPersonId),
                        PSParameter.ConvertStringToCleanString(sisTermId),
                        PSParameter.ConvertStringToCleanString(sisSectionId),
                        PSParameter.ConvertStringToCleanString(MisEnvironment.getProperty(environment, misCode,"peoplesoft.global.institution")),
                        PSParameter.ConvertStringToCleanString(MisEnvironment.getProperty(environment, misCode,"peoplesoft.global.acad_career")),
                        PSParameter.ConvertStringToCleanString(requestId)
                ]
                String[] errorMessages = api.peoplesoft.PSConnection.callMethod(peoplesoftSession, packageName, className, methodName, "<", args)
                if (errorMessages && errorMessages.length > 0) {
                    errorMessages.each {
                        log.error("put: Enrollment engine error: " + it)
                    }
                }

                throw new InternalServerException(InternalServerException.Errors.sisQueryError, "Enrollment request denied by Enrollment Engine")
            }

            String dataEventsEnabled = MisEnvironment.getProperty(environment, misCode, "peoplesoft.dataEvents.enabled")
            if(StringUtils.equalsIgnoreCase(dataEventsEnabled, "true")) {
                try {
                    log.debug("put: triggering the remote peoplesoft event onRequestEnrollmentDropped")

                    args = [ requestId ]

                    api.peoplesoft.PSConnection.triggerEvent(peoplesoftSession, "onRequestEnrollmentDropped", "<", args)
                }
                catch (Exception exc) {
                    if(peoplesoftSession != null) {
                        String[] msgs = api.peoplesoft.PSConnection.getErrorMessages(peoplesoftSession)
                        if(msgs && msgs.length > 0) {
                            msgs.each {
                                log.warn("put: Triggering the onRequestEnrollmentDropped event failed: [" + it + "]")
                            }
                        } else {
                            log.warn("put: Triggering the onRequestEnrollmentDropped event failed with message [" + exc.getMessage() + "]")
                        }
                    }
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
                        log.error("put: enrollment request failed: [" + it + "]")
                    }
                }
            }
            throw new InternalServerException(InternalServerException.Errors.internalServerError, messageToThrow)
        }
        finally {
            log.debug("put: disconnecting")
            if(peoplesoftSession != null) {
                peoplesoftSession.disconnect()
            }
        }

        log.debug("put: done")

        List<com.ccctc.adaptor.model.Enrollment> getResults = this.getEnrollments(misCode, sisTermId, sisPersonId, sisSectionId)
        if(getResults && getResults.size() > 0) {
            return getResults[0]
        }

        return null
    }

    /**
     * Request the student be enrolled in the given class for the given term per the Enrollment Request Peoplesoft table
     * Uses the CCTC_StudentEnrollment:requestEnrollment peoplesoft method
     * @param misCode The College Code used for grabbing college specific settings from the environment. (required)
     * @param enrollment The requested enrollment object to provide the section, term, and person params. (required)
     * @return the first enrollment matching if any; null otherwise.
     * @Note return value may be null since We only submit an enrollment request. There will not be an enrollment record
     * until the app engine processes the enrollment request. even then, the request my be denied.
     */
    com.ccctc.adaptor.model.Enrollment post(String misCode, com.ccctc.adaptor.model.Enrollment enrollment) {

        log.debug("post: requesting student enrollment")

        //****** Validate parameters ****
        if (!misCode) {
            throw new InvalidRequestException(InvalidRequestException.Errors.invalidSearchCriteria, "misCode cannot be null or blank")
        }

        if (enrollment == null) {
            throw new InvalidRequestException(InvalidRequestException.Errors.invalidRequest, "Enrollment cannot be null or blank")
        }

        if (enrollment.getEnrollmentStatus() && enrollment.getEnrollmentStatus() != EnrollmentStatus.Enrolled) {
            throw new InvalidRequestException(InvalidRequestException.Errors.generalEnrollmentError, "Enrollment Status not supported")
        }

        if (!enrollment.getSisTermId()) {
            throw new InvalidRequestException(InvalidRequestException.Errors.generalEnrollmentError, "Enrollment term cannot be null or blank")
        }

        if (!enrollment.getSisSectionId()) {
            throw new InvalidRequestException(InvalidRequestException.Errors.generalEnrollmentError, "Enrollment class is required")
        }

        String sisPersonId = enrollment.getSisPersonId()
        if (!sisPersonId) {
            log.debug("post: personId not provided; attempting to translate from cccid")
            String cccid = enrollment.getCccid()
            if(!cccid) {
                throw new InvalidRequestException(InvalidRequestException.Errors.generalEnrollmentError, "cccid cannot be null or blank if sisPersonId not also provided")
            }
            api.peoplesoft.Identity identityApi = new api.peoplesoft.Identity(this.environment)
            sisPersonId = identityApi.translateCCCIdToSisPersonId(misCode, cccid)
            if(!sisPersonId) {
                throw new InvalidRequestException(InvalidRequestException.Errors.generalEnrollmentError, "No Student found for cccid")
            }
        }


        log.debug("post: params ok; building method params")
        //****** Build parameters to send to our custom Peopletools API ****
        String packageName = "CCTC_ENROLLMENT_PKG"
        String className = "CCTC_StudentEnrollment"
        String methodName = "requestEnrollment"

        String[] args = [
                PSParameter.ConvertStringToCleanString(sisPersonId),
                PSParameter.ConvertStringToCleanString(enrollment.getSisTermId()),
                PSParameter.ConvertStringToCleanString(enrollment.getSisSectionId()),
                PSParameter.ConvertStringToCleanString(MisEnvironment.getProperty(environment, misCode,"peoplesoft.global.institution")),
                PSParameter.ConvertStringToCleanString(MisEnvironment.getProperty(environment, misCode,"peoplesoft.global.acad_career")),
                PSParameter.ConvertStringToCleanString(MisEnvironment.getProperty(environment, misCode,"peoplesoft.enrollment.add.action_reason"))
        ]

        log.debug("post: attempting to get a new peoplesoft Session")
        //****** Create Connection to Peoplesoft and call remote method ******
        def peoplesoftSession = api.peoplesoft.PSConnection.getSession(environment, misCode)
        try {
            log.debug("post: calling the remote peoplesoft method")
            String[] results = api.peoplesoft.PSConnection.callMethod(peoplesoftSession, packageName, className, methodName, "<", args)
            if(results.length != 2) {
                log.error("post: invalid results length")
                throw new InternalServerException(InternalServerException.Errors.sisQueryError, "invalid results length")
            }
            String requestId = results[0]
            if(!requestId) {
                log.error("post: invalid requestId when submitting enrl req [" + requestId + "]")
                throw new InternalServerException(InternalServerException.Errors.sisQueryError, "invalid requestId [" + requestId + "]")
            }

            String statusCode = results[1]
            if(!statusCode){
                log.error("post: empty Enrollment engine request status")
                throw new InternalServerException(InternalServerException.Errors.sisQueryError, "empty Enrollment engine request status")
            }

            Integer max_tries = 15
            while(statusCode.equalsIgnoreCase("P") && max_tries > 0){
                //enrollment request still processing
                // wait a half a second and check the status again
                Thread.sleep(500)

                methodName = "checkEnrollmentRequest"
                args = [
                        PSParameter.ConvertStringToCleanString(sisPersonId),
                        PSParameter.ConvertStringToCleanString(enrollment.getSisTermId()),
                        PSParameter.ConvertStringToCleanString(enrollment.getSisSectionId()),
                        PSParameter.ConvertStringToCleanString(MisEnvironment.getProperty(environment, misCode,"peoplesoft.global.institution")),
                        PSParameter.ConvertStringToCleanString(MisEnvironment.getProperty(environment, misCode,"peoplesoft.global.acad_career")),
                        PSParameter.ConvertStringToCleanString(requestId)
                ]
                String[] statusResults = api.peoplesoft.PSConnection.callMethod(peoplesoftSession, packageName, className, methodName, "<", args)
                statusCode = statusResults[0]

                if(!statusCode){
                    log.error("post: empty Enrollment engine request status")
                    throw new InternalServerException(InternalServerException.Errors.sisQueryError, "empty Enrollment engine request status")
                }
                max_tries = max_tries - 1
            }

            if(statusCode == "E") {
                // enrollment engine reports an error
                // lets find out what the error was
                methodName = "getEnrollmentErrorMessage"
                args = [
                        PSParameter.ConvertStringToCleanString(sisPersonId),
                        PSParameter.ConvertStringToCleanString(enrollment.getSisTermId()),
                        PSParameter.ConvertStringToCleanString(enrollment.getSisSectionId()),
                        PSParameter.ConvertStringToCleanString(MisEnvironment.getProperty(environment, misCode,"peoplesoft.global.institution")),
                        PSParameter.ConvertStringToCleanString(MisEnvironment.getProperty(environment, misCode,"peoplesoft.global.acad_career")),
                        PSParameter.ConvertStringToCleanString(requestId)
                ]
                String[] errorMessages = api.peoplesoft.PSConnection.callMethod(peoplesoftSession, packageName, className, methodName, "<", args)
                if (errorMessages && errorMessages.length > 0) {
                    errorMessages.each {
                        log.error("post: Enrollment engine error: " + it)
                    }
                }

                throw new InternalServerException(InternalServerException.Errors.sisQueryError, "Enrollment request denied by Enrollment Engine")
            }

            String dataEventsEnabled = MisEnvironment.getProperty(environment, misCode, "peoplesoft.dataEvents.enabled")
            if(StringUtils.equalsIgnoreCase(dataEventsEnabled,"true")) {
                try {
                    log.debug("post: triggering the remote peoplesoft event onEnrollmentRequestSubmitted")

                    args = [requestId]

                    api.peoplesoft.PSConnection.triggerEvent(peoplesoftSession, "onEnrollmentRequestSubmitted", "<", args)
                }
                catch (Exception exc) {
                    if(peoplesoftSession != null) {
                        String[] msgs = api.peoplesoft.PSConnection.getErrorMessages(peoplesoftSession)
                        if(msgs && msgs.length > 0) {
                            msgs.each {
                                log.warn("post: Triggering the onEnrollmentRequestSubmitted event failed: [" + it + "]")
                            }
                        } else {
                            log.warn("post: Triggering the onEnrollmentRequestSubmitted event failed with message [" + exc.getMessage() + "]")
                        }
                    }
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
                        log.error("post: enrollment request failed: [" + it + "]")
                    }
                }
            }

            InvalidRequestException.Errors eCode = InvalidRequestException.Errors.generalEnrollmentError
            if(StringUtils.equalsIgnoreCase(messageToThrow, "enrollment record already exists")) {
                eCode = InvalidRequestException.Errors.alreadyEnrolled
            }
            throw new InvalidRequestException(eCode, messageToThrow)
        }
        finally {
            log.debug("post: disconnecting")
            if(peoplesoftSession != null) {
                peoplesoftSession.disconnect()
            }
        }

        log.debug("post: done")

        List<com.ccctc.adaptor.model.Enrollment> getResults = this.getEnrollments(misCode, enrollment.getSisTermId(), sisPersonId, enrollment.getSisSectionId())
        if(getResults && getResults.size() > 0) {
            return getResults[0]
        }

        return null
    }

    /**
     * Retrieves all the enrollment records from the legit Peoplesoft table for the given class and the given term.
     * Uses the CCTC_StudentEnrollment:getEnrollments peoplesoft method
     * @param misCode The College Code used for grabbing college specific settings from the environment. (required)
     * @param sisTermId The internal/College-specific Term Id (aka strm) to limit results to. (required)
     * @param sisSectionId The internal/College-specific Class Section Id (aka class_nbr) to limit results to. (required)
     * @return List of Enrollments matching the Term and section specified
     */
    List<com.ccctc.adaptor.model.Enrollment> getSection(String misCode, String sisTermId, String sisSectionId) {

        //****** Validate parameters ****
        if (!sisSectionId) {
            throw new InvalidRequestException(InvalidRequestException.Errors.invalidSearchCriteria, "section cannot be null or blank")
        }
        if (!sisTermId) {
            throw new InvalidRequestException(InvalidRequestException.Errors.invalidSearchCriteria, "term cannot be null or blank")
        }
        if (!misCode) {
            throw new InvalidRequestException(InvalidRequestException.Errors.invalidSearchCriteria, "misCode cannot be null or blank")
        }

        return this.getEnrollments(misCode, sisTermId, null, sisSectionId)
    }

    /**
     * Gets all the enrollment records from the legit Peoplesoft table using
     * the CCTC_StudentEnrollment:getEnrollments peoplesoft method
     * @param misCode The College Code used for grabbing college specific settings from the environment. (required)
     * @param sisTermId The internal/College-specific Term Id (aka strm) to limit results to. (optional)
     * @param sisPersonId The internal/College-specific person Id (aka emplid) to limit results to. (optional)
     * @param sisSectionId The internal/College-specific Class Section Id (aka class_nbr) to limit results to. (optional)
     * @return List of Enrollments matching the Person, term and/or section specified
     * @Note While the above three params are all optional, if sisPersonId is not included, Term and Section must be.
     * e.g. Term/Section are only optional if Person is provided
     */
    protected List<com.ccctc.adaptor.model.Enrollment> getEnrollments(String misCode, String sisTermId, String sisPersonId, String sisSectionId) {


        log.debug("getEnrollments: building method params")

        //****** Build parameters to send to our custom Peopletools API ****
        String packageName = "CCTC_ENROLLMENT_PKG"
        String className = "CCTC_StudentEnrollment"
        String methodName = "getEnrollments"

        String[] args = [
                PSParameter.ConvertStringToCleanString(sisPersonId),
                PSParameter.ConvertStringToCleanString(sisTermId),
                PSParameter.ConvertStringToCleanString(sisSectionId),
                PSParameter.ConvertStringToCleanString(MisEnvironment.getProperty(environment, misCode,"peoplesoft.global.institution")),
                PSParameter.ConvertStringToCleanString(MisEnvironment.getProperty(environment, misCode,"peoplesoft.global.acad_career"))
        ]

        String[] enrlData = []
        log.debug("getEnrollments: attempting to get a new peoplesoft Session")
        //****** Create Connection to Peoplesoft and call remote method ******
        def peoplesoftSession = api.peoplesoft.PSConnection.getSession(environment, misCode)
        try {
            log.debug("getEnrollments: calling the remote peoplesoft method")

            enrlData = api.peoplesoft.PSConnection.callMethod(peoplesoftSession, packageName, className, methodName, "<", args)
        }
        catch(psft.pt8.joa.JOAException e) {
            String messageToThrow = e.getMessage()
            if(peoplesoftSession != null) {
                String[] msgs = api.peoplesoft.PSConnection.getErrorMessages(peoplesoftSession)
                if(msgs && msgs.length > 0) {
                    messageToThrow = msgs[0]
                    msgs.each {
                        log.error("getEnrollments: retrieval failed: [" + it + "]")
                    }
                }
            }
            throw new InternalServerException(InternalServerException.Errors.internalServerError, messageToThrow)
        }
        finally {
            log.debug("getEnrollments: disconnecting")
            if(peoplesoftSession != null) {
                peoplesoftSession.disconnect()
            }
        }

        List<com.ccctc.adaptor.model.Enrollment> retList = this.convertStringArrayToListOfEnrollment(misCode, enrlData)

        log.debug("getEnrollments: done")
        return retList
    }

    /**
     * Converts the string representation of, potentially multiple, Enrollment records into a list of Rich objects
     * @param misCode The College Code used to set the misCode in each object (required)
     * @param enrlData String array (to convert) with each enrollment represented as consecutive strings in array (required)
     */
    protected List<com.ccctc.adaptor.model.Enrollment> convertStringArrayToListOfEnrollment(String misCode, String[] enrlData) {

        List<com.ccctc.adaptor.model.Enrollment> enrollments = []

        if (enrlData == null || enrlData.length == 0) {
            return enrollments
        }

        SimpleDateFormat df = new SimpleDateFormat("yyyy-MM-dd")
        api.peoplesoft.Identity identityApi = new api.peoplesoft.Identity(this.environment)
        Integer i = 0
        while (i + 14 <= enrlData.length) {
            String cur_sisPersonId = enrlData[i++]
            String cur_termId = enrlData[i++]
            String cur_sisSectionId = enrlData[i++]
            String cur_stdntEnrlStatus = enrlData[i++]
            String cur_action_dt = enrlData[i++]
            String cur_untTaken = enrlData[i++]
            String cur_gradingBasisEnrl = enrlData[i++]
            String cur_auditGradeBasis = enrlData[i++]
            String cur_crseGradeOff = enrlData[i++]
            String cur_gradeDt = enrlData[i++]
            String cur_crseId = enrlData[i++]
            String cur_title = enrlData[i++]
            String cur_endDt = enrlData[i++]

            String cur_cid = enrlData[i++]

            // When getting all enrollments of a section, the cccId will be different for each student
            // so cant just use the cccId passed in
            // so translate from sisPersonId in result-set to cccId
            String cur_cccId = identityApi.translateSisPersonIdToCCCId(misCode, cur_sisPersonId)

            def builder = new com.ccctc.adaptor.model.Enrollment.Builder()
            builder.cccid(cur_cccId ?: null)
                    .sisPersonId(cur_sisPersonId)
                    .sisTermId(cur_termId)
                    .sisSectionId(cur_sisSectionId)
                    .enrollmentStatus(this.parseEnrollmentStatus(misCode, cur_stdntEnrlStatus))
                    .enrollmentStatusDate((cur_action_dt != "") ? df.parse(cur_action_dt) : null)
                    .units(cur_untTaken.toFloat() ?: 0)
                    .passNoPass(this.isPassNoPass(misCode, cur_gradingBasisEnrl))
                    .audit(cur_auditGradeBasis == 'Y' ? true : false)
                    .grade(cur_crseGradeOff ?: null)
                    .gradeDate((cur_gradeDt != "") ? df.parse(cur_gradeDt) : null)
                    .sisCourseId(cur_crseId ?: null)
                    .c_id(cur_cid ?: null)
                    .lastDateAttended((cur_endDt != "") ? df.parse(cur_endDt) : null)
                    .title(cur_title.trim() ?: null)
            enrollments += builder.build()
        } // while

        return enrollments
    }

    /**
     * Parses the Peoplsoft's enrollment status into rich EnrollmentStatus object
     * @param misCode The College Code used for grabbing college specific settings from the environment. (required)
     * @param enrlStatus student enrollment status to parse (required)
     */
    protected EnrollmentStatus parseEnrollmentStatus(String misCode, String enrlStatus){
        EnrollmentStatus result
        if(enrlStatus) {
            String configMapping = MisEnvironment.getProperty(environment, misCode, "peoplesoft.enrollment.mappings.enrollmentStatus.stdnt_enrl_status." + enrlStatus)
            if (configMapping) {
                EnrollmentStatus found = EnrollmentStatus.values().find {
                    return it.name().equalsIgnoreCase(configMapping)
                }
                if (!found) {
                    log.warn("Could not parse EnrollmentStatus [" + enrlStatus + "]")
                } else {
                    result = found
                }
            } else {
                log.warn("EnrollmentStatus [" + enrlStatus + "] mapping not found")
            }
        }
        return result
    }

    /**
     * Maps the Peoplsoft's grading basis to a boolean representing whether it is pass/NoPass
     * @param misCode The College Code used for grabbing college specific settings from the environment. (required)
     * @param gradingBasis grading basis to map (required)
     */
    protected Boolean isPassNoPass(String misCode, String gradingBasis){
        Boolean result
        if(gradingBasis) {
            String configMapping = MisEnvironment.getProperty(environment, misCode, "peoplesoft.enrollment.mappings.passNoPass.grading_basis." + gradingBasis)
            if (configMapping) {
                if(StringUtils.equalsIgnoreCase(configMapping, "true")) {
                    result = true
                } else if(StringUtils.equalsIgnoreCase(configMapping, "false") ){
                    result = false
                }
                else {
                    log.warn("Could not parse mapping for gradingBasis [" + gradingBasis + "]")
                }
            } else {
                log.warn("gradingBasis [" + gradingBasis + "] mapping not found")
            }
        }
        return result
    }

    /**
     * Gets the Prerequisite status enrollment records from the legit Peoplesoft table using
     * the CCTC_StudentEnrollment:getStudentPrerequisiteStatus peoplesoft method
     * @param misCode The College Code used for grabbing college specific settings from the environment. (required)
     * @param sisCourseId The internal/College-specific Course Id (subj-nbr) to limit results to. (required)
     * @param start The beginning date for the class that the student intends to attend (required)
     * @param cccId The California Community College Student Id to limit results to. This value is translated
     * to a sisPersonId as enrollments are based on sisPersonId. (required)
     * @return The Prerequisite Status for whether the specified student can enroll in the specified course
     * beginning the specified date
     */
    PrerequisiteStatus getPrereqStatus(String misCode, String sisCourseId, Date start, String cccid) {
        DateTimeFormatter dateFormat = DateTimeFormatter.ofPattern("yyyy-MM-dd")

        //****** Validate parameters ****
        if (!misCode) {
            throw new InvalidRequestException(InvalidRequestException.Errors.invalidSearchCriteria, "misCode cannot be null or blank")
        }

        if (!sisCourseId) {
            throw new InvalidRequestException(InvalidRequestException.Errors.invalidSearchCriteria, "sisCourseId cannot be null or blank")
        }

        if (!start) {
            throw new InvalidRequestException(InvalidRequestException.Errors.invalidSearchCriteria, "start Date cannot be null or blank")
        }

        if (!cccid) {
            throw new InvalidRequestException(InvalidRequestException.Errors.invalidSearchCriteria, "cccId cannot be null or blank")
        }

        api.peoplesoft.Identity identityApi = new api.peoplesoft.Identity(this.environment)
        String sisPersonId = identityApi.translateCCCIdToSisPersonId(misCode, cccid)
        if (!sisPersonId) {
            throw new InvalidRequestException(InvalidRequestException.Errors.invalidSearchCriteria, "student not found for given cccid")
        }

        log.debug("getPrereqStatus: building method params")

        //****** Build parameters to send to our custom Peopletools API ****
        String packageName = "CCTC_ENROLLMENT_PKG"
        String className = "CCTC_StudentEnrollment"
        String methodName = "getStudentPrerequisiteStatus"

        String[] args = [
                PSParameter.ConvertStringToCleanString(sisPersonId),
                PSParameter.ConvertDateToCleanString(start.toInstant().atZone(ZoneId.systemDefault()).toLocalDate(), dateFormat),
                PSParameter.ConvertStringToCleanString(sisCourseId),
                PSParameter.ConvertStringToCleanString(MisEnvironment.getProperty(environment, misCode,"peoplesoft.global.institution")),
                PSParameter.ConvertStringToCleanString(MisEnvironment.getProperty(environment, misCode,"peoplesoft.global.acad_career"))
        ]

        String[] preqData = []
        log.debug("getPrereqStatus: attempting to get a new peoplesoft Session")
        //****** Create Connection to Peoplesoft and call remote method ******
        def peoplesoftSession = api.peoplesoft.PSConnection.getSession(environment, misCode)
        try {
            log.debug("getPrereqStatus: calling the remote peoplesoft method")

            preqData = api.peoplesoft.PSConnection.callMethod(peoplesoftSession, packageName, className, methodName, "<", args)
        }
        catch(psft.pt8.joa.JOAException e) {
            String messageToThrow = e.getMessage()
            if(peoplesoftSession != null) {
                String[] msgs = api.peoplesoft.PSConnection.getErrorMessages(peoplesoftSession)
                if(msgs && msgs.length > 0) {
                    messageToThrow = msgs[0]
                    msgs.each {
                        log.error("getPrereqStatus: retrieval failed: [" + it + "]")
                    }
                }
            }
            throw new InternalServerException(InternalServerException.Errors.internalServerError, messageToThrow)
        }
        finally {
            log.debug("getPrereqStatus: disconnecting")
            if(peoplesoftSession != null) {
                peoplesoftSession.disconnect()
            }
        }

        if(preqData == null || preqData.length <= 0) {
            throw new EntityNotFoundException()
        }

        String prereq_status = preqData[0]
        String prereq_message = preqData[1]

        def builder = new com.ccctc.adaptor.model.PrerequisiteStatus.Builder()
        builder.status(prereq_status as PrerequisiteStatusEnum)
                .message(prereq_message)
        return builder.build()
    }
}
