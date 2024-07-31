package api.colleague.model

import groovy.transform.CompileStatic

/**
 * CAST.MIS.PARMS data from the CMRP form. Used to determine whether this SIS is multi-college and what locations
 * match to which MIS codes.
 */
@CompileStatic
class CastMisParms {
    boolean multiCollege
    String defaultInstitutionId
    String defaultMisCode
    List<InstitutionLocationMap> institutionLocationMapList

    List<String> getMisCodes() {
        return institutionLocationMapList?.collect { it.misCode }?.unique()
    }
}