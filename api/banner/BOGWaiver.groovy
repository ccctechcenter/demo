/* Created by Kumar 6/5/17 */
package api.banner

import com.ccctc.adaptor.exception.EntityConflictException
import com.ccctc.adaptor.exception.EntityNotFoundException
import com.ccctc.adaptor.exception.InvalidRequestException
import com.ccctc.adaptor.model.MaritalStatus
import com.ccctc.adaptor.util.MisEnvironment
import groovy.sql.Sql
import org.springframework.core.env.Environment

class BOGWaiver{

    Environment environment

    def get (String misCode, String cccid, String sisTermId){

        def bogf = new com.ccctc.adaptor.model.BOGWaiver.Builder()

        Sql sql = BannerConnection.getSession(environment, misCode)

        def query = MisEnvironment.getProperty( environment, misCode, "banner.cccid.getQuery")
        try{
            def mrow = sql.firstRow(query, [cccid: cccid])
            if (!mrow) {
                throw new InvalidRequestException(InvalidRequestException.Errors.studentNotFound, "Student with the specified CCCID does not exist.")
            }
            def term = validateTerm(sisTermId, misCode, sql)
            if(!term) {
                throw new InvalidRequestException(InvalidRequestException.Errors.termNotFound, "The term provided does not exist in the SIS")
            }
            query = MisEnvironment.getProperty(environment, misCode, "banner.bogwaiver.bogfw.getQuery")
            def row = sql.firstRow(query, [pidm:mrow.pidm,  sisTermId: sisTermId, misCode:misCode])
            if (!row) {
                throw new EntityNotFoundException("No BOGFW record found for Student.")
            }


            def maritalStatus
            def dependentParentTaxes
            def dependencyStatus

             bogf.cccid(cccid)
             bogf.sisTermId(sisTermId)

            if(row.maritalStatus) {
                switch (row.maritalStatus) {
                    case 'S':
                        maritalStatus = MaritalStatus.SINGLE
                        break
                    case 'M':
                        maritalStatus = MaritalStatus.MARRIED
                        break
                    case 'D':
                        maritalStatus = MaritalStatus.DIVORCED
                        break
                    case 'W':
                        maritalStatus = MaritalStatus.WIDOWED
                        break
                    case 'X':
                        maritalStatus = MaritalStatus.SEPARATED
                        break

                }
                bogf.maritalStatus(maritalStatus)
            }

            if ( row.regDomPartner )
                bogf.regDomPartner(decodeYN(row.regDomPartner))
            if (row.bornBefore23Year)
                bogf.bornBefore23Year(decodeYN(row.bornBefore23Year))
            if (row.marriedOrRdp)
                bogf.marriedOrRDP(decodeYN(row.marriedOrRdp))
            if (row.usVeteran)
                bogf.usVeteran(decodeYN(row.usVeteran))
            if (row.dependents)
                bogf.dependents(decodeYN(row.dependents))
            if (row.parentsDeceased)
                bogf.parentsDeceased(decodeYN(row.parentsDeceased))
            if (row.emancipatedMinor)
                bogf.emancipatedMinor(decodeYN(row.emancipatedMinor))
            if (row.legalGuardianShip)
                bogf.legalGuardianship(decodeYN(row.legalGuardianShip))
             if (row.homelessYouthSchool)
                bogf.homelessYouthSchool(decodeYN(row.homelessYouthSchool))
             if (row.homelessYouthHUD)
                bogf.homelessYouthHUD(decodeYN(row.homelessYouthHUD))
             if (row.homelessYouthOther)
                bogf.homelessYouthOther(decodeYN(row.homelessYouthOther))


            if(row.dependentOnParentTaxes) {
                switch (row.dependentOnParentTaxes) {
                    case 'N':
                    case '2':
                        dependentParentTaxes = com.ccctc.adaptor.model.BOGWaiver.DependentOnParentTaxesEnum.NO
                        break
                    case 'Y':
                    case '1':
                        dependentParentTaxes = com.ccctc.adaptor.model.BOGWaiver.DependentOnParentTaxesEnum.YES
                        break
                    case 'W':
                    case '0':
                        dependentParentTaxes = com.ccctc.adaptor.model.BOGWaiver.DependentOnParentTaxesEnum.PARENTS_NOT_FILE
                        break
                }
                bogf.dependentOnParentTaxes(dependentParentTaxes)
            }

             if (row.livingWithParents)
                bogf.livingWithParents(decodeYN(row.livingWithParents))
            if(row.dependencyStatus) {
                switch (row.dependencyStatus) {
                    case 'D':
                        dependencyStatus = com.ccctc.adaptor.model.BOGWaiver.DependencyStatus.DEPENDENT
                        break;
                    case 'I':
                        dependencyStatus = com.ccctc.adaptor.model.BOGWaiver.DependencyStatus.INDEPENDENT
                        break;
                }
                bogf.dependencyStatus(dependencyStatus)
            }

            if (row.certVeteranAffairs)
                bogf.certVeteranAffairs(decodeYN(row.certVeteranAffairs))
            if (row.certNationalGuard)
               bogf.certNationalGuard(decodeYN(row.certNationalGuard))
            if (row.eligMedalHonor)
               bogf.eligMedalHonor(decodeYN(row.eligMedalHonor))
            if (row.eligSept11)
               bogf.eligSept11(decodeYN(row.eligSept11))
            if (row.eligPoliceFire)
               bogf.eligPoliceFire(decodeYN(row.eligPoliceFire))
            if (row.tanfCalWorks)
               bogf.tanfCalworks(decodeYN(row.tanfCalWorks))
            if (row.ssiSSP)
               bogf.ssiSSP(decodeYN(row.ssiSSP))
            if (row.generalAssistance)
               bogf.generalAssistance(decodeYN(row.generalAssistance))
            if (row.parentsAssistance)
               bogf.parentsAssistance(decodeYN(row.parentsAssistance))
            bogf.depNumberHousehold(row.depNumberHousehold?.intValueExact())

            bogf.indNumberHousehold(row.indNumberHousehold?.intValueExact())

            bogf.depGrossIncome(row.depGrossIncome?.intValueExact())

            bogf.indGrossIncome(row.indGrossIncome?.intValueExact())

            bogf.depOtherIncome(row.depOtherIncome?.intValueExact())

            bogf.indOtherIncome(row.indOtherIncome?.intValueExact())

            bogf.depTotalIncome(row.depTotalIncome?.intValueExact())

            bogf.indTotalIncome(row.indTotalIncome?.intValueExact())
            if( row.eligibility ) {
                bogf.eligibility(getCCCTCEligibility(row.eligibility, misCode))
            }
            if( row.determinedResidentCA ) {
                bogf.determinedResidentCA(decodeYN(row.determinedResidentCA))
            }
            if( row.determinedAB540Eligible ) {
                bogf.determinedAB540Eligible(decodeYN(row.determinedAB540Eligible))
            }
            if( row.determinedNonResExempt ) {
                bogf.determinedNonResExempt(decodeYN(row.determinedNonResExempt))
            }
            if( row.determinedHomeless ) {
                bogf.determinedHomeless(decodeYN(row.determinedHomeless))
            }

        }finally {
            sql?.close()
        }
      return bogf.build()

    }

