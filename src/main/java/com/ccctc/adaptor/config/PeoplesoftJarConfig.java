package com.ccctc.adaptor.config;

import com.ccctc.adaptor.CollegeAdaptorApplication;
import org.apache.commons.lang.StringUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Conditional;
import org.springframework.context.annotation.Configuration;

import java.io.IOException;
import java.io.InputStream;
import java.net.MalformedURLException;
import java.net.URL;
import java.nio.file.Files;
import java.nio.file.Paths;
import java.nio.file.StandardCopyOption;

@Configuration
@Conditional(PeoplesoftCondition.class)
public class PeoplesoftJarConfig {

    protected final static Logger log = LoggerFactory.getLogger(PeoplesoftJarConfig.class);

    @Bean
    public PeoplesoftJarConfig refresh(@Value("${misCode:}") String misCode,
                                       @Value("${peoplesoft.jars.psjoa.url:}") String psjoa_url,
                                       @Value("${peoplesoft.jars.psci.url:}") String psci_url) {
        if(StringUtils.isEmpty(misCode)) {
            log.error("WARNING: Peoplesoft not fully Configured because the misCode could not be detected.");
            return this;
        }

        log.info("Initializing Peoplesoft Configuration for misCode: " + misCode);
        if(!StringUtils.isEmpty(psjoa_url)) {
            try {
                log.info("Initializing psjoa jar file with URL: " + psjoa_url);
                InputStream in = new URL(psjoa_url).openStream();
                Files.copy(in, Paths.get("./jars/psjoa_" + misCode + ".jar"), StandardCopyOption.REPLACE_EXISTING);
            }
            catch (MalformedURLException e) {
                log.error("Could not Parse the given URL for the psjoa jar file: " + e.getMessage());
            } catch (IOException e) {
                log.error("Could not save the psjoa jar file from the given url: " + e.getMessage());
            }
        } else {
            log.warn("WARNING: Peoplesoft psjoa is not fully configured!");
        }

        if(!StringUtils.isEmpty(psci_url)) {
            try {
                log.info("Initializing psci jar file with URL: " + psci_url);
                InputStream in = new URL(psci_url).openStream();
                Files.copy(in, Paths.get("./jars/psci_" + misCode + ".jar"), StandardCopyOption.REPLACE_EXISTING);
            }
            catch (MalformedURLException e) {
                log.error("Could not Parse the given URL for the psci jar file: " + e.getMessage());
            } catch (IOException e) {
                log.error("Could not save the psci jar file from the given url: " + e.getMessage());
            }
        } else {
            log.warn("WARNING: Peoplesoft psci is not fully configured!");
        }

        return this;
    }
}
