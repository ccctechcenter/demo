/**
 * Created by Rasul on 1/18/2016.
 */
package api.banner

import com.ccctc.adaptor.exception.EntityConflictException
import com.ccctc.adaptor.exception.EntityNotFoundException
import com.ccctc.adaptor.exception.InvalidRequestException
import com.ccctc.adaptor.model.ApplicationStatus
import com.ccctc.adaptor.model.Cohort
import com.ccctc.adaptor.model.CohortTypeEnum
import com.ccctc.adaptor.model.OrientationStatus
import com.ccctc.adaptor.model.ResidentStatus
import com.ccctc.adaptor.util.MisEnvironment
import com.ccctc.adaptor.model.students.*
import groovy.sql.GroovyRowResult
import groovy.sql.Sql
import groovy.util.logging.Slf4j
import oracle.sql.TIMESTAMP
import org.springframework.core.env.Environment

import java.sql.Timestamp
import java.time.LocalDate
import java.time.LocalDateTime


@Slf4j
class Student {

    Environment environment

    static Map<String, MetaProperty> identifiersProperties = com.ccctc.adaptor.model.students.StudentIdentityDTO.metaClass.properties.collectEntries() {
        [(it.name.toUpperCase()): it]
    }
    static Map<String, MetaProperty> matriculationProperties = com.ccctc.adaptor.model.students.StudentMatriculationDTO.metaClass.properties.collectEntries() {
        [(it.name.toUpperCase()): it]
    }
    static Map<String, MetaProperty> allProperties = com.ccctc.adaptor.model.Student.metaClass.properties.collectEntries() {
        [(it.name.toUpperCase()): it]
    }
    static Map<String, MetaProperty> cohortProperties = com.ccctc.adaptor.model.Cohort.metaClass.properties.collectEntries() {
        [(it.name.toUpperCase()): it]
    }

    def getHomeCollege(String misCode, String cccid) {
        Sql sql = BannerConnection.getSession(environment, misCode)

        try {
            def query = MisEnvironment.getProperty(environment, misCode, "banner.cccid.getQuery")
            def student = sql.firstRow(query, [cccid: cccid])
            if (!student) {
                throw new EntityNotFoundException("No student found with the specified cccid")
            }
            query = MisEnvironment.getProperty(environment, misCode, "banner.student.misEnrollment.getQuery")
            def misRecord = sql.firstRow(query, [pidm: student.pidm])
            if (!misRecord) {
                query = MisEnvironment.getProperty(environment, misCode, "banner.student.applicationStatus.getQuery")
                misRecord = sql.firstRow(query, [pidm: student.pidm, sisTermId: "999999"])
            }
            if (misRecord) {
                query = MisEnvironment.getProperty(environment, misCode, "banner.campus.misCode.getQuery")
                def mis = sql.firstRow(query, [campus: misRecord.campus])
                if (!mis) {
                    throw new InvalidRequestException(InvalidRequestException.Errors.invalidRequest, "MIS for campus could not be determined.")
                }
                def builder = new com.ccctc.adaptor.model.StudentHomeCollege.Builder()
                builder
                        .cccid(cccid)
                        .misCode(mis?.misCode)
                return builder.build()
            }
            throw new InvalidRequestException(InvalidRequestException.Errors.invalidRequest, "The primary MIS for the student could not be determined.")
        } finally {
            sql.close()
        }
    }

    def postCohort(String cccid, CohortTypeEnum cohortName, String misCode, String sisTermId) {

        def query = MisEnvironment.getProperty(environment, misCode, "banner.cccid.getQuery")
        Sql sql = BannerConnection.getSession(environment, misCode)
        try {
            def row = sql.firstRow(query, [cccid: cccid])
            if (!row) {
                throw new InvalidRequestException(InvalidRequestException.Errors.studentNotFound, "Student with the specified CCCID does not exist.")
            }
            def term = validateTerm(sisTermId, misCode, sql)
            if (!term) {
                throw new InvalidRequestException(InvalidRequestException.Errors.termNotFound, "The term provided was not found in the SIS")
            }
            query = MisEnvironment.getProperty(environment, misCode, "banner.student.cohort.getQuery")
            def cohort
            switch(cohortName?.name()) {
                case { CohortTypeEnum.COURSE_EXCHANGE.name() }:
                    cohort = MisEnvironment.getProperty(environment, misCode, "banner.student.cohort.courseExchange")
                    break
                default:
                    throw new InvalidRequestException(InvalidRequestException.Errors.invalidCohort, "The Cohort Type does not exist.")
            }
            def cohortRow = sql.firstRow(query, [pidm: row.pidm, sisTermId: sisTermId, name: cohort])
            if (cohortRow != null) {
                throw new EntityConflictException("Student Cohort already exists.")
            } else {
                createStudentCohort(misCode, row.pidm, cohort, sisTermId)
            }
        } finally {
            sql?.close()
        }
    }

