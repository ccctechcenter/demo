package api.colleague

import api.colleague.InternationalApply as GroovyInternationalApply
import com.ccctc.adaptor.exception.EntityConflictException
import com.ccctc.adaptor.exception.InternalServerException
import com.ccctc.adaptor.exception.InvalidRequestException
import com.ccctc.adaptor.model.apply.InternationalApplication
import com.ccctc.adaptor.model.apply.SupplementalQuestions
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

class InternationalApplySpec extends Specification {

    EntityMetadataService entityMetadataService
    EntityMetadata entityMetadata

    def environment = Mock(Environment)
    def dmiService = Mock(DmiService)
    def dmiDataService = Mock(DmiDataService)
    def dmiCTXService = Mock(DmiCTXService)
    def cache = Mock(Cache)
    GroovyInternationalApply groovyInternationalApply = new GroovyInternationalApply()

    Long appId = 1
    String misCode = "000"
    String cccId = "ABC123"

    InternationalApplication application = new InternationalApplication(
            appId: appId,
            cccId: cccId,
            collegeId: misCode,
            addressVerified: true,
            collegeCount: 0,
            collegeEduLevel: "Mock value.",
            collegeExpelledSummary: false,
            collegeName: "Mock value."
    )


    def setup() {
        entityMetadataService = Mock(EntityMetadataService)
        ClassMap services = new ClassMap()
        services.putAll([(DmiService.class): dmiService, (DmiCTXService.class): dmiCTXService,
                         (DmiDataService.class): dmiDataService, (Cache.class): cache])


        dmiDataService.getEntityMetadataService() >> entityMetadataService
        groovyInternationalApply.colleagueInit(misCode, environment, services)

        CddEntry[] cddEntries = [
                CddEntry.builder().name("XCTC.I.APP.ID").fieldPlacement(1).build(),
                CddEntry.builder().name("XCTC.I.CCC.ID").fieldPlacement(2).build(),
                CddEntry.builder().name("XCTC.I.COLLEGE.ID").fieldPlacement(3).build(),
                CddEntry.builder().name("XCTC.I.COLLEGE.COUNT").fieldPlacement(4).build(),
                CddEntry.builder().name("XCTC.I.COLLEGE.EDU.LEVEL").fieldPlacement(5).build(),
                CddEntry.builder().name("XCTC.I.COL.EXPELLED.SUMMARY").fieldPlacement(6).build(),
                CddEntry.builder().name("XCTC.I.COLLEGE.NAME").fieldPlacement(7).build()
        ]

        entityMetadata = new EntityMetadata("PHYS", null,
                cddEntries.collectEntries { i -> [(i.name): i]},
                cddEntries as CddEntry[])
    }

    def "get"() {
        setup:
        ColleagueData data = new ColleagueData("1", [
                "XCTC.I.APP.ID"                : appId,
                "XCTC.I.CCC.ID"                : application.cccId,
                "XCTC.I.COLLEGE.ID"            : application.collegeId,
                "XCTC.I.COLLEGE.COUNT"         : application.collegeCount.toString(),
                "XCTC.I.COLLEGE.EDU.LEVEL"     : application.collegeEduLevel,
                "XCTC.I.COL.EXPELLED.SUMMARY"  : application.collegeExpelledSummary ? 'Y' : 'N',
                "XCTC.I.COLLEGE.NAME"          : application.collegeName
        ])
        ColleagueData supplementalQuestionsData = new ColleagueData("1", [
                "XCTC.I.SUPP.APP.ID"                : appId
        ])

        when:
        InternationalApplication result = groovyInternationalApply.get(misCode, appId)

        then:
        2 * entityMetadataService.get(*_) >> entityMetadata
        1 * dmiDataService.singleKey(*_) >> data
        1 * dmiDataService.singleKey(*_) >> supplementalQuestionsData
        result.appId == application.appId
        result.cccId == application.cccId
        result.collegeId == application.collegeId
        result.collegeCount == application.collegeCount
        result.collegeEduLevel == application.collegeEduLevel
        result.collegeExpelledSummary == application.collegeExpelledSummary
        result.collegeName == application.collegeName
    }

    def "post"() {
        setup:
        ColleagueData data = new ColleagueData("1", [
                "XCTC.I.APP.ID"                : appId,
                "XCTC.I.CCC.ID"                : application.cccId,
                "XCTC.I.COLLEGE.ID"            : application.collegeId,
                "XCTC.I.COLLEGE.COUNT"         : application.collegeCount.toString(),
                "XCTC.I.COLLEGE.EDU.LEVEL"     : application.collegeEduLevel,
                "XCTC.I.COL.EXPELLED.SUMMARY"  : application.collegeExpelledSummary ? 'Y' : 'N',
                "XCTC.I.COLLEGE.NAME"          : application.collegeName
        ])
        ColleagueData supplementalQuestionsData = new ColleagueData("1", [
                "XCTC.I.SUPP.APP.ID" : appId
        ])
        InternationalApplication applction = application
        applction.supplementalQuestions = new SupplementalQuestions(
                "appId": appId,
                "suppCheck01": true
        )
        CTXData postResponse = new CTXData([
                "ErrorOccurred": false,
                "ErrorCodes": null,
                "ErrorMsgs": null,
                "OutId": appId
        ], [:])

        when:
        groovyInternationalApply.post(misCode, application)

        then:
        // Checks for existence
        1 * dmiDataService.singleKey(*_) >> null
        // post application
        1 * entityMetadataService.get(*_) >> entityMetadata
        1 * dmiCTXService.execute(*_) >> postResponse
        // post supplemental questions
        1 * entityMetadataService.get(*_) >> entityMetadata
        1 * dmiCTXService.execute(*_) >> postResponse
    }

    def "post - existing app"() {
        setup:
        ColleagueData data = new ColleagueData("1", [ : ])

        when:
        groovyInternationalApply.post(misCode, application)

        then:
        // Checks for existence
        1 * dmiDataService.singleKey(*_) >> data
        thrown EntityConflictException
    }

    def "post - missing and bad params"() {
        when: groovyInternationalApply.post(null, null)
        then: thrown InvalidRequestException

        when: groovyInternationalApply.post(misCode, null)
        then: thrown InvalidRequestException

        when: groovyInternationalApply.post(misCode, new InternationalApplication())
        then: thrown InvalidRequestException

        when: groovyInternationalApply.post(misCode, new InternationalApplication(collegeId: misCode))
        then: thrown InvalidRequestException

        when: groovyInternationalApply.post(misCode, new InternationalApplication(collegeId: misCode, cccId: cccId))
        then: thrown InvalidRequestException

        when: groovyInternationalApply.post(misCode, new InternationalApplication(collegeId: misCode, appId: appId))
        then: thrown InvalidRequestException

        when: groovyInternationalApply.post(misCode, new InternationalApplication(appId: appId, collegeId: "bad-mis-code", cccId: cccId))
        then: thrown InvalidRequestException
    }

    def "post - other error"() {
        setup:
        CTXData error = new CTXData(["ErrorOccurred": true], [:])

        when:
        groovyInternationalApply.post(misCode, application)

        then:
        // Checks for existence
        1 * dmiDataService.singleKey(*_) >> null
        // Insert Application
        1 * entityMetadataService.get(*_) >> entityMetadata
        1 * dmiCTXService.execute(*_) >> error
        thrown InternalServerException
    }
}
