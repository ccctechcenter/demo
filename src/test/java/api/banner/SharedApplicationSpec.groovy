package api.banner

import com.ccctc.adaptor.exception.EntityConflictException
import com.ccctc.adaptor.exception.EntityNotFoundException
import com.ccctc.adaptor.exception.InternalServerException
import groovy.sql.GroovyRowResult
import groovy.sql.Sql
import org.springframework.core.env.Environment
import spock.lang.Specification

import java.sql.Timestamp
import java.time.LocalDate
import java.time.LocalDateTime

class SharedApplicationSpec extends Specification {

    def misCode = "000"
    api.banner.SharedApplication apply
    Sql sql
    Environment environment

    def setup() {
        sql = Mock(Sql)
        environment = Mock(Environment)

        apply = Spy(new api.banner.SharedApplication())
        apply.environment = environment

        GroovyMock(BannerConnection, global: true)
        BannerConnection.getSession(*_) >> sql
    }

    def "get"() {
        setup:
        def map = new HashMap()
        map.put("appId", 1)

        def record = new GroovyRowResult(map)

        when:
        def result =  apply.get(misCode, 1)

        then:
        1 * sql.firstRow(*_) >>> [record]
        result.appId == 1
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

        def application = new com.ccctc.adaptor.model.apply.SharedApplication()
        application.appId = 1

        when:
        apply.post(misCode, application)

        then:
        1 * sql.firstRow(*_) >> record
        thrown EntityConflictException
    }

    def "post - error on sql call"(){
        setup:
        def application = new com.ccctc.adaptor.model.apply.SharedApplication()
        application.appId = 1

        when:
        apply.post(misCode, application)

        then:
        1 * sql.firstRow(*_) >> null
        1 * sql.execute(*_) >> { throw new Exception("oops") }
        thrown InternalServerException
    }

    def "createApply"(){
        setup:
        def map = new HashMap()
        map.put("appId", 1)
        map.put("higherCompDate", LocalDate.of(2018,01,10))
        def record = new GroovyRowResult(map)
        def application = new com.ccctc.adaptor.model.apply.SharedApplication()
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

        def applicationRecord = new GroovyRowResult(map)

        when:
        def a = new com.ccctc.adaptor.model.apply.SharedApplication()
        Utility.mapValues(Apply.appProperties, applicationRecord, a)

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
    }
}

