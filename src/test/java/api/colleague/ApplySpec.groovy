package api.colleague

import api.colleague.Apply as GroovyApply
import com.ccctc.adaptor.exception.EntityConflictException
import com.ccctc.adaptor.exception.EntityNotFoundException
import com.ccctc.adaptor.exception.InternalServerException
import com.ccctc.adaptor.exception.InvalidRequestException
import com.ccctc.adaptor.model.apply.Application
import com.ccctc.adaptor.model.apply.SupplementalQuestions
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

class ApplySpec extends Specification {

    String misCode = "000"
    GroovyApply groovyApply
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

        groovyApply = new GroovyApply()
        groovyApply.colleagueInit(misCode, null, services)

        def cddEntries = [
                CddEntry.builder().name("XCTC.CA.FIRSTNAME").fieldPlacement(1).build(),
                CddEntry.builder().name("XCTC.CA.GRADE.POINT.AVERAGE").fieldPlacement(2).build(),
                CddEntry.builder().name("XCTC.CA.BIRTHDATE").fieldPlacement(3).build(),
                CddEntry.builder().name("XCTC.CA.EMAIL").fieldPlacement(4).build(),
                CddEntry.builder().name("XCTC.CA.SUPP.CHECK.01").fieldPlacement(5).build(),
                CddEntry.builder().name("XCTC.CA.SUFFIX").fieldPlacement(6).build()
        ]

