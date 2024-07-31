package api.colleague

import api.colleague.collection.TupleIterable
import api.colleague.util.ColleagueUtils
import api.colleague.util.DataUtils
import api.colleague.util.DmiDataServiceAsync
import com.ccctc.adaptor.exception.InvalidRequestException
import com.ccctc.adaptor.model.Email
import com.ccctc.adaptor.model.EmailType
import com.ccctc.adaptor.model.Person as CAPerson
import com.ccctc.adaptor.exception.EntityNotFoundException
import com.ccctc.adaptor.util.ClassMap
import org.ccctc.colleaguedmiclient.model.ColleagueData
import org.ccctc.colleaguedmiclient.service.DmiCTXService
import org.ccctc.colleaguedmiclient.service.DmiDataService
import org.ccctc.colleaguedmiclient.service.DmiService
import org.springframework.cache.Cache
import org.springframework.core.env.Environment

class Person {

    // batch size for various operations
    private static final BATCH_SIZE = 250

    DmiCTXService dmiCTXService
    DmiDataService dmiDataService
    DmiDataServiceAsync dmiDataServiceAsync
    DataUtils dataUtils

    // columns
    static personColumns = ["LAST.NAME", "FIRST.NAME", "PERSON.ALT.IDS", "PERSON.ALT.ID.TYPES", "PERSON.EMAIL.ADDRESSES",
                            "PERSON.PREFERRED.EMAIL"]

    static orgEntityColumns = ["OE.ORG.ENTITY.ENV"]

    static orgEntityEnvColumns = ["OEE.RESOURCE", "OEE.USERNAME"]

    //
    // configuration from properties files
    //
    String eppnStudentOrStaffRule
    String studentEppnSuffix
    String staffEppnSuffix
    List<String> cccIdTypes
    List<String> emailDomains


    /**
     * Initialize services, read properties
     */
    void colleagueInit(String misCode, Environment environment, ClassMap services) {
        def dmiService = services.get(DmiService.class)
        def cache = services.get(Cache.class)
        this.dmiDataService = services.get(DmiDataService.class)
        this.dmiCTXService = services.get(DmiCTXService.class)
        this.dmiDataServiceAsync = new DmiDataServiceAsync(dmiDataService, cache)
        this.dataUtils = new DataUtils(misCode, environment, dmiService, dmiCTXService, dmiDataService, cache)

        // read configuration
        this.eppnStudentOrStaffRule = ColleagueUtils.getColleagueProperty(environment, misCode, "eppn.student.or.staff.rule")
        this.studentEppnSuffix = ColleagueUtils.getColleagueProperty(environment, misCode, "student.eppn.suffix")
        this.staffEppnSuffix = ColleagueUtils.getColleagueProperty(environment, misCode, "staff.eppn.suffix")
        this.cccIdTypes = ColleagueUtils.getColleaguePropertyAsList(environment, misCode, "ccc.id.types")
        this.emailDomains = ColleagueUtils.getColleaguePropertyAsList(environment, misCode, "email.domains")

        ColleagueUtils.keepAlive(dmiService)
    }


    /**
     * Get a PERSON record by SIS Person ID
     *
     * @param misCode     MIS Code
     * @param sisPersonId SIS Person ID
     * @return Person
     */
    CAPerson get(String misCode, String sisPersonId) {
        assert sisPersonId != null

        def fPerson = dmiDataServiceAsync.singleKeyAsync("CORE", "PERSON", personColumns, sisPersonId)

        // ORG.ENTITY -> ORG.ENTITY.ENV to get login information
        def orgEntity = dmiDataService.singleKey("UT", "ORG.ENTITY", orgEntityColumns, sisPersonId)
        def oeeId = (String) orgEntity?.values?.get("OE.ORG.ENTITY.ENV")
        def orgEntityEnv = oeeId ? dmiDataService.singleKey("UT", "ORG.ENTITY.ENV", orgEntityEnvColumns, oeeId) : null

        def person = fPerson.get()

        if (!person)
            throw new EntityNotFoundException("Person not found")

        // check rules for EPPN suffix
        def suffixOverride = null
        if (eppnStudentOrStaffRule) {
            def studentLogins = checkLoginSuffixRule([person.key])
            suffixOverride = studentLogins?.contains(person.key) ? studentEppnSuffix : staffEppnSuffix
        }

        def result = parse(misCode, person, orgEntityEnv, suffixOverride)

        return result
    }


