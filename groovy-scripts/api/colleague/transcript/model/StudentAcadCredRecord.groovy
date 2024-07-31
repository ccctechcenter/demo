package api.colleague.transcript.model

import org.ccctc.colleaguedmiclient.annotation.Association
import org.ccctc.colleaguedmiclient.annotation.Entity
import org.ccctc.colleaguedmiclient.annotation.Join
import org.ccctc.colleaguedmiclient.model.ColleagueRecord
import groovy.transform.CompileStatic

import java.time.LocalDate

@CompileStatic
@Entity(appl = "ST", name = "STUDENT.ACAD.CRED")
class StudentAcadCredRecord extends ColleagueRecord {

    String stcTerm
    BigDecimal stcCred
    BigDecimal stcCmplCred
    BigDecimal stcAltcumContribCmplCred
    BigDecimal stcAltcumContribGradePts
    BigDecimal stcAltcumContribGpaCred
    String stcCourseName
    String stcSectionNo
    String stcReplCode
    String stcTitle
    LocalDate stcStartDate
    LocalDate stcEndDate
    String stcPrintedComments

    @Association
    List<StcStatusesAssoc> stcStatusesAssoc

    @Join
    StudentAcadCredGradeRecord stcVerifiedGrade

    @Join
    CoursesRecord stcCourse

    @Join
    StudentCourseSecRecord stcStudentCourseSec

}
