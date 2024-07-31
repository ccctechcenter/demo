package api.colleague

import api.colleague.model.DataWriteResult
import api.colleague.util.DataWriter
import com.ccctc.adaptor.exception.EntityConflictException
import com.ccctc.adaptor.exception.EntityNotFoundException
import com.ccctc.adaptor.exception.InternalServerException
import com.ccctc.adaptor.exception.InvalidRequestException
import com.ccctc.adaptor.model.apply.InternationalApplication
import com.ccctc.adaptor.model.apply.SupplementalQuestions
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
 * <h1>CCCApply International Application Colleague Interface</h1>
 * <summary>
 *     <p>Provides for:</p>
 *     <ol>
 *         <li>Storing and populating International Applications from CCCApply into two custom colleague staging tables</li>
 *     <ol>
 *     <p>Uses Custom Transactions to insert the record(s)
 *     </p>
 * </summary>
 *
 * @version CAv4.6.0
 *
 */
class InternationalApply {
    private static final Logger log = LoggerFactory.getLogger(InternationalApply.class)

    //****** Populated by colleagueInit ClassMap ***
    private DmiDataService dmiDataService
    private EntityMetadataService entityMetadataService
    private DataWriter dataWriter


    /**
     * Initialize
     */
    void colleagueInit(String misCode, Environment environment, ClassMap services) {
        this.dmiDataService = services.get(DmiDataService.class)
        this.entityMetadataService = dmiDataService.getEntityMetadataService()

        DmiCTXService dmiCTXService = services.get(DmiCTXService.class)
        this.dataWriter = new DataWriter(dmiCTXService)
    }

    /**
     * Gets International Application Data from a Colleague staging table using DMI Client
     * @param misCode The College Code used for grabbing college specific settings from the environment
     * @param appId The application Id used when originally populating the data into the staging table
     */
    InternationalApplication get(String misCode, Long appId) {
        log.debug("get: getting international application data")

        //****** Validate parameters ****
        if (!appId) {
            String errMsg = "get: Application Id cannot be null or blank"
            log.warn(errMsg)
            throw new InvalidRequestException(InvalidRequestException.Errors.invalidRequest, errMsg)
        }
        if (!misCode) {
            String errMsg = "get: misCode cannot be null or blank"
            log.warn(errMsg)
            throw new InvalidRequestException(InvalidRequestException.Errors.invalidSearchCriteria, errMsg)
        }

        log.debug("get: params ok; pulling meta data")
        EntityMetadata entity = entityMetadataService.get("ST", "XCTC.CCC.APPLY.INTL.APPL")

        if(entity == null) {
            String errMsg = "get: unable to retrieve intl application metadata"
            log.error(errMsg)
            throw new InternalServerException(errMsg)
        }

        log.debug("get: meta data retrieved; gathering column names")
        // get column names from the table, excluding any that aren't physical columns (no field placement)
        List<String> columns = entity.entries
                .findAll { it.value.fieldPlacement != null }
                .collect { it.value.name }

        log.debug("get: gathered column names; retrieving app by id")
        ColleagueData application = dmiDataService.singleKey("ST", "XCTC.CCC.APPLY.INTL.APPL", columns, appId.toString())

        if (application == null) {
            String errMsg = "get: CCC Apply International application not found"
            log.warn(errMsg)
            throw new EntityNotFoundException(errMsg)
        }

        log.debug("get: app found; mapping fields")
        InternationalApplication result = this.mapFromColleague(application)

        if(result != null && result.appId != null) {

            log.debug("get: intl app found; checking for supplemental questions. grabbing metadata")
            EntityMetadata sq_entity = entityMetadataService.get("ST", "XCTC.CCC.APPLY.INTL.QNS")

            if(sq_entity == null) {
                String errMsg = "get: unable to retrieve intl application sq metadata"
                log.error(errMsg)
                throw new InternalServerException(errMsg)
            }
            // get column names from the table, excluding any that aren't physical columns (no field placement)
            List<String> sq_columns = sq_entity.entries
                    .findAll { it.value.fieldPlacement != null }
                    .collect { it.value.name }

            ColleagueData supplementalQuestions = dmiDataService.singleKey("ST", "XCTC.CCC.APPLY.INTL.QNS", sq_columns, appId.toString())

            if (supplementalQuestions != null) {
                result.supplementalQuestions = this.mapSupplementalQuestionsFromColleague(supplementalQuestions)
                result.supplementalQuestions.appId = appId
            } else {
                result.supplementalQuestions = null
                log.debug("get: No Supplemental Questions in Colleague for International Application ${appId}")
            }
        }

        log.debug("get: application built. done.")
        return result
    }

