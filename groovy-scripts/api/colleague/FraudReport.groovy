package api.colleague

import api.colleague.model.DataWriteResult
import api.colleague.util.ColleagueUtils
import api.colleague.util.DataWriter
import com.ccctc.adaptor.exception.EntityConflictException
import com.ccctc.adaptor.exception.EntityNotFoundException
import com.ccctc.adaptor.exception.InternalServerException
import com.ccctc.adaptor.exception.InvalidRequestException
import com.ccctc.adaptor.model.fraud.FraudReport as FraudReportModel
import com.ccctc.adaptor.util.ClassMap
import org.apache.commons.lang.StringUtils
import org.ccctc.colleaguedmiclient.model.CTXData
import org.ccctc.colleaguedmiclient.model.CddEntry
import org.ccctc.colleaguedmiclient.model.ColleagueData
import org.ccctc.colleaguedmiclient.model.EntityMetadata
import org.ccctc.colleaguedmiclient.model.KeyValuePair
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
 * <h1>Fraud Report Colleague Interface</h1>
 * <summary>
 *     <p>Provides a Wrapper around the FraudReport Colleague sis functionality:</p>
 *     <ol>
 *         <li>Storing and populating Fraud Reports into the Colleague staging table</li>
 *         <li>Retrieving Fraud Reports by Id, and/or by appId/cccId, from the Colleague staging table</li>
 *         <li>Removing Fraud Reports by Id from the Colleague staging table</li>
 *         <li>Rescinding Fraud Report by appId/ccId within the Colleague staging table</li>
 *     <ol>
 * </summary>
 *
 * @version CAv4.11.0
 *
 */
class FraudReport {

    protected final static Logger log = LoggerFactory.getLogger(FraudReport.class)

    //****** Populated by injection in Groovy Service Implementation ****
    Environment environment

    //****** Populated by colleagueInit ClassMap ****
    private DmiDataService dmiDataService
    private DmiCTXService dmiCTXService
    private EntityMetadataService entityMetadataService
    private DataWriter dataWriter

    /**
     * Initialize services
     */
    void colleagueInit(String misCode, Environment environment, ClassMap services) {
        this.dmiDataService = services.get(DmiDataService.class)
        this.entityMetadataService = this.dmiDataService.getEntityMetadataService()

        this.dmiCTXService = services.get(DmiCTXService.class)
        this.dataWriter = new DataWriter(this.dmiCTXService)
    }

    /**
     * Default Constructor needed since we also specify an alternative Constructor
     */
    FraudReport(){
        // do nothing, except be available
    }

    /**
     * Alternative Constructor to set the environment during initialization.
     * Useful for when other groovy PS APIs need to use these Functions that depend on environment
     */
    FraudReport(Environment e) {
        this.environment = e
    }

    /**
     * Gets FraudReport Data from the sis staging table matching strictly on the sisFraudReportId
     * @param misCode The College Code used for grabbing college specific settings from the environment.
     * @param sisFraudReportId The internal sis Fraud Report Id generated when originally populating the data into the in memory db
     * @returns A matching Fraud Report
     */
    FraudReportModel getById(String misCode, Long sisFraudReportId) {
        log.debug("getById: getting fraud data by id")

        //****** Validate parameters ****
        if (!misCode) {
            throw new InvalidRequestException(InvalidRequestException.Errors.invalidRequest, "Mis Code cannot be null or blank")
        }
        if (!sisFraudReportId) {
            throw new InvalidRequestException(InvalidRequestException.Errors.invalidRequest, "sis fraud report id cannot be null or blank")
        }

        log.debug("getById: params ok; pulling meta data")
        EntityMetadata entity = entityMetadataService.get("ST", "XCTC.FRAUD.REPORT")

        if(entity == null) {
            String errMsg = "getById: unable to retrieve fraud report metadata"
            log.error(errMsg)
            throw new InternalServerException(errMsg)
        }

        log.debug("getById: meta data retrieved; gathering column names")
        // get column names from the table, excluding any that aren't physical columns (no field placement)
        List<String> columns = entity.entries
                .findAll { it.value.fieldPlacement != null }
                .collect { it.value.name }

        log.debug("getById: gathered column names; retrieving fraud report by id")
        ColleagueData fraudReport = dmiDataService.singleKey("ST", "XCTC.FRAUD.REPORT", columns, sisFraudReportId.toString())
        if(fraudReport && columns.contains("XCTC.FR.TSTMP.RESCIND.DATE")) {
            log.debug("getById: detected rescind date field in metadata. checking if this is rescinded...")
            if (fraudReport.values["XCTC.FR.TSTMP.RESCIND.DATE"] != null) {
                log.debug("getById: this fr rescinded. returning not found...")
                // if rescinded, dont return it
                fraudReport = null
            } else {
                log.debug("getById: this fr is not rescinded. returning per usual...")
            }
        }
        if (fraudReport == null) {
            String errMsg = "get: fraud report not found"
            log.warn(errMsg)
            throw new EntityNotFoundException(errMsg)
        }
        log.debug("getById: fraud report found; mapping fields")
        FraudReportModel result = this.mapFromColleague(fraudReport)

        log.debug("getById: fraud report built. done.")
        return result
    }

