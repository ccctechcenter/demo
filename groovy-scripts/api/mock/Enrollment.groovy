package api.mock

import com.ccctc.adaptor.exception.EntityNotFoundException

import com.ccctc.adaptor.exception.InvalidRequestException
import com.ccctc.adaptor.model.Course
import com.ccctc.adaptor.model.EnrollmentStatus
import com.ccctc.adaptor.model.PrerequisiteStatus
import com.ccctc.adaptor.model.PrerequisiteStatusEnum
import com.ccctc.adaptor.model.mock.CourseDB
import com.ccctc.adaptor.model.mock.EnrollmentDB
import com.ccctc.adaptor.model.mock.StudentDB
import com.ccctc.adaptor.model.mock.StudentPrereqDB
import com.ccctc.adaptor.model.mock.TermDB
import org.springframework.core.env.Environment

class Enrollment {
    Environment environment
    // injected using GroovyServiceImpl on creation of class
    private CourseDB courseDB
    private EnrollmentDB enrollmentDB
    private StudentPrereqDB studentPrereqDB
    private StudentDB studentDB
    private TermDB termDB

    def getPrereqStatus(String misCode, String sisCourseId, Date startDate, String cccId) {
        def status = PrerequisiteStatusEnum.Incomplete
        def message = "Prerequisite(s) not started"
        def prereq
        try {
            prereq = studentPrereqDB.get(misCode, "cccid:$cccId", sisCourseId)
        } catch (EntityNotFoundException e) {
            prereq = null
        }

        if (prereq) {
            if (startDate != null) {
                if (prereq.started <= startDate) {
                    if (prereq.complete && prereq.completed <= startDate) {
                        status = PrerequisiteStatusEnum.Complete
                        message = "Prerequisite(s) completed on " + (prereq.completed?.format('MM/dd/yyyy') ?: "(unknown date)")
                    } else {
                        status = PrerequisiteStatusEnum.Pending
                        message = "Prerequisite(s) started on " + (prereq.started?.format('MM/dd/yyyy') ?: "(unknown date)")
                    }
                } else {
                    message = "Prerequisite(s) not started as of " + startDate.format('MM/dd/yyyy') +
                            " (however, they were started on " + (prereq.started?.format('MM/dd/yyyy') ?: "(unknown date)") +
                            (prereq.completed ? " and completed on " + (prereq.completed?.format('MM/dd/yyyy') ?: "(unknown date)") : "") +
                            ")"
                }
            } else if (prereq.complete) {
                status = PrerequisiteStatusEnum.Complete
                message = "Prerequisite(s) completed on " + (prereq.completed?.format('MM/dd/yyyy') ?: "(unknown date)")
            } else {
                status = PrerequisiteStatusEnum.Pending
                message = "Prerequisite(s) started on " + (prereq.started?.format('MM/dd/yyyy') ?: "(unknown date)")
            }
        } else {
            // if no specific pre-req info found, find the course that was active on that date and see if it has
            // a pre-requisite

            def terms = termDB.find([misCode: misCode])

            def getTermStart = { Course c ->
                def t = terms.find { t -> t.sisTermId == c.sisTermId }
                return t?.start
            }

            // sort courses by term start date in descending order (newest course first)
            def courses = courseDB.find([misCode: misCode, sisCourseId: sisCourseId])

            if (!courses)
                throw new InvalidRequestException(InvalidRequestException.Errors.courseNotFound, "Course not found")

            // attempt to find the course with a closest term start date that is before startDate
            // otherwise default to current version of course (first in the list as its sorted in descending order)
            courses.sort { c -> -(getTermStart(c) ? getTermStart(c).getTime() : 0L) }
            def course = (startDate ? courses.find { c -> getTermStart(c) && getTermStart(c) <= startDate } : null) ?: courses.first()

            if (!course.prerequisites && !course.prerequisiteList) {
                status = PrerequisiteStatusEnum.Complete
                message = "Course does not have any prerequisite(s)"
            }
        }

        return new PrerequisiteStatus(status: status, message: message)
    }

    def getSection(String misCode, String sisTermId, String sisSectionId) {
        return enrollmentDB.findSorted([misCode: misCode, sisTermId: sisTermId, sisSectionId: sisSectionId])
    }

    def getStudent(String misCode, String sisTermId, String cccId, String sisSectionId) {
        def map = [misCode: misCode, cccid: cccId]
        if (sisTermId) map.put("sisTermId", sisTermId)
        if (sisSectionId) map.put("sisSectionId", sisSectionId)

        return enrollmentDB.findSorted(map)
    }

    def post(String misCode, com.ccctc.adaptor.model.Enrollment enrollment) {
        if (!enrollment.misCode)
            enrollment.misCode = misCode
        else if (enrollment.misCode != misCode)
            throw new InvalidRequestException("misCode does match enrollment")

        if (!enrollment.enrollmentStatus || enrollment.enrollmentStatus != EnrollmentStatus.Enrolled)
            throw new InvalidRequestException("enrollment statuses other than Enrolled not supported")

        if (!enrollment.enrollmentStatusDate)
            enrollment.enrollmentStatusDate = new Date()

        return enrollmentDB.add(enrollment)
    }

    def put(String misCode, String cccId, String sisSectionId, String sisTermId, com.ccctc.adaptor.model.Enrollment enrollment) {
        return enrollmentDB.update(misCode, "cccid:$cccId", sisTermId, sisSectionId, enrollment)
    }
}
