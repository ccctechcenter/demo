package com.ccctc.adaptor.controller

import com.ccctc.adaptor.model.apply.Application
import com.ccctc.adaptor.model.apply.SupplementalQuestions
import com.ccctc.adaptor.util.GroovyService
import com.fasterxml.jackson.databind.ObjectMapper
import com.fasterxml.jackson.databind.SerializationFeature
import com.fasterxml.jackson.datatype.jsr310.JavaTimeModule
import net.sf.cglib.core.Local
import org.springframework.test.web.servlet.MockMvc
import org.springframework.test.web.servlet.setup.MockMvcBuilders
import spock.lang.Specification

import java.text.DateFormat
import java.text.SimpleDateFormat
import java.time.LocalDate

import static org.springframework.http.MediaType.APPLICATION_JSON
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status

class ApplyControllerSpec extends Specification {

    MockMvc mockMvc
    GroovyService groovyService

    def setup() {
        groovyService = Mock(GroovyService)

        def controller = new ApplyController(groovyService: groovyService)
        mockMvc = MockMvcBuilders.standaloneSetup(controller).build()
    }

    def "GET"() {
        def result
        def app = new Application(appId: 12345, cccId: "ABC123", termId: 1)
        def mapper = new ObjectMapper()

        when:
        result = mockMvc.perform(get('/apply/12345?mis=001').accept(APPLICATION_JSON))
        def response = result.andReturn().response
        assert response.content != null
        def resultApp = mapper.readValue(response.content.toString(), Application.class)

        then:
        1 * groovyService.run(*_) >> app
        result.andExpect(status().isOk())
        resultApp.appId == app.appId
        resultApp.cccId == app.cccId
        resultApp.termId == app.termId
    }

    def "POST"() {
        def result
        def app = new Application(appId: 12345, cccId: "ABC123", collegeId: "001", termId: 1)
        app.setSupplementalQuestions(new SupplementalQuestions());
        def mapper = new ObjectMapper()

        when:
        result = mockMvc.perform(post('/apply?mis=001').contentType(APPLICATION_JSON)
                .content(mapper.writeValueAsString(app)))
        def response = result.andReturn().response
        assert response.content != null
        def resultApp = mapper.readValue(response.content.toString(), Application.class)

        then:
        1 * groovyService.run(*_) >> app
        result.andExpect(status().isCreated())
        resultApp.appId == app.appId
        resultApp.cccId == app.cccId
        resultApp.termId == app.termId
        response.headers["Location"].toString().contains("/apply/12345?mis=001")
    }

    def "POSTSupplementalDates"() {
        def result
        def app = new Application(appId: 12345, cccId: "ABC123", collegeId: "001", termId: 1)
        def suppQ = new SupplementalQuestions();
        suppQ.setSuppDate01(new LocalDate(19,1,1));
        suppQ.setSuppDate02(new LocalDate(19,1,1));
        suppQ.setSuppDate03(new LocalDate(19,1,1));
        suppQ.setSuppDate04(new LocalDate(19,1,1));
        suppQ.setSuppDate05(new LocalDate(19,1,1));
        app.setSupplementalQuestions(suppQ);

        def mapper = new ObjectMapper()
        mapper.registerModule(new JavaTimeModule());
        mapper.configure(SerializationFeature.WRITE_DATES_AS_TIMESTAMPS, true);

        when:
        result = mockMvc.perform(post('/apply?mis=001').contentType(APPLICATION_JSON)
                .content(mapper.writeValueAsString(app)))
        def response = result.andReturn().response
        assert response.content != null
        def resultApp = mapper.readValue(response.content.toString(), Application.class)

        then:
        1 * groovyService.run(*_) >> app
        result.andExpect(status().isCreated())
        resultApp.appId == app.appId
        resultApp.cccId == app.cccId
        resultApp.termId == app.termId
        response.headers["Location"].toString().contains("/apply/12345?mis=001")
    }


}
