package api.colleague.transcript.model

import org.ccctc.colleaguedmiclient.annotation.Entity
import org.ccctc.colleaguedmiclient.annotation.Ignore
import org.ccctc.colleaguedmiclient.model.ColleagueRecord
import groovy.transform.CompileStatic

@CompileStatic
@Entity(appl = "ST", name = "GRADES")
class GradesRecord extends ColleagueRecord {

    String grdGrade
    String grdAttCredFlag
    String grdCmplCredFlag
    String grdGpaCredFlag
    BigDecimal grdValue
    String grdLegend
    String grdGradeScheme

    // derived in the code
    @Ignore
    String gradeScaleCode
}