    def deleteCohort(String cccid, CohortTypeEnum cohortName, String misCode, String sisTermId) {
        def query = MisEnvironment.getProperty(environment, misCode, "banner.cccid.getQuery")
        Sql sql = BannerConnection.getSession(environment, misCode)
        try {
            def row = sql.firstRow(query, [cccid: cccid])
            if (!row) {
                throw new InvalidRequestException(InvalidRequestException.Errors.studentNotFound, "Student with the specified CCCID does not exist.")
            }
            def term = validateTerm(sisTermId, misCode, sql)
            if (!term) {
                throw new InvalidRequestException(InvalidRequestException.Errors.termNotFound, "The term provided was not found in the SIS")
            }
            def cohort
            switch(cohortName?.name()) {
                case CohortTypeEnum.COURSE_EXCHANGE.name():
                    cohort = MisEnvironment.getProperty(environment, misCode, "banner.student.cohort.courseExchange")
                    break
                default:
                    throw new InvalidRequestException(InvalidRequestException.Errors.invalidCohort, "The Cohort Type does not exist.")
            }
            query = MisEnvironment.getProperty(environment, misCode, "banner.student.cohort.getQuery")
            def cohortRow = sql.firstRow(query, [pidm: row.pidm, sisTermId: sisTermId, name: cohort])

            if (cohortRow == null) {
                throw new EntityNotFoundException("Student Cohort record not found.")
            } else {

                deleteStudentCohort(misCode, row.pidm, cohort, sisTermId)
            }
        } finally {
            sql?.close()
        }
    }

    def getCohortsList(def response, String sisTermId, String misCode, Sql sql) {
        def list = []
        def query = MisEnvironment.getProperty(environment, misCode, "banner.student.cohort.getAllQuery")

        def cohorts = sql.rows(query, [pidm: response.pidm, sisTermId: sisTermId])
        for( GroovyRowResult cohortRow : cohorts ){
            if (MisEnvironment.checkPropertyMatch(environment, misCode, 'banner.student.cohort.courseExchange', cohortRow.name)) {
                def cohort = new Cohort.Builder()
                cohort.name(CohortTypeEnum.COURSE_EXCHANGE)
                list << cohort.build()
            }
        }
        return list
    }

    void deleteStudentCohort(String misCode, def pidm, String cohort, String sisTermId) {
        def query = MisEnvironment.getProperty(environment, misCode, "banner.student.cohort.deleteQuery")
        Sql sql = BannerConnection.getSession(environment, misCode)
        try {
            sql.execute(query, [pidm     : pidm,
                                sisTermId: sisTermId,
                                name     : cohort,
                                misCode  : misCode]
            )
        } finally {
            sql?.close()
        }
    }


    void createStudentCohort(String misCode, def pidm, String cohortName, String sisTermId) {

        def query = MisEnvironment.getProperty(environment, misCode, "banner.student.cohort.insertQuery")
        Sql sql = BannerConnection.getSession(environment, misCode)
        try {
            sql.execute(query, [pidm     : pidm,
                                sisTermId: sisTermId,
                                name     : cohortName,
                                misCode  : misCode
            ]
            )
        } finally {
            sql?.close()
        }
    }

    def getStudentCCCIds(String misCode, String sisPersonId) {
        def query = MisEnvironment.getProperty(environment, misCode, "banner.person.cccidList.getQuery")
        Sql sql = BannerConnection.getSession(environment, misCode)
        try {
            def result = getPersonRecord(misCode, sisPersonId, sql)
            def cccids = []
            def rows = sql.rows(query, [sisPersonId: sisPersonId])
            for( GroovyRowResult row : rows ){
                cccids << row.cccid
            }
            return cccids
        } finally {
            sql?.close()
        }
    }

    def postStudentCCCId(String misCode, String sisPersonId, String cccId) {
        def studentCCCIds = getStudentCCCIds(misCode, sisPersonId)
        if (studentCCCIds?.size()) {
            throw new EntityConflictException("CCCID is already assigned to the student")
        }
        Sql sql = BannerConnection.getSession(environment, misCode)
        try {
            def person = getPersonRecord(misCode, sisPersonId, sql)
            def query = MisEnvironment.getProperty(environment, misCode, "banner.person.cccid.insertQuery")
            sql.execute(query, [pidm: person.pidm, cccId: cccId])
        } finally {
            sql?.close()
        }
        return getStudentCCCIds(misCode, sisPersonId)
    }

    def getPersonRecord(String misCode, String sisPersonId, Sql sql) {
        def query = MisEnvironment.getProperty(environment, misCode, "banner.person.sisPersonId.getQuery")
        def inString = buildInString([sisPersonId])
        def result = sql.rows(buildInQuery(query, inString), buildInMap([sisPersonId]))
        if (result?.size() > 0) {
            return result[0]
        } else {
            throw new InvalidRequestException(InvalidRequestException.Errors.personNotFound, "Person could not be found with the given sisPersonId")
        }
    }

