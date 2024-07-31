package api.colleague.etran

import com.ccctc.core.coremain.v1_14.impl.UserDefinedExtensionsTypeImpl
import com.fasterxml.jackson.annotation.JsonProperty
import com.xap.ccctran.impl.CourseCCCExtensionsImpl

class ColleagueUserDefinedExtensionsTypeImpl extends UserDefinedExtensionsTypeImpl {

    @JsonProperty("academicRecordCCCExtensions")
    void setAcademicRecordCCCExtensions(ColleagueAcademicRecordCCCExtensionsImpl value) {
        super.academicRecordCCCExtensions = value
    }

    @JsonProperty("courseCCCExtensions")
    void setCourseCCCExtensions(CourseCCCExtensionsImpl value) {
        super.courseCCCExtensions = value
    }
}
