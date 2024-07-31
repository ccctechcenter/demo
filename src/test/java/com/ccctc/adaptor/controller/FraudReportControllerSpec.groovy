package com.ccctc.adaptor.controller

import com.ccctc.adaptor.model.fraud.FraudReport
import com.ccctc.adaptor.util.GroovyService
import com.fasterxml.jackson.databind.ObjectMapper
import org.springframework.test.web.servlet.MockMvc
import org.springframework.test.web.servlet.setup.MockMvcBuilders
import spock.lang.Specification

import static org.springframework.http.MediaType.APPLICATION_JSON
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status


class FraudReportControllerSpec extends Specification {

    MockMvc mockMvc
    GroovyService groovyService

    def setup() {
        groovyService = Mock(GroovyService)

        def controller = new FraudReportController(groovyService: groovyService)
        mockMvc = MockMvcBuilders.standaloneSetup(controller).build()
    }

    def "GET - by Id"() {
        def result
        def expect = new FraudReport(sisFraudReportId: 34, misCode: "001", appId: 12345, cccId: "ABC123", fraudType: "Application", reportedByMisCode: "002", reportedBySource: "testing")
        def mapper = new ObjectMapper()

        when:
        result = mockMvc.perform(get('/fraudReport/34?mis=001').accept(APPLICATION_JSON))
        def response = result.andReturn().response
        assert response.content != null
        def actual = mapper.readValue(response.content.toString(), FraudReport.class)

        then:
        1 * groovyService.run(*_) >> expect
        result.andExpect(status().isOk())
        expect.sisFraudReportId == actual.sisFraudReportId
        expect.appId == actual.appId
        expect.cccId == actual.cccId
        expect.reportedByMisCode == actual.reportedByMisCode
        expect.reportedBySource == actual.reportedBySource
    }

    def "POST"() {
        def newId = 100l
        def newModel = new FraudReport(misCode: "001",
                                       appId: 12345l,
                                       cccId: "ABC123",
                                       fraudType: "Application",
                                       reportedByMisCode:  "ZZ1",
                                       reportedBySource: "testing")
        def mapper = new ObjectMapper()

        when:
        def result = mockMvc.perform(
                        post('/fraudReport?mis=001')
                        .contentType(APPLICATION_JSON)
                        .content(mapper.writeValueAsString(newModel))
                                )

        then:
        1 * groovyService.run(*_) >> newId
        def response = result.andReturn().response
        result.andExpect(status().isCreated())
        response.headers["Location"].toString().contains("/fraudReport/" + newId + "?mis=001")
    }
}
