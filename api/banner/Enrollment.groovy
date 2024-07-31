/**
 * Created by Rasul on 2/18/16.
 * This class is based on Banner Student 8.6.6
 * The post() method is heavily reliant on the Banner version and any other
 *    versions of Banner Student would need to be examined for differences in
 *    the registration creation procedure which is installed under BANINST1 by the college.
 */
package api.banner

import com.ccctc.adaptor.exception.InvalidRequestException
import com.ccctc.adaptor.model.EnrollmentStatus
import com.ccctc.adaptor.model.PrerequisiteStatus
import com.ccctc.adaptor.model.PrerequisiteStatusEnum
import com.ccctc.adaptor.util.MisEnvironment
import groovy.sql.Sql
import groovy.util.logging.Slf4j
import org.springframework.core.env.Environment

@Slf4j
class Enrollment {

    Environment environment

    def getSection(String misCode, String sisTermId, String sisSectionId) {

        Sql sql = BannerConnection.getSession(environment, misCode)
        def results
        List grades, enrollments
        try {
            def query = MisEnvironment.getProperty(environment, misCode, "banner.section.getQuery")
            if (!sql.firstRow(query, [sisTermId: sisTermId, sisSectionId: sisSectionId]))
                throw new InvalidRequestException(InvalidRequestException.Errors.sectionNotFound, "Section not found")
            query = MisEnvironment.getProperty(environment, misCode, "banner.enrollment.sectionGrades.getQuery")
            grades = sql.rows(query, [sisTermId: sisTermId, sisSectionId: sisSectionId])
            query = MisEnvironment.getProperty(environment, misCode, "banner.enrollment.section.getQuery")
            enrollments = sql.rows(query, [sisTermId: sisTermId, sisSectionId: sisSectionId])
            results = buildEnrollments(misCode, enrollments, grades, sisTermId, sql)
        } finally {
            sql.close()
        }
        return results
    }

    def getStudent(String misCode, String sisTermId, String cccid, String sisSectionId) {

        Sql sql = BannerConnection.getSession(environment, misCode)

        def student, results
        List grades, enrollments
        try {
            def query = MisEnvironment.getProperty(environment, misCode, "banner.cccid.getQuery")
            student = sql.firstRow(query, [cccid: cccid])
            if (!student)
                throw new InvalidRequestException(InvalidRequestException.Errors.studentNotFound, "Student cccId was not found")
            if (sisSectionId) {
                query = MisEnvironment.getProperty(environment, misCode, "banner.section.getQuery")
                if (!sql.firstRow(query, [sisTermId: sisTermId, sisSectionId: sisSectionId]))
                    throw new InvalidRequestException(InvalidRequestException.Errors.sectionNotFound, "Section not found")
            }
            query = MisEnvironment.getProperty(environment, misCode, "banner.enrollment.grades.getQuery")
            grades = sql.rows(query, [pidm: student.pidm, sisTermId: sisTermId, sisSectionId: sisSectionId])
            query = MisEnvironment.getProperty(environment, misCode, "banner.enrollment.getQuery")
            enrollments = sql.rows(query, [pidm: student.pidm, sisTermId: sisTermId, sisSectionId: sisSectionId])
            results = buildEnrollments(misCode, enrollments, grades, sisTermId, sql)
        } finally {
            sql.close()
        }
        return results
    }

    def getCid(Sql sql, def enrollment, def sisTermId, String misCode) {
        def query = MisEnvironment.getProperty(environment, misCode, "banner.course.cid.getQuery")
        def cid = sql.firstRow(query, [sisTermId: sisTermId, subject: enrollment.subject, number: enrollment.number])
        return cid?.cid
    }

