package api.colleague

import api.colleague.StudentPlacement as GroovyStudentPlacement
import com.ccctc.adaptor.exception.InternalServerException
import com.ccctc.adaptor.exception.InvalidRequestException
import com.ccctc.adaptor.model.placement.StudentPlacementData
import com.ccctc.adaptor.util.ClassMap
import org.ccctc.colleaguedmiclient.model.CTXData
import org.ccctc.colleaguedmiclient.model.CddEntry
import org.ccctc.colleaguedmiclient.model.ColleagueData
import org.ccctc.colleaguedmiclient.model.EntityMetadata
import org.ccctc.colleaguedmiclient.service.DmiCTXService
import org.ccctc.colleaguedmiclient.service.DmiDataService
import org.ccctc.colleaguedmiclient.service.DmiService
import org.ccctc.colleaguedmiclient.service.EntityMetadataService
import org.springframework.cache.Cache
import org.springframework.core.env.Environment
import spock.lang.Specification

import java.time.LocalDateTime
import java.time.ZoneOffset
import java.time.format.DateTimeFormatter

class StudentPlacementSpec extends Specification {

    EntityMetadataService entityMetadataService
    EntityMetadata entityMetadata

    Environment environment = Mock(Environment)
    DmiService dmiService = Mock(DmiService)
    DmiDataService dmiDataService = Mock(DmiDataService)
    DmiCTXService dmiCTXService = Mock(DmiCTXService)
    Cache cache = Mock(Cache)
    GroovyStudentPlacement groovyStudentPlacement = new GroovyStudentPlacement()

    String misCode = "000"
    String cccId = "ABC123"
    String ssId = "B123456"
    LocalDateTime now = LocalDateTime.now(ZoneOffset.UTC)
    Long nanoseconds = Long.parseLong(now.format(DateTimeFormatter.ofPattern("n")))
    LocalDateTime nowZeroNano = now.minusNanos(nanoseconds)
    LocalDateTime lastWeek = nowZeroNano.minusWeeks(1)
    String activityDateString = now.format(DateTimeFormatter.ofPattern("yyyy-MM-dd"))
    String activityTimeString = now.format(DateTimeFormatter.ofPattern("HH:mm:ss"))
    String erpDateString = lastWeek.format(DateTimeFormatter.ofPattern("yyyy-MM-dd"))
    String erpTimeString = lastWeek.format(DateTimeFormatter.ofPattern("HH:mm:ss"))

    StudentPlacementData placement = new StudentPlacementData(
            miscode: misCode,
            californiaCommunityCollegeId: cccId,
            statewideStudentId: ssId,
            dataSource: 3,
            tstmpERPTransmit: lastWeek.atOffset(ZoneOffset.UTC).toInstant().toEpochMilli(),
            english: 1,
            slam: 1,
            stem: 1,
            isAlgI: true,
            isAlgII: false,
            trigonometry: false,
            preCalculus: false,
            placementStatus: "COMPLETE_PLACEMENT",
            calculus: false,
            completedEleventhGrade: true,
            cumulativeGradePointAverage: 3.89,
            englishCompletedCourseId: 3,
            englishCompletedCourseGrade: 'A',
            mathematicsCompletedCourseId: 10,
            mathematicsCompletedCourseGrade: 'D',
            mathematicsPassedCourseId: 9,
            mathematicsPassedCourseGrade: 'C'
    )


