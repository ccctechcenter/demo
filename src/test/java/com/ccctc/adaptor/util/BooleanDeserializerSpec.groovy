package com.ccctc.adaptor.util

import com.fasterxml.jackson.core.JsonProcessingException
import com.fasterxml.jackson.databind.ObjectMapper
import com.fasterxml.jackson.databind.annotation.JsonDeserialize
import groovy.transform.CompileStatic
import spock.lang.Specification

class BooleanDeserializerSpec extends Specification {

    def mapper = new ObjectMapper()

    def "trues"() {
        when:
        def trues = [mapper.readValue('{"test":true}', testClass.class),
                     mapper.readValue('{"test":"y"}', testClass.class),
                     mapper.readValue('{"test":"yes"}', testClass.class),
                     mapper.readValue('{"test":"Y"}', testClass.class),
                     mapper.readValue('{"test":"YES"}', testClass.class),
                     mapper.readValue('{"test":"TRUE"}', testClass.class),
                     mapper.readValue('{"test":"1"}', testClass.class),
                     mapper.readValue('{"test":1}', testClass.class)]

        then:
        trues.find { it.test == Boolean.FALSE } == null
    }

    def "falses"() {
        when:
        def falses = [mapper.readValue('{"test":false}', testClass.class),
                     mapper.readValue('{"test":"n"}', testClass.class),
                     mapper.readValue('{"test":"no"}', testClass.class),
                     mapper.readValue('{"test":"N"}', testClass.class),
                     mapper.readValue('{"test":"NO"}', testClass.class),
                     mapper.readValue('{"test":"FALSE"}', testClass.class),
                     mapper.readValue('{"test":"0"}', testClass.class),
                     mapper.readValue('{"test":0}', testClass.class)]

        then:
        falses.find { it.test == Boolean.TRUE } == null
    }

    def "nulls"() {
        when:
        def nulls = [mapper.readValue('{"test":null}', testClass.class),
                     mapper.readValue('{"test":""}', testClass.class)]

        then:
        nulls.find { it.test != null } == null
    }

    def "exceptions"() {
        when: mapper.readValue('{"test":"bad-string"}', testClass.class)
        then: thrown JsonProcessingException
        when: mapper.readValue('{"test":999}', testClass.class)
        then: thrown JsonProcessingException
        when: mapper.readValue('{"test":1.2}', testClass.class)
        then: thrown JsonProcessingException
    }

    @CompileStatic
    static class testClass {
        @JsonDeserialize(using = BooleanDeserializer.class)
        private Boolean test

        Boolean getTest() {
            return test
        }

        void setTest(Boolean test) {
            this.test = test
        }
    }
}
