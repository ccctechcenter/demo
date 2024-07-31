package api.colleague

import api.colleague.model.DataWriteResult
import api.colleague.util.ColleagueUtils
import api.colleague.util.DataWriter
import com.ccctc.adaptor.exception.EntityNotFoundException
import com.ccctc.adaptor.exception.InternalServerException
import com.ccctc.adaptor.exception.InvalidRequestException
import com.ccctc.adaptor.model.placement.StudentPlacementData
import com.ccctc.adaptor.util.ClassMap
import org.apache.commons.lang.StringUtils
import org.ccctc.colleaguedmiclient.model.CddEntry
import org.ccctc.colleaguedmiclient.model.ColleagueData
import org.ccctc.colleaguedmiclient.model.EntityMetadata
import org.ccctc.colleaguedmiclient.service.DmiCTXService
import org.ccctc.colleaguedmiclient.service.DmiDataService
import org.ccctc.colleaguedmiclient.service.DmiService
import org.ccctc.colleaguedmiclient.service.EntityMetadataService
import org.ccctc.colleaguedmiclient.util.CddUtils
import org.slf4j.Logger
import org.slf4j.LoggerFactory
import org.springframework.core.env.Environment

import java.time.Instant
import java.time.LocalDateTime
import java.time.ZoneOffset

/**
 * <h1>Student Placement Colleague Interface</h1>
 * <summary>
 *     <p>Provides a Wrapper around the custom placement staging table:</p>
 *     <ol>
 *         <li>Storing and populating Student Placement data into the custom staging table</li>
 *         <li>Retrieving Student Placement data from the custom staging table</li>
 *     <ol>
 * </summary>
 *
 * @version 4.0.0
 *
 */
class StudentPlacement {

    protected final static Logger log = LoggerFactory.getLogger(StudentPlacement.class)

    protected DmiDataService dmiDataService
    protected EntityMetadataService entityMetadataService
    protected DataWriter dataWriter

    /**
     * Initialize
     * @param misCode The College Code used for grabbing college specific settings from the environment
     * @param environment The college-specific configurations properties
     * @param services the pre-loaded Services containing the underlying data connections
     */
    void colleagueInit(String misCode, Environment environment, ClassMap services) {

        this.dmiDataService = services.get(DmiDataService.class)
        this.entityMetadataService = dmiDataService.getEntityMetadataService()

        def dmiCTXService = services.get(DmiCTXService.class)
        this.dataWriter = new DataWriter(dmiCTXService)

        def dmiService = services.get(DmiService.class)
        ColleagueUtils.keepAlive(dmiService)
    }

