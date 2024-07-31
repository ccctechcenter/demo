package com.ccctc.adaptor.config;

import com.github.benmanes.caffeine.cache.Caffeine;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.cache.CacheManager;
import org.springframework.cache.annotation.EnableCaching;
import org.springframework.cache.caffeine.CaffeineCache;
import org.springframework.cache.support.SimpleCacheManager;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import java.util.Arrays;
import java.util.concurrent.TimeUnit;

@Configuration
@EnableCaching
public class CacheConfig {

    private static final Logger logger = LoggerFactory.getLogger(CacheConfig.class);

    public final static String ADAPTOR_VERSION_CACHE = "ADAPTOR_VERSION";
    public final static String ADAPTOR_SIS_HEALTH_CACHE = "SIS_HEALTH";
    public final static String COLLEAGUE_CACHE = "COLLEAGUE_CACHE";

    @Bean
    public CacheManager cacheManager() {
        logger.info("Initializing Caffeine Cache Manager");
        SimpleCacheManager cacheManager = new SimpleCacheManager();

        // Cache for SIS Version - 15 minutes
        CaffeineCache versionCache = new CaffeineCache(ADAPTOR_VERSION_CACHE, Caffeine.newBuilder()
                        .expireAfterWrite(15, TimeUnit.MINUTES)
                        .build());

        // Cache for SIS Health - 15 minutes
        CaffeineCache healthCache =
                new CaffeineCache(ADAPTOR_SIS_HEALTH_CACHE, Caffeine.newBuilder()
                        .expireAfterWrite(15, TimeUnit.MINUTES)
                        .build());

        // Cache for Colleague data - 15 minutes
        CaffeineCache colleagueCache =
                new CaffeineCache(COLLEAGUE_CACHE, Caffeine.newBuilder()
                        .expireAfterWrite(15, TimeUnit.MINUTES)
                        .build());

        cacheManager.setCaches(Arrays.asList(versionCache, healthCache, colleagueCache));
        return cacheManager;
    }
}
