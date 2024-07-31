package com.ccctc.adaptor.controller

import com.ccctc.adaptor.model.apply.Application
import com.ccctc.adaptor.model.placement.StudentPlacementData
import com.ccctc.adaptor.util.GroovyService
import com.fasterxml.jackson.databind.ObjectMapper
import groovy.json.JsonBuilder
import org.springframework.test.web.servlet.MockMvc
import org.springframework.test.web.servlet.setup.MockMvcBuilders
import spock.lang.Specification

import static org.springframework.http.MediaType.APPLICATION_JSON
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status

class StudentPlacementControllerSpec extends Specification {
         MockMvc mockMvc
        GroovyService groovyService

        def setup() {
            groovyService = Mock(GroovyService)

            def controller = new StudentPlacementController(groovyService: groovyService)
            mockMvc = MockMvcBuilders.standaloneSetup(controller).build()
        }

    def "Student Placement Controller get"() {
        def mapper = new ObjectMapper()
           def result
        def placement = new StudentPlacementData(miscode: "683",californiaCommunityCollegeId: "AAC9999",statewideStudentId: "111121211")
            when:
            result = mockMvc.perform(get('/student-placements/AAC9999?mis=683').accept(APPLICATION_JSON))
            def response = result.andReturn().response
            assert response.content != null
            def resultplacement = mapper.readValue(response.content.toString(), StudentPlacementData.class)
            then:
            1 * groovyService.run(*_) >> placement
            result.andExpect(status().isOk())
            resultplacement.californiaCommunityCollegeId == placement.californiaCommunityCollegeId
            resultplacement.statewideStudentId == placement.statewideStudentId
            resultplacement.miscode == placement.miscode
        }

    def "Student Placement Controller post"() {
        setup:
        def groovyUtil = Mock( GroovyService )
        def controller = new StudentPlacementController(groovyService: groovyUtil)
        def mockMvc = MockMvcBuilders.standaloneSetup(controller).build()
        def mapper = new ObjectMapper()

        /*
        example:
            {
              "dataSource": 3,
              "english": 1,
              "slam": 1,
              "stem": 1,
              "isAlgI": true,
              "isAlgII": false,
              "trigonometry": false,
              "preCalculus": false,
              "calculus": false
            }
         */
        def placementData = new StudentPlacementData.Builder()
                .dataSource(1)
                .english(1)
                .slam(1)
                .stem(1)
                .isAlgI(true)
                .isAlgII(false)
                .trigonometry(false)
                .preCalculus(false)
                .calculus(false);


        def result, resultPlacementTransaction = null

        when:
        result = mockMvc.perform(post('/student-placements?mis=001&cccId=AAC3706&ssId=B123456')
                        .contentType(APPLICATION_JSON)
                        .content(new JsonBuilder(placementData).toString()))

        then:
        result.andExpect(status().isAccepted())

    }
}