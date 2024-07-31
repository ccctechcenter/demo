package api.colleague.etran

import com.ccctc.core.coremain.v1_14.impl.AcademicSessionDetailTypeImpl
import com.ccctc.sector.academicrecord.v1_9.impl.AcademicProgramTypeImpl
import com.ccctc.sector.academicrecord.v1_9.impl.AcademicSessionTypeImpl
import com.fasterxml.jackson.annotation.JsonProperty

class ColleagueAcademicSessionTypeImpl extends AcademicSessionTypeImpl {

    @JsonProperty("academicSessionDetail")
    void setAcademicSessionDetail(AcademicSessionDetailTypeImpl value) {
        super.setAcademicSessionDetail(value)
    }

    @JsonProperty("academicPrograms")
    void setAcademicPrograms(List<AcademicProgramTypeImpl> value) {
        super.academicPrograms = value
    }

    @JsonProperty("courses")
    void setCourses(List<ColleagueCourseTypeImpl> value) {
        super.courses = value
    }

    @JsonProperty("academicSummaries")
    void setAcademicSummaries(List<ColleagueAcademicSummaryFTypeImpl> value) {
        super.academicSummaries = value
    }
}
