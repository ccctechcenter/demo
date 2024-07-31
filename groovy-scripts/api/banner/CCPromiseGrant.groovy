package api.banner

import com.ccctc.adaptor.exception.EntityConflictException
import com.ccctc.adaptor.exception.EntityNotFoundException
import com.ccctc.adaptor.exception.InternalServerException
import com.ccctc.adaptor.util.MisEnvironment
import groovy.sql.GroovyRowResult
import groovy.sql.Sql
import groovy.util.logging.Slf4j
import org.springframework.core.env.Environment

import java.sql.Date
import java.sql.Timestamp

@Slf4j
class CCPromiseGrant {

    Environment environment

    static Map<String, MetaProperty> promiseGrantProperties = com.ccctc.adaptor.model.apply.CCPromiseGrant.metaClass.properties.collectEntries { [(it.name.toUpperCase()): it] }

    def get(String misCode, Long id) {
        Sql sql = BannerConnection.getSession(environment, misCode)

        try {
            def promiseGrantQuery = MisEnvironment.getProperty(environment, misCode, "sqljdbc.application.promiseGrantQuery")
            GroovyRowResult promiseGrantRecord = sql.firstRow(promiseGrantQuery, [appId: id])

            if (!promiseGrantRecord)
                throw new EntityNotFoundException()

            def returnPromiseGrant = new com.ccctc.adaptor.model.apply.CCPromiseGrant()
            Utility.mapValues(promiseGrantProperties, promiseGrantRecord, returnPromiseGrant)

            return returnPromiseGrant
        } finally {
            sql.close()
        }
    }

    def post(String misCode, com.ccctc.adaptor.model.apply.CCPromiseGrant ccPromiseGrant) {
        Sql sql = BannerConnection.getSession(environment, misCode)

        try {
            def promiseGrantQuery = MisEnvironment.getProperty(environment, misCode, "sqljdbc.application.promiseGrantQuery")
            def promiseGrantRecord = sql.firstRow(promiseGrantQuery, [appId: ccPromiseGrant.getAppId()])
            if (promiseGrantRecord)
                throw new EntityConflictException("Promise Grant ID already exists")

            insertPromiseGrant(misCode, ccPromiseGrant, sql)
        } finally {
            sql.close()
        }

        try {
            return get(misCode, ccPromiseGrant.appId)
        } catch (EntityNotFoundException e) {
            throw new InternalServerException("POST reported success, but GET failed to find record " + e.getMessage() + e.printStackTrace())
        }
    }