    def setup() {

        ClassMap services = new ClassMap()
        services.putAll([
                (DmiService.class): dmiService,
                (DmiCTXService.class): dmiCTXService,
                (DmiDataService.class): dmiDataService,
                (Cache.class): cache
        ])

        entityMetadataService = Mock(EntityMetadataService)
        dmiDataService.getEntityMetadataService() >> entityMetadataService

        groovyStudentPlacement.colleagueInit(misCode, environment, services)

        CddEntry[] cddEntries = [
                CddEntry.builder().name("XCTC.PLMT.CCCID").fieldPlacement(3).build(),
                CddEntry.builder().name("XCTC.PLMT.SSID").fieldPlacement(4).build(),
                CddEntry.builder().name("XCTC.PLMT.MIS.CODE").fieldPlacement(5).build(),
                CddEntry.builder().name("XCTC.PLMT.DATA.SOURCE").fieldPlacement(6).build(),
                CddEntry.builder().name("XCTC.PLMT.ENGLISH").fieldPlacement(7).build(),
                CddEntry.builder().name("XCTC.PLMT.SLAM.SUPPORT").fieldPlacement(8).build(),
                CddEntry.builder().name("XCTC.PLMT.STEM.SUPPORT").fieldPlacement(9).build(),
                CddEntry.builder().name("XCTC.PLMT.IS.ALG.I").fieldPlacement(10).build(),
                CddEntry.builder().name("XCTC.PLMT.IS.ALG.II").fieldPlacement(11).build(),
                CddEntry.builder().name("XCTC.PLMT.TRIGONOMETRY").fieldPlacement(12).build(),
                CddEntry.builder().name("XCTC.PLMT.PRE.CALCULUS").fieldPlacement(13).build(),
                CddEntry.builder().name("XCTC.PLMT.CALCULUS").fieldPlacement(14).build(),
                CddEntry.builder().name("XCTC.PLMT.COMPLETED.11TH.GRD").fieldPlacement(15).build(),
                CddEntry.builder().name("XCTC.PLMT.CUMULATIVE.GPA").fieldPlacement(16).build(),
                CddEntry.builder().name("XCTC.PLMT.ENG.COMP.CRS.ID").fieldPlacement(17).build(),
                CddEntry.builder().name("XCTC.PLMT.ENG.COMP.CRS.GRD").fieldPlacement(18).build(),
                CddEntry.builder().name("XCTC.PLMT.MATH.COMP.CRS.GRD").fieldPlacement(19).build(),
                CddEntry.builder().name("XCTC.PLMT.MATH.PASS.CRS.GRD").fieldPlacement(20).build(),
                CddEntry.builder().name("XCTC.PLMT.MATH.COMP.CRS.ID").fieldPlacement(21).build(),
                CddEntry.builder().name("XCTC.PLMT.MATH.PASS.CRS.ID").fieldPlacement(22).build(),
                CddEntry.builder().name("XCTC.PLMT.PLACEMENT.STATUS").fieldPlacement(23).build(),
                CddEntry.builder().name("XCTC.PLMT.ACTIVITY.DATE").fieldPlacement(24).build(),
                CddEntry.builder().name("XCTC.PLMT.ACTIVITY.TIME").fieldPlacement(25).build(),
                CddEntry.builder().name("XCTC.PLMT.ERP.DATE").fieldPlacement(26).build(),
                CddEntry.builder().name("XCTC.PLMT.ERP.TIME").fieldPlacement(27).build(),
                CddEntry.builder().name("XCTC.PLMT.APP.ID").fieldPlacement(28).build(),
                CddEntry.builder().name("XCTC.PLMT.PROCESSED.DATE").fieldPlacement(29).build(),
                CddEntry.builder().name("XCTC.PLMT.PROCESSED.TIME").fieldPlacement(30).build(),
                CddEntry.builder().name("XCTC.PLMT.PROCESSED.ID").fieldPlacement(31).build(),
                CddEntry.builder().name("XCTC.PLMT.USER1").fieldPlacement(32).build(),
                CddEntry.builder().name("XCTC.PLMT.USER2").fieldPlacement(33).build(),
                CddEntry.builder().name("XCTC.PLMT.USER3").fieldPlacement(33).build(),
                CddEntry.builder().name("XCTC.PLMT.LIST.USER1").fieldPlacement(34).build(),
                CddEntry.builder().name("XCTC.PLMT.LIST.USER2").fieldPlacement(35).build(),
                CddEntry.builder().name("XCTC.PLMT.LIST.USER3").fieldPlacement(36).build(),
                CddEntry.builder().name("XCTC.PLMT.APP.ID").fieldPlacement(37).build(),
                CddEntry.builder().name("XCTC.PLMT.HIGHEST.GRADE.COMP").fieldPlacement(38).build()
        ]

        entityMetadata = new EntityMetadata("PHYS", null,
                cddEntries.collectEntries { i -> [(i.name): i]},
                cddEntries)
    }

