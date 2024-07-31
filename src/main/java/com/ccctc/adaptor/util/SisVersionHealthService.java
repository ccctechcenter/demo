package com.ccctc.adaptor.util;

import com.ccctc.adaptor.config.CacheConfig;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.actuate.health.Status;
import org.springframework.cache.annotation.Cacheable;
import org.springframework.stereotype.Service;

import java.util.Map;

@Service
public class SisVersionHealthService {

    private GroovyService groovyService;

    @Autowired
    public SisVersionHealthService(GroovyService groovyService) {
        this.groovyService = groovyService;
    }

    /**
     * Get the SIS Version information. Cache the results.
     *
     * @return SIS version information
     */
    @Cacheable(CacheConfig.ADAPTOR_VERSION_CACHE)
    public Map<String, Object> getSisVersion() {
        return groovyService.run((String) null, "Utility", "getSisVersion", null);
    }

    /**
     * Get the status from the SIS. Cache the results.
     *
     * @return SIS status
     */
    @Cacheable(CacheConfig.ADAPTOR_SIS_HEALTH_CACHE)
    public Status getSisConnectionStatus() {
        return groovyService.run((String) null, "Utility", "getSisConnectionStatus", null);
    }
}
