package api.colleague.util

import org.ccctc.colleaguedmiclient.model.ColleagueData
import org.ccctc.colleaguedmiclient.model.ElfTranslateTable
import org.ccctc.colleaguedmiclient.model.Valcode
import org.ccctc.colleaguedmiclient.service.DmiDataService
import org.springframework.cache.Cache

/**
 * Various DMI service commands with caching support
 */
class DmiDataServiceCached {

    DmiDataService dmiDataService
    Cache cache

    DmiDataServiceCached(DmiDataService dmiDataService, Cache cache) {
        this.dmiDataService = dmiDataService
        this.cache = cache
    }

    List<ColleagueData> batchSelect(String appl, String viewName, Iterable<String> columns, String criteria) {
        String cacheKey = "BS_" + appl + "_" + viewName + "_" + criteria
        List<ColleagueData> v = cache.get(cacheKey, List.class)

        if (v == null) {
            v = dmiDataService.batchSelect(appl, viewName, columns, criteria)
            cache.put(cacheKey, v)
        }

        return v
    }

    Valcode valcode(String appl, String key) {
        String cacheKey = "VC_" + appl + "_" + key
        Valcode v = cache.get(cacheKey, Valcode.class)

        if (v == null) {
            v = dmiDataService.valcode(appl, key)
            cache.put(cacheKey, v)
        }

        return v
    }

    ElfTranslateTable elfTranslationTable(String key) {
        String cacheKey = "ELF_" + key
        ElfTranslateTable e = cache.get(cacheKey, ElfTranslateTable.class)

        if (e == null) {
            e = dmiDataService.elfTranslationTable(key)
            cache.put(cacheKey, e)
        }

        return e
    }

    List<ElfTranslateTable> elfTranslationTables(Iterable<String> keys) {
        String cacheKey = "ELF_" + keys.toString()
        List<ElfTranslateTable> e = cache.get(cacheKey, List.class)

        if (e == null) {
            e = dmiDataService.elfTranslationTables(keys)
            cache.put(cacheKey, e)
        }

        return e
    }
}