    def get(String misCode, String cccid, String sisTermId) {

        Sql sql = BannerConnection.getSession(environment, misCode)
        def builder = new com.ccctc.adaptor.model.Student.Builder()
        try {
            def query = MisEnvironment.getProperty(environment, misCode, "banner.student.getQuery")
            def response = sql.firstRow(query, ["cccid": cccid])
            if (!response) {
                throw new EntityNotFoundException("No student found with the specified cccid")
            }
            def isIncarcerated, isConcurrentlyEnrolled, hasMathAssessment, hasEnglishAssessment, hasFinancialAidAward, hasBogfw,
                residency, applicationStatus, registration, disability, hasEducationPlan, completedOrientation, hasCaliforniaAddress, balance, isActive
            if (!sisTermId) {
                def termQuery = MisEnvironment.getProperty(environment, misCode, "banner.student.getTerm")
                sisTermId = sql.firstRow(termQuery)
            } else {
                def term = validateTerm(sisTermId, misCode, sql)
                if (!term) {
                    throw new InvalidRequestException(InvalidRequestException.Errors.termNotFound, "The term provided was not found in the SIS")
                }
            }
            query = MisEnvironment.getProperty(environment, misCode, "banner.student.residency.getQuery")
            residency = sql.firstRow(query, ["pidm": response.pidm, "sisTermId": sisTermId])
            query = MisEnvironment.getProperty(environment, misCode, "banner.student.applicationStatus.getQuery")
            applicationStatus = sql.firstRow(query, ["pidm": response.pidm, "sisTermId": sisTermId])
            query = MisEnvironment.getProperty(environment, misCode, "banner.student.registration.getQuery")
            registration = sql.firstRow(query, ["pidm": response.pidm, "sisTermId": sisTermId])
            query = MisEnvironment.getProperty(environment, misCode, "banner.student.attributes.getQuery")

            isIncarcerated = isConcurrentlyEnrolled = hasFinancialAidAward = hasBogfw = false
            def attrs = sql.rows(query, ["pidm": response.pidm, "sisTermId": sisTermId])
            for( GroovyRowResult attr : attrs)
            {
                if (MisEnvironment.checkPropertyMatch(environment, misCode, "banner.student.incarcerated", attr.attribute))
                    isIncarcerated = true
                if (MisEnvironment.checkPropertyMatch(environment, misCode, "banner.student.concurrentlyEnrolled", attr.attribute))
                    isConcurrentlyEnrolled = true

            }
            query = MisEnvironment.getProperty(environment, misCode, "banner.student.disability.getQuery")
            disability = sql.firstRow(query, [pidm: response.pidm, sisTermId: sisTermId])
            query = MisEnvironment.getProperty(environment, misCode, "banner.student.educationPlan.getQuery")
            def parameters = checkTermParameter(query, [pidm: response.pidm], sisTermId)
            def edPlan = sql.firstRow(query, parameters)
            if( edPlan ) {
                hasEducationPlan = edPlan.educationPlan == 'Y'
            }
            else {
                hasEducationPlan = false
            }
            query = MisEnvironment.getProperty(environment, misCode, "banner.student.finaid.getQuery")
            def awards = sql.rows(query, [pidm: response.pidm, sisTermId: sisTermId])
            for( GroovyRowResult award : awards ){
                if (MisEnvironment.checkPropertyMatch(environment, misCode, "banner.student.bogfw", award.fundCode) && award.amount > 0) {
                    hasBogfw = true
                    hasFinancialAidAward = true
                } else if (award.amount > 0) hasFinancialAidAward = true
            }
            query = MisEnvironment.getProperty(environment, misCode, "banner.student.hold.getQuery")
            def hold = sql.firstRow(query, ["pidm": response.pidm])
            query = MisEnvironment.getProperty(environment, misCode, "banner.student.visa.getQuery")
            def visa = sql.firstRow(query, ["pidm": response.pidm])
            query = MisEnvironment.getProperty(environment, misCode, "banner.student.address.getQuery")
            hasCaliforniaAddress = false
            def addrs = sql.rows(query, ["pidm"       : response.pidm,
                                "addressType": MisEnvironment.getProperty(environment, misCode, "banner.student.addressType")])
            for( GroovyRowResult addr : addrs ) {
                if (addr.state == "CA")
                    hasCaliforniaAddress = true
            }
            hasMathAssessment = hasEnglishAssessment = false
            query = MisEnvironment.getProperty(environment, misCode, "banner.student.test.getQuery")
            parameters = checkTermParameter(query, [pidm: response.pidm], sisTermId)
            def tests = sql.rows(query, parameters)
            for ( GroovyRowResult test : tests ){
                if (MisEnvironment.checkPropertyMatch(environment, misCode, "banner.student.math", test.test))
                    hasMathAssessment = true
                if (MisEnvironment.checkPropertyMatch(environment, misCode, "banner.student.english", test.test))
                    hasEnglishAssessment = true
            }
            com.ccctc.adaptor.model.OrientationStatus orientation = null

            query = MisEnvironment.getProperty(environment, misCode, "banner.student.orientation.getQuery")
            parameters = checkTermParameter(query, [pidm: response.pidm], sisTermId)
            completedOrientation = (sql.firstRow(query, parameters)?.orientation == 'Y') ? true : false
            if (completedOrientation) {
                orientation = OrientationStatus.COMPLETED
            }


            if (orientation != OrientationStatus.COMPLETED) {
                query = MisEnvironment.getProperty(environment, misCode, "banner.student.orientation.getExemptQuery")
                parameters = checkTermParameter(query, [pidm: response.pidm], sisTermId)
                def orientationExempt = (sql.firstRow(query, parameters)?.orientationExempt == 'Y') ? true : false
                if (orientationExempt == true) {
                    orientation = OrientationStatus.OPTIONAL
                } else
                    orientation = OrientationStatus.REQUIRED
            }

            query = MisEnvironment.getProperty(environment, misCode, "banner.student.balance.getQuery")
            balance = sql.firstRow(query, ["pidm": response.pidm])
            query = MisEnvironment.getProperty(environment, misCode, "banner.student.isActive.getQuery")
            isActive = sql.firstRow(query, ["pidm": response.pidm, "sisTermId": sisTermId])?.isActive == 'Y' ? true : false


            builder
                    .misCode(misCode)
                    .cohorts(getCohortsList(response, sisTermId, misCode, sql))
                    .cccid(response.cccid)
                    .orientationStatus(orientation)
                    .sisPersonId(response.sisPersonId)
                    .sisTermId(sisTermId)
                    .hasEducationPlan(hasEducationPlan ?: false)
                    .hasMathAssessment(hasMathAssessment)
                    .hasEnglishAssessment(hasEnglishAssessment)
                    .accountBalance(balance?.accountBalance ?: 0)
                    .applicationStatus(getApplicationStatus(misCode, applicationStatus))
                    .residentStatus(getResidentStatus(misCode, residency?.residentStatus))
                    .hasHold(hold?.hold == 'Y')
                    .registrationDate(registration?.registrationStartDate)
                    .hasFinancialAidAward(hasFinancialAidAward)
                    .hasBogfw(hasBogfw)
                    .visaType(visa?.visaType)
                    .hasCaliforniaAddress(hasCaliforniaAddress)
                    .isIncarcerated(isIncarcerated)
                    .isConcurrentlyEnrolled(isConcurrentlyEnrolled)
                    .dspsEligible(disability?.dspsEligible == 'Y' ? true : false)
                    .isActive(isActive)
        }
        finally {
            sql.close()
        }
        return builder.build()
    }

