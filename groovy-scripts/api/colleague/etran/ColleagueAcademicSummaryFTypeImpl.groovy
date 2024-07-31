package api.colleague.etran

import com.ccctc.core.coremain.v1_14.impl.GPATypeImpl
import com.ccctc.sector.academicrecord.v1_9.impl.AcademicSummaryFTypeImpl
import com.fasterxml.jackson.annotation.JsonProperty

class ColleagueAcademicSummaryFTypeImpl extends AcademicSummaryFTypeImpl {

    @JsonProperty("gpa")
    void setGpa(GPATypeImpl value) {
        super.setGPA(value)
    }
}