    def getCCCTCEligibility (def eligibility, String misCode) {
        switch ( eligibility ) {
            case { MisEnvironment.checkPropertyMatch(environment, misCode, "banner.bogfw.methodA", it )}:
                return com.ccctc.adaptor.model.BOGWaiver.Eligibility.METHOD_A
            case { MisEnvironment.checkPropertyMatch(environment, misCode, "banner.bogfw.methodB", it )}:
                return com.ccctc.adaptor.model.BOGWaiver.Eligibility.METHOD_B
            case { MisEnvironment.checkPropertyMatch(environment, misCode, "banner.bogfw.methodD", it )}:
                return com.ccctc.adaptor.model.BOGWaiver.Eligibility.METHOD_D
            case { MisEnvironment.checkPropertyMatch(environment, misCode, "banner.bogfw.methodAB", it )}:
                return com.ccctc.adaptor.model.BOGWaiver.Eligibility.METHOD_A_AND_B
            default:
                return com.ccctc.adaptor.model.BOGWaiver.Eligibility.NOT_ELIGIBLE
        }
        return null
    }

    def decodeYN( def value ) {
        if( value == 'Y' )
            return true
        else if( value == 'N')
            return false
        else
            return null
    }

    def post (String misCode,com.ccctc.adaptor.model.BOGWaiver bogfw){
        def query = MisEnvironment.getProperty(environment, misCode, "banner.cccid.getQuery")
        Sql sql = BannerConnection.getSession(environment, misCode)
        try {
            def student = sql.firstRow(query, [cccid: bogfw.cccid])
            if (!student) {
                throw new InvalidRequestException(InvalidRequestException.Errors.studentNotFound, "Student with the specified CCCID does not exist.")
            }

            query = MisEnvironment.getProperty(environment, misCode, "banner.term.getQuery")
            def term = sql.firstRow(query, [sisTermId: bogfw.sisTermId])
            if (!term) {
                throw new InvalidRequestException(InvalidRequestException.Errors.termNotFound, "Term does not exist.")
            }

            query = MisEnvironment.getProperty(environment, misCode, "banner.bogwaiver.stage.getQuery")
            def row = sql.firstRow(query,[cccid: bogfw.cccid, sisTermId: bogfw.sisTermId, misCode:misCode])
            if(row?.bogExists != null) {
               throw new EntityConflictException("Bogwaiver record already exists.")
            } else{
                createBogfw(misCode,student.pidm, term, bogfw)
               }

        } finally {
           sql?.close()
            }
        return bogfw
    }

