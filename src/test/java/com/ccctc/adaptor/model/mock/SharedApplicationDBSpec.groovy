package com.ccctc.adaptor.model.mock

import com.ccctc.adaptor.exception.EntityConflictException
import com.ccctc.adaptor.exception.EntityNotFoundException
import com.ccctc.adaptor.exception.InvalidRequestException
import com.ccctc.adaptor.model.College
import com.ccctc.adaptor.model.apply.Application
import com.ccctc.adaptor.model.apply.SharedApplication
import spock.lang.Specification

class SharedApplicationDBSpec extends Specification {
    List<Application> all
    Application first

    def misCode = "001"
    def homeMis = "002"
    def cccId = "cccid1"

    def collegeDB = new CollegeDB()
    def applyDB = new SharedApplicationDB(collegeDB)

    def setup() {
        // initialize databases
        collegeDB.init()
        applyDB.init()

        // seed databases
        collegeDB.add(new College(misCode: misCode, districtMisCode: "000"))
        applyDB.add(new SharedApplication(appId: 1, collegeId: homeMis, cccId: cccId, misCode: misCode, termId: 1))
        applyDB.add(new SharedApplication(appId: 2, collegeId: homeMis, cccId: cccId, misCode: misCode, termId: 2))

        all = applyDB.getAll()
        first = all[0]
    }

    def "get"() {
        when:
        def result = applyDB.get(misCode, first.appId)

        then:
        result.appId == first.appId
    }

    def "add"() {
        // missing keys
        when:
        applyDB.add(new SharedApplication())

        then:
        thrown InvalidRequestException

        // missing keys
        when:
        applyDB.add(new SharedApplication(appId: 10))

        then:
        thrown InvalidRequestException

        // already exists
        when:
        applyDB.add(new SharedApplication(appId: 1, misCode: misCode))

        then:
        thrown EntityConflictException
    }

    def "update"() {
        setup:
        applyDB.add(new SharedApplication(appId: 99, collegeId: homeMis, cccId: "ZZZ999", misCode: misCode, termId: 1))

        when:
        def result = applyDB.update(misCode, 99, new SharedApplication(collegeId: homeMis, termId: 2))

        then:
        result.appId == 99
        result.collegeId == homeMis
        result.misCode == misCode
        result.cccId == null
        result.termId == 2
    }

    def "patch"() {
        setup:
        applyDB.add(new SharedApplication(appId: 100, collegeId: homeMis, cccId: "ZZZ999", misCode: misCode, termId: 1))

        when:
        def result = applyDB.patch(misCode, 100, [termId: 2])

        then:
        result.appId == 100
        result.collegeId == homeMis
        result.misCode == misCode
        result.cccId == "ZZZ999"
        result.termId == 2
    }

    def "delete"() {
        setup:
        applyDB.add(new SharedApplication(appId: 101, collegeId: homeMis, cccId: "ZZZ999", misCode: misCode, termId: 1))

        when:
        def result = applyDB.delete(misCode, 101, true)

        then:
        result.appId == 101
        result.collegeId == homeMis
        result.misCode == misCode
        result.cccId == "ZZZ999"
        result.termId == 1

        when:
        result = applyDB.get(misCode, 101)

        then:
        thrown EntityNotFoundException
    }

}