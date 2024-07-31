package api.mock

import com.ccctc.adaptor.exception.InvalidRequestException
import com.ccctc.adaptor.model.mock.FinancialAidUnitsDB
import org.slf4j.Logger
import org.slf4j.LoggerFactory
import org.springframework.core.env.Environment

class FinancialAidUnits {
    private final static Logger log = LoggerFactory.getLogger(FinancialAidUnits.class)

    Environment environment

    // injected using GroovyServiceImpl on creation of class
    private FinancialAidUnitsDB financialAidUnitsDB

    def getUnitsList(String misCode, String cccid, String sisTermId) {
        log.debug("getUnitsList(" + misCode + ", " + cccid + "," + sisTermId + ")")

        return financialAidUnitsDB.findSorted([misCode: misCode, cccid: cccid, sisTermId: sisTermId])
    }

    def post(String misCode, com.ccctc.adaptor.model.FinancialAidUnits financialAidUnits) {
        log.debug("post (" + misCode + ", " + financialAidUnits + ")")

        if (financialAidUnits != null) {
            if (!financialAidUnits.misCode)
                financialAidUnits.misCode = misCode
            else if (financialAidUnits.misCode != misCode)
                throw new InvalidRequestException(null, "misCode does match financialAidUnits")
        }

        // throws 400 if student or term not found
        // throws 409 if record already exists
        financialAidUnitsDB.add(financialAidUnits)
    }

    def delete(String misCode, String cccid, String sisTermId, String enrolledMisCode, String cid) {
        log.debug("delete (" + misCode + ", " + cccid + ", " + sisTermId + "," + enrolledMisCode + ", " + cid + ")")

        // throws 400 if student or term not found
        // throws 404 if not found
        return financialAidUnitsDB.delete(misCode, sisTermId, "cccid:$cccid", enrolledMisCode, cid, false)
    }
}