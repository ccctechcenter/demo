package api.colleague

import api.colleague.CCPromiseGrant as GroovyCCPG
import com.ccctc.adaptor.exception.EntityConflictException
import com.ccctc.adaptor.exception.EntityNotFoundException
import com.ccctc.adaptor.exception.InternalServerException
import com.ccctc.adaptor.exception.InvalidRequestException
import com.ccctc.adaptor.model.apply.CCPromiseGrant
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

class CCPromiseGrantSpec extends Specification {

    String misCode = "000"
    GroovyCCPG groovyCCPG
    EntityMetadataService entityMetadataService
    DmiDataService dmiDataService
    DmiCTXService dmiCTXService

    EntityMetadata entityMetadata

    def setup() {
        entityMetadataService = Mock(EntityMetadataService)
        dmiCTXService = Mock(DmiCTXService)
        dmiDataService = Mock(DmiDataService)
        dmiDataService.getEntityMetadataService() >> entityMetadataService

        ClassMap services = new ClassMap()
        services.putAll([(DmiCTXService.class): dmiCTXService, (DmiDataService.class): dmiDataService])

        groovyCCPG = new GroovyCCPG()
        groovyCCPG.colleagueInit(misCode, null, services)

        CddEntry[] cddEntries = [
                CddEntry.builder().name("XCTC.CP.FIRSTNAME").fieldPlacement(1).build(),
                CddEntry.builder().name("XCTC.CP.DEP.TOTAL.INCOME").fieldPlacement(2).build(),
                CddEntry.builder().name("XCTC.CP.BIRTHDATE").fieldPlacement(3).build(),
                CddEntry.builder().name("XCTC.CP.EMAIL").fieldPlacement(4).build(),
                CddEntry.builder().name("XCTC.CP.ACK.FIN.AID").fieldPlacement(5).build(),
                CddEntry.builder().name("XCTC.CP.YEAR.CODE").fieldPlacement(6).build()
        ]

        entityMetadata = new EntityMetadata("PHYS", null,
                cddEntries.collectEntries { i -> [(i.name): i]},
                cddEntries as CddEntry[])

    }

    def "bad input"() {
        when:
        groovyCCPG.get(null, null)
        then:
        thrown InvalidRequestException
        when:
        groovyCCPG.get("000", null)
        then:
        thrown InvalidRequestException
    }

    def "post - bad input"(){
        setup:
        String cccId = "ABC123"
        String appId = "1"

        when: groovyCCPG.post(null, null)
        then: thrown InvalidRequestException

        when: groovyCCPG.post(misCode, null)
        then: thrown InvalidRequestException

        when: groovyCCPG.post(misCode, new CCPromiseGrant())
        then: thrown InvalidRequestException

        when: groovyCCPG.post(misCode, new CCPromiseGrant(collegeId: misCode))
        then: thrown InvalidRequestException

        when: groovyCCPG.post(misCode, new CCPromiseGrant(collegeId: misCode, cccId: cccId))
        then: thrown InvalidRequestException

        when: groovyCCPG.post(misCode, new CCPromiseGrant(collegeId: misCode, appId: appId))
        then: thrown InvalidRequestException

        when: groovyCCPG.post(misCode, new CCPromiseGrant(appId: appId, collegeId: "bad-mis-code", cccId: cccId))
        then: thrown InvalidRequestException

    }

    def "get"() {
        setup:
        ColleagueData data = new ColleagueData("1", [
                "XCTC.CP.FIRSTNAME": "name",
                "XCTC.CP.DEP.TOTAL.INCOME": 9999 as Integer,
                "XCTC.CP.BIRTHDATE": LocalDate.of(2000, 1, 1),
                "XCTC.CP.EMAIL": "email@email.com",
                "XCTC.CP.ACK.FIN.AID": "Y",
                "XCTC.CP.YEAR.CODE": 1L,
                "XCTC.CP.TSTMP.CREATE.DATE" : LocalDate.of(2018, 1, 1),
                "XCTC.CP.TSTMP.CREATE.TIME" : LocalTime.of(10, 30),
                "XCTC.CP.TSTMP.UPDATE.DATE" : LocalDate.of(2018, 1, 1),
                "XCTC.CP.DETERMINED.AB540.ELI" : "Y",
                "XCTC.CP.DETERMINED.HOMELESS" : "N",
                "XCTC.CP.DETERMINED.NON.RES.E" : null

        ])

        when:
        CCPromiseGrant result = groovyCCPG.get(misCode, 1L)

        then:
        1 * entityMetadataService.get(*_) >> entityMetadata
        1 * dmiDataService.singleKey(*_) >> data
        result.appId == 1L
        result.firstname == "name"
        result.depTotalIncome == 9999
        result.birthdate == LocalDate.of(2000, 1, 1)
        result.email == "email@email.com"
        result.ackFinAid == true
        result.yearCode == 1L
        result.tstmpCreate == LocalDateTime.of(2018, 1, 1, 10, 30)
        result.tstmpUpdate == LocalDateTime.of(2018, 1, 1, 0, 0)
        result.determinedAB540Eligible == true
        result.determinedHomeless == false
        result.determinedNonResExempt == null
    }

    def "get - not found"() {
        when:
        groovyCCPG.get(misCode, 1L)

        then:
        1 * entityMetadataService.get(*_) >> entityMetadata
        1 * dmiDataService.singleKey(*_) >> null
        thrown EntityNotFoundException
    }

    def "post"() {
        setup:
        CCPromiseGrant app = new CCPromiseGrant(
                appId: 1L,
                collegeId: misCode,
                cccId: "ABC123",
                firstname: "name",
                depTotalIncome: 1,
                tstmpCreate: LocalDateTime.now(ZoneOffset.UTC)
        )

        CTXData response = new CTXData(["ErrorOccurred": false, "OutId": 1L], null)

        when:
        groovyCCPG.post(misCode, app)

        then:
        // Checks for existence
        1 * dmiDataService.singleKey(*_) >> null
        // Gets metadata for mapping
        1 * entityMetadataService.get(*_) >> entityMetadata
        // Insert
        1 * dmiCTXService.execute(*_) >> response
    }

    def "post - already exists"() {
        setup:
        def app = new CCPromiseGrant(appId: 1L, collegeId: misCode, cccId: "ABC123")
        ColleagueData data = new ColleagueData("1", [ : ])

        when:
        groovyCCPG.post(misCode, app)

        then:
        // Checks for existence
        1 * dmiDataService.singleKey(*_) >> data
        thrown EntityConflictException
    }

    def "post - error"() {
        setup:
        CCPromiseGrant app = new CCPromiseGrant(appId: 1L, collegeId: misCode, cccId: "ABC123")

        CTXData rError1 = new CTXData(["ErrorOccurred": true], null)

        when:
        groovyCCPG.post(misCode, app)

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
        CCPromiseGrant app = new CCPromiseGrant(appId: 1L, collegeId: misCode, cccId: "ABC123")

        CTXData rError2 = new CTXData(["ErrorOccurred": true, "ErrorMsgs":["Error"]], null)

        when:
        groovyCCPG.post(misCode, app)

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
        CCPromiseGrant app = new CCPromiseGrant(appId: 1L, collegeId: misCode, cccId: "ABC123")
        CTXData response = new CTXData([:], [:])

        when:
        groovyCCPG.post(misCode, app)

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
        CCPromiseGrant app = new CCPromiseGrant(appId: 1L, collegeId: misCode, cccId: "ABC123")
        CTXData response = new CTXData(["OutId": "abc"], [:])

        when:
        groovyCCPG.post(misCode, app)

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

