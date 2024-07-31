package api.colleague

import api.colleague.util.ColleagueUtils
import com.ccctc.adaptor.exception.InternalServerException
import com.ccctc.adaptor.exception.InvalidRequestException
import com.ccctc.adaptor.model.EnrollmentStatus
import com.ccctc.adaptor.model.PrerequisiteStatus
import com.ccctc.adaptor.model.PrerequisiteStatusEnum
import com.ccctc.adaptor.util.ClassMap
import org.ccctc.colleaguedmiclient.model.ColleagueData
import org.ccctc.colleaguedmiclient.model.Valcode
import org.ccctc.colleaguedmiclient.service.DmiDataService
import org.ccctc.colleaguedmiclient.service.DmiService
import org.springframework.cache.Cache
import org.springframework.cache.support.SimpleValueWrapper
import org.springframework.core.env.Environment
import spock.lang.Specification
import api.colleague.Enrollment as GroovyEnrollment
import com.ccctc.adaptor.model.Enrollment as CAEnrollment

import java.time.LocalDate

class EnrollmentSpec extends Specification {


    def environment = Mock(Environment)
    def dmiService = Mock(DmiService)
    def dmiDataService = Mock(DmiDataService)
    def cache = Mock(Cache)
    def groovyEnrollment = new GroovyEnrollment()

    def misCode = "000"
    def sisTermId = "TERM"
    def sisSectionId = "1234"
    def cccIdType = "CCCID"
    def cccId = "ABC123"

    def grades = [new ColleagueData("1", ["GRD.GRADE" : "A"]),
                  new ColleagueData("2", ["GRD.GRADE" : "B"]),
                  new ColleagueData("3", ["GRD.GRADE" : "C"])]

    def acadCredStatuses = new Valcode("STUDENT.ACAD.CRED.STATUSES", [
            new Valcode.Entry("A", "Add", "1", null),
            new Valcode.Entry("N", "New", "2", null),
            new Valcode.Entry("D", "Drop", "3", null),
            new Valcode.Entry("C", "Cancel", "6", null)
    ])

    def setup() {
        ClassMap services = new ClassMap()
        services.putAll([(DmiService.class): dmiService, (DmiDataService.class): dmiDataService, (Cache.class): cache])
        environment.getProperty("colleague.ccc.id.types") >> cccIdType
        groovyEnrollment.colleagueInit(misCode, environment, services)
    }

    def "getSection - missing params"() {
        when: groovyEnrollment.getSection(null, null, null)
        then: thrown AssertionError

        when: groovyEnrollment.getSection("000", null, null)
        then: thrown AssertionError

        when: groovyEnrollment.getSection("000", "sisTermId", null)
        then: thrown AssertionError
    }

    def "getSection - term not found"() {
        when:
        groovyEnrollment.getSection(misCode, sisTermId, sisSectionId)

        then:
        1 * dmiDataService.selectKeys("TERMS", null, [sisTermId]) >> ([] as String[])
        def e = thrown InvalidRequestException
        e.code == InvalidRequestException.Errors.termNotFound
    }

    def "getSection - multiple sections"() {
        when:
        groovyEnrollment.getSection(misCode, sisTermId, sisSectionId)

        then:
        1 * dmiDataService.selectKeys("TERMS", null, [sisTermId]) >> ([sisTermId] as String[])
        1 * dmiDataService.batchSelect("ST", "COURSE.SECTIONS", _, _) >> [new ColleagueData("1", [:]), new ColleagueData("2", [:])]
        thrown InternalServerException

    }

    def "getSection - section not found"() {
        when:
        groovyEnrollment.getSection(misCode, sisTermId, sisSectionId)

        then:
        1 * dmiDataService.selectKeys("TERMS", null, [sisTermId]) >> ([sisTermId] as String[])
        1 * dmiDataService.batchSelect("ST", "COURSE.SECTIONS", _, _) >> []
        def e = thrown InvalidRequestException
        e.code == InvalidRequestException.Errors.sectionNotFound
    }

    def "getSection - no enrollments"() {
        when:
        def result = groovyEnrollment.getSection(misCode, sisTermId, sisSectionId)

        then:
        1 * dmiDataService.selectKeys("TERMS", null, [sisTermId]) >> ([sisTermId] as String[])
        1 * dmiDataService.batchSelect("ST", "COURSE.SECTIONS", _, _) >> [new ColleagueData("1", [:])]
        result.size() == 0
    }

