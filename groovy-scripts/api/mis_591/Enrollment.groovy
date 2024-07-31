package api.mis_591

import com.ccctc.adaptor.exception.InternalServerException
import com.ccctc.adaptor.exception.InvalidRequestException
import com.ccctc.adaptor.model.PrerequisiteStatus

class Enrollment extends api.colleague.Enrollment {

    //
    // Columbia uses a "C" prefix on their courses - for Course Exchange pre-requisite checking try finding
    // both the course as passed as well as with a "C" in the prefix
    //

    @Override
    PrerequisiteStatus getPrereqStatus(String misCode, String sisCourseId, Date start, String cccId) {
        assert misCode != null
        assert sisCourseId != null
        assert cccId != null

        throw new InternalServerException("Unsupported")

        /*
        try {
            return super.getPrereqStatus(misCode, sisCourseId, start, cccId)
        } catch (InvalidRequestException e) {
            if (e.code != InvalidRequestException.Errors.courseNotFound)
                throw e
        }

        try {
            return super.getPrereqStatus(misCode, "C" + sisCourseId, start, cccId)
        } catch (InvalidRequestException e) {
            if (e.code != InvalidRequestException.Errors.courseNotFound)
                throw e
        }

        throw new InvalidRequestException(InvalidRequestException.Errors.courseNotFound,
                "Course not found (searched for both $sisCourseId and C$sisCourseId)")*/
    }
}
