package com.ccctc.adaptor.util.impl

import com.ccctc.adaptor.exception.EntityNotFoundException
import com.ccctc.adaptor.exception.InternalServerException
import com.ccctc.adaptor.exception.InvalidRequestException
import com.ccctc.adaptor.model.*
import org.apache.commons.lang3.reflect.FieldUtils
import org.ccctc.colleaguedmiclient.service.DmiCTXService
import org.ccctc.colleaguedmiclient.service.DmiDataService
import org.ccctc.colleaguedmiclient.service.DmiEntityService
import org.ccctc.colleaguedmiclient.service.DmiService
import org.springframework.cache.CacheManager
import org.springframework.core.env.Environment
import spock.lang.Specification

/**
 * Created by jrscanlon on 12/14/15.
 */
class GroovyServiceImplSpec extends Specification {

    def environment = Mock(Environment)
    def groovyService = new GroovyServiceImpl(environment: environment, sisType: "mock")

    def "no sistype"() {
        when:
        new GroovyServiceImpl().run("000", "test", "test", null)

        then:
        thrown InternalServerException
    }

    def "nofile"() {
        def result = null
        when:
        try {
            groovyService.run("000", "invalid", "run", null)
        } catch (InternalServerException e) {
            result = e
        }
        then:
        result instanceof InternalServerException
    }

    def "doit" () {
        def String[] searchPath = [ "./src/test/java/com/ccctc/adaptor/" ]
        when:
        def result = groovyService.run("000", searchPath, "Doit", "run", null)
        then:
        result == "Stinky"
    }

    def "Bad Groovy - missing method"() {
        def String[] searchPath = [ "./src/test/java/com/ccctc/adaptor/" ]
        def result
        when:
        try {
            result = groovyService.run(searchPath, "BadGroovy", "get", null)
        } catch (Exception e) {
            result = e
        }
        then:
        result instanceof MissingMethodException
    }

    def "Bad Groovy - wrong type"() {
        String[] searchPath = [ "./src/test/java/com/ccctc/adaptor/" ]
        Exception err = null
        when:
        try {
            Section result = groovyService.run("000", searchPath, "BadGroovy", "wrongType", null)
        } catch (Exception e) {
            err = e
        }
        then:
        err instanceof ClassCastException
    }

    def "Argument Check"() {
        def String[] searchPath = [ "./src/test/java/com/ccctc/adaptor/" ]
        when:
        def result = groovyService.run("000", searchPath, "Doit", "returnSomething", "something")
        then:
        result == "something"
    }

    def "Cache Check"() {
        setup:
        String[] searchPath = [ "./src/test/java/com/ccctc/adaptor/" ]

        // run a script, verify it made it into the cache
        when:
        groovyService.clearGroovyClassCache()
        def result = groovyService.run("000", searchPath, "Doit", "returnSomething", "something")
        def map = FieldUtils.readField(groovyService, "cachedClassMap", true)
        def loader = FieldUtils.readField(groovyService, "loader", true)

        then:
        result == "something"
        assert map instanceof Map<String, GroovyServiceImpl.CachedGroovyClass>
        assert loader instanceof GroovyClassLoader
        map.containsKey("./src/test/java/com/ccctc/adaptor/Doit.groovy")
        loader.getLoadedClasses().size() == 1

        // simulate a changed last modified date in the cache, verify that the cache was updated
        when:
        def oldEntry = map["./src/test/java/com/ccctc/adaptor/Doit.groovy"]
        def oldClass = loader.getLoadedClasses()[0]
        map["./src/test/java/com/ccctc/adaptor/Doit.groovy"] = new GroovyServiceImpl.CachedGroovyClass(oldEntry.name, oldEntry.lastModified + 1000)
        //new File("./src/test/java/com/ccctc/adaptor/Doit.groovy").setLastModified( System.currentTimeMillis())
        result = groovyService.run("000", searchPath, "Doit", "returnSomething", "something")
        map = FieldUtils.readField(groovyService, "cachedClassMap", true)
        loader = FieldUtils.readField(groovyService, "loader", true)

        then:
        result == "something"
        assert map instanceof Map<String, GroovyServiceImpl.CachedGroovyClass>
        assert loader instanceof GroovyClassLoader
        map["./src/test/java/com/ccctc/adaptor/Doit.groovy"] != oldEntry
        loader.getLoadedClasses()[0] != oldClass

        when:
        result = groovyService.run("000", searchPath, "DoitAgain", "returnSomething", "something")
        result = groovyService.run("000", searchPath, "DoitAgain", "returnSomething", "something")
        map = FieldUtils.readField(groovyService, "cachedClassMap", true)
        loader = FieldUtils.readField(groovyService, "loader", true)

        then:
        result == "something"
        assert map instanceof Map<String, GroovyServiceImpl.CachedGroovyClass>
        assert loader instanceof GroovyClassLoader
        map.size() == 2
        loader.getLoadedClasses().size() == 2

        when:
        groovyService.clearGroovyClassCache()
        map = FieldUtils.readField(groovyService, "cachedClassMap", true)

        then:
        assert map instanceof Map
        map.size() == 0
    }

    def "run - bad mis code"() {
        when:
        InvalidRequestException e = null
        try {
            groovyService.run("this is a bad mis code", "o", "m", null)
        } catch (InvalidRequestException ex) {
            e = ex
        }

        then:
        e != null
        e.code == InvalidRequestException.Errors.invalidRequest

    }

    def "run - null mis code"() {
        when:
        InternalServerException e = null
        try {
            groovyService.run((String) null, "not", "found", null)
        } catch (InternalServerException ex) {
            // still throws exception because the groovy script is not found
            e = ex
        }

        then:
        e != null
        e.code == InternalServerException.Errors.internalServerError
    }

    def "run - good mis code"() {
        // good mis code
        when:
        InternalServerException e = null
        try {
            groovyService.run("123", "not", "found", null)
        } catch (InternalServerException ex) {
            // still throws exception because the groovy script is not found
            e = ex
        }

        then:
        e != null
        e.code == InternalServerException.Errors.internalServerError
    }

    def "colleague test"() {
        setup:
        def mockDmiService = Mock(DmiService)
        def mockDmiDataService = Mock(DmiDataService)
        def mockCacheManager = Mock(CacheManager)
        def mockDmiCTXService = Mock(DmiCTXService)
        def mockDmiEntityService = Mock(DmiEntityService)
        def groovyServiceColleague = new GroovyServiceImpl(environment: environment, sisType: "colleague",
                dmiDataService: mockDmiDataService, dmiService: mockDmiService, cacheManager: mockCacheManager,
                dmiCTXService: mockDmiCTXService, dmiEntityService: mockDmiEntityService)

        groovyServiceColleague.init()

        when:
        groovyServiceColleague.run("000", "Term", "get", ["000", "TERM"])

        then:
        // not found response
        1 * mockDmiEntityService.readForEntity(*_) >> []
        thrown EntityNotFoundException
    }
}
