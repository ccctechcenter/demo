package com.ccctc.adaptor.controller

import com.ccctc.adaptor.controller.StudentApiController
import com.ccctc.adaptor.exception.ApiExceptionHandler
import com.ccctc.adaptor.model.ApplicationStatus
import com.ccctc.adaptor.model.Cohort
import com.ccctc.adaptor.model.CohortTypeEnum
import com.ccctc.adaptor.model.OrientationStatus
import com.ccctc.adaptor.model.ResidentStatus
import com.ccctc.adaptor.model.Student
import com.ccctc.adaptor.model.StudentHomeCollege
import com.ccctc.adaptor.util.GroovyService
 import com.fasterxml.jackson.databind.ObjectMapper;
 import com.fasterxml.jackson.databind.ObjectWriter;
import com.google.common.collect.Lists
import org.springframework.test.web.servlet.setup.MockMvcBuilders
import spock.lang.Specification

import static org.hamcrest.Matchers.is;
import static org.springframework.http.MediaType.APPLICATION_JSON
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.delete
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.patch
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.put
import static org.springframework.test.web.servlet.result.MockMvcResultHandlers.print
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.content
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status

/**
 * Created by jrscanlon on 12/14/15.
 */
class StudentControllerSpec extends Specification {

    def groovyUtil
    def studentApiController
    def mockMvc

    def setup() {
        setup:
        this.groovyUtil = Mock(GroovyService)
        this.studentApiController = new StudentApiController(groovyService: groovyUtil)
        this.mockMvc = MockMvcBuilders.standaloneSetup(studentApiController)
                .setControllerAdvice(new ApiExceptionHandler())
                .build()
    }

    def "Student Controller get All by Term"() {
        when:
        def result = mockMvc.perform(get('/students?sisTermId=201220&sisTermId=201221&mis=001').contentType(APPLICATION_JSON))

        then:
        1 * groovyUtil.run(_, _, _, _) >> [
                new Student.Builder().cccid("cccid1").misCode("001").orientationStatus(OrientationStatus.COMPLETED).residentStatus(ResidentStatus.Resident).sisTermId("201220").isIncarcerated(false).isConcurrentlyEnrolled(false).build(),
                new Student.Builder().cccid("cccid2").misCode("001").orientationStatus(OrientationStatus.REQUIRED).residentStatus(ResidentStatus.Resident).sisTermId("201221").isIncarcerated(false).isConcurrentlyEnrolled(false).build()
        ]
        result.andExpect(status().isOk())
        result.andExpect(content().json('[ {"cccid":"cccid1","orientationStatus":"COMPLETED","residentStatus":"Resident"}, {"cccid":"cccid2","orientationStatus":"REQUIRED","residentStatus":"Resident"}]'))

    }

    def "Student Controller"() {
        when:
        def result = mockMvc.perform(get('/students/cccid?sisTermId=201220&mis=001').contentType(APPLICATION_JSON))

        then:
        1 * groovyUtil.run(_, _, _, _) >> new Student.Builder().cccid("cccid").residentStatus(ResidentStatus.Resident).sisTermId("termid").orientationStatus(OrientationStatus.COMPLETED).isIncarcerated(false).isConcurrentlyEnrolled(false).build()
        result.andExpect(status().isOk())
        result.andExpect(content().json('{"cccid":"cccid","orientationStatus":"COMPLETED","residentStatus":"Resident"}'))

    }

    def "Student Controller Home College"() {
        when:
        def result = mockMvc.perform(get('/students/homecollege/cccid?mis=001').contentType(APPLICATION_JSON))

        then:
        1 * groovyUtil.run(_, _, _, _) >> new StudentHomeCollege.Builder().cccid("cccid").misCode("misCode").build()
        result.andExpect(status().isOk())
        result.andExpect(content().json('{"cccid":"cccid","misCode":"misCode"}'))

    }

    def cohort1 = new Cohort.Builder()
            .name(CohortTypeEnum.COURSE_EXCHANGE)
            .build()

    List<Cohort> cohorts = Lists.asList(cohort1)

    def student = new Student.Builder()
            .cccid("ACF60709")
            .sisPersonId("abc123")
            .sisTermId("2014FA")
            .visaType("NA")
            .hasCaliforniaAddress(true)
            .orientationStatus(OrientationStatus.COMPLETED)
            .hasEducationPlan(true)
            .hasMathAssessment(true)
            .hasEnglishAssessment(true)
            .applicationStatus(ApplicationStatus.ApplicationAccepted)
            .residentStatus(ResidentStatus.Resident)
            .hasHold(false)
            .hasFinancialAidAward(false)
            .hasBogfw(true)
            .accountBalance(0)
            .dspsEligible(true)
            .isIncarcerated(false)
            .cohorts(cohorts)
            .isConcurrentlyEnrolled(false)
            .isActive(true)
            .build()

