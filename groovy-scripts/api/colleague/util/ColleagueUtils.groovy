package api.colleague.util

import groovy.transform.CompileStatic
import org.ccctc.colleaguedmiclient.service.DmiService
import org.springframework.core.env.Environment

import java.time.Instant
import java.time.LocalDate
import java.time.LocalDateTime
import java.time.LocalTime
import java.time.ZoneOffset
import java.time.format.DateTimeFormatter

/**
 * Colleague Utilities
 */
@CompileStatic
class ColleagueUtils {

    private static LocalDateTime keepAliveExpiration
    private static final int KEEP_ALIVE_SECONDS = 90

    static final LocalDate BASE_DATE = LocalDate.of(0, 12, 30)

    /**
     * Get a Colleague Property from the environment. An MIS code specific value is returned first, if available. If not,
     * a non-MIS Code specific value is returned. The properties will be named {misCode}.colleague.{value} and
     * colleague.{value} respectively.
     * <p>
     * Notes:
     * 1. If misCode is null, the misCode from the environment will be used
     * 2. Leading and trailing whitespaces are trimmed
     * 3. An empty or whitespace only property is converted to null
     *
     * @param environment  Environment
     * @param misCode      MIS code
     * @param propertyName Property name
     * @return Value of property
     */
    static String getColleagueProperty(Environment environment, String misCode, String propertyName) {
        if (misCode == null) misCode = environment.getProperty("misCode")
        String p = environment.getProperty("${misCode}.colleague.$propertyName") ?: environment.getProperty("colleague.$propertyName")
        p = p?.trim()
        return (p == "") ? null : p
    }

    /**
     * Get a Colleague Property from the environment and convert it into a list (values are assumed to be commma separated).
     * An MIS code specific value is returned first, if available. If not, a non-MIS Code specific value is returned.
     * The properties will be named {misCode}.colleague.{value} and colleague.{value} respectively.
     * <p>
     * Note: If misCode is null, the misCode from the environment will be used.
     * <p>
     * Values in the list are trimmed, so leading and trailing spaces are removed. Empty strings are converted to null.
     *
     * @param environment  Environment
     * @param misCode      MIS code
     * @param propertyName Property name
     * @return Value of property
     */
    static List<String> getColleaguePropertyAsList(Environment environment, String misCode, String propertyName) {
        String v = getColleagueProperty(environment, misCode, propertyName)
        v = v?.trim()

        if (v) {
            def split = v.split(",").toList()
            return (List<String>) split.collect { it.trim() ?: null }
        }

        return null
    }


    /**
     * Safely quote a single for use in UniQuery statements. This will wrap the string in single or double quotes and
     * remove any CR / LF characters. Note that a string containing both single and double quotes cannot be quoted
     * properly as there is no escape character. In this case, the single quotes will be removed.
     *
     * @param i String to quote
     * @return Quoted string
     */
    static String quoteString(String i) {
        if (i == null)
            return null

        String result

        // as far as I can tell, you can't construct a UniQuery string with both single and double quotes
        // as there is no escape character. So this is the best we can do - if both are passed in a string,
        // the single-quotes are removed.
        if (i.contains("\""))
            result = "'" + i.replace("'", "") + "'"
        else
            result = '"' + i + '"'

        // remove CR / LF characters - these cause problems
        return result.replace("\r", "").replace("\n", "")
    }

    /**
     * Convert a LocalDate to a Java data, timezone = local
     *
     * @param date Local Date
     * @return Java Date
     */
    static Date fromLocalDate(LocalDate date) {
        return fromLocalDateTime(date?.atStartOfDay())
    }

    /**
     * Convert a LocalTime to a Java data, timezone = local
     *
     * @param time Local Time
     * @return Java Date
     */
    static Date fromLocalTime(LocalTime time) {
        return fromLocalDateTime(time?.atDate(BASE_DATE))
    }

