package com.ccctc.adaptor.controller

import com.ccctc.adaptor.controller.BOGWaiverController
import com.ccctc.adaptor.model.BOGWaiver
import com.ccctc.adaptor.model.MaritalStatus
import com.ccctc.adaptor.util.GroovyService
import groovy.json.JsonBuilder
import org.springframework.test.web.servlet.setup.MockMvcBuilders
import spock.lang.Specification

import static org.springframework.http.MediaType.APPLICATION_JSON
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status

class BOGWaiverControllerSpec extends Specification {

    def mockMvc
    def groovyUtil, environment

    def setup() {
        groovyUtil = Mock(GroovyService)

        BOGWaiverController bogWaiverController = new BOGWaiverController(groovyService: groovyUtil)
        mockMvc = MockMvcBuilders.standaloneSetup(bogWaiverController).build()
    }

    def "GET"() {
        def result, body

        // get all
        when:
        result = mockMvc.perform(get('/bogfw/cccid/sisTermId?mis=001').accept(APPLICATION_JSON))

        then:
        result.andExpect(status().isOk())
    }

    def "POST"() {
        def result, body

        BOGWaiver.Builder builder = new BOGWaiver.Builder();

        builder.cccid("ABC123")
        builder.sisTermId("2017FA")

        builder.maritalStatus(MaritalStatus.MARRIED)
        builder.regDomPartner(true)
        builder.bornBefore23Year(true)
        builder.marriedOrRDP(true)
        builder.usVeteran(true)
        builder.dependents(true)
        builder.parentsDeceased(true)
        builder.emancipatedMinor(true)
        builder.legalGuardianship(true)
        builder.homelessYouthSchool(true)
        builder.homelessYouthHUD(true)
        builder.homelessYouthOther(true)
        builder.dependentOnParentTaxes(com.ccctc.adaptor.model.BOGWaiver.DependentOnParentTaxesEnum.PARENTS_NOT_FILE)
        builder.livingWithParents(true)
        builder.dependencyStatus(com.ccctc.adaptor.model.BOGWaiver.DependencyStatus.DEPENDENT)
        builder.certVeteranAffairs(true)
        builder.certNationalGuard(true)
        builder.eligMedalHonor(true)
        builder.eligSept11(true)
        builder.eligPoliceFire(true)
        builder.tanfCalworks(true)
        builder.ssiSSP(true)
        builder.generalAssistance(true)
        builder.parentsAssistance(true)
        builder.depNumberHousehold(1)
        builder.indNumberHousehold(1)
        builder.depGrossIncome(1)
        builder.indGrossIncome(1)
        builder.depOtherIncome(1)
        builder.indOtherIncome(1)
        builder.depTotalIncome(1)
        builder.indTotalIncome(1)

        BOGWaiver bogWaiver = builder.build()

        when:
        result = mockMvc.perform(post('/bogfw?mis=001')
                .contentType(APPLICATION_JSON)
                .content(new JsonBuilder(bogWaiver).toString()))

        then:
        result.andExpect(status().isCreated())
    }

}