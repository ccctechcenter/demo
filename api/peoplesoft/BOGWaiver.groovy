package api.peoplesoft

import org.springframework.core.env.Environment

/**
 * <h1>BOG (Board of Governors) Fee Waiver (Deprecated)</h1>
 * <summary>Staged into PS, the bog fee waiver data from the CCPromise system</summary>
 * @Deprecated replaced by CCPromiseGrant class per updated specs
 */
@Deprecated
class BOGWaiver {

    //****** Populated by injection in Groovy Service Implementation ****
    Environment environment

    com.ccctc.adaptor.model.BOGWaiver get(String misCode, String cccid, String sisTermId) {
        throw new UnsupportedOperationException("Use CCPromiseGrant endpoint instead")
    }

    com.ccctc.adaptor.model.BOGWaiver post(String misCode, com.ccctc.adaptor.model.BOGWaiver bogWaiver) {
        throw new UnsupportedOperationException("Use CCPromiseGrant endpoint instead")
    }
}