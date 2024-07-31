package api.mock

import api.mock.Course as CourseGroovy
import com.ccctc.adaptor.exception.InvalidRequestException
import com.ccctc.adaptor.model.Course
import com.ccctc.adaptor.model.Term
import com.ccctc.adaptor.model.mock.CourseDB
import com.ccctc.adaptor.model.mock.TermDB
import spock.lang.Specification

class CourseSpec extends Specification {

    def termDB = Mock(TermDB)
    def courseDB = Mock(CourseDB)
    def groovyClass = new CourseGroovy(termDB: termDB, courseDB: courseDB)

    def "get - current term"() {
        setup:
        def currentTerm = new Term(sisTermId: "sisTermId")
        def course = new Course()
        def result

        when:
        result = groovyClass.get("misCode", "sisCourseId", null)

        then:
        1 * termDB.getCurrentTerm("misCode") >> currentTerm
        1 * courseDB.get("misCode", "sisTermId", "sisCourseId") >> course
        result == course
    }

    def "get - no current term"() {
        when:
        groovyClass.get("misCode", "sisCourseId", null)

        then:
        1 * termDB.getCurrentTerm("misCode") >> null
        thrown InvalidRequestException
    }

    def "get"() {
        setup:
        def course = new Course()
        def result

        when:
        result = groovyClass.get("misCode", "sisCourseId", "sisTermId")

        then:
        1 * courseDB.get("misCode", "sisTermId", "sisCourseId") >> course
        0 * _
        result == course
    }
}
