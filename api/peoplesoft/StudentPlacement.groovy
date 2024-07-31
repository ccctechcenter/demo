
package api.peoplesoft

import com.ccctc.adaptor.exception.EntityNotFoundException
import com.ccctc.adaptor.exception.InternalServerException
import com.ccctc.adaptor.exception.InvalidRequestException
import com.ccctc.adaptor.model.placement.StudentPlacementData
import com.ccctc.adaptor.util.MisEnvironment
import org.apache.commons.lang.StringUtils
import org.slf4j.Logger
import org.slf4j.LoggerFactory
import org.springframework.core.env.Environment

import java.time.Instant
import java.time.ZoneOffset
import java.time.LocalDateTime
import java.time.format.DateTimeFormatter

/**
 * <h1>Student Placement Peoplesoft Interface</h1>
 * <summary>
 *     <p>Provides a Wrapper around the native peoplesoft java API for:</p>
 *     <ol>
 *         <li>Storing and populating Student Placement data into a custom peoplesoft staging table</li>
 *         <li>Retrieving Student Placement data from a custom peoplesoft staging table</li>
 *     <ol>
 *     <p>Most logic for College Adaptor's Peoplesoft support is found within each Peoplesoft instance itself as
 *        this renders the highest level of transparency to each college for code-reviews and deployment authorization.
 *        See App Designer/PeopleTools projects latest API info
 *     </p>
 * </summary>
 *
 * @version 3.4.0
 *
 */
class StudentPlacement {

    protected final static Logger log = LoggerFactory.getLogger(StudentPlacement.class)

    //****** Populated by injection in Groovy Service Implementation ****
    Environment environment

    /**
     * Default Constructor needed since we also specify an alternative Constructor
     */
    StudentPlacement(){
        // do nothing, except be available
    }

    /**
     * Alternative Constructor to set the environment during initialization.
     * Useful for when other groovy PS APIs need to use these Functions that depend on environment
     */
    StudentPlacement(Environment e) {
        this.environment = e
    }