    def "getSection - minimal data"() {
        setup:
        def scsIds = ["1"]
        def stcIds = ["11"]
        def secStudents = [new ColleagueData("1", ["SEC.STUDENTS" : scsIds as String[]])]
        def studentAcadCred = [new ColleagueData("11", ["STC.TERM" : sisTermId, "STC.STUDENT.COURSE.SEC": "1"])]
        def studentCourseSec = [new ColleagueData("1", ["SCS.COURSE.SECTION": "1"])]
        def sections = [new ColleagueData("1", ["SEC.SYNONYM" : sisSectionId])]

        when:
        def result = groovyEnrollment.getSection(misCode, sisTermId, sisSectionId)

        then:
        1 * dmiDataService.selectKeys("TERMS", null, [sisTermId]) >> ([sisTermId] as String[])
        1 * dmiDataService.batchSelect("ST", "COURSE.SECTIONS", _, _) >> secStudents
        1 * dmiDataService.selectKeys("STUDENT.COURSE.SEC", *_) >> (stcIds as String[])
        1 * dmiDataService.valcode("ST", "STUDENT.ACAD.CRED.STATUSES") >> null
        1 * dmiDataService.batchSelect("ST", "GRADES", *_) >> []
        1 * dmiDataService.batchKeys("ST", "STUDENT.ACAD.CRED", *_) >> studentAcadCred
        1 * dmiDataService.batchKeys("ST", "STUDENT.COURSE.SEC", *_) >> studentCourseSec
        1 * dmiDataService.batchKeys("ST", "COURSE.SECTIONS", *_) >> sections

        result.size() == 1
        result[0].misCode == misCode
        result[0].sisSectionId == sisSectionId
        result[0].sisTermId == sisTermId
    }

    def "getSection - more data"() {
        setup:
        def gradeDate = LocalDate.of(2000, 2, 1)
        def statusDate = LocalDate.of(2000, 1, 1)

        def scsIds = ["1", "2"]
        def stcIds = ["11", "12"]
        def secStudents = [new ColleagueData("1", ["SEC.STUDENTS" : scsIds as String[]])]
        def studentAcadCred = [
                new ColleagueData("11", [
                        "STC.TERM"               : sisTermId,
                        "STC.STUDENT.COURSE.SEC" : "1",
                        "STC.COURSE"             : "C1",
                        "STC.PERSON.ID"          : "123",
                        "STC.STATUS"             : ["D", "A"] as String[],
                        "STC.STATUS.DATE"        : [statusDate, statusDate.minusDays(1)] as LocalDate[],
                        "STC.COURSE.NAME"        : "CRS-1",
                        "STC.VERIFIED.GRADE"     : "1",
                        "STC.VERIFIED.GRADE.DATE": gradeDate]),
                new ColleagueData("12", [
                        "STC.TERM"               : sisTermId,
                        "STC.STUDENT.COURSE.SEC" : "2",
                        "STC.COURSE"             : "C1",
                        "STC.PERSON.ID"          : "456",
                        "STC.STATUS"             : ["C", "A"] as String[],
                        "STC.STATUS.DATE"        : [statusDate, statusDate.minusDays(1)] as LocalDate[],
                        "STC.COURSE.NAME"        : "CRS-1"])
        ]
        def studentCourseSec = [
                new ColleagueData("1", ["SCS.COURSE.SECTION": "1"]),
                new ColleagueData("2", ["SCS.COURSE.SECTION": "1"])
        ]
        def sections = [new ColleagueData("1", ["SEC.SYNONYM" : sisSectionId])]
        def courses = [new ColleagueData("C1", ["CRS.STANDARD.ARTICULATION.NO" : "COURSE 100"])]
        def persons = [
                new ColleagueData("123", [
                        "PERSON.ALT.IDS"     : [cccId] as String[],
                        "PERSON.ALT.ID.TYPES": [cccIdType] as String[]]),
                new ColleagueData("456", [
                        "PERSON.ALT.IDS"     : ["NOT-A-CCC-ID"] as String[],
                        "PERSON.ALT.ID.TYPES": ["OTHER-TYPE"] as String[]]),
        ]

        when:
        def result = groovyEnrollment.getSection(misCode, sisTermId, sisSectionId)

        then:
        1 * dmiDataService.selectKeys("TERMS", null, [sisTermId]) >> ([sisTermId] as String[])
        1 * dmiDataService.batchSelect("ST", "COURSE.SECTIONS", _, _) >> secStudents
        1 * dmiDataService.selectKeys("STUDENT.COURSE.SEC", *_) >> (stcIds as String[])
        1 * dmiDataService.valcode("ST", "STUDENT.ACAD.CRED.STATUSES") >> acadCredStatuses
        1 * dmiDataService.batchSelect("ST", "GRADES", *_) >> grades
        1 * dmiDataService.batchKeys("ST", "STUDENT.ACAD.CRED", *_) >> studentAcadCred
        1 * dmiDataService.batchKeys("ST", "STUDENT.COURSE.SEC", *_) >> studentCourseSec
        1 * dmiDataService.batchKeys("ST", "COURSE.SECTIONS", *_) >> sections
        1 * dmiDataService.batchKeys("ST", "COURSES", *_) >> courses
        1 * dmiDataService.batchKeys("CORE", "PERSON", *_) >> persons

        result.size() == 2

        result[0].cccid == cccId
        result[0].sisCourseId == "CRS-1"
        result[0].sisSectionId == sisSectionId
        result[0].grade == "A"
        result[0].gradeDate == ColleagueUtils.fromLocalDate(gradeDate)
        result[0].enrollmentStatus == EnrollmentStatus.Dropped
        result[0].enrollmentStatusDate == ColleagueUtils.fromLocalDate(statusDate)

        result[1].cccid == null
        result[1].sisCourseId == "CRS-1"
        result[1].sisSectionId == sisSectionId
        result[1].grade == null
        result[1].gradeDate == null
        result[1].enrollmentStatus == EnrollmentStatus.Cancelled
        result[1].enrollmentStatusDate == ColleagueUtils.fromLocalDate(statusDate)
    }


