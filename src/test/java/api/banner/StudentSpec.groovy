package api.banner

import com.ccctc.adaptor.exception.EntityConflictException
import com.ccctc.adaptor.exception.EntityNotFoundException
import com.ccctc.adaptor.exception.InvalidRequestException
import com.ccctc.adaptor.model.ApplicationStatus
import com.ccctc.adaptor.model.CohortTypeEnum
import com.ccctc.adaptor.model.OrientationStatus
import com.ccctc.adaptor.model.ResidentStatus
import com.ccctc.adaptor.model.students.StudentFieldSet
import groovy.sql.GroovyRowResult
import groovy.sql.Sql
import org.springframework.core.env.Environment
import spock.lang.Specification


class StudentSpec extends Specification {
    def static misCode = "001"
    def static cccId = "12345"
    def static sisTermId = "111111"

    Student Student
    com.ccctc.adaptor.model.Student updates
    Sql sql
    Environment environment

    def setup() {
        sql = Mock(Sql)
        environment = Mock(Environment)

        Student = Spy(new api.banner.Student())
        Student.environment = environment

        GroovyMock(BannerConnection, global: true)
        BannerConnection.getSession(*_) >> sql
    }

    def "getHomeCollege" () {
        setup:
        environment.getProperty("banner.cccid.getQuery")  >> "cccid"
        environment.getProperty("banner.student.misEnrollment.getQuery") >> "misEnrollment"
        environment.getProperty("banner.student.applicationStatus.getQuery") >> "appStatus"
        environment.getProperty("banner.campus.misCode.getQuery") >> "misCode"

        when:
        Student.getHomeCollege(misCode, cccId)

        then:
        thrown EntityNotFoundException

        when:
        Student.getHomeCollege(misCode, cccId)

        then:
        1 * sql.firstRow("cccid", *_) >> new GroovyRowResult(["pidm": "11111", "cccid": cccId])
        1 * sql.firstRow("misEnrollment", *_) >> new GroovyRowResult(["campus": "ABC"])
        1 * sql.firstRow("misCode", *_) >> null

        thrown InvalidRequestException

        when:
        Student.getHomeCollege(misCode, cccId)

        then:
        1 * sql.firstRow("cccid", *_) >> new GroovyRowResult(["pidm": "11111", "cccid": cccId])
        1 * sql.firstRow("misEnrollment", *_) >> null
        1 * sql.firstRow("appStatus", *_) >> null


        thrown InvalidRequestException

        when:
        def result = Student.getHomeCollege(misCode, cccId)

        then:
        1 * sql.firstRow("cccid", *_) >> new GroovyRowResult(["pidm": "11111", "cccid": cccId])
        1 * sql.firstRow("misEnrollment", *_) >> new GroovyRowResult(["campus": "ABC"])
        1 * sql.firstRow("misCode", *_) >> new GroovyRowResult(["misCode": misCode])
        result.misCode == misCode
        result.cccid == cccId

        when:
        result = Student.getHomeCollege(misCode, cccId)

        then:
        1 * sql.firstRow("cccid", *_) >> new GroovyRowResult(["pidm": "11111", "cccid": cccId])
        1 * sql.firstRow("misEnrollment", *_) >> null
        1 * sql.firstRow("appStatus", *_) >> new GroovyRowResult(["campus": "ABC"])
        1 * sql.firstRow("misCode", *_) >> new GroovyRowResult(["misCode": misCode])
        result.misCode == misCode
        result.cccid == cccId
    }

