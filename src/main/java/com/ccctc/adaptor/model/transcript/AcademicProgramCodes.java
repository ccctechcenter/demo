package com.ccctc.adaptor.model.transcript;

/**
 * Created by james on 4/14/17.
 */
public enum AcademicProgramCodes {
    ProgramCIPCode, /*(recommended choice)
                    ProgramCIPCode
                    Code indicating a discipline or field of study assigned by the Classification
                    of Instructional Programs by the US
                    Department of Education’s National
                    Center for Education Statistics
                    */
    ProgramHEGISCode, /*(recommended choice)
                         ProgramHEGISCode
                        Code indicating a discipline or field of study assigned by Higher Education
                        General Information Survey
                      */
    ProgramCSISCode, /* ProgramCSISCode
                        Code indicating a discipline or field of
                        study assigned by Statistics Canada
                        Canadian College Student
                        Information System
                     */
    ProgramUSISCode,  /*
                        Code indicating a discipline or field of
                        study assigned by the Statistics
                        Canada University Student
                        Information System.
                     */
    ProgramESISCode,  /*
                          Code indicating a discipline or field of
                          study assigned by the Statistics
                          Canada Postsecondary Student
                          Information System.
                       */

}
