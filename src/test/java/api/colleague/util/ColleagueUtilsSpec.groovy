package api.colleague.util

import org.springframework.core.env.Environment
import spock.lang.Specification

class ColleagueUtilsSpec extends Specification {

    def env = Mock(Environment)

    def "getColleagueProperty - whitespace handling"() {
        when:
        def r1 = ColleagueUtils.getColleagueProperty(env, "123", "prop")
        def r2 = ColleagueUtils.getColleagueProperty(env, "123", "prop")
        def r3 = ColleagueUtils.getColleagueProperty(env, "123", "prop")

        then:
        3 * env.getProperty("123.colleague.prop") >>> ["", "   ", "abc   "]

        r1 == null
        r2 == null
        r3 == "abc"
    }

    def "getColleagueProperty - null MIS Code"() {
        when:
        def r = ColleagueUtils.getColleagueProperty(env, null, "prop")

        then:
        1 * env.getProperty("misCode") >> "000"
        1 * env.getProperty("000.colleague.prop") >> null
        1 * env.getProperty("colleague.prop") >> null

        r == null
    }

    def "getColleaguePropertyAsList"() {
        when:
        def list1 = ColleagueUtils.getColleaguePropertyAsList(env, "000", "list")
        def list2 = ColleagueUtils.getColleaguePropertyAsList(env, "000", "list")
        def list3 = ColleagueUtils.getColleaguePropertyAsList(env, "000", "list")

        then:
        3 * env.getProperty("000.colleague.list") >>> ["1,2, 3", ", ,1", " a "]
        list1 == ["1", "2", "3"]
        list2 == [null, null, "1"]
        list3 == ["a"]
    }

    def "quoteString"() {
        when:
        def nullQuote = ColleagueUtils.quoteString(null)
        def singleQuote = ColleagueUtils.quoteString("'test'")
        def doubleQuote = ColleagueUtils.quoteString("\"test\"")
        def singleAndDoubleQuote = ColleagueUtils.quoteString("\"test\" and 'test'")
        def crLfQuote = ColleagueUtils.quoteString("'hello \r\nworld'")

        then:
        nullQuote == null
        singleQuote == "\"'test'\""
        doubleQuote == "'\"test\"'"
        singleAndDoubleQuote == "'\"test\" and test'"
        crLfQuote == "\"'hello world'\""
    }

    def "getMVKeyValue"() {
        when:
        def x = ColleagueUtils.getMVKeyValue(null, 0)
        def a = ColleagueUtils.getMVKeyValue("1234567*2018FA*UG", 1)
        def b = ColleagueUtils.getMVKeyValue("1234567*2018FA*UG", 2)
        def c = ColleagueUtils.getMVKeyValue("1234567*2018FA*UG", 3)
        def d = ColleagueUtils.getMVKeyValue("1234567*2018FA*UG", 4)

        then:
        x == null
        a == "1234567"
        b == "2018FA"
        c == "UG"
        d == null
    }
}
