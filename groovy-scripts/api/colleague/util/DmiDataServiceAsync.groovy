package api.colleague.util

import groovy.transform.CompileStatic
import org.ccctc.colleaguedmiclient.model.ColleagueData
import org.ccctc.colleaguedmiclient.model.ElfTranslateTable
import org.ccctc.colleaguedmiclient.model.Valcode
import org.ccctc.colleaguedmiclient.service.DmiDataService
import org.springframework.cache.Cache

import java.util.concurrent.CompletableFuture

/**
 * Asynchronous functions for the methods in DmiDataService. These use CompletableFuture.supplyAsync {} which makes use
 * of the common ForkJoinPool.
 */
@CompileStatic
class DmiDataServiceAsync {

    DmiDataService dmiDataService
    DmiDataServiceCached dmiDataServiceCached

    DmiDataServiceAsync(DmiDataService dmiDataService, DmiDataServiceCached dmiDataServiceCached) {
        this.dmiDataService = dmiDataService
        this.dmiDataServiceCached = dmiDataServiceCached
    }

    DmiDataServiceAsync(DmiDataService dmiDataService, Cache cache) {
        this.dmiDataService = dmiDataService
        this.dmiDataServiceCached = new DmiDataServiceCached(dmiDataService, cache)
    }

    CompletableFuture<List<ColleagueData>> batchSelectAsync(String appl, String viewName, Iterable<String> columns, String criteria) {
        return CompletableFuture.supplyAsync { dmiDataService.batchSelect(appl, viewName, columns, criteria) }
    }

    CompletableFuture<List<ColleagueData>> batchSelectAsyncCached(String appl, String viewName, Iterable<String> columns, String criteria) {
        return CompletableFuture.supplyAsync { dmiDataServiceCached.batchSelect(appl, viewName, columns, criteria) }
    }

    CompletableFuture<List<ColleagueData>> batchKeysAsync(String appl, String viewName, Iterable<String> columns, Iterable<String> keys) {
        return CompletableFuture.supplyAsync { dmiDataService.batchKeys(appl, viewName, columns, keys) }
    }


    CompletableFuture<ColleagueData> singleKeyAsync(String appl, String viewName, Iterable<String> columns, String key) {
        return CompletableFuture.supplyAsync { dmiDataService.singleKey(appl, viewName, columns, key) }
    }


    CompletableFuture<String[]> selectKeysAsync(String viewName, String criteria, Iterable<String> limitingKeys) {
        return CompletableFuture.supplyAsync { dmiDataService.selectKeys(viewName, criteria, limitingKeys) }
    }


    CompletableFuture<Valcode> valcodeAsyncCached(String appl, String key) {
        return CompletableFuture.supplyAsync { dmiDataServiceCached.valcode(appl, key) }
    }


    CompletableFuture<ElfTranslateTable> elfTranslationTableAsyncCached(String key) {
        return CompletableFuture.supplyAsync { dmiDataServiceCached.elfTranslationTable(key) }
    }


    CompletableFuture<List<ElfTranslateTable>> elfTranslationTablesAsyncCached(Iterable<String> keys) {
        return CompletableFuture.supplyAsync { dmiDataServiceCached.elfTranslationTables(keys) }
    }
}