    def "get" () {
        setup:
        environment.getProperty("banner.student.getQuery") >> "student"
        environment.getProperty("banner.term.getQuery") >> "term"
        environment.getProperty("banner.student.orientation.getExemptQuery") >> "orientation :sisTermId"
        environment.getProperty("banner.student.orientation.getQuery") >> "orientation"
        environment.getProperty("banner.student.test.getQuery") >> "test"
        environment.getProperty("banner.student.educationPlan.getQuery") >> "edplan"
        environment.getProperty("banner.student.residency.getQuery") >> "residency"
        environment.getProperty("banner.student.residentStatus.Resident") >> "C"
        environment.getProperty("banner.student.residentStatus.NonResident") >> "D"
        environment.getProperty("banner.student.residentStatus.AB540") >> "A"
        environment.getProperty("banner.student.applicationStatus.getQuery") >> "application"
        environment.getProperty("banner.student.applicationStatus.ApplicationDenied") >> "D"
        environment.getProperty("banner.student.applicationStatus.ApplicationAccepted") >> "A"
        environment.getProperty("banner.student.applicationStatus.ApplicationPending") >> "P"
        environment.getProperty("banner.student.registration.getQuery") >> "registration"
        environment.getProperty("banner.student.disability.getQuery") >> "disability"
        environment.getProperty("banner.student.hold.getQuery") >> "hold"
        environment.getProperty("banner.student.visa.getQuery") >> "visa"
        environment.getProperty("banner.student.balance.getQuery") >> "balance"
        environment.getProperty("banner.student.getTerm") >> "getTerm"
        environment.getProperty("banner.student.attributes.getQuery") >> "attributes"
        environment.getProperty("banner.student.incarcerated") >> "I"
        environment.getProperty("banner.student.concurrentlyEnrolled") >> "HS"
        environment.getProperty("banner.student.finaid.getQuery") >> "finaid"
        environment.getProperty("banner.student.bogfw") >> "BOG"
        environment.getProperty("banner.student.address.getQuery") >> "address"
        environment.getProperty("banner.student.test.getQuery") >> "test"
        environment.getProperty("banner.student.math") >> "MATH"
        environment.getProperty("banner.student.english") >> "ENG"
        environment.getProperty("banner.student.cohort.getAllQuery") >> "cohort"
        environment.getProperty("banner.student.cohort.courseExchange") >> "CE"

        def regDate = new Date(1599541200000)
        def map = ["pidm": "1111", "cccId": cccId, "sisPersonId": "S111111111"]
        def record = new GroovyRowResult(map)
        def term = new GroovyRowResult(["sisTermId": sisTermId])
        def residency = new GroovyRowResult(["residentStatus": "C"])
        def appStatus = new GroovyRowResult(["applicationDecision": "D"])
        def registration = new GroovyRowResult(["registrationStartDate": regDate, "registrationStartTime": null])
        def disability = new GroovyRowResult(["dspsEligible": "Y"])
        def edplan = new GroovyRowResult(["educationPlan": "Y"])
        def hold = new GroovyRowResult(["hold": "Y"])
        def visa = new GroovyRowResult(["visaType": "111"])
        def orientation = new GroovyRowResult(["orientation": "Y"])
        def balance = new GroovyRowResult(["accountBalance": 100])
        def attributes = [new GroovyRowResult(["attribute":"I"]),new GroovyRowResult(["attribute":"HS"]), new GroovyRowResult(["attribute":"BS"])]
        def finaid = [new GroovyRowResult(["fundCode": "BOG", "amount":100]),new GroovyRowResult(["fundCode":"FIN", "amount": 100])]
        def addresses = [new GroovyRowResult(["state":"CA"])]
        def tests = [new GroovyRowResult(["test":"ENG"]),new GroovyRowResult(["test":"MATH"])]
        def cohorts = [new GroovyRowResult(["name": "CE"])]


        when:
        def result = Student.get(misCode, cccId, sisTermId)

        then:
        1 * sql.firstRow("student", *_) >> record
        1 * sql.firstRow("edplan", *_) >> edplan
        1 * sql.firstRow("term", *_) >> term
        1 * sql.firstRow("residency", *_) >> residency
        1 * sql.firstRow("application", *_) >> appStatus
        1 * sql.firstRow("registration",*_) >> registration
        1 * sql.firstRow("orientation",*_) >> orientation
        1 * sql.firstRow("disability",*_) >> disability
        1 * sql.firstRow("hold",*_) >> hold
        1 * sql.firstRow("visa", *_) >> visa
        1 * sql.firstRow("balance",*_) >> balance
        1 * sql.rows("attributes",*_) >> attributes
        1 * sql.rows("finaid",*_) >> finaid
        1 * sql.rows("address",*_) >> addresses
        1 * sql.rows("test",*_) >> tests
        1 * sql.rows("cohort",*_) >> cohorts
        result.misCode == misCode
        result.cccid == cccId
        result.accountBalance == 100
        result.applicationStatus == ApplicationStatus.ApplicationDenied
        result.hasEducationPlan
        result.hasHold
        result.orientationStatus == OrientationStatus.COMPLETED
        result.visaType == "111"
        result.dspsEligible
        result.residentStatus == ResidentStatus.Resident
        result.registrationDate.compareTo(regDate) == 0
        result.incarcerated
        result.concurrentlyEnrolled
        result.hasBogfw
        result.hasFinancialAidAward
        result.hasCaliforniaAddress
        result.hasEnglishAssessment
        result.hasMathAssessment
        result.cohorts[0].getName() == CohortTypeEnum.COURSE_EXCHANGE

        when:
        result = Student.get(misCode, cccId, sisTermId)

        then:
        1 * sql.firstRow("student", *_) >> record
        1 * sql.firstRow("edplan", *_) >> null
        1 * sql.firstRow("term", *_) >> term
        1 * sql.firstRow("residency", *_) >> new GroovyRowResult(["residentStatus": "D"])
        1 * sql.firstRow("application", *_) >> new GroovyRowResult(["applicationDecision": "P"])
        1 * sql.firstRow("registration",*_) >> new GroovyRowResult(["registrationStartDate": new Date(1599570000000)])
        1 * sql.firstRow("orientation",*_) >> new GroovyRowResult(["orientation": "N"])
        1 * sql.firstRow("orientation :sisTermId",*_) >> new GroovyRowResult(["orientationExempt": "Y"])
        1 * sql.firstRow("disability",*_) >> disability
        1 * sql.firstRow("hold",*_) >> hold
        1 * sql.firstRow("visa", *_) >> visa
        1 * sql.firstRow("balance",*_) >> balance
        1 * sql.rows("attributes",*_) >> [new GroovyRowResult(["attribute":"BS"])]
        1 * sql.rows("finaid",*_) >> [new GroovyRowResult(["fundCode": "BOG", "amount":0]),new GroovyRowResult(["fundCode":"FIN", "amount": 100])]
        1 * sql.rows("address",*_) >> [new GroovyRowResult(["state":"ND"])]
        1 * sql.rows("test",*_) >> [new GroovyRowResult(["test":"BIO"]),new GroovyRowResult(["test":"ACCT"])]
        1 * sql.rows("cohort",*_) >> [new GroovyRowResult(["name": "C"])]
        result.misCode == misCode
        result.cccid == cccId
        result.accountBalance == 100
        result.applicationStatus == ApplicationStatus.ApplicationPending
        !result.hasEducationPlan
        result.hasHold
        result.orientationStatus == OrientationStatus.OPTIONAL
        result.visaType == "111"
        result.dspsEligible
        result.residentStatus == ResidentStatus.NonResident
        result.registrationDate.compareTo(new Date(1599570000000)) == 0
        !result.incarcerated
        !result.concurrentlyEnrolled
        !result.hasBogfw
        result.hasFinancialAidAward
        !result.hasCaliforniaAddress
        !result.hasEnglishAssessment
        !result.hasMathAssessment

        when:
        result = Student.get(misCode, cccId, sisTermId)

        then:
        1 * sql.firstRow("student", *_) >> record
        1 * sql.firstRow("edplan", *_) >> edplan
        1 * sql.firstRow("term", *_) >> term
        1 * sql.firstRow("residency", *_) >> new GroovyRowResult(["residentStatus": "A"])
        1 * sql.firstRow("application", *_) >> new GroovyRowResult(["applicationDecision": "A"])
        1 * sql.firstRow("registration",*_) >> new GroovyRowResult(["registrationStartDate": new Date(1599571800000)])
        1 * sql.firstRow("orientation",*_) >> null
        1 * sql.firstRow("disability",*_) >> disability
        1 * sql.firstRow("hold",*_) >> hold
        1 * sql.firstRow("visa", *_) >> visa
        1 * sql.firstRow("balance",*_) >> balance
        1 * sql.rows("finaid",*_) >> [new GroovyRowResult(["fundCode": "BOG", "amount":0]),new GroovyRowResult(["fundCode":"FIN", "amount": 0])]
        result.misCode == misCode
        result.cccid == cccId
        result.accountBalance == 100
        result.applicationStatus == ApplicationStatus.ApplicationAccepted
        result.hasEducationPlan
        result.hasHold
        result.orientationStatus == OrientationStatus.REQUIRED
        result.visaType == "111"
        result.dspsEligible
        result.residentStatus == ResidentStatus.AB540
        result.registrationDate.compareTo(new Date(1599571800000)) == 0

        when:
        result = Student.get(misCode, cccId, sisTermId)

        then:
        1 * sql.firstRow("student", *_) >> record
        1 * sql.firstRow("edplan", *_) >> edplan
        1 * sql.firstRow("term", *_) >> term
        1 * sql.firstRow("residency", *_) >> new GroovyRowResult(["residentStatus": "X"])
        1 * sql.firstRow("application", *_) >> new GroovyRowResult(["applicationDecision": null])
        1 * sql.firstRow("registration",*_) >> registration
        1 * sql.firstRow("orientation",*_) >> orientation
        1 * sql.firstRow("disability",*_) >> new GroovyRowResult(["dspsEligible": "N"])
        1 * sql.firstRow("hold",*_) >> hold
        1 * sql.firstRow("visa", *_) >> visa
        1 * sql.firstRow("balance",*_) >> null
        result.misCode == misCode
        result.cccid == cccId
        result.accountBalance == 0
        result.applicationStatus == ApplicationStatus.ApplicationPending
        result.hasEducationPlan
        result.hasHold
        result.orientationStatus == OrientationStatus.COMPLETED
        result.visaType == "111"
        !result.dspsEligible
        result.residentStatus == ResidentStatus.NonResident
        result.registrationDate.compareTo(regDate) == 0

        when:
        result = Student.get(misCode, cccId, null)

        then:
        1 * sql.firstRow("student", *_) >> record
        1 * sql.firstRow("edplan", *_) >> edplan
        1 * sql.firstRow("getTerm", *_) >> term
        1 * sql.firstRow("residency", *_) >> null
        1 * sql.firstRow("application", *_) >> new GroovyRowResult(["applicationDecision": "X"])
        1 * sql.firstRow("registration",*_) >> registration
        1 * sql.firstRow("orientation",*_) >> orientation
        1 * sql.firstRow("disability",*_) >> null
        1 * sql.firstRow("hold",*_) >> hold
        1 * sql.firstRow("visa", *_) >> visa
        1 * sql.firstRow("balance",*_) >> new GroovyRowResult(["accountBalance": null])
        result.misCode == misCode
        result.cccid == cccId
        result.accountBalance == 0
        result.applicationStatus == ApplicationStatus.NoApplication
        result.hasEducationPlan
        result.hasHold
        result.orientationStatus == OrientationStatus.COMPLETED
        result.visaType == "111"
        !result.dspsEligible
        result.residentStatus == ResidentStatus.NonResident
        result.registrationDate.compareTo(regDate) == 0

        when:
        result = Student.get(misCode, cccId, null)

        then:
        1 * sql.firstRow("student", *_) >> record
        1 * sql.firstRow("edplan", *_) >> edplan
        1 * sql.firstRow("getTerm", *_) >> term
        1 * sql.firstRow("residency", *_) >> null
        1 * sql.firstRow("application", *_) >> null
        1 * sql.firstRow("registration",*_) >> registration
        1 * sql.firstRow("orientation",*_) >> orientation
        1 * sql.firstRow("disability",*_) >> null
        1 * sql.firstRow("hold",*_) >> hold
        1 * sql.firstRow("visa", *_) >> visa
        1 * sql.firstRow("balance",*_) >> new GroovyRowResult(["accountBalance": null])
        result.misCode == misCode
        result.cccid == cccId
        result.accountBalance == 0
        result.applicationStatus == ApplicationStatus.NoApplication
        result.hasEducationPlan
        result.hasHold
        result.orientationStatus == OrientationStatus.COMPLETED
        result.visaType == "111"
        !result.dspsEligible
        result.residentStatus == ResidentStatus.NonResident
        result.registrationDate.compareTo(regDate) == 0

        when:
        Student.get(misCode, cccId, sisTermId)

        then:
        1 * sql.firstRow("student", *_) >> null
        thrown EntityNotFoundException

        when:
        Student.get(misCode, cccId, sisTermId)

        then:
        1 * sql.firstRow("student", *_) >> record
        1 * sql.firstRow("term", *_) >> null
        thrown InvalidRequestException
    }

