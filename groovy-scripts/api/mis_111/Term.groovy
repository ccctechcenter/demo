package api.mis_111

import api.colleague.model.TermsRecord
import com.ccctc.adaptor.model.TermType
import groovy.transform.CompileStatic

@CompileStatic
class Term extends api.colleague.base.Term {

    //
    // Butte College - filter out "reporting terms". These terms have the letter "R" as the 3rd characters and are not
    // actual terms. For example, FAR2007 means Fall 2007 Reporting Term. The actual term for Fall 2007 is 2007FA.
    //

    @Override
    protected List<TermsRecord> filter(String misCode, List<String> sisTermIds, List<TermsRecord> data) {
        def result = super.filter(misCode, sisTermIds, data)

        if (result)
            result.removeIf { it.recordId.length() == 7 && it.recordId.charAt(2) == "R" as char }

        return result
    }

    //
    // Butte College - prior to 1985FA, terms were on the quarter system
    //
    @Override
    protected com.ccctc.adaptor.model.Term parse(String misCode, TermsRecord termData) {
        def data = super.parse(misCode, termData)

        if (data.sisTermId <= "1985XX" && data.sisTermId != "1985FA")
            data.type = TermType.Quarter

        return data
    }
}
