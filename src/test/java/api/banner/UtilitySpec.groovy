package api.banner

import groovy.sql.GroovyRowResult
import org.springframework.boot.actuate.health.Status
import org.springframework.core.env.Environment
import spock.lang.Specification
import groovy.sql.Sql

class UtilitySpec extends Specification {

    String misCode = "000"
    Environment environment
    Sql sql
    Utility utility

    def setup() {
        sql = Mock(Sql)
        environment = Mock(Environment)

        utility = Spy(new Utility())
        utility.environment = environment

        GroovyMock(BannerConnection, global: true)
        BannerConnection.getSession(*_) >> sql
    }

    def "get SIS Version"() {
        setup:
        def row = new GroovyRowResult(["product":"Student","release":"8.5.1"])
        when:
        def result = utility.getSisVersion()

        then:
        1 * sql.rows(_) >> [row]

        result == ["Banner Versions":["Student":"8.5.1"]]
    }

    def "get SIS Connection Status"() {
        setup:
        def row = new GroovyRowResult(["":1])
        when:
        def result = utility.getSisConnectionStatus()

        then:
        1 * sql.firstRow(*_) >> row
        result == Status.UP
    }

    def "get SIS Connection Status Down"() {

        when:
        def result = utility.getSisConnectionStatus()

        then:
        1 * sql.firstRow(*_) >>> null
        result == Status.DOWN
    }
}