        entityMetadata = new EntityMetadata("PHYS", null,
                cddEntries.collectEntries { i -> [(i.name): i]},
                cddEntries as CddEntry[])

    }

    def "bad input"() {
        when: groovyApply.get(null, null)
        then: thrown InvalidRequestException
        when: groovyApply.get("000", null)
        then: thrown InvalidRequestException
        when: groovyApply.post(null, null)
        then: thrown InvalidRequestException
        when: groovyApply.post("000", null)
        then: thrown InvalidRequestException
        when: groovyApply.post("000", new Application())
        then: thrown InvalidRequestException
    }

    def "get"() {
        setup:
        ColleagueData data = new ColleagueData("1", [
                "XCTC.CA.FIRSTNAME": "name",
                "XCTC.CA.GRADE.POINT.AVERAGE": new BigDecimal("3.51"),
                "XCTC.CA.BIRTHDATE": LocalDate.of(2000, 1, 1),
                "XCTC.CA.EMAIL": "email@email.com",
                "XCTC.CA.SUPP.CHECK.01": "Y",
                "XCTC.CA.SUPP.CHECK.02": "N",
                "XCTC.CA.SUPP.CHECK.03": null,
                "XCTC.CA.SUFFIX": "Jr.",
                "XCTC.CA.TSTMP.CREATE.DATE" : LocalDate.of(2018, 1, 1),
                "XCTC.CA.TSTMP.CREATE.TIME" : LocalTime.of(10, 30),
                "XCTC.CA.TSTMP.UPDATE.DATE" : LocalDate.of(2018, 1, 1),
                "XCTC.CA.CA.COLLEGE.EMPLOYEE" : "Y",
                "XCTC.CA.CA.SCHOOL.EMPLOYEE" : "N",
                "XCTC.CA.CA.SEASONAL.AG" : null

        ])

        when:
        Application result = groovyApply.get(misCode, 1L)

        then:
        1 * entityMetadataService.get(*_) >> entityMetadata
        1 * dmiDataService.singleKey(*_) >> data
        result.appId == 1L
        result.firstname == "name"
        result.gradePointAverage == "3.51"
        result.birthdate == LocalDate.of(2000, 1, 1)
        result.email == "email@email.com"
        result.supplementalQuestions.suppCheck01 == true
        result.supplementalQuestions.suppCheck02 == false
        result.supplementalQuestions.suppCheck03 == null
        result.suffix == "Jr."
        result.tstmpCreate == LocalDateTime.of(2018, 1, 1, 10, 30)
        result.tstmpUpdate == LocalDateTime.of(2018, 1, 1, 0, 0)
        result.supplementalQuestions.appId == result.appId
        result.caCollegeEmployee == true
        result.caSchoolEmployee == false
        result.caSeasonalAg == null
    }

    def "get - no supplemental"() {
        setup:
        ColleagueData data = new ColleagueData("1", [
                "XCTC.CA.FIRSTNAME": "name"
        ])

        when:
        Application result = groovyApply.get(misCode, 1L)

        then:
        1 * entityMetadataService.get(*_) >> entityMetadata
        1 * dmiDataService.singleKey(*_) >> data
        result.appId == 1L
        result.firstname == "name"
        result.supplementalQuestions == null
    }


    def "get - not found"() {
        when:
        groovyApply.get(misCode, 1L)

        then:
        1 * entityMetadataService.get(*_) >> entityMetadata
        1 * dmiDataService.singleKey(*_) >> null
        thrown EntityNotFoundException
    }

    def "post"() {
        setup:

        Application app = new Application(
                appId: 1L,
                cccId: "abc123",
                collegeId: misCode,
                firstname: "name",
                highestMathCourseTaken: 1,
                highestMathCoursePassed: 1,
                highestMathPassedGrade: "A",
                tstmpCreate: LocalDateTime.now(ZoneOffset.UTC),
                supplementalQuestions: new SupplementalQuestions(
                    appId: 1L, suppCheck01: true)
        )

        CTXData response = new CTXData(["ErrorOccurred": false, "OutId": 1L], null)

        when:
        groovyApply.post(misCode, app)

        then:
        // Checks for existence
        1 * dmiDataService.singleKey(*_) >> null
        // Gets metadata for mapping
        1 * entityMetadataService.get(*_) >> entityMetadata
        // Insert
        1 * dmiCTXService.execute(*_) >> response
    }

    def "post - missing and bad params"() {
        setup:
        String cccId = "ABC123"
        String appId = "1"

        when: groovyApply.post(null, null)
        then: thrown InvalidRequestException

        when: groovyApply.post(misCode, null)
        then: thrown InvalidRequestException

        when: groovyApply.post(misCode, new Application())
        then: thrown InvalidRequestException

        when: groovyApply.post(misCode, new Application(collegeId: misCode))
        then: thrown InvalidRequestException

        when: groovyApply.post(misCode, new Application(collegeId: misCode, cccId: cccId))
        then: thrown InvalidRequestException

        when: groovyApply.post(misCode, new Application(collegeId: misCode, appId: appId))
        then: thrown InvalidRequestException

        when: groovyApply.post(misCode, new Application(appId: appId, collegeId: "bad-mis-code", cccId: cccId))
        then: thrown InvalidRequestException
    }

    def "post - already exists"() {
        setup:
        Application app = new Application(appId: 1L, collegeId: misCode, cccId: "ABC123")
        ColleagueData data = new ColleagueData("1", [ : ])

        when:
        groovyApply.post(misCode, app)

        then:
        // Checks for existence
        1 * dmiDataService.singleKey(*_) >> data
        thrown EntityConflictException
    }

    def "post - error"() {
        setup:
        Application app = new Application(appId: 1L, collegeId: misCode, cccId: "ABC123")

        CTXData rError1 = new CTXData(["ErrorOccurred": true], null)

        when:
        groovyApply.post(misCode, app)

        then:
        // Checks for existence
        1 * dmiDataService.singleKey(*_) >> null
        // Gets metadata for mapping
        1 * entityMetadataService.get(*_) >> entityMetadata
        // Insert
        1 * dmiCTXService.execute(*_) >> rError1
        thrown InternalServerException
    }

    def "post - error2"() {
        setup:
        Application app = new Application(appId: 1L, collegeId: misCode, cccId: "ABC123")

        CTXData rError2 = new CTXData(["ErrorOccurred": true, "ErrorMsgs": ["Error"]], null)

        when:
        groovyApply.post(misCode, app)

        then:
        // Checks for existence
        1 * dmiDataService.singleKey(*_) >> null
        // Gets metadata for mapping
        1 * entityMetadataService.get(*_) >> entityMetadata
        // Insert
        1 * dmiCTXService.execute(*_) >> rError2
        thrown InternalServerException
    }

    def "post - no out id"() {
        setup:
        Application app = new Application(appId: 1L, collegeId: misCode, cccId: "ABC123")
        CTXData response = new CTXData([:], [:])

        when:
        groovyApply.post(misCode, app)

        then:
        // Checks for existence
        1 * dmiDataService.singleKey(*_) >> null
        // Gets metadata for mapping
        1 * entityMetadataService.get(*_) >> entityMetadata
        // Insert
        1 * dmiCTXService.execute(*_) >> response
        thrown InternalServerException
    }

    def "post - invalid out id"() {
        setup:
        Application app = new Application(appId: 1L, collegeId: misCode, cccId: "ABC123")
        CTXData response = new CTXData(["OutId": "abc"], [:])

        when:
        groovyApply.post(misCode, app)

        then:
        // Checks for existence
        1 * dmiDataService.singleKey(*_) >> null
        // Gets metadata for mapping
        1 * entityMetadataService.get(*_) >> entityMetadata
        // Insert
        1 * dmiCTXService.execute(*_) >> response
        thrown InternalServerException
    }
}

