package api.colleague

import api.colleague.Course as GroovyCourse
import api.colleague.util.ColleagueUtils
import com.ccctc.adaptor.exception.EntityNotFoundException
import com.ccctc.adaptor.exception.InternalServerException
import com.ccctc.adaptor.model.CourseStatus
import com.ccctc.adaptor.model.CreditStatus
import com.ccctc.adaptor.model.GradingMethod
import com.ccctc.adaptor.model.InstructionalMethod
import com.ccctc.adaptor.model.TransferStatus
import com.ccctc.adaptor.util.ClassMap
import org.ccctc.colleaguedmiclient.model.ColleagueData
import org.ccctc.colleaguedmiclient.model.ElfTranslateTable
import org.ccctc.colleaguedmiclient.model.Valcode
import org.ccctc.colleaguedmiclient.service.DmiCTXService
import org.ccctc.colleaguedmiclient.service.DmiDataService
import org.ccctc.colleaguedmiclient.service.DmiService
import org.springframework.cache.Cache
import org.springframework.core.env.Environment
import spock.lang.Specification

import java.time.LocalDate

class CourseSpec extends Specification {

    def mockEnvironment = Mock(Environment)
    def mockDmiService = Mock(DmiService)
    def mockDataService = Mock(DmiDataService)
    def groovyCourse = new GroovyCourse()
    def mockCache = Mock(Cache)

    def misCode = "000"

    def courseStatuses = new Valcode("COURSE.STATUSES", [
            new Valcode.Entry("A", "Active", "1", null),
            new Valcode.Entry("C", "Canceled", "2", null)
    ])

    def castMisParms = new ColleagueData("CAST.MIS.PARMS", [
            "CASTMP.INSTITUTION.MAP": ["MIS000", "MIS001"],
            "CASTMP.LOC.MAP": ["1", "2"],
            "CASTMP.DEFAULT.INST.ID": "MIS000"
    ])

    def institutions = [
            new ColleagueData("MIS000", ["INST.OTHER.ID": "000"]),
            new ColleagueData("MIS001", ["INST.OTHER.ID": "001"])
    ]

    def xf01 = new ElfTranslateTable("CAST.XF01", "description", ["comments"], "ORIG.FIELD", "NEW.FIELD", [
            new ElfTranslateTable.Entry("LEC", "02", null, null),
            new ElfTranslateTable.Entry("LAB", "04", null, null),
            new ElfTranslateTable.Entry("OTH", null, null, null),

    ])

    def cb04 = new ElfTranslateTable("CAST.CB04", "description", ["comments"], "ORIG.FIELD", "NEW.FIELD", [
            new ElfTranslateTable.Entry("C", "C", null, null),
            new ElfTranslateTable.Entry("D", "D", null, null),
            new ElfTranslateTable.Entry("N", "N", null, null),

    ])

    def cb05 = new ElfTranslateTable("CAST.CB05", "description", ["comments"], "ORIG.FIELD", "NEW.FIELD", [
            new ElfTranslateTable.Entry("UC", "A", null, null),
            new ElfTranslateTable.Entry("CSU", "B", null, null),
            new ElfTranslateTable.Entry("X", "C", null, null),

    ])


    def setup() {
        ClassMap services = new ClassMap()
        services.putAll([(DmiService.class): mockDmiService, (DmiDataService.class): mockDataService, (Cache.class): mockCache])
        groovyCourse.colleagueInit(misCode, mockEnvironment, services)
    }

    def "missing params"() {
        when: groovyCourse.get(null, null, null)
        then: thrown AssertionError

        when: groovyCourse.get("000", null, null)
        then: thrown AssertionError
    }

    def "get - not found"() {
        when:
        groovyCourse.get(misCode, "CRS-1", null)

        then:
        thrown EntityNotFoundException
    }

    def "get - COURSE.STATUSES not found"() {
        when:
        groovyCourse.get(misCode, "CRS-1", null)

        then:
        1 * mockDataService.batchSelect("ST", "COURSES", _, _) >> [new ColleagueData("1234", ["CRS.NAME": "CRS-1"])]
        def e = thrown InternalServerException
        e.getMessage() == "COURSES.STATUSES valcode not found"
    }