    def "post cohort" () {

        setup:
        environment.getProperty("banner.cccid.getQuery") >> "cccid"
        environment.getProperty("banner.term.getQuery") >> "term"
        environment.getProperty("banner.student.cohort.getQuery") >> "cohort"
        environment.getProperty("banner.student.cohort.courseExchange") >> "ABC"
        environment.getProperty("banner.student.cohort.insertQuery") >> "insert"

        when:
        Student.postCohort(cccId, CohortTypeEnum.COURSE_EXCHANGE, misCode, sisTermId)

        then:
        thrown InvalidRequestException

        when:
        Student.postCohort(cccId, CohortTypeEnum.COURSE_EXCHANGE, misCode, sisTermId)

        then:
        1 * sql.firstRow("cccid", *_) >> new GroovyRowResult(["pidm": 11111])
        thrown InvalidRequestException

        when:
        Student.postCohort(cccId, CohortTypeEnum.COURSE_EXCHANGE, misCode, sisTermId)

        then:
        1 * sql.firstRow("cccid", *_) >> new GroovyRowResult(["pidm": 11111])
        1 * sql.firstRow("term", *_) >> new GroovyRowResult(["sisTermId": sisTermId])
        1 * sql.firstRow("cohort", *_) >> new GroovyRowResult(["name": "ABC"])
        thrown EntityConflictException

        when:
        def result = Student.postCohort(cccId, CohortTypeEnum.COURSE_EXCHANGE, misCode, sisTermId)

        then:
        1 * sql.firstRow("cccid", *_) >> new GroovyRowResult(["pidm": 11111])
        1 * sql.firstRow("term", *_) >> new GroovyRowResult(["sisTermId": sisTermId])
        1 * sql.firstRow("cohort", *_) >> null
        1 * sql.execute("insert", *_) >> { arguments ->
            assert arguments[0] == "insert"
            HashMap params = arguments[1]
            assert params.pidm == 11111
            assert params.sisTermId == sisTermId
            assert params.name == "ABC"
            assert params.misCode == misCode
        }



    }