    void createBogfw(String misCode, def pidm, def term, com.ccctc.adaptor.model.BOGWaiver bogfw) {

        def query = MisEnvironment.getProperty(environment, misCode, "banner.bogwaiver.bogfw.insertQuery")
        Sql sql = BannerConnection.getSession(environment, misCode)
     try {


         sql.execute(query,
                 [misCode               : misCode,
                  pidm                  : pidm,
                  cccid                 : bogfw.cccid,
                  sisTermId             : bogfw.sisTermId,
                  srcMisCode            : bogfw.misCode,
                  maritalStatus         : getMartlStatus(bogfw.maritalStatus),
                  regDomPartner         : getValue(bogfw.regDomPartner),
                  bornBefore23Year      : getValue(bogfw.bornBefore23Year),
                  usVeteran             : getValue(bogfw.usVeteran),
                  dependents            : getValue(bogfw.dependents),
                  parentsDeceased       : getValue(bogfw.parentsDeceased),
                  emancipatedMinor     : getValue(bogfw.emancipatedMinor),
                  legalGuardianship     : getValue(bogfw.legalGuardianship),
                  homelessYouthSchool   : getValue(bogfw.homelessYouthSchool),
                  homelessYouthHUD      : getValue(bogfw.homelessYouthHUD),
                  homelessYouthOther    : getValue(bogfw.homelessYouthOther),
                  dependentOnParentTaxes: getDepParentStatus(bogfw.dependentOnParentTaxes),
                  livingWithParents     : getValue(bogfw.livingWithParents),
                  dependencyStatus      : getDepStatus(bogfw.dependencyStatus),
                  certVeteranAffairs    : getValue(bogfw.certVeteranAffairs),
                  certNationalGuard     : getValue(bogfw.certNationalGuard),
                  eligMedalHonor        : getValue(bogfw.eligMedalHonor),
                  eligSept11            : getValue(bogfw.eligSept11),
                  eligPoliceFire        : getValue(bogfw.eligPoliceFire),
                  tanfCalworks          : getValue(bogfw.tanfCalworks),
                  ssiSSP                : getValue(bogfw.ssiSSP),
                  generalAssistance     : getValue(bogfw.generalAssistance),
                  parentsAssistance     : getValue(bogfw.parentsAssistance),
                  depNumberHousehold    : bogfw.depNumberHousehold,
                  indNumberHousehold    : bogfw.indNumberHousehold,
                  depGrossIncome        : bogfw.depGrossIncome,
                  indGrossIncome        : bogfw.indGrossIncome,
                  depOtherIncome        : bogfw.depOtherIncome,
                  indOtherIncome        : bogfw.indOtherIncome,
                  depTotalIncome        : bogfw.depTotalIncome,
                  indTotalIncome        : bogfw.indTotalIncome,
                  eligibility           : bogfw.eligibility?.name(),
                  determinedResidentCA  : getValue(bogfw.determinedResidentCA),
                  determinedAB540Eligible: getValue(bogfw.determinedAB540Eligible),
                  determinedNonResExempt: getValue(bogfw.determinedNonResExempt),
                  determinedHomeless    : getValue(bogfw.determinedHomeless)
                 ]
                 )
         if( MisEnvironment.getProperty(environment, misCode, "banner.bogwaiver.populate") == "1") {
             query = MisEnvironment.getProperty(environment, misCode, "banner.bogwaiver.bogfw.getQuery")
             def row = sql.firstRow(query, [pidm:pidm,  sisTermId: term.sisTermId, misCode:misCode])
             if (!row) {
                 query = MisEnvironment.getProperty(environment, misCode, "banner.bogwaiver.insertQuery")
                 sql.execute(query, [
                         pidm                   : pidm,
                         aidyCode               : term.aidyCode,
                         collCode               : MisEnvironment.getProperty(environment, misCode, "banner.collegeCode"),
                         regDomPartner          : getValue(bogfw.regDomPartner),
                         bornBefore23Year       : getValue(bogfw.bornBefore23Year),
                         marriedOrRdp           : getValue(bogfw.marriedOrRDP),
                         usVeteran              : getValue(bogfw.usVeteran),
                         dependents             : getValue(bogfw.dependents),
                         parentsDeceased        : getValue(bogfw.parentsDeceased),
                         emancipatedMinor       : getValue(bogfw.emancipatedMinor),
                         legalGuardianship      : getValue(bogfw.legalGuardianship),
                         homelessYouthSchool    : getValue(bogfw.homelessYouthSchool),
                         homelessYouthHUD       : getValue(bogfw.homelessYouthHUD),
                         homelessYouthOther     : getValue(bogfw.homelessYouthOther),
                         dependentOnParentTaxes : getDepParentStatus(bogfw.dependentOnParentTaxes),
                         livingWithParents      : getValue(bogfw.livingWithParents),
                         dependencyStatus       : getDepStatus(bogfw.dependencyStatus),
                         certVeteranAffairs     : getValue(bogfw.certVeteranAffairs),
                         certNationalGuard      : getValue(bogfw.certNationalGuard),
                         eligMedalHonor         : getValue(bogfw.eligMedalHonor),
                         eligSept11             : getValue(bogfw.eligSept11),
                         eligPoliceFire         : getValue(bogfw.eligPoliceFire),
                         tanfCalworks           : getValue(bogfw.tanfCalworks),
                         ssiSSP                 : getValue(bogfw.ssiSSP),
                         generalAssistance      : getValue(bogfw.generalAssistance),
                         parentsAssistance      : getValue(bogfw.parentsAssistance),
                         depNumberHousehold     : bogfw.depNumberHousehold,
                         indNumberHousehold     : bogfw.indNumberHousehold,
                         depGrossIncome         : bogfw.depGrossIncome,
                         indGrossIncome         : bogfw.indGrossIncome,
                         depOtherIncome         : bogfw.depOtherIncome,
                         indOtherIncome         : bogfw.indOtherIncome,
                         depTotalIncome         : bogfw.depTotalIncome,
                         indTotalIncome         : bogfw.indTotalIncome,
                         eligibility            : getBannerEligibility(bogfw.eligibility, misCode),
                         determinedResidentCA   : getValue(bogfw.determinedResidentCA),
                         determinedAB540Eligible: getValue(bogfw.determinedAB540Eligible),
                         determinedNonResExempt : getValue(bogfw.determinedNonResExempt),
                         determinedHomeless     : getValue(bogfw.determinedHomeless),
                         bEligible              : getBannerBEligible(bogfw.eligibility, misCode),
                         maritalStatus          : getMartlStatus(bogfw.maritalStatus),

                 ])
             }
         }
     } finally {
            sql?.close()
        }

    }

