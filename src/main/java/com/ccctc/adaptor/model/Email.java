package com.ccctc.adaptor.model;

import com.ccctc.adaptor.util.CoverageIgnore;
import io.swagger.annotations.ApiModel;
import io.swagger.annotations.ApiModelProperty;

import java.io.Serializable;

/**
 * Created by Rasul on 5/20/16.
 */
@ApiModel
public class Email implements Serializable {

    @ApiModelProperty(value = "Email address type of the person.", example="Institution,Personal")
    private EmailType type;
    @ApiModelProperty(value = "Email address of the person.", example="msimpson@school.edu")
    private String emailAddress;

    private Email() {
    }

    private Email(Builder builder) {
        setType(builder.type);
        setEmailAddress(builder.emailAddress);
    }

    public EmailType getType() {
        return type;
    }

    public void setType(EmailType type) {
        this.type = type;
    }

    public String getEmailAddress() {
        return emailAddress;
    }

    public void setEmailAddress(String emailAddress) {
        this.emailAddress = emailAddress;
    }

    public static final class Builder {
        private EmailType type;
        private String emailAddress;

        public Builder() {
        }

        public Builder type(EmailType val) {
            type = val;
            return this;
        }

        public Builder emailAddress(String val) {
            emailAddress = val;
            return this;
        }

        public Email build() {
            return new Email(this);
        }
    }

    @CoverageIgnore
    @Override
    public String toString() {
        return "Email{" +
                "type=" + type +
                ", emailAddress='" + emailAddress + '\'' +
                '}';
    }
}
