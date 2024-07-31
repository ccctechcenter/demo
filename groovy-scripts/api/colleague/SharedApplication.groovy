package api.colleague

import api.colleague.model.DataWriteResult
import api.colleague.util.ColleagueUtils
import api.colleague.util.DataWriter
import com.ccctc.adaptor.exception.EntityConflictException
import com.ccctc.adaptor.exception.EntityNotFoundException
import com.ccctc.adaptor.exception.InternalServerException
import com.ccctc.adaptor.exception.InvalidRequestException
import com.ccctc.adaptor.model.apply.Application as StandardApp
import com.ccctc.adaptor.model.apply.SharedApplication as SharedApp
import com.ccctc.adaptor.util.ClassMap
import org.ccctc.colleaguedmiclient.model.CddEntry
import org.ccctc.colleaguedmiclient.model.ColleagueData
import org.ccctc.colleaguedmiclient.model.EntityMetadata
import org.ccctc.colleaguedmiclient.service.DmiCTXService
import org.ccctc.colleaguedmiclient.service.DmiDataService
import org.ccctc.colleaguedmiclient.service.EntityMetadataService
import org.ccctc.colleaguedmiclient.util.CddUtils
import org.slf4j.Logger
import org.slf4j.LoggerFactory
import org.springframework.core.env.Environment

import java.time.LocalDate
import java.time.LocalDateTime
import java.time.LocalTime

/**
 * <h1>Shared CCCApply Application Colleague Interface</h1>
 * <summary>
 *     <p>Provides:</p>
 *     <ol>
 *         <li>Storing and populating Shared Application into a single custom colleague staging table</li>
 *         <li>Retrieving Shared Applications from the custom colleague staging table</li>
 *     <ol>
 *     <p>Uses the DMI DataWriter service to insert the record.
 *     </p>
 * </summary>
 *
 * @version CAv4.10.0
 *
 */
class SharedApplication {

    protected final static Logger log = LoggerFactory.getLogger(SharedApplication.class)

    //****** Populated by colleagueInit ClassMap ****
    private DmiDataService dmiDataService
    private EntityMetadataService entityMetadataService
    private DataWriter dataWriter

    /**
     * Initialize services
     */
    void colleagueInit(String misCode, Environment environment, ClassMap services) {
        this.dmiDataService = services.get(DmiDataService.class)
        this.entityMetadataService = dmiDataService.getEntityMetadataService()

        DmiCTXService dmiCTXService = services.get(DmiCTXService.class)
        this.dataWriter = new DataWriter(dmiCTXService)
    }


    /**
     * Gets Standard Application Data from a Colleague staging table using the DMI client
     * @param misCode The College Code used for grabbing college specific settings from the environment
     * @param appId The application Id used when originally populating the data into the staging table
     * @returns A matching Standard Application
     */
    SharedApp get(String teachMisCode, Long appId) {

        log.debug("get: getting shared-application data")

        //****** Validate parameters ****
        if (!appId) {
            String errMsg = "get: Application Id cannot be null or blank"
            log.warn(errMsg)
            throw new InvalidRequestException(InvalidRequestException.Errors.invalidRequest, errMsg)
        }
        if (!teachMisCode) {
            String errMsg = "get: teachMisCode cannot be null or blank"
            log.warn(errMsg)
            throw new InvalidRequestException(InvalidRequestException.Errors.invalidSearchCriteria, errMsg)
        }

        log.debug("get: params ok; pulling meta data")
        EntityMetadata entity = entityMetadataService.get("ST", "XCTC.SHARED.APPL")

        if(entity == null) {
            String errMsg = "get: unable to retrieve shared-application metadata"
            log.error(errMsg)
            throw new InternalServerException(errMsg)
        }

        log.debug("get: meta data retrieved; gathering column names")
        // get column names from the table, excluding any that aren't physical columns (no field placement)
        List<String> columns = entity.entries
                .findAll { it.value.fieldPlacement != null }
                .collect { it.value.name }

        log.debug("get: gathered column names; retrieving app by id")
        String appIdColumnName = this.toColleagueField("appId", false)
        String teachMisCodeColumnName = this.toColleagueField("misCode", false)
        String query = """${appIdColumnName} = ${appId} AND ${teachMisCodeColumnName} = ${ColleagueUtils.quoteString(teachMisCode)}"""

        // Doing a Batch Select since the Key is just a surrogate primary key that means nothing.
        // The real key is a composite of { appId, teachMisCode }, so this query should only ever return 0 or 1 results.
        List<ColleagueData> applications = dmiDataService.batchSelect("ST", "XCTC.SHARED.APPL", columns, query)

        if (applications == null || applications.size() == 0) {
            String errMsg = "get: Application not found"
            log.warn(errMsg)
            throw new EntityNotFoundException(errMsg)
        }

        log.debug("get: app found; mapping fields")
        SharedApp result = this.mapFromColleague(applications[0])

        log.debug("get: application built. done.")
        return result
    }


