package com.ccctc.adaptor.model.mock

import com.ccctc.adaptor.exception.InvalidRequestException
import com.ccctc.adaptor.model.College
import com.ccctc.adaptor.model.Course
import com.ccctc.adaptor.model.Term
import spock.lang.Specification

/**
 * Created by zekeo on 7/20/2017.
 */
class CourseDBSpec extends Specification  {

    List<Course> all
    Course first

    def misCode = "001"
    def sisTermId = "TERM"
    def sisCourseId = "ENGL-101"

    def courseWithPrereq = "ENGL-102"

    def coreqCourse1 = "COR-1"
    def coreqCourse2 = "COR-2"

    def collegeDB = new CollegeDB()
    def termDB = new TermDB(collegeDB)
    def courseDB = new CourseDB(termDB)

    def setup() {
        // initialize databases
        collegeDB.init()
        termDB.init()
        courseDB.init()

        // seed databases
        collegeDB.add(new College(misCode: misCode, districtMisCode: "000"))
        termDB.add(new Term(misCode: misCode, sisTermId: sisTermId, start: new Date(2000, 1, 1), end: new Date(2000, 6, 30)))

        first = courseDB.add(new Course(misCode: misCode, sisTermId: sisTermId, sisCourseId: sisCourseId))

        // course with pre-req
        courseDB.add(new Course(misCode: misCode, sisTermId: sisTermId, sisCourseId: courseWithPrereq, prerequisiteList:
                [new Course(misCode: misCode, sisCourseId: sisCourseId)]))

        // courses that are co-requisites
        courseDB.add(new Course(misCode: misCode, sisTermId: sisTermId, sisCourseId: coreqCourse1))
        courseDB.add(new Course(misCode: misCode, sisTermId: sisTermId, sisCourseId: coreqCourse2))

        // have to patch in co-requisites as they validate the course
        courseDB.patch(misCode, sisTermId, coreqCourse1, [
                corequisiteList: [new Course(misCode: misCode, sisCourseId: coreqCourse2)]
        ])

        courseDB.patch(misCode, sisTermId, coreqCourse2, [
                corequisiteList: [new Course(misCode: misCode, sisCourseId: coreqCourse1)]
        ])

        all = courseDB.getAll()
    }

    def "get"() {
        when:
        def r = courseDB.get(first.misCode, first.sisTermId, first.sisCourseId)

        then:
        r.toString() == first.toString()
    }

    def "add"() {
        setup:
        def course = new Course()
        def term = termDB.get(first.misCode, first.sisTermId)

        // missing mis code
        when:
        courseDB.add(course)

        then:
        thrown InvalidRequestException

        // missing term
        when:
        course.misCode = first.misCode
        courseDB.add(course)

        then:
        thrown InvalidRequestException

        // missing course
        when:
        course.sisTermId = first.sisTermId
        courseDB.add(course)

        then:
        thrown InvalidRequestException

        // bad term
        when:
        course.sisTermId = "nope"
        courseDB.add(course)

        then:
        thrown InvalidRequestException

        // bad start date
        when:
        course.sisTermId = first.sisTermId
        course.sisCourseId = "test-course"
        course.start = term.end + 1
        course.end = term.end + 2
        courseDB.add(course)

        then:
        thrown InvalidRequestException

        // bad end date
        when:
        course.start = term.start - 2
        course.end = term.start - 1
        courseDB.add(course)

        then:
        thrown InvalidRequestException

        // end date before start date
        when:
        course.start = term.start +5
        course.end = term.start + 4
        courseDB.add(course)

        then:
        thrown InvalidRequestException

        // success (clear start/end date, inherit from term)
        when:
        course.start = null
        course.end = null
        def result = courseDB.add(course)
        // start and end date were populated from term, so update those here so our toString() test works below
        course.start = result.start
        course.end = result.end

        then:
        result.toString() == course.toString()
    }

    def "update"() {
        setup:
        def copy = courseDB.deepCopy(first)
        def result

        when:
        copy.sisCourseId = "nope"
        courseDB.update(first.misCode, first.sisTermId, first.sisCourseId, copy)

        then:
        thrown InvalidRequestException

        when:
        copy.sisCourseId = first.sisCourseId
        copy.c_id = "TEST 1"
        result = courseDB.update(first.misCode, first.sisTermId, first.sisCourseId, copy)

        then:
        result.toString() == copy.toString()
    }

    def "patch"() {
        when:
        def r = courseDB.patch(first.misCode, first.sisTermId, first.sisCourseId, [description: "patched"])

        then:
        r.description == "patched"
    }

    def "delete"() {
        when:
        def result = courseDB.delete(first.misCode, first.sisTermId, first.sisCourseId, true)

        then:
        result.toString() == first.toString()
    }

    def "copy"() {
        when:
        termDB.add(new Term(misCode: first.misCode, sisTermId: "sisTermId", start: new Date(), end: new Date()+1))
        def result = courseDB.copy(first.misCode, first.sisTermId, first.sisCourseId, "sisTermId")

        then:
        result.sisTermId == "sisTermId"
    }

    def "coreq tests"() {
        setup:
        def course = first

        // invalid course
        when:
        course.setCorequisiteList([new Course()])
        courseDB.update(course.misCode, course.sisTermId, course.sisCourseId, course)

        then:
        thrown InvalidRequestException

        // invalid mis code
        when:
        course.setCorequisiteList([new Course(misCode: "bad")])
        courseDB.update(course.misCode, course.sisTermId, course.sisCourseId, course)

        then:
        thrown InvalidRequestException

        // valid course
        when:
        def newCourse = courseDB.add(new Course(misCode: first.misCode, sisCourseId: "TEST-1", sisTermId: first.sisTermId))
        course.setCorequisiteList([newCourse])
        def result = courseDB.update(course.misCode, course.sisTermId, course.sisCourseId, course)

        then:
        result.corequisiteList.size() == 1
        result.corequisiteList[0].toString() == newCourse.toString()

        // valid course, but only pass sisCourseId
        when:
        course.setCorequisiteList([new Course(sisCourseId: newCourse.sisCourseId)])
        result = courseDB.update(course.misCode, course.sisTermId, course.sisCourseId, course)

        then:
        result.corequisiteList.size() == 1
        result.corequisiteList[0].toString() == newCourse.toString()
    }

    def "validate"() {
        when:
        courseDB.validate(misCode, "bad-course")

        then:
        thrown InvalidRequestException

        when:
        courseDB.validate(misCode, sisTermId, "bad-course")

        then:
        thrown InvalidRequestException

        when:
        def result = courseDB.validate(misCode, sisCourseId)

        then:
        result.size() > 0

        when:
        def result2 = courseDB.validate(first.misCode, first.sisTermId, first.sisCourseId)

        then:
        result2.toString() == first.toString()
    }

    def "cascadeDelete"() {
        when:
        termDB.delete(misCode, sisTermId, true)
        courseDB.get(misCode, sisTermId, sisCourseId)

        then:
        thrown InvalidRequestException // invalid sisTermId

    }
}
