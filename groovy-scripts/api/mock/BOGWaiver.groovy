package api.mock

import com.ccctc.adaptor.exception.InvalidRequestException
import com.ccctc.adaptor.model.mock.BOGWaiverDB
import org.slf4j.Logger
import org.slf4j.LoggerFactory
import org.springframework.core.env.Environment

class BOGWaiver {
    private final static Logger log = LoggerFactory.getLogger(BOGWaiver.class)

    Environment environment

    // injected using GroovyServiceImpl on creation of class
    private BOGWaiverDB bogWaiverDB

    def get(String misCode, String cccid, String sisTermId) {
        log.debug("get ($misCode, $cccid, $sisTermId)")
        return bogWaiverDB.get(misCode, sisTermId, "cccid:$cccid")
    }

    def post(String misCode, com.ccctc.adaptor.model.BOGWaiver bogWaiver) {
        log.debug("post ($misCode, $bogWaiver)")

        if (bogWaiver.misCode && bogWaiver.misCode != misCode)
            throw new InvalidRequestException(InvalidRequestException.Errors.invalidRequest, "misCode in body does not match url")

        bogWaiver.misCode = misCode

        return bogWaiverDB.add(bogWaiver)
    }
}