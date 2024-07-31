package com.ccctc.adaptor.controller

import com.ccctc.adaptor.model.apply.Application
import com.ccctc.adaptor.model.apply.SharedApplication
import com.ccctc.adaptor.util.GroovyService
import com.fasterxml.jackson.databind.ObjectMapper
import org.springframework.test.web.servlet.MockMvc
import org.springframework.test.web.servlet.setup.MockMvcBuilders
import spock.lang.Specification

import static org.springframework.http.MediaType.APPLICATION_JSON
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status

class SharedApplicationControllerSpec extends Specification {

    MockMvc mockMvc
    GroovyService groovyService

    def setup() {
        groovyService = Mock(GroovyService)

        def controller = new SharedApplicationController(groovyService: groovyService)
        mockMvc = MockMvcBuilders.standaloneSetup(controller).build()
    }

    def "GET"() {
        def result
        def teachMisCode = "001"
        def homeMisCode = "002"
        def testAppId = 12345
        def sharedApp = new SharedApplication(misCode: teachMisCode, appId: testAppId, cccId: "ABC123", termId: 1, collegeId: homeMisCode)
        def mapper = new ObjectMapper()

        when:
        result = mockMvc.perform(get('/shared-application/' + testAppId + '?mis=' + teachMisCode).accept(APPLICATION_JSON))
        def response = result.andReturn().response
        assert response.content != null
        def resultApp = mapper.readValue(response.content.toString(), SharedApplication.class)

        then:
        1 * groovyService.run(*_) >> sharedApp
        result.andExpect(status().isOk())
        resultApp.misCode == sharedApp.misCode
        resultApp.collegeId == sharedApp.collegeId
        resultApp.appId == sharedApp.appId
        resultApp.cccId == sharedApp.cccId
        resultApp.termId == sharedApp.termId
    }

    def "POST"() {
        def result
        def teachMisCode = "001"
        def homeMisCode = "002"
        def app = new Application(appId: 12345, cccId: "ABC123", collegeId: homeMisCode, termId: 1)
        app.setSupplementalQuestions(null);
        def mapper = new ObjectMapper()

        when:
        result = mockMvc.perform(post('/shared-application?mis=' + teachMisCode).contentType(APPLICATION_JSON)
                .content(mapper.writeValueAsString(app)))
        def response = result.andReturn().response

        then:
        1 * groovyService.run(*_)
        result.andExpect(status().isCreated())
        response.headers["Location"].toString().contains("/shared-application/" + app.appId + "?mis=" + teachMisCode)
    }
}
