package api.colleague

import api.colleague.SharedApplication as GroovySharedApplication
import com.ccctc.adaptor.exception.EntityConflictException
import com.ccctc.adaptor.exception.EntityNotFoundException
import com.ccctc.adaptor.exception.InternalServerException
import com.ccctc.adaptor.exception.InvalidRequestException
import com.ccctc.adaptor.model.apply.Application as StandardApp
import com.ccctc.adaptor.model.apply.SharedApplication as SharedApp
import com.ccctc.adaptor.util.ClassMap
import org.ccctc.colleaguedmiclient.model.CTXData
import org.ccctc.colleaguedmiclient.model.CddEntry
import org.ccctc.colleaguedmiclient.model.ColleagueData
import org.ccctc.colleaguedmiclient.model.EntityMetadata
import org.ccctc.colleaguedmiclient.service.DmiCTXService
import org.ccctc.colleaguedmiclient.service.DmiDataService
import org.ccctc.colleaguedmiclient.service.EntityMetadataService
import spock.lang.Specification

import java.time.LocalDate
import java.time.LocalDateTime
import java.time.LocalTime
import java.time.ZoneOffset

class SharedApplicationSpec extends Specification {

    String teachMisCode = "000"
    String homeMisCode = "002"
    GroovySharedApplication groovySharedApplication
    EntityMetadataService entityMetadataService
    DmiDataService dmiDataService
    DmiCTXService dmiCTXService

    EntityMetadata entityMetadata

    def setup() {
        entityMetadataService = Mock(EntityMetadataService)
        dmiCTXService = Mock(DmiCTXService)
        dmiDataService = Mock(DmiDataService)
        ClassMap services = new ClassMap()
        services.putAll([(DmiCTXService.class): dmiCTXService, (DmiDataService.class): dmiDataService])

        dmiDataService.getEntityMetadataService() >> entityMetadataService

        groovySharedApplication = new GroovySharedApplication()
        groovySharedApplication.colleagueInit(teachMisCode, null, services)

        def cddEntries = [
                CddEntry.builder().name("XCTC.SD.FIRSTNAME").fieldPlacement(1).build(),
                CddEntry.builder().name("XCTC.SD.GRADE.POINT.AVERAGE").fieldPlacement(2).build(),
                CddEntry.builder().name("XCTC.SD.BIRTHDATE").fieldPlacement(3).build(),
                CddEntry.builder().name("XCTC.SD.EMAIL").fieldPlacement(4).build(),
                CddEntry.builder().name("XCTC.SD.SUPP.CHECK.01").fieldPlacement(5).build(),
                CddEntry.builder().name("XCTC.SD.SUFFIX").fieldPlacement(6).build()
        ]

        entityMetadata = new EntityMetadata("PHYS", null,
                cddEntries.collectEntries { i -> [(i.name): i]},
                cddEntries as CddEntry[])

    }

    def "bad input"() {
        when: groovySharedApplication.get(null, null)
        then: thrown InvalidRequestException
        when: groovySharedApplication.get("000", null)
        then: thrown InvalidRequestException
        when: groovySharedApplication.post(null, null)
        then: thrown InvalidRequestException
        when: groovySharedApplication.post("000", null)
        then: thrown InvalidRequestException
        when: groovySharedApplication.post("000", new StandardApp())
        then: thrown InvalidRequestException
    }

    def "get"() {
        setup:
        ColleagueData data = new ColleagueData("34", [
                "XCTC.SD.MIS.CODE": teachMisCode,
                "XCTC.SD.APP.ID": 1,
                "XCTC.SD.COLLEGE.ID": homeMisCode,
                "XCTC.SD.FIRSTNAME": "name",
                "XCTC.SD.GRADE.POINT.AVERAGE": new BigDecimal("3.51"),
                "XCTC.SD.BIRTHDATE": LocalDate.of(2000, 1, 1),
                "XCTC.SD.EMAIL": "email@email.com",
                "XCTC.SD.SUFFIX": "Jr.",
                "XCTC.SD.TSTMP.CREATE.DATE" : LocalDate.of(2018, 1, 1),
                "XCTC.SD.TSTMP.CREATE.TIME" : LocalTime.of(10, 30),
                "XCTC.SD.TSTMP.UPDATE.DATE" : LocalDate.of(2018, 1, 1),
                "XCTC.SD.CA.COLLEGE.EMPLOYEE" : "Y",
                "XCTC.SD.CA.SCHOOL.EMPLOYEE" : "N",
                "XCTC.SD.CA.SEASONAL.AG" : null

        ])

        when:
        SharedApp result = groovySharedApplication.get(teachMisCode, 1L)

        then:
        1 * entityMetadataService.get(*_) >> entityMetadata
        1 * dmiDataService.batchSelect(*_) >> new ArrayList<ColleagueData>([ data ])
        result.misCode == teachMisCode
        result.appId == 1L
        result.collegeId == homeMisCode
        result.firstname == "name"
        result.gradePointAverage == "3.51"
        result.birthdate == LocalDate.of(2000, 1, 1)
        result.email == "email@email.com"
        result.suffix == "Jr."
        result.tstmpCreate == LocalDateTime.of(2018, 1, 1, 10, 30)
        result.tstmpUpdate == LocalDateTime.of(2018, 1, 1, 0, 0)
        result.caCollegeEmployee == true
        result.caSchoolEmployee == false
        result.caSeasonalAg == null
    }

