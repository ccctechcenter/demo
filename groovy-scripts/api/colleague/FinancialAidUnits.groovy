package api.colleague

import com.ccctc.adaptor.exception.InternalServerException
import com.ccctc.adaptor.model.FinancialAidUnits as CAFinancialAidUnits

class FinancialAidUnits {

    /**
     * Get additional Financial Aid Units for a student for a term
     *
     * @param misCode MIS Code
     * @param cccid CCC ID
     * @param sisTermId SIS Term ID
     * @return List of FA Units
     */
    List<CAFinancialAidUnits> getUnitsList(String misCode, String cccid, String sisTermId) {
        assert misCode != null
        assert cccid != null
        assert sisTermId != null

        throw new InternalServerException("Unsupported")
    }

    /**
     * Add additional Financial Aid Units to a student
     *
     * @param misCode MIS Code
     * @param faUnits FA Units to add
     * @return FA Units added
     */
    CAFinancialAidUnits post(String misCode, CAFinancialAidUnits faUnits) {
        assert misCode != null
        assert faUnits != null

        throw new InternalServerException("Unsupported")

    }

    /**
     * Remove additional Financial Aid Units from a student
     *
     * @param misCode MIS Code
     * @param cccid CCC ID
     * @param sisTermId SIS Term ID
     * @param enrolledMisCode Enrolled MIS Code
     * @param cid C-ID
     */
    void delete(String misCode, String cccid, String sisTermId, String enrolledMisCode, String cid) {
        assert misCode != null
        assert cccid != null
        assert sisTermId != null
        assert enrolledMisCode != null
        assert cid != null

        throw new InternalServerException("Unsupported")

    }
}