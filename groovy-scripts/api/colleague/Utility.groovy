package api.colleague

import api.colleague.util.ColleagueUtils
import com.ccctc.adaptor.util.ClassMap
import groovy.transform.CompileStatic
import org.ccctc.colleaguedmiclient.service.DmiDataService
import org.ccctc.colleaguedmiclient.service.DmiService
import org.slf4j.Logger
import org.slf4j.LoggerFactory
import org.springframework.core.env.Environment
import org.springframework.boot.actuate.health.Status

@CompileStatic
class Utility {

    private static final Logger log = LoggerFactory.getLogger(Utility.class)

    DmiService dmiService
    DmiDataService dmiDataService
    Status status

    /**
     * Initialize
     */
    void colleagueInit(String misCode, Environment environment, ClassMap services) {
        this.dmiService = services.get(DmiService.class)
        this.dmiDataService = services.get(DmiDataService.class)
    }

    /**
     * Get version(s) of installed Envision code
     */
    Map getSisVersion() {
        ColleagueUtils.keepAlive(dmiService)

        Map result = [:]
        Object packages

        try {
            def data = dmiDataService.batchSelect("ST", "XCTC.PRODUCT.VERSIONS", ["XCTC.PRV.VERSION"], null)
            packages = data.collectEntries { [(it.key) : (String) it.values["XCTC.PRV.VERSION"]] }
        } catch (Exception e) {
            packages = "unknown - connection error: " + e.message
        }

        result << ["Installed Envision Packages": packages ?: null]

        return result
    }

    /**
     * Get SIS Connection Status.
     *
     * This forces a "keep alive" call to the DMI to see if it responds, reconnecting if necessary.
     *
     */
    Status getSisConnectionStatus() {
        try {
            ColleagueUtils.keepAlive(dmiService, true)
            return Status.UP
        } catch (Exception e) {
            log.error("Health check failed", e)
            return Status.DOWN
        }
    }
}