    def insertPromiseGrant(String misCode, ccPromiseGrant, Sql sql) {
       def insertPromiseGrantQuery = MisEnvironment.getProperty(environment, misCode, "sqljdbc.application.PromiseGrantInsertQuery")

        try {
            sql.execute(insertPromiseGrantQuery, ["preferredMethodOfContact": ccPromiseGrant.preferredMethodOfContact,
                                               "mainphoneVerified": Utility.convertBoolToYesNo(ccPromiseGrant.mainphoneVerified),
                                               "mainphoneVerifiedTimestamp": ccPromiseGrant.mainphoneVerifiedTimestamp != null ? Timestamp.valueOf(ccPromiseGrant.mainphoneVerifiedTimestamp): null,
                                               "emailVerified": Utility.convertBoolToYesNo(ccPromiseGrant.emailVerified),
                                               "emailVerifiedTimestamp": ccPromiseGrant.emailVerifiedTimestamp != null ? Timestamp.valueOf(ccPromiseGrant.emailVerifiedTimestamp): null,
                                               "addressValidationOverride": Utility.convertBoolToYesNo(ccPromiseGrant.addressValidationOverride),
                                               "addressValidationOverrideTimestamp": ccPromiseGrant.addressValidationOverrideTimestamp != null ? Timestamp.valueOf(ccPromiseGrant.addressValidationOverrideTimestamp): null,
                                               "appId": ccPromiseGrant.appId,
                                               "cccId": ccPromiseGrant.cccId,
                                               "confirmationNumber": ccPromiseGrant.confirmationNumber,
                                               "status": ccPromiseGrant.status,
                                               "appLang": ccPromiseGrant.appLang,
                                               "collegeId": ccPromiseGrant.collegeId,
                                               "yearCode": ccPromiseGrant.yearCode,
                                               "yearDescription": ccPromiseGrant.yearDescription,
                                               "determinedResidentca": Utility.convertBoolToYesNo(ccPromiseGrant.determinedResidentca),
                                               "determinedAB540Eligible": Utility.convertBoolToYesNo(ccPromiseGrant.determinedAB540Eligible),
                                               "determinedNonResExempt": Utility.convertBoolToYesNo(ccPromiseGrant.determinedNonResExempt),
                                               "lastname": ccPromiseGrant.lastname,
                                               "firstname": ccPromiseGrant.firstname,
                                               "middlename": ccPromiseGrant.middlename,
                                               "mainphone": ccPromiseGrant.mainphone,
                                               "mainphoneExt": ccPromiseGrant.mainphoneExt,
                                               "mainphoneAuthText": Utility.convertBoolToYesNo(ccPromiseGrant.mainphoneAuthText),
                                               "email": ccPromiseGrant.email,
                                               "nonUsAddress": Utility.convertBoolToYesNo(ccPromiseGrant.nonUsAddress),
                                               "streetaddress1": ccPromiseGrant.streetaddress1,
                                               "streetaddress2": ccPromiseGrant.streetaddress2,
                                               "city": ccPromiseGrant.city,
                                               "state": ccPromiseGrant.state,
                                               "province": ccPromiseGrant.province,
                                               "country": ccPromiseGrant.country,
                                               "postalcode": ccPromiseGrant.postalcode,
                                               "ssn": ccPromiseGrant.ssn,
                                               "ssnType": ccPromiseGrant.ssnType,
                                               "studentCollegeId": ccPromiseGrant.studentCollegeId,
                                               "birthdate": ccPromiseGrant.birthdate != null ? Date.valueOf(ccPromiseGrant.birthdate): null,
                                               "maritalStatus": ccPromiseGrant.maritalStatus,
                                               "regDomPartner": Utility.convertBoolToYesNo(ccPromiseGrant.regDomPartner),
                                               "bornBefore23Year": Utility.convertBoolToYesNo(ccPromiseGrant.bornBefore23Year),
                                               "marriedOrRdp": Utility.convertBoolToYesNo(ccPromiseGrant.marriedOrRdp),
                                               "usVeteran": Utility.convertBoolToYesNo(ccPromiseGrant.usVeteran),
                                               "dependents": Utility.convertBoolToYesNo(ccPromiseGrant.dependents),
                                               "parentsDeceased": Utility.convertBoolToYesNo(ccPromiseGrant.parentsDeceased),
                                               "emancipatedMinor": Utility.convertBoolToYesNo(ccPromiseGrant.emancipatedMinor),
                                               "legalGuardianship": Utility.convertBoolToYesNo(ccPromiseGrant.legalGuardianship),
                                               "homelessYouthSchool": Utility.convertBoolToYesNo(ccPromiseGrant.homelessYouthSchool),
                                               "homelessYouthHud": Utility.convertBoolToYesNo(ccPromiseGrant.homelessYouthHud),
                                               "homelessYouthOther": Utility.convertBoolToYesNo(ccPromiseGrant.homelessYouthOther),
                                               "dependentOnParentTaxes": ccPromiseGrant.dependentOnParentTaxes,
                                               "livingWithParents": Utility.convertBoolToYesNo(ccPromiseGrant.livingWithParents),
                                               "dependencyStatus": ccPromiseGrant.dependencyStatus,
                                               "certVeteranAffairs": Utility.convertBoolToYesNo(ccPromiseGrant.certVeteranAffairs),
                                               "certNationalGuard": Utility.convertBoolToYesNo(ccPromiseGrant.certNationalGuard),
                                               "eligMedalHonor": Utility.convertBoolToYesNo(ccPromiseGrant.eligMedalHonor),
                                               "eligSept11": Utility.convertBoolToYesNo(ccPromiseGrant.eligSept11),
                                               "eligPoliceFire": Utility.convertBoolToYesNo(ccPromiseGrant.eligPoliceFire),
                                               "tanfCalworks": Utility.convertBoolToYesNo(ccPromiseGrant.tanfCalworks),
                                               "ssiSsp": Utility.convertBoolToYesNo(ccPromiseGrant.ssiSsp),
                                               "generalAssistance": Utility.convertBoolToYesNo(ccPromiseGrant.generalAssistance),
                                               "parentsAssistance": Utility.convertBoolToYesNo(ccPromiseGrant.parentsAssistance),
                                               "depNumberHousehold": ccPromiseGrant.depNumberHousehold,
                                               "indNumberHousehold": ccPromiseGrant.indNumberHousehold,
                                               "depGrossIncome": ccPromiseGrant.depGrossIncome,
                                               "indGrossIncome": ccPromiseGrant.indGrossIncome,
                                               "depOtherIncome": ccPromiseGrant.depOtherIncome,
                                               "indOtherIncome": ccPromiseGrant.indOtherIncome,
                                               "depTotalIncome": ccPromiseGrant.depTotalIncome,
                                               "indTotalIncome": ccPromiseGrant.indTotalIncome,
                                               "eligMethodA": Utility.convertBoolToYesNo(ccPromiseGrant.eligMethodA),
                                               "eligMethodB": Utility.convertBoolToYesNo(ccPromiseGrant.eligMethodB),
                                               "eligBogfw": ccPromiseGrant.eligBogfw,
                                               "confirmationParentGuardian": Utility.convertBoolToYesNo(ccPromiseGrant.confirmationParentGuardian),
                                               "parentGuardianName": ccPromiseGrant.parentGuardianName,
                                               "ackFinAid": Utility.convertBoolToYesNo(ccPromiseGrant.ackFinAid),
                                               "confirmationApplicant": Utility.convertBoolToYesNo(ccPromiseGrant.confirmationApplicant),
                                               "lastPage": ccPromiseGrant.lastPage,
                                               "ssnLast4": ccPromiseGrant.ssnLast4,
                                               "tstmpSubmit": ccPromiseGrant.tstmpSubmit != null ? Timestamp.valueOf(ccPromiseGrant.tstmpSubmit): null,
                                               "tstmpCreate": ccPromiseGrant.tstmpCreate != null ? Timestamp.valueOf(ccPromiseGrant.tstmpCreate): null,
                                               "tstmpUpdate": ccPromiseGrant.tstmpUpdate != null ? Timestamp.valueOf(ccPromiseGrant.tstmpUpdate): null,
                                               "tstmpDownload": ccPromiseGrant.tstmpDownload != null ? Timestamp.valueOf(ccPromiseGrant.tstmpDownload): null,
                                               "termCode": ccPromiseGrant.termCode,
                                               "ipAddress": ccPromiseGrant.ipAddress,
                                               "campaign1": ccPromiseGrant.campaign1,
                                               "campaign2": ccPromiseGrant.campaign2,
                                               "campaign3": ccPromiseGrant.campaign3,
                                               "ssnException": Utility.convertBoolToYesNo(ccPromiseGrant.ssnException),
                                               "studentParent": Utility.convertBoolToYesNo(ccPromiseGrant.studentParent),
                                               "collegeName": ccPromiseGrant.collegeName,
                                               "preferredFirstname": ccPromiseGrant.preferredFirstname,
                                               "preferredMiddlename": ccPromiseGrant.preferredMiddlename,
                                               "preferredLastname": ccPromiseGrant.preferredLastname,
                                               "preferredName": Utility.convertBoolToYesNo(ccPromiseGrant.preferredName),
                                               "ssnNo": Utility.convertBoolToYesNo(ccPromiseGrant.ssnNo),
                                               "noPermAddressHomeless": Utility.convertBoolToYesNo(ccPromiseGrant.noPermAddressHomeless),
                                               "noMailingAddressHomeless": Utility.convertBoolToYesNo(ccPromiseGrant.noMailingAddressHomeless),
                                               "determinedHomeless": Utility.convertBoolToYesNo(ccPromiseGrant.determinedHomeless),
                                               "eligMethodD": Utility.convertBoolToYesNo(ccPromiseGrant.eligMethodD),
                                               "mainphoneintl": ccPromiseGrant.mainphoneintl,
                                               "eligExoneratedCrime": Utility.convertBoolToYesNo(ccPromiseGrant.eligExoneratedCrime),
                                               "eligCovidDeath": Utility.convertBoolToYesNo(ccPromiseGrant.eligCovidDeath),
                                               "phoneType": ccPromiseGrant.phoneType,
                                               "mailingAddressValidationOverride": Utility.convertBoolToYesNo(ccPromiseGrant.mailingAddressValidationOverride),
                                               "ipAddressAtAccountCreation": ccPromiseGrant.ipAddressAtAccountCreation,
                                               "ipAddressAtAppCreation": ccPromiseGrant.ipAddressAtAppCreation,
                                               "acceptedTerms": Utility.convertBoolToYesNo(ccPromiseGrant.acceptedTerms),
                                               "acceptedTermsTimestamp": ccPromiseGrant.acceptedTermsTimestamp != null ? Timestamp.valueOf(ccPromiseGrant.acceptedTermsTimestamp): null,
                                               "idmeConfirmationTimestamp": ccPromiseGrant.idmeConfirmationTimestamp != null ? Timestamp.valueOf(ccPromiseGrant.idmeConfirmationTimestamp): null,
                                               "idmeOptinTimestamp": ccPromiseGrant.idmeOptinTimestamp != null ? Timestamp.valueOf(ccPromiseGrant.idmeOptinTimestamp): null,
                                               "idmeWorkflowStatus": ccPromiseGrant.idmeWorkflowStatus,
                                               "studentDepsUnder18": ccPromiseGrant.studentDepsUnder18,
                                               "studentDeps18Over": ccPromiseGrant.studentDeps18Over])
        }

        catch (Exception e) {
          throw new InternalServerException("Server encountered an error: " + e.getMessage() + e.stackTrace)
        }
    }
}