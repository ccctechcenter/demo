package api.mis_591

import com.ccctc.adaptor.exception.InternalServerException
import spock.lang.Specification
import api.mis_591.Enrollment as GroovyEnrollment

class EnrollmentSpec extends Specification {

    def groovyEnrollment = new GroovyEnrollment()

    def "getPrereqStatus - missing params"() {
        when:
        groovyEnrollment.getPrereqStatus(null, null, null, null)

        then:
        thrown AssertionError

        when:
        groovyEnrollment.getPrereqStatus("000", null, null, null)

        then:
        thrown AssertionError

        when:
        groovyEnrollment.getPrereqStatus("000", "sisCourseId", null, null)

        then:
        thrown AssertionError
    }

    def "getPrereqStatus"() {
        when:
        groovyEnrollment.getPrereqStatus("000", "sisCourseId", null, "cccId")

        then:
        thrown InternalServerException
    }
}