    /**
     * Removes FraudReport Data from the sis staging table matching strictly on the sisFraudReportId
     * @param misCode The College Code used for grabbing college specific settings from the environment.
     * @param sisFraudReportId The internal sis Fraud Report Id generated when originally populating the data into the in memory db
     */
    void deleteById(String misCode, Long sisFraudReportId) {
        log.debug("deleteById: removing fraud data by id")

        //****** Validate parameters ****
        if (!misCode) {
            throw new InvalidRequestException(InvalidRequestException.Errors.invalidRequest, "Mis Code cannot be null or blank")
        }
        if (!sisFraudReportId) {
            throw new InvalidRequestException(InvalidRequestException.Errors.invalidRequest, "sis fraud report id cannot be null or blank")
        }

        log.debug("deleteById: params ok; building method params")

        ArrayList params = []
        params << new KeyValuePair("Id", sisFraudReportId.toString())
        CTXData fraud_deleteResult = this.dmiCTXService.execute("ST", "X.CCCTC.FRAUD.REPORT.DELETE", params)

        Boolean errorOccurred = (fraud_deleteResult.variables["ErrorOccurred"] as boolean)
        if (errorOccurred) {
            List<String> errMsgs = (List<String>) fraud_deleteResult.variables["ErrorMsgs"]
            String errorMessages = "unknown"
            if(errMsgs) {
                errorMessages = errMsgs?.toString()
                errMsgs.each {
                    log.error("deleteById: deleting the record failed: [" + it + "]")
                }
            }
            throw new InternalServerException("Error Occurred: " + errorMessages)
        }

        log.debug("deleteById: done")
    }

    void deleteFraudReport( FraudReportModel report ) {
        List<ColleagueData> reports = getMatchingRaw(report.misCode, report.appId, report.cccId)
        if( !reports  || reports.size() <= 0) {
            throw new EntityNotFoundException("No Fraud Reports found by given params to rescind")
        }

        for (ColleagueData r : reports) {

            LocalDateTime tstmpRescind = LocalDateTime.now()
            log.debug("deleteFraudReport: mapping raw fields")
            String[] record = this.mapToRecord(r, tstmpRescind)

            /// Colleague doesn't appear to actually/natively support direct record updates!
            /// In the past, We've used Custom colleague transactions, that had the (unused)
            /// ability to do updates. But we've moved away from having custom transactions.
            /// But when looking at the old custom transactions, to support updates, it internally
            /// did a MIO.DELETE.RECORD call before rebuilding the record and re-creating it (via MIO.WRITE.RECORD).
            /// So instead of creating a custom transaction, use what we've got to do the same thing:
            /// delete the record by id, and then re-create it.
            this.deleteById(report.misCode, Long.parseLong(r.key))
            log.debug("deleteFraudReport: updating record")
            DataWriteResult fraud_writeResult = dataWriter.write("XCTC.FRAUD.REPORT", r.key, false, record.toList())

            if (fraud_writeResult.errorOccurred) {
                String errorMessages = "unknown"
                if(fraud_writeResult.errorMessages) {
                    errorMessages = fraud_writeResult.errorMessages?.toString()
                    fraud_writeResult.errorMessages.each {
                        log.error("deleteFraudReport: setting the record failed: [" + it + "]")
                    }

                    if (errorMessages.contains("Record already exists")) {
                        throw new EntityConflictException(errorMessages)
                    }
                }
                throw new InternalServerException("Error Occurred: " + errorMessages)
            }

            if (fraud_writeResult.id == null) {
                String errMsg = "deleteFraudReport: Error, no record ID returned from database write"
                log.error(errMsg)
                throw new InternalServerException(errMsg)
            } else if(!fraud_writeResult.id.isLong()) {
                String errMsg = "deleteFraudReport: Error, record ID returned from database write is not a number"
                log.error(errMsg)
                throw new InternalServerException(errMsg)
            }
        }
    }