    /**
     * Convert a LocalDateTime to a Java data, timezone = local
     *
     * @param time Local Time
     * @return Java Date
     */
    static Date fromLocalDateTime(LocalDateTime dateTime) {
        if (dateTime == null) return null
        Instant instant = dateTime.atZone(ZoneOffset.systemDefault()).toInstant()
        return Date.from(instant)
    }


    /**
     * Convert date string and time string to Local Date Time
     *
     * @param dateString String
     * @param timeString String
     * @return LocalDateTime
     */
    static LocalDateTime localDateTimeFromString(String dateString, String timeString) {
        dateString = dateString == "null" ? null : dateString
        timeString = timeString == "null" ? null : timeString
        if (dateString != null) {
            def dateValue = LocalDate.parse(dateString, DateTimeFormatter.ofPattern("yyyy-MM-dd"))
            if (timeString != null) {
                // Return date time string
                def timeValue = LocalTime.parse(timeString, DateTimeFormatter.ofPattern("HH:mm:ss"))
                return LocalDateTime.of(dateValue, timeValue)
            } else {
                // Return date at midnight
                return LocalDateTime.of(dateValue, LocalTime.of(0, 0))
            }
        }
        null
    }

    /**
     * Utility that gets a value from an array, but only if the array is not null and is holds a value
     * at position x.
     *
     * @param array Array
     * @param index Index in array
     * @return Value
     */
    static <T> T getAt(T[] array, int index) {
        if (array == null || array.length <= index) return null
        return array[index]
    }

    /**
     * Remove any characters but 1-127 ASCII as these should not be part of a given request. Some special characters,
     * for example the delimiters @FM, @VM, etc could cause problems. Most others just would not be printing characters.
     */
    static String sanitize(String value) {
        return value?.replaceAll(/[^\x01-\x7F]/, "")
    }

    /**
     * Convert a string to proper case in a similar way as Colleague does for names
     *
     * @param str String
     * @return Proper cased string
     */
    static String properCase(String str) {
        def oldChars = str.trim().chars
        def size = oldChars.length
        def newChars = new char[size]

        def firstChar = true
        for (int x = 0; x < size; x++) {
            def c = oldChars[x]

            if (c == (' ' as char) || c == ('-' as char) || c == ('.' as char)) {
                newChars[x] = c
                firstChar = true
            } else {
                newChars[x] = firstChar ? Character.toUpperCase(c) : Character.toLowerCase(c)
                firstChar = false
            }
        }

        return new String(newChars)
    }

    /**
     * Execute a keep alive, but only if the specific interval has expired. This is useful to ensure the connection is
     * active every so often before attempting to fire off requests that will fail due to inactivity.
     *
     * @param dmiService DMI Service
     * @param force      Force?
     */
    static void keepAlive(DmiService dmiService, boolean force = false) {
        if (force || keepAliveExpiration == null || keepAliveExpiration.isBefore(LocalDateTime.now(ZoneOffset.UTC))) {
            dmiService.keepAlive()
            keepAliveExpiration = LocalDateTime.now(ZoneOffset.UTC).plusSeconds(KEEP_ALIVE_SECONDS)
        }
    }


    /**
     * Get a portion of a multi-valued key. If the key is blank, does not exist or the passed in value is null, null
     * is returned.
     *
     * Example for STUDENT.TERMS:
     * getMVKeyValue("1234567*2018FA*UG", 1) == 1234567
     * getMVKeyValue("1234567*2018FA*UG", 2) == 2018FA
     * getMVKeyValue("1234567*2018FA*UG", 3) == UG
     * getMVKeyValue("1234567*2018FA*UG", 4) == null
     *
     * @param key   Multi-valued key
     * @param value Value in key to retrieve
     * @return Key
     */
    static String getMVKeyValue(String key, int value) {
        if (key == null) return null

        char[] result = new char[key.length()]
        int x = 0
        int sep = 1
        for (char c : key.chars) {
            if (c == '*' as char) {
                if (sep == value) break

                sep++
            } else if (sep == value) {
                result[x++] = c
            }
        }

        if (x == 0) return null

        return new String(result, 0, x)
    }
}
