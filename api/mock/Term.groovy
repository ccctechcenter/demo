package api.mock

import com.ccctc.adaptor.model.mock.TermDB
import org.springframework.core.env.Environment

class Term {
    Environment environment
    // injected using GroovyServiceImpl on creation of class
    private TermDB termDB

    def get(String misCode, String sisTermId) {
        return termDB.get(misCode, sisTermId)
    }

    def getAll(String misCode) {
        return termDB.findSorted([misCode: misCode])
    }
}