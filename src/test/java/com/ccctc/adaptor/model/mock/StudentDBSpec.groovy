package com.ccctc.adaptor.model.mock

import com.ccctc.adaptor.exception.EntityConflictException
import com.ccctc.adaptor.exception.EntityNotFoundException
import com.ccctc.adaptor.exception.InvalidRequestException
import com.ccctc.adaptor.model.ApplicationStatus
import com.ccctc.adaptor.model.Cohort
import com.ccctc.adaptor.model.CohortTypeEnum
import com.ccctc.adaptor.model.College
import com.ccctc.adaptor.model.Person
import com.ccctc.adaptor.model.Student
import com.ccctc.adaptor.model.Term
import com.ccctc.adaptor.util.mock.MockUtils
import spock.lang.Specification

/**
 * Created by zekeo on 7/20/2017.
 */
class StudentDBSpec extends Specification {
    List<Student> all
    Student first

    def misCode = "001"
    def sisTermId = "term1"
    def sisPersonId = "person1"
    def cccId = "cccid1"

    def collegeDB = new CollegeDB()
    def termDB = new TermDB(collegeDB)
    def personDB = new PersonDB(collegeDB)
    def studentDB = new StudentDB(collegeDB, termDB, personDB)

    def setup() {
        // initialize databases
        collegeDB.init()
        termDB.init()
        personDB.init()
        studentDB.init()

        // seed databases
        collegeDB.add(new College(misCode: misCode, districtMisCode: "000"))
        termDB.add(new Term(misCode: misCode, sisTermId: sisTermId, start: MockUtils.removeTime(new Date()), end: MockUtils.removeTime(new Date() + 1)))
        personDB.add(new Person(misCode: misCode, sisPersonId: sisPersonId, cccid: cccId))
        studentDB.add(new Student(misCode: misCode, sisPersonId: sisPersonId, sisTermId: sisTermId, applicationStatus: ApplicationStatus.ApplicationAccepted))

        all = studentDB.getAll()
        first = all[0]
    }

    def "get - direct hit"() {
        setup:
        def result

        // by sisPersonId
        when:
        result = studentDB.get(first.misCode, first.sisTermId, first.sisPersonId)

        then:
        result.toString() == first.toString()

        // by cccid
        when:
        result = studentDB.get(first.misCode, first.sisTermId, "cccid:" + first.cccid)

        then:
        result.toString() == first.toString()
    }

    def "get - other terms"() {
        setup:
        termDB.add(new Term(misCode: misCode, sisTermId: "older-term", start: MockUtils.removeTime(new Date() - 10), end: MockUtils.removeTime(new Date() - 9)))
        termDB.add(new Term(misCode: misCode, sisTermId: "newer-term", start: MockUtils.removeTime(new Date() + 9), end: MockUtils.removeTime(new Date() + 10)))

        when:
        def result1 = studentDB.get(first.misCode, "older-term", first.sisPersonId)
        def result2 = studentDB.get(first.misCode, "newer-term", first.sisPersonId)

        then:
        result1.applicationStatus == ApplicationStatus.NoApplication
        result2.applicationStatus == first.applicationStatus
    }

    def "get - person found but no student"() {
        setup:
        personDB.add(new Person(misCode: misCode, sisPersonId: "other"))

        when:
        studentDB.get(first.misCode, sisTermId, "other")

        then:
        thrown EntityNotFoundException
    }

    def "add"() {
        setup:
        def student = new Student()
        student.cohorts = [new Cohort.Builder().name(CohortTypeEnum.COURSE_EXCHANGE).build()]

        // missing mis code
        when:
        studentDB.add(student)

        then:
        thrown InvalidRequestException

        // missing term
        when:
        student.misCode = first.misCode
        studentDB.add(student)

        then:
        thrown InvalidRequestException

        // missing student
        when:
        student.sisTermId = first.sisTermId
        studentDB.add(student)

        then:
        thrown InvalidRequestException

        // invalid person
        when:
        student.sisPersonId = "test"
        studentDB.add(student)

        then:
        thrown InvalidRequestException

        // success
        when:
        personDB.add(new Person(misCode: first.misCode, sisPersonId: "test"))
        def result = studentDB.add(student)

        then:
        result.toString() == student.toString()
        result.cohorts.size() == 1
        result.cohorts[0].name == CohortTypeEnum.COURSE_EXCHANGE
        result.cohorts[0].description == CohortTypeEnum.COURSE_EXCHANGE.description
    }

