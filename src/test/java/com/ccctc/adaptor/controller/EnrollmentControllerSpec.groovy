package com.ccctc.adaptor.controller

import com.ccctc.adaptor.controller.EnrollmentApiController
import com.ccctc.adaptor.model.EnrollmentStatus
import com.ccctc.adaptor.model.PrerequisiteStatus
import com.ccctc.adaptor.model.PrerequisiteStatusEnum
import com.ccctc.adaptor.model.Enrollment
import com.ccctc.adaptor.util.GroovyService
import org.springframework.test.web.servlet.setup.MockMvcBuilders
import spock.lang.Specification

import static org.springframework.http.MediaType.APPLICATION_JSON
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.put
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.content
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status

/**
 * Created by jrscanlon on 12/14/15.
 */
class EnrollmentControllerSpec extends Specification {
    def enrollments = new ArrayList<Enrollment>()
    def preReqStatus = new PrerequisiteStatus()

    def "Enrollment Controller getPreReqStatus"() {
        setup:
        def groovyUtil = Mock( GroovyService )
        def enrollmentApiController = new EnrollmentApiController(groovyService: groovyUtil )
        def mockMvc = MockMvcBuilders.standaloneSetup(enrollmentApiController).build()
        preReqStatus = new PrerequisiteStatus.Builder().status(PrerequisiteStatusEnum.Complete).message("message").build()
        enrollments.add(new Enrollment.Builder().cccid("cccid").sisTermId("termid").sisSectionId("sectionid").grade("grade").build())
        Long startDate = new Date(2013,1,2).getTime()
        String requestUrl = String.format('/enrollments/prerequisitestatus/%s?sisTermId=%s&start=%s&cccid=%s&mis=%s',
                'courseid', 'termId', startDate, '123', '001')
        def result

        when:
        result = mockMvc.perform(get(requestUrl).contentType(APPLICATION_JSON))

        then:
        1 * groovyUtil.run(_ , _ ,_ ,_) >> preReqStatus
        result.andExpect(status().isOk())
        result.andExpect(content().json('{"status":"Complete","message":"message"}'))
    }

    def "Enrollment Controller getPreReqStatus not complete"() {
        setup:
        def groovyUtil = Mock( GroovyService )
        def enrollmentApiController = new EnrollmentApiController(groovyService: groovyUtil )
        def mockMvc = MockMvcBuilders.standaloneSetup(enrollmentApiController).build()
        preReqStatus = new PrerequisiteStatus.Builder().status(PrerequisiteStatusEnum.Incomplete).message("message").build()
        enrollments.add(new Enrollment.Builder().cccid("cccid").sisTermId("termid").sisSectionId("sectionid").grade("grade").build())

        def result
        Long startDate = new Date(2013,1,2).getTime();
        String requestUrl = String.format('/enrollments/prerequisitestatus/%s?start=%s&cccid=%s&mis=%s','courseid', startDate, '123', '001')
        when:
        result = mockMvc.perform(get(requestUrl).contentType(APPLICATION_JSON))

        then:
        1 * groovyUtil.run(_ , _ ,_ ,_) >> preReqStatus
        result.andExpect(status().isOk())
        result.andExpect(content().json('{"status":"Incomplete","message":"message"}'))
    }

    def "Enrollment Controller getPreReqStatus pending"() {
        setup:
        def groovyUtil = Mock( GroovyService )
        def enrollmentApiController = new EnrollmentApiController(groovyService: groovyUtil )
        def mockMvc = MockMvcBuilders.standaloneSetup(enrollmentApiController).build()
        preReqStatus = new PrerequisiteStatus.Builder().status(PrerequisiteStatusEnum.Pending).message("message").build()
        enrollments.add(new Enrollment.Builder().cccid("cccid").sisTermId("termid").sisSectionId("sectionid").grade("grade").build())

        Long startDate = new Date(2013,1,2).getTime();
        String requestUrl = String.format('/enrollments/prerequisitestatus/%s?start=%s&cccid=%s&mis=%s','courseid',startDate,'123','001')

        def result
        when:
        result = mockMvc.perform(get(requestUrl).contentType(APPLICATION_JSON))

        then:
        1 * groovyUtil.run(_ , _ ,_ ,_) >> preReqStatus
        result.andExpect(status().isOk())
        result.andExpect(content().json('{"status":"Pending","message":"message"}'))
    }

