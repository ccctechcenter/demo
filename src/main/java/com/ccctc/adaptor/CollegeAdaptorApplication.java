/**
* Copyright (c) 2019 California Community Colleges Technology Center
* Licensed under the MIT license.
* A copy of this license may be found at https://opensource.org/licenses/mit-license.php
**/

package com.ccctc.adaptor;

import com.ccctc.adaptor.util.CoverageIgnore;
import com.ulisesbocchio.jasyptspringboot.annotation.EnableEncryptableProperties;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.context.annotation.Configuration;

@SpringBootApplication
@Configuration
@EnableEncryptableProperties
public class CollegeAdaptorApplication {

    static {
        javax.net.ssl.HttpsURLConnection.setDefaultHostnameVerifier(
                new javax.net.ssl.HostnameVerifier(){
                    @CoverageIgnore
                    public boolean verify(String hostname,
                                          javax.net.ssl.SSLSession sslSession) {
                        return true;
                    }
                });
    }

    public static void main(String[] args) {
        // Setting cloud config credentials at startup due to properties
        // from environment not being resolved prior to fetch from server
        System.setProperty("spring.cloud.config.username", System.getenv("miscode"));
        System.setProperty("spring.cloud.config.password", System.getenv("conductor_authClientSecret"));
        SpringApplication.run(CollegeAdaptorApplication.class, args);
    }
}
