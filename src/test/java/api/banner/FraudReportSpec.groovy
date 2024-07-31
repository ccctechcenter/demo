package api.banner

import com.ccctc.adaptor.exception.EntityConflictException
import com.ccctc.adaptor.exception.EntityNotFoundException
import com.ccctc.adaptor.exception.InternalServerException
import groovy.sql.GroovyRowResult
import groovy.sql.Sql
import oracle.sql.TIMESTAMP
import org.springframework.core.env.Environment
import spock.lang.Specification
import java.sql.Timestamp
import java.time.Instant
import java.time.LocalDateTime

class FraudReportSpec extends Specification {

    def misCode = "000"
    api.banner.FraudReport fraudReport
    Sql sql
    Environment environment

    def setup() {
        sql = Mock(Sql)
        environment = Mock(Environment)

        fraudReport = Spy(new api.banner.FraudReport())
        fraudReport.environment = environment

        GroovyMock(BannerConnection, global: true)
        BannerConnection.getSession(*_) >> sql
    }

    def "getById"() {
        setup:
        def map = new HashMap()
        map.put("sisFraudReportId", 1)

        def record = new GroovyRowResult(map)

        when:
        def result = fraudReport.getById(misCode, 1)

        then:
        1 * sql.firstRow(*_) >>> [record]
        result.sisFraudReportId == 1
    }

    def "get not found"() {
        when:
        def result = fraudReport.getById(misCode, 1)

        then:
        1 * sql.firstRow(*_) >> null
        thrown EntityNotFoundException
    }

    def "create - with timestamp"(){
        setup:
        def cc = new com.ccctc.adaptor.model.fraud.FraudReport()
        cc.appId = 1
        cc.cccId = "AAA0001"
        cc.fraudType = "APPLICATION"
        cc.reportedByMisCode = "ZZ1"
        cc.tstmpSubmit = LocalDateTime.now()

        when:
        def result = fraudReport.create(misCode, cc)

        then:
        1 * sql.firstRow(*_) >> ["reportId":1]
        1 * sql.execute(*_)
        result == 1
    }

    def "create - without timestamp"(){
        setup:
        def cc = new com.ccctc.adaptor.model.fraud.FraudReport()
        cc.appId = 1
        cc.cccId = "AAA0001"
        cc.fraudType = "APPLICATION"
        cc.reportedByMisCode = "ZZ1"
        cc.tstmpSubmit = null

        when:
        def result = fraudReport.create(misCode, cc)

        then:
        1 * sql.firstRow(*_) >> ["reportId":2]
        1 * sql.execute(*_)
        result == 2
    }

    def "create - error on sql call"(){
        setup:
        def cc = new com.ccctc.adaptor.model.fraud.FraudReport()
        cc.appId = 1
        cc.cccId = "AAA0001"
        cc.fraudType = "APPLICATION"
        cc.reportedByMisCode = "ZZ1"
        cc.tstmpSubmit = LocalDateTime.now()

        when:
        def result = fraudReport.create(misCode, cc)

        then:
        1 * sql.firstRow(*_) >> ["reportId":0]
        1 * sql.execute(*_) >> { throw new Exception("reportId already exists") }
        thrown InternalServerException
    }

    def "create - error duplicate"(){
        setup:
        def cc = new com.ccctc.adaptor.model.fraud.FraudReport()
        cc.appId = 1
        cc.cccId = "AAA0001"
        cc.fraudType = "APPLICATION"
        cc.reportedByMisCode = "ZZ1"
        cc.tstmpSubmit = LocalDateTime.now()
        def map = new HashMap()
        map.put("sisFraudReportId", 1)
        map.put("appId", 1)
        map.put("cccId", "AAA0001")
        map.put("tstmpSubmit", "2022-10-15 13:14:15")
        map.put("misCode", misCode)
        map.put("reportedByMisCode", "ZZ1")
        map.put("fraudType", "APPLICATION")
        map.put("sisProcessedFlag", false)
        def record = new GroovyRowResult(map)
        sql.rows(*_) >> [record]
        when:
        def result = fraudReport.create(misCode, cc)

        then:
        thrown EntityConflictException
    }

    def "delete" (){
        setup:
        def map = new HashMap()
        map.put("sisFraudReportId", 1)

        def record = new GroovyRowResult(map)
        when:
        fraudReport.deleteById(misCode, 1)
        then:
        1 * sql.firstRow(*_) >> record
        1 == 1
    }

    def "delete not found"(){
        when:
        fraudReport.deleteById(misCode, 1)
        then:
        1 * sql.firstRow(*_) >> null
        thrown EntityNotFoundException
    }

    def "delete - report not found"() {
        setup:
        def cc = new com.ccctc.adaptor.model.fraud.FraudReport()
        cc.appId = 1
        cc.reportedByMisCode = "ZZ1"
        cc.misCode = "ZZ2"

        when:
        def result = fraudReport.deleteFraudReport(cc)

        then:
        thrown EntityNotFoundException
    }

