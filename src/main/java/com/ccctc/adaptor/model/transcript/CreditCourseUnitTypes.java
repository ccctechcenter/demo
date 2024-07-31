package com.ccctc.adaptor.model.transcript;

/**
 * Created by james on 11/2/16.
 */
public enum CreditCourseUnitTypes {

    CarnegieUnits, //A common unit of credit used in secondary schools in the US. The unit was developed in 1906 by the Carnegie Foundation as a measure of the amount of time a student has studied a subject. For example, a total of 120 hours in one subject -- meeting 4 or 5 times a week for 40 to 60 minutes, for 36 to 40 weeks each year -- earns the student one "Carnegie unit" of high school credit. Fourteen units were deemed to constitute the minimum amount of preparation that may be interpreted as "four years of academic or high school preparation".
    ClockHours, //A unit used primarily for non-credit courses, or for a non-credit (but required) lab or clinic part of a college credit course. It is typically defined as either a 50 minute or a 60 minute hour.
    ContinuingEducationUnits, //A Continuing Education Unit is defined as ten contact (60 minute) hours in a non-credit course with prescribed requirements and evaluation methods.
    NoCredit, //This is typically used for a non credit course for which no credit is ever awarded. However, it could also be used for an individual student for a regular course for which prior agreement has been reached to award no credit for this particular student for this course.
    Other, //Any type of a unit of credit not described in any of the other enumerated values. It should be further clarified by an instance of NoteMessage.
    Quarter, //A unit of credit that describes the value of a lecture course that meets for 50 minutes per week for a term or session that meets from 10 to 12 weeks. It is typically associated with a quarter system calendar, but this is not necessarily required.
    Semester, //A unit of credit that describes the value of a lecture course that meets for 50 minutes per week for a term or session that meets for 15 or 16 weeks. It is typically associated with a semester or trimester system calendar, but this is not necessarily required.
    Units, //This is a loosely defined unit of measure of the worth of the course that is neither a quarter nor a semester unit. It is more often used for secondary school courses.
    Unreported //Since this data element would not normally be required, this value would be rarely used. If the data element were to be required and the credit unit for the course were not known, then this value could be used.

}
