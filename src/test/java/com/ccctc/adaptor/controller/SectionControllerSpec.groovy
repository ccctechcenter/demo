package com.ccctc.adaptor.controller

import com.ccctc.adaptor.controller.SectionApiController
import com.ccctc.adaptor.model.Section
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
class SectionControllerSpec extends Specification {

    def "Section Controller"() {
        setup:
        def groovyUtil = Mock( GroovyService )
        def sectionApiController = new SectionApiController(groovyService: groovyUtil )
        def mockMvc = MockMvcBuilders.standaloneSetup(sectionApiController).build()

        def result

        when:
        result = mockMvc.perform(get('/sections/sectionid?mis=001&sisTermId=term').contentType(APPLICATION_JSON))

        then:
        1 * groovyUtil.run(_ , _ ,_ ,_) >> new Section.Builder().campus("campus").minimumUnits(new Float(3.0)).sisCourseId("sisCourseId").sisTermId("termid").maxWaitlist(25).build()
        result.andExpect(status().isOk())
        result.andExpect(content().json('{"campus":"campus","minimumUnits":3.0,"sisCourseId":"sisCourseId","sisTermId":"termid","maxWaitlist":25}'))

    }

    def "Section Controller getAll"() {
        setup:
        def groovyUtil = Mock( GroovyService )
        def sectionApiController = new SectionApiController(groovyService: groovyUtil )
        def mockMvc = MockMvcBuilders.standaloneSetup(sectionApiController).build()

        def result

        // by term
        when:
        result = mockMvc.perform(get('/sections?sisTermId=termid&mis=001').contentType(APPLICATION_JSON))

        then:
        1 * groovyUtil.run(_ , _ ,_ ,_) >> [new Section.Builder().campus("campus").minimumUnits(new Float(3.0)).sisCourseId("sisCourseId").sisTermId("termid").maxWaitlist(25).build()]
        result.andExpect(status().isOk())
        result.andExpect(content().json('[{"campus":"campus","minimumUnits":3.0,"sisCourseId":"sisCourseId","sisTermId":"termid","maxWaitlist":25}]'))

        // by term and course
        when:
        result = mockMvc.perform(get('/sections?sisTermId=termid&sisCourseId=courseid&mis=001').contentType(APPLICATION_JSON))

        then:
        1 * groovyUtil.run(_ , _ ,_ ,_) >> [new Section.Builder().campus("campus").minimumUnits(new Float(3.0)).sisCourseId("sisCourseId").sisTermId("termid").maxWaitlist(25).build()]
        result.andExpect(status().isOk())
        result.andExpect(content().json('[{"campus":"campus","minimumUnits":3.0,"sisCourseId":"sisCourseId","sisTermId":"termid","maxWaitlist":25}]'))
    }

    def "Section Controller search"() {
        setup:
        def groovyUtil = Mock( GroovyService )
        def sectionApiController = new SectionApiController(groovyService: groovyUtil )
        def mockMvc = MockMvcBuilders.standaloneSetup(sectionApiController).build()

        def result

        when:
        result = mockMvc.perform(get('/sections/search?sisTermId=termid&mis=001&words=word').contentType(APPLICATION_JSON))

        then:
        1 * groovyUtil.run(_ , _ ,_ ,_) >> [new Section.Builder().campus("campus").minimumUnits(new Float(3.0)).sisCourseId("sisCourseId").sisTermId("termid").maxWaitlist(25).build()]
        result.andExpect(status().isOk())
        result.andExpect(content().json('[{"campus":"campus","minimumUnits":3.0,"sisCourseId":"sisCourseId","sisTermId":"termid","maxWaitlist":25}]'))
    }
}