    def "getStudent - missing parms"() {
        when: groovyEnrollment.getStudent(null, null, null, null)
        then: thrown AssertionError
        when: groovyEnrollment.getStudent("000", null, null, null)
        then: thrown AssertionError
    }

    def "getStudent - invalid term"() {
        when:
        groovyEnrollment.getStudent(misCode, sisTermId, cccId, sisSectionId)

        then:
        1 * dmiDataService.selectKeys("TERMS", null, [sisTermId]) >> ([] as String[])
        def e = thrown InvalidRequestException
        e.code == InvalidRequestException.Errors.termNotFound
    }

    def "getStudent - invalid section"() {
        when:
        groovyEnrollment.getStudent(misCode, sisTermId, cccId, sisSectionId)

        then:
        1 * dmiDataService.selectKeys("TERMS", null, [sisTermId]) >> ([sisTermId] as String[])
        1 * dmiDataService.selectKeys("COURSE.SECTIONS", _) >> ([] as String[])
        def e = thrown InvalidRequestException
        e.code == InvalidRequestException.Errors.sectionNotFound
    }

    def "getStudent - invalid student"() {
        when:
        groovyEnrollment.getStudent(misCode, sisTermId, cccId, sisSectionId)

        then:
        1 * dmiDataService.selectKeys("TERMS", null, [sisTermId]) >> ([sisTermId] as String[])
        1 * dmiDataService.selectKeys("COURSE.SECTIONS", _) >> (["1"] as String[])
        1 * dmiDataService.batchSelect("CORE", "PERSON", *_) >> []
        def e = thrown InvalidRequestException
        e.code == InvalidRequestException.Errors.studentNotFound
    }

    def "getStudent - multiple students"() {
        setup:
        def persons = [
                new ColleagueData("123", [
                        "PERSON.ALT.IDS"     : [cccId] as String[],
                        "PERSON.ALT.ID.TYPES": [cccIdType] as String[]]),
                new ColleagueData("123", [
                        "PERSON.ALT.IDS"     : [cccId] as String[],
                        "PERSON.ALT.ID.TYPES": [cccIdType] as String[]])]


        when:
        groovyEnrollment.getStudent(misCode, sisTermId, cccId, sisSectionId)

        then:
        1 * dmiDataService.selectKeys("TERMS", null, [sisTermId]) >> ([sisTermId] as String[])
        1 * dmiDataService.selectKeys("COURSE.SECTIONS", _) >> (["1"] as String[])
        1 * dmiDataService.batchSelect("CORE", "PERSON", *_) >> persons
        def e = thrown InvalidRequestException
        e.code == InvalidRequestException.Errors.multipleResultsFound
    }

