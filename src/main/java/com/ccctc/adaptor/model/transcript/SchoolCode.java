package com.ccctc.adaptor.model.transcript;

import io.swagger.annotations.ApiModel;
import io.swagger.annotations.ApiModelProperty;

/**
 * Created by james on 2/16/17.
 */
@ApiModel(value = "A code and code type that uniquely identifies an institution.")
public class SchoolCode {
    @ApiModelProperty(value = "The type of school code provided.", example="FICE")
    private SchoolCodeTypes codeType;

    @ApiModelProperty(value = "The school code.", example="001234")
    private String code;

    public SchoolCode() {

    }

    public SchoolCode(String code, SchoolCodeTypes codeType) {
        this.code = code;
        this.codeType = codeType;
    }


    public SchoolCodeTypes getCodeType() {
        return codeType;
    }

    public void setCodeType(SchoolCodeTypes codeType) {
        this.codeType = codeType;
    }

    public String getCode() {
        return code;
    }

    public void setCode(String code) {
        this.code = code;
    }
}
