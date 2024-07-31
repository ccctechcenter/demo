package com.ccctc.adaptor.util

import com.ccctc.adaptor.util.LogUtils
import spock.lang.Specification

/**
 * Created by zekeo on 7/26/2017.
 */
class LogUtilsSpec extends Specification {

    def "null string"() {
        when:
        def result = LogUtils.escapeString(null)

        then:
        result == null
    }

    def "good string"() {
        when:
        def str = "no problem with this string - _ 1235456 ! ~"
        def result = LogUtils.escapeString(str)

        then:
        result == str
    }

    def "cr lf replacement"() {
        when:
        def str= "test string\r\n"
        def result = LogUtils.escapeString(str)

        then:
        !result.contains("\r")
        !result.contains("\n")
    }

    def "non-printing character replacement"() {
        when:
        def str = "\u0000\u0001\u0010\u0013\u0012"
        def result = LogUtils.escapeString(str)

        then:
        result == "?????"
    }
}
