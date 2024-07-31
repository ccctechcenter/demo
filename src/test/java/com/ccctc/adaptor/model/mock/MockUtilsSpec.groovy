package com.ccctc.adaptor.model.mock

import com.ccctc.adaptor.util.mock.MockUtils
import spock.lang.Specification

/**
 * Created by zekeo on 7/21/2017.
 */
class MockUtilsSpec extends Specification {

    def "extractCccId"() {
        setup:
        def result

        when:
        result = MockUtils.extractCccId(null)

        then:
        result == null

        when:
        result = MockUtils.extractCccId("short")

        then:
        result == null

        when:
        result = MockUtils.extractCccId("longer")

        then:
        result == null

        when:
        result = MockUtils.extractCccId("cccid:ABC1234")

        then:
        result == "ABC1234"
    }

    def "fixMap"() {
        setup:
        def map = null

        when:
        MockUtils.fixMap(map)

        then:
        map == null

        when:
        map = [:]
        MockUtils.fixMap(map)

        then:
        map.size() == 0

        when:
        map = ["sisPersonId" : "student"]
        MockUtils.fixMap(map)

        then:
        map.sisPersonId == "student"
        !map.containsKey("cccid")

        when:
        map = ["sisPersonId" : "cccid:student"]
        MockUtils.fixMap(map)

        then:
        map.cccid == "student"
        !map.containsKey("sisPersonId")

    }
}
