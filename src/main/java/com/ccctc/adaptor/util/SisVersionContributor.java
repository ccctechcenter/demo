package com.ccctc.adaptor.util;


import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.actuate.health.Health;
import org.springframework.boot.actuate.health.HealthIndicator;
import org.springframework.boot.actuate.health.Status;
import org.springframework.boot.actuate.info.Info;
import org.springframework.boot.actuate.info.InfoContributor;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.stereotype.Component;

/**
 * Created by rcshishe on 1/25/17.
 */
@Component
@ConditionalOnProperty(value= "health.sis.enabled", matchIfMissing = true)
public class SisVersionContributor implements InfoContributor, HealthIndicator {

    private final static Logger log = LoggerFactory.getLogger(SisVersionContributor.class);

    private SisVersionHealthService sisVersionHealthService;

    @Autowired
    public SisVersionContributor(SisVersionHealthService sisVersionHealthService) {
        this.sisVersionHealthService = sisVersionHealthService;
    }

    /**
     * Contribute SIS version information to the /info endpoint
     *
     * @param builder Builder
     */
    public void contribute(Info.Builder builder) {
        try {
            builder.withDetails(sisVersionHealthService.getSisVersion());
        } catch (Exception e) {
            log.error("Error retrieving version details from SIS. {} ", e.getMessage(), e);
        }
    }

    /**
     * Return health information for the /health endpoint
     *
     * @return health
     */
    public Health health() {
        try {
            Status health = sisVersionHealthService.getSisConnectionStatus();

            if (health == Status.UP)
                return new Health.Builder().up().build();
            else
                return new Health.Builder().down().build();
        } catch (Exception ex) {
            return new Health.Builder().down(ex).build();
        }
    }
}