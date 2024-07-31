/**
 * Created by Rasul on 5/31/16
 */
package api.banner


import com.ccctc.adaptor.exception.InvalidRequestException
import groovy.sql.Sql
import com.ccctc.adaptor.model.Email
import com.ccctc.adaptor.model.EmailType
import com.ccctc.adaptor.util.MisEnvironment
import org.springframework.core.env.Environment

class Person {


    Environment environment

    def institutionCode
    def personalCode


    def get(String misCode, String sisPersonId) {
        def resultList = getAll(misCode, [sisPersonId], [])
        if (resultList.size()) {
            return resultList[0]
        } else {
            throw new InvalidRequestException(InvalidRequestException.Errors.personNotFound, "Person was not found.")
        }
    }

    def getStudentPerson(String misCode, def sisPersonId, def eppn, def cccId) {
        def resultList = getAll(misCode, sisPersonId ? [sisPersonId] : [], cccId ? [cccId] : [], eppn ? [eppn] : [])
        if (resultList.size()) {
            return resultList[0]
        } else {
            throw new InvalidRequestException(InvalidRequestException.Errors.personNotFound, "Person was not found.")
        }
    }

    def getAll(String misCode, def sisPersonIds, def cccids) {
        return getAll(misCode, sisPersonIds, cccids, [])
    }

    def getAll(String misCode, def sisPersonIds, def cccids, def eppnsIn) {
        personalCode = MisEnvironment.getProperty(environment, misCode, "banner.person.email.personal")
        institutionCode = MisEnvironment.getProperty(environment, misCode, "banner.person.email.institution")
        // Use JDBC to make a package call or sql query
        Sql sql = BannerConnection.getSession(environment, misCode)
        def eppns = []
        eppnsIn?.each { eppn ->
            eppn = eppn.replace("@" + MisEnvironment.getProperty(environment, misCode, "banner.person.loginSuffix.student"), "")
            eppn = eppn.replace("@" + MisEnvironment.getProperty(environment, misCode, "banner.person.loginSuffix.staff"), "")
            eppns << eppn
        }
        sisPersonIds = sisPersonIds?.toList()
        cccids = cccids?.toList()
        eppns = eppns.toList()
        def sqlPeople
        def people = []
        try {
            def query
            def inString
            def listParams
            if (sisPersonIds?.size()) {
                while (sisPersonIds.size()) {
                    listParams = sisPersonIds.take(500)
                    sisPersonIds = sisPersonIds.drop(500)
                    query = MisEnvironment.getProperty(environment, misCode, "banner.person.sisPersonId.getQuery")
                    inString = buildInString(listParams)
                    sqlPeople = sql.rows(buildInQuery(query, inString), buildInMap(listParams))
                }
            } else if (cccids?.size()) {
                while (cccids.size()) {
                    listParams = cccids.take(500)
                    cccids = cccids.drop(500)
                    query = MisEnvironment.getProperty(environment, misCode, "banner.person.cccid.getQuery")
                    inString = buildInString(listParams)
                    sqlPeople = sql.rows(buildInQuery(query, inString), buildInMap(listParams))
                }
            } else if (eppns?.size()) {
                while (eppns.size()) {
                    listParams = eppns.take(500)
                    eppns = eppns.drop(500)
                    query = MisEnvironment.getProperty(environment, misCode, "banner.person.eppn.getQuery")
                    inString = buildInString(listParams)
                    sqlPeople = sql.rows(buildInQuery(query, inString), buildInMap(listParams))
                }
            } else {
                throw new InvalidRequestException(InvalidRequestException.Errors.invalidSearchCriteria, "Must provide at least one cccid or sisPersonId to search for.")
            }
            def queryList = []
            def peopleBuilderMap = [:]
            sqlPeople.each { person ->
                queryList << person.pidm
                peopleBuilderMap[person.pidm] = buildPerson(person)
            }
            listParams
            while (queryList.size()) {
                listParams = queryList.take(500)
                queryList = queryList.drop(500)
                inString = buildInString(listParams)
                def inMap = buildInMap(listParams)

                //LoginIds
                query = MisEnvironment.getProperty(environment, misCode, "banner.person.loginId.getQuery")
                def sqlLoginIds = sql.rows(buildInQuery(query, inString), inMap)
                buildLoginIds(sqlLoginIds, peopleBuilderMap)

                //LoginSuffix(Role)
                query = MisEnvironment.getProperty(environment, misCode, "banner.person.role.student.getQuery")
                def studentRoles = sql.rows(buildInQuery(query, inString), inMap)
                buildLoginSuffix(studentRoles, peopleBuilderMap, misCode)

                //Emails - query orders preferred last so it writes over non-preferred
                query = MisEnvironment.getProperty(environment, misCode, "banner.person.email.getQuery")
                inMap["personal"] = personalCode
                inMap["institution"] = institutionCode
                def sqlEmails = sql.rows(buildInQuery(query, inString), inMap)
                buildEmails(sqlEmails, peopleBuilderMap)
            }
            //Build the builders
            peopleBuilderMap.each { person ->
                person.value.misCode(misCode)
                people << person.value.build()
            }
        } finally {
            sql.close()
        }
        return people
    }

    def buildPerson(def sqlPerson) {
        def person = new Person.Builder()
        person.sisPersonId(sqlPerson.sisPersonId)
                .firstName(sqlPerson.firstName)
                .lastName(sqlPerson.lastName)
                .cccid(sqlPerson.cccid)
        return person
    }

    def buildLoginIds(List sqlLoginIds, Map people) {
        sqlLoginIds.each { loginId ->
            people[loginId.pidm].loginId(loginId.loginId)
        }
    }

    def buildLoginSuffix(List studentRoles, Map people, String misCode) {
        def studentSuffix = MisEnvironment.getProperty(environment, misCode, "banner.person.loginSuffix.student")
        def staffSuffix = MisEnvironment.getProperty(environment, misCode, "banner.person.loginSuffix.staff")
        studentRoles.each { role ->
            people[role.pidm].loginSuffix(role.student == 'Y' ? studentSuffix : staffSuffix)
        }
    }

    def buildEmails(List sqlEmails, Map people) {
        def addressList = [:]
        def currentPidm, previousPidm
        sqlEmails.each { email ->
            currentPidm = email.pidm
            if (!previousPidm) previousPidm = currentPidm
            if (currentPidm != previousPidm) {
                people[previousPidm].emailAddresses(addressList.values().toList())
                addressList = [:]
                previousPidm = currentPidm
            }
            addressList[getEmailType(email.emailCode)] = buildEmail(email)
        }
        if (addressList.size())
            people[currentPidm].emailAddresses(addressList.values().toList())

    }

    def buildEmail(def email) {
        return new Email.Builder().emailAddress(email.emailAddress)
                .type(getEmailType(email.emailCode))
                .build()

    }

    def getEmailType(def emailCode) {
        (emailCode == personalCode) ? EmailType.Personal :
                (emailCode == institutionCode ? EmailType.Institution : null)
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

}