    def "getStudent - ok"() {
        setup:
        def stcIds = ["1"]
        def persons = [new ColleagueData("123", [
                        "PERSON.ALT.IDS"     : [cccId] as String[],
                        "PERSON.ALT.ID.TYPES": [cccIdType] as String[]])]

        def studentAcadCred = [
                new ColleagueData("1", [
                        "STC.TERM"               : sisTermId,
                        "STC.STUDENT.COURSE.SEC" : "1",
                        "STC.COURSE"             : "C1",
                        "STC.PERSON.ID"          : "123"])
        ]
        def studentCourseSec = [new ColleagueData("1", ["SCS.COURSE.SECTION": "1"])]
        def sections = [new ColleagueData("1", ["SEC.SYNONYM" : sisSectionId])]
        def courses = [new ColleagueData("C1", ["CRS.STANDARD.ARTICULATION.NO" : "COURSE 100"])]

        when:
        def result = groovyEnrollment.getStudent(misCode, sisTermId, cccId, sisSectionId)

        then:
        1 * cache.get("validateTerm:" + sisTermId) >> new SimpleValueWrapper("X")
        1 * cache.get("validateSection:" + sisTermId + "_" + sisSectionId) >> new SimpleValueWrapper("X")
        1 * dmiDataService.batchSelect("CORE", "PERSON", *_) >> persons
        1 * dmiDataService.selectKeys("STUDENT.ACAD.CRED", _) >> (stcIds as String[])
        1 * dmiDataService.valcode("ST", "STUDENT.ACAD.CRED.STATUSES") >> acadCredStatuses
        1 * dmiDataService.batchSelect("ST", "GRADES", *_) >> grades
        1 * dmiDataService.batchKeys("ST", "STUDENT.ACAD.CRED", *_) >> studentAcadCred
        1 * dmiDataService.batchKeys("ST", "STUDENT.COURSE.SEC", *_) >> studentCourseSec
        1 * dmiDataService.batchKeys("ST", "COURSE.SECTIONS", *_) >> sections
        1 * dmiDataService.batchKeys("ST", "COURSES", *_) >> courses
        1 * dmiDataService.batchKeys("CORE", "PERSON", *_) >> persons

        result.size() == 1
    }

    def "getPrereqStatus - missing params"() {
        when:
        groovyEnrollment.getPrereqStatus(null, null, null, null)

        then:
        thrown AssertionError

        when:
        groovyEnrollment.getPrereqStatus("000", null, null, null)

        then:
        thrown AssertionError

        when:
        groovyEnrollment.getPrereqStatus("000", "sisCourseId", null, null)

        then:
        thrown AssertionError
    }


    def "getPrereqStatus"() {
        setup:
        def result
        def prerequisiteStatus = new PrerequisiteStatus(status: PrerequisiteStatusEnum.Complete, message: "message")
        def exception = new InvalidRequestException(InvalidRequestException.Errors.courseNotFound, "Course not found")

        when:
        result = groovyEnrollment.getPrereqStatus("000", "sisCourseId", null, "cccId")

        then:
        thrown InternalServerException

        when:
        groovyEnrollment.getPrereqStatus("000", "sisCourseId", null, "cccId")

        then:
        thrown InternalServerException
    }

    def "post - missing parameters"() {
        when:
        groovyEnrollment.post(null, null)

        then:
        thrown AssertionError

        when:
        groovyEnrollment.post("000", null)

        then:
        thrown AssertionError
    }

    def "post"() {
        when:
        groovyEnrollment.post("00", new CAEnrollment())

        then:
        thrown InternalServerException
    }

    def "put - missing parameters"() {
        when:
        groovyEnrollment.put(null, null, null, null, null)

        then:
        thrown AssertionError

        when:
        groovyEnrollment.put("000", null, null, null, null)

        then:
        thrown AssertionError

        when:
        groovyEnrollment.put("000", "cccId", null, null, null)

        then:
        thrown AssertionError

        when:
        groovyEnrollment.put("000", "cccId", "sisSectionId", null, null)

        then:
        thrown AssertionError

        when:
        groovyEnrollment.put("000", "cccId", "sisSectionId", "sisTermId", null)

        then:
        thrown AssertionError
    }


    def "put"() {
        when:
        groovyEnrollment.put("000", "cccId", "sisSectionId", "sisTermId", new CAEnrollment())

        then:
        thrown InternalServerException
    }
}