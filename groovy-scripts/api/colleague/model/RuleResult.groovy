package api.colleague.model

import groovy.transform.CompileStatic

/**
 * Result of running a rule through the Colleague Transaction X.CCCTC.EVAL.RULE.ST
 */
@CompileStatic
class RuleResult {

    /**
     * true if operation was successful
     */
    boolean result

    /**
     * valid IDs
     */
    String[] validIds

    /**
     * invalid IDs
     */
    String[] invalidIds

    RuleResult(boolean result, String[] validIds, String[] invalidIds) {
        this.result = result
        this.validIds = validIds
        this.invalidIds = invalidIds
    }
}