    def "get"() {
        setup:
        ColleagueData colleagueData = new ColleagueData("PLCMT_ID_1", [
                "XCTC.PLMT.CCCID"               : placement.getCaliforniaCommunityCollegeId(),
                "XCTC.PLMT.SSID"                : placement.getStatewideStudentId(),
                "XCTC.PLMT.MIS.CODE"            : placement.getMiscode(),
                "XCTC.PLMT.DATA.SOURCE"         : placement.getDataSource().toString(),
                "XCTC.PLMT.ENGLISH"             : placement.getEnglish().toString(),
                "XCTC.PLMT.SLAM.SUPPORT"        : placement.getSlam().toString(),
                "XCTC.PLMT.STEM.SUPPORT"        : placement.getStem().toString(),
                "XCTC.PLMT.IS.ALG.I"            : placement.getIsAlgI() ? 'Y' : 'N',
                "XCTC.PLMT.IS.ALG.II"           : placement.getIsAlgII() ? 'Y' : 'N',
                "XCTC.PLMT.TRIGONOMETRY"        : placement.getTrigonometry() ? 'Y' : 'N',
                "XCTC.PLMT.PRE.CALCULUS"        : placement.getPreCalculus() ? 'Y' : 'N',
                "XCTC.PLMT.CALCULUS"            : placement.getCalculus() ? 'Y' : 'N',
                "XCTC.PLMT.COMPLETED.11TH.GRD"  : placement.getCompletedEleventhGrade() ? 'Y' : 'N',
                "XCTC.PLMT.CUMULATIVE.GPA"      : placement.getCumulativeGradePointAverage().toString(),
                "XCTC.PLMT.ENG.COMP.CRS.GRD"    : placement.getEnglishCompletedCourseGrade(),
                "XCTC.PLMT.ENG.COMP.CRS.ID"     : placement.getEnglishCompletedCourseId().toString(),
                "XCTC.PLMT.MATH.COMP.CRS.GRD"   : placement.getMathematicsCompletedCourseGrade(),
                "XCTC.PLMT.MATH.PASS.CRS.GRD"   : placement.getMathematicsPassedCourseGrade(),
                "XCTC.PLMT.MATH.COMP.CRS.ID"    : placement.getMathematicsCompletedCourseId().toString(),
                "XCTC.PLMT.MATH.PASS.CRS.ID"    : placement.getMathematicsPassedCourseId().toString(),
                "XCTC.PLMT.PLACEMENT.STATUS"    : placement.getPlacementStatus(),
                "XCTC.PLMT.ACTIVITY.DATE"       : activityDateString,
                "XCTC.PLMT.ACTIVITY.TIME"       : activityTimeString,
                "XCTC.PLMT.ERP.DATE"            : erpDateString,
                "XCTC.PLMT.ERP.TIME"            : erpTimeString,
                "XCTC.PLMT.PROCESSED.DATE"      : null,
                "XCTC.PLMT.PROCESSED.TIME"      : null,
                "XCTC.PLMT.PROCESSED.ID"        : null,
                "XCTC.PLMT.USER1"               : null,
                "XCTC.PLMT.USER2"               : null,
                "XCTC.PLMT.USER3"               : null,
                "XCTC.PLMT.LIST.USER1"          : [ ],
                "XCTC.PLMT.LIST.USER2"          : [ ],
                "XCTC.PLMT.LIST.USER3"          : [ ],
                "XCTC.PLMT.APP.ID"              : placement.getAppId(),
                "XCTC.PLMT.HIGHEST.GRADE.COMP"  : placement.getHighestGradeCompleted()
        ])


        when:
        StudentPlacementData result = groovyStudentPlacement.get(misCode, cccId, ssId)

        then:
        1 * entityMetadataService.get(*_) >> entityMetadata
        1 * dmiDataService.batchSelect(*_) >> new ArrayList<ColleagueData>([
                colleagueData
        ])

        result.getCalculus() == placement.getCalculus()
        result.getCaliforniaCommunityCollegeId() == placement.getCaliforniaCommunityCollegeId()
        result.getCompletedEleventhGrade() == placement.getCompletedEleventhGrade()
        result.getCumulativeGradePointAverage() == placement.getCumulativeGradePointAverage()
        result.getDataSource() == placement.getDataSource()
        result.getEnglishCompletedCourseGrade() == placement.getEnglishCompletedCourseGrade()
        result.getEnglishCompletedCourseId() == placement.getEnglishCompletedCourseId()
        result.getEnglish() == placement.getEnglish()
        result.getIsAlgI() == placement.getIsAlgI()
        result.getIsAlgII() == placement.getIsAlgII()
        result.getMathematicsCompletedCourseGrade() == placement.getMathematicsCompletedCourseGrade()
        result.getMathematicsCompletedCourseGrade() == placement.getMathematicsCompletedCourseGrade()
        result.getMathematicsPassedCourseGrade() == placement.getMathematicsPassedCourseGrade()
        result.getMathematicsPassedCourseId() == placement.getMathematicsPassedCourseId()
        result.getMiscode() == placement.getMiscode()
        result.getPlacementStatus() == placement.getPlacementStatus()
        result.getPreCalculus() == placement.getPreCalculus()
        result.getSlam() == placement.getSlam()
        result.getStatewideStudentId() == placement.getStatewideStudentId()
        result.getStem() == placement.getStem()
        result.getTrigonometry() == placement.getTrigonometry()
        result.getTstmpERPTransmit() == placement.getTstmpERPTransmit()
    }