    def "delete cohort" () {

        setup:
        environment.getProperty("banner.cccid.getQuery") >> "cccid"
        environment.getProperty("banner.term.getQuery") >> "term"
        environment.getProperty("banner.student.cohort.getQuery") >> "cohort"
        environment.getProperty("banner.student.cohort.courseExchange") >> "ABC"
        environment.getProperty("banner.student.cohort.deleteQuery") >> "delete"

        when:
        Student.deleteCohort(cccId, CohortTypeEnum.COURSE_EXCHANGE, misCode, sisTermId)

        then:
        thrown InvalidRequestException

        when:
        Student.deleteCohort(cccId, CohortTypeEnum.COURSE_EXCHANGE, misCode, sisTermId)

        then:
        1 * sql.firstRow("cccid", *_) >> new GroovyRowResult(["pidm": 11111])
        thrown InvalidRequestException

        when:
        Student.deleteCohort(cccId, CohortTypeEnum.COURSE_EXCHANGE, misCode, sisTermId)

        then:
        1 * sql.firstRow("cccid", *_) >> new GroovyRowResult(["pidm": 11111])
        1 * sql.firstRow("term", *_) >> new GroovyRowResult(["sisTermId": sisTermId])
        1 * sql.firstRow("cohort", *_) >> null
        thrown EntityNotFoundException

        when:
        Student.deleteCohort(cccId, CohortTypeEnum.COURSE_EXCHANGE, misCode, sisTermId)

        then:
        1 * sql.firstRow("cccid", *_) >> new GroovyRowResult(["pidm": 11111])
        1 * sql.firstRow("term", *_) >> new GroovyRowResult(["sisTermId": sisTermId])
        1 * sql.firstRow("cohort", *_) >> new GroovyRowResult(["name": "ABC"])
        1 * sql.execute("delete", *_) >> { arguments ->
            assert arguments[0] == "delete"
            HashMap params = arguments[1]
            assert params.pidm == 11111
            assert params.sisTermId == sisTermId
            assert params.name == "ABC"
            assert params.misCode == misCode
        }



    }