    /**
     * Gets Student Placement Data from the staging table
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
        log.debug("get: retrieving placement meta data")
        EntityMetadata entity = entityMetadataService.get("ST", "XCTC.PLMT.PLACEMENT")
        if(entity == null) {
            throw new InternalServerException("Placement Table Meta data not found")
        }
        // get column names from the table, excluding any that aren't physical columns (no field placement)
        List<String> columnNames = entity.entries
                .findAll { it.value.fieldPlacement != null }
                .collect { it.value.name }
        log.debug("get: retrieved placement meta data")

        log.debug("get: building search criteria")
        StringBuilder criteria = new StringBuilder()
        criteria.append("XCTC.PLMT.MIS.CODE = ")
                .append(ColleagueUtils.quoteString(misCode))
                .append("AND XCTC.PLMT.CCCID = ")
                .append(ColleagueUtils.quoteString(cccId))

        if(theSSID != null) {
            criteria.append("AND XCTC.PLMT.SSID = ")
                    .append(ColleagueUtils.quoteString(theSSID))
        }

        log.debug("get: retrieving matching placement data")
        List<ColleagueData> studentPlacements = dmiDataService.batchSelect(
                "ST",
                "XCTC.PLMT.PLACEMENT",
                columnNames,
                criteria.toString()
        )

        log.debug("get: checking if exists")
        if(studentPlacements.size() == 0) {
            throw new EntityNotFoundException("Student placement data not found")
        }

        ColleagueData mostRecent = studentPlacements.last()
        log.debug("get: mapping data to rich student object")
        StudentPlacementData result = this.convertColleagueDataToAStudentPlacementData(mostRecent)

        log.debug("get: done")
        return result
    }

    /**
     * Sets Student Placement Data into the custom staging table using the dataWriter transaction
     * @param misCode The College Code used for grabbing college specific settings from the environment
     * @param placementData The placement data to set into the staging table
     */
    void post(String misCode, StudentPlacementData placementData) {
        log.debug("post: setting student placement data")

        //****** Validate parameters ****
        if (!misCode) {
            throw new InvalidRequestException(InvalidRequestException.Errors.invalidRequest, "Mis Code cannot be null or blank")
        }
        if (placementData == null) {
            throw new InvalidRequestException(InvalidRequestException.Errors.invalidRequest, "Student Placement data cannot be null")
        }
        if (!placementData.californiaCommunityCollegeId) {
            throw new InvalidRequestException(InvalidRequestException.Errors.invalidRequest, "californiaCommunityCollegeId cannot be null or blank")
        }
        if (!placementData.miscode) {
            throw new InvalidRequestException(InvalidRequestException.Errors.invalidRequest, "placement misCode cannot be null or blank")
        }

        // ensure mis code in url matches body
        if (placementData.miscode != misCode) {
            throw new InvalidRequestException(InvalidRequestException.Errors.invalidSearchCriteria, "MIS Code in body does not match URL")
        }
        log.debug("post: parameters validated.")

        log.debug("post: grabbing placement field metadata.")
        EntityMetadata entity = entityMetadataService.get("ST", "XCTC.PLMT.PLACEMENT")
        if(entity == null) {
            throw new InternalServerException("Placement Table Meta data for post not found")
        }
        log.debug("post: grabbed placement field metadata.")

        log.debug("post: parsing placement.")
        String[] record = this.convertStudentPlacementDataToStringArray(entity.entries, placementData)
        log.debug("post: placement data parsed into record: writing " + record)

        DataWriteResult dwr = dataWriter.write("XCTC.PLMT.PLACEMENT", "", true, record.toList())

        if(dwr.errorOccurred) {
            if(dwr.errorMessages && dwr.errorMessages.size() > 0) {
                for (String error in dwr.errorMessages) {
                    log.error("post: dataWriteError: [" + error + "]")
                }
                throw new InternalServerException(dwr.errorMessages[0])
            } else {
                throw new InternalServerException("Unknown Error")
            }
        } else {
            log.debug("post: record written as [" + dwr.id + "]. done")
        }
    }