    /**
     * Sets Shared Application Data into a Colleague staging table using the DMI Client
     * @param misCode The College Code used for grabbing college specific settings from the environment
     * @param application The application data to set into the staging table
     * @returns void/null
     */
    void post(String teachMisCode, StandardApp application) {
        log.debug("post: setting shared-application data")

        //****** Validate parameters ****
        if (application == null) {
            String errMsg = "post: Shared-Application data cannot be null"
            log.warn(errMsg)
            throw new InvalidRequestException(InvalidRequestException.Errors.invalidRequest, errMsg)
        }
        if (!application.appId) {
            String errMsg = "post: Application Id cannot be null or blank"
            log.warn(errMsg)
            throw new InvalidRequestException(InvalidRequestException.Errors.invalidRequest, errMsg)
        }
        if (!application.cccId) {
            String errMsg = "post: CCC Id cannot be null or blank"
            log.warn(errMsg)
            throw new InvalidRequestException(InvalidRequestException.Errors.invalidRequest, errMsg)
        }
        if (!application.collegeId) {
            String errMsg = "post: Application College Id cannot be null or blank"
            log.warn(errMsg)
            throw new InvalidRequestException(InvalidRequestException.Errors.invalidRequest, errMsg)
        }
        if (!teachMisCode) {
            String errMsg = "post: teachMisCode cannot be null or blank"
            log.warn(errMsg)
            throw new InvalidRequestException(InvalidRequestException.Errors.invalidSearchCriteria, errMsg)
        }

        // ensure mis code in url DOES NOT match body
        if (application.collegeId == teachMisCode) {
            String errMsg = "post: home MIS Code in body matches teach MIS Code in url"
            log.warn(errMsg)
            throw new InvalidRequestException(InvalidRequestException.Errors.invalidSearchCriteria, errMsg)
        }

        log.debug("post: params ok; checking to see if already exists.")
        String appIdColumnName = this.toColleagueField("appId", false)
        String teachMisCodeColumnName = this.toColleagueField("misCode", false)
        String query = """${appIdColumnName} = ${application.appId} AND ${teachMisCodeColumnName} = ${ColleagueUtils.quoteString(teachMisCode)}"""

        // Doing a Batch Select since the Key is just a surrogate primary key that means nothing.
        // The real key is a composite of { appId, teachMisCode }, so this query should only ever return 0 or 1 results.
        List<ColleagueData> existing_applications = dmiDataService.batchSelect("ST", "XCTC.SHARED.APPL", [ appIdColumnName ], query)
        if (existing_applications != null && existing_applications.size() > 0) {
            String errMsg = "post: Application ID already exists for the provided teachMisCode"
            log.warn(errMsg)
            throw new EntityConflictException(errMsg)
        }

        log.debug("post: app at teachingCollege does not yet exist; mapping fields")
        String[] record = this.mapToColleague(teachMisCode, application)

        log.debug("post: writing application record")
        DataWriteResult app_writeResult = dataWriter.write("XCTC.SHARED.APPL", null, true, record.toList())

        if (app_writeResult.errorOccurred) {
            String errorMessages = "unknown"
            if(app_writeResult.errorMessages) {
                errorMessages = app_writeResult.errorMessages?.toString()
                app_writeResult.errorMessages.each {
                    log.error("post: setting the record failed: [" + it + "]")
                }

                if (errorMessages.contains("Record already exists")) {
                    throw new EntityConflictException(errorMessages)
                }
            }
            throw new InternalServerException("Error Occurred: " + errorMessages)
        }

        if (app_writeResult.id == null) {
            String errMsg = "post: Error, no record ID returned from database write"
            log.error(errMsg)
            throw new InternalServerException(errMsg)
        } else if(!app_writeResult.id.isLong()) {
            String errMsg = "post: Error, record ID returned from database write is not a number"
            log.error(errMsg)
            throw new InternalServerException(errMsg)
        }

        log.debug("post: record written. done")
    }



