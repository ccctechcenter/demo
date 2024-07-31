package api.colleague.etran

import com.ccctc.core.coremain.v1_14.impl.StudentLevelTypeImpl
import com.ccctc.sector.academicrecord.v1_9.impl.AcademicRecordTypeImpl
import com.ccctc.sector.academicrecord.v1_9.impl.SchoolTypeImpl
import com.fasterxml.jackson.annotation.JsonProperty

class ColleagueAcademicRecordTypeImpl extends AcademicRecordTypeImpl {

    @JsonProperty("academicAwardService")
    void setAcademicAwards(List<ColleagueAcademicAwardTypeImpl> value) {
        super.academicAwards = value
    }

    @JsonProperty("academicSessions")
    void setAcademicSessions(List<ColleagueAcademicSessionTypeImpl> value) {
        super.academicSessions = value
    }

    @JsonProperty("academicSummaries")
    void setAcademicSummaries(List<ColleagueAcademicSummaryFTypeImpl> value) {
        super.academicSummaries = value
    }

    @JsonProperty("courses")
    void setCourses(List<ColleagueCourseTypeImpl> value) {
        super.courses = value
    }

    @JsonProperty("school")
    void setSchool(SchoolTypeImpl value) {
        super.school = value
    }

    @JsonProperty("studentLevel")
    void setStudentLevel(StudentLevelTypeImpl value) {
        super.studentLevel = value
    }

    @JsonProperty("userDefinedExtensions")
    void setUserDefinedExtensions(ColleagueUserDefinedExtensionsTypeImpl value) {
        super.userDefinedExtensions = value
    }
}