    /**
     * Get PERSON record(s) associated with a list of SIS Person IDs and/or CCC IDs
     *
     * @param misCode      MIS Code
     * @param sisPersonIds SIS Person IDs
     * @param cccids       CCC IDs
     * @return List of PERSON records
     */
    List<CAPerson> getAll(String misCode, String[] sisPersonIds, String[] cccids) {

        def ids = sisPersonIds?.toList() ?: []

        if (cccids != null) {
            def pp = dataUtils.getColleagueIds(cccids.toList())
            ids.addAll(pp)
        }

        ids = ids.unique().findAll { it }

        def fPerson = dmiDataServiceAsync.batchKeysAsync("CORE", "PERSON", personColumns, ids)

        // ORG.ENTITY -> ORG.ENTITY.ENV to get login information
        def orgEntity = dmiDataService.batchKeys("UT", "ORG.ENTITY", orgEntityColumns, ids)
        def oeeIds = orgEntity.collect { (String) it.values["OE.ORG.ENTITY.ENV"] }.findAll { it }
        def orgEntityEnv = oeeIds ? dmiDataService.batchKeys("UT", "ORG.ENTITY.ENV", orgEntityEnvColumns, oeeIds) : []

        def persons = fPerson.get()

        if (!persons) return []
        
        // index ORG.ENTITY and ORG.ENTITY.ENV for quick lookups
        Map<String, ColleagueData> oeMap = orgEntity?.collectEntries { [(it.key): it] }
        Map<String, ColleagueData> oeeMap = orgEntityEnv?.collectEntries { [(it.key): it] }
        Set<String> studentLogins = eppnStudentOrStaffRule ? checkLoginSuffixRule(persons.key) : null

        def result = persons.collect {
            def oeeId = (String) oeMap[it.key]?.values?.get("OE.ORG.ENTITY.ENV")
            def oeeRec = oeeId ? oeeMap?.get(oeeId) : null

            def suffixOverride = null
            if (eppnStudentOrStaffRule)
                suffixOverride = studentLogins?.contains(it.key) ? studentEppnSuffix : staffEppnSuffix

            parse(misCode, it, oeeRec, suffixOverride)
        }

        return result
    }

    /**
     * Parse person data
     */
    CAPerson parse(String misCode, ColleagueData person, ColleagueData orgEntityEnv, String suffixOverride) {
        if (!person) return null

        def loginId = ((String) orgEntityEnv?.values?.get("OEE.USERNAME"))?.trim()
        def cccids = dataUtils.getCccIds((String[]) person.values["PERSON.ALT.IDS"], (String[]) person.values["PERSON.ALT.ID.TYPES"])
        def emails = getEmails(person)

        def suffix = null

        if (loginId) {
            if (suffixOverride) {
                suffix = suffixOverride
            } else if (emails) {
                // If there is no rule to determine EPPN type, see if they have an email address assigned with the appropriate EPPN
                def emailList = emails.collect { i -> i.emailAddress.toLowerCase() }

                if (staffEppnSuffix)
                    suffix = emailList.contains(loginId.toLowerCase() + "@" + staffEppnSuffix.toLowerCase()) ? staffEppnSuffix : null

                if (studentEppnSuffix && suffix == null)
                    suffix = emailList.contains(loginId.toLowerCase() + "@" + studentEppnSuffix.toLowerCase()) ? studentEppnSuffix : null
            }
        }

        def result = new CAPerson.Builder()
                .misCode(misCode)
                .sisPersonId(person.key)
                .firstName((String) person.values["FIRST.NAME"])
                .lastName((String) person.values["LAST.NAME"])
                .cccid(cccids?.first())
                .loginId(loginId)
                .loginSuffix(suffix)
                .emailAddresses(emails ?: null)
                .build()

        return result
    }


