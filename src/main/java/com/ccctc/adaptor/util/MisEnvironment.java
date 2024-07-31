package com.ccctc.adaptor.util;

import org.springframework.core.env.Environment;

import java.util.List;

/**
 * Created by Rasul on 10/13/16.
 */
public class MisEnvironment {

    /**
     * Method for coverage, you do not need to construct this static class
     */
    public MisEnvironment() {}

    /**
     *  Method that retrieves environment variables giving preference to properties prepended with the misCode.
     */
    public static String getProperty(Environment environment, String misCode, String propertyString ) {
        String property = environment.getProperty(misCode + "." + propertyString);
        if( property != null )
            return property;
        return environment.getProperty(propertyString);
    }

    public static boolean checkPropertyMatch(Environment environment, String misCode, String propertyString, String value) {
        if( value != null ) {
            String property = getProperty( environment, misCode, propertyString);
            String[] values;
            if(property != null && property.contains(",")) {
                values = property.split(",");
            }
            else {
                values = new String[] {property};
            }
            for( int i = 0; i < values.length; i++ ) {
                if( value.equals(values[i])) {
                    return true;
                }
            }
        }
        return false;
    }
}
