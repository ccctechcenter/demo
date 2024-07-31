package api.colleague.transcript.model

import api.colleague.util.DataUtils
import api.colleague.util.DmiDataServiceCached
import org.ccctc.colleaguedmiclient.service.DmiEntityService
import org.ccctc.colleaguedmiclient.service.DmiCTXService
import org.ccctc.colleaguedmiclient.service.DmiDataService
import org.ccctc.colleaguedmiclient.service.DmiService
import org.springframework.cache.Cache
import org.springframework.core.env.Environment

/**
 * Wrapper for all the services used in transcripts to easily pass them between the various service classes
 */
class TranscriptDmiServices {

    String misCode
    DmiService dmiService
    DmiDataService dmiDataService
    DmiCTXService dmiCTXService
    DmiDataServiceCached dmiDataServiceCached
    DmiEntityService dmiEntityService
    DataUtils dataUtils
    Environment environment
    Cache cache

    TranscriptDmiServices(String misCode, DmiService dmiService, DmiDataService dmiDataService, DmiCTXService dmiCTXService,
                          DmiDataServiceCached dmiDataServiceCached, DmiEntityService dmiEntityService, DataUtils dataUtils,
                          Environment environment, Cache cache) {
        this.misCode = misCode
        this.dmiService = dmiService
        this.dmiDataService = dmiDataService
        this.dmiCTXService = dmiCTXService
        this.dmiDataServiceCached = dmiDataServiceCached
        this.dmiEntityService = dmiEntityService
        this.dataUtils = dataUtils
        this.environment = environment
        this.cache = cache
    }
}