    /**
     * Converts a string array into an StudentPlacementData Object
     * @param results The string array to convert to an Student Placement Data Object
     * @retuns a Student Placement Data object with each field populated from the string array
     */
    protected StudentPlacementData convertColleagueDataToAStudentPlacementData(ColleagueData placement) {
        if(placement == null) {
            return null
        }

        StudentPlacementData data = new StudentPlacementData()

        data.californiaCommunityCollegeId  = (placement.values["XCTC.PLMT.CCCID"] as String)
        data.statewideStudentId  = (placement.values["XCTC.PLMT.SSID"] as String)
        data.miscode = (placement.values["XCTC.PLMT.MIS.CODE"] as String)
        data.placementStatus = (placement.values["XCTC.PLMT.PLACEMENT.STATUS"] as String)
        data.dataSource = (placement.values["XCTC.PLMT.DATA.SOURCE"] as String)?.toInteger()

        data.english = (placement.values["XCTC.PLMT.ENGLISH"] as String)?.toInteger()
        data.slam = (placement.values["XCTC.PLMT.SLAM.SUPPORT"] as String)?.toInteger()
        data.stem = (placement.values["XCTC.PLMT.STEM.SUPPORT"] as String)?.toInteger()

        String isAlg1 = (placement.values["XCTC.PLMT.IS.ALG.I"] as String)
        if(StringUtils.equalsIgnoreCase("Y", isAlg1)) {
            data.isAlgI = true
        } else if(StringUtils.equalsIgnoreCase("N", isAlg1)) {
            data.isAlgI = false
        }

        String isAlg2 = (placement.values["XCTC.PLMT.IS.ALG.II"] as String)
        if(StringUtils.equalsIgnoreCase("Y", isAlg2)) {
            data.isAlgII = true
        } else if(StringUtils.equalsIgnoreCase("N", isAlg2)) {
            data.isAlgII = false
        }

        String trig = (placement.values["XCTC.PLMT.TRIGONOMETRY"] as String)
        if(StringUtils.equalsIgnoreCase("Y", trig)) {
            data.trigonometry = true
        } else if(StringUtils.equalsIgnoreCase("N", trig)) {
            data.trigonometry = false
        }

        String preCalc = (placement.values["XCTC.PLMT.PRE.CALCULUS"] as String)
        if(StringUtils.equalsIgnoreCase("Y", preCalc)) {
            data.preCalculus = true
        } else if(StringUtils.equalsIgnoreCase("N", preCalc)) {
            data.preCalculus = false
        }

        String calc = (placement.values["XCTC.PLMT.CALCULUS"] as String)
        if(StringUtils.equalsIgnoreCase("Y", calc)) {
            data.calculus = true
        } else if(StringUtils.equalsIgnoreCase("N", calc)) {
            data.calculus = false
        }

        String eleventhGrade = (placement.values["XCTC.PLMT.COMPLETED.11TH.GRD"] as String)
        if(StringUtils.equalsIgnoreCase("Y", eleventhGrade)) {
            data.completedEleventhGrade = true
        } else if(StringUtils.equalsIgnoreCase("N", eleventhGrade)) {
            data.completedEleventhGrade = false
        }
        data.cumulativeGradePointAverage = (placement.values["XCTC.PLMT.CUMULATIVE.GPA"] as String)?.toFloat()

        data.englishCompletedCourseId = (placement.values["XCTC.PLMT.ENG.COMP.CRS.ID"] as String)?.toInteger()
        data.englishCompletedCourseGrade = (placement.values["XCTC.PLMT.ENG.COMP.CRS.GRD"] as String)

        data.mathematicsCompletedCourseId = (placement.values["XCTC.PLMT.MATH.COMP.CRS.ID"] as String)?.toInteger()
        data.mathematicsCompletedCourseGrade = (placement.values["XCTC.PLMT.MATH.COMP.CRS.GRD"] as String)

        data.mathematicsPassedCourseId = (placement.values["XCTC.PLMT.MATH.PASS.CRS.ID"] as String)?.toInteger()
        data.mathematicsPassedCourseGrade = (placement.values["XCTC.PLMT.MATH.PASS.CRS.GRD"] as String)

        data.appId = (placement.values["XCTC.PLMT.APP.ID"] as String)?.toLong()

        data.highestGradeCompleted = (placement.values["XCTC.PLMT.HIGHEST.GRADE.COMP"] as String)

        String erpDate = (placement.values["XCTC.PLMT.ERP.DATE"] as String)
        String erpTime = (placement.values["XCTC.PLMT.ERP.TIME"] as String)
        LocalDateTime ERPTransmit = ColleagueUtils.localDateTimeFromString(erpDate, erpTime)
        if(ERPTransmit) {
            data.tstmpERPTransmit = ERPTransmit.atOffset(ZoneOffset.UTC).toInstant().toEpochMilli()
        }


        String sisDate = (placement.values["XCTC.PLMT.ACTIVITY.DATE"] as String)
        String sisTime = (placement.values["XCTC.PLMT.ACTIVITY.TIME"] as String)
        data.tstmpSISTransmit = ColleagueUtils.localDateTimeFromString(sisDate, sisTime)

        String processedDate = (placement.values["XCTC.PLMT.PROCESSED.DATE"] as String)
        String processedTime = (placement.values["XCTC.PLMT.PROCESSED.TIME"] as String)
        data.tstmpSISProcessed = ColleagueUtils.localDateTimeFromString(processedDate, processedTime)
        if(StringUtils.isNotEmpty(processedDate)) {
            data.sisProcessedFlag = "Y"
        } else {
            data.sisProcessedFlag = "N"
        }
        data.sisProcessedNotes = (placement.values["XCTC.PLMT.USER1"] as String)

        return data
    }

