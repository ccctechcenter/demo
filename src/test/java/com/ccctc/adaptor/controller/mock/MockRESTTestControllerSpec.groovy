package com.ccctc.adaptor.controller.mock

import org.springframework.web.util.UriUtils

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get

class MockRESTTestControllerSpec extends BaseMockController {

    MockRESTTestController controller

    void setup() {
        controller = new MockRESTTestController()
        setMockMvc(controller)
    }

    def "get - 201 no body"() {
        when:
        def result = doGet("/mock/resttest?statusCode=201").andReturn()

        then:
        result.response.status == 201
    }

    def "get - 200 text body"() {
        setup:
        def body = "some-text"

        when:
        def result = doGet("/mock/resttest?statusCode=200&body=$body").andReturn()

        then:
        result.response.status == 200
        result.response.contentAsString == body
        result.response.contentType == "text/plain"
    }

    def "get - 400 json body"() {
        setup:
        def body = "{\"key\":\"value\"}"
        def uri = new URI("/mock/resttest?statusCode=400&body=" + UriUtils.encode(body, "UTF-8"))

        when:
        def result = mockMvc.perform(get(uri)).andReturn()

        then:
        result.response.status == 400
        result.response.contentAsString == body
        result.response.contentType == "application/json"
    }
}
