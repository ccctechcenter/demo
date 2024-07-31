package com.ccctc.adaptor.model.mock

import com.ccctc.adaptor.exception.EntityNotFoundException
import com.ccctc.adaptor.exception.InvalidRequestException
import com.ccctc.adaptor.model.College
import com.ccctc.adaptor.model.Term
import com.ccctc.adaptor.util.mock.MockUtils
import spock.lang.Specification

/**
 * Created by zekeo on 7/20/2017.
 */
class TermDBSpec extends Specification  {

    List<Term> all
    Term first

    def misCode = "001"
    def sisTermId = "TERM"

    def collegeDB = new CollegeDB()
    def termDB = new TermDB(collegeDB)

    def setup() {
        // initialize databases
        collegeDB.init()
        termDB.init()

        // seed databases
        collegeDB.add(new College(misCode: misCode, districtMisCode: "000"))
        termDB.add(new Term(misCode: misCode, sisTermId: sisTermId, start: new Date(2000, 1, 1), end: new Date(2000, 6, 30)))

        all = termDB.getAllSorted()
        first = all[0]
    }

    def "get"() {
        when:
        def r = termDB.get(first.misCode, first.sisTermId)

        then:
        r.toString() == first.toString()
    }

    def "find - date search by long number"() {
        when:
        def t = termDB.getAll().find { i -> i.start != null }
        def r = termDB.find([start: t.start.getTime()])

        then:
        r[0].toString() == t.toString()
    }

    def "add"() {
        setup:
        def term = new Term()

        // missing misCode
        when:
        termDB.add(term)

        then:
        thrown InvalidRequestException

        // missing sisTermId
        when:
        term.misCode = "999"
        termDB.add(term)

        then:
        thrown InvalidRequestException

        // bad misCode
        when:
        term.sisTermId = "sisTermId"
        termDB.add(term)

        then:
        thrown InvalidRequestException

        // missing start date
        when:
        term.misCode = first.misCode
        termDB.add(term)

        then:
        thrown InvalidRequestException

        // missing end date
        when:
        term.start = MockUtils.removeTime(new Date())
        termDB.add(term)

        then:
        thrown InvalidRequestException

        // success
        when:
        term.end = term.start + 1
        def result = termDB.add(term)

        then:
        result.toString() == term.toString()
    }

    def "update"() {
        setup:
        def copy = termDB.deepCopy(first)

        // can't change key
        when:
        copy.misCode = "999"
        termDB.update(first.misCode, first.sisTermId, copy)

        then:
        thrown InvalidRequestException

        // can't change key
        when:
        copy.misCode = first.misCode
        copy.sisTermId = "nope"
        termDB.update(first.misCode, first.sisTermId, copy)

        then:
        thrown InvalidRequestException

        when:
        copy.misCode = first.misCode
        copy.sisTermId = first.sisTermId
        copy.description = "changed"
        def result = termDB.update(first.misCode, first.sisTermId, copy)

        then:
        result.toString() == copy.toString()
    }

    def "patch"() {
        when:
        def r = termDB.patch(first.misCode, first.getSisTermId(), [description: "patched"])

        then:
        r.description == "patched"
    }

    def "delete"() {
        when:
        def result = termDB.delete(misCode, sisTermId, true)

        then:
        result.sisTermId == sisTermId
    }

    def "getCurrentTerm"() {
        // found
        when:
        def a = termDB.getCurrentTerm(first.misCode)

        then:
        a != null

        // not found
        when:
        termDB.deleteAll(true)
        termDB.getCurrentTerm(first.misCode)

        then:
        thrown EntityNotFoundException
    }

    def "findSorted"() {
        when:
        def a = termDB.findSorted(["misCode": first.misCode])

        then:
        a.size() > 0

        when:
        termDB.deleteAll(true)
        a = termDB.findSorted(["misCode": first.misCode])

        then:
        a.size() == 0
    }

    def "cascadeDelete"() {
        when:
        collegeDB.delete(misCode, true)
        def result = termDB.get(misCode, sisTermId)

        then:
        thrown InvalidRequestException // invalid college
    }
}