    /**
     * Retrieves FraudReport Data from the db matching on either appId or cccId or both
     * @param misCode The College Code used for grabbing college specific settings from the environment.
     * @param appId The Fraudulent AppId to match on
     * @param appId The Fraudulent cccId to match on
     * @returns A List of matching Fraud Reports
     */
    List<com.ccctc.adaptor.model.fraud.FraudReport> getMatching(String misCode, Long appId, String cccId) {
        log.debug("getMatching: getting matching fraud data by appId and/or cccId")

        //****** Validate parameters ****
        if (!misCode) {
            throw new InvalidRequestException(InvalidRequestException.Errors.invalidRequest, "Mis Code cannot be null or blank")
        }
        if (!appId && !cccId) {
            throw new InvalidRequestException(InvalidRequestException.Errors.invalidRequest, "either appId or cccId must be provided")
        }

        log.debug("getMatching: params ok; getting raw data")
        List<ColleagueData> fraudReports = getMatchingRaw(misCode, appId, cccId)
        List<FraudReportModel> results = new ArrayList<FraudReportModel>()

        if (fraudReports != null && fraudReports.size() > 0) {

            log.debug("getMatching: fraud reports found; mapping fields")
            for(int i =  0; i < fraudReports.size(); i++) {
                results.push(this.mapFromColleague(fraudReports[i]))
            }
        }
        log.debug("getMatching: done.")
        return results
    }

    protected List<ColleagueData> getMatchingRaw(String misCode, Long appId, String cccId) {
        log.debug("getMatchingRaw: getting matching fraud data by appId and/or cccId")

        //****** Validate parameters ****
        if (!misCode) {
            throw new InvalidRequestException(InvalidRequestException.Errors.invalidRequest, "Mis Code cannot be null or blank")
        }
        if (!appId && !cccId) {
            throw new InvalidRequestException(InvalidRequestException.Errors.invalidRequest, "either appId or cccId must be provided")
        }

        log.debug("getMatchingRaw: params ok; pulling meta data")
        EntityMetadata entity = entityMetadataService.get("ST", "XCTC.FRAUD.REPORT")

        if(entity == null) {
            String errMsg = "getMatching: unable to retrieve fraud report metadata"
            log.error(errMsg)
            throw new InternalServerException(errMsg)
        }

        log.debug("getMatching: meta data retrieved; gathering column names")
        // get column names from the table, excluding any that aren't physical columns (no field placement)
        List<String> columns = entity.entries
                .findAll { it.value.fieldPlacement != null }
                .collect { it.value.name }

        String criteria = "XCTC.FR.COLLEGE.ID = " + ColleagueUtils.quoteString(misCode)
        if(appId) {
            criteria += " AND XCTC.FR.APPL.ID = " + appId.toString()
        }
        if(cccId) {
            criteria += " AND XCTC.FR.CCC.ID = " + ColleagueUtils.quoteString(cccId)
        }

        log.debug("getMatchingRaw: gathered column names; retrieving fraud report by criteria")
        List<ColleagueData> results = new ArrayList<ColleagueData>()
        List<ColleagueData> fraudReports = dmiDataService.batchSelect("ST", "XCTC.FRAUD.REPORT", columns, criteria)
        for(ColleagueData cd in fraudReports) {
            // Manually removing from returned result set,
            // any that have been rescinded.
            // Originally, attempted to do this filter in uniquery but kept failing with
            // generic 'error executing query' message for trying to compare with null
            if(!columns.contains("XCTC.FR.TSTMP.RESCIND.DATE")) {
                results.add(cd)
            } else if (cd.values["XCTC.FR.TSTMP.RESCIND.DATE"] == null) {
                results.add(cd)
            }
        }
        log.debug("getMatchingRaw: done.")
        return results
    }

