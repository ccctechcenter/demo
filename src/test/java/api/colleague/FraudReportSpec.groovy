package api.colleague

import api.colleague.FraudReport as GroovyFraudReport
import com.ccctc.adaptor.exception.EntityConflictException
import com.ccctc.adaptor.exception.EntityNotFoundException
import com.ccctc.adaptor.exception.InternalServerException
import com.ccctc.adaptor.exception.InvalidRequestException
import com.ccctc.adaptor.model.fraud.FraudReport as FraudReportModel
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

class FraudReportSpec extends Specification {

    String misCode = "000"
    GroovyFraudReport groovyFraudReport
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

        groovyFraudReport = new GroovyFraudReport()
        groovyFraudReport.colleagueInit(misCode, null, services)

        ArrayList<CddEntry> cddEntries = [
                CddEntry.builder().name("XCTC.FR.COLLEGE.ID").fieldPlacement(1).build(),
                CddEntry.builder().name("XCTC.FR.APPL.ID").fieldPlacement(2).build(),
                CddEntry.builder().name("XCTC.FR.CCC.ID").fieldPlacement(3).build(),
                CddEntry.builder().name("XCTC.FR.FRAUD.TYPE").fieldPlacement(4).build(),
                CddEntry.builder().name("XCTC.FR.REPORTED.BY.MIS.CODE").fieldPlacement(9).build(),
                CddEntry.builder().name("XCTC.FR.TSTMP.SUBMIT.DATE").fieldPlacement(10).build(),
                CddEntry.builder().name("XCTC.FR.TSTMP.SUBMIT.TIME").fieldPlacement(11).build(),
                CddEntry.builder().name("XCTC.FR.TSTMP.PROCESSED.DATE").fieldPlacement(12).build(),
                CddEntry.builder().name("XCTC.FR.TSTMP.PROCESSED.TIME").fieldPlacement(13).build(),
                CddEntry.builder().name("XCTC.FR.USER1").fieldPlacement(14).build(),
                CddEntry.builder().name("XCTC.FR.USER2").fieldPlacement(15).build(),
                CddEntry.builder().name("XCTC.FR.USER3").fieldPlacement(16).build(),
                CddEntry.builder().name("XCTC.FR.TSTMP.RESCIND.DATE").fieldPlacement(17).build(),
                CddEntry.builder().name("XCTC.FR.TSTMP.RESCIND.TIME").fieldPlacement(18).build()
        ]

