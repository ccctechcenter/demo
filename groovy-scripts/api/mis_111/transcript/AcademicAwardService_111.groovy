package api.mis_111.transcript

import api.colleague.transcript.base.AcademicAwardService
import api.colleague.transcript.model.AcadCredentialsRecord
import com.ccctc.sector.academicrecord.v1_9.AcademicAwardType
import groovy.transform.CompileStatic

@CompileStatic
class AcademicAwardService_111 extends AcademicAwardService {

    AcademicAwardService_111(AcademicAwardService a) {
        super(a)

        this.institutionsAttendEntity = InstitutionsAttendRecord_111.class
    }

    @Override
    protected AcademicAwardType parseOne(AcadCredentialsRecord acadCredential) {
        def result = super.parseOne(acadCredential)

        def data = (AcadCredentialsRecord_111) acadCredential

        // add comments, which contain legacy awards
        if (data.acadComments) {
            result.noteMessages << data.acadComments
        }

        return result
    }
}
