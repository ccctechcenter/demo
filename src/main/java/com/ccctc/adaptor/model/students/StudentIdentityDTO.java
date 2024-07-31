package com.ccctc.adaptor.model.students;

import com.ccctc.adaptor.model.*;
import io.swagger.annotations.ApiModel;

import java.io.Serializable;


@ApiModel
@PrimaryKey({"sisTermId", "sisPersonId"})
public class StudentIdentityDTO implements Serializable {

    private String cccid;
    private String sisPersonId;
    private String sisTermId;

    /**
     * Needed for ObjectMapper to unmarshal a JSON string
     */
    public StudentIdentityDTO() {

    }

    public String getCccid() {
        return cccid;
    }

    public void setCccid(String cccid) {
        this.cccid = cccid;
    }

    public String getSisPersonId() {
        return sisPersonId;
    }

    public void setSisPersonId(String sisPersonId) {
        this.sisPersonId = sisPersonId;
    }

    public String getSisTermId() {
        return sisTermId;
    }

    public void setSisTermId(String sisTermId) {
        this.sisTermId = sisTermId;
    }
}
