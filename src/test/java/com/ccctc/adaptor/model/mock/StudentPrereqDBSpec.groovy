package com.ccctc.adaptor.model.mock

import com.ccctc.adaptor.exception.InvalidRequestException
import com.ccctc.adaptor.model.ApplicationStatus
import com.ccctc.adaptor.model.College
import com.ccctc.adaptor.model.Course
import com.ccctc.adaptor.model.Person
import com.ccctc.adaptor.model.Student
import com.ccctc.adaptor.model.Term
import com.ccctc.adaptor.util.mock.MockUtils
import spock.lang.Specification

/**
 * Created by zekeo on 7/20/2017.
 */
class StudentPrereqDBSpec extends Specification  {
    List<StudentPrereqDB.PrereqInfo> all
    StudentPrereqDB.PrereqInfo first
    def misCode = "001"
    def sisTermId = "term1"
    def sisPersonId = "person1"
    def cccId = "cccid1"
    def sisCourseId = "ENGL-101"

    def collegeDB = new CollegeDB()
    def termDB = new TermDB(collegeDB)
    def courseDB = new CourseDB(termDB)
    def personDB = new PersonDB(collegeDB)
    def studentDB = new StudentDB(collegeDB, termDB, personDB)
    def studentPrereqDB = new StudentPrereqDB(collegeDB, personDB, studentDB, courseDB)

    def setup() {
        // initialize databases
        collegeDB.init()
        termDB.init()
        courseDB.init()
        personDB.init()
        studentDB.init()
        studentPrereqDB.init()

        // seed databases
        collegeDB.add(new College(misCode: misCode, districtMisCode: "000"))
        termDB.add(new Term(misCode: misCode, sisTermId: sisTermId, start: MockUtils.removeTime(new Date()), end: MockUtils.removeTime(new Date() + 1)))
        personDB.add(new Person(misCode: misCode, sisPersonId: sisPersonId, cccid: cccId))
        studentDB.add(new Student(misCode: misCode, sisPersonId: sisPersonId, sisTermId: sisTermId, applicationStatus: ApplicationStatus.ApplicationAccepted))
        courseDB.add(new Course(misCode: misCode, sisTermId: sisTermId, sisCourseId: sisCourseId))
        studentPrereqDB.add(new StudentPrereqDB.PrereqInfo(misCode: misCode, sisPersonId: sisPersonId, sisCourseId: sisCourseId, completed: new Date()))

        all = studentPrereqDB.getAll()
        first = all[0]
    }

    def "get"() {
        setup:
        def result

        // by sisPersonId
        when:
        result = studentPrereqDB.get(first.misCode, first.sisPersonId, first.sisCourseId)

        then:
        result.toString() == first.toString()
    }

    def "add"() {
        setup:
        studentPrereqDB.deleteAll(true)
        def term = termDB.find([misCode: first.misCode]).first()
        def prereq = new StudentPrereqDB.PrereqInfo()

        // missing mis code
        when:
        studentPrereqDB.add(prereq)

        then:
        thrown InvalidRequestException

        // missing student
        when:
        prereq.misCode = first.misCode
        studentPrereqDB.add(prereq)

        then:
        thrown InvalidRequestException


        // missing course
        when:
        prereq.sisPersonId = "test"
        studentPrereqDB.add(prereq)

        then:
        thrown InvalidRequestException

        // invalid person
        when:
        prereq.sisCourseId = first.sisCourseId
        studentPrereqDB.add(prereq)

        then:
        thrown InvalidRequestException

        // invalid student
        when:
        personDB.add(new Person(misCode: first.misCode, sisPersonId: "test"))
        studentPrereqDB.add(prereq)

        then:
        thrown InvalidRequestException

        // success
        when:
        studentDB.add(new Student(misCode: first.misCode, sisTermId: term.sisTermId, sisPersonId: "test"))
        def result = studentPrereqDB.add(prereq)

        then:
        result.toString() == prereq.toString()
    }

    def "update"() {
        setup:
        def copy = studentPrereqDB.deepCopy(first)
        def result

        when:
        copy.started = new Date()
        copy.completed = null
        result = studentPrereqDB.update(first.misCode, first.sisPersonId, first.sisCourseId, copy)

        then:
        result.started == copy.started
        result.completed == null
        result.complete == false
    }

    def "patch"() {
        when:
        def d = new Date()
        def r = studentPrereqDB.patch(first.misCode, first.sisPersonId, first.sisCourseId, [completed: d])

        then:
        r.completed == d
    }

    def "delete"() {
        when:
        def result = studentPrereqDB.delete(first.misCode, first.sisPersonId, first.sisCourseId, true)

        then:
        result.toString() == first.toString()
    }

    def "cascade CCC ID change from person"() {
        setup:
        def newCccId = "newCccId"

        when:
        def person = personDB.patch(first.misCode, first.sisPersonId, [cccid: newCccId])
        def preReq = studentPrereqDB.get(studentPrereqDB.getPrimaryKey(first))

        then:
        person.cccid == newCccId
        preReq.cccid == newCccId
    }

    def "cascade delete from course"() {
        setup:
        def courses = courseDB.find([misCode: misCode, sisCourseId: sisCourseId])
        courses.each { i -> courseDB.delete(courseDB.getPrimaryKey(i), true)}

        when:
        studentPrereqDB.get(studentPrereqDB.getPrimaryKey(first))

        then:
        thrown InvalidRequestException // invalid course
    }

    def "cascade delete from student (by way of person)"() {
        setup:
        personDB.delete(first.misCode, first.sisPersonId, true)

        when:
        studentPrereqDB.get(studentPrereqDB.getPrimaryKey(first))

        then:
        thrown InvalidRequestException // invalid person
    }
}
