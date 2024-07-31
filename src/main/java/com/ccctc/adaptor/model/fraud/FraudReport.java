package com.ccctc.adaptor.model.fraud;

import com.ccctc.adaptor.model.PrimaryKey;
import io.swagger.annotations.ApiModelProperty;
import lombok.Getter;
import lombok.Setter;

import java.io.Serializable;
import java.time.LocalDateTime;

@Getter
@Setter
@PrimaryKey({"misCode", "sisFraudReportId"})
public class FraudReport implements Serializable {

    //sisFraudReport_id bigint NOT NULL
    @ApiModelProperty(value = "Internal Surrogate ID of the fraud report", example = "17762022")
    private Long sisFraudReportId;

    //tstmp_submit timestamp with time zone
    @ApiModelProperty(required = true, value = "Timestamp the Fraud was first reported", example = "2020-05-08T18:30:05.817")
    private LocalDateTime tstmpSubmit;

    //ccc_id character varying(7)
    @ApiModelProperty(value = "CCC ID of the fraudulent student", example = "ABC1234")
    private String cccId;

    //reported_by_mis_code character(3)
    @ApiModelProperty(required = true, value = "3 digit college MIS code that reported the Fraud", example = "111")
    private String reportedByMisCode;

    // misCode of college that this report is being shared with/to
    @ApiModelProperty(required = true, value = "3 digit college MIS code", example = "111")
    private String misCode;

    //fraud_type character varying(50)
    @ApiModelProperty(value = "Type of Reported Fraud: Application, Enrollment, or Financial", allowableValues = "Application, Enrollment, Financial")
    private String fraudType;

    //app_id bigint
    @ApiModelProperty(value = "CCCApply Application ID of the fraudulent app", example = "12343")
    private Long appId;

    @ApiModelProperty(value = "Processing Status by SIS" )
    private String sisProcessedFlag;

    @ApiModelProperty(value = "Date/Time Processed by SIS" )
    private LocalDateTime tstmpSISProcessed;

    @ApiModelProperty(value = "SIS Notes when processing" )
    private String sisProcessedNotes;

    @ApiModelProperty(value = "Type of Reported Source: college, apply-data-stream or testing")
    private String reportedBySource;
}
