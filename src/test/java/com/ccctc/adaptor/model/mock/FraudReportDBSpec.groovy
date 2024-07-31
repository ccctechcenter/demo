package com.ccctc.adaptor.model.mock

import com.ccctc.adaptor.exception.EntityNotFoundException
import com.ccctc.adaptor.model.College
import com.ccctc.adaptor.model.fraud.FraudReport
import spock.lang.Specification

class FraudReportDBSpec extends Specification {
    List<FraudReport> all
    FraudReport first

    def reportToMisCode = "001"
    def reportToMisCode2 = "002"
    def reportFromMisCode = "ZZ1"
    def cccId = "cccid1"
    def cccId2 = "cccid2"

    def collegeDB = new CollegeDB()
    def fraudDB = new FraudReportDB(collegeDB)

    def setup() {
        // initialize databases
        collegeDB.init()
        fraudDB.init()

        // seed databases
        collegeDB.add(new College(misCode: reportToMisCode, districtMisCode: "000"))
        collegeDB.add(new College(misCode: reportToMisCode2, districtMisCode: "000"))
        collegeDB.add(new College(misCode: reportFromMisCode, districtMisCode: "ZZ0"))

        fraudDB.add(new FraudReport(sisFraudReportId: fraudDB.getMaxId() + 3, appId: 1, misCode: reportToMisCode, cccId: cccId, fraudType: "Application", reportedByMisCode: reportFromMisCode))
        fraudDB.add(new FraudReport(sisFraudReportId: fraudDB.getMaxId() + 3, appId: 2, misCode: reportToMisCode, cccId: cccId, fraudType: "Application", reportedByMisCode: reportFromMisCode))

        fraudDB.add(new FraudReport(sisFraudReportId: fraudDB.getMaxId() + 3, appId: 1, misCode: reportToMisCode2, cccId: cccId, fraudType: "Application", reportedByMisCode: reportFromMisCode))
        fraudDB.add(new FraudReport(sisFraudReportId: fraudDB.getMaxId() + 3, appId: 0, misCode: reportToMisCode2, cccId: cccId, fraudType: "Enrollment", reportedByMisCode: reportFromMisCode))

        fraudDB.add(new FraudReport(sisFraudReportId: fraudDB.getMaxId() + 3, appId: 4, misCode: reportToMisCode, cccId: cccId2, fraudType: "Application", reportedByMisCode: reportFromMisCode))

        all = fraudDB.getAll()
        first = all[0]
    }

    def "getById"() {
        when:
        def result = fraudDB.getById(reportToMisCode, first.sisFraudReportId)

        then:
        result.sisFraudReportId == first.sisFraudReportId
    }

    def "getMatching - mis1 - by cccid - multiple apps"() {
        // missing keys
        when:
        def results = fraudDB.getMatching(reportToMisCode, 0, cccId)

        then:
        results.size() == 2
        results[0].cccId == cccId
        results[1].cccId == cccId
    }

    def "getMatching - mis1 - by cccid - one app"() {
        // missing keys
        when:
        def results = fraudDB.getMatching(reportToMisCode, 0, cccId2)

        then:
        results.size() == 1
        results[0].cccId == cccId2
    }

    def "getMatching - mis1 - by appId"() {
        // missing keys
        when:
        def results = fraudDB.getMatching(reportToMisCode, 1, null)

        then:
        results.size() == 1
        results[0].appId == 1
    }

    def "getMatching - mis1 - by appId and cccId - found"() {
        // missing keys
        when:
        def results = fraudDB.getMatching(reportToMisCode, 1, cccId)

        then:
        results.size() == 1
        results[0].appId == 1
        results[0].cccId == cccId
    }

    def "getMatching - mis1 - by appId and cccId - not found"() {
        // missing keys
        when:
        def results = fraudDB.getMatching(reportToMisCode, 1, cccId2)

        then:
        results.size() == 0
    }

    def "getMatching - mis1"() {
        // missing keys
        when:
        def results = fraudDB.getMatching(reportToMisCode, 0, null)

        then:
        results.size() == 3
    }

    def "deleteById"() {
        setup:
        def newReport = fraudDB.add(new FraudReport(sisFraudReportId: fraudDB.getMaxId() + 3, appId: 101, misCode: reportToMisCode, cccId: "ZZZ999", reportedByMisCode: reportFromMisCode))

        when:
        def result = fraudDB.deleteById(reportToMisCode, newReport.sisFraudReportId)

        then:
        result.sisFraudReportId == newReport.sisFraudReportId
        result.appId == 101
        result.misCode == reportToMisCode
        result.cccId == "ZZZ999"

        when:
        fraudDB.getById(reportToMisCode, newReport.sisFraudReportId)

        then:
        thrown EntityNotFoundException
    }

}