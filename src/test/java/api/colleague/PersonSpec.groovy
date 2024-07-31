package api.colleague

import api.colleague.Person as GroovyPerson
import com.ccctc.adaptor.exception.EntityNotFoundException
import com.ccctc.adaptor.util.ClassMap
import org.ccctc.colleaguedmiclient.model.CTXData
import org.ccctc.colleaguedmiclient.model.ColleagueData
import org.ccctc.colleaguedmiclient.service.DmiCTXService
import org.ccctc.colleaguedmiclient.service.DmiDataService
import org.ccctc.colleaguedmiclient.service.DmiService
import org.springframework.cache.Cache
import org.springframework.core.env.Environment
import spock.lang.Specification

class PersonSpec extends Specification {

    def environment = Mock(Environment)
    def dmiService = Mock(DmiService)
    def dmiCTXService = Mock(DmiCTXService)
    def dmiDataService = Mock(DmiDataService)
    def cache = Mock(Cache)

    def misCode = "000"
    def sisPersonId = "1234567"

    def cccIdTypes = "CCCID"
    def studentEmail = "student.edu"
    def staffEmail = "staff.edu"

    def groovyPerson = new GroovyPerson()

    def setup() {
        ClassMap services = new ClassMap()
        services.putAll([(DmiService.class): dmiService, (DmiCTXService.class): dmiCTXService,
                         (DmiDataService.class): dmiDataService, (Cache.class): cache])

        environment.getProperty("colleague.ccc.id.types") >> cccIdTypes
        environment.getProperty("colleague.student.eppn.suffix") >> studentEmail
        environment.getProperty("colleague.staff.eppn.suffix") >> staffEmail
        environment.getProperty("colleague.email.domains") >> staffEmail

        groovyPerson.colleagueInit(misCode, environment, services)
    }


    def "assertions"() {
        when: groovyPerson.get(null, null)
        then: thrown AssertionError
    }

    def "get - not found"() {
        when:
        groovyPerson.get(misCode, sisPersonId)

        then:
        1 * dmiDataService.singleKey("CORE", "PERSON", *_) >> null
        thrown EntityNotFoundException
    }

    def "get - bare bones"() {
        setup:
        def data = new ColleagueData(sisPersonId, [
                "FIRST.NAME" : "But",
                "LAST.NAME" : "Ok"
        ])

        when:
        def result = groovyPerson.get(misCode, sisPersonId)

        then:
        1 * dmiDataService.singleKey("CORE", "PERSON", *_) >> data
        result.misCode == misCode
        result.sisPersonId == sisPersonId
        result.firstName =="But"
        result.lastName == "Ok"
    }

    def "get - alt IDs"() {
        setup:
        def data = new ColleagueData(sisPersonId, [
                "PERSON.ALT.IDS" : ["ALT1", "CCCID", "ALT3"] as String[],
                "PERSON.ALT.ID.TYPES" : ["A", cccIdTypes, "C"] as String[]
        ])

        when:
        def result = groovyPerson.get(misCode, sisPersonId)

        then:
        1 * dmiDataService.singleKey("CORE", "PERSON", *_) >> data
        result.cccid == "CCCID"
    }

    def "get - email addresses, student login"() {
        def data = new ColleagueData(sisPersonId, [
                "PERSON.EMAIL.ADDRESSES" : ["test@test.com", "student@" + studentEmail] as String[],
                "PERSON.PREFERRED.EMAIL" : [null, "Y"] as String[]
        ])

        def oe = new ColleagueData(sisPersonId, ["OE.ORG.ENTITY.ENV": "1"])
        def oee = new ColleagueData(sisPersonId, ["OEE.USERNAME": "student"])

        when:
        def result = groovyPerson.get(misCode, sisPersonId)

        then:
        1 * dmiDataService.singleKey("CORE", "PERSON", *_) >> data
        1 * dmiDataService.singleKey("UT", "ORG.ENTITY", *_) >> oe
        1 * dmiDataService.singleKey("UT", "ORG.ENTITY.ENV", *_) >> oee
        result.emailAddresses.size() == 2
        result.emailAddresses[0].emailAddress == "student@" + studentEmail
        result.loginId == "student"
        result.loginSuffix == studentEmail
    }

    def "get - email addresses, staff login"() {
        def data = new ColleagueData(sisPersonId, [
                "PERSON.EMAIL.ADDRESSES" : ["test@test.com", "staff@" + staffEmail] as String[],
                "PERSON.PREFERRED.EMAIL" : [null, null] as String[]
        ])

        def oe = new ColleagueData(sisPersonId, ["OE.ORG.ENTITY.ENV": "1"])
        def oee = new ColleagueData("1", ["OEE.USERNAME": "staff"])

        when:
        def result = groovyPerson.get(misCode, sisPersonId)

        then:
        1 * dmiDataService.singleKey("CORE", "PERSON", *_) >> data
        1 * dmiDataService.singleKey("UT", "ORG.ENTITY", *_) >> oe
        1 * dmiDataService.singleKey("UT", "ORG.ENTITY.ENV", *_) >> oee
        result.emailAddresses.size() == 2
        result.emailAddresses[0].emailAddress == "test@test.com"
        result.loginId == "staff"
        result.loginSuffix == staffEmail
    }