    def getApplicationStatus(String misCode, def applicationStatus) {
        if (!applicationStatus) {
            return ApplicationStatus.NoApplication
        } else if (!applicationStatus.applicationDecision) {
            return ApplicationStatus.ApplicationPending
        }
        switch (applicationStatus.applicationDecision) {
            case {
                MisEnvironment.checkPropertyMatch(environment, misCode, "banner.student.applicationStatus.ApplicationDenied", it)
            }:
                return ApplicationStatus.ApplicationDenied
            case {
                MisEnvironment.checkPropertyMatch(environment, misCode, "banner.student.applicationStatus.ApplicationAccepted", it)
            }:
                return ApplicationStatus.ApplicationAccepted
            case {
                MisEnvironment.checkPropertyMatch(environment, misCode, "banner.student.applicationStatus.ApplicationPending", it)
            }:
                return ApplicationStatus.ApplicationPending
            default:
                return ApplicationStatus.NoApplication
        }
    }

    def getResidentStatus(String misCode, String residentStatus) {
        if (!residentStatus) {
            return ResidentStatus.NonResident
        }
        switch (residentStatus) {
            case {
                MisEnvironment.checkPropertyMatch(environment, misCode, "banner.student.residentStatus.Resident", it)
            }:
                return ResidentStatus.Resident
                break
            case {
                MisEnvironment.checkPropertyMatch(environment, misCode, "banner.student.residentStatus.NonResident", it)
            }:
                return ResidentStatus.NonResident
                break
            case { MisEnvironment.checkPropertyMatch(environment, misCode, "banner.student.residentStatus.AB540", it) }:
                return ResidentStatus.AB540
                break
            default:
                return ResidentStatus.NonResident
        }
    }

    def checkTermParameter(String query, Map parameters, String sisTermId) {
        if (query.contains(":sisTermId"))
            parameters.put("sisTermId", sisTermId)
        return parameters
    }

    def validateTerm(String sisTermId, String misCode, Sql sql) {
        def query = MisEnvironment.getProperty(environment, misCode, "banner.term.getQuery")
        return sql.firstRow(query, [sisTermId: sisTermId])
    }

    def buildInString(List params) {
        def inString = '('
        params.eachWithIndex { param, i ->
            inString += " :personId" + i
            if (i + 1 < params.size())
                inString += ', '
            else
                inString += ' '
        }
        inString += ')'
        return inString
    }

    def buildInQuery(def sqlQuery, def inList) {
        sqlQuery.replace(':inList', inList)
    }

    def buildInMap(List params) {
        def returnMap = [:]
        params.eachWithIndex { param, i ->
            returnMap["personId" + i] = param
        }
        return returnMap
    }