    def "update"() {
        setup:
        def copy = studentDB.deepCopy(first)
        def result

        when:
        copy.cccid = "nope"
        studentDB.update(first.misCode, first.sisTermId, first.sisPersonId, copy)

        then:
        thrown InvalidRequestException

        when:
        copy.cccid = first.cccid
        copy.accountBalance = 5
        result = studentDB.update(first.misCode, first.sisTermId, first.sisPersonId, copy)

        then:
        result.toString() == copy.toString()
    }

    def "patch"() {
        when:
        def r = studentDB.patch(first.misCode, first.sisTermId, first.sisPersonId, [dspsEligible: true])

        then:
        r.dspsEligible == true
    }

    def "delete"() {
        when:
        def result = studentDB.delete(first.misCode, first.sisTermId, first.sisPersonId, true)

        then:
        result.toString() == first.toString()
    }

    def "copy"() {
        when:
        termDB.add(new Term(misCode: first.misCode, sisTermId: "sisTermId", start: new Date(), end: new Date() + 1))
        def result = studentDB.copy(first.misCode, first.sisTermId, first.sisPersonId, "sisTermId")

        then:
        result.sisTermId == "sisTermId"
    }

    def "get home college"() {
        when:
        def result = studentDB.getHomeCollege(first.misCode, first.sisPersonId)

        then:
        result.cccid == first.cccid

        // student not found
        when:
        studentDB.getHomeCollege(first.misCode, "nope")

        then:
        thrown InvalidRequestException
    }

    def "update home college"() {
        setup:
        def college1 = collegeDB.get(first.misCode)
        def college2 = collegeDB.add(new College(misCode: "999", districtMisCode: college1.districtMisCode))

        when:
        def result = studentDB.updateHomeCollege(first.misCode, first.sisPersonId, college2.misCode)

        then:
        result.misCode == college2.misCode

        // outside district
        when:
        studentDB.updateHomeCollege(first.misCode, first.sisPersonId, "998")

        then:
        thrown InvalidRequestException

    }

    def "getStudentCCCIds"() {
        setup:
        personDB.add(new Person(misCode: misCode, sisPersonId: "other", cccid: "otherCccId"))

        // person does not exist
        when:
        studentDB.getStudentCCCIds(misCode, "badStudentId")

        then:
        thrown InvalidRequestException

        // person not a student
        when:
        studentDB.getStudentCCCIds(misCode, "other")

        then:
        thrown EntityNotFoundException

        when:
        studentDB.add(new Student(misCode: misCode, sisTermId: sisTermId, sisPersonId: "other"))
        def result = studentDB.getStudentCCCIds(misCode, "other")

        then:
        result.contains("otherCccId")
    }

    def "postStudentCCCId - exception"() {
        setup:
        def newCccId = "newCccId"
        def usedCccId = "usedCccId"
        personDB.add(new Person(misCode: misCode, sisPersonId: "other", cccid: usedCccId))

        // person does not exist
        when:
        studentDB.postStudentCCCId(first.misCode, "badStudentId", newCccId)

        then:
        thrown InvalidRequestException

        // CCC ID already used by another person
        when:
        studentDB.postStudentCCCId(first.misCode, first.sisPersonId, usedCccId)

        then:
        thrown EntityConflictException

        // valid cccId
        when:
        studentDB.postStudentCCCId(first.misCode, first.sisPersonId, newCccId)

        then:
        thrown EntityConflictException

        // valid cccId - but no change because it's not different
        when:
        studentDB.postStudentCCCId(first.misCode, first.sisPersonId, newCccId)

        then:
        thrown EntityConflictException
    }

    def "postStudentCCCId - success"() {
        setup:
        personDB.add(new Person(misCode: misCode, sisPersonId: "other"))
        studentDB.add(new Student(misCode: misCode, sisPersonId: "other", sisTermId: sisTermId))

        // person does not exist
        when:
        def result = studentDB.postStudentCCCId(first.misCode, "other", "newCccId")

        then:
        result.contains("newCccId")
    }
}