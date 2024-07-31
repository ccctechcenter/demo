package api.colleague.util

import api.colleague.collection.TupleIterable
import api.colleague.collection.TupleIterator
import api.colleague.model.RuleResult
import com.ccctc.adaptor.exception.InternalServerException
import com.ccctc.adaptor.exception.InvalidRequestException
import org.ccctc.colleaguedmiclient.model.CTXData
import org.ccctc.colleaguedmiclient.model.ColleagueData
import org.ccctc.colleaguedmiclient.model.KeyValuePair
import org.ccctc.colleaguedmiclient.service.DmiCTXService
import org.ccctc.colleaguedmiclient.service.DmiDataService
import org.ccctc.colleaguedmiclient.service.DmiService
import org.ccctc.colleaguedmiclient.transaction.data.ViewType
import org.ccctc.colleaguedmiclient.util.StringUtils
import org.springframework.cache.Cache
import org.springframework.core.env.Environment

/**
 * Commonly used data utilities
 */
class DataUtils {

    // batch size for various operations
    private static final BATCH_SIZE = 250


    String misCode
    Environment environment
    DmiService dmiService
    DmiCTXService dmiCTXService
    DmiDataService dmiDataService
    Cache cache

    static altIdColumns = ["PERSON.ALT.IDS", "PERSON.ALT.ID.TYPES"]



    //
    // configuration values
    //
    List<String> cccIdTypes


    DataUtils(String misCode, Environment environment, DmiService dmiService, DmiCTXService dmiCTXService,
              DmiDataService dmiDataService, Cache cache) {
        this.misCode = misCode
        this.environment = environment
        this.dmiService = dmiService
        this.dmiCTXService = dmiCTXService
        this.dmiDataService = dmiDataService
        this.cache = cache

        this.cccIdTypes = ColleagueUtils.getColleaguePropertyAsList(environment, misCode, "ccc.id.types")
        if(this.cccIdTypes == null || this.cccIdTypes.size() <= 0) {
            this.cccIdTypes = ColleagueUtils.getColleaguePropertyAsList(environment, misCode, "identity.cccId.alt_id_type")
        }
    }

    /**
     * Determine whether an sisTermId is valid or not. Uses caching for performance.
     *
     * @param sisTermId SIS Term ID
     * @return True if sisTermId is valid
     */
    boolean validateTerm(String sisTermId) {
        // validate term
        def key = "validateTerm:" + sisTermId
        def term = cache.get(key)
        if (term == null) {
            def terms = dmiDataService.selectKeys("TERMS", null, [sisTermId])
            if (!terms) return false

            cache.put(key, "X")
        }

        return true
    }

    /**
     * Determine whether a Section (identified by sisTermId and sisSectionId) is valid or not. Uses caching for performance.
     *
     * @param sisTermId    SIS Term ID
     * @param sisSectionId SIS Section ID
     * @return True if sisTermId is valid
     */
    boolean validateSection(String sisTermId, String sisSectionId) {
        // validate term
        def key = "validateSection:" + sisTermId + "_" + sisSectionId
        def section = cache.get(key)
        if (section == null) {
            def query = "SEC.TERM = " + ColleagueUtils.quoteString(sisTermId) +
                    " AND SEC.SYNONYM = " + ColleagueUtils.quoteString(sisSectionId)

            def sections = dmiDataService.selectKeys("COURSE.SECTIONS", query)
            if (!sections) return false

            cache.put(key, "X")
        }

        return true
    }


    /**
     * Get the CCC IDs for a person - these will be the PERSON.ALT.IDS with a PERSON.ALT.ID.TYPES that matches one of the
     * cccIdTypes.
     *
     * If no matching CCC IDs are found, no CCC IDs are passed in, or no CCC ID types are specified, null is returned
     *
     * @param personAltIds     Person Alt IDS
     * @param personAltIdTypes Person Alt ID Types
     * @return List of CCC IDs
     */
    List<String> getCccIds(String[] personAltIds, String[] personAltIdTypes) {
        if (!personAltIds || !cccIdTypes) return null

        def tuple = new TupleIterable(personAltIds, personAltIdTypes)
        def cccIds = tuple
                .findAll { i -> cccIdTypes.contains(i[1]) }
                .collect { i -> (String) i[0] }

        return cccIds ?: null
    }


