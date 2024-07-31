/**
 * Created by Rasul on 6/15/16.
 */
package api.banner

import com.ccctc.adaptor.exception.EntityNotFoundException
import com.ccctc.adaptor.model.placement.StudentPlacementData
import com.ccctc.adaptor.util.MisEnvironment
import groovy.sql.Sql
import groovy.util.logging.Slf4j
import oracle.sql.TIMESTAMP
import org.springframework.core.env.Environment

import java.sql.Date
import java.sql.Timestamp
import java.time.LocalDate
import java.time.LocalDateTime
import java.time.format.DateTimeFormatter

@Slf4j
class StudentPlacement {

    Environment environment

    static Map<String, MetaProperty> appProperties = StudentPlacementData.metaClass.properties.collectEntries { [(it.name.toUpperCase()): it] }
    static DateTimeFormatter dateTimeFormat = DateTimeFormatter.ofPattern ("yyyy-MM-dd HH:mm:ss" )

    def get(String misCode, String cccId, String ssId){

        Sql sql = BannerConnection.getSession(environment, misCode)
        def retPlacement = new StudentPlacementData()
        def getPlacementQuery = MisEnvironment.getProperty(environment, misCode, "sqljdbc.assessment.placement.getQuery")
        def retPlacementQuery = sql.firstRow(getPlacementQuery, [cccId: cccId, misCode: misCode, ssId: ssId])
        mapValues(appProperties, retPlacementQuery, retPlacement)
        retPlacement
    }

    def post(String misCode, StudentPlacementData placement) {
        def retPlacement = new StudentPlacementData()
        def staging = MisEnvironment.getProperty(environment, misCode, "sqljdbc.assessment.audit")
        if (staging == '1') {
            Sql sql = BannerConnection.getSession(environment, misCode)
            try {
                def student
                if (MisEnvironment.getProperty(environment, misCode, "sqljdbc.assessment.validateId") == '1') {
                    def query = MisEnvironment.getProperty(environment, misCode, "sqljdbc.cccid.getQuery")
                    student = sql.firstRow(query, [cccid: placement.californiaCommunityCollegeId])
                    if (!student) {
                        throw new EntityNotFoundException("Student not found")
                    }
                }
                def placementSequence = createPlacement(student, placement, misCode, sql)
                def placementSequenceValue = placementSequence.getAt("plmtSequence")
                def postGetPlacementQuery = MisEnvironment.getProperty(environment, misCode, "sqljdbc.assessment.placement.postGetQuery")
                def retPlacementQuery = sql.firstRow(postGetPlacementQuery, [seqNum: placementSequenceValue])
                mapValues(appProperties, retPlacementQuery, retPlacement)
            } finally {
                sql.close()
            }
        }
        return retPlacement
    }

    def createPlacement(def student, StudentPlacementData placement, String misCode, Sql sql) {
        def query, placementSequence
        query = MisEnvironment.getProperty(environment, misCode, "sqljdbc.assessment.placement.getSequence")
        placementSequence = sql.firstRow(query)
        query = MisEnvironment.getProperty(environment, misCode, "sqljdbc.assessment.placement.insertQuery")
        sql.execute(query, [
                plmtSequence            : placementSequence.plmtSequence,
                pidm                    : student?.pidm,
                misCode                 : misCode,
                cccid                   : placement.californiaCommunityCollegeId,
                ssid                    : placement.statewideStudentId,
                dataSource              : placement.dataSource,
                english                 : placement.english,
                slamSupport             : placement.slam,
                stemSupport             : placement.stem,
                isAlgI                  : Utility.convertBoolToYesNo(placement.isAlgI),
                isAlgII                 : Utility.convertBoolToYesNo(placement.isAlgII),
                trigonometry            : Utility.convertBoolToYesNo(placement.trigonometry),
                preCalculus             : Utility.convertBoolToYesNo(placement.preCalculus),
                calculus                : Utility.convertBoolToYesNo(placement.calculus),
                comp11thGrade           : Utility.convertBoolToYesNo(placement.completedEleventhGrade),
                cumulativeGPA           : placement.cumulativeGradePointAverage,
                engCompCourseId         : placement.englishCompletedCourseId,
                engCompCourseGrade      : placement.englishCompletedCourseGrade,
                mathCompCourseId        : placement.mathematicsCompletedCourseId,
                mathCompCourseGrade     : placement.mathematicsCompletedCourseGrade,
                mathPassCourseId        : placement.mathematicsPassedCourseId,
                mathPassCourseGrade     : placement.mathematicsPassedCourseGrade,
                placementStatus         : placement.placementStatus,
                tstmpERPTransmit        : new Timestamp(placement.tstmpERPTransmit),
                tstmpSISTransmit        : placement.tstmpSISTransmit != null ? Timestamp.valueOf(placement.tstmpSISTransmit) : null,
                appId                   : placement.appId,
                highestGradeCompleted   : placement.highestGradeCompleted]
        )
        placementSequence
    }

    static void mapValues(Map<String, MetaProperty> propertyMap, Map source, Object destination) {
        // map values from SQL Map to any Object
        if( source?.containsKey("mathematicsCompletedCrseGrade"))
            source.put("mathematicsCompletedCourseGrade", source.get("mathematicsCompletedCrseGrade"))
        if( source?.containsKey("tstmpERPTransmit"))
            source.put("tstmpERPTransmit", ((Timestamp)source.get("tstmpERPTransmit")).time)
        Utility.mapValues(propertyMap, source, destination)
    }
}