        entityMetadata = new EntityMetadata("PHYS", null,
                cddEntries.collectEntries { i -> [(i.name): i]},
                cddEntries as CddEntry[])

    }

    def "getById"() {
        setup:
        ColleagueData data = new ColleagueData("1", [
                "XCTC.FR.COLLEGE.ID": misCode,
                "XCTC.FR.APPL.ID": 10,
                "XCTC.FR.CCC.ID": "ABKT1234",
                "XCTC.FR.FRAUD.TYPE": "Application",
                "XCTC.FR.REPORTED.BY.MIS.CODE" : "ZZ1",
                "XCTC.FR.TSTMP.SUBMIT.DATE" : LocalDate.of(2018, 1, 1),
                "XCTC.FR.TSTMP.SUBMIT.TIME" : LocalTime.of(10, 30),
                "XCTC.FR.TSTMP.PROCESSED.DATE" : null,
                "XCTC.FR.TSTMP.PROCESSED.TIME" : null,
                "XCTC.FR.USER1" : null,
                "XCTC.FR.USER2" : null,
                "XCTC.FR.USER3" : null
        ])

        when:
        FraudReportModel result = groovyFraudReport.getById(misCode, 1L)

        then:
        1 * entityMetadataService.get(*_) >> entityMetadata
        1 * dmiDataService.singleKey(*_) >> data
        result.sisFraudReportId == 1L
        result.misCode == misCode
        result.appId == 10
        result.cccId == "ABKT1234"
        result.fraudType == "Application"
        result.reportedByMisCode == "ZZ1"
        result.tstmpSubmit == LocalDateTime.of(2018, 1, 1, 10, 30)
    }

    def "getById - missing misCode"() {
        when:
        groovyFraudReport.getById(null, 1)

        then:
        thrown InvalidRequestException
    }

    def "getById - not found"() {
        setup:
        ColleagueData data = null;

        when:
        groovyFraudReport.getById(misCode, 999)

        then:
        1 * entityMetadataService.get(*_) >> entityMetadata
        1 * dmiDataService.singleKey(*_) >> data
        thrown EntityNotFoundException
    }

    def "getById - rescinded not found"() {
        setup:
        ColleagueData data = new ColleagueData("1", [
                "XCTC.FR.COLLEGE.ID": misCode,
                "XCTC.FR.APPL.ID": 10,
                "XCTC.FR.CCC.ID": "ABKT1234",
                "XCTC.FR.FRAUD.TYPE": "Application",
                "XCTC.FR.REPORTED.BY.MIS.CODE" : "ZZ1",
                "XCTC.FR.TSTMP.SUBMIT.DATE" : LocalDate.of(2018, 1, 1),
                "XCTC.FR.TSTMP.SUBMIT.TIME" : LocalTime.of(10, 30),
                "XCTC.FR.TSTMP.PROCESSED.DATE" : null,
                "XCTC.FR.TSTMP.PROCESSED.TIME" : null,
                "XCTC.FR.TSTMP.RESCIND.DATE" : LocalDate.of(2018, 2, 1),
                "XCTC.FR.TSTMP.RESCIND.TIME" : LocalTime.of(10, 30),
                "XCTC.FR.USER1" : null,
                "XCTC.FR.USER2" : null,
                "XCTC.FR.USER3" : null
        ])

        when:
        FraudReportModel result = groovyFraudReport.getById(misCode, 1L)

        then:
        1 * entityMetadataService.get(*_) >> entityMetadata
        1 * dmiDataService.singleKey(*_) >> data
        thrown EntityNotFoundException
    }

    def "deleteById"() {
        setup:
        CTXData response = new CTXData(["ErrorOccurred": false], null)

        when:
        groovyFraudReport.deleteById("000", 1)

        then:
        1 * dmiCTXService.execute(*_) >> response
    }

    def "deleteById - missing misCode"() {
        when:
        groovyFraudReport.deleteById(null, 1)

        then:
        thrown InvalidRequestException
    }

    def "deleteById - not found"() {
        setup:
        CTXData response = new CTXData(["ErrorOccurred": true, "ErrorMsgs": [ "not found"] ], null)

        when:
        groovyFraudReport.deleteById(misCode, 999)

        then:
        1 * dmiCTXService.execute(*_) >> response
        thrown InternalServerException
    }

    def "deleteFraudReport"() {
        setup:
        FraudReportModel deleteThisModel = new FraudReportModel(misCode: misCode, appId: 1, cccId: "AGB123", fraudType: "Application", reportedByMisCode: "002")
        List<ColleagueData> data = [
                new ColleagueData("1", [
                        "XCTC.FR.COLLEGE.ID": misCode,
                        "XCTC.FR.APPL.ID": 1,
                        "XCTC.FR.CCC.ID": "AGB123",
                        "XCTC.FR.FRAUD.TYPE": "Application",
                        "XCTC.FR.REPORTED.BY.MIS.CODE" : "002",
                        "XCTC.FR.TSTMP.SUBMIT.DATE" : LocalDate.of(2018, 1, 1),
                        "XCTC.FR.TSTMP.SUBMIT.TIME" : LocalTime.of(10, 30),
                        "XCTC.FR.TSTMP.PROCESSED.DATE" : null,
                        "XCTC.FR.TSTMP.PROCESSED.TIME" : null,
                        "XCTC.FR.USER1" : null,
                        "XCTC.FR.USER2" : null,
                        "XCTC.FR.USER3" : null
                ])
        ]
        CTXData deleteResponse = new CTXData(["ErrorOccurred": false], null)

        CTXData createResponse = new CTXData(["ErrorOccurred": false, "OutId": 4L], null)

        when:
        groovyFraudReport.deleteFraudReport(deleteThisModel)


        then:
        // Get matching raw
        1 * entityMetadataService.get(*_) >> entityMetadata
        1 * dmiDataService.batchSelect(*_) >> data

        // Gets metadata for mapping
        1 * entityMetadataService.get(*_) >> entityMetadata

        // Delete By Id
        1 * dmiCTXService.execute(*_) >> deleteResponse

        // Write new record
        1 * dmiCTXService.execute(*_) >> createResponse
    }


    def "getMatching"() {
        setup:
        List<ColleagueData> data = [
                new ColleagueData("1", [
                    "XCTC.FR.COLLEGE.ID": misCode,
                    "XCTC.FR.APPL.ID": 10,
                    "XCTC.FR.CCC.ID": "ABKT1234",
                    "XCTC.FR.FRAUD.TYPE": "Application",
                    "XCTC.FR.REPORTED.BY.MIS.CODE" : "ZZ1",
                    "XCTC.FR.TSTMP.SUBMIT.DATE" : LocalDate.of(2018, 1, 1),
                    "XCTC.FR.TSTMP.SUBMIT.TIME" : LocalTime.of(10, 30),
                    "XCTC.FR.TSTMP.PROCESSED.DATE" : null,
                    "XCTC.FR.TSTMP.PROCESSED.TIME" : null,
                    "XCTC.FR.USER1" : null,
                    "XCTC.FR.USER2" : null,
                    "XCTC.FR.USER3" : null
            ])
        ]
        when:
        List<FraudReportModel> results = groovyFraudReport.getMatching("000", 10, "ABKT1234")

        then:
        1 * entityMetadataService.get(*_) >> entityMetadata
        1 * dmiDataService.batchSelect(*_) >> data
        results != null
        results.size() == 1
    }

    def "getMatching - missing misCode"() {
        when:
        groovyFraudReport.getMatching(null, 0, "ABG123")

        then:
        thrown InvalidRequestException
    }

    def "getMatching - missing search terms"() {
        when:
        groovyFraudReport.getMatching("000", 0l, "")

        then:
        thrown InvalidRequestException
    }

    def "create"() {
        setup:
        FraudReportModel newModel = new FraudReportModel(misCode: "001", appId: 1, cccId: "AGB123", fraudType: "Application", reportedByMisCode: "002")

        CTXData response = new CTXData(["ErrorOccurred": false, "OutId": 4L], null)
        List<CTXData> noneExisting = []

        when:
        Long newId = groovyFraudReport.create("000", newModel)

        then:
        // Gets metadata for existence check
        1 * entityMetadataService.get(*_) >> entityMetadata

        // check for existence
        1 * dmiDataService.batchSelect(*_) >> noneExisting

        // Gets metadata for mapping
        1 * entityMetadataService.get(*_) >> entityMetadata
        // Insert
        1 * dmiCTXService.execute(*_) >> response
        newId == 4l
    }

    def "create - missing params"() {
        when:
        groovyFraudReport.create(null, null)

        then:
        thrown InvalidRequestException
    }

    def "create - missing misCode"() {
        setup:
        FraudReportModel newModel = new FraudReportModel(misCode: "001", appId: 1, cccId: "AGB123", reportedByMisCode: "002")

        when:
        groovyFraudReport.create(null, newModel)

        then:
        thrown InvalidRequestException
    }

    def "create - missing model"() {
        setup:
        FraudReportModel newModel = null

        when:
        groovyFraudReport.create("000", newModel)

        then:
        thrown InvalidRequestException
    }

    def "create - missing appId"() {
        setup:
        FraudReportModel newModel = new FraudReportModel(misCode: "001", cccId: "AGB123", reportedByMisCode: "002")

        when:
        groovyFraudReport.create(null, newModel)

        then:
        thrown InvalidRequestException
    }

    def "create - error"() {
        setup:
        FraudReportModel newModel = new FraudReportModel(appId: 1L, misCode: misCode, cccId: "ABC123", fraudType: "Application")

        CTXData rError1 = new CTXData(["ErrorOccurred": true], null)
        List<CTXData> noneExisting = []

        when:
        groovyFraudReport.create(misCode, newModel)

        then:
        // Gets metadata for existence check
        1 * entityMetadataService.get(*_) >> entityMetadata

        // check for existence
        1 * dmiDataService.batchSelect(*_) >> noneExisting

        // Gets metadata for mapping
        1 * entityMetadataService.get(*_) >> entityMetadata
        // Insert
        1 * dmiCTXService.execute(*_) >> rError1
        thrown InternalServerException
    }

    def "create - already exists"() {
        setup:
        ColleagueData data = new ColleagueData("1", [
                "XCTC.FR.COLLEGE.ID": misCode,
                "XCTC.FR.APPL.ID": 10,
                "XCTC.FR.CCC.ID": "ABKT1234",
                "XCTC.FR.FRAUD.TYPE": "Application",
                "XCTC.FR.REPORTED.BY.MIS.CODE" : "ZZ1",
                "XCTC.FR.TSTMP.SUBMIT.DATE" : LocalDate.of(2018, 1, 1),
                "XCTC.FR.TSTMP.SUBMIT.TIME" : LocalTime.of(10, 30),
                "XCTC.FR.TSTMP.PROCESSED.DATE" : null,
                "XCTC.FR.TSTMP.PROCESSED.TIME" : null,
                "XCTC.FR.USER1" : null,
                "XCTC.FR.USER2" : null,
                "XCTC.FR.USER3" : null
        ])
        FraudReportModel newModel = new FraudReportModel(appId: 1L, misCode: misCode, cccId: "ABC123", fraudType: "Application")

        CTXData rError1 = new CTXData(["ErrorOccurred": true], null)
        List<ColleagueData> oneExisting = [
            data
        ]

        when:
        groovyFraudReport.create(misCode, newModel)

        then:
        // Gets metadata for existence check
        1 * entityMetadataService.get(*_) >> entityMetadata

        // check for existence
        1 * dmiDataService.batchSelect(*_) >> oneExisting

        thrown EntityConflictException
    }

    def "create - no out id"() {
        setup:
        FraudReportModel newModel = new FraudReportModel(appId: 1L, misCode: misCode, cccId: "ABC123", fraudType: "Application")
        CTXData response = new CTXData([:], [:])
        List<CTXData> noneExisting = []

        when:
        groovyFraudReport.create(misCode, newModel)

        then:
        // Gets metadata for existence check
        1 * entityMetadataService.get(*_) >> entityMetadata

        // check for existence
        1 * dmiDataService.batchSelect(*_) >> noneExisting

        // Gets metadata for mapping
        1 * entityMetadataService.get(*_) >> entityMetadata

        // Insert
        1 * dmiCTXService.execute(*_) >> response
        thrown InternalServerException
    }

    def "create - invalid out id"() {
        setup:
        FraudReportModel model = new FraudReportModel(appId: 1L, misCode: misCode, cccId: "ABC123", fraudType: "Application")
        CTXData response = new CTXData(["OutId": "abc"], [:])
        List<CTXData> noneExisting = []

        when:
        groovyFraudReport.create(misCode, model)

        then:
        // Gets metadata for existence check
        1 * entityMetadataService.get(*_) >> entityMetadata

        // check for existence
        1 * dmiDataService.batchSelect(*_) >> noneExisting

        // Gets metadata for mapping
        1 * entityMetadataService.get(*_) >> entityMetadata
        // Insert
        1 * dmiCTXService.execute(*_) >> response
        thrown InternalServerException
    }
}

