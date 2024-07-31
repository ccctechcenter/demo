package api.peoplesoft

import com.ccctc.adaptor.exception.EntityConflictException
import com.ccctc.adaptor.exception.InternalServerException
import com.ccctc.adaptor.exception.InvalidRequestException
import com.ccctc.adaptor.model.CourseExchangeEnrollment
import com.ccctc.adaptor.model.Section
import com.ccctc.adaptor.util.MisEnvironment
import org.apache.commons.lang.StringUtils
import org.slf4j.Logger
import org.slf4j.LoggerFactory
import org.springframework.core.env.Environment

/**
 * <h1>Course Exchange Financial Aid Units Peoplesoft Interface</h1>
 * <summary>
 *     <p>Provides a Wrapper around the native peoplesoft java API for:</p>
 *     <ol>
 *         <li>Adding Financial Aid data into a custom peoplesoft staging tables</li>
 *         <li>Retrieving Financial Aid data from a custom peoplesoft staging tables</li>
 *         <li>Removing Financial Aid data from a custom peoplesoft staging tables</li>
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
class FinancialAidUnits {

    protected final static Logger log = LoggerFactory.getLogger(FinancialAidUnits.class)

    //****** Populated by injection in Groovy Service Implementation ****
    Environment environment

    /**
     * Default Constructor needed since we also specify an alternative Constructor
     */
    FinancialAidUnits(){
        // do nothing, except be available
    }

    /**
     * Alternative Constructor to set the environment during initialization.
     * Useful for when other groovy PS APIs need to use these Functions that depend on environment
     */
    FinancialAidUnits(Environment e) {
        this.environment = e
    }

    /**
     * Retrieves a listing of all the course exchange financial aid units for the given student and term
     * @param misCode The College Code used for grabbing college specific settings from the environment (required)
     * @param cccId The California Community College Student Id to limit results to. (required)
     * @param sisTermId The internal/College-specific Term Id (aka strm) to limit results to. (required)
     * @return listing of matching financial aid unit records
     */
    List<com.ccctc.adaptor.model.FinancialAidUnits> getUnitsList(String misCode, String cccid, String sisTermId) {
        log.debug("getUnitsList: getting FA Units List for student and term")

        //****** Validate parameters ****
        if (!misCode) {
            throw new InvalidRequestException(InvalidRequestException.Errors.invalidSearchCriteria, "misCode cannot be null or blank")
        }
        if (!sisTermId) {
            throw new InvalidRequestException(InvalidRequestException.Errors.invalidSearchCriteria, "sisTermId cannot be null or blank")
        }
        if (!cccid) {
            throw new InvalidRequestException(InvalidRequestException.Errors.invalidSearchCriteria, "cccid cannot be null or blank")
        }

        log.debug("getUnitsList: params ok; building method params")

        //****** Build parameters to send to our custom Peopletools API ****
        String packageName = "CCTC_FINAID_PKG"
        String className = "CCTC_FinancialAidUnits"
        String methodName = "getList"

        String[] args = [
                PSParameter.ConvertStringToCleanString(cccid),
                PSParameter.ConvertStringToCleanString(sisTermId)
        ]

        String[] unitsData = []

        log.debug("getUnitsList: attempting to get a new peoplesoft Session")
        //****** Create Connection to Peoplesoft and call remote method ******
        def peoplesoftSession = api.peoplesoft.PSConnection.getSession(environment, misCode)
        try {
            log.debug("getUnitsList: calling the remote peoplesoft method")

            unitsData = api.peoplesoft.PSConnection.callMethod(peoplesoftSession, packageName, className, methodName, "<", args)
        }
        catch(psft.pt8.joa.JOAException e) {
            String messageToThrow = e.getMessage()
            if(peoplesoftSession != null) {
                String[] msgs = api.peoplesoft.PSConnection.getErrorMessages(peoplesoftSession)
                if(msgs && msgs.length > 0) {
                    messageToThrow = msgs[0]
                    msgs.each {
                        log.error("getUnitsList: retrieval failed: [" + it + "]")
                    }
                }
            }
            throw new InternalServerException(InternalServerException.Errors.sisQueryError, messageToThrow)
        }
        finally {
            log.debug("getUnitsList: disconnecting")
            if(peoplesoftSession != null) {
                peoplesoftSession.disconnect()
            }
        }

        List<com.ccctc.adaptor.model.FinancialAidUnits> faList = []

        if (unitsData && unitsData.length > 0) {
            log.debug("getUnitsList: parsing list")
            Integer i = 0
            while (i + 6 <= unitsData.size()) {

                String enrolledMisCode = unitsData[i++]
                String collegeName = unitsData[i++]
                String cid = unitsData[i++]
                String sisSectionId = unitsData[i++]
                String title = unitsData[i++]
                String unitTaken = unitsData[i++]

                String statusFlg = unitsData[i++]
                String statusDttm = unitsData[i++]
                String notes = unitsData[i++]
                String lastupdateOpr = unitsData[i++]
                String lastupdateDttm = unitsData[i++]

                com.ccctc.adaptor.model.FinancialAidUnits.Builder faBuilder = new com.ccctc.adaptor.model.FinancialAidUnits.Builder()
                CourseExchangeEnrollment.Builder ceEnrollmentBuilder = new CourseExchangeEnrollment.Builder()
                Section.Builder sectionBuilder = new Section.Builder()

                sectionBuilder
                        .sisTermId(sisTermId)
                        .sisSectionId(sisSectionId)
                        .title(title)

                ceEnrollmentBuilder
                        .misCode(enrolledMisCode)
                        .collegeName(collegeName)
                        .c_id(cid.toString() ?: null)
                        .section(sectionBuilder.build())
                        .units(unitTaken.toFloat())

                faBuilder
                        .misCode(misCode)
                        .cccid(cccid)
                        .sisTermId(sisTermId)
                        .ceEnrollment(ceEnrollmentBuilder.build())

                faList.add(faBuilder.build())
            }
        }

        log.debug("getUnitsList: done")
        return faList
    }

    /**
     * Submits a Financial Aid Units Record for insertion into the underlying peoplesoft staging table
     * @param misCode The College Code used for grabbing college specific settings from the environment (required)
     * @param faUnits The payload to submit to the underlying peoplesoft database (required)
     * @return the successfully created record
     */
    com.ccctc.adaptor.model.FinancialAidUnits post(String misCode, com.ccctc.adaptor.model.FinancialAidUnits faUnits) {
        log.debug("post: inserting FA Units Record")

        //****** Validate parameters ****
        if (!misCode) {
            throw new InvalidRequestException(InvalidRequestException.Errors.invalidRequest, "misCode cannot be null or blank")
        }

        if (!faUnits) {
            throw new InvalidRequestException(InvalidRequestException.Errors.invalidRequest, "FA Units payload cannot be null or blank")
        }

        String cccid = faUnits.getCccid()
        if (!cccid) {
            throw new InvalidRequestException(InvalidRequestException.Errors.invalidRequest, "CCC Id cannot be null or blank")
        }

        String sisTermId = faUnits.getSisTermId()
        if (!sisTermId) {
            throw new InvalidRequestException(InvalidRequestException.Errors.invalidRequest, "Term cannot be null or blank")
        }

        CourseExchangeEnrollment ceEnrollment = faUnits.getCeEnrollment()
        if (!ceEnrollment) {
            throw new InvalidRequestException(InvalidRequestException.Errors.invalidRequest, "CE enrollment cannot be null or blank")
        }

        String takenAtCollegeMisCode = ceEnrollment.getMisCode()
        if (!takenAtCollegeMisCode) {
            throw new InvalidRequestException(InvalidRequestException.Errors.invalidRequest, "MisCode (for taken at college) cannot be null or blank")
        }

        String takenAtCollegeName = ceEnrollment.getCollegeName()
        if (!takenAtCollegeName) {
            throw new InvalidRequestException(InvalidRequestException.Errors.invalidRequest, "College Name (for taken at college) cannot be null or blank")
        }

        String c_id = ceEnrollment.getC_id()
        if (!c_id) {
            throw new InvalidRequestException(InvalidRequestException.Errors.invalidRequest, "CID cannot be null or blank")
        }

        Float unitsTaken = ceEnrollment.getUnits()
        if (unitsTaken == null || unitsTaken <= 0) {
            throw new InvalidRequestException(InvalidRequestException.Errors.invalidRequest, "Units taken cannot be null or blank")
        }

        Section ceSection = ceEnrollment.getSection()
        if (!ceSection) {
            throw new InvalidRequestException(InvalidRequestException.Errors.invalidRequest, "CE Enrollment Section cannot be null or blank")
        }

        String sisSectionId = ceSection.getSisSectionId()
        if (!sisSectionId) {
            throw new InvalidRequestException(InvalidRequestException.Errors.invalidRequest, "Section Id (for taken at college) cannot be null or blank")
        }

        String sectionTitle = ceSection.getTitle()
        if (!sectionTitle) {
            throw new InvalidRequestException(InvalidRequestException.Errors.invalidRequest, "Section Title (for taken at college) cannot be null or blank")
        }

        log.debug("post: params ok; building method params")

        //****** Build parameters to send to our custom Peopletools API ****
        String packageName = "CCTC_FINAID_PKG"
        String className = "CCTC_FinancialAidUnits"
        String methodName = "getStudentUnits"

        String[] args = [
                PSParameter.ConvertStringToCleanString(cccid),
                PSParameter.ConvertStringToCleanString(sisTermId),
                PSParameter.ConvertStringToCleanString(takenAtCollegeMisCode),
                PSParameter.ConvertStringToCleanString(c_id)
        ]

        log.debug("post: attempting to get a new peoplesoft Session")
        //****** Create Connection to Peoplesoft and call remote method ******
        def peoplesoftSession = api.peoplesoft.PSConnection.getSession(environment, misCode)
        try {

            String[] existCheck = api.peoplesoft.PSConnection.callMethod(peoplesoftSession, packageName, className, methodName, "<", args)
            if(existCheck.length > 0) {
               throw new EntityConflictException("Record already exists")
            }

            packageName = "CCTC_FINAID_PKG"
            className = "CCTC_FinancialAidUnits"
            methodName = "setStudentUnits"

            args = [
                    PSParameter.ConvertStringToCleanString(cccid),
                    PSParameter.ConvertStringToCleanString(sisTermId),
                    PSParameter.ConvertStringToCleanString(takenAtCollegeMisCode),
                    PSParameter.ConvertStringToCleanString(takenAtCollegeName),
                    PSParameter.ConvertStringToCleanString(c_id),
                    PSParameter.ConvertStringToCleanString(sisSectionId),
                    PSParameter.ConvertStringToCleanString(sectionTitle),
                    PSParameter.ConvertFloatToCleanString(unitsTaken)
            ]

            log.debug("post: calling the remote peoplesoft method")

            String[] rowsAffected = api.peoplesoft.PSConnection.callMethod(peoplesoftSession, packageName, className, methodName, "<", args)
            if(rowsAffected.length != 1) {
                log.error("post: invalid number of rows affected. empty array")
                throw new InternalServerException(InternalServerException.Errors.sisQueryError, "invalid number of rows affected. empty array")
            }
            if(!StringUtils.equalsIgnoreCase(rowsAffected[0], "1")) {
                log.error("post: invalid number of rows affected [" + rowsAffected[0] + "]")
                throw new InternalServerException(InternalServerException.Errors.sisQueryError, "invalid number of rows affected [" + rowsAffected[0] + "]")
            }

            String dataEventsEnabled = MisEnvironment.getProperty(environment, misCode, "peoplesoft.dataEvents.enabled")
            if(StringUtils.equalsIgnoreCase(dataEventsEnabled, "true")) {
                try {
                    log.debug("post: triggering the remote peoplesoft event onFinAidUnitsInserted")

                    args = [
                        PSParameter.ConvertStringToCleanString(cccid),
                        PSParameter.ConvertStringToCleanString(sisTermId),
                        PSParameter.ConvertStringToCleanString(takenAtCollegeMisCode),
                        PSParameter.ConvertStringToCleanString(c_id)
                    ]
                    api.peoplesoft.PSConnection.triggerEvent(peoplesoftSession, "onFinAidUnitsInserted", "<", args)
                }
                catch (Exception exc) {
                    log.warn("post: Triggering the onFinAidUnitsInserted event failed with message [" + exc.getMessage() + "]")
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
                        log.error("post: insertion failed: [" + it + "]")
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
        return faUnits
    }

    /**
     * Removes all matching Financial Aid Unit records for the given student, term, course, and enrolled School
     * @param misCode The College Code used for grabbing college specific settings from the environment (required)
     * @param cccId The California Community College Student Id to limit results to. (required)
     * @param sisTermId The internal/College-specific Term Id (aka strm) to limit results to. (required)
     * @param enrolledMisCode The College Code at which the student is enrolled in the course exchange class (required)
     * @param cid The C-ID common course ID for which the student is enrolled in (required)
     */
    void delete(String misCode, String cccid, String sisTermId, String enrolledMisCode, String cid) {
        log.debug("delete: removing FA Units List for student, term, college, course")

        //****** Validate parameters ****
        if (!misCode) {
            throw new InvalidRequestException(InvalidRequestException.Errors.invalidSearchCriteria, "misCode cannot be null or blank")
        }
        if (!sisTermId) {
            throw new InvalidRequestException(InvalidRequestException.Errors.invalidSearchCriteria, "sisTermId cannot be null or blank")
        }
        if (!cccid) {
            throw new InvalidRequestException(InvalidRequestException.Errors.invalidSearchCriteria, "cccid cannot be null or blank")
        }
        if (!enrolledMisCode) {
            throw new InvalidRequestException(InvalidRequestException.Errors.invalidSearchCriteria, "enrolledMisCode cannot be null or blank")
        }
        if (!cid) {
            throw new InvalidRequestException(InvalidRequestException.Errors.invalidSearchCriteria, "cid cannot be null or blank")
        }

        log.debug("delete: params ok; building method params")

        //****** Build parameters to send to our custom Peopletools API ****
        String packageName = "CCTC_FINAID_PKG"
        String className = "CCTC_FinancialAidUnits"
        String methodName = "getStudentUnits"

        String[] args = [
                PSParameter.ConvertStringToCleanString(cccid),
                PSParameter.ConvertStringToCleanString(sisTermId),
                PSParameter.ConvertStringToCleanString(enrolledMisCode),
                PSParameter.ConvertStringToCleanString(cid)
        ]

        log.debug("delete: attempting to get a new peoplesoft Session")
        //****** Create Connection to Peoplesoft and call remote method ******
        def peoplesoftSession = api.peoplesoft.PSConnection.getSession(environment, misCode)
        try {
            log.debug("delete: calling the remote peoplesoft method")

            String[] existCheck = api.peoplesoft.PSConnection.callMethod(peoplesoftSession, packageName, className, methodName, "<", args)

            if(existCheck.length <= 0) {
                throw new InvalidRequestException(InvalidRequestException.Errors.invalidSearchCriteria, "Record not found")
            }

            packageName = "CCTC_FINAID_PKG"
            className = "CCTC_FinancialAidUnits"
            methodName = "removeStudentUnits"


            String[] rowsAffected = api.peoplesoft.PSConnection.callMethod(peoplesoftSession, packageName, className, methodName, "<", args)
            if(rowsAffected.length != 1) {
                log.error("delete: invalid number of rows affected. empty array")
                throw new InternalServerException(InternalServerException.Errors.sisQueryError, "invalid number of rows affected. empty array")
            }
            if(!StringUtils.equalsIgnoreCase(rowsAffected[0],"1")) {
                log.error("delete: invalid number of rows affected [" + rowsAffected[0] + "]")
                throw new InternalServerException(InternalServerException.Errors.sisQueryError, "invalid number of rows affected [" + rowsAffected[0] + "]")
            }

            String dataEventsEnabled = MisEnvironment.getProperty(environment, misCode, "peoplesoft.dataEvents.enabled")
            if(StringUtils.equalsIgnoreCase(dataEventsEnabled, "true")) {
                try {
                    log.debug("delete: triggering the remote peoplesoft event onFinAidUnitsRemoved")

                    args = [
                            PSParameter.ConvertStringToCleanString(cccid),
                            PSParameter.ConvertStringToCleanString(sisTermId),
                            PSParameter.ConvertStringToCleanString(enrolledMisCode),
                            PSParameter.ConvertStringToCleanString(cid)
                    ]
                    api.peoplesoft.PSConnection.triggerEvent(peoplesoftSession, "onFinAidUnitsRemoved", "<", args)
                }
                catch (Exception exc) {
                    log.warn("delete: Triggering the onFinAidUnitsRemoved event failed with message [" + exc.getMessage() + "]")
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
                        log.error("delete: removal failed: [" + it + "]")
                    }
                }
            }
            throw new InternalServerException(InternalServerException.Errors.internalServerError, messageToThrow)
        }
        finally {
            log.debug("delete: disconnecting")
            if(peoplesoftSession != null) {
                peoplesoftSession.disconnect()
            }
        }

        log.debug("delete: done")
        return
    }
}