    /**
     * Sets the student details for the given cccid for the given term
     * @param misCode The College Code used for grabbing college specific settings from the environment. (required)
     * @param cccId The California Community College person Id to limit results to. (required)
     * @param sisTermId the internal/college-specific Term code to report student information on (optional; if blank, tries to find 'current' term, or closest next term)
     */
    void patch(String misCode, String cccId, String sisTermId, com.ccctc.adaptor.model.Student updates) {

        //****** Validate parameters ****
        if (!misCode) {
            String errMsg = "patch: misCode cannot be null or blank"
            log.error(errMsg)
            throw new InvalidRequestException(InvalidRequestException.Errors.invalidSearchCriteria, errMsg)
        }

        if (!cccId) {
            String errMsg = "patch: cccId cannot be null or blank"
            log.error(errMsg)
            throw new InvalidRequestException(InvalidRequestException.Errors.invalidSearchCriteria, errMsg)
        } else {
            cccId = cccId.toUpperCase()
        }

        if (!updates) {
            String errMsg = "patch: Student updates cannot be null or blank"
            log.error(errMsg)
            throw new InvalidRequestException(InvalidRequestException.Errors.invalidSearchCriteria, errMsg)
        }

        if (updates.applicationStatus != null) {
            String errMsg = "patch: updating Application/Admissions status is not supported"
            log.error(errMsg)
            throw new InvalidRequestException(InvalidRequestException.Errors.invalidRequest, errMsg)
        }

        if (updates.hasEducationPlan != null) {
            String errMsg = "patch: updating hasEducationPlan is not supported"
            log.error(errMsg)
            throw new InvalidRequestException(InvalidRequestException.Errors.invalidRequest, errMsg)
        }

        if (updates.orientationStatus != null) {
            Sql sql = BannerConnection.getSession(environment, misCode)
            try {
                def student
                def query = MisEnvironment.getProperty(environment, misCode, "banner.cccid.getQuery")
                student = sql.firstRow(query, [cccid: cccId])
                if (!student) {
                    throw new EntityNotFoundException("Student not found")
                }
                if (!sisTermId && !updates.sisTermId) {
                    def termQuery = MisEnvironment.getProperty(environment, misCode, "banner.student.getTerm")
                    def term = sql.firstRow(termQuery)
                    updates.sisTermId = term?.sisTermId
                    if (!updates.sisTermId) {
                        throw new InvalidRequestException(InvalidRequestException.Errors.invalidSearchCriteria, "sisTermId not provided and could not be defaulted")
                    }
                }
                def pidm = Integer.parseInt(student.pidm.toString())
                def _sisTermId = sisTermId ?: updates.sisTermId
                if (updates.orientationStatus == OrientationStatus.COMPLETED) {
                    log.debug("patch: inserting student orientation in svrornt table")
                    String orientationCode = MisEnvironment.getProperty(environment, misCode, "banner.orientationStatus.value")
                    String orientationDataOrigin = MisEnvironment.getProperty(environment, misCode, "banner.orientationDataOrigin.value")
                    String orientationOrigCode = MisEnvironment.getProperty(environment, misCode, "banner.orientationOrigCode.value")
                    def orientationSequence
                    query = MisEnvironment.getProperty(environment, misCode, "banner.orientation.getSequence")
                    orientationSequence = sql.firstRow(query, ["pidm": pidm, "sisTermId": _sisTermId])
                    query = MisEnvironment.getProperty(environment, misCode, "banner.orientation.insertQuery")
                    sql.execute(query, [
                            orientSequence       : orientationSequence.orientSequence,
                            sisTermId            : _sisTermId,
                            misCode              : misCode,
                            pidm                 : pidm,
                            orientationCode      : orientationCode,
                            orientationOrigCode  : orientationOrigCode,
                            orientationDataOrigin: orientationDataOrigin
                    ]

                    )
                } else if (MisEnvironment.getProperty(environment, misCode, "banner.student.allowOrientationReset")) {
                    query = MisEnvironment.getProperty(environment, misCode, "banner.orientation.deleteQuery")
                    sql.execute(query, [
                            pidm     : pidm,
                            sisTermId: _sisTermId
                    ])

                } else {
                    throw new InvalidRequestException(InvalidRequestException.Errors.invalidRequest, "Updating orientation status to given value unsupported")
                }
            }finally {
                sql.close()
            }
        }
    }

