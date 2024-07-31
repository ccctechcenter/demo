/**
 * Created by Rasul on 1/20/2016.
 */
package api.banner

import com.ccctc.adaptor.exception.EntityNotFoundException
import com.ccctc.adaptor.exception.InvalidRequestException
import com.ccctc.adaptor.model.CourseContact
import com.ccctc.adaptor.model.CourseStatus
import com.ccctc.adaptor.model.CreditStatus
import com.ccctc.adaptor.model.GradingMethod
import com.ccctc.adaptor.model.InstructionalMethod
import com.ccctc.adaptor.model.TransferStatus
import com.ccctc.adaptor.util.MisEnvironment

import groovy.sql.Sql
import org.springframework.core.env.Environment

class Course {
    Environment environment

    def get(String misCode, sisCourseId, sisTermId) {


        def courseId = sisCourseId.split('-')
        if (courseId.size() < 2) {
            throw new InvalidRequestException(InvalidRequestException.Errors.invalidSearchCriteria, "Invalid sisCourseId provided")
        }

        // Use JDBC to make a package call or sql query
        Sql sql = BannerConnection.getSession(environment, misCode)
        def course
        try {
            course = queryCourse(misCode, sisTermId, courseId[0], courseId[1], sql)
        } finally {
            sql.close()
        }
        return course

    }

    def queryCourse(String misCode, String sisTermId, String subject, String number, Sql sql, def count = 1) {
        def query, response, title, description, support
        query = MisEnvironment.getProperty(environment, misCode, "banner.course.getQuery")
        response = sql.firstRow(query, [sisTermId: sisTermId, subject: subject, number: number])
        if (!response) {
            throw new EntityNotFoundException("Search returned no results.")
        }
        query = MisEnvironment.getProperty(environment, misCode, "banner.course.title.getQuery")
        title = sql.firstRow(query, [sisTermId: sisTermId, subject: subject, number: number])
        query = MisEnvironment.getProperty(environment, misCode, "banner.course.description.getQuery")
        description = sql.firstRow(query, [sisTermId: sisTermId, subject: subject, number: number])
        query = MisEnvironment.getProperty(environment, misCode, "banner.course.support.getQuery")
        support = sql.firstRow(query, [sisTermId: sisTermId, subject: subject, number: number])
        query = MisEnvironment.getProperty(environment, misCode, "banner.course.fee.getQuery")
        def fees = sql.firstRow(query, [sisTermId: sisTermId, subject: subject, number: number])
        query = MisEnvironment.getProperty(environment, misCode, "banner.course.transferStatus.getQuery")
        def csu, uc
        sql.eachRow(query, [sisTermId: sisTermId, subject: subject, number: number]) { transferRecord ->
            switch (transferRecord.transferStatus) {
                case MisEnvironment.getProperty(environment, misCode, "banner.course.transferStatus.csu"):
                    csu = true
                    break
                case MisEnvironment.getProperty(environment, misCode, "banner.course.transferStatus.uc"):
                    uc = true
                    break
                default:
                    break
            }
        }
        query = MisEnvironment.getProperty(environment, misCode, "banner.course.cid.getQuery")
        def cid = sql.firstRow(query, [sisTermId: sisTermId, subject: subject, number: number])
        query = MisEnvironment.getProperty(environment, misCode, "banner.course.corequisites.getQuery")
        def corequisites = []
        if (count) {
            count--
            sql.eachRow(query, [sisTermId: sisTermId, subject: subject, number: number]) { coreq ->
                corequisites << queryCourse(misCode, sisTermId, coreq.subject, coreq.number, sql, count)
            }
        }
        def contactList = []
        query = MisEnvironment.getProperty(environment, misCode, "banner.course.contact.getQuery")
        sql.eachRow(query, [sisTermId: sisTermId, subject: subject, number: number]) { contact ->
            def courseContact = new CourseContact.Builder()
            courseContact.instructionalMethod = getInstructionalMethod(contact.instructionalMethod, misCode)
            contactList << courseContact.build()

        }
        query = MisEnvironment.getProperty(environment, misCode, "banner.course.gradingMethod.getQuery")
        def graded, notGraded, passNoPassOptional, passNoPassOnly
        sql.eachRow(query, [sisTermId: sisTermId, subject: subject, number: number]) { gradeMethod ->
            switch (gradeMethod.gradingMethod) {
                case {
                    MisEnvironment.checkPropertyMatch(environment, misCode, "banner.course.gradingMethod.Graded", it)
                }:
                    graded = true
                    break;
                case {
                    MisEnvironment.checkPropertyMatch(environment, misCode, "banner.course.gradingMethod.NotGraded", it)
                }:
                    notGraded = true
                    break;
                case {
                    MisEnvironment.checkPropertyMatch(environment, misCode, "banner.course.gradingMethod.PassNoPassOptional", it)
                }:
                    passNoPassOptional = true
                    break;
                case {
                    MisEnvironment.checkPropertyMatch(environment, misCode, "banner.course.gradingMethod.PassNoPassOnly", it)
                }:
                    passNoPassOnly = true
                    break;
                default:
                    break;
            }
        }
        query = MisEnvironment.getProperty(environment, misCode, "banner.course.prerequisites.getQuery")
        def prerequisites = ''
        sql.eachRow(query, [sisTermId: sisTermId, subject: subject, number: number]) { preq ->
            prerequisites += getConnector(preq.connector) + ' ' + (preq.leftParen ?: "") + (preq.preSubject ? (preq.preSubject + '-' + preq.preNumber + ' with ' + preq.preGradeMin + ' grade minimum') :
                    (preq.testDescription + ' with ' + preq.testScore + ' score minimum')) + (preq.rightParen ?: "") + '\\n'
        }
        prerequisites = prerequisites ?: null
        def status = response.status == 'A' ? CourseStatus.Active : CourseStatus.Inactive

        def builder = new com.ccctc.adaptor.model.Course.Builder()
        builder.misCode(misCode)
                .sisCourseId(response.sisCourseId)
                .sisTermId(sisTermId)
        //.c_id(response?.c_id)
                .controlNumber(support.controlNumber)
                .subject(response.subject)
                .number(response.number)
                .title(title?.title ?: response.title)
                .description(description?.description?.asciiStream?.text)
        //.outline(response?.outline)
                .prerequisites(prerequisites)
                .corequisiteList(corequisites)
                .minimumUnits(response.minimumUnits)
                .maximumUnits(response.maximumUnits)
                .transferStatus(uc ? TransferStatus.CsuUc : (csu ? TransferStatus.Csu : TransferStatus.NotTransferable))
                .creditStatus(getCreditStatus(support?.creditStatus, csu, uc, misCode))
                .gradingMethod(passNoPassOnly ? GradingMethod.PassNoPassOnly :
                (notGraded ? GradingMethod.NotGraded :
                        ((passNoPassOptional && graded) ? GradingMethod.PassNoPassOptional :
                                (graded ? GradingMethod.Graded : null)
                        )))
                .c_id(cid.cid)
                .courseContacts(contactList)
                .fee(fees?.fee ?: 0)
                .status(status)
                .start(response.start)
                .end(response.end)
        return builder.build()
    }

