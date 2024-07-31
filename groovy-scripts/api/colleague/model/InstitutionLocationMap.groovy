package api.colleague.model

import groovy.transform.CompileStatic

/**
 * CAST.MIS.PARMS data from the CMRP form. Used to determine whether this SIS is multi-college and what locations
 * match to which MIS codes.
 */
@CompileStatic
class InstitutionLocationMap {
    String institution
    String location
    String misCode
}