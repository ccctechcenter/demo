package com.ccctc.adaptor.model;

import io.swagger.annotations.ApiModel;
import io.swagger.annotations.ApiModelProperty;

import java.io.Serializable;

/**
 * Created by jrscanlon on 12/9/15.
 */
@ApiModel
public class Instructor implements Serializable {

    // Colleague - person.first_name
    @ApiModelProperty(value = "First name of the instructor.", example="Marge")
    private String firstName;

    // Colleague - person.last_name
    @ApiModelProperty(value = "Last name of the instructor.", example="Simpson")
    private String lastName;

    // Colleague - people_email.person_email_addresses
    @ApiModelProperty(value = "Email address of the instructor.", example="msimpson@school.edu")
    private String emailAddress;

    // Used by Canvas LMS to build enrollments for teachers of sections.
    @ApiModelProperty(value = "SIS identifier of the instructor.", example="S1234556")
    private String sisPersonId;
    
    private Instructor(Builder builder) {
        setFirstName(builder.firstName);
        setLastName(builder.lastName);
        setEmailAddress(builder.emailAddress);
        setSisPersonId(builder.sisPersonId);
    }
    /**
     * Needed for ObjectMapper to unmarshal a JSON string
     */
    public Instructor(){}

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

    public String getEmailAddress() {
        return emailAddress;
    }

    public void setEmailAddress(String emailAddress) {
        this.emailAddress = emailAddress;
    }

    public String getSisPersonId() { return sisPersonId; }

    public void setSisPersonId( String sisPersonId) {
        this.sisPersonId = sisPersonId;
    }


    public static final class Builder {
        private String firstName;
        private String lastName;
        private String emailAddress;
        private String sisPersonId;

        public Builder() {
        }

        public Builder firstName(String val) {
            firstName = val;
            return this;
        }

        public Builder lastName(String val) {
            lastName = val;
            return this;
        }

        public Builder emailAddress(String val) {
            emailAddress = val;
            return this;
        }

        public Builder sisPersonId(String val) {
            sisPersonId = val;
            return this;
        }

        public Instructor build() {
            return new Instructor(this);
        }
    }

    @Override
    public String toString() {
        return "Instructor{" +
                "firstName='" + firstName + '\'' +
                ", lastName='" + lastName + '\'' +
                ", emailAddress='" + emailAddress + '\'' +
                ", sisPersonId='" + sisPersonId + '\'' +
                '}';
    }
}
