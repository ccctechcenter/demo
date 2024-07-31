package api.colleague.model

import groovy.transform.CompileStatic

/**
 * Result from a data write Colleague Transaction
 */
@CompileStatic
class DataWriteResult {
    String id
    boolean errorOccurred
    List<String> errorMessages

    DataWriteResult(String id, boolean errorOccurred, List<String> errorMessages) {
        this.id = id
        this.errorOccurred = errorOccurred
        this.errorMessages = errorMessages
    }
}