    protected String[] convertStudentPlacementDataToStringArray(Map<String, CddEntry> fields, StudentPlacementData placementData) {
        if(!placementData) {
            throw new InternalServerException("placement cannot be null")
        }

        if(!fields || fields.size() < 0) {
            throw new InternalServerException("fields cannot be null")
        }

        String[] result = new String [fields.max{ it.value.fieldPlacement }.value.fieldPlacement]

        this.mapValue(ColleagueUtils.sanitize(placementData.californiaCommunityCollegeId), fields["XCTC.PLMT.CCCID"], result)
        this.mapValue(ColleagueUtils.sanitize(placementData.statewideStudentId), fields["XCTC.PLMT.SSID"], result)
        this.mapValue(ColleagueUtils.sanitize(placementData.miscode), fields["XCTC.PLMT.MIS.CODE"], result)
        this.mapValue(placementData.dataSource, fields["XCTC.PLMT.DATA.SOURCE"], result)
        this.mapValue(placementData.english, fields["XCTC.PLMT.ENGLISH"], result)
        this.mapValue(placementData.slam, fields["XCTC.PLMT.SLAM.SUPPORT"], result)
        this.mapValue(placementData.stem, fields["XCTC.PLMT.STEM.SUPPORT"], result)
        this.mapValue(placementData.isAlgI, fields["XCTC.PLMT.IS.ALG.I"], result)
        this.mapValue(placementData.isAlgII, fields["XCTC.PLMT.IS.ALG.II"], result)
        this.mapValue(placementData.trigonometry, fields["XCTC.PLMT.TRIGONOMETRY"], result)
        this.mapValue(placementData.preCalculus, fields["XCTC.PLMT.PRE.CALCULUS"], result)
        this.mapValue(placementData.calculus, fields["XCTC.PLMT.CALCULUS"], result)
        this.mapValue(placementData.completedEleventhGrade, fields["XCTC.PLMT.COMPLETED.11TH.GRD"], result)
        this.mapValue(placementData.cumulativeGradePointAverage, fields["XCTC.PLMT.CUMULATIVE.GPA"], result)
        this.mapValue(placementData.englishCompletedCourseId, fields["XCTC.PLMT.ENG.COMP.CRS.ID"], result)
        this.mapValue(ColleagueUtils.sanitize(placementData.englishCompletedCourseGrade), fields["XCTC.PLMT.ENG.COMP.CRS.GRD"], result)
        this.mapValue(ColleagueUtils.sanitize(placementData.mathematicsCompletedCourseGrade), fields["XCTC.PLMT.MATH.COMP.CRS.GRD"], result)
        this.mapValue(ColleagueUtils.sanitize(placementData.mathematicsPassedCourseGrade), fields["XCTC.PLMT.MATH.PASS.CRS.GRD"], result)
        this.mapValue(placementData.mathematicsCompletedCourseId, fields["XCTC.PLMT.MATH.COMP.CRS.ID"], result)
        this.mapValue(placementData.mathematicsPassedCourseId, fields["XCTC.PLMT.MATH.PASS.CRS.ID"], result)
        this.mapValue(ColleagueUtils.sanitize(placementData.placementStatus), fields["XCTC.PLMT.PLACEMENT.STATUS"], result)
        this.mapValue(placementData.tstmpSISTransmit, fields["XCTC.PLMT.ACTIVITY.DATE"], result)
        this.mapValue(placementData.tstmpSISTransmit, fields["XCTC.PLMT.ACTIVITY.TIME"], result)

        LocalDateTime erpDateTime = null
        if(placementData.tstmpERPTransmit != null && placementData.tstmpERPTransmit > 0) {
            erpDateTime  = LocalDateTime.ofInstant(Instant.ofEpochMilli(placementData.tstmpERPTransmit), ZoneOffset.UTC)
        }
        this.mapValue(erpDateTime, fields["XCTC.PLMT.ERP.DATE"], result)
        this.mapValue(erpDateTime, fields["XCTC.PLMT.ERP.TIME"], result)

        this.mapValue(placementData.appId, fields["XCTC.PLMT.APP.ID"], result)
        this.mapValue(ColleagueUtils.sanitize(placementData.highestGradeCompleted), fields["XCTC.PLMT.HIGHEST.GRADE.COMP"], result)

        return result
    }
    
    /**
     * Map a value to the correct location in a record for transmission to Colleague
     */
    protected void mapValue(Object value, CddEntry cdd, String[] record) {
        if (cdd != null && cdd.fieldPlacement != null && cdd.fieldPlacement > 0) {
            def v = CddUtils.convertFromValue(value, cdd, true)
            record[cdd.fieldPlacement - 1] = v
        }
    }
}