    /**
     * Inserts a FraudReport Data into the sis staging table
     * @param misCode The College Code used for grabbing college specific settings from the environment.
     * @param newModel The Fraud Report to insert into the sis staging table
     * @returns the sisFraudReportId of the newly inserted report
     */
    Long create(String misCode, com.ccctc.adaptor.model.fraud.FraudReport newModel) {
        log.debug("create: setting fraud report data")

        //****** Validate parameters ****
        if (newModel == null) {
            throw new InvalidRequestException(InvalidRequestException.Errors.invalidRequest, "Fraud Report data cannot be null")
        }
        if (!newModel.appId || !newModel.cccId) {
            throw new InvalidRequestException(InvalidRequestException.Errors.invalidRequest, "Both Application Id and cccId must be provided")
        }
        if (StringUtils.isEmpty(newModel.fraudType)) {
            throw new InvalidRequestException(InvalidRequestException.Errors.invalidRequest, "fraudType cannot be null")
        }

        if (!misCode) {
            throw new InvalidRequestException(InvalidRequestException.Errors.invalidSearchCriteria, "misCode cannot be null or blank")
        }

        List<com.ccctc.adaptor.model.fraud.FraudReport> existing = getMatching(misCode, newModel.appId, newModel.cccId)
        if(existing.size() > 0) {
            throw new EntityConflictException("Fraud Report already exists for given cccId and appId")
        }

        log.debug("create: mapping fields")
        String[] record = this.mapToColleague(newModel)

        log.debug("create: writing record")
        DataWriteResult fraud_writeResult = dataWriter.write("XCTC.FRAUD.REPORT", "", true, record.toList())

        if (fraud_writeResult.errorOccurred) {
            String errorMessages = "unknown"
            if(fraud_writeResult.errorMessages) {
                errorMessages = fraud_writeResult.errorMessages?.toString()
                fraud_writeResult.errorMessages.each {
                    log.error("create: setting the record failed: [" + it + "]")
                }

                if (errorMessages.contains("Record already exists")) {
                    throw new EntityConflictException(errorMessages)
                }
            }
            throw new InternalServerException("Error Occurred: " + errorMessages)
        }

        if (fraud_writeResult.id == null) {
            String errMsg = "create: Error, no record ID returned from database write"
            log.error(errMsg)
            throw new InternalServerException(errMsg)
        } else if(!fraud_writeResult.id.isLong()) {
            String errMsg = "create: Error, record ID returned from database write is not a number"
            log.error(errMsg)
            throw new InternalServerException(errMsg)
        }

        log.debug("create: record written. done")

        return Long.parseLong(fraud_writeResult.id)
    }


