package com.ccctc.adaptor.util

import com.ccctc.adaptor.config.CacheConfig
import com.ccctc.adaptor.util.impl.GroovyServiceImpl
import org.springframework.cache.CacheManager
import org.springframework.cache.caffeine.CaffeineCache
import org.springframework.test.context.ContextConfiguration
import org.springframework.test.util.ReflectionTestUtils
import spock.lang.Specification

import javax.annotation.Resource

@ContextConfiguration(classes = [SisVersionHealthService.class, CacheConfig.class, GroovyServiceImpl.class])
class SisVersionHealthServiceSpec extends Specification {

    def groovyService = Mock(GroovyService)

    @Resource
    SisVersionHealthService sisVersionHealthService

    @Resource
    CacheManager cacheManager

    def setup() {
        // replace the groovy util with a mock. this is necessary as we needed the SisVersionContributor bean to take
        // advantage of and test caching, so we couldn't construct the objects ourselves
        ReflectionTestUtils.setField(sisVersionHealthService, "groovyService", groovyService)

        // clear the cache
        def cache1 = (CaffeineCache) cacheManager.getCache(CacheConfig.ADAPTOR_SIS_HEALTH_CACHE)
        def cache2 = (CaffeineCache) cacheManager.getCache(CacheConfig.ADAPTOR_VERSION_CACHE)

        cache1.clear()
        cache2.clear()
    }

    def "getSisVersion"() {
        when:
        sisVersionHealthService.getSisVersion()

        then:
        1 * groovyService.run(*_)

        when:
        sisVersionHealthService.getSisVersion()

        then:
        0 * _
    }

    def "getSisConnectionStatus"() {
        when:
        sisVersionHealthService.getSisConnectionStatus()

        then:
        1 * groovyService.run(*_)

        when:
        sisVersionHealthService.getSisConnectionStatus()

        then:
        0 * _
    }
}