    def "delete - report not found no results"() {
        setup:
        def cc = new com.ccctc.adaptor.model.fraud.FraudReport()
        cc.appId = 1
        cc.reportedByMisCode = "ZZ1"
        cc.misCode = "ZZ2"
        sql.rows(*_) >> []

        when:
        def result = fraudReport.deleteFraudReport(cc)

        then:
        thrown EntityNotFoundException
    }

    def "delete - report"() {
        setup:
        def cc = new com.ccctc.adaptor.model.fraud.FraudReport()
        cc.appId = 1
        cc.reportedByMisCode = "ZZ1"
        cc.misCode = "ZZ2"
        def map = new HashMap()
        map.put("sisFraudReportId", 1)
        map.put("appId", 1)
        map.put("cccId", "AAA0001")
        map.put("tstmpSubmit", "2022-10-15 13:14:15")
        map.put("misCode", misCode)
        map.put("reportedByMisCode", "ZZ1")
        map.put("fraudType", "APPLICATION")
        map.put("sisProcessedFlag", false)
        def record = new GroovyRowResult(map)
        sql.rows(*_) >> [record]

        when:
        def result = fraudReport.deleteFraudReport(cc)

        then:
        1 * sql.execute(*_)
    }

    def "match - by appId" (){
        setup:
        def map = new HashMap()
        map.put("sisFraudReportId", 1)
        map.put("appId", 1)
        map.put("cccId", "AAA0001")
        map.put("tstmpSubmit", "2022-10-15 13:14:15")
        map.put("misCode", misCode)
        map.put("reportedByMisCode", "ZZ1")
        map.put("fraudType", "APPLICATION")
        map.put("sisProcessedFlag", false)
        def record = new GroovyRowResult(map)
        def map2 = new HashMap()
        map2.put("appId", 1)
        map2.put("sisFraudReportId", 2)
        map2.put("cccId", "AAA0001")
        map2.put("tstmpSubmit", "2022-10-15 13:14:15")
        map2.put("misCode", misCode)
        map2.put("reportedByMisCode", "ZZ2")
        map2.put("fraudType", "APPLICATION")
        map2.put("sisProcessedFlag", false)
        def record2 = new GroovyRowResult(map2)
        sql.rows(*_) >> [record,record2]
        when:
        def result = fraudReport.getMatching(misCode,1,null)
        then:
        result.size() == 2
        result.every{ it.appId == 1 }
        result.every{ it.misCode == misCode }
        result.any{ it.sisFraudReportId == 1 }
        result.any{ it.sisFraudReportId == 2 }
        result.every{ it.cccId == "AAA0001" }
    }

    def "match - by cccId" (){
        setup:
        def map = new HashMap()
        map.put("sisFraudReportId", 1)
        map.put("appId", 1)
        map.put("cccId", "AAA0001")
        map.put("tstmpSubmit", "2022-10-15 13:14:15")
        map.put("misCode", misCode)
        map.put("reportedByMisCode", "ZZ1")
        map.put("fraudType", "APPLICATION")
        map.put("sisProcessedFlag", false)
        def record = new GroovyRowResult(map)
        def map2 = new HashMap()
        map2.put("appId", 2)
        map2.put("sisFraudReportId", 2)
        map2.put("cccId", "AAA0001")
        map2.put("tstmpSubmit", "2022-10-15 13:14:15")
        map2.put("misCode", misCode)
        map2.put("reportedByMisCode", "ZZ2")
        map2.put("fraudType", "APPLICATION")
        map2.put("sisProcessedFlag", false)
        def record2 = new GroovyRowResult(map2)
        sql.rows(*_) >> [record,record2]
        when:
        def result = fraudReport.getMatching(misCode,null,"AAA0001")
        then:
        result.size() == 2
        result.every{ it.cccId == "AAA0001" }
        result.every{ it.misCode == misCode }
        result.any{ it.sisFraudReportId == 1 }
        result.any{ it.sisFraudReportId == 2 }
        result.any{ it.appId == 1 }
        result.any{ it.appId == 2 }
    }

    def "mapValues"() {
        setup:
        def time = Timestamp.from(Instant.now())
        def map = [
                misCode: misCode,
                sisFraudReportId: 12341234,
                appId: 12345,
                cccId: "ABC1234",
                fraudType: "VERY_BAD",
                reportedByMisCode: "123",
                tstmpSubmit: new TIMESTAMP(time)
        ]

        GroovyRowResult applicationRecord = new GroovyRowResult(map)

        when:
        def a = new com.ccctc.adaptor.model.fraud.FraudReport()
        Utility.mapValues(FraudReport.fraudReportProperties, applicationRecord, a)

        then:
        a.misCode == misCode
        a.sisFraudReportId == 12341234
        a.appId == 12345
        a.cccId == "ABC1234"
        a.fraudType == "VERY_BAD"
        a.reportedByMisCode == "123"
        a.tstmpSubmit.isEqual(time.toLocalDateTime())
    }
}
