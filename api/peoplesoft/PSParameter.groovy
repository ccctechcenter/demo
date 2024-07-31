
package api.peoplesoft

import org.apache.commons.lang.BooleanUtils
import org.apache.commons.lang.StringUtils

import java.time.format.DateTimeFormatter
import java.time.LocalDate
import java.time.LocalDateTime

/**
 * <h1>PeopleSoft Parameter Utility</h1>
 * <summary>
 *     <p>Provides a Wrapper around the common formatting of parameters passed to peoplesoft</p>
 * </summary>
 * <usage>
 *     String cleanValue = PSParameter.ConvertBoolToCleanString(someBoolVariable)
 * </usage>
 *
 * @version 4.5.0
 *
 */
class PSParameter {

    /**
     * Cleans a string variable according to the needs of Peoplesoft
     * @param unclean The string to check null for
     * @return the unclean parameter if not null, otherwise single space
     */
    static String ConvertStringToCleanString(String unclean) {
        if(unclean == null) {
            return null
        }
        if(unclean.length() == 0) {
            return " "
        }
        return unclean
    }

    /**
     * Cleans a boolean variable according to the needs of Peoplesoft
     * @param unclean The bool to clean
     * @return Y or N per boolean value
     */
    static String ConvertBoolToCleanString(Boolean unclean) {
        if (BooleanUtils.isTrue(unclean)){
            return "Y"
        }
        if (BooleanUtils.isFalse(unclean)){
            return "N"
        }
        return null
    }

    /**
     * Cleans a integer variable according to the needs of Peoplesoft
     * @param unclean The number to clean
     * @return string representation of the integer; 0 if null
     */
    static String ConvertIntegerToCleanString(Integer unclean) {
        return String.valueOf(unclean ?: 0)
    }

    /**
     * Cleans a float variable according to the needs of Peoplesoft
     * @param unclean The number to clean
     * @return string representation of the float; 0 if null
     */
    static String ConvertFloatToCleanString(Float unclean) {
        return String.valueOf(unclean ?: 0)
    }

    /**
     * Cleans a long variable according to the needs of Peoplesoft
     * @param unclean The number to clean
     * @return string representation of the long; 0 if null
     */
    static String ConvertLongToCleanString(Long unclean) {
        return String.valueOf(unclean ?: 0)
    }

    /**
     * Cleans a double variable according to the needs of Peoplesoft
     * @param unclean The number to clean
     * @return string representation of the double; 0 if null
     */
    static String ConvertDoubleToCleanString(Double unclean) {
        return String.valueOf(unclean ?: 0)
    }

    /**
     * Cleans a date variable according to the needs of Peoplesoft
     * @param unclean The date to clean
     * * @param dateFormat the format desired for the date Time value to be converted to string as
     * @return string representation of the date; single space if null
     */
    static String ConvertDateToCleanString(LocalDate unclean, DateTimeFormatter dateFormat)
    {
        return unclean ? unclean.format(dateFormat) : " "
    }

    /**
     * Cleans a dateTime variable according to the needs of Peoplesoft
     * @param unclean The dateTime to clean
     * @param dateFormat the format desired for the date Time value to be converted to string as
     * @return string representation of the dateTime in given format; single space if null
     */
    static String ConvertDateTimeToCleanString(LocalDateTime unclean, DateTimeFormatter dateFormat)
    {
        return unclean ? unclean.format(dateFormat) : " "
    }
}

