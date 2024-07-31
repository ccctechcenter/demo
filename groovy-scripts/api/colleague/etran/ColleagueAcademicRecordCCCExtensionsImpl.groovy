package api.colleague.etran

import com.fasterxml.jackson.annotation.JsonProperty
import com.xap.ccctran.impl.AcademicRecordCCCExtensionsImpl
import static com.xap.ccctran.impl.AcademicRecordCCCExtensionsImpl.*

class ColleagueAcademicRecordCCCExtensionsImpl extends AcademicRecordCCCExtensionsImpl {

    @JsonProperty("gradeSchemeEntries")
    void setGradeSchemeEntries(List<GradeSchemeEntryImpl> value) {
        super.gradeSchemeEntries = value
    }

    @JsonProperty("notationLegendEntries")
    void setNotationLegendEntries(List<NotationLegendEntryImpl> value) {
        super.notationLegendEntries = value
    }

    @JsonProperty("institutionInformationEntries")
    void setInstitutionInformationEntries(List<InstitutionInformationEntryImpl> value) {
        super.institutionInformationEntries = value
    }

    @JsonProperty("csuAmerHistCompletion")
    void setCSUAmerHistCompletion(CSUAmerHistCompletionImpl value) {
        super.setCSUAmerHistCompletion(value)
    }

    @JsonProperty("csugeCompletion")
    void setCSUGECompletion(CSUGECompletionImpl value) {
        super.setCSUGECompletion(value)
    }

    @JsonProperty("csuigetcCompletion")
    void setCSUIGETCCompletion(CSUIGETCCompletionImpl value) {
        super.setCSUIGETCCompletion(value)
    }

    @JsonProperty("ucigetcCompletion")
    void setUCIGETCCompletion(UCIGETCCompletionImpl value) {
        super.setUCIGETCCompletion(value)
    }
}