    /**
     * Map data from Colleague to an Application
     */
    private SharedApp mapFromColleague(ColleagueData data) {
        SharedApp result = new SharedApp()
        result.supplementalQuestions = null

        // map application
        result.metaClass.properties.each { p ->
            String javaField = p.name
            Object value = null

            //Y if processed date has a value
            if (javaField == "sisProcessedFlag") {
                value = (this.getValue(javaField, data)) ? "Y" : "N"
            }
            // LocalDateTime needs to be mapped to two fields (Date and Time) in Colleague
            else if (p.type == LocalDateTime.class) {
                LocalDate dateValue = this.getValue(javaField + "Date", data) as LocalDate
                LocalTime timeValue = this.getValue(javaField + "Time", data) as LocalTime

                if (dateValue != null && timeValue != null) {
                    value = LocalDateTime.of(dateValue, timeValue)
                } else if (dateValue != null) {
                    value = LocalDateTime.of(dateValue, LocalTime.of(0, 0))
                }
            } else if (p.type == Boolean.class) {
                String booleanValue = this.getValue(javaField, data) as String
                if (booleanValue == "Y") {
                    value = Boolean.TRUE
                }
                else if (booleanValue == "N") {
                    value = Boolean.FALSE
                }
            } else if(p.type == Long.class) {
                value = this.getValue(javaField, data)
                if(value != null && value.getClass() == String.class) {
                    if((value as String).isEmpty()) {
                        value = 0L
                    } else {
                        value = Long.parseLong(value as String)
                    }
                }
            } else {
                value = this.getValue(javaField, data)
            }

            if (value != null) {
                result."$javaField" = value
            }
        }

        return result
    }


    /**
     * Map data from an Application to a String array for transmission to Colleague
     */
    private String[] mapToColleague(String teachMisCode, StandardApp application) {

        EntityMetadata entity = entityMetadataService.get("ST", "XCTC.SHARED.APPL")

        if(entity == null) {
            String errMsg = "mapToColleague: Unable to retrieve metaData columns for Shared Application"
            log.error(errMsg)
            throw new InternalServerException(errMsg)
        }

        Integer recordSize = entity.entries
                .collect { it.value.fieldPlacement }
                .max { it ?: 0 }

        if(recordSize == 0) {
            String errMsg = "mapToColleague: Unable to determine recordSize for Shared Application mapping"
            log.error(errMsg)
            throw new InternalServerException(errMsg)
        }
        String[] record = new String[recordSize]

        application.properties.each { p ->
            if (p.value != null) {
                // LocalDateTime needs to be mapped to two fields (Date and Time) in Colleague
                if (p.value.getClass() == LocalDateTime.class) {
                    // date component
                    CddEntry cdd = this.getCddEntry(entity, (String) p.key + "Date")
                    this.mapValue(p.value, cdd, record)

                    // time component
                    cdd = this.getCddEntry(entity, (String) p.key + "Time")
                    this.mapValue(p.value, cdd, record)

                } else {
                    CddEntry cdd = this.getCddEntry(entity, (String) p.key)
                    this.mapValue(p.value, cdd, record)
                }
            }
        }

        CddEntry cdd = this.getCddEntry(entity, "misCode")
        this.mapValue(teachMisCode, cdd, record)

        return record
    }


    /**
     * Get value from Colleague data based on java field name
     */
    private Object getValue(String javaField, ColleagueData data) {
        String colleagueField = this.toColleagueField(javaField, false)
        Object value = data.values[colleagueField]

        if (value == null) {
            colleagueField = this.toColleagueField(javaField, true)
            value = data.values[colleagueField]
        }

        return value
    }


    /**
     * Get a CDD Entry by java field name
     */
    private CddEntry getCddEntry(EntityMetadata entity, String javaFieldName) {
        String f = this.toColleagueField(javaFieldName, false)
        CddEntry cdd = entity.entries[f]

        if (cdd == null) {
            f = this.toColleagueField(javaFieldName, true)
            cdd = entity.entries[f]
        }

        return cdd
    }


    /**
     * Map a value to the correct location in a record for transmission to Colleague
     */
    private void mapValue(Object value, CddEntry cdd, String[] record) {
        if (cdd != null && cdd.fieldPlacement != null && cdd.fieldPlacement > 0) {
            String v = CddUtils.convertFromValue(value, cdd, true)
            record[cdd.fieldPlacement - 1] = v
        }
    }


