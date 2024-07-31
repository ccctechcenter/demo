package api.colleague.util

import api.colleague.model.CastMisParms
import api.colleague.model.InstitutionLocationMap
import groovy.transform.CompileStatic
import org.ccctc.colleaguedmiclient.model.ColleagueData
import org.ccctc.colleaguedmiclient.service.DmiDataService
import org.ccctc.colleaguedmiclient.transaction.data.ViewType
import org.springframework.cache.Cache

import java.util.concurrent.CompletableFuture

/**
 * Utility class for working with CAST.MIS.PARMS data which is used for multi-college checking / filtering
 */
@CompileStatic
class CastMisParmsUtil {

    CastMisParms castMisParms
    DmiDataService dmiDataService
    Cache cache

    CastMisParmsUtil(DmiDataService dmiDataService, Cache cache, boolean forceRefresh) {
        this.dmiDataService = dmiDataService
        this.cache = cache

        refresh(forceRefresh)
    }

    /**
     * Create CastMisParmsUtil asynchronously
     */
    static CompletableFuture<CastMisParmsUtil> fromAsync(DmiDataService dmiDataService, Cache cache, boolean forceRefresh) {
        return CompletableFuture.supplyAsync { new CastMisParmsUtil(dmiDataService, cache, false) }
    }

    /**
     * Test whether any of the locations provided matches the MIS Code provided.
     *
     * @param misCode   MIS Code
     * @param locations Locations
     * @return true/false
     */
    boolean checkLocations(String misCode, List<String> locations) {
        if (castMisParms?.multiCollege) {
            for(def loc : castMisParms.institutionLocationMapList) {
                if (loc.misCode == misCode) {
                    if (locations.contains(loc.location)) return true
                }
            }

            return false
        }

        return true
    }

    /**
     * Test whether the MIS Code is valid for this district/college
     *
     * @param misCode MIS Code
     * @return true if the MIS Code is valid
     */
    boolean checkMisCode(String misCode) {
        return castMisParms?.getMisCodes()?.contains(misCode)
    }

    /**
     * Get the Institution ID (key to INSTITUTIONS table) associated with an MIS Code
     *
     * @param misCode MIS Code
     * @return Institution ID
     */
    String getInstititionId(String misCode) {
        return castMisParms?.institutionLocationMapList?.find { it.misCode == misCode }?.institution
    }

    /**
     * Test whether a location provided matches the MIS Code provided.
     *
     * @param misCode  MIS Code
     * @param location Location
     * @return true/false
     */
    boolean checkLocation(String misCode, String location) {
        if (castMisParms?.multiCollege) {
            for(def loc : castMisParms.institutionLocationMapList) {
                if (loc.misCode == misCode && location == loc.location) {
                    return true
                }
            }

            return false
        }

        return true
    }

    /**
     * Set / update CastMisParms data
     *
     * @param force Force refresh?
     */
    void refresh(boolean force) {
        // check cache
        if (!force) {
            def cached = cache.get("CastMisParms", CastMisParms.class)
            if (cached != null) {
                this.castMisParms = cached
                return
            }
        }

        def result = new CastMisParms()

        def data = dmiDataService.singleKey("ST", "ST.PARMS", ViewType.PHYS,
                ["CASTMP.INSTITUTION.MAP", "CASTMP.LOC.MAP", "CASTMP.DEFAULT.INST.ID"],
                "CAST.MIS.PARMS", "CAST.MIS.PARMS")

        if (data != null) {
            def locIds = (String[]) data.values["CASTMP.INSTITUTION.MAP"]
            List<ColleagueData> locData = null
            if (locIds)
                locData = dmiDataService.batchKeys("CORE", "INSTITUTIONS", ["INST.OTHER.ID"], Arrays.asList(locIds))

            def instMap = (String[]) data.values["CASTMP.INSTITUTION.MAP"]
            def locMap = (String[]) data.values["CASTMP.LOC.MAP"]

            result.defaultInstitutionId = data.values["CASTMP.DEFAULT.INST.ID"]
            result.multiCollege = (instMap && (instMap as List).unique(false).size() > 1)
            result.institutionLocationMapList = []

            if (instMap && locData) {
                for (int x = 0; x < instMap.size(); x++) {
                    def inst = locData.find { i -> i.key == instMap[x] }

                    result.institutionLocationMapList << new InstitutionLocationMap(
                            institution: instMap[x],
                            location: (locMap && locMap.size() > x) ? locMap[x] : null,
                            misCode: inst ? (String) inst.values["INST.OTHER.ID"] : null
                    )

                    if (instMap[x] == result.defaultInstitutionId)
                        result.defaultMisCode = inst ? inst.values["INST.OTHER.ID"] : null
                }
            }

            // cache value
            cache.put("CastMisParms", result)

            this.castMisParms = result
            return
        }

        this.castMisParms = null
    }
}
