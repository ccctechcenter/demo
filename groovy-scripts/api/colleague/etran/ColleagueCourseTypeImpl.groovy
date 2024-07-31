package api.colleague.etran

import com.ccctc.core.coremain.v1_14.impl.LicensureTypeImpl
import com.ccctc.core.coremain.v1_14.impl.RAPTypeImpl
import com.ccctc.sector.academicrecord.v1_9.impl.CourseSupplementalAcademicGradeTypeImpl
import com.ccctc.sector.academicrecord.v1_9.impl.CourseTypeImpl
import com.ccctc.sector.academicrecord.v1_9.impl.SchoolTypeImpl
import com.fasterxml.jackson.annotation.JsonProperty

class ColleagueCourseTypeImpl extends CourseTypeImpl {

    @JsonProperty("requirements")
    void setRequirements(List<RAPTypeImpl> value) {
        super.requirements = value
    }

    @JsonProperty("attributes")
    void setAttributes(List<RAPTypeImpl> value) {
        super.attributes = value
    }

    @JsonProperty("proficiencies")
    void setProficiencies(List<RAPTypeImpl> value) {
        super.proficiencies = value
    }

    @JsonProperty("licensures")
    void setLicensures(List<LicensureTypeImpl> value) {
        super.licensures = value
    }

    @JsonProperty("courseSupplementalAcademicGrade")
    void setCourseSupplementalAcademicGrade(CourseSupplementalAcademicGradeTypeImpl value) {
        super.courseSupplementalAcademicGrade = value
    }

    @JsonProperty("courseOverrideSchool")
    void setCourseOverrideSchool(SchoolTypeImpl value) {
        super.courseOverrideSchool = value
    }

    @JsonProperty("userDefinedExtensions")
    void setUserDefinedExtensions(ColleagueUserDefinedExtensionsTypeImpl value) {
        super.userDefinedExtensions = value
    }
}
