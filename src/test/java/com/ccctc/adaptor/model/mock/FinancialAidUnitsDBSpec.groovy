package com.ccctc.adaptor.model.mock

import com.ccctc.adaptor.exception.InvalidRequestException
import com.ccctc.adaptor.model.ApplicationStatus
import com.ccctc.adaptor.model.College
import com.ccctc.adaptor.model.CourseExchangeEnrollment
import com.ccctc.adaptor.model.FinancialAidUnits
import com.ccctc.adaptor.model.Person
import com.ccctc.adaptor.model.Student
import com.ccctc.adaptor.model.Term
import com.ccctc.adaptor.util.mock.MockUtils
import spock.lang.Specification

/**
 * Created by zekeo on 7/20/2017.
 */
class FinancialAidUnitsDBSpec extends Specification  {
    List<FinancialAidUnits> all
    FinancialAidUnits first
    def misCode = "001"
    def sisTermId = "term1"
    def sisPersonId = "person1"
    def cccId = "cccid1"

    def collegeDB = new CollegeDB()
    def termDB = new TermDB(collegeDB)
    def personDB = new PersonDB(collegeDB)
    def studentDB = new StudentDB(collegeDB, termDB, personDB)
    def financialAidUnitsDB = new FinancialAidUnitsDB(collegeDB, termDB, studentDB, personDB)

    def setup() {
        // initialize databases
        collegeDB.init()
        termDB.init()
        personDB.init()
        studentDB.init()
        financialAidUnitsDB.init()

        // seed databases
        collegeDB.add(new College(misCode: misCode, districtMisCode: "000"))
        termDB.add(new Term(misCode: misCode, sisTermId: sisTermId, start: MockUtils.removeTime(new Date()), end: MockUtils.removeTime(new Date() + 1)))
        personDB.add(new Person(misCode: misCode, sisPersonId: sisPersonId, cccid: cccId))
        studentDB.add(new Student(misCode: misCode, sisPersonId: sisPersonId, sisTermId: sisTermId, applicationStatus: ApplicationStatus.ApplicationAccepted))
        financialAidUnitsDB.add(new FinancialAidUnits(misCode: misCode, sisPersonId: sisPersonId, sisTermId: sisTermId,
                ceEnrollmentMisCode: "999", ceEnrollmentC_id: "ENG 100"
        ))

        all = financialAidUnitsDB.getAll()
        first = all[0]
    }

    def "get"() {
        setup:
        def result

        when:
        result = financialAidUnitsDB.get(first.misCode, first.sisTermId, first.sisPersonId, first.ceEnrollmentMisCode, first.ceEnrollmentC_id)

        then:
        result.toString() == first.toString()
    }

    def "add"() {
        setup:
        financialAidUnitsDB.deleteAll(true)
        def faunits = new FinancialAidUnits()

        // missing mis code
        when:
        financialAidUnitsDB.add(faunits)

        then:
        thrown InvalidRequestException

        // missing term
        when:
        faunits.misCode = first.misCode
        financialAidUnitsDB.add(faunits)

        then:
        thrown InvalidRequestException

        // missing student
        when:
        faunits.sisTermId = first.sisTermId
        financialAidUnitsDB.add(faunits)

        then:
        thrown InvalidRequestException

        // missing enrolled mis code
        when:
        faunits.sisPersonId = "test"
        financialAidUnitsDB.add(faunits)

        then:
        thrown InvalidRequestException

        // missing enrolled c_id
        when:
        faunits.ceEnrollmentMisCode = "999"
        financialAidUnitsDB.add(faunits)

        then:
        thrown InvalidRequestException

        // invalid person
        when:
        faunits.ceEnrollment.c_id = "TEST 100"
        financialAidUnitsDB.add(faunits)

        then:
        thrown InvalidRequestException

        // invalid student
        when:
        personDB.add(new Person(misCode: first.misCode, sisPersonId: "test"))
        financialAidUnitsDB.add(faunits)

        then:
        thrown InvalidRequestException

        // success
        when:
        studentDB.add(new Student(misCode: first.misCode, sisTermId: first.sisTermId, sisPersonId: "test"))
        def result = financialAidUnitsDB.add(faunits)

        then:
        result.toString() == faunits.toString()
    }

    def "update"() {
        setup:
        def copy = financialAidUnitsDB.deepCopy(first)
        def result

        when:
        copy.ceEnrollment.units = 5
        result = financialAidUnitsDB.update(first.misCode, first.sisTermId, first.sisPersonId, first.ceEnrollmentMisCode, first.ceEnrollmentC_id, copy)

        then:
        result.toString() == copy.toString()
    }

    def "patch"() {
        when:
        def r = financialAidUnitsDB.patch(first.misCode, first.sisTermId, first.sisPersonId, first.ceEnrollmentMisCode, first.ceEnrollmentC_id,
                [ceEnrollment: new CourseExchangeEnrollment(units: 9)])

        then:
        r.ceEnrollment.units == (float) 9
    }

    def "delete"() {
        when:
        def result = financialAidUnitsDB.delete(first.misCode, first.sisTermId, first.sisPersonId, first.ceEnrollmentMisCode, first.ceEnrollmentC_id, true)

        then:
        result.toString() == first.toString()
    }

    def "cascade CCC ID change from person"() {
        setup:
        def newCccId = "newCccId"

        when:
        def person = personDB.patch(first.misCode, first.sisPersonId, [cccid: newCccId])
        def faUnits = financialAidUnitsDB.get(financialAidUnitsDB.getPrimaryKey(first))

        then:
        person.cccid == newCccId
        faUnits.cccid == newCccId
    }

    def "cascade delete from student (by way of person)"() {
        setup:
        personDB.delete(first.misCode, first.sisPersonId, true)

        when:
        financialAidUnitsDB.get(financialAidUnitsDB.getPrimaryKey(first))

        then:
        thrown InvalidRequestException // invalid person
    }
}
