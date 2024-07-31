package com.ccctc.adaptor.controller

import com.ccctc.adaptor.controller.TermApiController
import com.ccctc.adaptor.model.Term
import com.ccctc.adaptor.model.TermSession
import com.ccctc.adaptor.model.TermType
import com.ccctc.adaptor.util.GroovyService
import org.springframework.test.web.servlet.setup.MockMvcBuilders
import spock.lang.Specification

import static org.springframework.http.MediaType.APPLICATION_JSON
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.content
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status

/**
 * Created by jrscanlon on 12/14/15.
 */
class TermControllerSpec extends Specification {

    def "Term Controller"() {
        setup:
        def groovyUtil = Mock( GroovyService )
        def termApiController = new TermApiController(groovyService: groovyUtil )
        def mockMvc = MockMvcBuilders.standaloneSetup(termApiController).build()

        def result

        when:
        result = mockMvc.perform(get('/terms/termid?mis=001').contentType(APPLICATION_JSON))

        then:
        1 * groovyUtil.run(_ , _ ,_ ,_) >> new Term.Builder().description("desc").type(TermType.Quarter).misCode("mis").session(TermSession.Fall).sisTermId("termid").build()
        result.andExpect(status().isOk())
        result.andExpect(content().json('{"misCode":"mis","sisTermId":"termid","year":null,"session":"Fall","type":"Quarter","start":null,"end":null,"preRegistrationStart":null,"preRegistrationEnd":null,"registrationStart":null,"registrationEnd":null,"addDeadline":null,"dropDeadline":null,"withdrawalDeadline":null,"feeDeadline":null,"censusDate":null,"description":"desc"}'))

    }

    def "Term Controller mis"() {
        setup:
        def groovyUtil = Mock( GroovyService )
        def termApiController = new TermApiController(groovyService: groovyUtil )
        def mockMvc = MockMvcBuilders.standaloneSetup(termApiController).build()

        def result

        when:
        result = mockMvc.perform(get('/terms/termid?mis=mis').contentType(APPLICATION_JSON))

        then:
        1 * groovyUtil.run(_ , _ ,_ ,_) >> new Term.Builder().description("desc").type(TermType.Quarter).misCode("mis").session(TermSession.Fall).sisTermId("termid").build()
        result.andExpect(status().isOk())
        result.andExpect(content().json('{"misCode":"mis","sisTermId":"termid","year":null,"session":"Fall","type":"Quarter","start":null,"end":null,"preRegistrationStart":null,"preRegistrationEnd":null,"registrationStart":null,"registrationEnd":null,"addDeadline":null,"dropDeadline":null,"withdrawalDeadline":null,"feeDeadline":null,"censusDate":null,"description":"desc"}'))

    }

    def "Term Controller getAll"() {
        setup:
        def groovyUtil = Mock( GroovyService )
        def termApiController = new TermApiController(groovyService: groovyUtil )
        def mockMvc = MockMvcBuilders.standaloneSetup(termApiController).build()

        def result

        when:
        result = mockMvc.perform(get('/terms?mis=001').contentType(APPLICATION_JSON))

        then:
        1 * groovyUtil.run(_ , _ ,_ ,_) >> [new Term.Builder().description("desc").type(TermType.Quarter).misCode("mis").session(TermSession.Fall).sisTermId("termid").build()]
        result.andExpect(status().isOk())
        result.andExpect(content().json('[{"misCode":"mis","sisTermId":"termid","year":null,"session":"Fall","type":"Quarter","start":null,"end":null,"preRegistrationStart":null,"preRegistrationEnd":null,"registrationStart":null,"registrationEnd":null,"addDeadline":null,"dropDeadline":null,"withdrawalDeadline":null,"feeDeadline":null,"censusDate":null,"description":"desc"}]'))

    }
}
