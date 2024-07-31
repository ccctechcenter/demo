package com.ccctc.adaptor.controller

import com.ccctc.adaptor.controller.PlacementController
import com.ccctc.adaptor.model.placement.CompetencyMapDiscipline
import com.ccctc.adaptor.model.placement.DataSourceType
import com.ccctc.adaptor.model.placement.ElaIndicator
import com.ccctc.adaptor.model.placement.Placement
import com.ccctc.adaptor.model.placement.PlacementComponentCCCAssess
import com.ccctc.adaptor.model.placement.PlacementComponentMmap
import com.ccctc.adaptor.model.placement.PlacementComponentType
import com.ccctc.adaptor.model.placement.PlacementCourse
import com.ccctc.adaptor.model.placement.PlacementSubjectArea
import com.ccctc.adaptor.model.placement.PlacementTransaction
import com.ccctc.adaptor.util.GroovyService
import com.fasterxml.jackson.databind.ObjectMapper
import org.springframework.test.web.servlet.setup.MockMvcBuilders
import spock.lang.Specification

import static org.springframework.http.MediaType.APPLICATION_JSON
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status

/**
 * Created by zekeo on 6/14/16.
 */
class PlacementControllerSpec extends Specification {

    def "Assessment Controller post"() {
        setup:
        def groovyUtil = Mock(GroovyService)
        def controller = new PlacementController(groovyService: groovyUtil)
        def mockMvc = MockMvcBuilders.standaloneSetup(controller).build()
        def mapper = new ObjectMapper()

        def placementComponentMmap = new PlacementComponentMmap.Builder()
                .dataSource("dataSource")
                .dataSourceDate(new Date())
                .dataSourceType(DataSourceType.SelfReported)
                .mmapCb21Code("A".toCharacter())
                .mmapQualifiedCourses(["course-a", "course-b"])
                .build()

        def placementCourse1 = new PlacementCourse.Builder()
                .number("1")
                .sisTestMappingLevel("1000")
                .subject("course")
                .build()

        def placementCourse2 = new PlacementCourse.Builder()
                .number("2")
                .sisTestMappingLevel("2000")
                .subject("course")
                .build()

        def component1 = new PlacementComponentCCCAssess.Builder()
                .assessmentAttemptId("assessmentAttemptId")
                .assessmentTitle("assessmentTitle")
                .completeDate(new Date())
                .cb21("A".toCharacter())
                .courseGroup('courseGroup')
                .courses([placementCourse1])
                .elaIndicator(ElaIndicator.English)
                .placementComponentDate(new Date())
                .placementComponentId("placementComponentId1")
                .trigger("trigger")
                .build()

        def component2 = new PlacementComponentMmap.Builder()
                .dataSource("dataSource")
                .dataSourceDate(new Date())
                .dataSourceType(DataSourceType.SelfReported)
                .mmapCb21Code("A".toCharacter())
                .mmapQualifiedCourses(["course-a", "course-b"])
                .cb21("A".toCharacter())
                .courseGroup('courseGroup')
                .courses([placementCourse2])
                .elaIndicator(ElaIndicator.English)
                .placementComponentDate(new Date())
                .placementComponentId("placementComponentId2")
                .trigger("trigger")
                .build()

        def placement = new Placement.Builder()
                .assignedDate(new Date())
                .cb21("A".toCharacter())
                .elaIndicator(ElaIndicator.English)
                .courseGroup("courseGroup")
                .courses([placementCourse1, placementCourse2])
                .isAssigned(true)
                .placementComponentIds(["placementComponentId1", "placementComponentId2"])
                .placementDate(new Date())
                .placementId("placementId")
                .build()

        def subjectArea = new PlacementSubjectArea.Builder()
                .competencyMapDiscipline(CompetencyMapDiscipline.English)
                .optInMmap(true)
                .optInMultipleMeasures(true)
                .optInSelfReported(true)
                .placementComponents([component1, component2])
                .placements([placement])
                .sisTestName("sisTestName")
                .subjectAreaId(0)
                .subjectAreaVersion(0)
                .title("title")
                .build()

        def placementTransaction = new PlacementTransaction.Builder()
                .misCode("mis")
                .cccid("cccid")
                .eppn("student@college.edu")
                .subjectArea(subjectArea)
                .build()

        def result, resultPlacementTransaction = null

        when:
        result = mockMvc.perform(post('/placements?mis=001').contentType(APPLICATION_JSON).content(mapper.writeValueAsString(placementTransaction)))

        then:
        1 * groovyUtil.run(_, _, _, _) >> {
            // capture the value passed to the groovy code so we can check it
            args -> resultPlacementTransaction = args[3][1]
        }
        0 * _
        result.andExpect(status().isNoContent())
        resultPlacementTransaction instanceof PlacementTransaction
        resultPlacementTransaction.subjectArea.placements.size() == 1
        resultPlacementTransaction.subjectArea.placementComponents.size() == 2
        resultPlacementTransaction.subjectArea.placementComponents[0].getType() == PlacementComponentType.CCCAssess
        resultPlacementTransaction.subjectArea.placementComponents[0] instanceof PlacementComponentCCCAssess
        resultPlacementTransaction.subjectArea.placementComponents[1].getType() == PlacementComponentType.Mmap
        resultPlacementTransaction.subjectArea.placementComponents[1] instanceof PlacementComponentMmap
        resultPlacementTransaction.toString() == placementTransaction.toString()
    }
}