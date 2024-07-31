package com.ccctc.adaptor.model.mock

import com.ccctc.adaptor.exception.EntityConflictException
import com.ccctc.adaptor.exception.EntityNotFoundException
import com.ccctc.adaptor.exception.InvalidRequestException
import com.ccctc.adaptor.model.College
import com.ccctc.adaptor.model.apply.CCPromiseGrant
import spock.lang.Specification

class CCPromiseGrantDBSpec extends Specification {
    List<CCPromiseGrant> all
    CCPromiseGrant first

    def misCode = "001"
    def cccId = "cccid1"

    def collegeDB = new CollegeDB()
    def ccPromiseGrantDB = new CCPromiseGrantDB(collegeDB)

    def setup() {
        // initialize databases
        collegeDB.init()
        ccPromiseGrantDB.init()

        // seed databases
        collegeDB.add(new College(misCode: misCode, districtMisCode: "000"))
        ccPromiseGrantDB.add(new CCPromiseGrant(appId: 1, confirmationNumber: "BOG-1", collegeId: misCode, cccId: cccId, yearCode: 1))
        ccPromiseGrantDB.add(new CCPromiseGrant(appId: 2, confirmationNumber: "BOG-2", collegeId: misCode, cccId: cccId, yearCode: 2))

        all = ccPromiseGrantDB.getAll()
        first = all[0]
    }

    def "get"() {
        when:
        def result = ccPromiseGrantDB.get(misCode, first.appId)

        then:
        result.appId == first.appId
    }

    def "add"() {
        // missing keys
        when:
        ccPromiseGrantDB.add(new CCPromiseGrant())

        then:
        thrown InvalidRequestException

        // missing keys
        when:
        ccPromiseGrantDB.add(new CCPromiseGrant(appId: 10))

        then:
        thrown InvalidRequestException

        // already exists
        when:
        ccPromiseGrantDB.add(new CCPromiseGrant(appId: 1, confirmationNumber: "BOG-1", collegeId: misCode))

        then:
        thrown EntityConflictException
    }

    def "update"() {
        setup:
        ccPromiseGrantDB.add(new CCPromiseGrant(appId: 99, confirmationNumber: "BOG-99", collegeId: misCode, cccId: "ZZZ999", yearCode: 1))

        when:
        def result = ccPromiseGrantDB.update(misCode, 99, new CCPromiseGrant(yearCode: 2))

        then:
        result.appId == 99
        result.collegeId == misCode
        result.cccId == null
        result.yearCode == 2
    }

    def "patch"() {
        setup:
        ccPromiseGrantDB.add(new CCPromiseGrant(appId: 100, collegeId: misCode, cccId: "ZZZ999", yearCode: 1))

        when:
        def result = ccPromiseGrantDB.patch(misCode, 100, [yearCode: 2])

        then:
        result.appId == 100
        result.collegeId == misCode
        result.cccId == "ZZZ999"
        result.yearCode == 2
    }

    def "delete"() {
        setup:
        ccPromiseGrantDB.add(new CCPromiseGrant(appId: 101, collegeId: misCode, cccId: "ZZZ999", yearCode: 1))

        when:
        def result = ccPromiseGrantDB.delete(misCode, 101, true)

        then:
        result.appId == 101
        result.collegeId == misCode
        result.cccId == "ZZZ999"
        result.yearCode == 1

        when:
        result = ccPromiseGrantDB.get(misCode, 101)

        then:
        thrown EntityNotFoundException
    }

}