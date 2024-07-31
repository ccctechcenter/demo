package com.ccctc.adaptor.model.mock

import com.ccctc.adaptor.exception.EntityConflictException
import com.ccctc.adaptor.exception.InvalidRequestException
import com.ccctc.adaptor.model.ApplicationStatus
import com.ccctc.adaptor.model.College
import com.ccctc.adaptor.model.Course
import com.ccctc.adaptor.model.Enrollment
import com.ccctc.adaptor.model.EnrollmentStatus
import com.ccctc.adaptor.model.Person
import com.ccctc.adaptor.model.Section
import com.ccctc.adaptor.model.SectionStatus
import com.ccctc.adaptor.model.Student
import com.ccctc.adaptor.model.Term
import com.ccctc.adaptor.util.mock.MockUtils
import spock.lang.Specification

/**
 * Created by zekeo on 7/20/2017.
 */
class EnrollmentDBSpec extends Specification {

    List<Enrollment> all
    Enrollment first

    def misCode = "001"
    def sisTermId = "term1"
    def sisPersonId = "person1"
    def cccId = "cccid1"
    def sisCourseId = "ENGL-101"
    def sisSectionId = "1"


    def collegeDB = new CollegeDB()
    def personDB = new PersonDB(collegeDB)
    def termDB = new TermDB(collegeDB)
    def courseDB = new CourseDB(termDB)
    def sectionDB = new SectionDB(collegeDB, termDB, courseDB)
    def studentDB = new StudentDB(collegeDB, termDB, personDB)
    def enrollmentDB = new EnrollmentDB(collegeDB, termDB, courseDB, sectionDB, studentDB, personDB)

    def setup() {
        // initialize databases
        collegeDB.init()
        personDB.init()
        termDB.init()
        courseDB.init()
        sectionDB.init()
        studentDB.init()
        enrollmentDB.init()

        // seed databases
        collegeDB.add(new College(misCode: misCode, districtMisCode: "000"))
        personDB.add(new Person(misCode: misCode, sisPersonId: sisPersonId, cccid: cccId))
        termDB.add(new Term(misCode: misCode, sisTermId: sisTermId, start: MockUtils.removeTime(new Date()), end: MockUtils.removeTime(new Date() + 1)))
        courseDB.add(new Course(misCode: misCode, sisTermId: sisTermId, sisCourseId: sisCourseId, minimumUnits: 3.0))
        sectionDB.add(new Section(misCode: misCode, sisTermId: sisTermId, sisCourseId: sisCourseId, sisSectionId: sisSectionId, minimumUnits: 3.0))
        studentDB.add(new Student(misCode: misCode, sisPersonId: sisPersonId, sisTermId: sisTermId, applicationStatus: ApplicationStatus.ApplicationAccepted))
        enrollmentDB.add(new Enrollment(misCode: misCode, sisPersonId: sisPersonId, sisTermId: sisTermId, sisCourseId: sisCourseId, sisSectionId: sisSectionId, enrollmentStatus: EnrollmentStatus.Enrolled))

        all = enrollmentDB.getAll()
        first = all[0]
    }

    def "get"() {
        when:
        def r = enrollmentDB.get(first.misCode, first.sisPersonId, first.sisTermId, first.sisSectionId)

        then:
        r.toString() == first.toString()
    }

    def "add"() {
        setup:
        def enrollment = new Enrollment()
        def result

        // missing mis code
        when:
        enrollmentDB.add(enrollment)

        then:
        thrown InvalidRequestException

        // missing term
        when:
        enrollment.misCode = first.misCode
        enrollmentDB.add(enrollment)

        then:
        thrown InvalidRequestException

        // missing section
        when:
        enrollment.sisTermId = first.sisTermId
        enrollmentDB.add(enrollment)

        then:
        thrown InvalidRequestException

        // missing person
        when:
        enrollment.sisSectionId = first.sisSectionId
        enrollmentDB.add(enrollment)

        then:
        thrown InvalidRequestException

        // already exists
        when:
        enrollment.sisPersonId = first.sisPersonId
        enrollmentDB.add(enrollment)

        then:
        thrown EntityConflictException

        // success
        when:
        enrollment = enrollmentDB.delete(first.misCode, first.sisPersonId, first.sisTermId, first.sisSectionId, true)
        result = enrollmentDB.add(enrollment)

        then:
        enrollment.toString() == result.toString()

        // success by ccc id
        when:
        enrollment = enrollmentDB.delete(first.misCode, first.sisPersonId, first.sisTermId, first.sisSectionId, true)
        enrollment.sisPersonId = null
        enrollment.cccid = first.cccid
        result = enrollmentDB.add(enrollment)

        then:
        result.sisPersonId != null
        result.cccid == enrollment.cccid
    }

