package api.mock

import api.mock.Enrollment as EnrollmentGroovy
import com.ccctc.adaptor.exception.InvalidRequestException
import com.ccctc.adaptor.model.College
import com.ccctc.adaptor.model.Course
import com.ccctc.adaptor.model.Enrollment
import com.ccctc.adaptor.model.EnrollmentStatus
import com.ccctc.adaptor.model.PrerequisiteStatusEnum
import com.ccctc.adaptor.model.Person
import com.ccctc.adaptor.model.Section
import com.ccctc.adaptor.model.Student
import com.ccctc.adaptor.model.Term
import com.ccctc.adaptor.model.mock.CollegeDB
import com.ccctc.adaptor.model.mock.CourseDB
import com.ccctc.adaptor.model.mock.EnrollmentDB
import com.ccctc.adaptor.model.mock.PersonDB
import com.ccctc.adaptor.model.mock.SectionDB
import com.ccctc.adaptor.model.mock.StudentDB
import com.ccctc.adaptor.model.mock.StudentPrereqDB
import com.ccctc.adaptor.model.mock.TermDB
import spock.lang.Specification

class EnrollmentSpec extends Specification {

    def misCode = "001"
    def sisCourseId = "sisCourseId"
    def sisTermId = "sisTermId"
    def sisPersonId = "sisPersonId"
    def sisSectionId = "sisSectionId"
    def cccId = "cccId"
    def termStartDate = new Date("1/1/2001")

    CollegeDB collegeDB
    TermDB termDB
    CourseDB courseDB
    SectionDB sectionDB
    StudentDB studentDB
    PersonDB personDB
    EnrollmentDB enrollmentDB
    StudentPrereqDB studentPrereqDB

    EnrollmentGroovy groovyClass

    def setup() {
        // due to the inability to use mocking for methods in parent class BaseMockDataDB (such as find),
        // create actual instances of databases and seed with data
        collegeDB = new CollegeDB()
        termDB = new TermDB(collegeDB)
        personDB = new PersonDB(collegeDB)
        courseDB = new CourseDB(termDB)
        sectionDB = new SectionDB(collegeDB, termDB, courseDB)
        studentDB = new StudentDB(collegeDB, termDB, personDB)
        enrollmentDB = new EnrollmentDB(collegeDB, termDB, courseDB, sectionDB, studentDB, personDB)
        studentPrereqDB = new StudentPrereqDB(collegeDB, personDB, studentDB, courseDB)

        // seed some base data
        collegeDB.add(new College(misCode: misCode))
        personDB.add(new Person(misCode: misCode, sisPersonId: sisPersonId, cccid: cccId))
        termDB.add(new Term(misCode: misCode, sisTermId: sisTermId, start: termStartDate))
        studentDB.add(new Student(misCode: misCode, sisPersonId: sisPersonId, cccid: cccId, sisTermId: sisTermId))
        courseDB.add(new Course(misCode: misCode, sisTermId: sisTermId, sisCourseId: sisCourseId))
        sectionDB.add(new Section(misCode: misCode, sisTermId: sisTermId, sisCourseId: sisCourseId, sisSectionId: sisSectionId))

        groovyClass = new EnrollmentGroovy(termDB: termDB, courseDB: courseDB, enrollmentDB: enrollmentDB,
                studentPrereqDB: studentPrereqDB)
    }

    def "getPrereqStatus - error no courses"() {
        when:
        courseDB.deleteAll(true)
        groovyClass.getPrereqStatus(misCode, sisCourseId, null, cccId)

        then:
        thrown InvalidRequestException
    }

    def "getPrereqStatus - in progress"() {
        setup:
        def startedDate = termStartDate
        studentPrereqDB.add(new StudentPrereqDB.PrereqInfo(misCode: misCode,
                sisCourseId: sisCourseId, sisPersonId: sisPersonId,
                complete: false, started: startedDate))
        def result1, result2, result3, result4

        when:
        result1 = groovyClass.getPrereqStatus(misCode, sisCourseId, startedDate-1, cccId)
        result2 = groovyClass.getPrereqStatus(misCode, sisCourseId, startedDate, cccId)
        result3 = groovyClass.getPrereqStatus(misCode, sisCourseId, startedDate+1, cccId)
        result4 = groovyClass.getPrereqStatus(misCode, sisCourseId, null, cccId)

        then:
        result1.status == PrerequisiteStatusEnum.Incomplete
        result2.status == PrerequisiteStatusEnum.Pending
        result3.status == PrerequisiteStatusEnum.Pending
        result4.status == PrerequisiteStatusEnum.Pending
    }

