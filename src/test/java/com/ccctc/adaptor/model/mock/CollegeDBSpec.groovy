package com.ccctc.adaptor.model.mock

import com.ccctc.adaptor.exception.EntityNotFoundException
import com.ccctc.adaptor.exception.InvalidRequestException
import com.ccctc.adaptor.model.College
import spock.lang.Specification

/**
 * Created by zekeo on 7/20/2017.
 */
class CollegeDBSpec extends Specification  {

    List<College> all
    College first

    def misCode = "001"

    def collegeDB = new CollegeDB()

    def setup() {
        // initialize databases
        collegeDB.init()

        // seed databases
        collegeDB.add(new College(misCode: misCode, districtMisCode: "000"))

        all = collegeDB.getAll()
        first = all[0]
    }

    def "get"() {
        when:
        def r = collegeDB.get(first.misCode)

        then:
        r.toString() == first.toString()
    }

    def "add"() {
        setup:
        def college = new College()

        // missing mis code
        when:
        collegeDB.add(college)

        then:
        thrown InvalidRequestException

        // bad mis code
        when:
        college.misCode = "nope"
        collegeDB.add(college)

        then:
        thrown InvalidRequestException

        // missing district
        when:
        college.misCode = "999"
        collegeDB.add(college)

        then:
        thrown InvalidRequestException

        // success
        when:
        college.districtMisCode = "000"
        def result = collegeDB.add(college)

        then:
        result.toString() == college.toString()
    }

    def "update"() {
        setup:
        def copy = collegeDB.deepCopy(first)

        when:
        copy.districtMisCode = "nope"
        collegeDB.update(first.misCode, copy)

        then:
        thrown InvalidRequestException

        when:
        copy.districtMisCode = "000"
        copy.name = "updated"
        def result = collegeDB.update(first.misCode, copy)

        then:
        result.toString() == copy.toString()
    }

    def "patch"() {
        when:
        def r = collegeDB.patch(first.misCode, [name: "patched"])

        then:
        r.name == "patched"
    }

    def "delete"() {
        when:
        def a = collegeDB.add(new College(misCode: "999", districtMisCode: "000", name: "test"))
        def b = collegeDB.get("999")
        def c = collegeDB.delete("999", true)

        then:
        a.toString() == b.toString()
        b.toString() == c.toString()

        when:
        collegeDB.get("999")

        then:
        thrown EntityNotFoundException
    }

    def "validate"() {
        when:
        collegeDB.validate("444")

        then:
        thrown InvalidRequestException

        when:
        def result = collegeDB.validate(misCode)

        then:
        result.misCode == misCode

    }
}
