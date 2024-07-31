package com.ccctc.adaptor.util.impl;

import com.ccctc.adaptor.config.CacheConfig;
import com.ccctc.adaptor.exception.InternalServerException;
import com.ccctc.adaptor.exception.InvalidRequestException;
import com.ccctc.adaptor.model.mock.*;
import com.ccctc.adaptor.util.ClassMap;
import com.ccctc.adaptor.util.GroovyService;
import com.ccctc.adaptor.util.LogUtils;
import groovy.lang.GroovyClassLoader;
import groovy.lang.GroovyObject;
import org.apache.commons.lang.StringEscapeUtils;
import org.ccctc.colleaguedmiclient.service.DmiCTXService;
import org.ccctc.colleaguedmiclient.service.DmiDataService;
import org.ccctc.colleaguedmiclient.service.DmiEntityService;
import org.ccctc.colleaguedmiclient.service.DmiService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.cache.Cache;
import org.springframework.cache.CacheManager;
import org.springframework.cloud.context.config.annotation.RefreshScope;
import org.springframework.core.env.Environment;
import org.springframework.stereotype.Service;

import javax.annotation.PostConstruct;
import java.io.File;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

/**
 * Service to run custom groovy scripts
 */
@Service
@RefreshScope
public class GroovyServiceImpl implements GroovyService {
    private final static Logger log = LoggerFactory.getLogger(GroovyServiceImpl.class);

    @Value("${adaptor.classpath}")
    private String adaptorClassPath;

    @Value("${sisType}")
    private String sisType;

    @Autowired
    private Environment environment;
    @Autowired
    private CacheManager cacheManager;
    @Autowired(required = false)
    private CourseDB courseDB;
    @Autowired(required = false)
    private EnrollmentDB enrollmentDB;
    @Autowired(required = false)
    private PersonDB personDB;
    @Autowired(required = false)
    private SectionDB sectionDB;
    @Autowired(required = false)
    private StudentDB studentDB;
    @Autowired(required = false)
    private TermDB termDB;
    @Autowired(required = false)
    private FinancialAidUnitsDB financialAidUnitsDB;
    @Autowired(required = false)
    private BOGWaiverDB bogWaiverDB;
    @Autowired(required = false)
    private StudentPrereqDB studentPrereqDB;
    @Autowired(required = false)
    private PlacementDB placementDB;
    @Autowired(required = false)
    private ApplyDB applyDB;
    @Autowired(required = false)
    private InternationalApplicationDB internationalApplicationDB;
    @Autowired(required = false)
    private CCPromiseGrantDB ccPromiseGrantDB;
    @Autowired(required = false)
    private StudentPlacementDB studentPlacementDB;
    @Autowired(required = false)
    private SharedApplicationDB sharedApplicationDB;
    @Autowired(required = false)
    private FraudReportDB fraudReportDB;

    // Services used by Colleague
    @Autowired(required = false)
    private DmiService dmiService;
    @Autowired(required = false)
    private DmiCTXService dmiCTXService;
    @Autowired(required = false)
    private DmiDataService dmiDataService;
    @Autowired(required = false)
    private DmiEntityService dmiEntityService;
    private Cache colleagueCache;
    private ClassMap colleagueServiceMap;

    private GroovyClassLoader loader = new GroovyClassLoader(GroovyServiceImpl.class.getClassLoader());
    private Map<String, CachedGroovyClass> cachedClassMap = new ConcurrentHashMap<>();

    @PostConstruct
    public void init() {
        if (adaptorClassPath != null) {
            String[] pathList = adaptorClassPath.split(",");
            for (int inx = 0; inx < pathList.length; inx++) {
                loader.addClasspath(pathList[inx]);
            }
        }

        colleagueCache = cacheManager.getCache(CacheConfig.COLLEAGUE_CACHE);

        // put colleague services in the Map
        colleagueServiceMap = new ClassMap();
        colleagueServiceMap.put(DmiService.class, dmiService);
        colleagueServiceMap.put(DmiCTXService.class, dmiCTXService);
        colleagueServiceMap.put(DmiDataService.class, dmiDataService);
        colleagueServiceMap.put(DmiEntityService.class, dmiEntityService);
        colleagueServiceMap.put(Cache.class, colleagueCache);

    }

