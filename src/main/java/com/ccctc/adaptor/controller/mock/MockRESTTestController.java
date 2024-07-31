package com.ccctc.adaptor.controller.mock;

import com.ccctc.adaptor.config.MockCondition;
import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import io.swagger.annotations.Api;
import io.swagger.annotations.ApiOperation;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.context.annotation.Conditional;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.io.IOException;
import java.util.HashMap;

@Api(
        value = "Mock REST Tests",
        tags = "Mock REST Tests",
        description = "Operations to simulate REST responses",
        produces = "application/json",
        position = 1)
@RestController
@RequestMapping("/mock/resttest")
@Conditional(MockCondition.class)
public class MockRESTTestController {

    private final static Logger logger = LoggerFactory.getLogger(MockRESTTestController.class);
    private final static ObjectMapper mapper = new ObjectMapper();

    // ~~~~~~~~~~~~~~~~~~~~~~~~~ GET ~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~
    @ApiOperation(
            value = "Mock the result of a GET by returning the passed in status code and optional body"
    )
    @GetMapping
    public ResponseEntity get(@RequestParam(value = "statusCode") int statusCode,
                              @RequestParam(value = "body", required = false) String body) {
        logger.debug("Mock REST : get {} {}", statusCode, body);

        ResponseEntity.BodyBuilder r = ResponseEntity
                .status(statusCode);

        // auto-determine content type - either application/json or text/plain
        if (body != null) {
            MediaType mediaType = null;

            try {
                Object a = mapper.readValue(body, new TypeReference<HashMap<String, Object>>() {});
                if (a != null) mediaType = MediaType.APPLICATION_JSON;
            } catch (IOException ignored) { }

            return r.contentType(mediaType == null ? MediaType.TEXT_PLAIN : mediaType).body(body);
        }

        return r.build();
    }
}