    def "get cccids" () {
        setup:
        environment.getProperty("banner.person.cccidList.getQuery") >> "cccids"
        environment.getProperty("banner.person.sisPersonId.getQuery") >> "select something from somewhere where something :inList"

        when:
        Student.getStudentCCCIds(misCode, "A111111")

        then:
        1 * sql.rows("select something from somewhere where something ( :personId0 )", *_ ) >> null
        thrown InvalidRequestException

        when:
        def result = Student.getStudentCCCIds(misCode, "A111111")

        then:
        1 * sql.rows("select something from somewhere where something ( :personId0 )", *_ ) >> [new GroovyRowResult(["sisPersonId":"A111111"])]
        1 * sql.rows("cccids", *_) >> [new GroovyRowResult(["cccid": cccId]), new GroovyRowResult(["cccid": "ABC1234"])]
        result.size() == 2
    }

    def "post cccids" () {
        setup:
        environment.getProperty("banner.person.cccidList.getQuery") >> "cccids"
        environment.getProperty("banner.person.sisPersonId.getQuery") >> "select something from somewhere where something :inList"
        environment.getProperty("banner.person.cccid.insertQuery") >> "insert"

        when:
        Student.postStudentCCCId(misCode, "A111111", cccId)

        then:
        1 * sql.rows("select something from somewhere where something ( :personId0 )", *_ ) >> [new GroovyRowResult(["pidm": 11111, "sisPersonId":"A111111"])]
        1 * sql.rows("cccids", *_) >> [new GroovyRowResult(["cccid": cccId])]
        thrown EntityConflictException

        when:
        Student.postStudentCCCId(misCode, "A111111", cccId)

        then:
        3 * sql.rows("select something from somewhere where something ( :personId0 )", *_ ) >> [new GroovyRowResult(["pidm": 11111, "sisPersonId":"A111111"])]
        2 * sql.rows("cccids", *_) >>> null
        1 * sql.execute("insert", *_) >> { arguments ->
            HashMap params = arguments[1]
            assert params.pidm == 11111
            assert params.cccId == cccId
        }
    }

