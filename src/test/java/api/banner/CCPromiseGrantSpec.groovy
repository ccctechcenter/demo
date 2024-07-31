package api.banner

import com.ccctc.adaptor.exception.EntityConflictException
import com.ccctc.adaptor.exception.EntityNotFoundException
import com.ccctc.adaptor.exception.InternalServerException
import com.ccctc.adaptor.model.apply.CCPromiseGrant
import groovy.sql.GroovyRowResult
import groovy.sql.Sql
import org.springframework.core.env.Environment
import spock.lang.Specification

import java.sql.Timestamp
import java.time.LocalDateTime

class CCPromiseGrantSpec extends Specification {

    def misCode = "000"
    api.banner.CCPromiseGrant ccPromiseGrant
    Sql sql
    Environment environment

    def setup() {
        sql = Mock(Sql)
        environment = Mock(Environment)

        ccPromiseGrant = Spy(new api.banner.CCPromiseGrant())
        ccPromiseGrant.environment = environment

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
        def result = ccPromiseGrant.get(misCode, 1)

        then:
        1 * sql.firstRow(*_) >>> [record, record2]
        result.appId == 1
    }

    def "get not found"() {
        when:
        ccPromiseGrant.get(misCode, 1)

        then:
        1 * sql.firstRow(*_) >> null
        thrown EntityNotFoundException
    }

    def "postEntityExists"(){
        setup:
        def map = new HashMap()
        map.put("appId", 1)

        def record = new GroovyRowResult(map)

        def cc = new CCPromiseGrant()
        cc.appId = 1

        when:
        ccPromiseGrant.post(misCode, cc)

        then:
        1 * sql.firstRow(*_) >> record
        thrown EntityConflictException
    }

    def "post - error on sql call"(){
        setup:
        def cc = new CCPromiseGrant()
        cc.appId = 1

        when:
        ccPromiseGrant.post(misCode, cc)

        then:
        1 * sql.firstRow(*_) >> null
        1 * sql.execute(*_) >> { throw new Exception("oops") }
        thrown InternalServerException
    }

    def "post - error on get"(){
        setup:
        def cc = new CCPromiseGrant()
        cc.appId = 1

        when:
        ccPromiseGrant.post(misCode, cc)

        then:
        1 * sql.firstRow(*_) >> null
        1 * sql.execute(*_)
        1 * sql.firstRow(*_) >> null
        thrown InternalServerException
    }

    def "ccPromiseGrantCreate"(){
        setup:
        def map = new HashMap()
        map.put("appId", 1)
        def record = new GroovyRowResult(map)
        def cc = new CCPromiseGrant()
        cc.appId = 1

        when:
        ccPromiseGrant.insertPromiseGrant(misCode, cc, sql)

        then:
        sql.firstRow(*_) >> record
    }

    def "mapValues"() {
        setup:
        def map = [
                appId: 1,
                tstmpSubmit: Timestamp.valueOf(LocalDateTime.of(2001, 1, 1, 12, 45, 1)),
                tstmpCreate: "2001-01-01 12:45:01",
                confirmationApplicant: true,
                noPermAddressHomeless: false
        ]

        def ccRecord = new GroovyRowResult(map)

        when:
        def a = new CCPromiseGrant()
        Utility.mapValues(api.banner.CCPromiseGrant.promiseGrantProperties, ccRecord, a)

        then:
        a.appId == 1
    }
}