    /**
     * Sets International Application Data into 2 Colleague staging tables using the DMI Writer
     * @param misCode The College Code used for grabbing college specific settings from the environment
     * @param application The application data to set into the staging tables
     */
    void post(String misCode, InternationalApplication application) {
        log.debug("post: setting international application data")

        //****** Validate parameters ****
        if (application == null) {
            String errMsg = "post: International Application data cannot be null"
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
        if (!misCode) {
            String errMsg = "post: misCode cannot be null or blank"
            log.warn(errMsg)
            throw new InvalidRequestException(InvalidRequestException.Errors.invalidSearchCriteria, errMsg)
        }

        // ensure mis code in url matches body
        if (application.collegeId != misCode) {
            String errMsg = "post: MIS Code in body does not match URL"
            log.warn(errMsg)
            throw new InvalidRequestException(InvalidRequestException.Errors.invalidSearchCriteria, errMsg)
        }

        log.debug("post: params ok; checking to see if already exists.")
        ColleagueData existing_application = dmiDataService.singleKey("ST", "XCTC.CCC.APPLY.INTL.APPL", [ this.toColleagueField("cccId", false, false) ], application.appId.toString())
        if (existing_application != null && existing_application.key == application.appId.toString()) {
            String errMsg = "post: InternationalApplication ID already exists"
            log.warn(errMsg)
            throw new EntityConflictException(errMsg)
        }

        log.debug("post: app does not yet exist; mapping fields")
        String[] record = this.mapToColleague(application)

        log.debug("post: writing application record")
        DataWriteResult app_writeResult = dataWriter.write("XCTC.CCC.APPLY.INTL.APPL", application.appId.toString(), false, record.toList())

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

        log.debug("post: application written. checking supplemental Questions")
        if(application.supplementalQuestions != null) {

            // Note: we assume that the supplementalQuestions do not exist before trying to insert
            // following the logic that if it existed, the parent record should also exist and would have been
            // captured by the that check.

            log.debug("post: params ok; building method params")
            String[] sq_record = this.mapSupplementalQuestionsToColleague(application.supplementalQuestions)

            log.debug("post: writing application's sq record")
            DataWriteResult sq_writeResult = dataWriter.write("XCTC.CCC.APPLY.INTL.QNS", application.appId.toString(), false, sq_record.toList())

            if (sq_writeResult.errorOccurred) {
                String errorMessages = "unknown"
                if(sq_writeResult.errorMessages) {
                    errorMessages = sq_writeResult.errorMessages?.toString()
                    sq_writeResult.errorMessages.each {
                        log.error("post: setting the sq_record failed: [" + it + "]")
                    }

                    if (errorMessages.contains("SQ_Record already exists")) {
                        throw new EntityConflictException(errorMessages)
                    }
                }
                throw new InternalServerException("Error Occurred: " + errorMessages)
            }

            if (sq_writeResult.id == null) {
                String errMsg = "post: Error, no record ID returned from database write"
                log.error(errMsg)
                throw new InternalServerException(errMsg)
            } else if(!sq_writeResult.id.isLong()) {
                String errMsg = "post: Error, record ID returned from database write is not a number"
                log.error(errMsg)
                throw new InternalServerException(errMsg)
            }
        }

        log.debug("post: record(s) written. done")
    }

    /**
     * Map data from Colleague to an International Application application
     */
    private InternationalApplication mapFromColleague(ColleagueData data) {
        InternationalApplication result = new InternationalApplication()
        result.appId = Long.parseLong(data.key)

        // map application
        result.metaClass.properties.each { p ->
            String javaField = p.name
            Object value = null

            //Y if processed date has a value
            if (javaField == "sisProcessedFlag") {
                value = (getValue(javaField, data, false)) ? "Y" : "N"
            }
            // LocalDateTime needs to be mapped to two fields (Date and Time) in Colleague
            else if (p.type == LocalDateTime.class) {
                LocalDate dateValue = this.getValue(javaField + "Date", data, false) as LocalDate
                LocalTime timeValue = this.getValue(javaField + "Time", data, false) as LocalTime

                if (dateValue != null && timeValue != null) {
                    value = LocalDateTime.of(dateValue, timeValue)
                } else if (dateValue != null) {
                    value = LocalDateTime.of(dateValue, LocalTime.of(0, 0))
                }
            } else if (p.type == Boolean.class) {
                String booleanValue = this.getValue(javaField, data, false) as String
                if (booleanValue == "Y") {
                    value = Boolean.TRUE
                } else if (booleanValue == "N") {
                    value = Boolean.FALSE
                }
            } else if (p.type == Integer.class || p.type == Long.class) {
                String stringValue = this.getValue(javaField, data, false) as String
                value = (stringValue == null) ? null : (p.type == Integer.class) ?
                        (stringValue.isInteger()) ? stringValue as Integer : null :
                        (stringValue.isLong()) ? stringValue as Long : null
            } else {
                value = this.getValue(javaField, data, false)
            }

            if (value != null)
                result."$javaField" = value
        }

        return result
    }


    /**
     * Map data from Colleague to international application's supplemental questions application
     */
    private SupplementalQuestions mapSupplementalQuestionsFromColleague(ColleagueData data) {
        SupplementalQuestions result = new SupplementalQuestions()

        // map questions
        result.metaClass.properties.each { p ->
            String javaField = p.name
            Object value = null

            //Y if processed date has a value
            if (javaField == "sisProcessedFlag") {
                value = (this.getValue(javaField, data, true)) ? "Y" : "N"
            }
            // LocalDateTime needs to be mapped to two fields (Date and Time) in Colleague
            else if (p.type == LocalDateTime.class) {
                LocalDate dateValue = this.getValue(javaField + "Date", data, true) as LocalDate
                LocalTime timeValue = this.getValue(javaField + "Time", data, true) as LocalTime

                if (dateValue != null && timeValue != null) {
                    value = LocalDateTime.of(dateValue, timeValue)
                } else if (dateValue != null) {
                    value = LocalDateTime.of(dateValue, LocalTime.of(0, 0))
                }
            } else if (p.type == Boolean.class) {
                String booleanValue = this.getValue(javaField, data, true) as String
                if (booleanValue == "Y") {
                    value = Boolean.TRUE
                }
                else if (booleanValue == "N") {
                    value = Boolean.FALSE
                }
            } else if (p.type == Integer.class || p.type == Long.class) {
                String stringValue = this.getValue(javaField, data, true) as String
                value = (stringValue == null) ? null : (p.type == Integer.class) ?
                        (stringValue.isInteger()) ? stringValue as Integer : null :
                        (stringValue.isLong()) ? stringValue as Long : null
            } else {
                value = this.getValue(javaField, data, true)
            }

            if (value != null) {
                result."$javaField" = value
            }
        }

        return result
    }

    /**
     * Map data from an International Application to a String array for transmission to Colleague
     */
    private String[] mapToColleague(InternationalApplication application) {

        EntityMetadata entity = entityMetadataService.get("ST", "XCTC.CCC.APPLY.INTL.APPL")

        if(entity == null) {
            String errMsg = "mapToColleague: Unable to retrieve metaData columns for International Application"
            log.error(errMsg)
            throw new InternalServerException(errMsg)
        }

        Integer recordSize = entity.entries
                .collect { it.value.fieldPlacement }
                .max { it ?: 0 }

        if(recordSize == 0) {
            String errMsg = "mapToColleague: Unable to determine recordSize for International Application mapping"
            log.error(errMsg)
            throw new InternalServerException(errMsg)
        }
        String[] record = new String[recordSize]

        application.properties.each { p ->
            if (p.value != null) {
                // LocalDateTime needs to be mapped to two fields (Date and Time) in Colleague
                if (p.value.getClass() == LocalDateTime.class) {
                    // date component
                    CddEntry cdd = this.getCddEntry(entity, (String) p.key + "Date", false)
                    this.mapValue(p.value, cdd, record)

                    // time component
                    cdd = this.getCddEntry(entity, (String) p.key + "Time", false)
                    this.mapValue(p.value, cdd, record)

                } else {
                    CddEntry cdd = this.getCddEntry(entity, (String) p.key, false)
                    this.mapValue(p.value, cdd, record)
                }
            }
        }

        return record
    }


    /**
     * Map data from an International Application to a String array for transmission to Colleague
     */
    private String[] mapSupplementalQuestionsToColleague(SupplementalQuestions questions) {

        EntityMetadata entity = entityMetadataService.get("ST", "XCTC.CCC.APPLY.INTL.APPL")

        Integer recordSize = entity.entries
                .collect { it.value.fieldPlacement }
                .max { it ?: 0 }

        String[] record = new String[recordSize]

        questions.properties.each { p ->
            if (p.value != null) {
                // LocalDateTime needs to be mapped to two fields (Date and Time) in Colleague
                if (p.value.getClass() == LocalDateTime.class) {
                    // date component
                    CddEntry cdd = this.getCddEntry(entity, (String) p.key + "Date", true)
                    this.mapValue(p.value, cdd, record)

                    // time component
                    cdd = this.getCddEntry(entity, (String) p.key + "Time", true)
                    this.mapValue(p.value, cdd, record)

                } else {
                    CddEntry cdd = this.getCddEntry(entity, (String) p.key, true)
                    this.mapValue(p.value, cdd, record)
                }
            }
        }

        return record
    }

    /**
     * Get value from Colleague data based on java field name
     */
    private Object getValue(String javaField, ColleagueData data, boolean isSupplementalQuestion) {
        String colleagueField = this.toColleagueField(javaField, false, isSupplementalQuestion)
        Object value = data.values[colleagueField]

        if (value == null) {
            colleagueField = this.toColleagueField(javaField, true, isSupplementalQuestion)
            value = data.values[colleagueField]
        }

        return value
    }

    /**
     * Get a CDD Entry by java field name
     */
    private CddEntry getCddEntry(EntityMetadata entity, String javaFieldName, boolean isSupplementalQuestion) {
        String f = this.toColleagueField(javaFieldName, false, isSupplementalQuestion)
        CddEntry cdd = entity.entries[f]

        if (cdd == null) {
            f = this.toColleagueField(javaFieldName, true, isSupplementalQuestion)
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
     * Convert a java field to a colleague field name in the XCTC.CCC.APPLY.INTL.APPL and XCTC.CCC.APPLY.INTL.APPL.QSN table.
     * Camel case is converted to upper case with "." delimiting words. So collegeId becomes XCTC.I.COLLEGE.ID.
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
    private String toColleagueField(String javaFieldName, boolean numberMode, boolean isSupplementalQuestion) {

        // exceptions due to field truncation / casing
        if(isSupplementalQuestion) {
            switch (javaFieldName) {
                case "sisProcessedFlag":
                    //Gets the value of processed date and later checks if there is a value
                    return "XCTC.I.PROCESSED.DATE"
                case "tstmpSISProcessedDate":
                    return "XCTC.I.PROCESSED.DATE"
                case "tstmpSISProcessedTime":
                    return "XCTC.I.PROCESSED.Time"
                case "sisProcessedNotes":
                    return "XCTC.I.USER1"
            }
        } else {
            switch (javaFieldName) {
                case "altNonUsPhoneAuthTxt":
                    return "XCTC.I.ALT.NON.US.PH.ATH.TXT"
                case "altNonUsPhoneNumber":
                    return "XCTC.I.ALT.NON.US.PHONE"
                case "authorizeAgentInfoRelease":
                    return "XCTC.I.AUTH.AGT.INFO.RELEASE"
                case "col1CollegeAttendedId":
                    return "XCTC.I.COL1.COLLEGE.ATT.ID"
                case "col1PrimaryInstructionLanguage":
                    return "XCTC.I.COL1.PRI.INST.LANG"
                case "col2CollegeAttendedId":
                    return "XCTC.I.COL2.COLLEGE.ATT.ID"
                case "col2PrimaryInstructionLanguage":
                    return "XCTC.I.COL2.PRI.INST.LANG"
                case "col3CollegeAttendedId":
                    return "XCTC.I.COL3.COL.ATT.ID"
                case "col3PrimaryInstructionLanguage":
                    return "XCTC.I.COL3.PRI.INST.LANG"
                case "col4CollegeAttendedId":
                    return "XCTC.I.COL4.COLL.ATT.ID"
                case "col4PrimaryInstructionLanguage":
                    return "XCTC.I.COL4.PRI.INST.LANG"
                case "collegeExpelledSummary":
                    return "XCTC.I.COL.EXPELLED.SUMMARY"
                case "countryOfCitizenship":
                    return "XCTC.I.CNTRY.OF.CITIZENSHIP"
                case "currentMailingAddressOutsideUs":
                    return "XCTC.I.ADDRESS.OUTSIDE.US"
                case "currentMailingAddressVerified":
                    return "XCTC.I.CUR.ADDRESS.VERIFIED"
                case "currentMailingCountry":
                    return "XCTC.I.MAILING.COUNTRY"
                case "currentMailingNonUsAddress":
                    return "XCTC.I.MAILING.NON.US.ADDR"
                case "currentMailingNonUsPostalCode":
                    return "XCTC.I.MAILING.NON.US.PCODE"
                case "currentMailingNonUsProvince":
                    return "XCTC.I.MAILING.NON.US.PROV"
                case "currentMailingSameAsPermanent":
                    return "XCTC.I.MAILING.SAME.AS.PERM"
                case "currentMailingStreet1":
                    return "XCTC.I.MAILING.STREET.1"
                case "currentMailingStreet2":
                    return "XCTC.I.MAILING.STREET.2"
                case "currentMailingZipCode":
                    return "XCTC.I.MAILING.ZIP.CODE"
                case "dep10CountryOfBirth":
                    return "XCTC.I.DEP10.CO.OF.BIRTH"
                case "emergencyContactAddressVerified":
                    return "XCTC.I.EMERG.CT.ADDR.VER"
                case "emergencyContactCity":
                    return "XCTC.I.EMERG.CONTACT.CITY"
                case "emergencyContactCountry":
                    return "XCTC.I.EMERG.CONTACT.COUNTRY"
                case "emergencyContactEmail":
                    return "XCTC.I.EMERG.CONTACT.EMAIL"
                case "emergencyContactFirstName":
                    return "XCTC.I.EMERG.CO.FIRST.NAME"
                case "emergencyContactLastName":
                    return "XCTC.I.EMERG.CO.LAST.NAME"
                case "emergencyContactNoFirstName":
                    return "XCTC.I.EMERG.CO.NO.F.NAME"
                case "emergencyContactNonUsAddress":
                    return "XCTC.I.EMERG.CO.NON.US.ADDR"
                case "emergencyContactNonUsPostalCode":
                    return "XCTC.I.EMERG.CO.NON.US.PCODE"
                case "emergencyContactNonUsProvince":
                    return "XCTC.I.EMERG.CO.NON.US.PROV"
                case "emergencyContactPhoneAuthTxt":
                    return "XCTC.I.EMERG.CO.PH.AUTH.TXT"
                case "emergencyContactPhoneExt":
                    return "XCTC.I.EMERG.CO.PHONE.EXT"
                case "emergencyContactPhoneNumber":
                    return "XCTC.I.EMERG.CO.PHONE.NUMBER"
                case "emergencyContactRelationship":
                    return "XCTC.I.EMERG.CO.RELATIONSHIP"
                case "emergencyContactState":
                    return "XCTC.I.EMERG.CONTACT.STATE"
                case "emergencyContactStreet1":
                    return "XCTC.I.EMERG.CO.STREET.1"
                case "emergencyContactStreet2":
                    return "XCTC.I.EMERGENCY.CONTACT.2"
                case "emergencyContactZipCode":
                    return "XCTC.I.EMERG.CO.ZIP.CODE"
                case "engProficiencyPrerequisite":
                    return "XCTC.I.ENG.PROFICIENCY.PREQ"
                case "engProficiencyShowScore":
                    return "XCTC.I.ENG.PROFCNCY.SHOW.SCR"
                case "enrollTermDescription":
                    return "XCTC.I.ENROLL.TERM.DESC"
                case "i20IssuingSchoolName":
                    return "XCTC.I.I20.ISS.SCHOOL.NAME"
                case "noI94ExpirationDate":
                    return "XCTC.I.NO.I94.EXP.DATE"
                case "nonUsPermanentHomeAddressSameAsPermanent":
                    return "XCTC.I.NON.US.SAME.AS.PERM"
                case "nonUsPermanentHomeAddressVerified":
                    return "XCTC.I.NON.US.PERM.ADDR.VER"
                case "nonUsPermanentHomeCity":
                    return "XCTC.I.NON.US.PERM.HOME.CITY"
                case "nonUsPermanentHomeCountry":
                    return "XCTC.I.NON.US.PERM.HOME.CO"
                case "nonUsPermanentHomeNonUsAddress":
                    return "XCTC.I.NON.US.NON.US.ADDR"
                case "nonUsPermanentHomeNonUsPostalCode":
                    return "XCTC.I.NON.US.PERM.PCODE"
                case "nonUsPermanentHomeNonUsProvince":
                    return "XCTC.I.NON.US.PERM.PROV"
                case "nonUsPermanentHomeStreet1":
                    return "XCTC.I.NON.US.PERM.HOME.ST.1"
                case "nonUsPermanentHomeStreet2":
                    return "XCTC.I.NON.US.PERM.HOME.ST.2"
                case "numberOfPracticalTraining":
                    return "XCTC.I.NO.OF.PRACTICAL.TRNG"
                case "parentGuardianAddressVerified":
                    return "XCTC.I.GUARDIAN.ADDR.VER"
                case "parentGuardianCountry":
                    return "XCTC.I.GUARDIAN.COUNTRY"
                case "parentGuardianFirstName":
                    return "XCTC.I.GUARDIAN.FIRST.NAME"
                case "parentGuardianLastName":
                    return "XCTC.I.GUARDIAN.LAST.NAME"
                case "parentGuardianNoFirstName":
                    return "XCTC.I.GUARDIAN.NO.F.NAME"
                case "parentGuardianNonUsAddress":
                    return "XCTC.I.GUARDIAN.NON.US.ADDR"
                case "parentGuardianNonUsPostalCode":
                    return "XCTC.I.GUARDIAN.NON.US.PCODE"
                case "parentGuardianNonUsProvince":
                    return "XCTC.I.GUARDIAN.NON.US.PROV"
                case "parentGuardianPhoneAuthTxt":
                    return "XCTC.I.GUARDIAN.PH.AUTH.TXT"
                case "parentGuardianPhoneExt":
                    return "XCTC.I.GUARDIAN.PHONE.EXT"
                case "parentGuardianPhoneNumber":
                    return "XCTC.I.GUARDIAN.PHONE"
                case "parentGuardianRelationship":
                    return "XCTC.I.GUARDIAN.RELATIONSHIP"
                case "parentGuardianStreet1":
                    return "XCTC.I.GUARDIAN.STREET.1"
                case "parentGuardianStreet2":
                    return "XCTC.I.GUARDIAN.STREET.2"
                case "parentGuardianZipCode":
                    return "XCTC.I.GUARDIAN.ZIP.CODE"
                case "passportCountryOfIssuance":
                    return "XCTC.I.PASSPORT.CO.ISSUANCE"
                case "passportExpirationDate":
                    return "XCTC.I.PASSPORT.EXP.DATE"
                case "permAddrAddressVerified":
                    return "XCTC.I.PERM.ADDR.ADDRESS.VER"
                case "permAddrNonUsAddress":
                    return "XCTC.I.PERM.ADDR.NON.US.ADDR"
                case "permAddrNonUsPostalCode":
                    return "XCTC.I.ADDR.NON.US.PCODE"
                case "permAddrNonUsProvince":
                    return "XCTC.I.PERM.ADDR.NON.US.PROV"
                case "presentlyStudyingInUs":
                    return "XCTC.I.PRESENTLY.STDY.IN.US"
                case "pt1AuthorizingSchool":
                    return "XCTC.I.PT1.AUTH.SCHOOL"
                case "pt1PracticalTrainingId":
                    return "XCTC.I.PT1.PRACTICAL.TRNG.ID"
                case "pt2AuthorizingSchool":
                    return "XCTC.I.PT2.AUTH.SCHOOL"
                case "pt2PracticalTrainingId":
                    return "XCTC.I.PT2.PRACTICAL.TRNG.ID"
                case "pt3AuthorizingSchool":
                    return "XCTC.I.PT3.AUTH.SCHOOL"
                case "pt3PracticalTrainingId":
                    return "XCTC.I.PT3.PRACTICAL.TRNG.ID"
                case "pt4AuthorizingSchool":
                    return "XCTC.I.PT4.AUTH.SCHOOL"
                case "pt4PracticalTrainingId":
                    return "XCTC.I.PT4.PRACTICAL.TRNG.ID"
                case "sisProcessedFlag":
                    //Gets the value of processed date and later checks if there is a value
                    return "XCTC.I.PROCESSED.DATE"
                case "tstmpSISProcessedDate":
                    return "XCTC.I.PROCESSED.DATE"
                case "tstmpSISProcessedTime":
                    return "XCTC.I.PROCESSED.Time"
                case "sisProcessedNotes":
                    return "XCTC.I.USER1"
                case "noMailingAddressHomeless":
                    return "XCTC.I.NO.ADDRESS.HOMELESS"
                case "noNonUsaPermAddressHomeless":
                    return "XCTC.I.NO.N.US.P.ADDR.HMLESS"
                case "noPermAddressHomeless":
                    return "XCTC.I.NO.PERM.ADDR.HOMELESS"
                case "engProficiencyType":
                    return "XCTC.I.ENG.PROFCNCY.TYPE"
                case "emailVerifiedTimestampDate":
                    return "XCTC.I.EMAIL.VERIFIED.DATE"
                case "emailVerifiedTimestampTime":
                    return "XCTC.I.EMAIL.VERIFIED.TIME"
                case "mainphoneVerified":
                    return "XCTC.I.MAIN.PHONE.VERIFIED"
                case "mainphoneVerifiedTimestampDate":
                    return "XCTC.I.MAIN.PHONE.VRFD.DATE"
                case "mainphoneVerifiedTimestampTime":
                    return "XCTC.I.MAIN.PHONE.VRFD.TIME"
                case "addressValidationOverride":// manual mapping since it was created as 27 long string to match ccpg and cccApply
                    return "XCTC.I.ADDRESS.VALIDATION.O"
                case "addressValidationOverrideTimestampDate":
                    return "XCTC.I.ADDRESS.VALID.O.DATE"
                case "addressValidationOverrideTimestampTime":
                    return "XCTC.I.ADDRESS.VALID.O.TIME"
                case "preferredMethodOfContact":
                    return "XCTC.I.PREFERRED.METHOD.CON"
                case "acceptedTerms":
                    return "XCTC.I.ACCEPTED.TRMS"
                case "acceptedTermsTimestampDate":
                    return "XCTC.I.ACCEPTED.TRMS.DATE"
                case "acceptedTermsTimestampTime":
                    return "XCTC.I.ACCEPTED.TRMS.TIME"
                case "phoneType":
                    return "XCTC.I.MAIN.PHONE.TYPE"
                case "ipAddressAtAccountCreation":
                    return "XCTC.I.IP.ADDRESS.ACCT"
                case "ipAddressAtAppCreation":
                    return "XCTC.I.IP.ADDRESS.APPL"
                case "idmeConfirmationTimestampDate":
                    return "XCTC.I.IDME.CONFIRMED.DATE";
                case "idmeConfirmationTimestampTime":
                    return "XCTC.I.IDME.CONFIRMED.TIME";
                case "idmeOptinTimestampDate":
                    return "XCTC.I.IDME.OPTIN.DATE";
                case "idmeOptinTimestampTime":
                    return "XCTC.I.IDME.OPTIN.TIME";
                case "idmeWorkflowStatus":
                    return "XCTC.I.IDME.WORKFLOW.STATUS";
            }
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
        String fieldName = "XCTC.I." + new String(newChars, 0, pos)
        if (fieldName.length() > 28) {
            fieldName = fieldName.substring(0, 28)
        }

        return fieldName
    }
}
