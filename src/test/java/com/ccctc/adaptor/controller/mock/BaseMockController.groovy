package com.ccctc.adaptor.controller.mock

import groovy.json.JsonBuilder
import org.springframework.test.web.servlet.MockMvc
import org.springframework.test.web.servlet.ResultActions
import org.springframework.test.web.servlet.setup.MockMvcBuilders
import spock.lang.Specification

import static org.springframework.http.MediaType.APPLICATION_JSON
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.put
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.patch
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.delete

/**
 * Created by zekeo on 7/21/2017.
 */
abstract class BaseMockController extends Specification {

    MockMvc mockMvc

    def setMockMvc(controller) {
        mockMvc = MockMvcBuilders.standaloneSetup(controller).build()
    }

    ResultActions doGet(String url) {
        return mockMvc.perform(get(url))
    }

    ResultActions doPost(String url, Object body) {
        return mockMvc.perform(post(url)
                .contentType(APPLICATION_JSON)
                .content(new JsonBuilder(body).toString()))

    }

    ResultActions doPut(String url, Object body) {
        return mockMvc.perform(put(url)
                .contentType(APPLICATION_JSON)
                .content(new JsonBuilder(body).toString()))

    }

    ResultActions doPatch(String url, Object body) {
        return mockMvc.perform(patch(url)
                .contentType(APPLICATION_JSON)
                .content(new JsonBuilder(body).toString()))

    }

    ResultActions doDelete(String url) {
        return mockMvc.perform(delete(url))
    }
}