    /**
     * Get a Groovy Class from the specified path. Caching is used for performance.
     *
     * @param path Full path to groovy file
     * @return Groovy class
     * @throws IOException IOException
     */
    private synchronized Class getGroovyClass(String path) throws IOException {
        Class[] classes = loader.getLoadedClasses();
        CachedGroovyClass cachedGroovyClass = cachedClassMap.get(path);
        File file = new File(path);
        Class objectClass = null;

        if (cachedGroovyClass != null) {
            if (file.lastModified() == cachedGroovyClass.getLastModified()) {
                for (int inx = 0; inx < classes.length; inx++) {
                    if (classes[inx].getName().equals(cachedGroovyClass.getName())) {
                        objectClass = classes[inx];
                        break;
                    }
                }
            } else {
                clearGroovyClassCache();
            }
        }

        if (objectClass == null) {
            objectClass = loader.parseClass(file);
            cachedClassMap.put(path, new CachedGroovyClass(objectClass.getName(), file.lastModified()));
        }

        return objectClass;
    }

    /**
     * Run a groovy script given an MIS Code
     *
     * @param misCode    MIS Code (optional)
     * @param objectName Name of groovy file (minus the .groovy)
     * @param methodName Method in groovy file to run
     * @param args       Arguments to pass to method
     * @param <T>        Return type
     * @return Result from groovy call
     */
    @Override
    public <T> T run(String misCode, String objectName, String methodName, Object[] args) {
        // validate misCode
        if (misCode != null && !misCode.matches("[0-9A-Z]{3}"))
            throw new InvalidRequestException(InvalidRequestException.Errors.invalidRequest, "Invalid MIS Code");

        if (sisType == null)
            throw new InternalServerException("Unable to call groovy code - sisType is missing");

        List<String> searchPath = new ArrayList<>();

        // new "groovy-scripts" path
        searchPath.add("./groovy-scripts/api/local/");
        if (misCode != null) searchPath.add("./groovy-scripts/api/mis_" + misCode + "/");
        searchPath.add("./groovy-scripts/api/" + sisType.toLowerCase() + "/");

        // old "api" path
        searchPath.add("./api/local/");
        if (misCode != null) searchPath.add("./api/mis_" + misCode + "/");
        searchPath.add("./api/" + sisType.toLowerCase() + "/");

        return run(misCode, searchPath.toArray(new String[searchPath.size()]), objectName, methodName, args);
    }

