package com.ccctc.adaptor.controller

import com.ccctc.adaptor.model.apply.CCPromiseGrant
import com.ccctc.adaptor.util.GroovyService
import com.fasterxml.jackson.databind.ObjectMapper
import org.springframework.test.web.servlet.MockMvc
import org.springframework.test.web.servlet.setup.MockMvcBuilders
import spock.lang.Specification

import static org.springframework.http.MediaType.APPLICATION_JSON
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status

class CCPromiseGrantControllerSpec extends Specification {

    MockMvc mockMvc
    GroovyService groovyService

    def setup() {
        groovyService = Mock(GroovyService)

        def controller = new CCPromiseGrantController(groovyService: groovyService)
        mockMvc = MockMvcBuilders.standaloneSetup(controller).build()
    }

    def "GET"() {
        def result
        def app = new CCPromiseGrant(appId: 12345, cccId: "ABC123")
        def mapper = new ObjectMapper()

        when:
        result = mockMvc.perform(get('/ccpg/12345?mis=001').accept(APPLICATION_JSON))
        def response = result.andReturn().response
        assert response.content != null
        def resultApp = mapper.readValue(response.content.toString(), CCPromiseGrant.class)

        then:
        1 * groovyService.run(*_) >> app
        result.andExpect(status().isOk())
        resultApp.appId == app.appId
        resultApp.cccId == app.cccId
    }

    def "POST"() {
        def result
        def app = new CCPromiseGrant(appId: 12345, cccId: "ABC123", collegeId: "001")
        def mapper = new ObjectMapper()

        when:
        result = mockMvc.perform(post('/ccpg?mis=001').contentType(APPLICATION_JSON)
                .content(mapper.writeValueAsString(app)))
        def response = result.andReturn().response
        assert response.content != null
        def resultApp = mapper.readValue(response.content.toString(), CCPromiseGrant.class)

        then:
        1 * groovyService.run(*_) >> app
        result.andExpect(status().isCreated())
        resultApp.appId == app.appId
        resultApp.cccId == app.cccId
        response.headers["Location"].toString().contains("/ccpg/12345?mis=001")
    }
}
