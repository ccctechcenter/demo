package api.colleague

import api.colleague.FinancialAidUnits as GroovyFinancialAidUnits
import com.ccctc.adaptor.exception.InternalServerException
import com.ccctc.adaptor.model.FinancialAidUnits
import spock.lang.Specification

class FinancialAidUnitsSpec extends Specification {

    def groovyFinancialAidUnits = new GroovyFinancialAidUnits()

    def "getUnitsList - missing params"() {
        when: groovyFinancialAidUnits.getUnitsList(null, null, null)
        then: thrown AssertionError
        when: groovyFinancialAidUnits.getUnitsList("000", null, null)
        then: thrown AssertionError
        when: groovyFinancialAidUnits.getUnitsList("000", "cccid", null)
        then: thrown AssertionError
    }

    def "post - missing params"() {
        when: groovyFinancialAidUnits.post(null, null)
        then: thrown AssertionError
        when: groovyFinancialAidUnits.post("000", null)
        then: thrown AssertionError
    }

    def "delete - missing params"() {
        when: groovyFinancialAidUnits.delete(null, null, null, null, null)
        then: thrown AssertionError
        when: groovyFinancialAidUnits.delete("000", null, null, null, null)
        then: thrown AssertionError
        when: groovyFinancialAidUnits.delete("000", "cccid", null, null, null)
        then: thrown AssertionError
        when: groovyFinancialAidUnits.delete("000", "cccid", "sisTermId", null, null)
        then: thrown AssertionError
        when: groovyFinancialAidUnits.delete("000", "cccid", "sisTermId", "enrolledMisCode", null)
        then: thrown AssertionError
    }

    def "getUnitsList"() {
        when:
        groovyFinancialAidUnits.getUnitsList("000", "cccid", "sisTermId")

        then:
        thrown InternalServerException
    }

    def "post"() {
        setup:
        def f = new FinancialAidUnits()

        when:
        groovyFinancialAidUnits.post("000", f)

        then:
        thrown InternalServerException
    }

    def "delete"() {
        when:
        groovyFinancialAidUnits.delete("000", "cccid", "sisTermId", "enrolledMisCode", "c id")

        then:
        thrown InternalServerException
    }
}
