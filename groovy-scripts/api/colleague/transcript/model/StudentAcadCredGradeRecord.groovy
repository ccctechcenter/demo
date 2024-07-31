package api.colleague.transcript.model

import groovy.transform.CompileStatic
import org.ccctc.colleaguedmiclient.annotation.Association
import org.ccctc.colleaguedmiclient.annotation.Entity
import org.ccctc.colleaguedmiclient.annotation.Join
import org.ccctc.colleaguedmiclient.model.ColleagueRecord

import java.time.LocalDate

@CompileStatic
@Entity(appl = "ST", name = "GRADES")
class StudentAcadCredGradeRecord extends ColleagueRecord {

    String grdGrade

}
