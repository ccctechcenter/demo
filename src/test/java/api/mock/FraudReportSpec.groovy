package api.mock

import api.mock.FraudReport as GroovyFraudReport
import com.ccctc.adaptor.exception.InvalidRequestException
import com.ccctc.adaptor.model.fraud.FraudReport as modelFraudReport
import com.ccctc.adaptor.model.mock.FraudReportDB
import spock.lang.Specification

class FraudReportSpec extends Specification {

    def mockFraudReportDB = Mock(FraudReportDB)
    def groovyFraudReport = new GroovyFraudReport(fraudReportDB: mockFraudReportDB)

    def "getById"() {
        when:
        groovyFraudReport.getById("000", 1)

        then:
        1 * mockFraudReportDB.getById("000", 1)
    }

    def "getById - missing misCode"() {
        when:
        groovyFraudReport.getById(null, 1)

        then:
        thrown InvalidRequestException
    }

    def "deleteById"() {
        when:
        groovyFraudReport.deleteById("000", 1)

        then:
        1 * mockFraudReportDB.deleteById("000", 1)
    }

    def "deleteById - missing misCode"() {
        when:
        groovyFraudReport.deleteById(null, 1)

        then:
        thrown InvalidRequestException
    }

    def "deleteFraudReport"() {
        setup:
        com.ccctc.adaptor.model.fraud.FraudReport report = new com.ccctc.adaptor.model.fraud.FraudReport();
        report.setAppId(1)
        report.setReportedByMisCode("001")
        report.setMisCode("000")
        when:

        groovyFraudReport.deleteFraudReport(report)

        then:
        1 * mockFraudReportDB.deleteFraudReport(*_)
    }

    def "deleteFraudReport - no appId"() {
        setup:
        com.ccctc.adaptor.model.fraud.FraudReport report = new com.ccctc.adaptor.model.fraud.FraudReport();
        //report.setAppId(1)
        report.setReportedByMisCode("001")
        report.setMisCode("000")
        when:

        groovyFraudReport.deleteFraudReport(report)

        then:
        thrown InvalidRequestException
    }


    def "deleteFraudReport - no reportedByMisCode"() {
        setup:
        com.ccctc.adaptor.model.fraud.FraudReport report = new com.ccctc.adaptor.model.fraud.FraudReport();
        report.setAppId(1)
        //report.setReportedByMisCode("001")
        report.setMisCode("000")
        when:

        groovyFraudReport.deleteFraudReport(report)

        then:
        thrown InvalidRequestException
    }

    def "deleteFraudReport - no misCode"() {
        setup:
        com.ccctc.adaptor.model.fraud.FraudReport report = new com.ccctc.adaptor.model.fraud.FraudReport();
        report.setAppId(1)
        report.setReportedByMisCode("001")
        //report.setMisCode("000")
        when:

        groovyFraudReport.deleteFraudReport(report)

        then:
        thrown InvalidRequestException
    }

    def "getMatching"() {
        when:
        groovyFraudReport.getMatching("000", 1, "ABG123")

        then:
        1 * mockFraudReportDB.getMatching("000", 1, "ABG123")
    }

    def "getMatching no CCCID"() {
        when:
        groovyFraudReport.getMatching("000", 1, "")

        then:
        1 * mockFraudReportDB.getMatching("000", 1, "")
    }

    def "getMatching no appId"() {
        when:
        groovyFraudReport.getMatching("000", 0l, "ABG123")

        then:
        1 * mockFraudReportDB.getMatching("000", 0l, "ABG123")
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
        def curMaxId = 400
        def newModel = new modelFraudReport(misCode: "001", appId: 1, cccId: "AGB123", reportedByMisCode: "002")

        when:
        groovyFraudReport.create("000", newModel)

        then:
        1 * mockFraudReportDB.getMaxId() >> curMaxId
        1 * mockFraudReportDB.add(_) >> newModel
        newModel.getSisFraudReportId() == (curMaxId + 3)
    }

    def "create - 0l sent for fraudReportId"() {
        setup:
        def curMaxId = 400
        def newModel = new modelFraudReport(misCode: "001", appId: 1, cccId: "AGB123", reportedByMisCode: "002", sisFraudReportId: 0l)

        when:
        groovyFraudReport.create("000", newModel)

        then:
        1 * mockFraudReportDB.getMaxId() >> curMaxId
        1 * mockFraudReportDB.add(_) >> newModel
    }

    def "create - 10 sent for fraudReportId"() {
        setup:
        def curMaxId = 400
        def newModel = new modelFraudReport(misCode: "001", appId: 1, cccId: "AGB123", reportedByMisCode: "002", sisFraudReportId: 10)

        when:
        groovyFraudReport.create("000", newModel)

        then:
        1 * mockFraudReportDB.add(_) >> newModel
        newModel.getSisFraudReportId() == 10
    }

    def "create - missing params"() {
        when:
        groovyFraudReport.create(null, null)

        then:
        thrown InvalidRequestException
    }

    def "create - missing misCode"() {
        setup:
        def curMaxId = 400
        def newModel = new modelFraudReport(misCode: "001", appId: 1, cccId: "AGB123", reportedByMisCode: "002")

        when:
        groovyFraudReport.create(null, newModel)

        then:
        thrown InvalidRequestException
    }

    def "create - missing model"() {
        setup:
        def curMaxId = 400
        def newModel = null

        when:
        groovyFraudReport.create("000", newModel)

        then:
        thrown InvalidRequestException
    }


}
