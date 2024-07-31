package com.ccctc.adaptor.model.mock

import com.ccctc.adaptor.exception.EntityConflictException
import com.ccctc.adaptor.exception.EntityNotFoundException
import com.ccctc.adaptor.exception.InvalidRequestException
import com.ccctc.adaptor.model.apply.Application
import com.ccctc.adaptor.model.College
import spock.lang.Specification

class ApplyDBSpec extends Specification {
    List<Application> all
    Application first

    def misCode = "001"
    def cccId = "cccid1"

    def collegeDB = new CollegeDB()
    def applyDB = new ApplyDB(collegeDB)

    def setup() {
        // initialize databases
        collegeDB.init()
        applyDB.init()

        // seed databases
        collegeDB.add(new College(misCode: misCode, districtMisCode: "000"))
        applyDB.add(new Application(appId: 1, collegeId: misCode, cccId: cccId, termId: 1))
        applyDB.add(new Application(appId: 2, collegeId: misCode, cccId: cccId, termId: 2))

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
        applyDB.add(new Application())

        then:
        thrown InvalidRequestException

        // missing keys
        when:
        applyDB.add(new Application(appId: 10))

        then:
        thrown InvalidRequestException

        // already exists
        when:
        applyDB.add(new Application(appId: 1, collegeId: misCode))

        then:
        thrown EntityConflictException
    }

    def "update"() {
        setup:
        applyDB.add(new Application(appId: 99, collegeId: misCode, cccId: "ZZZ999", termId: 1))

        when:
        def result = applyDB.update(misCode, 99, new Application(termId: 2))

        then:
        result.appId == 99
        result.collegeId == misCode
        result.cccId == null
        result.termId == 2
    }

    def "patch"() {
        setup:
        applyDB.add(new Application(appId: 100, collegeId: misCode, cccId: "ZZZ999", termId: 1))

        when:
        def result = applyDB.patch(misCode, 100, [termId: 2])

        then:
        result.appId == 100
        result.collegeId == misCode
        result.cccId == "ZZZ999"
        result.termId == 2
    }

    def "delete"() {
        setup:
        applyDB.add(new Application(appId: 101, collegeId: misCode, cccId: "ZZZ999", termId: 1))

        when:
        def result = applyDB.delete(misCode, 101, true)

        then:
        result.appId == 101
        result.collegeId == misCode
        result.cccId == "ZZZ999"
        result.termId == 1

        when:
        result = applyDB.get(misCode, 101)

        then:
        thrown EntityNotFoundException
    }

}