    static void mapValues(Map<String, MetaProperty> propertyMap, Map source, Object destination) {
        // map values from SQL Map to any Object
        for (def m : source) {
            def key = (String) m.key
            def val = m.value
            def p = propertyMap[key.toUpperCase()]

            if (p != null) {
                if (val != null) {
                    // convert from Oracle TIMESTAMP to SQL Timestamp
                    if (val instanceof TIMESTAMP)
                        val = ((TIMESTAMP) val).timestampValue()

                    // convert Y / N to Boolean
                    if (p.type == Boolean.class) {
                        Boolean b = null
                        if (val == "Y" || val == "true") b = true
                        else if (val == "N" || val == "false") b = false

                        val = b
                    } else if (p.type == LocalDate.class) {
                        if (val instanceof Timestamp)
                            val = ((Timestamp) val).toLocalDateTime().toLocalDate()
                        else
                            val = LocalDate.parse(val as String)
                    } else if (p.type == LocalDateTime.class) {
                        if (val instanceof Timestamp)
                            val = ((Timestamp) val).toLocalDateTime()

                    }
                }

                p.setProperty(destination, val)
            }
        }
    }

/* Not using this due to performance issues.
    protected CohortTypeEnum parseCohortType(String misCode, String cohort_code) {
        CohortTypeEnum result
        if (cohort_code) {
            String configMapping = MisEnvironment.getProperty(environment, misCode, "banner.student.cohort.courseExchange." + cohort_code)
            if (configMapping) {
                CohortTypeEnum found = CohortTypeEnum.values().find {
                    return it.name().equalsIgnoreCase(configMapping)
                }
                if (!found) {
                    log.warn("Could not parse cohort_code [" + cohort_code + "]")
                } else {
                    result = found
                }
            } else {
                log.warn("cohort_code [" + cohort_code + "] mapping not found")
            }
        }
        return result
    }
*/