    def buildEnrollments(String misCode, List enrollments, List grades, def sisTermId, Sql sql) {

        def enrollmentList = []

        enrollments.each { enrollment ->
            def current = new com.ccctc.adaptor.model.Enrollment.Builder()
            def grade = grades.find {
                it.sisSectionId == enrollment.sisSectionId &&
                        it.sisTermId == enrollment.sisTermId &&
                        it.pidm == enrollment.pidm
            }
            current.cccid(enrollment.cccid)
                    .sisPersonId(enrollment.sisPersonId)
                    .sisTermId(enrollment.sisTermId)
                    .sisSectionId(enrollment.sisSectionId)
                    .enrollmentStatus(getEnrollmentStatus(enrollment.enrollmentStatus))
                    .enrollmentStatusDate(enrollment.enrollmentStatusDate)
                    .units(enrollment.units)
                    .passNoPass(getPassNoPass(enrollment.gradingMethod, misCode))
                    .audit(MisEnvironment.checkPropertyMatch(environment, misCode, "banner.enrollment.auditStatus", enrollment.auditStatus))
                    .grade(grade?.grade)
                    .gradeDate(grade?.gradeDate)
                    .c_id(getCid(sql, enrollment, sisTermId, misCode))
                    .sisCourseId(enrollment.sisCourseId)
                    .title(enrollment.title)
                    .lastDateAttended(enrollment.lastDateAttended)
            enrollmentList << current.build()
        }
        return enrollmentList
    }

    def getPassNoPass(def gradingMethod, String misCode) {
        if (MisEnvironment.checkPropertyMatch(environment, misCode, "banner.course.gradingMethod.PassNoPassOptional", gradingMethod)) {
            return true
        } else if (MisEnvironment.checkPropertyMatch(environment, misCode, "banner.course.gradingMethod.PassNoPassOnly", gradingMethod)) {
            return true
        } else {
            return false
        }
    }

    def post(String misCode, com.ccctc.adaptor.model.Enrollment enrollment) {
        Sql sql = BannerConnection.getSession(environment, misCode)
        try {
            def rsts_code = MisEnvironment.getProperty(environment, misCode, "banner.enrollment.enrollStatus")
            log.info('Enrolling student: ' + enrollment.cccid + ' in Term:' + enrollment.sisTermId +
                    ' sisSectionId:' + enrollment.sisSectionId + ' with status code:' + rsts_code)
            createEnrollment(misCode, enrollment, rsts_code, sql)
        } finally {
            sql.close()
        }
        return getStudent(misCode, enrollment.sisTermId, enrollment.cccid, enrollment.sisSectionId)[0]
    }

    def put(String misCode, String cccid, String sisSectionId, String sisTermId, com.ccctc.adaptor.model.Enrollment enrollment) {
        Sql sql = BannerConnection.getSession(environment, misCode)
        try {
            def rsts_code = MisEnvironment.getProperty(environment, misCode, "banner.enrollment.dropStatus")
            if (enrollment.sisSectionId != sisSectionId || enrollment.sisTermId != sisTermId || enrollment.cccid != cccid) {
                throw new InvalidRequestException(InvalidRequestException.Errors.generalEnrollmentError, "You may not update the sisSectionId, sisTermId, or cccid of an enrollment")
            }
            if (enrollment.enrollmentStatus == EnrollmentStatus.Dropped) {
                log.info('Dropping student: ' + cccid + ' in Term:' + sisTermId +
                        ' sisSectionId:' + sisSectionId + ' with status code:' + rsts_code)
                createEnrollment(misCode, enrollment, rsts_code, sql)
            } else {
                throw new InvalidRequestException(InvalidRequestException.Errors.generalEnrollmentError, "Invalid enrollment status provided on update, valid status is: Dropped")
            }
        } finally {
            sql.close()
        }
        return getStudent(misCode, enrollment.sisTermId, enrollment.cccid, enrollment.sisSectionId)[0]
    }

