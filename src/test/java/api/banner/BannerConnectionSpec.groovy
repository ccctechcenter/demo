package api.banner

import groovy.sql.Sql
import org.springframework.core.env.Environment
import spock.lang.Specification

class BannerConnectionSpec extends Specification {

    String misCode = "000"
    Environment environment
    Sql sql

    def setup() {
        sql = Mock(Sql)
        environment = Mock(Environment)

        GroovyMock(Sql, global:true)
        Sql.newInstance(*_) >> sql
    }

    def "get Session Connection"() {
        when:
        Sql result = BannerConnection.getSession(environment, misCode)

        then:
        result != null
    }
}