    def "post - missing and bad params"() {
        when: groovyStudentPlacement.post(null, null)
        then: thrown InvalidRequestException

        when: groovyStudentPlacement.post(misCode, null)
        then: thrown InvalidRequestException

        when: groovyStudentPlacement.post(misCode, new StudentPlacementData())
        then: thrown InvalidRequestException

        when: groovyStudentPlacement.post(misCode, new StudentPlacementData(miscode: misCode))
        then: thrown InvalidRequestException

        // InternalServerException due to not setting up the entity meta data
        when: groovyStudentPlacement.post(misCode, new StudentPlacementData(miscode: misCode, californiaCommunityCollegeId: cccId))
        then: thrown InternalServerException

        when: groovyStudentPlacement.post(misCode, new StudentPlacementData(miscode: "bad-mis-code", californiaCommunityCollegeId: cccId))
        then: thrown InvalidRequestException

    }

    def "post - failure"() {
        setup:
        StudentPlacementData tempPlacement = new StudentPlacementData(
                miscode: misCode,
                californiaCommunityCollegeId: cccId,
                statewideStudentId: ssId,
                dataSource: 3,
                tstmpERPTransmit: lastWeek.atOffset(ZoneOffset.UTC).toInstant().toEpochMilli(),
                english: 1,
                slam: 1,
                stem: 1,
                isAlgI: true,
                isAlgII: false,
                trigonometry: false,
                preCalculus: false,
                placementStatus: "COMPLETE_PLACEMENT",
                calculus: false,
                completedEleventhGrade: true,
                cumulativeGradePointAverage: 3.89,
                englishCompletedCourseId: 3,
                englishCompletedCourseGrade: 'A',
                mathematicsCompletedCourseId: 10,
                mathematicsCompletedCourseGrade: 'D',
                mathematicsPassedCourseId: 9,
                mathematicsPassedCourseGrade: 'C'
        )

        when:
        groovyStudentPlacement.post(misCode, tempPlacement)

        then:
        1 * entityMetadataService.get(*_) >> entityMetadata

        1 * dmiCTXService.execute("ST", "X.CCCTC.WRITE.RECORD", *_) >> new CTXData([
                "ErrorOccurred": true,
                "ErrorMsgs"     : ["Error 1"] as String[],
                "OutId"        : ""
        ], [:])

        thrown InternalServerException
    }

    def "post - success"() {
        setup:
        StudentPlacementData tempPlacement = new StudentPlacementData(
                miscode: misCode,
                californiaCommunityCollegeId: cccId,
                statewideStudentId: ssId,
                dataSource: 3,
                tstmpERPTransmit: lastWeek.atOffset(ZoneOffset.UTC).toInstant().toEpochMilli(),
                english: 1,
                slam: 1,
                stem: 1,
                isAlgI: true,
                isAlgII: false,
                trigonometry: false,
                preCalculus: false,
                placementStatus: "COMPLETE_PLACEMENT",
                calculus: false,
                completedEleventhGrade: true,
                cumulativeGradePointAverage: 3.89,
                englishCompletedCourseId: 3,
                englishCompletedCourseGrade: 'A',
                mathematicsCompletedCourseId: 10,
                mathematicsCompletedCourseGrade: 'D',
                mathematicsPassedCourseId: 9,
                mathematicsPassedCourseGrade: 'C'
        )

        when:
        groovyStudentPlacement.post(misCode, tempPlacement)

        then:
        1 * entityMetadataService.get(*_) >> entityMetadata

        1 * dmiCTXService.execute("ST", "X.CCCTC.WRITE.RECORD", *_) >> new CTXData([
                "ErrorOccurred": false,
                "ErrorMsgs"    : [],
                "OutId"        : "42"
        ], [:])
    }
}