    /**
     * Get CCC IDs for a list of PERSON IDs. First matching CCC ID for the person will be used.
     *
     * personData must include the columns PERSON.ALT.IDS and PERSON.ALT.ID.TYPES
     *
     * If no CCC ID is found for the person, the result will still include a map entry but with a null value for CCC ID
     *
     * @param personData List of ColleagueData for PERSON that includes PERSON.ALT.IDS and PERSON.ALT.ID.TYPES
     * @return Map of student ID to CCC ID
     */
    Map<String, String> getCccIds(List<ColleagueData> personData) {
        Map<String, String> result = [:]

        if (personData) {
            for (def p : personData) {
                def ids = (String[]) p.values["PERSON.ALT.IDS"]
                def types = (String[]) p.values["PERSON.ALT.ID.TYPES"]
                def cccIds = getCccIds(ids, types)
                result.put(p.key, cccIds?.first())
            }
        }

        return result
    }

    Map<String, String> getCccIdsByColleagueIds(List<String> colleagueIds) {
        if (colleagueIds && cccIdTypes) {
            List<ColleagueData> persons = dmiDataService.batchKeys("CORE", "PERSON", altIdColumns, colleagueIds)
            if (persons && persons.size() > 0) {
                return getCccIds(persons)
            }
        }
        return new HashMap<String, String>()
    }

    /**
     * Get Colleague IDs for a list of CCC IDs. In the event of a large list, selection is done in batches to avoid
     * overloading the DMI.
     *
     * @param cccIds CCC IDs
     * @return List of Colleague IDs
     */
    List<String> getColleagueIds(List<String> cccIds) {
        if (!cccIds || !cccIdTypes) return []

        def partitions = cccIds.collate(BATCH_SIZE)

        List<List<String>> lists = partitions.collect { List<String> k ->
            List<String> personIds = []
            def set = k.unique().toSet()
            def criteria =  "PERSON.ALT.IDS = " + (k.collect { ColleagueUtils.quoteString(it) }).join("")
            def persons = dmiDataService.batchSelect("CORE", "PERSON", altIdColumns, criteria)

            // make sure the matching alt IDs we got are from a CCC ID and not some other alt ID
            for (def p : persons) {
                def indexes = ((String[])p.values["PERSON.ALT.ID.TYPES"])?.findIndexValues { cccIdTypes.contains(it) } ?: []
                for (def i : indexes) {
                    def id = ColleagueUtils.getAt((String[]) p.values["PERSON.ALT.IDS"], i.intValue())
                    if (set.contains(id)) {
                        personIds << p.key
                        break
                    }
                }
            }

            return personIds
        }

        return lists.flatten().unique()
    }

    /**
     * Get Colleague ID(s) for a CCC ID
     *
     * @param cccId CCC ID
     * @return List of Colleague IDs
     */
    List<String> getColleagueIds(String cccId) {
        def result = []

        if (cccId && cccIdTypes) {
            def criteria = "PERSON.ALT.IDS = " + ColleagueUtils.quoteString(cccId)
            def persons = dmiDataService.batchSelect("CORE", "PERSON", altIdColumns, criteria)

            // make sure the matching alt IDs we got are from a CCC ID and not some other alt ID
            for (def p : persons) {
                if (p.values["PERSON.ALT.IDS"]) {
                    def tuple = new TupleIterable((String[]) p.values["PERSON.ALT.IDS"], (String[]) p.values["PERSON.ALT.ID.TYPES"])
                    for (def t : tuple) {
                        def id = t[0]
                        def type = t[1]

                        if (id == cccId && type && cccIdTypes.contains(type)) {
                            result << p.key
                            break
                        }
                    }
                }
            }
        }

        return result
    }


