package api.banner

import com.ccctc.adaptor.exception.EntityConflictException
import com.ccctc.adaptor.exception.EntityNotFoundException
import com.ccctc.adaptor.exception.InternalServerException
import com.ccctc.adaptor.model.apply.InternationalApplication
import com.ccctc.adaptor.model.apply.SupplementalQuestions
import groovy.sql.GroovyRowResult
import groovy.sql.Sql
import org.springframework.core.env.Environment
import spock.lang.Specification

import java.sql.Timestamp
import java.time.LocalDate
import java.time.LocalDateTime

class InternationalApplySpec extends Specification {

    def misCode = "000"
    InternationalApply InternationalApply
    Sql sql
    Environment environment

    def setup() {
        sql = Mock(Sql)
        environment = Mock(Environment)

        InternationalApply = Spy(new api.banner.InternationalApply())
        InternationalApply.environment = environment

        GroovyMock(BannerConnection, global: true)
        BannerConnection.getSession(*_) >> sql
    }

    def "get"() {
        setup:
        def map = new HashMap()
        map.put("appId", 1)

        def record = new GroovyRowResult(map)

        when:
        def result = InternationalApply.get(misCode, 1)

        then:
        1 * sql.firstRow(*_) >> record
        result.appId == 1
        result.supplementalQuestions.appId == 1

    }

    def "get not found"() {
        when:
        InternationalApply.get(misCode, 1)

        then:
        1 * sql.firstRow(*_) >> null
        thrown EntityNotFoundException
    }

    def "postEntityExists"(){
        setup:
        def map = new HashMap()
        map.put("appId", 1)

        def record = new GroovyRowResult(map)

        def InternationalApplication = new InternationalApplication()
        InternationalApplication.appId = 1

        when:
        InternationalApply.post(misCode, InternationalApplication)

        then:
        1 * sql.firstRow(*_) >> record
        thrown EntityConflictException
    }

    def "post - error on sql call"(){
        setup:
        def application = new InternationalApplication()
        application.appId = 1

        when:
        InternationalApply.post(misCode, application)

        then:
        1 * sql.firstRow(*_) >> null
        1 * sql.execute(*_) >> { throw new Exception("oops") }
        thrown InternalServerException
    }

    def "post - error on get"(){
        setup:
        def InternationalApplication = new InternationalApplication()
        InternationalApplication.appId = 1

        when:
        InternationalApply.post(misCode, InternationalApplication)

        then:
        1 * sql.firstRow(*_) >> null
        1 * sql.execute(*_)
        1 * sql.firstRow(*_) >> null
        thrown InternalServerException
    }

    def "createApply"(){
        setup:
        def map = new HashMap()
        map.put("appId", 1)
        map.put("higherCompDate", LocalDate.of(2018,01,10))
        def record = new GroovyRowResult(map)
        def InternationalApplication = new InternationalApplication()
        InternationalApplication.appId = 1

        when:
        InternationalApply.createInternationalApply(misCode, InternationalApplication, sql)

        then:
        sql.firstRow(*_) >> record
    }

    def "mapValues"() {
        setup:
        def map = [
                appId: 1,
                citizenshipStatus: "X",
                addressVerified: "true",
                cccId: "ABC1234",
                appLang: "EN",
                col1City: "Manchester United",
                authorizeAgentInfoRelease: "true",
                cryptokeyid: 1233,
                hsCompDate: Timestamp.valueOf(LocalDateTime.of(2001, 1, 1, 0, 0, 0)),
                tstmpSubmit: Timestamp.valueOf(LocalDateTime.of(2001, 1, 1, 12, 45, 1)),
                tstmpCreate: "2001-01-01 12:45:01",
                emergContactAddressVerified: "Y",
                emergContactNonUsPostalCode: "12345",
                nonUsPrmntHomeProvince: "PASS TEST",
                nonUsPrmntHomePostalCode: "44423232",
                nonUsPrmntHomeAddrVerified: "true",
                agentEmail: "bob@bob.com"
        ]

        def map2 = [
                appId: 1,
                suppText01: "text",
                suppCheck01: "Y",
                suppCheck02: "N",
                suppCheck03: "X"
        ]

        def applicationRecord = new GroovyRowResult(map)
        def supplementalRecord = new GroovyRowResult(map2)


        when:
        def a = new InternationalApplication()
        def b = new SupplementalQuestions()

        InternationalApply.mapValues(InternationalApply.appProperties, applicationRecord, a)
        InternationalApply.mapValues(InternationalApply.supProperties, supplementalRecord, b)
        a.supplementalQuestions = b

        then:
        a.appId == 1
        a.citizenshipStatus == "X"
        a.addressVerified == true
        a.cccId == "ABC1234"
        a.appLang == "EN"
        a.col1City == "Manchester United"
        a.authorizeAgentInfoRelease == true
        a.cryptokeyid == 1233
        a.hsCompDate == LocalDate.of(2001, 1, 1)
        a.tstmpSubmit == LocalDateTime.of(2001, 1, 1, 12, 45, 1)
        a.tstmpSubmit == a.tstmpCreate
        a.emergencyContactAddressVerified == true
        a.emergencyContactNonUsPostalCode == "12345"
        a.nonUsPermanentHomeNonUsProvince  == "PASS TEST"
        a.nonUsPermanentHomeNonUsPostalCode == "44423232"
        a.nonUsPermanentHomeAddressVerified == true
        a.agentEmail == "bob@bob.com"

        a.supplementalQuestions.appId == 1
        a.supplementalQuestions.suppText01 == "text"
        a.supplementalQuestions.suppCheck01 == true
        a.supplementalQuestions.suppCheck02 == false
        a.supplementalQuestions.suppCheck03 == null

    }
}

