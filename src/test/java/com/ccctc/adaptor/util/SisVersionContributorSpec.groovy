package com.ccctc.adaptor.util

import com.ccctc.adaptor.exception.InvalidRequestException
import org.springframework.boot.actuate.health.Status
import org.springframework.boot.actuate.info.Info
import spock.lang.Specification

class SisVersionContributorSpec extends Specification {

    def sisVersionHealthService = Mock(SisVersionHealthService)
    def sisVersionContributor = new SisVersionContributor(sisVersionHealthService)

    def "Get Info"() {
        setup:
        def builder = new Info.Builder()

        when:
        sisVersionContributor.contribute(builder)
        def built = builder.build()

        then:
        1 * sisVersionHealthService.getSisVersion() >> [version: "test"]
        built.details.size() == 1
        built.get("version") == "test"
    }

    def "Get Info Exception"() {
        setup:
        def builder = new Info.Builder()

        when:
        sisVersionContributor.contribute(builder)
        def built = builder.build()

        then:
        1 * sisVersionHealthService.getSisVersion() >> { throw new Exception("error") }
        // exception is caught and message logged gracefully.
        built.details.size() == 0
    }

    def "Get Health"() {
        when:
        def result = sisVersionContributor.health()

        then:
        1 * sisVersionHealthService.getSisConnectionStatus() >> Status.UP
        result.toString().contains(Status.UP.toString())
    }

    def "Get Health Down"() {
        when:
        def result = sisVersionContributor.health()

        then:
        1 * sisVersionHealthService.getSisConnectionStatus() >> Status.DOWN
        result.toString().contains(Status.DOWN.toString())
    }

    def "Get Health Down Exception"() {
        when:
        def result = sisVersionContributor.health()

        then:
        1 * sisVersionHealthService.getSisConnectionStatus() >> { throw new InvalidRequestException("TestingError") }
        result.toString().contains(Status.DOWN.toString())
    }
}