    def "find - bad sisCourseId"() {
        setup:
        def exception = null

        when:
        try {
            enrollmentDB.find([misCode: first.misCode, sisTermId: first.sisTermId, sisSectionId: first.sisSectionId, sisCourseId: "nope"])
        } catch (InvalidRequestException e) {
            exception = e
        }

        then:
        exception != null
        exception.code == InvalidRequestException.Errors.courseNotFound
    }

    def "find - mismatched sisSectionId to sisCourseId"() {
        setup:
        // get a different but valid course
        def otherSisCourseId = "OTHER-1"
        courseDB.add(new Course(misCode: misCode, sisTermId: sisTermId, sisCourseId: otherSisCourseId))
        def exception = null

        when:
        try {
            enrollmentDB.find([misCode: first.misCode, sisTermId: first.sisTermId, sisSectionId: first.sisSectionId, sisCourseId: otherSisCourseId])
        } catch (InvalidRequestException e) {
            exception = e
        }

        then:
        exception != null
        exception.code == InvalidRequestException.Errors.sectionNotFound
    }


    def "update"() {
        setup:
        Enrollment copy

        // cannot change id
        when:
        copy = enrollmentDB.deepCopy(first)
        copy.sisPersonId = "nope"
        enrollmentDB.update(first.misCode, first.sisPersonId, first.sisTermId, first.sisSectionId, copy)

        then:
        thrown InvalidRequestException

        // update status
        when:
        copy = enrollmentDB.deepCopy(first)
        copy.sisPersonId = first.sisPersonId
        copy.enrollmentStatus = EnrollmentStatus.Cancelled
        def result = enrollmentDB.update(first.misCode, first.sisPersonId, first.sisTermId, first.sisSectionId, copy)

        then:
        result.toString() == copy.toString()

        // cannot change cccid
        when:
        copy = enrollmentDB.deepCopy(first)
        copy.cccid = "nope"
        enrollmentDB.update(first.misCode, first.sisPersonId, first.sisTermId, first.sisSectionId, copy)

        then:
        thrown InvalidRequestException
    }

    def "patch"() {
        when:
        def r = enrollmentDB.patch(first.misCode, first.sisPersonId, first.sisTermId, first.sisSectionId, [enrollmentStatus: EnrollmentStatus.Dropped])

        then:
        r.enrollmentStatus == EnrollmentStatus.Dropped
    }

    def "delete"() {
        when:
        def result = enrollmentDB.delete(first.misCode, first.sisPersonId, first.sisTermId, first.sisSectionId, true)

        then:
        result.toString() == first.toString()
    }

    def "cascadeUpdateFromSection"() {
        when:
        courseDB.patch(first.misCode, first.sisTermId, first.sisCourseId, [minimumUnits: null, maximumUnits: null])
        sectionDB.patch(first.misCode, first.sisTermId, first.sisSectionId, [status: SectionStatus.Cancelled, minimumUnits: null, maximumUnits: null])
        def e = enrollmentDB.get(first.misCode, first.sisPersonId, first.sisTermId, first.sisSectionId)

        then:
        e.enrollmentStatus == EnrollmentStatus.Cancelled
        e.units == null

        when:
        sectionDB.patch(first.misCode, first.sisTermId, first.sisSectionId, [status: SectionStatus.Active, minimumUnits: 1])
        e = enrollmentDB.get(first.misCode, first.sisPersonId, first.sisTermId, first.sisSectionId)

        then:
        e.enrollmentStatus == EnrollmentStatus.Dropped
        e.units == 1

        when:
        sectionDB.patch(first.misCode, first.sisTermId, first.sisSectionId, [status: SectionStatus.Active, minimumUnits: 2])
        e = enrollmentDB.get(first.misCode, first.sisPersonId, first.sisTermId, first.sisSectionId)

        then:
        e.units == 2
    }

    def "cascadeDelete - student"() {
        when:
        studentDB.delete(first.misCode, first.sisTermId, first.sisPersonId, true)
        enrollmentDB.get(first.misCode, first.sisPersonId, first.sisTermId, first.sisSectionId)

        then:
        thrown InvalidRequestException // student not found
    }

    def "cascadeDelete - section"() {
        when:
        sectionDB.delete(first.misCode, first.sisTermId, first.sisSectionId, true)
        enrollmentDB.get(first.misCode, first.sisPersonId, first.sisTermId, first.sisSectionId)

        then:
        thrown InvalidRequestException // section not found
    }

    def "cascade update to course"() {
        setup:
        def newCId = "newCId"

        when:
        def course = courseDB.patch(first.misCode, first.sisTermId, first.sisCourseId, [c_id: newCId])
        def enrollment = enrollmentDB.get(enrollmentDB.getPrimaryKey(first))

        then:
        course.c_id == newCId
        enrollment.c_id == newCId
    }

}