    def "patch exceptions" () {
        setup:
        updates = new com.ccctc.adaptor.model.Student()
        updates.applicationStatus = appStatus
        updates.hasEducationPlan = edPlan
        updates.orientationStatus = orientStatus

        when:
        Student.patch(thisMisCode, thisCccId, thisSisTermId, updates)

        then:
        Exception e = thrown()
        e instanceof InvalidRequestException
        e.message == errMsg

        where:
        thisMisCode | thisCccId | thisSisTermId | appStatus | edPlan | orientStatus                 | error                   | errMsg
        null        | null      | null          | null      | null   | null                         | InvalidRequestException | "patch: misCode cannot be null or blank"
        misCode     | null      | null          | null      | null   | null                         | InvalidRequestException | "patch: cccId cannot be null or blank"
        misCode     | cccId     | sisTermId     | ApplicationStatus.NoApplication | null   | null   | InvalidRequestException | "patch: updating Application/Admissions status is not supported"
        misCode     | cccId     | sisTermId     | null      | true   | null                         | InvalidRequestException | "patch: updating hasEducationPlan is not supported"
    }

    def "patch null object" () {
        when:
        Student.patch(misCode, cccId, sisTermId, updates)

        then:
        Exception e = thrown()
        e instanceof InvalidRequestException
        e.message == "patch: Student updates cannot be null or blank"
    }

    def "patch bad student" () {
        setup:
        updates = new com.ccctc.adaptor.model.Student()
        environment.getProperty("banner.cccid.getQuery") >> "student"

        when:
        Student.patch(misCode, cccId, sisTermId, updates)

        then:
        1 * sql.firstRow("student", *_ ) >> null
        Exception e = thrown()
        e instanceof EntityNotFoundException
        e.message == "Student not found"
    }

    def "patch term not found" () {
        setup:
        updates = new com.ccctc.adaptor.model.Student()
        environment.getProperty("banner.cccid.getQuery") >> "student"
        environment.getProperty("banner.student.getTerm") >> "term"

        when:
        Student.patch(misCode, cccId, null, updates)

        then:
        1 * sql.firstRow("student", *_ ) >> new GroovyRowResult(["cccId": "111", "pidm": 11111])
        1 * sql.firstRow("term", *_ ) >> null
        Exception e = thrown()
        e instanceof InvalidRequestException
        e.message == "sisTermId not provided and could not be defaulted"
    }

