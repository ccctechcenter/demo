package api.mock

import com.ccctc.adaptor.exception.InvalidRequestException
import com.ccctc.adaptor.model.mock.CourseDB
import com.ccctc.adaptor.model.mock.TermDB
import org.springframework.core.env.Environment

class Course {
    Environment environment
    // injected using GroovyServiceImpl on creation of class
    private CourseDB courseDB
    private TermDB termDB

    def get(String misCode, String sisCourseId, String sisTermId) {
        // get "current term" if no term is supplied
        def termId = sisTermId ?: termDB.getCurrentTerm(misCode)?.sisTermId

        if (!termId)
            throw new InvalidRequestException(InvalidRequestException.Errors.invalidSearchCriteria, "No current term found in mock database")

        return courseDB.get(misCode, termId, sisCourseId)
    }
}
