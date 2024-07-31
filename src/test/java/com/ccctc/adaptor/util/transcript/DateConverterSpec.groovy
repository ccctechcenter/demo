package com.ccctc.adaptor.util.transcript

import com.ccctc.adaptor.util.transcript.DateConverter
import spock.lang.Specification

/**
 * Created by rcshishe on 5/25/17.
 */
class DateConverterSpec extends Specification {

    def "Test date converter"() {
        setup:
            String date = "2017-12-05"
            String dateTime = "1985-05-01-07:00"
            Date newDate = new Date()
        when:
            def result = DateConverter.parseDate(date)
        then:
            result instanceof Date
            DateConverter.parseDateTime(dateTime) instanceof Date
            DateConverter.parseTime(dateTime) instanceof Date
            DateConverter.parseYearMonth(dateTime)
            DateConverter.parseYear(dateTime) instanceof Date
            DateConverter.parseMonthDay(dateTime) == dateTime
            DateConverter.printMonthDay(dateTime) == dateTime
            DateConverter.printDate(newDate) instanceof String
            DateConverter.printDateTime(newDate) instanceof String
            DateConverter.printYear(newDate) instanceof String
            DateConverter.printTime(newDate) instanceof String
            DateConverter.printYearMonth(newDate) instanceof String
    }
}
