package com.ccctc.adaptor.util;

import java.util.HashMap;
import java.util.Map;

/**
 * Classes mapped by type
 */
public class ClassMap {

    private final Map<Class, Object> map = new HashMap<>();

    public void put(Object o) {
        map.put(o.getClass(), o);
    }

    public void put(Class type, Object o) {
        map.put(type, o);
    }

    public void putAll(Map<Class, Object> entries) {
        map.putAll(entries);
    }

    public <T> T get(Class<T> type) {
        return (T) map.get(type);
    }
}