    def "get - no supplemental"() {
        setup:
        ColleagueData data = new ColleagueData("17", [
                "XCTC.SD.MIS.CODE": teachMisCode,
                "XCTC.SD.APP.ID": 1,
                "XCTC.SD.FIRSTNAME": "name"
        ])

        when:
        SharedApp result = groovySharedApplication.get(teachMisCode, 1L)

        then:
        1 * entityMetadataService.get(*_) >> entityMetadata
        1 * dmiDataService.batchSelect(*_) >> new ArrayList<ColleagueData>([ data ])
        result.misCode == teachMisCode
        result.appId == 1L
        result.firstname == "name"
        result.supplementalQuestions == null
    }


    def "get - not found"() {
        when:
        groovySharedApplication.get(teachMisCode, 1L)

        then:
        1 * entityMetadataService.get(*_) >> entityMetadata
        1 * dmiDataService.batchSelect(*_) >> null
        thrown EntityNotFoundException
    }

    def "post"() {
        setup:

        StandardApp app = new StandardApp(
                appId: 1L,
                cccId: "abc123",
                collegeId: homeMisCode,
                firstname: "name",
                highestMathCourseTaken: 1,
                highestMathCoursePassed: 1,
                highestMathPassedGrade: "A",
                tstmpCreate: LocalDateTime.now(ZoneOffset.UTC)
        )

        CTXData response = new CTXData(["ErrorOccurred": false, "OutId": 83L], null)

        when:
        groovySharedApplication.post(teachMisCode, app)

        then:
        // Checks for existence
        1 * dmiDataService.batchSelect(*_) >> null
        // Gets metadata for mapping
        1 * entityMetadataService.get(*_) >> entityMetadata
        // Insert
        1 * dmiCTXService.execute(*_) >> response
    }

    def "post - missing and bad params"() {
        setup:
        String cccId = "ABC123"
        String appId = "1"

        when: groovySharedApplication.post(null, null)
        then: thrown InvalidRequestException

        when: groovySharedApplication.post(teachMisCode, null)
        then: thrown InvalidRequestException

        when: groovySharedApplication.post(teachMisCode, new StandardApp())
        then: thrown InvalidRequestException

        when: groovySharedApplication.post(teachMisCode, new StandardApp(collegeId: homeMisCode))
        then: thrown InvalidRequestException

        when: groovySharedApplication.post(teachMisCode, new StandardApp(collegeId: homeMisCode, cccId: cccId))
        then: thrown InvalidRequestException

        when: groovySharedApplication.post(teachMisCode, new StandardApp(collegeId: homeMisCode, appId: appId))
        then: thrown InvalidRequestException

        when: groovySharedApplication.post(teachMisCode, new StandardApp(appId: appId, collegeId: teachMisCode, cccId: cccId))
        then: thrown InvalidRequestException
    }

    def "post - already exists"() {
        setup:
        StandardApp app = new StandardApp(appId: 1L, collegeId: homeMisCode, cccId: "ABC123")
        ColleagueData data = new ColleagueData("96", [ : ])

        when:
        groovySharedApplication.post(teachMisCode, app)

        then:
        // Checks for existence
        1 * dmiDataService.batchSelect(*_) >> new ArrayList<ColleagueData>([ data ])
        thrown EntityConflictException
    }

    def "post - error"() {
        setup:
        StandardApp app = new StandardApp(appId: 1L, collegeId: homeMisCode, cccId: "ABC123")

        CTXData rError1 = new CTXData(["ErrorOccurred": true], null)

        when:
        groovySharedApplication.post(teachMisCode, app)

        then:
        // Checks for existence
        1 * dmiDataService.batchSelect(*_) >> null
        // Gets metadata for mapping
        1 * entityMetadataService.get(*_) >> entityMetadata
        // Insert
        1 * dmiCTXService.execute(*_) >> rError1
        thrown InternalServerException
    }

    def "post - error2"() {
        setup:
        StandardApp app = new StandardApp(appId: 1L, collegeId: homeMisCode, cccId: "ABC123")

        CTXData rError2 = new CTXData(["ErrorOccurred": true, "ErrorMsgs": ["Error"]], null)

        when:
        groovySharedApplication.post(teachMisCode, app)

        then:
        // Checks for existence
        1 * dmiDataService.batchSelect(*_) >> null
        // Gets metadata for mapping
        1 * entityMetadataService.get(*_) >> entityMetadata
        // Insert
        1 * dmiCTXService.execute(*_) >> rError2
        thrown InternalServerException
    }

    def "post - no out id"() {
        setup:
        StandardApp app = new StandardApp(appId: 1L, collegeId: homeMisCode, cccId: "ABC123")
        CTXData response = new CTXData([:], [:])

        when:
        groovySharedApplication.post(teachMisCode, app)

        then:
        // Checks for existence
        1 * dmiDataService.batchSelect(*_) >> null
        // Gets metadata for mapping
        1 * entityMetadataService.get(*_) >> entityMetadata
        // Insert
        1 * dmiCTXService.execute(*_) >> response
        thrown InternalServerException
    }

    def "post - invalid out id"() {
        setup:
        StandardApp app = new StandardApp(appId: 1L, collegeId: homeMisCode, cccId: "ABC123")
        CTXData response = new CTXData(["OutId": "abc"], [:])

        when:
        groovySharedApplication.post(teachMisCode, app)

        then:
        // Checks for existence
        1 * dmiDataService.batchSelect(*_) >> null
        // Gets metadata for mapping
        1 * entityMetadataService.get(*_) >> entityMetadata
        // Insert
        1 * dmiCTXService.execute(*_) >> response
        thrown InternalServerException
    }
}

