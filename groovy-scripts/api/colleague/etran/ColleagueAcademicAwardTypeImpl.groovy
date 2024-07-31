package api.colleague.etran

import com.ccctc.core.coremain.v1_14.impl.AcademicHonorsTypeImpl
import com.ccctc.sector.academicrecord.v1_9.impl.AcademicAwardTypeImpl
import com.ccctc.sector.academicrecord.v1_9.impl.AcademicProgramTypeImpl
import com.fasterxml.jackson.annotation.JsonProperty

class ColleagueAcademicAwardTypeImpl extends AcademicAwardTypeImpl {

    @JsonProperty("academicHonors")
    void setAcademicHonors(List<AcademicHonorsTypeImpl> value) {
        super.academicHonors = value
    }

    @JsonProperty("academicAwardPrograms")
    void setAcademicAwardPrograms(List<AcademicProgramTypeImpl> value) {
        super.academicAwardPrograms = value
    }
}
