package com.ccctc.adaptor.controller;

import com.ccctc.adaptor.model.ErrorResource;
import com.ccctc.adaptor.util.GroovyService;
import com.ccctc.adaptor.util.impl.TranscriptService;
import com.ccctc.message.collegetranscript.v1_6.CollegeTranscript;
import io.swagger.annotations.*;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.*;

import javax.annotation.Resource;
import javax.servlet.http.HttpServletResponse;
import javax.xml.bind.JAXBException;
import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.time.LocalDate;

/**
 * Created by James Whetstone on 10/24/16.
 */
@Api(
        value = "Transcripts",
        tags = "Transcripts",
        description = "Operations related to transcript data",
        produces = "application/json",
        position = 1)
@RestController
@RequestMapping("/transcripts")
public class TranscriptApiController {
    private final static Logger log = LoggerFactory.getLogger(TranscriptApiController.class);

    @Resource
    private GroovyService groovyService;

    @Resource
    private TranscriptService transcriptService;

    @ApiOperation(
            value = "Retrieve a college transcript in PESC format from college SIS. Note that either the student ID " +
                    "assigned by the record holder, or cccid or the student's name (first and last name) and birth date must be " +
                    "provided to use this API. The SSN, partialSSN and emailAddress are additional optional search parameters to first name, last name and birthDate.",
            response = CollegeTranscript.class,
            notes = "Possible error codes: multipleResultsFound, studentNotFound"
    )
    @ApiResponses({
            @ApiResponse(code = 400, response = ErrorResource.class, message = "multipleResultsFound"),
            @ApiResponse(code = 400, response = ErrorResource.class, message = "studentNotFound")
    })
    @RequestMapping(method = RequestMethod.GET)
    public void get(@RequestParam(value = "cccid", required = false) @ApiParam(value = "The student ID assigned by California Community Colleges (CCC). If this is provided, no additional student identifying information is required.") String cccID,
                    @RequestParam(value = "studentId", required = false) @ApiParam(value = "The student ID assigned by the record holding school. If this is provided, no additional student identifying information is required.") String schoolAssignedStudentID,
                    @RequestParam(value = "firstName", required = false) @ApiParam(value = "The student's first name.  Required if the student ID is not provided.") String firstName,
                    @RequestParam(value = "lastName", required = false) @ApiParam(value = "The student's last name.  Required if the student ID is not provided.") String lastName,
                    @RequestParam(value = "birthDate", required = false) @ApiParam(value = "The student's birth date (YYYY-MM-DD).  Required if the student ID is not provided.") @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate birthDate,
                    @RequestParam(value = "SSN", required = false) @ApiParam(value = "The student's social security number.", required = false) String SSN,
                    @RequestParam(value = "partialSSN", required = false) @ApiParam(value = "The last 4 digits of the student's social security number.", required = false) String partialSSN,
                    @RequestParam(value = "emailAddress", required = false) @ApiParam(value = "The student's email address.", required = false) String emailAddress,
                    @RequestParam(value = "mis", required = true) @ApiParam(value = "The management information system ID.", required = true) String misCode,
                    HttpServletResponse response) throws IOException, JAXBException {
        if (schoolAssignedStudentID == null && cccID == null) {
            if (firstName == null || lastName == null || birthDate == null) {
                throw new IllegalArgumentException("The student's first name, last name and birth date are required whenever the school assigned student ID or cccid are not provided.");
            }
        }

        CollegeTranscript collegeTranscript = groovyService.run(misCode, "Transcript", "get",
                new Object[]{misCode, firstName, lastName, birthDate != null ? birthDate.toString() : null, SSN, partialSSN, emailAddress, schoolAssignedStudentID, cccID, transcriptService});

        response.addHeader("Content-Type", "application/json");

        transcriptService.toJSONStream(collegeTranscript, response.getOutputStream());
    }

    @ApiOperation(
            value = "Post a transcript to the SIS",
            notes = "Possible error codes: multipleResultsFound, studentNotFound, invalidSchoolCode, sisQueryError, courseNotFound")
    @ApiResponses({
            @ApiResponse(code = 400, response = ErrorResource.class, message = "multipleResultsFound"),
            @ApiResponse(code = 400, response = ErrorResource.class, message = "studentNotFound"),
            @ApiResponse(code = 400, response = ErrorResource.class, message = "invalidSchoolCode"),
            @ApiResponse(code = 400, response = ErrorResource.class, message = "sisQueryError"),
            @ApiResponse(code = 400, response = ErrorResource.class, message = "courseNotFound")
    })
    @ResponseStatus(HttpStatus.NO_CONTENT)
    @PostMapping
    public void post(@ApiParam(value = "MIS Code of the College", example = "001", required = true) @RequestParam("mis") String misCode,
                     @ApiParam(value = "The transcript to be created", required = true) @RequestBody String transcriptText) {

        CollegeTranscript transcript;
        try {
            transcript = (CollegeTranscript) transcriptService.createTranscriptUnmarshaller(false, true).unmarshal(new ByteArrayInputStream(transcriptText.getBytes()));
        } catch (Exception e) {
            log.error("Failed to unmarshal JSON transcript.", e);
            throw new IllegalArgumentException("Failed to unmarshal transcript.", e);
        }
        groovyService.run(misCode, "Transcript", "post", new Object[]{misCode, transcript});
    }
}