    List<Map> getAllByTerm(String misCode, List<String> sisTermIds, StudentFieldSet fieldSet) {

        log.debug("getAllByTerm: retrieving student data by terms")

        ;


        if (!misCode) {
            String errMsg = "getAllByTerm: misCode cannot be null or blank"
            log.error(errMsg)
            throw new InvalidRequestException(InvalidRequestException.Errors.invalidSearchCriteria, errMsg)
        }
        def sisTermId1;
        def sisTermId2;
        def sisTermId3;
        if (sisTermIds) {
            if (sisTermIds.size() == 1) {
                sisTermId1 = sisTermIds.get(0);
            }
            if (sisTermIds.size() == 2) {
                sisTermId1 = sisTermIds.get(0);
                sisTermId2 = sisTermIds.get(1);
            }
            if (sisTermIds.size() == 3) {
                sisTermId1 = sisTermIds.get(0);
                sisTermId2 = sisTermIds.get(1);
                sisTermId3 = sisTermIds.get(2);
            }
        }
        Sql sql = BannerConnection.getSession(environment, misCode)
        try {
            def builder = new com.ccctc.adaptor.model.Student.Builder()
            if (!sisTermIds || sisTermIds.empty) {

                log.debug("getAllByTerm: no terms provided; attempting to find a default")
                def termQuery = MisEnvironment.getProperty(environment, misCode, "banner.student.getTerm")
                sisTermId1 = sql.firstRow(termQuery)?.sisTermId
                /* List<com.ccctc.adaptor.model.Term> terms = sisTermId1
            if (terms != null && terms.size() > 0) {
                sisTermId1 = terms[0].sisTermId
            }*/
            }

            if (!sisTermId1) {
                String errMsg = "getAllByTerm: valid sisTermIds not provided and could not find default"
                log.error(errMsg)
                throw new InvalidRequestException(InvalidRequestException.Errors.invalidSearchCriteria, errMsg)
            }

            log.debug("getAllByTerm: params ok")


            if (fieldSet == StudentFieldSet.IDENTIFIERS) {
                def query = MisEnvironment.getProperty(environment, misCode, "banner.student.getAllTerms.getIdentifiersRecords")
                def identifiersRecords = sql.rows(query, ["sisTermId1": sisTermId1, "sisTermId2": sisTermId2, "sisTermId3": sisTermId3]);
                def returnIdentifiers = new ArrayList<com.ccctc.adaptor.model.students.StudentIdentityDTO>()

                for (int i = 0; i < identifiersRecords?.size(); i++) {
                    def returnIdentifiersRecord = new com.ccctc.adaptor.model.students.StudentIdentityDTO()
                    mapValues(identifiersProperties, identifiersRecords.get(i), returnIdentifiersRecord)
                    returnIdentifiers.add(returnIdentifiersRecord)
                }

                return returnIdentifiers

            }
            else if (fieldSet == StudentFieldSet.MATRICULATION) {
                def deleteCachetableQuery = MisEnvironment.getProperty(environment, misCode, "banner.student.getAllTerms.deleteCachetable")
                sql.execute(deleteCachetableQuery)
                def buildIdentifiersRecordsQuery = MisEnvironment.getProperty(environment, misCode, "banner.student.getAllTerms.buildIdentifiersRecords")
                sql.execute(buildIdentifiersRecordsQuery, ["sisTermId1": sisTermId1, "sisTermId2": sisTermId2, "sisTermId3": sisTermId3])
                def buildOrientationRecordsQuery = MisEnvironment.getProperty(environment, misCode, "banner.student.getAllTerms.buildOrientationRecords")
                sql.execute(buildOrientationRecordsQuery)
                def buildEdPlanRecordsQuery = MisEnvironment.getProperty(environment, misCode, "banner.student.getAllTerms.buildEdPlanRecords")
                sql.execute(buildEdPlanRecordsQuery)
                def buildAppStatusRecordsQuery = MisEnvironment.getProperty(environment, misCode, "banner.student.getAllTerms.buildAppStatusRecords")
                sql.execute(buildAppStatusRecordsQuery)
                def buildHoldRecordsQuery = MisEnvironment.getProperty(environment, misCode, "banner.student.getAllTerms.buildHoldRecords")
                sql.execute(buildHoldRecordsQuery)
                def buildRegistrationDateRecordsQuery = MisEnvironment.getProperty(environment, misCode, "banner.student.getAllTerms.buildRegistrationDateRecords")
                sql.execute(buildRegistrationDateRecordsQuery)
                def buildActiveRecordsQuery = MisEnvironment.getProperty(environment, misCode, "banner.student.getAllTerms.buildActiveStudentRecords")
                sql.execute(buildActiveRecordsQuery)
                def query = MisEnvironment.getProperty(environment, misCode, "banner.student.getAllTerms.getMatriculationRecords")
                def returntMatriculationRecord = sql.rows(query);
                def returnMatriculation = new ArrayList<com.ccctc.adaptor.model.students.StudentMatriculationDTO>()

                for (int i = 0; i < returntMatriculationRecord?.size(); i++) {
                    def returnMatriculationRecord = new com.ccctc.adaptor.model.students.StudentMatriculationDTO()
                    mapValues(matriculationProperties, returntMatriculationRecord.get(i), returnMatriculationRecord)
                    returnMatriculationRecord.orientationStatus = returnMatriculationRecord.orientationStatus ?: OrientationStatus.REQUIRED
                    returnMatriculationRecord.hasEducationPlan = returnMatriculationRecord.hasEducationPlan ?: false
                    returnMatriculationRecord.applicationStatus = returnMatriculationRecord.applicationStatus ?: ApplicationStatus.NoApplication
                    returnMatriculation.add(returnMatriculationRecord)
                }

                return returnMatriculation

            }
            else if (fieldSet == StudentFieldSet.ALL) {
                def deleteCachetableQuery = MisEnvironment.getProperty(environment, misCode, "banner.student.getAllTerms.deleteCachetable")
                sql.execute(deleteCachetableQuery)
                def buildIdentifiersRecordsQuery = MisEnvironment.getProperty(environment, misCode, "banner.student.getAllTerms.buildIdentifiersRecords")
                sql.execute(buildIdentifiersRecordsQuery, ["sisTermId1": sisTermId1, "sisTermId2": sisTermId2, "sisTermId3": sisTermId3])
                def buildOrientationRecordsQuery = MisEnvironment.getProperty(environment, misCode, "banner.student.getAllTerms.buildOrientationRecords")
                sql.execute(buildOrientationRecordsQuery)
                def buildEdPlanRecordsQuery = MisEnvironment.getProperty(environment, misCode, "banner.student.getAllTerms.buildEdPlanRecords")
                sql.execute(buildEdPlanRecordsQuery)
                def buildAppStatusRecordsQuery = MisEnvironment.getProperty(environment, misCode, "banner.student.getAllTerms.buildAppStatusRecords")
                sql.execute(buildAppStatusRecordsQuery)
                def buildCohortRecordsQuery = MisEnvironment.getProperty(environment, misCode, "banner.student.getAllTerms.buildCohortRecords")
                sql.execute(buildCohortRecordsQuery)
                def buildVisaRecordsQuery = MisEnvironment.getProperty(environment, misCode, "banner.student.getAllTerms.buildVisaRecords")
                sql.execute(buildVisaRecordsQuery)
                def addressTypeParm = MisEnvironment.getProperty(environment, misCode, "banner.student.getAllTerms.addressType")
                def buildCaladdressRecordsQuery = MisEnvironment.getProperty(environment, misCode, "banner.student.getAllTerms.buildCaladdressRecords")
                sql.execute(buildCaladdressRecordsQuery, ["addressTypeParm": addressTypeParm])
                def buildAttributesRecordsQuery = MisEnvironment.getProperty(environment, misCode, "banner.student.getAllTerms.buildAttributesRecords")
                sql.execute(buildAttributesRecordsQuery)
                def incarceratedParm = MisEnvironment.getProperty(environment, misCode, "banner.student.getAllTerms.incarcerated")
                def buildIncarnatedRecordsQuery = MisEnvironment.getProperty(environment, misCode, "banner.student.getAllTerms.buildIncarnatedRecords")
                sql.execute(buildIncarnatedRecordsQuery, ["incarceratedParm": incarceratedParm])
                def concurrentlyEnrolledParm = MisEnvironment.getProperty(environment, misCode, "banner.student.getAllTerms.bogfw")
                def buildConcurrentlyEnrolledRecordsQuery = MisEnvironment.getProperty(environment, misCode, "banner.student.getAllTerms.buildConcurrentlyEnrolledRecords")
                sql.execute(buildConcurrentlyEnrolledRecordsQuery, ["concurrentlyEnrolledParm": concurrentlyEnrolledParm])
                def mathParm = MisEnvironment.getProperty(environment, misCode, "banner.student.getAllTerms.math")
                def buildMathTestRecordsQuery = MisEnvironment.getProperty(environment, misCode, "banner.student.getAllTerms.buildMathTestRecords")
                sql.execute(buildMathTestRecordsQuery, ["mathParm": mathParm])
                def englishParm = MisEnvironment.getProperty(environment, misCode, "banner.student.getAllTerms.english")
                def buildEnglishTestRecordsQuery = MisEnvironment.getProperty(environment, misCode, "banner.student.getAllTerms.buildEnglishTestRecords")
                sql.execute(buildEnglishTestRecordsQuery, ["englishParm": englishParm])
                def buildResidencyRecordsQuery = MisEnvironment.getProperty(environment, misCode, "banner.student.getAllTerms.buildResidencyRecords")
                sql.execute(buildResidencyRecordsQuery)
                def aB540Parm = MisEnvironment.getProperty(environment, misCode, "banner.student.getAllTerms.residentStatus.AB540")
                def buildResidencyAB540RecordsQuery = MisEnvironment.getProperty(environment, misCode, "banner.student.getAllTerms.buildResidencyAB540Records")
                sql.execute(buildResidencyAB540RecordsQuery, ["aB540Parm": aB540Parm])
                def nonResidentParm = MisEnvironment.getProperty(environment, misCode, "banner.student.getAllTerms.residentStatus.NonResident")
                def buildResidencyNonResidentRecordsQuery = MisEnvironment.getProperty(environment, misCode, "banner.student.getAllTerms.buildResidencyNonResidentRecords")
                sql.execute(buildResidencyNonResidentRecordsQuery, ["nonResidentParm": nonResidentParm])
                def residentParm = MisEnvironment.getProperty(environment, misCode, "banner.student.getAllTerms.residentStatus.Resident")
                def buildResidencyResidentRecordsQuery = MisEnvironment.getProperty(environment, misCode, "banner.student.getAllTerms.buildResidencyResidentRecords")
                sql.execute(buildResidencyResidentRecordsQuery, ["residentParm": residentParm])
                def buildHoldRecordsQuery = MisEnvironment.getProperty(environment, misCode, "banner.student.getAllTerms.buildHoldRecords")
                sql.execute(buildHoldRecordsQuery)
                def buildRegistrationDateRecordsQuery = MisEnvironment.getProperty(environment, misCode, "banner.student.getAllTerms.buildRegistrationDateRecords")
                sql.execute(buildRegistrationDateRecordsQuery)
                def buildActiveRecordsQuery = MisEnvironment.getProperty(environment, misCode, "banner.student.getAllTerms.buildActiveStudentRecords")
                sql.execute(buildActiveRecordsQuery)
                def buildAidRecordsQuery = MisEnvironment.getProperty(environment, misCode, "banner.student.getAllTerms.buildAidRecords")
                sql.execute(buildAidRecordsQuery)
                def bogListparm = MisEnvironment.getProperty(environment, misCode, "banner.student.getAllTerms.bogfw")
                def buildBOGFWRecordsQuery = MisEnvironment.getProperty(environment, misCode, "banner.student.getAllTerms.buildBOGFWRecords")
                sql.execute(buildBOGFWRecordsQuery, ["bogListparm": bogListparm])
                def buildBalanceRecordsQuery = MisEnvironment.getProperty(environment, misCode, "banner.student.getAllTerms.buildBalanceRecords")
                sql.execute(buildBalanceRecordsQuery)
                def buildDisabilityRecordsQuery = MisEnvironment.getProperty(environment, misCode, "banner.student.getAllTerms.buildDisabilityRecords")
                sql.execute(buildDisabilityRecordsQuery)
                def query = MisEnvironment.getProperty(environment, misCode, "banner.student.getAllTerms.getAllRecords")
                def returnAllRecords = sql.rows(query)
                def returnAll = new ArrayList<com.ccctc.adaptor.model.Student>()

                for (int i = 0; i < returnAllRecords?.size(); i++) {
                    def returnAllRecord = new com.ccctc.adaptor.model.Student()
                    mapValues(allProperties, returnAllRecords.get(i), returnAllRecord)
                    //Due to performance concerns we are taking it out for now
                    // def returnAllCohortRecords = MisEnvironment.getProperty(environment, misCode, "banner.student.getAllTerms.returnAllCohortRecords")
                    //sql.execute(returnAllCohortRecords)
                    // def returnAllCohorts = new ArrayList<com.ccctc.adaptor.model.Cohort>()
                    // for (int j = 0; j < returnAllCohortRecords.size(); j++) {
                    //    def returnCohort = new com.ccctc.adaptor.model.Cohort()
                    //    CohortTypeEnum cohort_type = this.parseCohortType(misCode, returnAllCohortRecords.get(j).getAt("DESCRIPTION"))
                    //     returnAllCohorts.add(returnCohort)
                    //  }
                    returnAllRecord.orientationStatus = returnAllRecord.orientationStatus ?: OrientationStatus.REQUIRED
                    returnAllRecord.hasEducationPlan = returnAllRecord.hasEducationPlan ?: false
                    returnAllRecord.applicationStatus = returnAllRecord.applicationStatus ?: ApplicationStatus.NoApplication
                    returnAll.add(returnAllRecord)
                }
                return returnAll
            }
        } finally { sql.close() }
    }
}