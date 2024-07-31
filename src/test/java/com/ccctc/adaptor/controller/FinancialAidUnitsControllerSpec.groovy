package com.ccctc.adaptor.controller

import com.ccctc.adaptor.controller.FinancialAidUnitsController
import com.ccctc.adaptor.model.CourseExchangeEnrollment
import com.ccctc.adaptor.model.FinancialAidUnits
import com.ccctc.adaptor.model.Section
import com.ccctc.adaptor.util.GroovyService
import groovy.json.JsonBuilder
import org.springframework.test.web.servlet.setup.MockMvcBuilders
import spock.lang.Specification

import static org.springframework.http.MediaType.APPLICATION_JSON
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status

/**
 * Created by bstout on 4/23/17.
 */
class FinancialAidUnitsControllerSpec extends Specification {

    def mockMvc
    def groovyUtil

    def setup() {
        groovyUtil = Mock(GroovyService)

        def financialAidController = new FinancialAidUnitsController(groovyService: groovyUtil)
        mockMvc = MockMvcBuilders.standaloneSetup(financialAidController).build()

    }

    def "GET"() {
        def result

        // get all
        when:
        result = mockMvc.perform(get('/faunits/cccid/sisTermId?mis=001').accept(APPLICATION_JSON))

        then:
        result.andExpect(status().isOk())

    }

    def "POST"() {
        def result

        FinancialAidUnits.Builder faBuilder = new FinancialAidUnits.Builder()
        faBuilder.misCode("001")
        faBuilder.sisPersonId("test-person")
        faBuilder.cccid("ABC123")
        faBuilder.sisTermId("2017FA")

        CourseExchangeEnrollment.Builder eBuilder = new CourseExchangeEnrollment.Builder()
        eBuilder.misCode("002")
        eBuilder.collegeName("Test College")
        eBuilder.units(3.0)
        eBuilder.c_id("ENGL 100")

        Section.Builder sBuilder = new Section.Builder()
        sBuilder.sisSectionId("1234")
        sBuilder.sisTermId("201703")
        sBuilder.title("Transfer English")

        eBuilder.section(sBuilder.build())
        faBuilder.ceEnrollment(eBuilder.build())
        FinancialAidUnits financialAidUnits = faBuilder.build()

        when:
        result = mockMvc.perform(post('/faunits?mis=001')
                .contentType(APPLICATION_JSON)
                .content(new JsonBuilder(financialAidUnits).toString()))

        then:
        result.andExpect(status().isCreated())

    }

    def "DELETE"() {
        def result

        when:
        result = mockMvc.perform(delete('/faunits/ABC123/201703/ENGL%20100?mis=001&enrolledMisCode=002').contentType(APPLICATION_JSON))

        then:
        result.andExpect(status().is(204))
    }
}
