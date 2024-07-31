package api.colleague.transcript.model

import com.ccctc.sector.academicrecord.v1_9.AcademicSessionType
import com.ccctc.sector.academicrecord.v1_9.CourseType

class AcademicSessionsResults {

    /**
     * Academic Sessions, indexed by term ID
     */
    Map<String, AcademicSessionType> academicSessions = [:]

    /**
     * Coursework not associated with an Academic Session
     */
    List<CourseType> otherCourses = []

}
