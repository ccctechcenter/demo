package com.ccctc.adaptor.model

import spock.lang.Specification

class CrosslistingDetailSpec extends Specification {

    def "hashcode and equals and toString"() {
        when:
        def a = new CrosslistingDetail.Builder().sisSectionIds(["1", "2"]).name("name").primarySisSectionId("1").sisTermId("sisTermId").build()
        def b = new CrosslistingDetail.Builder().sisSectionIds(["1", "2"]).name("name").primarySisSectionId("1").sisTermId("sisTermId").build()
        def c = new CrosslistingDetail.Builder().sisSectionIds(["3", "4"]).name("name").primarySisSectionId("3").sisTermId("sisTermId").build()

        then:
        a == b
        b != c

        a.hashCode() == b.hashCode()
        b.hashCode() != c.hashCode()

        a.toString() == b.toString()
        b.toString() != c.toString()
    }
}
