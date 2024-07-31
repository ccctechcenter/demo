package api.colleague

import com.ccctc.adaptor.exception.InternalServerException
import com.ccctc.adaptor.util.impl.TranscriptService
import com.ccctc.message.collegetranscript.v1_6.impl.CollegeTranscriptImpl
import spock.lang.Specification

class TranscriptSpec extends Specification {
    def mockTranscriptService = Mock(TranscriptService)

    // @TODO

    /*
    def transcript = new Transcript()

    def "get including lookup"() {
        when:
        def result = transcript.get(null, "firstName",
                "lastName",
                "birthDate",
                "ssn",
                "partialSSN",
                "emailAddress",
                "schoolAssignedStudentID",
                "cccID",
                mockTranscriptService)
        then:
        thrown InternalServerException
    }

    def "post"() {
        when:
        transcript.post("000", new CollegeTranscriptImpl())

        then:
        thrown InternalServerException
    }*/

}
