package api.colleague.util

import api.colleague.model.DataWriteResult
import org.ccctc.colleaguedmiclient.model.KeyValuePair
import org.ccctc.colleaguedmiclient.service.DmiCTXService
import org.ccctc.colleaguedmiclient.util.StringUtils

class DataWriter {

    private DmiCTXService dmiCTXService

    DataWriter(DmiCTXService dmiCTXService) {
        this.dmiCTXService = dmiCTXService
    }

    /**
     * Write a record to Colleague using the Colleague Transaction X.CCCTC.WRITE.RECORD.
     *
     * @param table    Table
     * @param id       Key
     * @param autoKey  Generate key automatically?
     * @param data     Data
     * @return Result
     */
    DataWriteResult write(String table, String id, Boolean autoKey, Iterable<String> data) {
        def r = StringUtils.join(StringUtils.VM, data)
        def params = []
        params << new KeyValuePair("Table", table)
        params << new KeyValuePair("Id", id)
        params << new KeyValuePair("AutoKey", autoKey ? "Y" : "N")
        params << new KeyValuePair("Record", r)
        def result = dmiCTXService.execute("ST", "X.CCCTC.WRITE.RECORD", params)

        return new DataWriteResult((String) result.variables["OutId"], result.variables["ErrorOccurred"] as boolean,
                (List<String>) result.variables["ErrorMsgs"])

    }
}