    /**
     * Gets Student Placement Data from a Peoplesoft staging table using the CCTC_StudentPlacement peoplesoft get method
     * @param misCode The College Code used for grabbing college specific settings from the environment
     * @param cccId The california community colleges student id to retrieve data for
     * @param theSSID The statewide student Id to retrieve data for (optional)
     */
    StudentPlacementData get(String misCode, String cccId, String theSSID) {
        log.debug("get: getting student placement data")

        //****** Validate parameters ****
        if (!misCode) {
            throw new InvalidRequestException(InvalidRequestException.Errors.invalidRequest, "Mis Code cannot be null or blank")
        }
        if (!cccId) {
            throw new InvalidRequestException(InvalidRequestException.Errors.invalidRequest, "californiaCommunityCollegeId cannot be null or blank")
        }

        log.debug("get: params ok; building method params")
        //****** Build parameters to send to our custom Peopletools API ****
        String packageName = "CCTC_PLACEMENT_PKG"
        String className = "CCTC_StudentPlacements"
        String methodName = "getPlacement"

        String[] args = []

        args += misCode
        args += cccId
        args += theSSID

        StudentPlacementData result = new StudentPlacementData()

        log.debug("get: attempting to get a new peoplesoft Session")
        //****** Create Connection to Peoplesoft and call remote method ******
        def peoplesoftSession = api.peoplesoft.PSConnection.getSession(environment, misCode)
        try {
            log.debug("get: calling the remote peoplesoft method")
            String[] placementData = api.peoplesoft.PSConnection.callMethod(peoplesoftSession, packageName, className, methodName, "<", args)
            if(placementData == null || placementData.length <= 0) {
                throw new EntityNotFoundException()
            }

            result = ConvertStringArrayToAStudentPlacementData(placementData)

            if(result == null) {
                throw new InternalServerException(InternalServerException.Errors.sisQueryError, "could not parse string data to student placement object")
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
        return result
    }

    /**
     * Sets Student Placement Data into a Peoplesoft staging table using the CCTC_StudentPlacement peoplesoft set method
     * @param misCode The College Code used for grabbing college specific settings from the environment
     * @param placementData The placement data to set into the staging table
     */
    void post(String misCode, StudentPlacementData placementData) {

        log.debug("post: setting student placement data")

        //****** Validate parameters ****
        if (placementData == null) {
            throw new InvalidRequestException(InvalidRequestException.Errors.invalidRequest, "Student Placement data cannot be null")
        }
        if (!placementData.californiaCommunityCollegeId) {
            throw new InvalidRequestException(InvalidRequestException.Errors.invalidRequest, "californiaCommunityCollegeId cannot be null or blank")
        }

        log.debug("post: params ok; building method params")
        //****** Build parameters to send to our custom Peopletools API ****
        String packageName = "CCTC_PLACEMENT_PKG"
        String className = "CCTC_StudentPlacements"
        String methodName = "setPlacement"

        String[] args = []

        args += placementData.californiaCommunityCollegeId
        args += placementData.statewideStudentId
        args += placementData.miscode
        args += placementData.placementStatus
        args += PSParameter.ConvertIntegerToCleanString(placementData.dataSource)

        args += PSParameter.ConvertIntegerToCleanString(placementData.english)
        args += PSParameter.ConvertIntegerToCleanString(placementData.slam)
        args += PSParameter.ConvertIntegerToCleanString(placementData.stem)

        args += PSParameter.ConvertBoolToCleanString(placementData.isAlgI)
        args += PSParameter.ConvertBoolToCleanString(placementData.isAlgII)
        args += PSParameter.ConvertBoolToCleanString(placementData.trigonometry)
        args += PSParameter.ConvertBoolToCleanString(placementData.preCalculus)
        args += PSParameter.ConvertBoolToCleanString(placementData.calculus)

        args += PSParameter.ConvertBoolToCleanString(placementData.completedEleventhGrade)
        args += PSParameter.ConvertFloatToCleanString(placementData.cumulativeGradePointAverage)

        args += PSParameter.ConvertIntegerToCleanString(placementData.englishCompletedCourseId)
        args += PSParameter.ConvertStringToCleanString(placementData.englishCompletedCourseGrade)

        args += PSParameter.ConvertIntegerToCleanString(placementData.mathematicsCompletedCourseId)
        args += PSParameter.ConvertStringToCleanString(placementData.mathematicsCompletedCourseGrade)

        args += PSParameter.ConvertIntegerToCleanString(placementData.mathematicsPassedCourseId)
        args += PSParameter.ConvertStringToCleanString(placementData.mathematicsPassedCourseGrade)

        args += PSParameter.ConvertLongToCleanString(placementData.appId)

        args += PSParameter.ConvertStringToCleanString(placementData.highestGradeCompleted)

        DateTimeFormatter dateTimeFormat = DateTimeFormatter.ofPattern("yyyy-MM-dd-HH.mm.ss.SSSSSS")

        LocalDateTime ERPTransmitDT = null
        if(placementData.tstmpERPTransmit != null) {
            // manually convert long to LocalDateTime
            Instant instant = Instant.ofEpochMilli(placementData.tstmpERPTransmit)
            ERPTransmitDT = LocalDateTime.ofInstant(instant, ZoneOffset.UTC)
        }
        args += PSParameter.ConvertDateTimeToCleanString(ERPTransmitDT, dateTimeFormat)

        args += PSParameter.ConvertDateTimeToCleanString(placementData.tstmpSISTransmit, dateTimeFormat)

        log.debug("post: attempting to get a new peoplesoft Session")
        //****** Create Connection to Peoplesoft and call remote method ******
        def peoplesoftSession = api.peoplesoft.PSConnection.getSession(environment, misCode)
        try {
            log.debug("post: calling the remote peoplesoft method")
            String[] rowsAffected = api.peoplesoft.PSConnection.callMethod(peoplesoftSession, packageName, className, methodName, "<", args)
            if(rowsAffected.length != 1) {
                throw new InternalServerException(InternalServerException.Errors.sisQueryError, "invalid number of rows affected. empty array")
            }
            if(!StringUtils.equalsIgnoreCase(rowsAffected[0], "1")) {
                throw new InternalServerException(InternalServerException.Errors.sisQueryError, "invalid number of rows affected [" + rowsAffected[0] + "]")
            }

            String dataEventsEnabled = MisEnvironment.getProperty(environment, misCode, "peoplesoft.dataEvents.enabled")
            if(StringUtils.equalsIgnoreCase(dataEventsEnabled,"true")) {
                try {
                    log.debug("post: triggering the remote peoplesoft event onStudentPlacementInserted")

                    args = []
                    args += placementData.californiaCommunityCollegeId
                    args += placementData.statewideStudentId
                    args += placementData.miscode

                    api.peoplesoft.PSConnection.triggerEvent(peoplesoftSession, "onStudentPlacementInserted", "<", args)
                }
                catch (Exception exc) {
                    log.warn("post: Triggering the onStudentPlacementInserted event failed with message [" + exc.getMessage() + "]")
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
            throw new InternalServerException(InternalServerException.Errors.internalServerError, "Peoplesoft Error- " + messageToThrow)
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
     * Converts a string array into an StudentPlacementData Object
     * @param results The string array to convert to an Student Placement Data Object
     * @retuns a Student Placement Data object with each field populated from the string array
     */
    protected StudentPlacementData ConvertStringArrayToAStudentPlacementData(String[] results) {
        if(results.length < 26) {
            return null
        }

        DateTimeFormatter dateTimeFormat = DateTimeFormatter.ofPattern("yyyy-MM-dd-HH.mm.ss.SSSSSS")

        Integer i = 0
        StudentPlacementData data = new StudentPlacementData()

        data.californiaCommunityCollegeId  = results[i++]
        data.statewideStudentId  = results[i++]
        data.miscode = results[i++]
        data.placementStatus = results[i++]
        data.dataSource = results[i++].toInteger()

        data.english = results[i++].toInteger()
        data.slam = results[i++].toInteger()
        data.stem = results[i++].toInteger()

        data.isAlgI = (results[i++] == 'Y')
        data.isAlgII = (results[i++] == 'Y')
        data.trigonometry = (results[i++] == 'Y')
        data.preCalculus = (results[i++] == 'Y')
        data.calculus = (results[i++] == 'Y')

        data.completedEleventhGrade = (results[i++] == 'Y')
        data.cumulativeGradePointAverage = results[i++].toFloat()

        data.englishCompletedCourseId = results[i++].toInteger()
        data.englishCompletedCourseGrade = results[i++]

        data.mathematicsCompletedCourseId = results[i++].toInteger()
        data.mathematicsCompletedCourseGrade = results[i++]

        data.mathematicsPassedCourseId = results[i++].toInteger()
        data.mathematicsPassedCourseGrade = results[i++]

        data.appId = (results[i] ? results[i].toLong() : null)
        i++

        data.highestGradeCompleted = results[i++]

        LocalDateTime ERPTransmit = (results[i] == null || results[i].trim().isEmpty() ? null : LocalDateTime.parse(results[i], dateTimeFormat))
        data.tstmpERPTransmit = ERPTransmit.atOffset(ZoneOffset.UTC).toInstant().toEpochMilli()
        i++

        data.tstmpSISTransmit = (results[i] == null || results[i].trim().isEmpty() ? null : LocalDateTime.parse(results[i], dateTimeFormat))
        i++

        data.sisProcessedFlag = results[i++]

        data.tstmpSISProcessed = (results[i] == null || results[i].trim().isEmpty() ? null : LocalDateTime.parse(results[i], dateTimeFormat))
        i++
        data.sisProcessedNotes = results[i]

        return data
    }
}

