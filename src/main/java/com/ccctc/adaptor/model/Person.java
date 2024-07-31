package com.ccctc.adaptor.model;

import com.ccctc.adaptor.util.CoverageIgnore;
import io.swagger.annotations.ApiModel;
import io.swagger.annotations.ApiModelProperty;

import java.io.Serializable;
import java.util.List;

/**
 * Created by Rasul on 5/20/16.
 */
@ApiModel
@PrimaryKey({"misCode", "sisPersonId"})
public class Person implements Serializable {

    @ApiModelProperty(required = true, value = "3 digit college MIS code", example = "111")
    private String misCode;
    @ApiModelProperty(required = true, value = "ID of the person in the SIS", example = "person1")
    private String sisPersonId;
    @ApiModelProperty(value = "First name of the person", example="Marge")
    private String firstName;
    @ApiModelProperty(value = "Last name of the person", example="Simpson")
    private String lastName;
    @ApiModelProperty(value = "List of email addresses of the person", example="msimpson@school.edu")
    private List<Email> emailAddresses;
    @ApiModelProperty(value = "The login id of the person. This is the first part of the EPPN.", example="msimpson or 001324892")
    private String loginId;
    @ApiModelProperty(value = "The login suffix of the person. This is the suffix or scope of the EPPN", example="student.college.edu")
    private String loginSuffix;
    @ApiModelProperty(value = "CCC ID of the person", example = "ABC123")
    private String cccid;

    public Person() {
    }

    private Person(Builder builder) {
        setMisCode(builder.misCode);
        setSisPersonId(builder.sisPersonId);
        setFirstName(builder.firstName);
        setLastName(builder.lastName);
        setEmailAddresses(builder.emailAddresses);
        setLoginId(builder.loginId);
        setLoginSuffix(builder.loginSuffix);
        setCccid(builder.cccid);
    }

    public String getMisCode() {
        return misCode;
    }

    public void setMisCode(String misCode) {
        this.misCode = misCode;
    }

    public String getSisPersonId() {
        return sisPersonId;
    }

    public void setSisPersonId(String sisPersonId) {
        this.sisPersonId = sisPersonId;
    }

    public String getFirstName() {
        return firstName;
    }

    public void setFirstName(String firstName) {
        this.firstName = firstName;
    }

    public String getLastName() {
        return lastName;
    }

    public void setLastName(String lastName) {
        this.lastName = lastName;
    }

    public List<Email> getEmailAddresses() {
        return emailAddresses;
    }

    public void setEmailAddresses(List<Email> emailAddresses) {
        this.emailAddresses = emailAddresses;
    }

    public String getLoginId() {
        return loginId;
    }

    public void setLoginId(String loginId) {
        this.loginId = loginId;
    }

    public String getLoginSuffix() {
        return loginSuffix;
    }

    public void setLoginSuffix(String loginSuffix) {
        this.loginSuffix = loginSuffix;
    }

    public String getCccid() { return cccid; }

    public void setCccid( String cccid) { this.cccid = cccid; }

    public static final class Builder {
        private String misCode;
        private String sisPersonId;
        private String firstName;
        private String lastName;
        private List<Email> emailAddresses;
        private String loginId;
        private String loginSuffix;
        private String cccid;

        public Builder() {
        }

        public Builder misCode(String misCode) {
            this.misCode = misCode;
            return this;
        }

        public Builder sisPersonId(String sisPersonId) {
            this.sisPersonId = sisPersonId;
            return this;
        }

        public Builder firstName(String firstName) {
            this.firstName = firstName;
            return this;
        }

        public Builder lastName(String lastName) {
            this.lastName = lastName;
            return this;
        }

        public Builder emailAddresses(List<Email> emailAddresses) {
            this.emailAddresses = emailAddresses;
            return this;
        }

        public Builder loginId(String loginId) {
            this.loginId = loginId;
            return this;
        }

        public Builder loginSuffix(String loginSuffix) {
            this.loginSuffix = loginSuffix;
            return this;
        }

        public Builder cccid(String cccid) {
            this.cccid = cccid;
            return this;
        }

        public Person build() {
            return new Person(this);
        }
    }

    @CoverageIgnore
    @Override
    public String toString() {
        return "Person{" +
                "misCode='" + misCode + '\'' +
                ", sisPersonId='" + sisPersonId + '\'' +
                ", firstName='" + firstName + '\'' +
                ", lastName='" + lastName + '\'' +
                ", emailAddresses=" + emailAddresses +
                ", loginId='" + loginId + '\'' +
                ", loginSuffix='" + loginSuffix + '\'' +
                ", cccid='" + cccid + '\'' +
                '}';
    }
}
