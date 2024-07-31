package api.colleague.util

import groovyx.gpars.GParsPool
import org.ccctc.colleaguedmiclient.model.ColleagueData
import org.ccctc.colleaguedmiclient.model.ElfTranslateTable
import org.ccctc.colleaguedmiclient.model.Valcode
import org.ccctc.colleaguedmiclient.service.DmiDataService
import org.springframework.cache.concurrent.ConcurrentMapCache
import spock.lang.Specification

class DmiDataServiceAsyncSpec extends Specification {

    def dmiDataService = Mock(DmiDataService)
    def cache = new ConcurrentMapCache("cache")
    def dmiDataServiceAsync = new DmiDataServiceAsync(dmiDataService, cache)

    def "requests"() {
        when:
        GParsPool.withPool {
            dmiDataServiceAsync.batchSelectAsync(null, null, null, null).get()
            dmiDataServiceAsync.batchKeysAsync(null, null, null, null).get()
            dmiDataServiceAsync.singleKeyAsync(null, null, null, null).get()
        }

        then:
        1 * dmiDataService.batchSelect(*_)
        1 * dmiDataService.batchKeys(*_)
        1 * dmiDataService.singleKey(*_)

    }

    def "batch select caching"() {
        setup:
        List<ColleagueData> r1 = null
        List<ColleagueData> r2 = null

        when:
        GParsPool.withPool {
            r1 = dmiDataServiceAsync.batchSelectAsyncCached("ST", "TABLE", [], null).get()
            r2 = dmiDataServiceAsync.batchSelectAsyncCached("ST", "TABLE", [], null).get()
        }

        then:
        1 * dmiDataService.batchSelect(*_) >> [new ColleagueData("KEY", [:])]
        r1 == r2
    }

    def "valcode caching"() {
        setup:
        def vc = new Valcode("VAL.CODE", null)
        def r1, r2

        when:
        GParsPool.withPool {
            r1 = dmiDataServiceAsync.valcodeAsyncCached("ST", "VAL.CODE").get()
            r2 = dmiDataServiceAsync.valcodeAsyncCached("ST", "VAL.CODE").get()
        }

        then:
        1 * dmiDataService.valcode(*_) >> vc
        r1 == r2
    }

    def "elf translation caching"() {
        setup:
        def et = [new ElfTranslateTable("ELF.TRANS1", null, null, null, null, null),
                  new ElfTranslateTable("ELF.TRANS2", null, null, null, null, null)]
        def r1, r2

        when:
        GParsPool.withPool {
            r1 = dmiDataServiceAsync.elfTranslationTablesAsyncCached(["ELF.TRANS1", "ELF.TRANS2"]).get()
            r2 = dmiDataServiceAsync.elfTranslationTablesAsyncCached(["ELF.TRANS1", "ELF.TRANS2"]).get()
        }

        then:
        1 * dmiDataService.elfTranslationTables(*_) >> et
        r1 == r2
    }
}