    def getConnector(String connector) {
        switch (connector) {
            case 'O':
                return "Or"
            case 'A':
                return "And"
            default:
                return ''
        }
    }

    def getCreditStatus(creditStatus, csu, uc, misCode) {
        switch (creditStatus) {
            case {
                (csu || uc) && MisEnvironment.checkPropertyMatch(environment, misCode, "banner.course.creditStatus.DegreeApplicable", it)
            }:
                return CreditStatus.DegreeApplicable
            case {
                MisEnvironment.checkPropertyMatch(environment, misCode, "banner.course.creditStatus.NotDegreeApplicable", it)
            }:
                return CreditStatus.NotDegreeApplicable
            default:
                return CreditStatus.NonCredit
        }
    }

    def getInstructionalMethod(String instructionalMethod, String misCode) {
        switch (instructionalMethod) {
            case { MisEnvironment.checkPropertyMatch(environment, misCode, "banner.instructionalMethod.Lab", it) }:
                return InstructionalMethod.Lab
                break
            case { MisEnvironment.checkPropertyMatch(environment, misCode, "banner.instructionalMethod.Lecture", it) }:
                return InstructionalMethod.Lecture
                break
            case {
                MisEnvironment.checkPropertyMatch(environment, misCode, "banner.instructionalMethod.LectureLab", it)
            }:
                return InstructionalMethod.LectureLab
                break
            default:
                return InstructionalMethod.Other
        }
    }
}