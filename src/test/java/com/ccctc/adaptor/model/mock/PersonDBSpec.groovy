package com.ccctc.adaptor.model.mock

import com.ccctc.adaptor.exception.EntityNotFoundException
import com.ccctc.adaptor.exception.InvalidRequestException
import com.ccctc.adaptor.model.College
import com.ccctc.adaptor.model.Person
import spock.lang.Specification

/**
 * Created by zekeo on 7/20/2017.
 */
class PersonDBSpec extends Specification  {

    List<Person> all
    Person first
    def misCode = "001"
    def sisPersonId = "person1"
    def cccid = "ABC123"
    def loginId = "stu"
    def loginSuffix = "stu.edu"

    def collegeDB = new CollegeDB()
    def personDB = new PersonDB(collegeDB)

    def setup() {
        // initialize databases
        collegeDB.init()
        personDB.init()

        // seed databases
        collegeDB.add(new College(misCode: misCode, districtMisCode: "000"))
        personDB.add(new Person(misCode: misCode, sisPersonId: sisPersonId, cccid: cccid, loginId: loginId, loginSuffix: loginSuffix))

        all = personDB.getAll()
        first = all[0]
    }

    def "get"() {
        setup:
        def result

        // by sisPersonId
        when:
        result = personDB.get(first.misCode, first.sisPersonId)

        then:
        result.toString() == first.toString()

        // by cccid
        when:
        result = personDB.get(first.misCode, "cccid:" + first.cccid)

        then:
        result.toString() == first.toString()
    }

    def "add"() {
        setup:
        def person = new Person()

        // missing mis code
        when:
        personDB.add(person)

        then:
        thrown InvalidRequestException

        // missing person
        when:
        person.misCode = first.misCode
        personDB.add(person)

        then:
        thrown InvalidRequestException

        // success
        when:
        person.sisPersonId = "test"
        def result = personDB.add(person)

        then:
        result.toString() == person.toString()
    }

    def "update"() {
        setup:
        def copy = personDB.deepCopy(first)

        when:
        copy.sisPersonId = "nope"
        personDB.update(first.misCode, first.sisPersonId, copy)

        then:
        thrown InvalidRequestException

        when:
        copy.sisPersonId = first.sisPersonId
        copy.firstName = "Test"
        def result = personDB.update(first.misCode, first.sisPersonId, copy)

        then:
        result.toString() == copy.toString()
    }

    def "patch"() {
        when:
        def r = personDB.patch(first.misCode, first.sisPersonId, [lastName: "Test"])

        then:
        r.lastName == "Test"
    }

    def "delete"() {
        when:
        def result = personDB.delete(first.misCode, first.sisPersonId, true)

        then:
        result.toString() == first.toString()
    }

    def "validate"() {
        setup:
        def result

        //mismatch
        when:
        personDB.validate(first.misCode, first.sisPersonId, "nope")

        then:
        thrown InvalidRequestException

        //ccid only
        when:
        result = personDB.validate(first.misCode, "cccid:" + first.cccid)

        then:
        result.toString() == first.toString()

        //not found
        when:
        personDB.validate(first.misCode, "nope", "nope")

        then:
        thrown InvalidRequestException

        //no parameters
        when:
        personDB.validate(first.misCode, null, null)

        then:
        thrown InvalidRequestException
    }

    def "validate - sisPersonId, eppn, cccid"() {
        setup:
        def result

        // successes

        when:
        result = personDB.validate(first.misCode, first.sisPersonId, null,null)

        then:
        result.toString() == first.toString()

        when:
        result = personDB.validate(first.misCode, first.sisPersonId, null, first.loginId + "@" + first.loginSuffix)

        then:
        result.toString() == first.toString()

        when:
        result = personDB.validate(first.misCode, null, null, first.loginId + "@" + first.loginSuffix)

        then:
        result.toString() == first.toString()

        when:
        result = personDB.validate(first.misCode, null, first.cccid, null)

        then:
        result.toString() == first.toString()

        when:
        result = personDB.validate(first.misCode, first.sisPersonId, null)

        then:
        result.toString() == first.toString()


        // failures

        //personId null
        when:
        personDB.validate(first.misCode, null, null, null)

        then:
        thrown EntityNotFoundException


        when:
        personDB.validate(first.misCode, first.sisPersonId, "nope",null)

        then:
        thrown InvalidRequestException

        when:
        personDB.validate(first.misCode, first.sisPersonId, null,"nope@nope")

        then:
        thrown InvalidRequestException

        when:
        personDB.validate(first.misCode, first.sisPersonId, "TEST001","nope")

        then:
        thrown InvalidRequestException

        when:
        personDB.validate(first.misCode, null, first.cccid,"nope")

        then:
        thrown InvalidRequestException

        when:
        personDB.validate(first.misCode, null, "nope","test1@mock.edu")

        then:
        thrown EntityNotFoundException

        //not found
        when:
        personDB.validate(first.misCode, "nope", "nope")

        then:
        thrown InvalidRequestException

        //no parameters
        when:
        personDB.validate(first.misCode, null, null)

        then:
        thrown InvalidRequestException

    }


    def "get - multipleResultsFound"() {
        //multiple results
        when:
        InvalidRequestException ex
        try {
            personDB.add(new Person(misCode: first.misCode, sisPersonId: "test", cccid: first.cccid))
            personDB.get(first.misCode, "cccid:" + first.cccid)
        } catch (InvalidRequestException e) {
            ex = e
        }

        then:
        ex != null
        ex.code == InvalidRequestException.Errors.multipleResultsFound
    }

    def "translateCccId - null"() {
        when:
        def r = personDB.translateCccId(misCode, "alsfh")

        then:
        r == null
    }

    def "cascade delete"() {
        when:
        collegeDB.delete(misCode, true)
        personDB.get(misCode, sisPersonId)

        then:
        thrown InvalidRequestException // invalid mis code
    }
}