    def "getPrereqStatus - complete"() {
        setup:
        def startedDate = termStartDate
        def completedDate = startedDate + 100
        studentPrereqDB.add(new StudentPrereqDB.PrereqInfo(misCode: misCode,
                sisCourseId: sisCourseId, sisPersonId: sisPersonId,
                complete: true, started: startedDate, completed: completedDate))
        def result1, result2, result3, result4, result5, result6, result7

        when:
        result1 = groovyClass.getPrereqStatus(misCode, sisCourseId, startedDate-1, cccId)
        result2 = groovyClass.getPrereqStatus(misCode, sisCourseId, startedDate, cccId)
        result3 = groovyClass.getPrereqStatus(misCode, sisCourseId, startedDate+1, cccId)
        result4 = groovyClass.getPrereqStatus(misCode, sisCourseId, completedDate-1, cccId)
        result5 = groovyClass.getPrereqStatus(misCode, sisCourseId, completedDate, cccId)
        result6 = groovyClass.getPrereqStatus(misCode, sisCourseId, completedDate+1, cccId)
        result7 = groovyClass.getPrereqStatus(misCode, sisCourseId, null, cccId)

        then:
        result1.status == PrerequisiteStatusEnum.Incomplete
        result2.status == PrerequisiteStatusEnum.Pending
        result3.status == PrerequisiteStatusEnum.Pending
        result4.status == PrerequisiteStatusEnum.Pending
        result5.status == PrerequisiteStatusEnum.Complete
        result6.status == PrerequisiteStatusEnum.Complete
        result7.status == PrerequisiteStatusEnum.Complete
    }

    def "getPrereqStatus - check course"() {
        when:
        def result1 = groovyClass.getPrereqStatus(misCode, sisCourseId, null, cccId)

        // add multiple courses / terms to validate sorting and whether it chooses the correct course (one has prereqs the other does not)
        termDB.add(new Term(misCode: misCode, sisTermId: "otherSisTermId", start: termStartDate + 1))
        courseDB.add(new Course(misCode: misCode, sisTermId: "otherSisTermId", sisCourseId: sisCourseId, prerequisites: "this one has prerequisites but is in the future"))

        def result2 = groovyClass.getPrereqStatus(misCode, sisCourseId, termStartDate, cccId) // this date should get the first course (no prereqs)
        def result3 = groovyClass.getPrereqStatus(misCode, sisCourseId, termStartDate+2, cccId) // this date should get the second course (has prereqs)
        def result4 = groovyClass.getPrereqStatus(misCode, sisCourseId, null, cccId) // no date - should get latest course which has prereqs

        then:
        result1.status == PrerequisiteStatusEnum.Complete
        result2.status == PrerequisiteStatusEnum.Complete
        result3.status == PrerequisiteStatusEnum.Incomplete
        result4.status == PrerequisiteStatusEnum.Incomplete
    }

    def "getSection"() {
        setup:
        def result

        when:
        result = groovyClass.getSection(misCode, sisTermId, sisSectionId)

        then:
        result.size() == 0

        when:
        enrollmentDB.add(new Enrollment(misCode: misCode, sisTermId: sisTermId, sisPersonId: sisPersonId, sisSectionId: sisSectionId))
        result = groovyClass.getSection(misCode, sisTermId, sisSectionId)

        then:
        result.size() == 1
    }

    def "getStudent"() {
        setup:
        enrollmentDB.add(new Enrollment(misCode: misCode, sisTermId: sisTermId, sisSectionId: sisSectionId, sisPersonId: sisPersonId))
        enrollmentDB.add(new Enrollment(misCode: misCode, sisTermId: "other-term", sisSectionId: sisSectionId, sisPersonId: sisPersonId))
        enrollmentDB.add(new Enrollment(misCode: misCode, sisTermId: "other-term", sisSectionId: "other-section", sisPersonId: sisPersonId))

        when:
        def result1 = groovyClass.getStudent(misCode, null, cccId, null)
        def result2 = groovyClass.getStudent(misCode, sisTermId, cccId, null)
        def result3 = groovyClass.getStudent(misCode, "other-term", cccId, null)
        def result4 = groovyClass.getStudent(misCode, sisTermId, cccId, sisSectionId)
        def result5 = groovyClass.getStudent(misCode, "other-term", cccId, sisSectionId)
        def result6 = groovyClass.getStudent(misCode, null, cccId, sisSectionId)

        then:
        result1.size() == 3
        result2.size() == 1
        result3.size() == 2
        result4.size() == 1
        result5.size() == 1
        result6.size() == 2
    }

    def "post"() {
        setup:
        def enrollment = new Enrollment(misCode: misCode, sisTermId: sisTermId, sisSectionId: sisSectionId, sisPersonId: sisPersonId, enrollmentStatus: EnrollmentStatus.Enrolled)
        def result

        when:
        result = groovyClass.post(misCode, enrollment)

        then:
        result.misCode == misCode

        when:
        enrollmentDB.deleteAll(true)
        enrollment.misCode = null
        result = groovyClass.post(misCode, enrollment)

        then:
        result.misCode == misCode

        when:
        enrollment.misCode = "mismatched-mis-code"
        groovyClass.post(misCode, enrollment)

        then:
        thrown InvalidRequestException

        when:
        enrollment.enrollmentStatus = null
        groovyClass.post(misCode, enrollment)

        then:
        thrown InvalidRequestException

        when:
        enrollment.enrollmentStatus = EnrollmentStatus.Cancelled
        groovyClass.post(misCode, enrollment)

        then:
        thrown InvalidRequestException
    }

    def "put"() {
        setup:
        def enrollment = new Enrollment(misCode: misCode, sisTermId: sisTermId, sisSectionId: sisSectionId, sisPersonId: sisPersonId, enrollmentStatus: EnrollmentStatus.Enrolled)
        groovyClass.post(misCode, enrollment)

        when:
        enrollment.enrollmentStatus = EnrollmentStatus.Dropped
        def result = groovyClass.put(misCode, cccId, sisSectionId, sisTermId, enrollment)

        then:
        result.enrollmentStatus == EnrollmentStatus.Dropped
    }
}
