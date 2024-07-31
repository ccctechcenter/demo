package com.ccctc.adaptor.util

import org.springframework.core.env.Environment
import spock.lang.Specification

/**
 * Created by jrscanlon on 12/14/15.
 */
class MisEnvironmentSpec extends Specification {


    def "Test getProperty Method"() {
        setup:
        def environment = Mock(Environment)
        String misCode = "002"
        String propertyString = "server.port"
        String normalProperty, misProperty
        environment.getProperty(propertyString) >> "8443"
        environment.getProperty("002." + propertyString) >> "7443"

        when:
        normalProperty = MisEnvironment.getProperty(environment, null, propertyString)
        misProperty = MisEnvironment.getProperty(environment, misCode, propertyString)

        then:
        normalProperty == "8443"
        misProperty == "7443"

    }

    def "Test checkPropertyMatch Method"() {
        setup:
        MisEnvironment env = new MisEnvironment()
        def environment = Mock(Environment)
        String misCode = "002"
        String propertyString = "server.port"
        String nullString = "null.property"
        Boolean normalProperty, misProperty, misProperty2, nullProperty,
                nullProperty2, nullProperty3, nullProperty4
        environment.getProperty(propertyString) >> "8443,9999,0000"
        environment.getProperty("002." + propertyString) >> "7443,1111,2222"
        environment.getProperty(nullString) >> null
        environment.getProperty("002." + nullString) >> 7443
        when:
        normalProperty = MisEnvironment.checkPropertyMatch(environment, null, propertyString, "9999")
        misProperty = MisEnvironment.checkPropertyMatch(environment, misCode, propertyString, "2222")
        misProperty2 = MisEnvironment.checkPropertyMatch(environment, misCode, propertyString, "7443")
        nullProperty = MisEnvironment.checkPropertyMatch(environment, misCode, nullString, "7443")
        nullProperty2 = MisEnvironment.checkPropertyMatch(environment, misCode, nullString, null)
        nullProperty3 = MisEnvironment.checkPropertyMatch(environment, null, nullString, null)
        nullProperty4 = MisEnvironment.checkPropertyMatch(environment, null, null, "7443")

        then:
        normalProperty == true
        misProperty == true
        misProperty2 == true
        nullProperty == true
        nullProperty2 == false
        nullProperty3 == false
        nullProperty4 == false

    }

}
