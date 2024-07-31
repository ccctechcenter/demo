package com.ccctc.adaptor.model.mock

import com.ccctc.adaptor.exception.InvalidRequestException
import com.ccctc.adaptor.model.BOGWaiver
import com.ccctc.adaptor.model.College
import com.ccctc.adaptor.model.MaritalStatus
import com.ccctc.adaptor.model.Person
import com.ccctc.adaptor.model.Student
import com.ccctc.adaptor.model.Term
import spock.lang.Specification

/**
 * Created by zekeo on 7/20/2017.
 */
class BogWaiverDBSpec extends Specification  {
    List<BOGWaiver> all
    BOGWaiver first

    def misCode = "001"
    def sisTermId = "TERM"
    def cccId = "cccid1"
    def sisPersonId = "person1"

    def collegeDB = new CollegeDB()
    def termDB = new TermDB(collegeDB)
    def personDB = new PersonDB(collegeDB)
    def studentDB = new StudentDB(collegeDB, termDB, personDB)
    def bogWaiverDB = new BOGWaiverDB(collegeDB, termDB, studentDB, personDB)

    def setup() {
        // initialize databases
        collegeDB.init()
        termDB.init()
        personDB.init()
        studentDB.init()
        bogWaiverDB.init()

        // seed databases
        collegeDB.add(new College(misCode: misCode, districtMisCode: "000"))
        termDB.add(new Term(misCode: misCode, sisTermId: sisTermId, start: new Date(2000, 1, 1), end: new Date(2000, 6, 30)))
        personDB.add(new Person(misCode: misCode, sisPersonId: sisPersonId, cccid: cccId))
        studentDB.add(new Student(misCode: misCode, sisPersonId: sisPersonId, sisTermId: sisTermId))

        bogWaiverDB.add(new BOGWaiver(misCode: misCode, sisTermId: sisTermId, sisPersonId: sisPersonId))

        all = bogWaiverDB.getAll()
        first = all[0]
    }

    def "get"() {
        setup:
        def result

        // by sisPersonId
        when:
        result = bogWaiverDB.get(first.misCode, first.sisTermId, first.sisPersonId)

        then:
        result.toString() == first.toString()
    }

    def "add"() {
        setup:
        bogWaiverDB.deleteAll(true)
        def bogw = new BOGWaiver()
        def result

        // missing mis code
        when:
        bogWaiverDB.add(bogw)

        then:
        thrown InvalidRequestException

        // missing student
        when:
        bogw.misCode = first.misCode
        bogWaiverDB.add(bogw)

        then:
        thrown InvalidRequestException

        // missing term
        when:
        bogw.cccid = "testcccid"
        bogWaiverDB.add(bogw)

        then:
        thrown InvalidRequestException

        // invalid person
        when:
        bogw.sisTermId = first.sisTermId
        bogWaiverDB.add(bogw)

        then:
        thrown InvalidRequestException

        // invalid student
        when:
        personDB.add(new Person(misCode: first.misCode, sisPersonId: "test", cccid: "testcccid"))
        bogWaiverDB.add(bogw)

        then:
        thrown InvalidRequestException

        // success
        when:
        studentDB.add(new Student(misCode: first.misCode, sisTermId: first.sisTermId, sisPersonId: "test", cccid: "testcccid"))
        result = bogWaiverDB.add(bogw)

        then:
        result.toString() == bogw.toString()
    }

    def "update"() {
        setup:
        def copy = bogWaiverDB.deepCopy(first)
        def result

        when:
        copy.maritalStatus = MaritalStatus.DIVORCED
        result = bogWaiverDB.update(first.misCode, first.sisTermId, first.sisPersonId, copy)

        then:
        result.toString() == copy.toString()
    }

    def "patch"() {
        when:
        def r = bogWaiverDB.patch(first.misCode, first.sisTermId, first.sisPersonId, [maritalStatus: MaritalStatus.SEPARATED])

        then:
        r.maritalStatus == MaritalStatus.SEPARATED
    }

    def "delete"() {
        when:
        def result = bogWaiverDB.delete(first.misCode, first.sisTermId, first.sisPersonId, true)

        then:
        result.toString() == first.toString()
    }

    def "cascadeUpdateFromPerson and find"() {
        when:
        personDB.patch(misCode, sisPersonId, [cccid: "newcccid"])
        def result = bogWaiverDB.find([sisPersonId: sisPersonId])

        then:
        result.size() == 1
        result[0].cccid == "newcccid"
    }

    def "cascadeDelete"() {
        when:
        personDB.delete(misCode, sisPersonId, true)
        bogWaiverDB.get(first.misCode, first.sisTermId, first.sisPersonId)

        then:
        thrown InvalidRequestException // person not found
    }
}