    def "patch reset not supported" () {
        setup:
        updates = new com.ccctc.adaptor.model.Student()
        updates.sisTermId = sisTermId
        updates.orientationStatus = OrientationStatus.REQUIRED
        environment.getProperty("banner.cccid.getQuery") >> "student"
        environment.getProperty("banner.student.getTerm") >> "term"

        when:
        Student.patch(misCode, cccId, sisTermId, updates)

        then:
        1 * sql.firstRow("student", *_ ) >> new GroovyRowResult(["cccId": "111", "pidm": 11111])
        Exception e = thrown()
        e instanceof InvalidRequestException
        e.message == "Updating orientation status to given value unsupported"
    }

    def "patch" () {
        setup:
        updates = new com.ccctc.adaptor.model.Student()
        updates.sisTermId = sisTermId
        updates.orientationStatus = OrientationStatus.COMPLETED
        environment.getProperty("banner.cccid.getQuery") >> "student"
        environment.getProperty("banner.student.getTerm") >> "term"
        environment.getProperty("banner.orientationStatus.value") >> "ABC"
        environment.getProperty("banner.orientationOrigCode.value") >> "123"
        environment.getProperty("banner.orientationDataOrigin.value") >> "TEST"
        environment.getProperty("banner.orientation.insertQuery") >> "insert"

        when:
        Student.patch(misCode, cccId, sisTermId, updates)

        then:
        1 * sql.firstRow("student", *_ ) >> new GroovyRowResult(["cccId": "111", "pidm": 11111])
        1 * sql.firstRow(*_) >> new GroovyRowResult(["orientSequence": 1])
        1 * sql.execute(*_) >> { arguments ->
                assert arguments[0] == "insert"
                HashMap map = arguments[1]
                assert map.orientSequence == 1
                assert map.sisTermId == sisTermId
                assert map.misCode == misCode
                assert map.pidm == 11111
                assert map.orientationCode == "ABC"
                assert map.orientationOrigCode == "123"
                assert map.orientationDataOrigin == "TEST"
            }

        when:
        Student.patch(misCode, cccId, null, updates)

        then:
        1 * sql.firstRow("student", *_ ) >> new GroovyRowResult(["cccId": "111", "pidm": 11111])
        1 * sql.firstRow(*_) >> new GroovyRowResult(["orientSequence": 1])
        1 * sql.execute(*_) >> { arguments ->
            assert arguments[0] == "insert"
            HashMap map = arguments[1]
            assert map.orientSequence == 1
            assert map.sisTermId == sisTermId
            assert map.misCode == misCode
            assert map.pidm == 11111
            assert map.orientationCode == "ABC"
            assert map.orientationOrigCode == "123"
            assert map.orientationDataOrigin == "TEST"
        }
    }

    def "patch derive term" () {
        setup:
        updates = new com.ccctc.adaptor.model.Student()
        updates.orientationStatus = OrientationStatus.REQUIRED
        environment.getProperty("banner.cccid.getQuery") >> "student"
        environment.getProperty("banner.student.getTerm") >> "term"
        environment.getProperty("banner.orientationStatus.value") >> "ABC"
        environment.getProperty("banner.orientationOrigCode.value") >> "123"
        environment.getProperty("banner.orientationDataOrigin.value") >> "TEST"
        environment.getProperty("banner.orientation.deleteQuery") >> "delete"

        when:
        Student.patch(misCode, cccId, null, updates)

        then:
        1 * environment.getProperty("banner.student.allowOrientationReset") >> "1"
        1 * sql.firstRow("student", *_) >> new GroovyRowResult(["cccId": "111", "pidm": 11111])
        1 * sql.firstRow("term") >> new GroovyRowResult(["sisTermId": sisTermId])
        1 * sql.execute(*_) >> { arguments ->
            assert arguments[0] == "delete"
            HashMap map = arguments[1]
            assert map.sisTermId == sisTermId
            assert map.pidm == 11111
        }
    }