    def getPrereqStatus(String misCode, String sisCourseId, Date start, String cccId) {
        Sql sql = BannerConnection.getSession(environment, misCode)
        def courseId = sisCourseId.split('-')
        if (courseId.size() < 2) {
            throw new InvalidRequestException(InvalidRequestException.Errors.courseNotFound, "Invalid sisCourseId provided")
        }
        def query = MisEnvironment.getProperty(environment, misCode, "banner.cccid.getQuery")
        def student = sql.firstRow(query, [cccid: cccId])
        if (!student)
            throw new InvalidRequestException(InvalidRequestException.Errors.studentNotFound, "Student cccId was not found")
        query = MisEnvironment.getProperty(environment, misCode, "banner.term.byDate.getQuery")
        def term = sql.firstRow(query, [start: start?.toTimestamp()])
        query = MisEnvironment.getProperty(environment, misCode, "banner.course.getQuery")
        def course = sql.firstRow(query, [sisTermId: term.sisTermId, subject: courseId[0], number: courseId[1]])
        if (!course) {
            throw new InvalidRequestException(InvalidRequestException.Errors.courseNotFound, "Course does not exist")
        }
        if (course.prereqMethod == 'B') {
            query = MisEnvironment.getProperty(environment, misCode, "banner.course.prerequisites.getQuery")
            def prerequisites = sql.rows(query, [sisTermId: term.sisTermId, subject: course.subject, number: course.number])
            if (!prerequisites || prerequisites.size() == 0) {
                def prereqStatus = new PrerequisiteStatus.Builder()
                prereqStatus.status(PrerequisiteStatusEnum.Complete)
                return prereqStatus.build()
            }
            def failedMessage = getFullPrerequisiteMessage(prerequisites)
            query = MisEnvironment.getProperty(environment, misCode, "banner.enrollment.grades.getQuery")
            def history = sql.rows(query, [pidm: student.pidm])
            query = MisEnvironment.getProperty(environment, misCode, "banner.enrollment.transfers.getQuery")
            def transfers = sql.rows(query, [pidm: student.pidm])
            def tests = []
            if (prerequisites.find { it.test }) {
                query = MisEnvironment.getProperty(environment, misCode, "banner.student.test.getQuery")
                tests = sql.rows(query, [pidm: student.pidm])
            }
            query = MisEnvironment.getProperty(environment, misCode, "banner.enrollment.inprogress.getQuery")
            def enrollments = sql.rows(query, [pidm: student.pidm, start: start?.toTimestamp()])
            def prereqStatus = new PrerequisiteStatus.Builder()
            def result = checkPrerequisites(prerequisites, tests, history, transfers, enrollments)
            prereqStatus.status(result.status)
            prereqStatus.message(result.message ?: result.status == PrerequisiteStatusEnum.Incomplete ? failedMessage : null)
            return prereqStatus.build()
        }
        else {
            def prereqStatus = new PrerequisiteStatus.Builder()
            prereqStatus.status(PrerequisiteStatusEnum.Incomplete)
            prereqStatus.message("Pre-requisite status could not be determined due to using Banner CAPP or DegreeWorks")
            return prereqStatus.build()
        }
    }

// Recursive method that short circuits when a prerequisite fails.
    def checkPrerequisites(List prerequisites, List tests, List courses, List transfers, List enrollments) {
        def status = [:]
        def connector
        def currentPrereq = prerequisites?.get(0)
        if (currentPrereq.leftParen) { //When we encounter a grouping, call managePrereqList to split the grouping
            // into a seperate call/result of checkPrerequisites.
            status = managePrereqList(prerequisites, tests, courses, transfers, enrollments)
        } else {  // This is a prerequisite to be checked, this returns null if there was no prerequisite on the row.
            status = checkPrerequisite(currentPrereq, tests, courses, transfers, enrollments)
            log.info("Result of check: " + status?.status + " Message: " + status?.message)
            prerequisites.remove(currentPrereq)
            if (!status && prerequisites.size()) {
                //Sometimes a prerequisite row has no check and comes at the beginning
                // of the rowset.  Have to avoid returning a null status when there are
                // more prerequisites to check at this level.
                status = checkPrerequisites(prerequisites, tests, courses, transfers, enrollments)
            }
        }
        for (def i = 0; i < prerequisites.size() && !connector; i++) {
            //Sometimes the connector for this set of prerequisites
            // is not on the next row when this is a grouping.
            // We loop until we find the connector for this result.
            connector = prerequisites.get(0).connector
        }
        if (!connector) {
            //If we looped through all the remaining rows and didn't find a connector, send the status back
            // to the caller method so the caller can determine if we short-circuit or continue.
            // This also ensures we exit when we run out of prerequisites.
            return status
        }
        if (connector == 'O') {
            if (status.status == PrerequisiteStatusEnum.Complete) { //Done checking this level of pre-requisites.
                return status
            } else if (status.status == PrerequisiteStatusEnum.Pending) {
                //Need to continue checking, but recall that we did find a
                // pending result.
                def newStatus = checkPrerequisites(prerequisites, tests, courses, transfers, enrollments)
                if (newStatus.status == PrerequisiteStatusEnum.Complete) //A better result was found.
                    return status
                else
                    return status  //Return pending as no complete result was found.
            } else { // Keep looking for a complete or pending status.
                def newStatus = checkPrerequisites(prerequisites, tests, courses, transfers, enrollments)
                return newStatus ?: status  //Return an incomplete status rather than a null if the next record
                // had no prerequisite to check.
            }
        } else if (connector == 'A') {
            if (status.status == PrerequisiteStatusEnum.Complete) //Still have met all prerequisites, continue checks.
                return checkPrerequisites(prerequisites, tests, courses, transfers, enrollments)
            else if (status.status == PrerequisiteStatusEnum.Pending) {
                //Ensure a pending status is not overwritten by a Complete.
                def newStatus = checkPrerequisites(prerequisites, tests, courses, transfers, enrollments)
                if (newStatus.status != PrerequisiteStatusEnum.Incomplete) //Since one check was pending as long as the following
                // checks are not incomplete, the status is pending.
                    return status
                else
                    return newStatus
            } else
                return status  //Short circuit, one of the prerequisites was not met.
        }
    }

// Helper method for checkPrerequisites to handle parenthesis in prerequisites.
    def managePrereqList(List prerequisites, List tests, List courses, List transfers, List enrollments) {
        def subList = []
        def currPreq = prerequisites.get(0)
        prerequisites.remove(currPreq)
        currPreq.leftParen = null
        subList << currPreq
        def leftCount = 1
        def count = prerequisites.size() //Set count here as we are removing from prerequisites below.
        for (def i = 1; i <= count; i++) {
            //Slice out the prerequisites in this grouping include sub groups in the slice.
            currPreq = prerequisites.get(0)
            if (currPreq.leftParen)
                leftCount++
            if (currPreq.rightParen)
                leftCount--
            subList << currPreq
            prerequisites.remove(currPreq)
            if (leftCount == 0) //Send the slice as a new check to checkPrerequisites.
                return checkPrerequisites(subList, tests, courses, transfers, enrollments)
        }
    }

// Method that does the actual checking of a individual prerequisite.
    def checkPrerequisite(Map prerequisite, List tests, List courses, List transfers, List enrollments) {
        if (prerequisite.test) {
            log.info("Processing test prerequisite: " + prerequisite.toString())
            for (Map row : tests) {
                log.debug("Current test being checked:" + row.toString())
                if (row.test == prerequisite.test && row.score >= prerequisite.testScore) {
                    return [status: PrerequisiteStatusEnum.Complete]
                }
            }
            return [status: PrerequisiteStatusEnum.Incomplete]
        } else if (prerequisite.preSubject) {
            log.info "Processing course prerequisite: " + prerequisite.toString()
            for (Map course : courses) { //Check grade records.
                log.debug ("Current course being checked:" + course.toString())
                if (prerequisite.preSubject == course.subject && prerequisite.preNumber == course.number &&
                        (prerequisite.level ? prerequisite.level == course.level : true)) {
                    if (prerequisite.score) {
                        if (prerequisite.score <= course.score)
                            return [status: PrerequisiteStatusEnum.Complete]
                    } else
                        return [status: PrerequisiteStatusEnum.Complete]
                }
            }
            for (Map course : transfers) { //Check transfer records.
                log.debug ("Current transfer being checked:" + course.toString())
                if (prerequisite.preSubject == course.subject && prerequisite.preNumber == course.number &&
                        (prerequisite.level ? prerequisite.level == course.level : true)) {
                    if (prerequisite.score) {
                        if (prerequisite.score <= course.score)
                            return [status: PrerequisiteStatusEnum.Complete]
                    } else
                        return [status: PrerequisiteStatusEnum.Complete]
                }
            }
            for (Map course : enrollments) { //Check current ungrade rolled enrollments.
                log.debug ("Current enrollment being checked:" + course.toString())
                if (prerequisite.preSubject == course.subject && prerequisite.preNumber == course.number &&
                        (prerequisite.level ? prerequisite.level == course.level : true)) {
                    return [status: PrerequisiteStatusEnum.Pending, message: getPreqPendingMessage(course)]
                }
            }
            return [status: PrerequisiteStatusEnum.Incomplete]
        }
        return null  //Some rows have no prerequisite defined on them so return null.
    }

