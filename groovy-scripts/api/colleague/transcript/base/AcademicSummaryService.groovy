package api.colleague.transcript.base

import com.ccctc.core.coremain.v1_14.AcademicSummaryTypeType
import com.ccctc.core.coremain.v1_14.CourseCreditLevelType
import com.ccctc.core.coremain.v1_14.CourseGPAApplicabilityCodeType
import com.ccctc.core.coremain.v1_14.GPAType
import com.ccctc.core.coremain.v1_14.impl.GPATypeImpl
import com.ccctc.sector.academicrecord.v1_9.AcademicRecordType
import com.ccctc.sector.academicrecord.v1_9.AcademicSessionType
import com.ccctc.sector.academicrecord.v1_9.AcademicSummaryFType
import com.ccctc.sector.academicrecord.v1_9.CourseType
import com.ccctc.sector.academicrecord.v1_9.impl.AcademicSummaryFTypeImpl
import groovy.transform.CompileStatic

@CompileStatic
abstract class AcademicSummaryService {

    AcademicSummaryFType fromAcademicRecord(AcademicRecordType academicRecord) {
        def result = newAcademicSummary()

        // add in courses not associated with an academic session
        if (academicRecord.courses) {
            for (def c : academicRecord.courses) {
                addCourseCredits(c, result)
            }
        }

        // add in courses from academic sessions
        if (academicRecord.academicSessions) {
            for (def session : academicRecord.academicSessions) {
                if (session.courses) {
                    for (def c : session.courses) {
                        addCourseCredits(c, result)
                    }
                }
            }
        }

        if (result.GPA)
            setGradePointAverage(result.GPA)

        return result
    }

    AcademicSummaryFType fromAcademicSession(AcademicSessionType academicSession) {
        def result = newAcademicSummary()

        if (academicSession.courses) {
            for (def c : academicSession.courses) {
                addCourseCredits(c, result)
            }
        }

        if (result.GPA)
            setGradePointAverage(result.GPA)

        return result
    }

    protected AcademicSummaryFType newAcademicSummary() {
        def summary = new AcademicSummaryFTypeImpl()

        summary.academicSummaryLevel = CourseCreditLevelType.UNDERGRADUATE
        summary.academicSummaryType = AcademicSummaryTypeType.ALL

        summary.GPA = new GPATypeImpl()
        summary.GPA.creditHoursAttempted = new BigDecimal(0)
        summary.GPA.creditHoursEarned = new BigDecimal(0)
        summary.GPA.creditHoursforGPA = new BigDecimal(0)
        summary.GPA.totalQualityPoints = new BigDecimal(0)

        return summary
    }


    protected void addCourseCredits(CourseType course, AcademicSummaryFType summary) {
        if (course.courseCreditEarned) summary.GPA.creditHoursEarned += course.courseCreditEarned
        if (course.courseQualityPointsEarned) summary.GPA.totalQualityPoints += course.courseQualityPointsEarned
        if (course.courseGPAApplicabilityCode == CourseGPAApplicabilityCodeType.APPLICABLE && course.courseCreditValue) {
            summary.GPA.creditHoursAttempted += course.courseCreditValue
            summary.GPA.creditHoursforGPA += course.courseCreditValue
        }
    }

    protected void setGradePointAverage(GPAType gpa) {
        if (gpa.creditHoursforGPA > 0)
            gpa.gradePointAverage = (gpa.totalQualityPoints / gpa.creditHoursforGPA)
        else
            gpa.gradePointAverage = null
    }
}
