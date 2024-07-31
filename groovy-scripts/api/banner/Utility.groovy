package api.banner

import com.ccctc.adaptor.util.MisEnvironment
import groovy.sql.GroovyRowResult
import oracle.sql.TIMESTAMP
import org.springframework.boot.actuate.health.Status
import org.springframework.core.env.Environment

import java.sql.Date
import java.sql.Timestamp
import java.time.LocalDate
import java.time.LocalDateTime
import java.time.format.DateTimeFormatter

class Utility {

    Environment environment
    static DateTimeFormatter dateTimeFormat = DateTimeFormatter.ofPattern ("yyyy-MM-dd HH:mm:ss" )

    def getSisVersion() {
        def bannerVersions = [:]
        def sql = BannerConnection.getSession(environment, null)
        def query = MisEnvironment.getProperty(environment, null, "sqljdbc.version.getQuery")
        try {
            def versions = sql.rows(query)
            for( GroovyRowResult version : versions ){
                bannerVersions[version.product] = version.release
            }
        }
        finally {
            sql.close()
        }
        return ["Banner Versions": bannerVersions]
    }

    def getSisConnectionStatus() {
        def query = MisEnvironment.getProperty(environment, null, "sqljdbc.health.getQuery")
        if( !query )
            query = "select 1 from dual"
        def sql = BannerConnection.getSession(environment, null)
        def result
        try {
            result = sql.firstRow(query)
            if (result) {
                return Status.UP
            }
        } catch (Exception e) {
            return Status.DOWN
        } finally {
           sql.close()
        }
        return Status.DOWN

    }

    static def convertBoolToYesNo( Boolean value ) {
        if( value == null )
            return null;
        return value ? "Y" : "N";
    }

    static void mapValues(Map<String, MetaProperty> propertyMap, GroovyRowResult source, Object destination) {
        // map values from SQL Map to any Object
        for(def m : source) {
            def key = (String) m.key
            def val = m.value
            def p = propertyMap[key.toUpperCase()]

            if (p != null) {
                if (val != null) {
                    // convert from Oracle TIMESTAMP to SQL Timestamp
                    if (val instanceof TIMESTAMP)
                        val = ((TIMESTAMP)val).timestampValue()

                    // convert Y / N to Boolean
                    if (p.type == Boolean.class) {
                        Boolean b = null
                        if (val == "Y" || val == "true") b = true
                        else if (val == "N" || val == "false") b = false

                        val = b
                    } else if (p.type == LocalDate.class) {
                        if (val instanceof Timestamp)
                            val = ((Timestamp)val).toLocalDateTime().toLocalDate()
                        else if (val instanceof Date)
                            val = val.toLocalDate()
                        else
                            val = LocalDate.parse(val as String)
                    } else if (p.type == LocalDateTime.class) {
                        if (val instanceof Timestamp)
                            val = ((Timestamp)val).toLocalDateTime()
                        else
                            val = LocalDateTime.parse(val as String, dateTimeFormat)
                    }
                }

                p.setProperty(destination, val)
            }
        }
    }
}