    def "getAllByTerm" () {

        setup:
        environment.getProperty("banner.student.getTerm") >> "terms"
        environment.getProperty("banner.student.getAllTerms.getIdentifiersRecords") >> "identifiers"
        environment.getProperty("banner.student.getAllTerms.buildIdentifiersRecords") >> "identifiers"
        environment.getProperty("banner.student.getAllTerms.buildOrientationRecords") >> "orientations"
        environment.getProperty("banner.student.getAllTerms.buildEdPlanRecords") >> "edplans"
        environment.getProperty("banner.student.getAllTerms.buildAppStatusRecords") >> "appstatus"
        environment.getProperty("banner.student.getAllTerms.getMatriculationRecords") >> "records"
        environment.getProperty("banner.student.getAllTerms.getAllRecords") >> "records"

        when:
        def result = Student.getAllByTerm(misCode, null, StudentFieldSet.IDENTIFIERS)

        then:
        1 * sql.firstRow("terms", *_) >> new GroovyRowResult(["sisTermId": sisTermId])
        1 * sql.rows("identifiers", *_) >> [new GroovyRowResult(["cccid": cccId, "sisPersonId": "A11111", "sisTermId": sisTermId])]

        when:
        result = Student.getAllByTerm(null, null, StudentFieldSet.IDENTIFIERS)

        then:
        thrown InvalidRequestException

        when:
        result = Student.getAllByTerm(misCode, null, StudentFieldSet.IDENTIFIERS)

        then:
        thrown InvalidRequestException

        when:
        result = Student.getAllByTerm(misCode, [sisTermId], StudentFieldSet.IDENTIFIERS)

        then:
        1 * sql.rows("identifiers", *_) >> [new GroovyRowResult(["cccid": cccId, "sisPersonId": "A11111", "sisTermId": sisTermId])]

        when:
        result = Student.getAllByTerm(misCode, [sisTermId, '202020'], StudentFieldSet.MATRICULATION)

        then:
        1 * sql.execute("identifiers", *_) >> { arguments ->
            assert arguments[0] == "identifiers"
            HashMap map = arguments[1]
            assert map.sisTermId1 == sisTermId
        }
        1 * sql.execute("orientations",*_) >> { arguments ->
            assert arguments[0] == "orientations"
        }
        1 * sql.execute("edplans", *_) >> { arguments ->
            assert arguments[0] == "edplans"
        }
        1 * sql.execute("appstatus", *_) >> { arguments ->
            assert arguments[0] == "appstatus"
        }
        1 * sql.rows("records", *_) >> [new GroovyRowResult(["cccid": cccId, "sisPersonId": "A11111", "hasEducationPlan": true, "OrientationStatus": "COMPLETED", "ApplicationStatus": "ApplicationAccepted"]),
                new GroovyRowResult(["cccid": cccId, "sisPersonId": "A11111", "hasEducationPlan": true, "OrientationStatus": "RESTRICTED", "ApplicationStatus": "ApplicationDenied"]),
                new GroovyRowResult(["cccid": cccId, "sisPersonId": "A11111", "hasEducationPlan": null, "OrientationStatus": null, "ApplicationStatus": null, registrationDate: new Date()])]

        when:
        result = Student.getAllByTerm(misCode, [sisTermId, '202020', '202030'], StudentFieldSet.ALL)

        then:
        1 * sql.execute("identifiers", *_) >> { arguments ->
            assert arguments[0] == "identifiers"
            HashMap map = arguments[1]
            assert map.sisTermId1 == sisTermId
        }
        1 * sql.execute("orientations",*_) >> { arguments ->
            assert arguments[0] == "orientations"
        }
        1 * sql.execute("edplans", *_) >> { arguments ->
            assert arguments[0] == "edplans"
        }
        1 * sql.execute("appstatus", *_) >> { arguments ->
            assert arguments[0] == "appstatus"
        }
        1 * sql.rows("records", *_) >> [new GroovyRowResult(["cccid": cccId, "sisPersonId": "A11111", "hasEducationPlan": true, "OrientationStatus": "COMPLETED", "ApplicationStatus": "ApplicationAccepted"]),
                                        new GroovyRowResult(["cccid": cccId, "sisPersonId": "A11111", "hasEducationPlan": true, "OrientationStatus": "RESTRICTED", "ApplicationStatus": "ApplicationDenied"]),
                                        new GroovyRowResult(["cccid": cccId, "sisPersonId": "A11111", "hasEducationPlan": null, "OrientationStatus": null, "ApplicationStatus": null, registrationDate: new Date()])]

    }

}
