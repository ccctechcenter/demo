package api.colleague

import com.ccctc.adaptor.exception.EntityNotFoundException
import com.ccctc.adaptor.exception.InvalidRequestException
import com.ccctc.adaptor.model.CrosslistingStatus
import com.ccctc.adaptor.model.InstructionalMethod
import com.ccctc.adaptor.model.SectionStatus
import com.ccctc.adaptor.util.ClassMap
import org.ccctc.colleaguedmiclient.model.ColleagueData
import org.ccctc.colleaguedmiclient.model.ElfTranslateTable
import org.ccctc.colleaguedmiclient.model.Valcode
import org.ccctc.colleaguedmiclient.service.DmiCTXService
import org.ccctc.colleaguedmiclient.service.DmiDataService
import org.ccctc.colleaguedmiclient.service.DmiService
import org.springframework.cache.Cache
import org.springframework.cache.support.SimpleValueWrapper
import org.springframework.core.env.Environment
import spock.lang.Specification

class SectionSpec extends Specification {

    def misCode = "000"

    Environment environment
    Section sectionGroovy
    DmiService dmiService
    DmiDataService dmiDataService
    Cache cache

    def sectionStatuses = new Valcode("SECTION.STATUSES", [
            new Valcode.Entry("A", "Active", "1", null),
            new Valcode.Entry("C", "Cancelled", "2", null),
            new Valcode.Entry("P", "Pending", null, null)
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

    def buildings = [
            new ColleagueData("M1", ["BLDG.LOCATION": "1"]),
            new ColleagueData("M2", ["BLDG.LOCATION": "1"]),
            new ColleagueData("O1", ["BLDG.LOCATION": "2"]),
            new ColleagueData("O2", ["BLDG.LOCATION": "2"])
    ]

    def locations = [
            new ColleagueData("1", ["LOC.DESC": "Main Campus"]),
            new ColleagueData("2", ["LOC.DESC": "Other Campus"]),
    ]

    def rooms = [
            new ColleagueData("M1*1", ["ROOM.NAME": "Main 1, Rm 1"]),
            new ColleagueData("M2*1", ["ROOM.NAME": "Main 2, Rm 1"]),
            new ColleagueData("O1*1", ["ROOM.NAME": "Other 1, Rm 1"]),
            new ColleagueData("O2*1", ["ROOM.NAME": "Other 2, Rm 1"])
    ]



    def setup() {
        environment = Mock(Environment)
        dmiService = Mock(DmiService)
        dmiDataService = Mock(DmiDataService)
        cache = Mock(Cache)

        ClassMap services = new ClassMap()
        services.putAll([(DmiService.class)    : dmiService, (DmiDataService.class): dmiDataService, (Cache.class): cache])

        sectionGroovy = new Section()
        sectionGroovy.colleagueInit(misCode, environment, services)
    }

    def "assertions"() {
        when: sectionGroovy.get(null, null, null)
        then: thrown AssertionError
        when: sectionGroovy.get("000", null, null)
        then: thrown AssertionError
        when: sectionGroovy.get("000", "1", null)
        then: thrown AssertionError

        when: sectionGroovy.getAll(null, null, null)
        then: thrown AssertionError
        when: sectionGroovy.getAll("000", null, null)
        then: thrown AssertionError

        when: sectionGroovy.search(null, null, null)
        then: thrown AssertionError
        when: sectionGroovy.search("000", null, null)
        then: thrown AssertionError
        when: sectionGroovy.search("000", "TERM", null)
        then: thrown AssertionError
        when: sectionGroovy.search("000", "TERM", [] as String[])
        then: thrown AssertionError
    }

    def "get - ok - no meetings or instructors"() {
        setup:
        def section = [
                new ColleagueData("1234",
                        [
                                "SEC.LOCATION" : "1",
                                "SEC.SUBJECT"  : "SUBJ",
                                "SEC.COURSE.NO": "1",
                                "SEC.SYNONYM"  : "1",
                                "SEC.TERM"     : "TERM",
                                "SEC.MIN.CRED" : 3.0 as BigDecimal,
                                "SEC.STATUS"   : ["A"]
                        ])]

        when:
        def a = sectionGroovy.get(misCode, "1", "TERM")

        then:
        1 * cache.get("validateTerm:TERM") >> new SimpleValueWrapper("TERM")
        1 * dmiDataService.batchSelect("ST", "COURSE.SECTIONS", *_) >> section
        1 * dmiDataService.valcode("ST", "SECTION.STATUSES") >> sectionStatuses
        1 * dmiDataService.batchSelect("CORE", "LOCATIONS", *_) >> locations
        1 * dmiDataService.batchSelect("CORE", "BUILDINGS", *_) >> buildings
        1 * dmiDataService.elfTranslationTable("CAST.XF01") >> xf01
        a.misCode == misCode
        a.sisSectionId == "1"
        a.sisCourseId == "SUBJ-1"
        a.sisTermId == "TERM"
        a.minimumUnits == 3
        a.maximumUnits == 3
        a.campus == "Main Campus"
        a.status == SectionStatus.Active
        a.crosslistingStatus == CrosslistingStatus.NotCrosslisted
        a.crosslistingDetail == null
    }

    def "get - xlist primary"() {
        setup:
        def section = [
                new ColleagueData("100",
                        [
                                "SEC.SYNONYM"  : "1",
                                "SEC.TERM" : "TERM",
                                "SEC.XLIST" : "XLIST1",
                                "SEC.STATUS" : ["X"] as String[] // extra coverage (status doesnt match valcode)
                        ])]

        def xlists = [
                new ColleagueData("XLIST1",
                        [
                                "CSXL.COURSE.SECTIONS" : ["100", "101", "102"] as String[],
                                "CSXL.PRIMARY.SECTION" : "100"
                        ])]

        def secSynonyms = [
                new ColleagueData("100",
                        [
                                "SEC.SYNONYM"  : "1",
                        ]),
                new ColleagueData("101",
                        [
                                "SEC.SYNONYM"  : "2",
                        ]),
                new ColleagueData("102",
                        [
                                "SEC.SYNONYM"  : "3",
                        ])
        ]


        when:
        def a = sectionGroovy.get(misCode, "1", "TERM")

        then:
        1 * cache.get("validateTerm:TERM") >> new SimpleValueWrapper("TERM")
        1 * dmiDataService.batchSelect("ST", "COURSE.SECTIONS", *_) >> section
        1 * dmiDataService.batchKeys("ST", "COURSE.SEC.XLISTS", _, ["XLIST1"]) >> xlists
        1 * dmiDataService.batchKeys("ST", "COURSE.SECTIONS", _, ["100", "101", "102"]) >> secSynonyms
        1 * dmiDataService.valcode("ST", "SECTION.STATUSES") >> sectionStatuses
        a.status == SectionStatus.Pending
        a.crosslistingStatus == CrosslistingStatus.CrosslistedPrimary
        a.crosslistingDetail.sisTermId == "TERM"
        a.crosslistingDetail.sisSectionIds == ["1", "2", "3"]
        a.crosslistingDetail.name == "XLIST1"
        a.crosslistingDetail.primarySisSectionId == "1"
    }

    def "get - xlist secondary"() {
        setup:
        def section = [
                new ColleagueData("100",
                        [
                                "SEC.SYNONYM"  : "1",
                                "SEC.TERM" : "TERM",
                                "SEC.XLIST" : "XLIST1",
                                "SEC.STATUS" : ["X"] as String[] // extra coverage (status doesnt match valcode)
                        ])]

        def xlists = [
                new ColleagueData("XLIST1",
                        [
                                "CSXL.COURSE.SECTIONS" : ["100", "101", "102"] as String[],
                                "CSXL.PRIMARY.SECTION" : "101"
                        ])]

        def secSynonyms = [
                new ColleagueData("100",
                        [
                                "SEC.SYNONYM"  : "1",
                        ]),
                new ColleagueData("101",
                        [
                                "SEC.SYNONYM"  : "2",
                        ]),
                new ColleagueData("102",
                        [
                                "SEC.SYNONYM"  : "3",
                        ])
        ]


        when:
        def a = sectionGroovy.get(misCode, "1", "TERM")

        then:
        1 * cache.get("validateTerm:TERM") >> new SimpleValueWrapper("TERM")
        1 * dmiDataService.batchSelect("ST", "COURSE.SECTIONS", *_) >> section
        1 * dmiDataService.batchKeys("ST", "COURSE.SEC.XLISTS", _, ["XLIST1"]) >> xlists
        1 * dmiDataService.batchKeys("ST", "COURSE.SECTIONS", _, ["100", "101", "102"]) >> secSynonyms
        1 * dmiDataService.valcode("ST", "SECTION.STATUSES") >> sectionStatuses
        a.status == SectionStatus.Pending
        a.crosslistingStatus == CrosslistingStatus.CrosslistedSecondary
        a.crosslistingDetail.sisTermId == "TERM"
        a.crosslistingDetail.sisSectionIds == ["1", "2", "3"]
        a.crosslistingDetail.name == "XLIST1"
        a.crosslistingDetail.primarySisSectionId == "2"


    }

    def "get - not found"() {
        when:
        sectionGroovy.get(misCode, "1", "TERM")

        then:
        1 * cache.get("validateTerm:TERM") >> new SimpleValueWrapper("TERM")
        1 * dmiDataService.batchSelect("ST", "COURSE.SECTIONS", *_) >> []
        thrown EntityNotFoundException
    }

    def "get all - section statuses"() {
        setup:
        def sections = [
                new ColleagueData("1001", ["SEC.SYNONYM" : "1", "SEC.STATUS" : ["A"] as String[]]),
                new ColleagueData("1002", ["SEC.SYNONYM" : "2", "SEC.STATUS" : ["C"] as String[]]),
                new ColleagueData("1003", ["SEC.SYNONYM" : "3", "SEC.STATUS" : ["P"] as String[]])
        ]

        when:
        def a = sectionGroovy.getAll(misCode, "TERM", null)

        then:
        1 * cache.get("validateTerm:TERM") >> new SimpleValueWrapper("TERM")
        1 * dmiDataService.batchSelect("ST", "COURSE.SECTIONS", *_) >> sections
        1 * dmiDataService.valcode("ST", "SECTION.STATUSES") >> sectionStatuses
        a.size() == 3
        a[0].status == SectionStatus.Active
        a[1].status == SectionStatus.Cancelled
        a[2].status == SectionStatus.Pending
    }

    def "get all - sisCourseId, location filter, max units"() {
        setup:
        def sections = [
                new ColleagueData("1001", ["SEC.SYNONYM": "1", "SEC.LOCATION" : "1"]),
                new ColleagueData("1003", ["SEC.SYNONYM": "2", "SEC.LOCATION" : "2", "SEC.MIN.CRED" : 1 as BigDecimal, "SEC.MAX.CRED": 3 as BigDecimal])
        ]

        when:
        def a = sectionGroovy.getAll("001", "TERM", "SUBJ-1")

        then:
        1 * cache.get("validateTerm:TERM") >> new SimpleValueWrapper("TERM")
        1 * dmiDataService.batchSelect("ST", "COURSE.SECTIONS", *_) >> sections
        1 * dmiDataService.singleKey("ST", "ST.PARMS", *_) >> castMisParms
        1 * dmiDataService.batchKeys("CORE", "INSTITUTIONS", *_) >> institutions
        a.size() == 1
        a[0].misCode == "001"
        a[0].sisSectionId == "2"
        a[0].minimumUnits == 1
        a[0].maximumUnits == 3
    }

    def "get all - bad sisCourseId"() {
        when: sectionGroovy.getAll(misCode, "TERM", "BAD")
        then: thrown InvalidRequestException
        when: sectionGroovy.getAll(misCode, "TERM", "ALSO-IS-BAD")
        then: thrown InvalidRequestException
    }

    def "search - no sections"() {
        when:
        def a = sectionGroovy.search(misCode, "TERM", ["word"] as String[])

        then:
        1 * cache.get("validateTerm:TERM") >> new SimpleValueWrapper("TERM")
        1 * dmiDataService.selectKeys("COURSE.SECTIONS", "SEC.TERM = \"TERM\"") >> new String[0]
        a == []
    }

    def "search - one char, course sort"() {
        setup:
        def secKeys = ["1", "2", "3", "4", "5"] as String[]
        def matchingKeys = ["3", "4"] as String[]
        def sections = [
                new ColleagueData("1234", ["SEC.SYNONYM": "3", "SEC.SUBJECT": "SUB1", "SEC.COURSE.NO": "1"]),
                new ColleagueData("1234", ["SEC.SYNONYM": "4", "SEC.SUBJECT": "SUB2", "SEC.COURSE.NO": "1"])
        ]

        when:
        def a = sectionGroovy.search(misCode, "TERM", ["a"] as String[])

        then:
        1 * cache.get("validateTerm:TERM") >> new SimpleValueWrapper("TERM")
        1 * dmiDataService.selectKeys("COURSE.SECTIONS", _) >> secKeys
        1 * dmiDataService.selectKeys("COURSE.SECTIONS", _, _) >> matchingKeys
        1 * dmiDataService.batchKeys("ST", "COURSE.SECTIONS", _, ["3", "4"]) >> sections
        a.size() == 2
        a[0].sisCourseId == "SUB1-1"
        a[1].sisCourseId == "SUB2-1"
    }

    def "search - sisCourseId, location"() {
        setup:
        def secKeys = ["1", "2", "3", "4", "5"] as String[]
        def matchingKeys1 = ["3", "4"] as String[]
        def matchingKeys2 = ["3"] as String[]
        def sections = [
                new ColleagueData("1234", ["SEC.SYNONYM": "3", "SEC.SUBJECT": "ENGL", "SEC.COURSE.NO": "2A", "SEC.LOCATION": "1"])
        ]

        when:
        def a = sectionGroovy.search(misCode, "TERM", ["ENGL-2A", "Main campus"] as String[])

        then:
        1 * cache.get("validateTerm:TERM") >> new SimpleValueWrapper("TERM")
        _ * dmiDataService.batchSelect("CORE", "LOCATIONS", _, _) >> locations
        1 * dmiDataService.selectKeys("COURSE.SECTIONS", _) >> secKeys
        1 * dmiDataService.selectKeys("COURSE.SECTIONS", { it.contains("(SEC.SUBJECT = \"ENGL\" AND SEC.COURSE.NO = \"2A\")") }, _) >> matchingKeys1
        1 * dmiDataService.selectKeys("COURSE.SECTIONS", { it.contains("SEC.LOCATION = \"1\"")}, ["3", "4"]) >> matchingKeys2
        1 * dmiDataService.batchKeys("ST", "COURSE.SECTIONS", _, ["3"]) >> sections
        a.size() == 1
        a[0].sisSectionId == "3"
        a[0].campus == "Main Campus"
    }


    def "search - meetings"() {
        setup:
        def secKeys = ["1", "2", "3", "4", "5"] as String[]
        def meetingMatchKeys = ["3"] as String[]

        def childKeys = [
                new ColleagueData("1", ["SEC.MEETING" : ["1", "2"] as String[], "SEC.FACULTY" : new String[0]]),
                new ColleagueData("2", ["SEC.MEETING" : ["3", "4"] as String[], "SEC.FACULTY" : new String[0]]),
                // 5, 6 and 7 are for our matching meetings
                new ColleagueData("3", ["SEC.MEETING" : ["5", "6", "7"] as String[], "SEC.FACULTY" : new String[0]]),
        ]

        def sections = [
                // matching section
                new ColleagueData("3", ["SEC.SYNONYM": "3", "SEC.MEETING" : ["5", "6", "7"] as String[]])
        ]

        def meetings = [
                new ColleagueData("1", ["CSM.MONDAY": "Y", "CSM.BLDG" : "O1", "CSM.ROOM" : "1"]),
                new ColleagueData("2", ["CSM.THURSDAY": "Y", "CSM.BLDG" : "O1", "CSM.ROOM" : "2"]),
                new ColleagueData("3", ["CSM.MONDAY": "Y", "CSM.BLDG" : "O1", "CSM.ROOM" : "1"]),
                new ColleagueData("4", ["CSM.THURSDAY": "Y", "CSM.BLDG" : "O1", "CSM.ROOM" : "2"]),
                // 5, 6 and 7 are for our matching section
                new ColleagueData("5", ["CSM.MONDAY": "Y", "CSM.BLDG" : "M2", "CSM.ROOM" : "1", "CSM.INSTR.METHOD": "LEC"]),
                new ColleagueData("6", ["CSM.THURSDAY": "Y", "CSM.BLDG" : "M1", "CSM.ROOM" : "2", "CSM.INSTR.METHOD": "LAB"]),
                new ColleagueData("7", ["CSM.SUNDAY": "Y", "CSM.BLDG" : "M1", "CSM.ROOM" : "2", "CSM.INSTR.METHOD": "OTH"])
        ]

        when:
        def a = sectionGroovy.search(misCode, "TERM", ["M1"] as String[])

        then:
        1 * cache.get("validateTerm:TERM") >> new SimpleValueWrapper("TERM")
        1 * dmiDataService.selectKeys("COURSE.SECTIONS", _) >> secKeys
        1 * dmiDataService.selectKeys("COURSE.SECTIONS", _, _) >> new String[0] // no matching keys searching section
        1 * dmiDataService.batchKeys("ST", "COURSE.SECTIONS", ["SEC.MEETING", "SEC.FACULTY"], _) >> childKeys
        1 * dmiDataService.selectKeys("COURSE.SEC.MEETING", _, ["1", "2", "3", "4", "5", "6", "7"]) >> meetingMatchKeys // matching keys searching COURSE.SEC.MEETING
        1 * dmiDataService.batchKeys("ST", "COURSE.SECTIONS", _, ["3"]) >> sections
        1 * dmiDataService.batchKeys("ST", "COURSE.SEC.MEETING", _, ["5", "6", "7"]) >> meetings
        _ * dmiDataService.batchSelect("CORE", "BUILDINGS", *_) >> buildings
        _ * dmiDataService.batchSelect("CORE", "ROOMS", *_) >> rooms
        _ * dmiDataService.batchSelect("CORE", "LOCATIONS", *_) >> locations
        _ * dmiDataService.elfTranslationTable("CAST.XF01") >> xf01
        a.size() == 1
        a[0].meetingTimes.size() == 3
        a[0].meetingTimes[0].monday == true
        a[0].meetingTimes[1].thursday == true
        a[0].meetingTimes[2].sunday == true
        a[0].meetingTimes[0].instructionalMethod == InstructionalMethod.Lecture
        a[0].meetingTimes[1].instructionalMethod == InstructionalMethod.Lab
        a[0].meetingTimes[2].instructionalMethod == InstructionalMethod.Other
    }

    def "search - faculty"() {
        setup:
        def secKeys = ["1", "2", "3", "4", "5"] as String[]
        def facultyIds = ["101", "102", "103"] as String[]

        def matchingSectionKeys = ["3"] as String[]
        def matchingPersonKeys = ["103"] as String[]

        def childKeys = [
                new ColleagueData("1", ["SEC.MEETING" : new String[0], "SEC.FACULTY" : "1"]),
                new ColleagueData("2", ["SEC.MEETING" : new String[0], "SEC.FACULTY" : "2"]),
                // 3 matches our faculty
                new ColleagueData("3", ["SEC.MEETING" : new String[0], "SEC.FACULTY" : "3"]),
        ]

        def sections = [
                // matching section
                new ColleagueData("3", ["SEC.SYNONYM": "3", "SEC.FACULTY" : ["3"] as String[]])
        ]

        def facAssign = [
                new ColleagueData("3", ["CSF.FACULTY" : "103"])
        ]

        def persons = [
                new ColleagueData("103", [
                        "PERSON.EMAIL.ADDRESSES": ["email", "email2"] as String[],
                        "PERSON.PREFERRED.EMAIL": [null, "Y"] as String[],
                        "FIRST.NAME": "Steve",
                ])
        ]

        when:
        def a = sectionGroovy.search(misCode, "TERM", ["steve"] as String[])

        then:
        1 * cache.get("validateTerm:TERM") >> new SimpleValueWrapper("TERM")
        1 * dmiDataService.selectKeys("COURSE.SECTIONS", _) >> secKeys
        1 * dmiDataService.selectKeys("COURSE.SECTIONS", _, _) >> new String[0] // no matching keys searching section
        1 * dmiDataService.batchKeys("ST", "COURSE.SECTIONS", ["SEC.MEETING", "SEC.FACULTY"], _) >> childKeys
        1 * dmiDataService.selectKeys("COURSE.SEC.FACULTY", _, _) >> facultyIds // CSF.FACULTY values from COURSE.SEC.FACULTY to
        1 * dmiDataService.selectKeys("PERSON", _, _) >> matchingPersonKeys
        1 * dmiDataService.selectKeys("COURSE.SEC.FACULTY", { it.contains("103")}, _) >> matchingSectionKeys

        1 * dmiDataService.batchKeys("ST", "COURSE.SECTIONS", _, ["3"]) >> sections
        1 * dmiDataService.batchKeys("ST", "COURSE.SEC.FACULTY", _, ["3"]) >> facAssign
        1 * dmiDataService.batchKeys("CORE", "PERSON", _, ["103"]) >> persons
        a.size() == 1
        a[0].instructors.size() == 1
        a[0].instructors[0].firstName == "Steve"
        a[0].instructors[0].emailAddress == "email2"

    }

    def "invalid term coverage"() {
        setup:
        def e

        when:
        sectionGroovy.get(misCode, "1234", "invalid")

        then:
        1 * cache.get("validateTerm:invalid") >> null
        1 * dmiDataService.selectKeys("TERMS", _, _) >> ([] as String[])
        e = thrown InvalidRequestException
        e.code == InvalidRequestException.Errors.termNotFound

        when:
        sectionGroovy.getAll(misCode, "invalid", null)

        then:
        1 * cache.get("validateTerm:invalid") >> null
        1 * dmiDataService.selectKeys("TERMS", _, _) >> ([] as String[])
        e = thrown InvalidRequestException
        e.code == InvalidRequestException.Errors.termNotFound

        when:
        sectionGroovy.search(misCode, "invalid", ["word"] as String[])

        then:
        1 * cache.get("validateTerm:invalid") >> null
        1 * dmiDataService.selectKeys("TERMS", _, _) >> ([] as String[])
         e = thrown InvalidRequestException
        e.code == InvalidRequestException.Errors.termNotFound
    }

}