    def "get - email addresses, not student or staff login"() {
        def data = new ColleagueData(sisPersonId, [
                "PERSON.EMAIL.ADDRESSES" : ["test@test.com"] as String[],
                "PERSON.PREFERRED.EMAIL" : [null] as String[]
        ])

        def oe = new ColleagueData(sisPersonId, ["OE.ORG.ENTITY.ENV": "1"])
        def oee = new ColleagueData("1", ["OEE.USERNAME": "someone"])

        when:
        def result = groovyPerson.get(misCode, sisPersonId)

        then:
        1 * dmiDataService.singleKey("CORE", "PERSON", *_) >> data
        1 * dmiDataService.singleKey("UT", "ORG.ENTITY", *_) >> oe
        1 * dmiDataService.singleKey("UT", "ORG.ENTITY.ENV", *_) >> oee
        result.loginId == "someone"
        result.loginSuffix == null
    }

    def "getAll - sisPersonIds"() {
        def sisPersonId2 = "person2"
        def cccIdLookup = [
                new ColleagueData("nope", [
                        "PERSON.ALT.IDS" : ["ABC123"] as String[],
                        "PERSON.ALT.ID.TYPES" : ["NOT-A-CCCID"] as String[],
                ]),
                new ColleagueData(sisPersonId2, [
                        "PERSON.ALT.IDS" : ["ABC123"] as String[],
                        "PERSON.ALT.ID.TYPES" : [cccIdTypes] as String[],
                ]),
        ]

        def data = [
                new ColleagueData(sisPersonId, [
                        "LAST.NAME" : "H"
                ]),
                new ColleagueData(sisPersonId2, [
                        "LAST.NAME" : "I"
                ])
        ]

        def oe = [
                new ColleagueData(sisPersonId, ["OE.ORG.ENTITY.ENV": "1"]),
                new ColleagueData(sisPersonId2, ["OE.ORG.ENTITY.ENV": "2"])
        ]

        def oee = [
                new ColleagueData("1", ["OEE.USERNAME": "username1"]),
                new ColleagueData("2", ["OEE.USERNAME": "username2"])
        ]


        when:
        def result = groovyPerson.getAll(misCode, [sisPersonId] as String[], ["ABC123"] as String[])

        then:
        1 * dmiDataService.batchSelect("CORE", "PERSON", *_) >> cccIdLookup
        1 * dmiDataService.batchKeys("CORE", "PERSON", _, [sisPersonId, sisPersonId2]) >> data
        1 * dmiDataService.batchKeys("UT", "ORG.ENTITY", _, [sisPersonId, sisPersonId2]) >> oe
        1 * dmiDataService.batchKeys("UT", "ORG.ENTITY.ENV", _, ["1", "2"]) >> oee
        0 * _
        result.size() == 2
        result[0].loginId == "username1"
        result[1].loginId == "username2"
    }

    def "getStudentPerson - also test using rule for eppn"() {
        setup:
        def cccIdData = [new ColleagueData(sisPersonId, [
                "PERSON.ALT.IDS": ["cccid"] as String[],
                "PERSON.ALT.ID.TYPES": [cccIdTypes] as String[]
        ])]

        def eppnStudent = "test@" + studentEmail
        def eppnStaff = "test@" + staffEmail

        def data = new ColleagueData(sisPersonId,
                ["FIRST.NAME" : "Another",
                 "LAST.NAME" : "Test"])
        def oe = new ColleagueData(sisPersonId, ["OE.ORG.ENTITY.ENV": "1"])
        def oee = new ColleagueData("1", ["OEE.USERNAME": "test"])

        groovyPerson.eppnStudentOrStaffRule = "RULE"

        when:
        // student eppns
        def result1 = groovyPerson.getStudentPerson(misCode, sisPersonId, null, null)
        def result2 = groovyPerson.getStudentPerson(misCode, null, eppnStudent, null)
        def result3 = groovyPerson.getStudentPerson(misCode, null, null, "cccid")
        // staff eppns
        def result4 = groovyPerson.getStudentPerson(misCode, sisPersonId, eppnStaff, null)
        def result5 = groovyPerson.getStudentPerson(misCode, sisPersonId, eppnStaff, "cccid")

        then:
        3 * dmiDataService.selectKeys("ORG.ENTITY.ENV", _) >> ([sisPersonId] as String[])
        2 * dmiDataService.batchSelect("CORE", "PERSON", *_) >> cccIdData
        5 * dmiDataService.singleKey("CORE", "PERSON", _, _) >> data
        5 * dmiDataService.singleKey("UT", "ORG.ENTITY", _, _) >> oe
        5 * dmiDataService.singleKey("UT", "ORG.ENTITY.ENV", _, _) >> oee
        5 * dmiCTXService.execute("ST", "X.CCCTC.EVAL.RULE.ST", *_) >>> [
                new CTXData(["Result": true, "ValidIds": [sisPersonId] as String[], "InvalidIds": [] as String[]], null),
                new CTXData(["Result": true, "ValidIds": [sisPersonId] as String[], "InvalidIds": [] as String[]], null),
                new CTXData(["Result": true, "ValidIds": [sisPersonId] as String[], "InvalidIds": [] as String[]], null),
                new CTXData(["Result": true, "ValidIds": [] as String[], "InvalidIds": [sisPersonId] as String[]], null),
                new CTXData(["Result": true, "ValidIds": [] as String[], "InvalidIds": [sisPersonId] as String[]], null)
                ]

        0 * _

        // student eppn result
        result1.toString() == result2.toString()
        result1.toString() == result3.toString()

        // staff eppn result
        result4.toString() == result5.toString()

        result1.loginId == "test"
        result4.loginId == "test"
        result1.loginSuffix == studentEmail
        result4.loginSuffix == staffEmail
    }

}
