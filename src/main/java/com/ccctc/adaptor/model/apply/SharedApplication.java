package com.ccctc.adaptor.model.apply;

import com.ccctc.adaptor.model.PrimaryKey;
import com.ccctc.adaptor.model.apply.Application;
import lombok.Getter;
import lombok.Setter;

import java.io.Serializable;

@Getter
@Setter
@PrimaryKey({"misCode", "appId"})
public class SharedApplication extends Application implements Serializable {

    // Teaching college misCode
    private String misCode;

}
