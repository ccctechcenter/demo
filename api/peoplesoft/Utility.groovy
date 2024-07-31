package api.peoplesoft

import org.apache.commons.lang.StringUtils
import org.slf4j.Logger
import org.slf4j.LoggerFactory
import org.springframework.boot.actuate.health.Status
import org.springframework.core.env.Environment

import psft.pt8.joa.*
import PeopleSoft.Generated.CompIntfc.*

/**
 * <h1>Utility Health Interface</h1>
 * <summary>
 *     <p>Provides a Wrapper around:</p>
 *     <ol>
 *         <li>SIS Health Status</li>
 *         <li>SIS Version data</li>
 *     <ol>
 * </summary>
 *
 * @version 3.3.0
 *
 */
class Utility {

    protected final static Logger log = LoggerFactory.getLogger(Utility.class)

    //****** Populated by injection in Groovy Service Implementation ****
    Environment environment

    /**
     * Provides the PeopleTools version supporting the sis Back End
     * @return Mapping to the version
     * @Note utilized by the /info endpoint
     */
    def getSisVersion() {
        log.debug("getSisVersion: attempting to determine peopletools version, first from PSConnection class...")
        String version = api.peoplesoft.PSConnection.getToolsRelease()
        if(StringUtils.isEmpty(version)) {
            try {
                log.debug("getSisVersion: could not get version from PSConnection. Attempting to determine version from filename...")
                String vpath = environment.getProperty("adaptor.classpath")
                Integer fr = vpath.indexOf("psjoa_") + 6
                Integer to = -1
                if (fr >= 6) {
                    to = vpath.indexOf(".jar", fr)
                }
                if (to > fr) {
                    version = vpath.substring(fr, to)
                }

                if(StringUtils.isEmpty(version)){
                    log.warn("getSisVersion: Could not determine version from filename either.")
                } else {
                    log.debug("getSisVersion: version determined from filename as [" + version + "]")
                }
            } catch (e) {
                log.warn("getSisVersion: Could not determine version from filename - " + e.getMessage())
            }
        } else  {
            log.debug("getSisVersion: version determined from PSConnection as [" + version + "]")
        }

        return ["PeopleTools Version": version]
    }

    /**
     * Tests a Connection to the back end SIS to determine a health status.
     * Does not submit any queries
     * @return UP or DOWN depending on if connection to SIS is successful
     * @Note utilized by the /health endpoint
     */
    Status getSisConnectionStatus() {
        log.debug("getSisConnectionStatus: checking connection status ")

        try {
            def peoplesoftSession = api.peoplesoft.PSConnection.getSession(environment, "")
            if (peoplesoftSession != null) {
                peoplesoftSession.disconnect()
            }
            return Status.UP

        }
        catch (Exception e)
        {
            log.error("getSisConnectionStatus: connection failed: [" + e.getMessage() + "]")
            return Status.DOWN
        }
        return Status.DOWN
    }
}