    def "get - not found for location"() {
        setup:
        def courses = [new ColleagueData("1234", ["CRS.NAME" : "CRS-1", "CRS.LOCATIONS": ["2"] as String[]])]

        when:
        groovyCourse.get(misCode, "CRS-1", null)

        then:
        1 * mockDataService.batchSelect("ST", "COURSES", *_) >> courses
        1 * mockDataService.valcode("ST", "COURSE.STATUSES") >> courseStatuses
        1 * mockDataService.singleKey("ST", "ST.PARMS", *_) >> castMisParms
        1 * mockDataService.batchKeys("CORE", "INSTITUTIONS", *_) >> institutions
        thrown EntityNotFoundException
    }

    def "get - different courses by location, dates"() {
        setup:
        def goodDate = LocalDate.of(2001, 1, 1)
        def badDate = LocalDate.of(2000, 1, 1)
        def courses = [
                new ColleagueData("1234", ["CRS.NAME" : "CRS-1", "CRS.SHORT.TITLE": "1", "CRS.LOCATIONS": ["2"] as String[], "CRS.START.DATE" : goodDate]),
                new ColleagueData("1234", ["CRS.NAME" : "CRS-1", "CRS.SHORT.TITLE": "1", "CRS.LOCATIONS": ["2"] as String[], "CRS.START.DATE" : badDate]),
                new ColleagueData("4567", ["CRS.NAME" : "CRS-1", "CRS.SHORT.TITLE": "2", "CRS.LOCATIONS": ["1"] as String[], "CRS.START.DATE" : badDate]),
                new ColleagueData("4567", ["CRS.NAME" : "CRS-1", "CRS.SHORT.TITLE": "2", "CRS.LOCATIONS": ["1"] as String[], "CRS.START.DATE" : goodDate])
        ]

        when:
        def result1 = groovyCourse.get("000", "CRS-1", null)
        def result2 = groovyCourse.get("001", "CRS-1", null)

        then:
        2 * mockDataService.batchSelect("ST", "COURSES", *_) >> courses
        2 * mockDataService.valcode("ST", "COURSE.STATUSES") >> courseStatuses
        2 * mockDataService.singleKey("ST", "ST.PARMS", *_) >> castMisParms
        2 * mockDataService.batchKeys("CORE", "INSTITUTIONS", *_) >> institutions
        result1.title == "2"
        result2.title == "1"
        result1.start == ColleagueUtils.fromLocalDate(goodDate)
        result2.start == ColleagueUtils.fromLocalDate(goodDate)
    }

    def "get - by term dates, terms offered"() {
        def date1 = LocalDate.of(2000, 1, 1)
        def date2 = LocalDate.of(2001, 1, 1)
        def date3 = LocalDate.of(2002, 1, 1)

        def term1 = new ColleagueData("TERM1", ["TERM.END.DATE":  date1])
        def term2 = new ColleagueData("TERM1", ["TERM.END.DATE":  date2])
        def term3 = new ColleagueData("TERM3", ["TERM.END.DATE":  date3])

        def courses = [
                new ColleagueData("1", ["CRS.NAME" : "CRS-1", "CRS.SHORT.TITLE": "1", "CRS.START.DATE": date1.minusDays(1)]),
                new ColleagueData("2", ["CRS.NAME" : "CRS-1", "CRS.SHORT.TITLE": "2", "CRS.START.DATE": date1.plusDays(1)]),
                new ColleagueData("3", ["CRS.NAME" : "CRS-1", "CRS.SHORT.TITLE": "3", "CRS.TERMS.OFFERED": ["TERM3"] as String[]])
        ]

        when:
        def result3 = groovyCourse.get(misCode, "CRS-1", "TERM1")
        def result2 = groovyCourse.get(misCode, "CRS-1", "TERM2")
        def result1 = groovyCourse.get(misCode, "CRS-1", "TERM3")

        then:
        3 * mockDataService.batchSelect("ST", "COURSES", *_) >> courses
        3 * mockDataService.singleKey("ST", "TERMS", *_) >>> [term1, term2, term3]
        3 * mockDataService.valcode("ST", "COURSE.STATUSES") >> courseStatuses
        result1.title == "3"
        result2.title == "2"
        result3.title == "1"
    }