    def getBannerEligibility( def eligibility, String misCode) {
        switch(eligibility) {
            case com.ccctc.adaptor.model.BOGWaiver.Eligibility.METHOD_A:
                return MisEnvironment.getProperty(environment, misCode, "banner.bogfw.methodA")
            case com.ccctc.adaptor.model.BOGWaiver.Eligibility.METHOD_B:
                return MisEnvironment.getProperty(environment, misCode, "banner.bogfw.methodB")
            case com.ccctc.adaptor.model.BOGWaiver.Eligibility.METHOD_D:
                return MisEnvironment.getProperty(environment, misCode, "banner.bogfw.methodD")
            case com.ccctc.adaptor.model.BOGWaiver.Eligibility.METHOD_A_AND_B:
                return MisEnvironment.getProperty(environment, misCode, "banner.bogfw.methodAB")
            default:
                return MisEnvironment.getProperty(environment, misCode, "banner.bogfw.notEligible")
        }
        return null
    }

    def getBannerBEligible( def eligibility, String misCode) {
        if( eligibility == com.ccctc.adaptor.model.BOGWaiver.Eligibility.METHOD_B ||
                eligibility == com.ccctc.adaptor.model.BOGWaiver.Eligibility.METHOD_A_AND_B ) {
            return 'Y'
        }
        return 'N'
    }