    /**
     * Convert a java field to a colleague field name in the XCTC.SHARED.APPL table.
     * Camel case is converted to upper case with "." delimiting words. So collegeId becomes XCTC.SD.COLLEGE.ID.
     * <p>
     * Unfortunately camel case is ambiguous with numbers, so there are two modes to determine a name via numberMode.
     * Setting numberMode to true will add periods before and after numbers. false will not treat numbers as a new word.
     * <p>
     * Another challenge is field length, which can only be 28 characters in Colleague.
     * <p>
     * Notes:
     * 1. If the calculated field name is over 28 characters it will be truncated
     * 2. A few fields don't follow the normal pattern due to truncation ambiguities, so their translation is hardcoded
     */
    private String toColleagueField(String javaFieldName, boolean numberMode) {

        // exceptions due to field truncation
        switch (javaFieldName) {
            case "emailVerifiedTimestampDate":
                return "XCTC.SD.EMAIL.VERIFIED.DATE"
            case "emailVerifiedTimestampTime":
                return "XCTC.SD.EMAIL.VERIFIED.TIME"
            case "mainphoneVerifiedTimestampDate":
                return "XCTC.SD.MAINPHONE.VRFD.DATE"
            case "mainphoneVerifiedTimestampTime":
                return "XCTC.SD.MAINPHONE.VRFD.TIME"
            case "addressValidationOverrideTimestampDate":
                return "XCTC.SD.ADDRESS.VALID.O.DATE"
            case "addressValidationOverrideTimestampTime":
                return "XCTC.SD.ADDRESS.VALID.O.TIME"
            case "preferredMethodOfContact":
                return "XCTC.SD.PREFERRED.METHOD.CON"
            case "highestMathCourseTaken":
                return "XCTC.SD.HIGHEST.MATH.CRS.TAK"
            case "highestMathCoursePassed":
                return "XCTC.SD.HIGHEST.MATH.CRS.PAS"
            case "highestMathPassedGrade":
                return "XCTC.SD.HIGHEST.MATH.PASSEDG"
            case "acceptedTerms":
                return "XCTC.SD.ACCEPTED.TERMS";
            case "acceptedTermsTimestampDate":
                return "XCTC.SD.ACCEPTED.TERMS.DATE";
            case "acceptedTermsTimestampTime":
                return "XCTC.SD.ACCEPTED.TERMS.TIME";
            case "mailingAddressValidationOverride":
                return "XCTC.SD.MA.ADDRESS.VALID.O";
            case "phoneType":
                return "XCTC.SD.MAINPHONE.TYPE";
            case "ipAddressAtAccountCreation":
                return "XCTC.SD.IP.ADDRESS.CRTD";
            case "ipAddressAtAppCreation":
                return "XCTC.SD.IP.ADDRESS.APPL.CRTD";
            case "studentParent":
                return "XCTC.SD.STUDENT.PARENT";
            case "studentDepsUnder18":
                return "XCTC.SD.STUDENT.DEPS.UNDER18";
            case "studentDeps18Over":
                return "XCTC.SD.STUDENT.DEPS.18OVER";
            case "idmeConfirmationTimestampDate":
                return "XCTC.SD.IDME.CONFIRMED.DATE";
            case "idmeConfirmationTimestampTime":
                return "XCTC.SD.IDME.CONFIRMED.TIME";
            case "idmeOptinTimestampDate":
                return "XCTC.SD.IDME.OPTIN.DATE";
            case "idmeOptinTimestampTime":
                return "XCTC.SD.IDME.OPTIN.TIME";
            case "idmeWorkflowStatus":
                return "XCTC.SD.IDME.WORKFLOW.STATUS";
            case "raceAIANOtherDescription":
                return "XCTC.SD.RACE.AIAN.OTHER.DESC";
            case "sisProcessedFlag":
                //For sisProcessedFlag we get the value of processed date and later checks if there is a value
                return "XCTC.SD.PROCESSED.DATE"
            case "tstmpSISProcessedDate":
                return "XCTC.SD.PROCESSED.DATE"
            case "tstmpSISProcessedTime":
                return "XCTC.SD.PROCESSED.TIME"
            case "sisProcessedNotes":
                return "XCTC.SD.USER1"
        }

        // convert from camel case to "upper dot case" or whatever we might call it ...
        char[] newChars = new char[javaFieldName.length() * 2]
        Integer pos = 0
        boolean inNumber = false
        for (int x = 0; x < javaFieldName.length(); x++) {
            char c = javaFieldName.charAt(x)

            if (numberMode && c >= ('0' as char) && c <= ('9' as char)) {
                if (!inNumber) {
                    newChars[pos++] = '.'
                    inNumber = true
                }
            } else if (inNumber) {
                newChars[pos++] = '.'
                inNumber = false
            } else if (c >= ('A' as char) && c <= ('Z' as char)) {
                newChars[pos++] = '.'
            }

            newChars[pos++] = c.toUpperCase()
        }

        // truncate if over 28 characters
        String fieldName = "XCTC.SD." + new String(newChars, 0, pos)
        if (fieldName.length() > 28) {
            fieldName = fieldName.substring(0, 28)
        }

        return fieldName
    }
}
