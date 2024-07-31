package api.colleague

import api.colleague.BOGWaiver as GroovyBOGWaiver
import com.ccctc.adaptor.exception.InternalServerException
import com.ccctc.adaptor.exception.InvalidRequestException
import com.ccctc.adaptor.model.BOGWaiver
import spock.lang.Specification

class BOGWaiverSpec extends Specification {

    def groovyBOGWaiver = new GroovyBOGWaiver()

    def "assertions"() {
        when:
        groovyBOGWaiver.get(null, null, null)

        then:
        thrown AssertionError

        when:
        groovyBOGWaiver.get("000", null, null)

        then:
        thrown AssertionError

        when:
        groovyBOGWaiver.get("000", "ABC123", null)

        then:
        thrown AssertionError

        when:
        groovyBOGWaiver.post(null, null)

        then:
        thrown AssertionError

        when:
        groovyBOGWaiver.post("000", null)

        then:
        thrown AssertionError
    }

    def "get"() {
        def b = new BOGWaiver()

        when:
        def result = groovyBOGWaiver.get("000", "ABC123", "term")

        then:
        thrown InternalServerException
    }

    def "post"() {
        def b = new BOGWaiver()

        when:
        groovyBOGWaiver.post("000", b)

        then:
        thrown InvalidRequestException

        when:
        b.sisTermId = "term"
        groovyBOGWaiver.post("000", b)

        then:
        thrown InvalidRequestException

        when:
        b.cccid = "ABC123"
        def result = groovyBOGWaiver.post("000", b)

        then:
        thrown InternalServerException
    }
}