    def "Enrollment Controller section"() {
        setup:
        def groovyUtil = Mock( GroovyService )
        def enrollmentApiController = new EnrollmentApiController(groovyService: groovyUtil )
        def mockMvc = MockMvcBuilders.standaloneSetup(enrollmentApiController).build()
        enrollments.add(new Enrollment.Builder()
                .cccid("cccid").sisPersonId("studentid").sisTermId("termid").sisSectionId("sectionid")
                .enrollmentStatus(EnrollmentStatus.Enrolled).units(0).passNoPass(false).audit(false)
                .grade("grade").sisCourseId("course-id").c_id("cid").title("title")
                .build())

        def result

        when:
        result = mockMvc.perform(get('/enrollments/section/sectionid?sisTermId=termid&mis=001').contentType(APPLICATION_JSON))

        then:
        1 * groovyUtil.run(_ , _ ,_ ,_) >> enrollments
        result.andExpect(status().isOk())
        result.andExpect(content().json('[{"cccid":"cccid","sisPersonId":"studentid","sisTermId":"termid","sisSectionId":"sectionid","enrollmentStatus":"Enrolled","units":0,"passNoPass":false,"audit":false,"grade":"grade","sisCourseId":"course-id","c_id":"cid","title":"title"}]'))

    }

    def "Enrollment Controller student"() {
        setup:
        def groovyUtil = Mock( GroovyService )
        def enrollmentApiController = new EnrollmentApiController(groovyService: groovyUtil )
        def mockMvc = MockMvcBuilders.standaloneSetup(enrollmentApiController).build()
        enrollments.add(new Enrollment.Builder()
                .cccid("cccid").sisPersonId("studentid").sisTermId("termid").sisSectionId("sectionid")
                .enrollmentStatus(EnrollmentStatus.Enrolled).units(0).passNoPass(false).audit(false)
                .grade("grade").gradeDate(new Date(2014,1,1,0,0,0)).sisCourseId("course-id").c_id("cid").title("title")
                .enrollmentStatusDate( new Date(2014,1,1,0,0,0)).lastDateAttended(new Date(2014,1,1,0,0,0))
                .build())

        def result

        when:
        result = mockMvc.perform(get('/enrollments/student/cccid?sisTermId=termid&mis=001').contentType(APPLICATION_JSON))

        then:
        1 * groovyUtil.run(_ , _ ,_ ,_) >> enrollments
        result.andExpect(status().isOk())
        result.andExpect(content().json('[{"cccid":"cccid","sisPersonId":"studentid","sisTermId":"termid","sisSectionId":"sectionid","enrollmentStatus":"Enrolled","units":0,"passNoPass":false,"audit":false,"grade":"grade","sisCourseId":"course-id","c_id":"cid","title":"title"}]'))

    }

    def "Enrollment Controller post"() {
        setup:
        def groovyUtil = Mock( GroovyService )
        def enrollmentApiController = new EnrollmentApiController(groovyService: groovyUtil )
        def mockMvc = MockMvcBuilders.standaloneSetup(enrollmentApiController).build()
        def enrollment = new Enrollment.Builder().cccid("cccid").sisTermId("termid").sisSectionId("sectionid").grade("grade").build()

        def result

        when:
        result = mockMvc.perform(post('/enrollments?mis=001').contentType(APPLICATION_JSON).content('{"cccid":"cccid","sisTermId":"termid","sisSectionId":"sectionid","grade":"grade"}'))

        then:
        1 * groovyUtil.run(_ , _ ,_ ,_) >> enrollment
        result.andExpect(status().isCreated())
        result.andExpect(content().json('{"cccid":"cccid","sisTermId":"termid","sisSectionId":"sectionid","grade":"grade"}'))

    }

    def "Enrollment Controller update"() {
        setup:
        def groovyUtil = Mock( GroovyService )
        def enrollmentApiController = new EnrollmentApiController(groovyService: groovyUtil )
        def mockMvc = MockMvcBuilders.standaloneSetup(enrollmentApiController).build()
        def enrollment = new Enrollment.Builder().cccid("cccid").sisTermId("termid").sisSectionId("sectionid").grade("grade").build()

        def result

        when:
        result = mockMvc.perform(put('/enrollments/cccid?sisTermId=termid&sisSectionId=sectionid&mis=001').contentType(APPLICATION_JSON).content('{"cccid":"cccid","sisTermId":"termid","sisSectionId":"sectionid","grade":"grade"}'))

        then:
        1 * groovyUtil.run(_ , _ ,_ ,_) >> enrollment
        result.andExpect(status().isOk())
        result.andExpect(content().json('{"cccid":"cccid","sisTermId":"termid","sisSectionId":"sectionid","grade":"grade"}'))

    }
}