    /**
     * Get email addresses for a Person. Preferred email address will be the first in the list.
     */
    List<Email> getEmails(ColleagueData person) {
        if (!person.values["PERSON.EMAIL.ADDRESSES"]) return null

        def emails = []
        def tuple = new TupleIterable(
                (String[]) person.values["PERSON.EMAIL.ADDRESSES"],
                (String[]) person.values["PERSON.PREFERRED.EMAIL"])

        for(def e : tuple) {
            def emailAddress = ((String)e[0])?.trim()
            if (emailAddress) {
                def domainMatch = emailDomains?.find { i -> emailAddress.toLowerCase().contains("@" + i.toLowerCase()) }
                def emailType = domainMatch ? EmailType.Institution : EmailType.Personal

                def email = new Email.Builder()
                        .emailAddress(emailAddress)
                        .type(emailType)
                        .build()

                // add to the beginning if "preferred"
                if (e[1] == "Y") {
                    emails.add(0, email)
                } else {
                    emails.add(email)
                }
            }
        }

        return emails
    }


    /**
     * Check the rules for login suffix via the rule named in eppnStudentOrStaffRule. Any Colleague IDs that pass the
     * rule are counted as a "Student" whereas any that fail are "Staff".
     *
     * Processing is done in batches to avoid overloading the DMI.
     */
    Set<String> checkLoginSuffixRule(List<String> keys) {
        def batches = keys.collate(BATCH_SIZE)
        def results = batches.collect { dataUtils.executeRule([eppnStudentOrStaffRule], it) }
        return new HashSet<>((List<String>) results.collect { i -> i.validIds }.flatten())
    }


    /**
     * Get a PERSON record for a student by SIS Person ID, EPPN or CCC ID.
     *
     * If multiple criteria are specified they are treated as "AND" conditions.
     *
     * @param misCode     MIS Code
     * @param sisPersonId SIS Person ID
     * @param eppn        EPPN
     * @param cccId       CCC ID
     * @return Person
     */
    CAPerson getStudentPerson(String misCode, String sisPersonId, String eppn, String cccId) {

        List<String> ids = null

        if (sisPersonId)
            ids = [sisPersonId]

        if (eppn) {
            def loginId = eppn.split("@")[0]

            // search for the login ID as is, uppercase and lowercase
            def loginIds = [loginId, loginId.toLowerCase(), loginId.toUpperCase()]
                    .collect { ColleagueUtils.quoteString(it) }
                    .join("")

            def query = "OEE.USERNAME = " + loginIds + " SAVING UNIQUE OEE.RESOURCE"
            def b = dmiDataService.selectKeys("ORG.ENTITY.ENV", query)

            if (ids != null)
                ids = ids.intersect(b.toList())
            else
                ids = b.toList()
        }

        if (cccId) {
            def a = dataUtils.getColleagueIds(cccId)

            if (ids != null)
                ids = ids.intersect(a)
            else
                ids = a
        }

        if (ids.size() == 1) {
            def person = get(misCode, ids[0])

            // make sure the eppn matches as our query only checked the first portion of it
            def personEppn = person.loginId ? (person.loginId + "@" + person.loginSuffix).toLowerCase() : null
            if (!eppn || eppn.toLowerCase() == personEppn) return person

            throw new EntityNotFoundException("Student not found")
        }

        if(ids.size() > 1)
            throw new InvalidRequestException(InvalidRequestException.Errors.multipleResultsFound, "Multiple Person records found for search criteria")

        throw new EntityNotFoundException("Student not found")
    }

}