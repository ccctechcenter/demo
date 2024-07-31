package com.ccctc.adaptor.controller;

import com.ccctc.adaptor.model.SisType;
import io.swagger.annotations.Api;
import io.swagger.annotations.ApiOperation;
import io.swagger.annotations.ApiParam;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.core.env.Environment;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

/**
 * Created by Rasul on 8/4/16.
 */
@Api(
        value="SIS Type",
        tags="SIS Type",
        description="Operation to return the SIS type of this adaptor",
        produces="application/json",
        position=1)
@RestController
@RequestMapping("/sistype")
public class SisTypeApiController {

    @Autowired
    private Environment environment;

    @ApiOperation(
            value = "Retrieve the SIS type the adaptor is configured for this mis",
            response = SisType.class
    )
    @GetMapping
    public SisType get(@ApiParam(value = "MIS Code of the College", example = "001") @RequestParam(value = "mis", required = false) String mis) {
        return new SisType.Builder().misCode(mis).sisType(environment.getProperty("sisType")).build();
    }
}