    /**
     * Map data from Colleague to an Application
     */
    protected FraudReportModel mapFromColleague(ColleagueData data) {

        if(data == null) {
            return null
        }

        FraudReportModel result = new FraudReportModel()
        result.sisFraudReportId = Long.parseLong(data.key)

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
                String longValue = this.getValue(javaField, data) as String
                if(org.apache.commons.lang3.StringUtils.isNotEmpty(longValue)) {
                    value = Long.parseLong(longValue)
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
     * Map data from a model to a String array for transmission to Colleague
     */
    protected String[] mapToColleague(FraudReportModel model) {

        EntityMetadata entity = entityMetadataService.get("ST", "XCTC.FRAUD.REPORT")

        if(entity == null) {
            String errMsg = "mapToColleague: Unable to retrieve metaData columns for Fraud Report"
            log.error(errMsg)
            throw new InternalServerException(errMsg)
        }

        Integer recordSize = entity.entries
                .collect { it.value.fieldPlacement }
                .max { it ?: 0 }

        if(recordSize == 0) {
            String errMsg = "mapToColleague: Unable to determine recordSize for Fraud Report mapping"
            log.error(errMsg)
            throw new InternalServerException(errMsg)
        }
        String[] record = new String[recordSize]

        model.properties.each { p ->
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

        return record
    }



    protected String[] mapToRecord(ColleagueData model, LocalDateTime tstmpRescind) {

        EntityMetadata entity = entityMetadataService.get("ST", "XCTC.FRAUD.REPORT")

        if(entity == null) {
            String errMsg = "mapToRecord: Unable to retrieve metaData columns for Fraud Report"
            log.error(errMsg)
            throw new InternalServerException(errMsg)
        }

        Integer recordSize = entity.entries
                .collect { it.value.fieldPlacement }
                .max { it ?: 0 }

        if(recordSize == 0) {
            String errMsg = "mapToRecord: Unable to determine recordSize for Fraud Report mapping"
            log.error(errMsg)
            throw new InternalServerException(errMsg)
        }
        String[] record = new String[recordSize]

        model.values.each { p ->
            if (p.value != null) {
                CddEntry cdd = entity.entries[p.key]
                this.mapValue(p.value, cdd, record)
            }
        }

        if(tstmpRescind) {
            // date component
            CddEntry cdd = this.getCddEntry(entity, "tstmpRescindDate")
            this.mapValue(tstmpRescind, cdd, record)

            // time component
            cdd = this.getCddEntry(entity, "tstmpRescindTime")
            this.mapValue(tstmpRescind, cdd, record)
        }


        return record
    }
    /**
     * Get value from Colleague data based on java field name
     */
    protected Object getValue(String javaField, ColleagueData data) {
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
    protected CddEntry getCddEntry(EntityMetadata entity, String javaFieldName) {
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
    protected void mapValue(Object value, CddEntry cdd, String[] record) {
        if (cdd != null && cdd.fieldPlacement != null && cdd.fieldPlacement > 0) {
            String v = CddUtils.convertFromValue(value, cdd, true)
            record[cdd.fieldPlacement - 1] = v
        }
    }


    /**
     * Convert a java field to a colleague field name in the XCTC.FRAUD.REPORT table.
     * Camel case is converted to upper case with "." delimiting words. So fraudType becomes XCTC.FR.FRAUD.TYPE.
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
    protected String toColleagueField(String javaFieldName, boolean numberMode) {

        // exceptions due to field truncation
        switch (javaFieldName) {
            case "misCode":
                return "XCTC.FR.COLLEGE.ID"
            case "appId":
                return "XCTC.FR.APPL.ID"
            case "cccId":
                return "XCTC.FR.CCC.ID"
            case "fraudType":
                return "XCTC.FR.FRAUD.TYPE"
            case "reportedByMisCode":
                return "XCTC.FR.REPORTED.BY.MIS.CODE"
            case "reportedBySource":
                return "XCTC.FR.REPORTED.BY.SOURCE"
            case "tstmpSubmitDate":
                return "XCTC.FR.TSTMP.SUBMIT.DATE"
            case "tstmpSubmitTime":
                return "XCTC.FR.TSTMP.SUBMIT.TIME"
            case "tstmpRescindDate":
                return "XCTC.FR.TSTMP.RESCIND.DATE"
            case "tstmpRescindTime":
                return "XCTC.FR.TSTMP.RESCIND.TIME"
            case "sisProcessedFlag":
                //For sisProcessedFlag we get the value of processed date and later checks if there is a value
                return "XCTC.FR.PROCESSED.DATE"
            case "tstmpSISProcessedDate":
                return "XCTC.FR.PROCESSED.DATE"
            case "tstmpSISProcessedTime":
                return "XCTC.FR.PROCESSED.TIME"
            case "sisProcessedNotes":
                return "XCTC.FR.USER1"
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
        String fieldName = "XCTC.FR." + new String(newChars, 0, pos)
        if (fieldName.length() > 28) {
            fieldName = fieldName.substring(0, 28)
        }

        return fieldName
    }
}