    def "get - status / units"() {
        setup:
        def course1 = [
                new ColleagueData("1", [
                        "CRS.NAME" : "CRS-1",
                        "CRS.STATUS": ["A", "C"] as String[],
                        "CRS.MIN.CRED": new BigDecimal(1.5),
                        "CRS.MAX.CRED": new BigDecimal(3.0)
                        ])
        ]

        def course2 = [
                new ColleagueData("1", [
                        "CRS.NAME" : "CRS-1",
                        "CRS.SHORT.TITLE": "1",
                        "CRS.STATUS": ["C"] as String[],
                        "CRS.MIN.CRED": new BigDecimal(2.0)
                ])
        ]

        when:
        def result1 = groovyCourse.get(misCode, "CRS-1", null)
        def result2 = groovyCourse.get(misCode, "CRS-1", null)

        then:
        2 * mockDataService.batchSelect("ST", "COURSES", *_) >>> [course1, course2]
        2 * mockDataService.valcode("ST", "COURSE.STATUSES") >> courseStatuses
        result1.minimumUnits == 1.5
        result1.maximumUnits == 3.0
        result1.status == CourseStatus.Active
        result2.minimumUnits == 2.0
        result2.maximumUnits == 2.0
        result2.status == CourseStatus.Inactive
    }

    def "get - contacts"() {
        setup:
        def course = [
                new ColleagueData("1", [
                        "CRS.NAME" : "CRS-1",
                        "CRS.NO.WEEKS": (Integer) 10,
                        "CRS.INSTR.METHODS": ["LEC", "LAB", "OTH", "OTH"] as String[],
                        "CRS.CONTACT.HOURS": [new BigDecimal(25), new BigDecimal(0.75), new BigDecimal(0.5), null] as BigDecimal[],
                        "CRS.CONTACT.MEASURES": [null, "T", "W", "T"] as String[]
                ])
        ]

        when:
        def result = groovyCourse.get(misCode, "CRS-1", null)

        then:
        1 * mockDataService.batchSelect("ST", "COURSES", *_) >> course
        1 * mockDataService.valcode("ST", "COURSE.STATUSES") >> courseStatuses
        1 * mockDataService.elfTranslationTables(*_) >> [xf01]
        result.courseContacts.size() == 4
        result.courseContacts[0].instructionalMethod == InstructionalMethod.Lecture
        result.courseContacts[1].instructionalMethod == InstructionalMethod.Lab
        result.courseContacts[2].instructionalMethod == InstructionalMethod.Other
        result.courseContacts[3].instructionalMethod == InstructionalMethod.Other
        result.courseContacts[0].hours == 25
        result.courseContacts[1].hours == 0.75
        result.courseContacts[2].hours == 5.0
        result.courseContacts[3].hours == null
    }

    def "get - credit status, grading method, transfer status"() {
        setup:
        def course1 = [
                new ColleagueData("1", [
                        "CRS.NAME" : "CRS-1",
                        "CRS.TRANSFER.STATUS": "CSU",
                        "CRS.CRED.TYPE": "C",
                        "CRS.ONLY.PASS.NOPASS.FLAG": "Y"])]
        def course2 = [
                new ColleagueData("1", [
                        "CRS.NAME" : "CRS-1",
                        "CRS.TRANSFER.STATUS": "UC",
                        "CRS.CRED.TYPE": "D",
                        "CRS.ALLOW.PASS.NOPASS.FLAG": "Y"])]

        def course3 = [
                new ColleagueData("1", [
                        "CRS.NAME" : "CRS-1",
                        "CRS.TRANSFER.STATUS": "X",
                        "CRS.CRED.TYPE": "N"])]
        def course4 = [
                new ColleagueData("1", [
                        "CRS.NAME" : "CRS-1",
                        "CRS.CRED.TYPE": "X"])]

        when:
        def result1 = groovyCourse.get(misCode, "CRS-1", null)
        def result2 = groovyCourse.get(misCode, "CRS-1", null)
        def result3 = groovyCourse.get(misCode, "CRS-1", null)
        def result4 = groovyCourse.get(misCode, "CRS-1", null)

        then:
        4 * mockDataService.batchSelect("ST", "COURSES", *_) >>> [course1, course2, course3, course4]
        4 * mockDataService.valcode("ST", "COURSE.STATUSES") >> courseStatuses
        4 * mockDataService.elfTranslationTables(*_) >> [cb04, cb05]
        result1.creditStatus == CreditStatus.NotDegreeApplicable
        result2.creditStatus == CreditStatus.DegreeApplicable
        result3.creditStatus == CreditStatus.NonCredit
        result4.creditStatus == null
        result1.gradingMethod == GradingMethod.PassNoPassOnly
        result2.gradingMethod == GradingMethod.PassNoPassOptional
        result3.gradingMethod == GradingMethod.NotGraded
        result4.gradingMethod == GradingMethod.Graded
        result1.transferStatus == TransferStatus.Csu
        result2.transferStatus == TransferStatus.CsuUc
        result3.transferStatus == TransferStatus.NotTransferable
        result4.transferStatus == null
    }

