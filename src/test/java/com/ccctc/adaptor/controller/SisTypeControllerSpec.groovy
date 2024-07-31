package com.ccctc.adaptor.controller

import com.ccctc.adaptor.controller.SisTypeApiController
import org.springframework.core.env.Environment
import org.springframework.test.web.servlet.setup.MockMvcBuilders
import spock.lang.Specification

import static org.springframework.http.MediaType.APPLICATION_JSON
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.content
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status

/**
 * Created by jrscanlon on 12/14/15.
 */
class SisTypeControllerSpec extends Specification {

    def "SisType Controller"() {
        setup:
        def environment = Mock( Environment )
        def SisTypeApiController sisTypeApiController = new SisTypeApiController()
        def mockMvc = MockMvcBuilders.standaloneSetup(sisTypeApiController).build()
        environment.getProperty("sisType") >> "colleague"
        sisTypeApiController.environment = environment

        def result

        when:
        result = mockMvc.perform(get('/sistype?mis=123').contentType(APPLICATION_JSON))

        then:
        result.andExpect(status().isOk())
        result.andExpect(content().json('{"misCode":"123","sisType":"colleague"}'))

    }

}