    def testJson = '''
           {
    "cccid": "ACF60709",
    "sisPersonId": "abc123",
    "sisTermId": "2014FA",
    "visaType": "NA",
    "hasCaliforniaAddress": true,
    "hasAttendedOrientation": true,
    "hasEducationPlan": true,
    "hasMathAssessment": true,
    "hasEnglishAssessment": true,
    "applicationStatus": "ApplicationAccepted",
    "residentStatus": "Resident",
    "hasHold": false,
    "registrationDate": 1481309218278,
    "hasFinancialAidAward": false,
    "hasBogfw": true,
    "accountBalance": 0,
    "dspsEligible": true,
    "incarcerated": false,
    "cohorts" : [{"name" : "COURSE_EXCHANGE","description" : "Course Exchange"}],
    "concurrentlyEnrolled": false,
    "isActive": true
  }'''

    def "Student Controller PostCohort"() {
        when:
        def result = mockMvc.perform(post('/students/ACF6077/cohorts/COURSE_EXCHANGE?mis=001&sisTermId=2014FA').contentType(APPLICATION_JSON))

        then:
        1 * groovyUtil.run(_, _, _, _)
        result.andExpect(status().isNoContent())
    }

    def "Student Controller deleteCohort"() {
        when:
        def result = mockMvc.perform(delete('/students/ACF6077/cohorts/COURSE_EXCHANGE?mis=001&sisTermId=2014FA').contentType(APPLICATION_JSON))

        then:
        1 * groovyUtil.run(_, _, _, _)
        result.andExpect(status().isNoContent())
    }

    def "cohort extra coverage"() {
        when:
        def cohort = new Cohort.Builder().name(CohortTypeEnum.COURSE_EXCHANGE).build()

        then:
        cohort.getName() == CohortTypeEnum.COURSE_EXCHANGE
        cohort.getDescription() == CohortTypeEnum.COURSE_EXCHANGE.getDescription()
    }

    def "Student Get cccids Controller"() {
        setup:
        def cccIds = ["cccId"]

        when:
        def result = mockMvc.perform(get('/students/sisPersonId/cccids?mis=001').contentType(APPLICATION_JSON))

        then:
        1 * groovyUtil.run(_, _, _, _) >> cccIds
        result.andExpect(status().isOk())
    }

    def "Student CCcID Post Controller"() {
        setup:
        def cccIds = ["cccId"]

        when:
        def result = mockMvc.perform(post('/students/sisPersonId/cccids/cccid?mis=001').contentType(APPLICATION_JSON))

        then:
        1 * groovyUtil.run(_, _, _, _) >> cccIds
        result.andExpect(status().isOk())
    }

    def "Student Patch Controller"() {
        setup:
        def misCode = '001'
        def termId = "202006"
        def cccId = 'AAA1111'
        def updates = new Student.Builder().misCode(misCode).cccid(cccId).sisTermId(termId).orientationStatus(OrientationStatus.COMPLETED).build()
        def json = toJson(updates)

        when:
        def url = '/students/' + cccId + '?mis=' + misCode + '&sisTermId=' + termId;
        def result = mockMvc.perform(patch(url).contentType(APPLICATION_JSON).content(json)).andDo(print())

        then:
        result.andExpect(status().isNoContent())
        result.andExpect(content().string(""))
        1 * groovyUtil.run(misCode, 'Student', 'patch', _)
    }

    def "Student Patch Controller with no body"() {
        when:
        def result = mockMvc.perform(put('/students/cccid?mis=001').contentType(APPLICATION_JSON)).andDo(print())

        then:
        result.andExpect(status().is4xxClientError())
        result.andExpect(content().string(''))
        // would be better to return an error message
        //result.andExpect(jsonPath('$.code').value('invalidRequest'))
        //result.andExpect(jsonPath('$.message').value('???'))
        0 * groovyUtil.run(_, _, _, _)
    }

    def String toJson(Object obj) {
        ObjectMapper mapper = new ObjectMapper();
        ObjectWriter ow = mapper.writer().withDefaultPrettyPrinter();
        return ow.writeValueAsString(obj);
    }

}