    def "get - requisites"() {
        setup:
        def course = [
                new ColleagueData("1", [
                        "CRS.NAME" : "CRS-1",
                        "CRS.REQS": ["1", "2", "3"]])]

        def reqs = [
                new ColleagueData("1", [
                        "ACR.REQS.TIMING": "C",
                        "ACR.PRINTED.SPEC": "Coreq 1"
                ]),
                new ColleagueData("2", [
                        "ACR.REQS.TIMING": "C",
                        "ACR.PRINTED.SPEC": "Coreq 2"
                ]),
                new ColleagueData("3", [
                        "ACR.REQS.TIMING": "P",
                        "ACR.PRINTED.SPEC": "Prereq 1"
                ]),
                // this one should be ignored - no pointer to it
                new ColleagueData("4", [
                        "ACR.REQS.TIMING": "P",
                        "ACR.PRINTED.SPEC": "Prereq 2"
                ])
        ]

        when:
        def result = groovyCourse.get(misCode, "CRS-1", null)

        then:
        1 * mockDataService.batchSelect("ST", "COURSES", *_) >> course
        1 * mockDataService.valcode("ST", "COURSE.STATUSES") >> courseStatuses
        1 * mockDataService.batchKeys("ST", "ACAD.REQMTS", *_) >> reqs
        result.prerequisites == "Prereq 1"
        result.corequisites == "Coreq 1, Coreq 2"
    }

    def "get - fees"() {
        setup:
        def course1 = [
                new ColleagueData("1", [
                        "CRS.NAME" : "CRS-1",
                        "CRS.OTHER.REG.BILLING.RATES": ["1", "2"]])]

        def course2 = [
                new ColleagueData("1", [
                        "CRS.NAME" : "CRS-1",
                        "CRS.MIN.CRED": (BigDecimal) 3,
                        "CRS.OTHER.REG.BILLING.RATES": ["1", "2"]])]

        def fees = [
                new ColleagueData("1", [
                        "RGBR.AMT.CALC.TYPE": "F",
                        "RGBR.CHARGE.AMT": (BigDecimal) 12.5
                ]),
                new ColleagueData("2", [
                        "RGBR.AMT.CALC.TYPE": "A",
                        "RGBR.CHARGE.AMT": (BigDecimal) 6
                ]),
                // this one should be ignored - no pointer to it
                new ColleagueData("3", [
                        "RGBR.AMT.CALC.TYPE": "F",
                        "RGBR.CHARGE.AMT": (BigDecimal) 1.3
                ])
        ]

        when:
        def result1 = groovyCourse.get(misCode, "CRS-1", null)
        def result2 = groovyCourse.get(misCode, "CRS-1", null)

        then:
        2 * mockDataService.batchSelect("ST", "COURSES", *_) >>> [course1, course2]
        2 * mockDataService.valcode("ST", "COURSE.STATUSES") >> courseStatuses
        2 * mockDataService.batchKeys("ST", "REG.BILLING.RATES", *_) >> fees
        result1.fee == 12.5
        result2.fee == 30.5


    }

    def "get - sorting active / inactive"() {
        setup:
        def courses = [
                new ColleagueData("1", [
                        "CRS.NAME" : "CRS-1",
                        "CRS.SHORT.TITLE" : "1",
                        "CRS.STATUS" : ["A"] as String[]
                ]),
                new ColleagueData("2", [
                        "CRS.NAME" : "CRS-1",
                        "CRS.SHORT.TITLE" : "2",
                        "CRS.STATUS" : ["C"] as String[]
                ]),
                new ColleagueData("3", [
                        "CRS.NAME" : "CRS-1",
                        "CRS.SHORT.TITLE" : "3",
                        "CRS.STATUS" : ["A"] as String[]
                ]),
                new ColleagueData("4", [
                        "CRS.NAME" : "CRS-1",
                        "CRS.SHORT.TITLE" : "4",
                        "CRS.STATUS" : ["C"] as String[]
                ])
        ]

        when:
        def result = groovyCourse.get(misCode, "CRS-1", null)

        then:
        1 * mockDataService.batchSelect("ST", "COURSES", *_) >> courses
        1 * mockDataService.valcode("ST", "COURSE.STATUSES") >> courseStatuses
        result.title == "1" || result.title == "3"
    }

}
