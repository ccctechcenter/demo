package com.ccctc.adaptor.model.mock

import com.ccctc.adaptor.exception.EntityConflictException
import com.ccctc.adaptor.exception.EntityNotFoundException
import com.ccctc.adaptor.exception.InvalidRequestException
import com.ccctc.adaptor.model.apply.InternationalApplication
import com.ccctc.adaptor.model.College
import spock.lang.Specification

class InternationalApplicationDBSpec extends Specification {
    List<InternationalApplication> all
    InternationalApplication first

    def misCode = "001"
    def cccId = "cccid1"

    def collegeDB = new CollegeDB()
    def InternationalapplyDB = new InternationalApplicationDB(collegeDB)

    def setup() {
        // initialize databases
        collegeDB.init()
        InternationalapplyDB.init()

        // seed databases
        collegeDB.add(new College(misCode: misCode, districtMisCode: "000"))
        InternationalapplyDB.add(new InternationalApplication(appId: 1, collegeId: misCode, cccId: cccId, enrollTermCode: "1"))
        InternationalapplyDB.add(new InternationalApplication(appId: 2, collegeId: misCode, cccId: cccId, enrollTermCode: "2"))

        all = InternationalapplyDB.getAll()
        first = all[0]
    }

    def "get"() {
        when:
        def result = InternationalapplyDB.get(misCode, first.appId)

        then:
        result.appId == first.appId
    }

    def "add"() {
        // missing keys
        when:
        InternationalapplyDB.add(new InternationalApplication())

        then:
        thrown InvalidRequestException

        // missing keys
        when:
        InternationalapplyDB.add(new InternationalApplication(appId: 10))

        then:
        thrown InvalidRequestException

        // already exists
        when:
        InternationalapplyDB.add(new InternationalApplication(appId: 1, collegeId: misCode))

        then:
        thrown EntityConflictException
    }

    def "update"() {
        setup:
        InternationalapplyDB.add(new InternationalApplication(appId: 99, collegeId: misCode, cccId: "ZZZ999", enrollTermCode: "1"))

        when:
        def result = InternationalapplyDB.update(misCode, 99, new InternationalApplication(enrollTermCode: "2"))

        then:
        result.appId == 99
        result.collegeId == misCode
        result.cccId == null
        result.enrollTermCode == "2"
    }

    def "patch"() {
        setup:
        InternationalapplyDB.add(new InternationalApplication(appId: 100, collegeId: misCode, cccId: "ZZZ999", enrollTermCode: "1"))

        when:
        def result = InternationalapplyDB.patch(misCode, 100, [enrollTermCode: "2"])

        then:
        result.appId == 100
        result.collegeId == misCode
        result.cccId == "ZZZ999"
        result.enrollTermCode == "2"
    }

    def "delete"() {
        setup:
        InternationalapplyDB.add(new InternationalApplication(appId: 101, collegeId: misCode, cccId: "ZZZ999", enrollTermCode: "1"))

        when:
        def result = InternationalapplyDB.delete(misCode, 101, true)

        then:
        result.appId == 101
        result.collegeId == misCode
        result.cccId == "ZZZ999"
        result.enrollTermCode == "1"

        when:
        result = InternationalapplyDB.get(misCode, 101)

        then:
        thrown EntityNotFoundException
    }

}