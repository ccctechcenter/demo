package com.ccctc.adaptor.util

import com.ccctc.adaptor.util.ImageTagContributor
import org.springframework.boot.actuate.info.Info
import spock.lang.Specification

class ImageTagContributorSpec extends Specification {

    def "contribute"() {
        setup:
        def a = new ImageTagContributor("test")
        def builder = new Info.Builder()

        when:
        a.contribute(builder)
        def built = builder.build()

        then:
        built.details.size() == 1
        built.get("image") == "test"
    }
}