    /**
     * Run a groovy script given a list of paths to search
     *
     * @param misCode    MIS Code (optional)
     * @param searchPath List of paths to search for the groovy file
     * @param objectName Name of groovy file (minus the .groovy)
     * @param methodName Method in groovy file to run
     * @param args       Arguments to pass to method
     * @param <T>        Return type
     * @return Result from groovy call
     */
    @Override
    public <T> T run(String misCode, String[] searchPath, String objectName, String methodName, Object[] args) {
        boolean ranGroovy = false;
        T result = null;

        StringBuilder infoStringBuilder = new StringBuilder()
                .append(objectName)
                .append(".groovy")
                .append(" : ")
                .append(methodName)
                .append("( ");

        if (args != null) {
            for (int jnx = 0; jnx < args.length; jnx++) {
                if (jnx > 0) infoStringBuilder.append(", ");

                if (args[jnx] == null) {
                    infoStringBuilder.append("null");
                } else {
                    // escape the arguments and wrap in quotes
                    infoStringBuilder
                            .append("\"")
                            .append(StringEscapeUtils.escapeJava(args[jnx].toString()))
                            .append("\"");
                }
            }
        }

        infoStringBuilder.append(" )");

        // escape/cleanse the string so its suitable for logging
        String infoString = LogUtils.escapeString(infoStringBuilder.toString());

        for (int inx = 0; inx < searchPath.length; inx++) {
            StringBuilder path = new StringBuilder().append(searchPath[inx]).append(objectName).append(".groovy");
            try {
                Class objectClass = getGroovyClass(path.toString());
                GroovyObject object = (GroovyObject) objectClass.newInstance();
                log.info("Attempting Groovy API call - " + path.toString());

                if (sisType.equals("colleague")) {
                    Object[] cArgs = new Object[] { misCode, environment, colleagueServiceMap };

                    if (object.getMetaClass().getMetaMethod("colleagueInit", cArgs) != null) {
                        // new colleague script initialization uses a standard initialization method
                        object.invokeMethod("colleagueInit", cArgs);
                    }
                } else {
                    // Inject resources into new instance
                    if (object.getMetaClass().getMetaProperty("environment") != null) object.setProperty("environment", environment);

                    // special objects for "mock" adaptors
                    if (sisType.equals("mock")) {
                        if (object.getMetaClass().getMetaProperty("applyDB") != null) object.setProperty("applyDB", applyDB);
                        if (object.getMetaClass().getMetaProperty("bogWaiverDB") != null) object.setProperty("bogWaiverDB", bogWaiverDB);
                        if (object.getMetaClass().getMetaProperty("courseDB") != null) object.setProperty("courseDB", courseDB);
                        if (object.getMetaClass().getMetaProperty("ccPromiseGrantDB") != null) object.setProperty("ccPromiseGrantDB", ccPromiseGrantDB);
                        if (object.getMetaClass().getMetaProperty("internationalApplicationDB") != null) object.setProperty("internationalApplicationDB", internationalApplicationDB);
                        if (object.getMetaClass().getMetaProperty("enrollmentDB") != null) object.setProperty("enrollmentDB", enrollmentDB);
                        if (object.getMetaClass().getMetaProperty("environment") != null) object.setProperty("environment", environment);
                        if (object.getMetaClass().getMetaProperty("financialAidUnitsDB") != null) object.setProperty("financialAidUnitsDB", financialAidUnitsDB);
                        if (object.getMetaClass().getMetaProperty("personDB") != null) object.setProperty("personDB", personDB);
                        if (object.getMetaClass().getMetaProperty("placementDB") != null) object.setProperty("placementDB", placementDB);
                        if (object.getMetaClass().getMetaProperty("sectionDB") != null) object.setProperty("sectionDB", sectionDB);
                        if (object.getMetaClass().getMetaProperty("studentDB") != null) object.setProperty("studentDB", studentDB);
                        if (object.getMetaClass().getMetaProperty("studentPrereqDB") != null) object.setProperty("studentPrereqDB", studentPrereqDB);
                        if (object.getMetaClass().getMetaProperty("termDB") != null) object.setProperty("termDB", termDB);
                        if (object.getMetaClass().getMetaProperty("studentPlacementDB") != null) object.setProperty("studentPlacementDB", studentPlacementDB);
                        if (object.getMetaClass().getMetaProperty("sharedApplicationDB") != null) object.setProperty("sharedApplicationDB", sharedApplicationDB);
                        if (object.getMetaClass().getMetaProperty("fraudReportDB") != null) object.setProperty("fraudReportDB", fraudReportDB);
                    }

                    // Call "init" method once injection is done
                    if (object.getMetaClass().getMetaMethod("init", null) != null) object.invokeMethod("init", null);
                }

                result = (T) object.invokeMethod(methodName, args);
                ranGroovy = true;

                log.info("Finished Groovy  API call - " + infoString);
                break;
            } catch (FileNotFoundException e) {
                // ignore
            } catch (IOException | InstantiationException | IllegalAccessException e) {
                log.error("API call failed: " + infoString + ": " + e.toString(), e);
                throw new InternalServerException(e.toString());
            } catch (Exception e) {
                log.error("API call failed: " + infoString + ": " + e.toString());
                throw e;
            }
        }

        if (!ranGroovy) {
            log.error("API call failed to find groovy class to call: " + infoString);
            throw new InternalServerException("Groovy code not found: " + infoString);
        }

        return result;
    }

    /**
     * Clear the groovy cache
     */
    public void clearGroovyClassCache() {
        loader.clearCache();
        cachedClassMap.clear();
    }

    static class CachedGroovyClass {
        private String name;
        private long lastModified;

        public CachedGroovyClass(String name, long lastModified) {
            this.name = name;
            this.lastModified = lastModified;
        }

        public String getName() {
            return name;
        }

        public long getLastModified() {
            return lastModified;
        }
    }
}