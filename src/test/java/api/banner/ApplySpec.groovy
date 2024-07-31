package api.banner

import com.ccctc.adaptor.exception.EntityConflictException
import com.ccctc.adaptor.exception.EntityNotFoundException
import com.ccctc.adaptor.exception.InternalServerException
import com.ccctc.adaptor.model.apply.Application
import com.ccctc.adaptor.model.apply.SupplementalQuestions
import groovy.sql.GroovyRowResult
import groovy.sql.Sql
import org.springframework.core.env.Environment
import spock.lang.Specification

import java.sql.Timestamp
import java.time.LocalDate
import java.time.LocalDateTime

class ApplySpec extends Specification {

    def misCode = "000"
    Apply apply
    Sql sql
    Environment environment

    def setup() {
        sql = Mock(Sql)
        environment = Mock(Environment)

        apply = Spy(new Apply())
        apply.environment = environment

        GroovyMock(BannerConnection, global: true)
        BannerConnection.getSession(*_) >> sql
    }

    def "get"() {
        setup:
        def map = new HashMap()
        map.put("appId", 1L)

        def record = new GroovyRowResult(map)
        def record2 = new GroovyRowResult(map)

        when:
        def result = apply.get(misCode, 1)

        then:
        1 * sql.firstRow(*_) >>> [record, record2]
        result.appId == 1
        result.supplementalQuestions.appId == 1
    }

    def "get not found"() {
        when:
        apply.get(misCode, 1)

        then:
        1 * sql.firstRow(*_) >> null
        thrown EntityNotFoundException
    }

    def "postEntityExists"(){
        setup:
        def map = new HashMap()
        map.put("appId", 1)

        def record = new GroovyRowResult(map)

        def application = new Application()
        application.appId = 1

        when:
        apply.post(misCode, application)

        then:
        1 * sql.firstRow(*_) >> record
        thrown EntityConflictException
    }

    def "post - error on sql call"(){
        setup:
        def application = new Application()
        application.appId = 1

        when:
        apply.post(misCode, application)

        then:
        1 * sql.firstRow(*_) >> null
        1 * sql.execute(*_) >> { throw new Exception("oops") }
        thrown InternalServerException
    }

    def "post - error on get"(){
        setup:
        def application = new Application()
        application.appId = 1

        when:
        apply.post(misCode, application)

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
        def application = new Application()
        application.appId = 1

        when:
        apply.createApply(misCode, application, sql)

        then:
        sql.firstRow(*_) >> record
    }

    def "mapValues"() {
        setup:
        def map = [
                appId: 1,
                intendedMajor: "major",
                finAidRef: "Y",
                nonUsAddress: "N",
                addressSame: "X", // invalid, should map to null
                hsCompDate: Timestamp.valueOf(LocalDateTime.of(2001, 1, 1, 0, 0, 0)),
                higherCompDate: "2001-01-01",
                hsAttendance: 1,
                tstmpSubmit: Timestamp.valueOf(LocalDateTime.of(2001, 1, 1, 12, 45, 1)),
                tstmpCreate: "2001-01-01 12:45:01",
                invalidField: "invalid - will be ignored"
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
        def a = new Application()
        def b = new SupplementalQuestions()
        Utility.mapValues(Apply.appProperties, applicationRecord, a)
        Utility.mapValues(Apply.supProperties, supplementalRecord, b)
        a.supplementalQuestions = b

        then:
        a.appId == 1
        a.intendedMajor == "major"
        a.finAidRef == true
        a.nonUsAddress == false
        a.addressSame == null
        a.hsCompDate == LocalDate.of(2001, 1, 1)
        a.hsCompDate == a.higherCompDate
        a.hsAttendance == 1
        a.tstmpSubmit == LocalDateTime.of(2001, 1, 1, 12, 45, 1)
        a.tstmpSubmit == a.tstmpCreate

        a.supplementalQuestions.appId == 1
        a.supplementalQuestions.suppText01 == "text"
        a.supplementalQuestions.suppCheck01 == true
        a.supplementalQuestions.suppCheck02 == false
        a.supplementalQuestions.suppCheck03 == null
    }
}