    def getPreqPendingMessage(def prerequisite) {
        return "Prerequisite pending completion of: " + prerequisite.subject + "-" + prerequisite.number + ".  "
    }

    def getFullPrerequisiteMessage(List prerequisites) {
        def prerequisiteMessage = ''
        prerequisites.each { preq ->
            prerequisiteMessage += getConnector(preq.connector) + ' ' + (preq.leftParen ?: "") + (preq.preSubject ? (preq.preSubject + '-' + preq.preNumber + ' with ' + preq.preGradeMin + ' grade minimum') :
                    (preq.testDescription + ' with ' + preq.testScore + ' score minimum')) + (preq.rightParen ?: "") + '\\n'
        }
        return prerequisiteMessage
    }

    def getConnector(String connector) {
        switch (connector) {
            case 'O': return "Or"
            case 'A': return "And"
            default: return ''
        }
    }

    def getEnrollmentStatus(String enrollmentStatus) {
        switch (enrollmentStatus) {
            case 'R':
                return EnrollmentStatus.Enrolled
            case 'D':
            case 'W':
                return EnrollmentStatus.Dropped
            default:
                return null
        }

    }

    def createEnrollment(String misCode, com.ccctc.adaptor.model.Enrollment enrollment, def rsts_code, Sql sql) {
        def query = MisEnvironment.getProperty(environment, misCode, "banner.cccid.getQuery")
        def pidmRecord = sql.firstRow(query, [cccid: enrollment.cccid])
        if (!pidmRecord)
            throw new InvalidRequestException(InvalidRequestException.Errors.studentNotFound, "No such student found with the cccId provided.")
        def pidm = pidmRecord.pidm
        String plsql = """
   DECLARE

-- Our global variables
      term                 VARCHAR2(6);  -- Passed to p_altpin
      rsts                 STVRSTS.STVRSTS_CODE%TYPE;  -- Populates p_regs arrays
      pidm                 NUMBER;       -- Populates p_regs arrays
      crn                  VARCHAR2(5);  -- Populates p_regs arrays

    BEGIN
        term := ?;
        pidm := ?;
        rsts := ?;
        crn  := ?;
        BZCCREG.ADD_OR_DROP(term, rsts, pidm, crn );

    END;
        """
        try {
            sql.call(plsql, [enrollment.sisTermId, pidm, rsts_code, enrollment.sisSectionId])
        } catch (Exception e) {
            def message = e.getMessage().substring(e.getMessage().indexOf(':') + 2, e.getMessage().indexOf('\n'))
            def code = message.equals("DUPLICATE <ACRONYM title = \"Course Reference Number\">CRN</ACRONYM>") ?
                    InvalidRequestException.Errors.alreadyEnrolled : InvalidRequestException.Errors.generalEnrollmentError
            throw new InvalidRequestException(code, message)
        }
    }

}