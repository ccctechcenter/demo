package api.colleague.etran

import com.ccctc.sector.academicrecord.v1_9.impl.StudentTypeImpl
import com.fasterxml.jackson.annotation.JsonProperty

class ColleagueStudentTypeImpl extends StudentTypeImpl {

    @JsonProperty("person")
    void setPerson(ColleaguePersonTypeImpl value) {
        super.person = value
    }

    @JsonProperty("academicRecords")
    void setAcademicRecords(List<ColleagueAcademicRecordTypeImpl> value) {
        super.academicRecords = value
    }
}