    def getValue( def checkVal) {
        switch (checkVal) {
            case true:
                return 'Y'
            case false:
                return 'N'
            default: return 'N'
        }
    }


    def getDepStatus(def dependencyStatus){
        switch (dependencyStatus) {
            case com.ccctc.adaptor.model.BOGWaiver.DependencyStatus.DEPENDENT:
                return "D"
            default:
                return "I"
        }
    }

    def getDepParentStatus(def dependentParentTaxes){

        switch (dependentParentTaxes) {
           case com.ccctc.adaptor.model.BOGWaiver.DependentOnParentTaxesEnum.NO:
               return "N"
           case com.ccctc.adaptor.model.BOGWaiver.DependentOnParentTaxesEnum.YES:
               return "Y"
           case com.ccctc.adaptor.model.BOGWaiver.DependentOnParentTaxesEnum.PARENTS_NOT_FILE:
               return "W"
        }
        return null
    }

    def getMartlStatus(MaritalStatus maritalStatus){


        switch ( maritalStatus ){
            case MaritalStatus.SINGLE:
                return "S"

            case MaritalStatus.MARRIED:
                return "M"

            case MaritalStatus.DIVORCED:
                return "D"

            case MaritalStatus.WIDOWED:
                return "W"

            case MaritalStatus.SEPARATED:
                return "X"


        }
        return null
    }

    def validateTerm(String sisTermId, String misCode, Sql sql) {
        def query = MisEnvironment.getProperty(environment, misCode, "banner.term.getQuery")
        return sql.firstRow(query, [sisTermId: sisTermId])
    }

}