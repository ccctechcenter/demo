package com.ccctc.adaptor.controller

import com.ccctc.adaptor.model.Person
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
class PersonControllerSpec extends Specification {

    def "Person Controller"() {
        setup:
        def groovyUtil = Mock( GroovyService )
        def personApiController = new PersonApiController(groovyService: groovyUtil)
        def mockMvc = MockMvcBuilders.standaloneSetup(personApiController).build()

        def result

        when:
        result = mockMvc.perform(get('/persons/personid?mis=001').contentType(APPLICATION_JSON))

        then:
        1 * groovyUtil.run(_,_,_,_) >> new Person.Builder().misCode("mis").sisPersonId("personid").firstName("Marge").lastName("Simpson").loginId("msimpson").build()
        result.andExpect(status().isOk())
        result.andExpect(content().json('{"misCode":"mis","sisPersonId":"personid","firstName":"Marge","lastName":"Simpson","emailAddresses":null,"loginId":"msimpson"}'))
    }

    def "Person Controller getStudentPerson"() {
        setup:
        def groovyUtil = Mock( GroovyService )
        def personApiController = new PersonApiController(groovyService: groovyUtil)
        def mockMvc = MockMvcBuilders.standaloneSetup(personApiController).build()

        def result

        when:
        result = mockMvc.perform(get('/persons/student?mis=001&sisPersonId=person1&cccid=abc123&eppn=test@college.edu').contentType(APPLICATION_JSON))

        then:
        1 * groovyUtil.run(_,_,_,_) >> new Person.Builder().misCode("mis").sisPersonId("personid").firstName("Marge").lastName("Simpson").loginId("msimpson").loginSuffix("test@college.edu").cccid("cccid").build()
        result.andExpect(status().isOk())
        result.andExpect(content().json('{"misCode":"mis","sisPersonId":"personid","firstName":"Marge","lastName":"Simpson","emailAddresses":null,"loginId":"msimpson","loginSuffix":"test@college.edu","cccid":"cccid"}'))

        // ensure it works if you only pass one of sisPersonId, cccid and eppn
        when:
        def result1 = mockMvc.perform(get('/persons/student?mis=001&sisPersonId=person1').contentType(APPLICATION_JSON))
        def result2 = mockMvc.perform(get('/persons/student?mis=001&cccid=abc123').contentType(APPLICATION_JSON))
        def result3 = mockMvc.perform(get('/persons/student?mis=001&eppn=test@college.edu').contentType(APPLICATION_JSON))

        then:
        3 * groovyUtil.run(_,_,_,_)
        result1.andExpect(status().isOk())
        result2.andExpect(status().isOk())
        result3.andExpect(status().isOk())
    }

    def "Person Controller getStudentPerson invalid params"() {
        setup:
        def groovyUtil = Mock( GroovyService )
        def personApiController = new PersonApiController(groovyService: groovyUtil)
        def mockMvc = MockMvcBuilders.standaloneSetup(personApiController).build()

        def result

        when:
        result = mockMvc.perform(get('/persons/student?mis=001').contentType(APPLICATION_JSON))

        then:
        result.andExpect(status().is(400))
    }


    def "Person Controller getAll sisPersonIds"() {
        setup:
        def groovyUtil = Mock( GroovyService )
        def personApiController = new PersonApiController(groovyService: groovyUtil )
        def mockMvc = MockMvcBuilders.standaloneSetup(personApiController).build()

        def result

        when:
        result = mockMvc.perform(get('/persons?sisPersonIds=person1&mis=001').contentType(APPLICATION_JSON))

        then:
        1 * groovyUtil.run(_,_,_,_) >> [new Person.Builder().misCode("mis").sisPersonId("personid").firstName("Marge").lastName("Simpson").loginId("msimpson").build()]
        result.andExpect(status().isOk())
        result.andExpect(content().json('[{"misCode":"mis","sisPersonId":"personid","firstName":"Marge","lastName":"Simpson","emailAddresses":null,"loginId":"msimpson"}]'))
    }

    def "Person Controller getAll CCC ID"() {
        setup:
        def groovyUtil = Mock( GroovyService )
        def personApiController = new PersonApiController(groovyService: groovyUtil)
        def mockMvc = MockMvcBuilders.standaloneSetup(personApiController).build()

        def result

        when:
        result = mockMvc.perform(get('/persons?mis=mis&cccids=cccid').contentType(APPLICATION_JSON))

        then:
        1 * groovyUtil.run(_,_,_,_) >> [new Person.Builder().misCode("mis").sisPersonId("personid").firstName("Marge").lastName("Simpson").loginId("msimpson").build()]
        result.andExpect(status().isOk())
        result.andExpect(content().json('[{"misCode":"mis","sisPersonId":"personid","firstName":"Marge","lastName":"Simpson","emailAddresses":null,"loginId":"msimpson"}]'))
    }

    def "Person Controller getAll invalid parameters"() {
        setup:
        def groovyUtil = Mock( GroovyService )
        def personApiController = new PersonApiController(groovyService: groovyUtil)
        def mockMvc = MockMvcBuilders.standaloneSetup(personApiController).build()

        def result

        when:
        result = mockMvc.perform(get('/persons?mis=mis').contentType(APPLICATION_JSON))

        then:
        result.andExpect(status().is(400))
    }
}
