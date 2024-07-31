package com.ccctc.adaptor.controller

import com.ccctc.adaptor.controller.CourseApiController
import com.ccctc.adaptor.model.Course
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
class CourseControllerSpec extends Specification {

    def "Course Controller"() {
        setup:
        def groovyUtil = Mock( GroovyService )
        def courseApiController = new CourseApiController(groovyService: groovyUtil)
        def mockMvc = MockMvcBuilders.standaloneSetup(courseApiController).build()
        def result

        when:
        result = mockMvc.perform( get('/courses/eng101?mis=111').contentType(APPLICATION_JSON))

        then:
        1 * groovyUtil.run(_,_,_,_) >> new Course.Builder().c_id("c_id").subject("eng").number("101").description("College English").build()
        result.andExpect(status().isOk())
        result.andExpect(content().json('{"subject":"eng","c_id":"c_id","number":"101","description":"College English"}'))
    }
}