    /**
     * Get pointers from a list of records. Single-valued and multi-valued pointers are handled appropriately. Nulls
     * are eliminated and duplicate values are removed.
     */
    static List<String> getPointers(Iterable<ColleagueData> colleagueData, String fieldName) {
        if (!colleagueData) return []

        def result = []

        for (def d : colleagueData) {
            def p = d.values[fieldName]
            if (p) {
                if (p instanceof List || p instanceof String[])
                    result.addAll(p.findAll { it })
                else
                    result.add(p)
            }
        }

        return result.unique()
    }

    /**
     * Execute Colleague rule(s) by way of the Colleague Transaction X.CCCTC.EVAL.RULE.ST
     *
     * @param rules              Rules to execute
     * @param ids                IDs to run through rules
     * @param andOrFlag          AND or OR ? (optional - defaults to AND)
     * @param fileSuite          File Suite (optional)
     * @param createDummyRecord  Create dummy record ? (optional)
     * @param dummyTable         Dummy table (optional)
     * @return Rule result
     */
    RuleResult executeRule(List<String> rules, List<String> ids, String andOrFlag = null, String fileSuite = null,
                           boolean createDummyRecord = false, String dummyTable = null) {
        def params = []

        params << new KeyValuePair("Rules", rules.join(StringUtils.VM as String))
        params << new KeyValuePair("AndOrFlag", andOrFlag)
        params << new KeyValuePair("Ids", ids.join(StringUtils.VM as String))
        params << new KeyValuePair("FileSuite", fileSuite)
        params << new KeyValuePair("CreateDummyRec", createDummyRecord ? "1" : "0")
        params << new KeyValuePair("DummyTable", dummyTable)

        def result = dmiCTXService.execute("ST", "X.CCCTC.EVAL.RULE.ST", params)

        if (result.variables["ErrorOccurred"])
            throw new InternalServerException("Error processing rule: " + result.variables["ErrorMsg"])

        return new RuleResult((Boolean) result.variables["Result"], (String[]) result.variables["ValidIds"], (String[]) result.variables["InvalidIds"])
    }

    /**
     * Get value of DFLTS.HOST.CORP.ID, which is on PID2. This values is used in many places in the system to indicate
     * the Colleague ID associated with the college/district. Uses caching for performance.
     *
     * @return
     */
    String getDefaultHostCorpId() {
        final DFLTS_HOST_CORP_ID = "DFLTS.HOST.CORP.ID"

        def defaultHostCorpId = cache.get(DFLTS_HOST_CORP_ID, String.class)
        if (defaultHostCorpId == null) {
            def data = dmiDataService.singleKey("CORE", "CORE.PARMS", ViewType.PHYS,
                    [DFLTS_HOST_CORP_ID], "DEFAULTS", "DFLTS")

            defaultHostCorpId = data?.values[DFLTS_HOST_CORP_ID]
            cache.put(DFLTS_HOST_CORP_ID, defaultHostCorpId)
        }

        return defaultHostCorpId
    }


    public void validateExecution(CTXData data) {

        def errorOccurred = (Boolean) data.variables["ErrorOccurred"]
        def errorCodes = (String[]) data.variables["ErrorCodes"]
        def errorMsgs = (String[]) data.variables["ErrorMsgs"]

        // return different errors depending on the error codes returned.
        // only one error can be returned, so the lowest numbered error w/ messages is returned
        if (errorOccurred) {

            Integer minError = null
            String minErrorMessage = "Unknown error"

            if (errorCodes) {
                def tuple = new TupleIterable(errorCodes, errorMsgs)
                for (def t : tuple) {
                    try {
                        def err = Integer.parseInt((String) t[0])
                        if(minError == null || err <= minError) {
                            minError = err
                            minErrorMessage = t[1]
                        }
                    } catch (NumberFormatException ignored) {}
                }
            }

            switch (minError) {
                case "1": // missing term
                    throw new InvalidRequestException(minErrorMessage)
                case "2": // Invalid term
                    throw new InvalidRequestException(InvalidRequestException.Errors.termNotFound , minErrorMessage)
                case "3": // Student not found
                    throw new InvalidRequestException(InvalidRequestException.Errors.studentNotFound, minErrorMessage)
                default:
                    throw new InternalServerException(minErrorMessage)
            }
        }
    }
}
