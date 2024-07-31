/*
 * Copyright (c) 2017. California Community Colleges Technology Center
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package com.ccctc.adaptor.util.transcript;

import javax.xml.bind.DatatypeConverter;
import java.text.DateFormat;
import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.Calendar;
import java.util.Date;
import java.util.GregorianCalendar;

public class DateConverter {

    public static DateFormat createYearMonthFormat() {
        return new SimpleDateFormat("yyyy-MM");
    }

    public static DateFormat createYearFormat() {
        return new SimpleDateFormat("yyyy");
    }

    public static DateFormat createDateFormat() {
        return new SimpleDateFormat("yyyy-MM-dd");
    }

    public static Date parseDate(String s) {
        return DatatypeConverter.parseDate(s).getTime();
    }

    public static Date parseTime(String s) {
        return DatatypeConverter.parseTime(s).getTime();
    }

    public static Date parseDateTime(String s) {
        return DatatypeConverter.parseDateTime(s).getTime();
    }

    public static Date parseYearMonth(String s) {
        Date date=null;
        try {
            date = createYearMonthFormat().parse(s);
        }
        catch (ParseException e) {
            throw new IllegalArgumentException(e);
        }
        return date;
    }

    public static Date parseYear(String s) {
        Date date=null;
        try {
            date = createYearFormat().parse(s);
        }
        catch (ParseException e) {
            throw new IllegalArgumentException(e);
        }
        return date;
    }

    public static String parseMonthDay(String s) {
        return s;
    }

    public static String printDate(Date dt) {
        return createDateFormat().format(dt);
    }

    public static String printTime(Date dt) {
        Calendar cal = new GregorianCalendar();
        cal.setTime(dt);
        return DatatypeConverter.printTime(cal);
    }

    public static String printDateTime(Date dt) {
        Calendar cal = new GregorianCalendar();
        cal.setTime(dt);
        return DatatypeConverter.printDateTime(cal);
    }

    public static String printYearMonth(Date dt) {
        Calendar cal = new GregorianCalendar();
        cal.setTime(dt);
        return createYearMonthFormat().format(cal.getTime());
    }

    public static String printYear(Date dt) {
        Calendar cal = new GregorianCalendar();
        cal.setTime(dt);
        return createYearFormat().format(cal.getTime());
    }

    public static String printMonthDay(String dt) {
        return dt;
    }
}
