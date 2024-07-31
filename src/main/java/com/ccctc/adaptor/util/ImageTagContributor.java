package com.ccctc.adaptor.util;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.actuate.info.Info;
import org.springframework.boot.actuate.info.InfoContributor;
import org.springframework.stereotype.Component;

@Component
public class ImageTagContributor implements InfoContributor {

    private String imageTag;

    @Autowired
    public ImageTagContributor(@Value("${IMAGE_TAG:unknown}") String imageTag) {
        this.imageTag = imageTag;
    }

    /**
     * Contribute the image tag to the /info endpoint
     *
     * @param builder Builder
     */
    public void contribute(Info.Builder builder) {
        builder.withDetail("image", imageTag);
    }
}
