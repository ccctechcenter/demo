package com.ccctc.adaptor.util;

/**
 * Service to run custom groovy scripts
 */
public interface GroovyService {
    <T> T run(String misCode, String objectName, String methodName, Object[] args);
    <T> T run(String misCode, String[] searchPath, String objectName, String methodName, Object[] args);
    void clearGroovyClassCache();
}