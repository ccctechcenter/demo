package api.colleague

import com.ccctc.adaptor.exception.InternalServerException
import com.ccctc.adaptor.exception.InvalidRequestException
import com.ccctc.adaptor.model.BOGWaiver as CABOGWaiver

class BOGWaiver  {

    /**
     * GET BOG Fee Waiver application data for a student
     *
     * @param misCode MIS Code
     * @param cccid CCC ID
     * @param sisTermId SIS Term ID
     * @return BOG Fee Waiver Data
     */
    CABOGWaiver get(String misCode, String cccid, String sisTermId) {
        assert misCode != null
        assert cccid != null
        assert sisTermId != null

        throw new InternalServerException("Unsupported")
    }

    /**
     * POST BOG Fee Waiver application data to a student
     *
     * @param misCode MIS Code
     * @param bogWaiver BOG Fee Waiver Data
     * @return BOG Fee Waiver Data
     */
    CABOGWaiver post(String misCode, CABOGWaiver bogWaiver) {
        assert misCode != null
        assert bogWaiver != null

        if (!bogWaiver.sisTermId)
            throw new InvalidRequestException(InvalidRequestException.Errors.invalidSearchCriteria, "sisTermId cannot be null or blank")
        if (!bogWaiver.cccid)
            throw new InvalidRequestException(InvalidRequestException.Errors.invalidSearchCriteria, "cccid cannot be null or blank")

        throw new InternalServerException("Unsupported")
    }
}