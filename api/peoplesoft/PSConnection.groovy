
package api.peoplesoft

import com.ccctc.adaptor.exception.InternalServerException
import com.ccctc.adaptor.util.MisEnvironment
import org.apache.commons.lang.StringUtils
import org.slf4j.Logger
import org.slf4j.LoggerFactory
import org.springframework.core.env.Environment

import psft.pt8.joa.API
import psft.pt8.joa.ISession
import PeopleSoft.Generated.CompIntfc.ICctcRpcCi
import psft.pt8.net.LoginInfo

/**
 * <h1>PeopleSoft Connection Utility</h1>
 * <summary>
 *     <p>Provides a Wrapper around the native peoplesoft java API for: </p>
 *     <ol>
 *         <li>Abstracting the Session/Connection settings to pull them from the MisEnvironment</li>
 *         <li>Abstracting the CCCTC Component Interface communication as a function to send a generic command
 *             to the CCCTC Peoplesoft API while returning the results.
 *         </li>
 *     <ol>
 *     <p>Most logic for College Adaptor's Peoplesoft support is found within each Peoplesoft instance itself as
 *        this renders the highest level of transparency to each college for code-reviews and deployment authorization.
 *        See App Designer/PeopleTools projects latest API info
 *     </p>
 * </summary>
 * <usage>
 *     def peopleSoftSession = PSConnection.getSession(environment, {CollegeMisCode})
 *     try {
 *         String[] results = PSConnection.callMethod(peopleSoftSession, "CCTC_PackageName", "CCTC_ClassName", "SomeMethod", "?", ["Arg1", "Arg2"])
 *     } finally {
 *         if(peopleSoftSession != null) {
 *             peopleSoftSession.disconnect()
 *         }
 *     }
 * </usage>
 *
 * @version CA4.6.0
 *
 */
class PSConnection {

    protected final static Logger log = LoggerFactory.getLogger(PSConnection.class)

    /**
     * Gets the Peopletools release version that the psjoa.jar file is using.
     * Based on the assumption that the LoginInfo class contains it as a hard-coded value
     * @return a string containing the release version or empty string if not found
     */
    static String getToolsRelease() {
        log.debug("getToolsRelease: Searching for tools release moniker")
        String release = ""
        try
        {
            // We dont actually need to login, but the LoginInfo class contains logic to either pull the appropriate tools version or use a hard-coded value
            def loginInfo = new LoginInfo("dummyServerUrl", "dummyOprId", "dummyOprPwd", (String)null, true)
            release = loginInfo.getToolsRel();
            log.debug("getToolsRelease: release moniker found as [" + release + "]")
        }
        catch (e) {
            log.warn("getToolsRelease: something went awry searching for the tools release: " + e.getMessage())
        }
        return release
    }

    /**
     * Gets a New Peoplesoft Connection Session.
     * Uses the environment/misCode to pull the appserver/username/password/country/domainPwd for the connection
     * if the appsever settings contains ',' commas then will try to connect to each server one at a time in order.
     * @param environment The object holding all property settings. Must contain appServer, sisuser, and sispassword
     * @param misCode The College code that *may* override the 'default' property setting to support
     *                {misCode}.appServer, {misCode}.sisuser, {misCode}.sispassword, etc.
     * @return a new and fully connected Peoplesoft connection/Session to be used in the callMethod function
     * @exception InternalServerException when a connection using the sisuser/sispassword/country/domainPwd to appServer fails
     */
    static ISession getSession(Environment environment, String misCode) {
        log.debug("getSession: Getting a new Session")

        String appserver = MisEnvironment.getProperty(environment, misCode,"peoplesoft.connection.appServers")
        if(StringUtils.isEmpty(appserver)) {
            appserver = MisEnvironment.getProperty(environment, misCode, "appserver")
        }
        String[] appservers = appserver.split(',')

        String sisUserId = MisEnvironment.getProperty(environment, misCode,"peoplesoft.connection.userId")
        if(StringUtils.isEmpty(sisUserId)) {
            sisUserId = MisEnvironment.getProperty(environment, misCode,"sisuser")
        }
        String sisPassword = MisEnvironment.getProperty(environment, misCode,"peoplesoft.connection.userPwd")
        if(StringUtils.isEmpty(sisPassword)) {
            sisPassword = MisEnvironment.getProperty(environment, misCode,"sispassword")
        }

        String domainPassword = MisEnvironment.getProperty(environment, misCode,"peoplesoft.connection.domainConnectionPwd")
        String countryCd = MisEnvironment.getProperty(environment, misCode,"peoplesoft.connection.countryCd")

        //****** Create PeopleSoft Session Object ******
        def oSession = API.createSession()
        log.debug("getSession: Created Peoplesoft session object")

        //****** Connect to the App Server ******
        boolean connected = false
        for (int i = 0; i < appservers.size(); i++) {
            if(StringUtils.isNotEmpty(appservers[i])) {
                if(StringUtils.isNotEmpty(domainPassword)) {
                    if(StringUtils.isNotEmpty(countryCd)) {
                        log.debug("getSession: attempting to connect to [" + appservers[i] + "] with a domainConnectionPwd and CountryCd")
                        connected = oSession.connectUsingCountryCdS(1, appservers[i], sisUserId, sisPassword, countryCd, null, domainPassword)
                    } else {
                        log.debug("getSession: attempting to connect to [" + appservers[i] + "] with a domainConnectionPwd (without CountryCd)")
                        connected = oSession.connectS(1, appservers[i], sisUserId, sisPassword, null, domainPassword)
                    }
                } else {
                    if(StringUtils.isNotEmpty(countryCd)) {
                        log.debug("getSession: attempting to connect to [" + appservers[i] + "] using CountryCd (without domainConnectionPwd)")
                        connected = oSession.connectUsingCountryCd(1, appservers[i], sisUserId, sisPassword, countryCd, null)
                    } else {
                        log.debug("getSession: attempting to connect to [" + appservers[i] + "] (without domainConnectionPwd nor CountryCd)")
                        connected = oSession.connect(1, appservers[i], sisUserId, sisPassword, null)
                    }
                }
                if (connected) {
                    log.debug("getSession: successfully connected to [" + appservers[i] + "]")
                    break
                } else {
                    log.debug("getSession: unable to connect to [" + appservers[i] + "]")
                }
            }
        }

        // clearing credentials out of memory (for security reasons)
        sisUserId = ""
        sisPassword = ""
        domainPassword = ""


        if (!connected) {
            String[] errorsAsArray = this.getErrorMessages(oSession)
            log.error("Error Connecting: " + errorsAsArray)
            String msg = "Unable to Connect to any of the Application Servers in [" + appserver + "]"
            throw new InternalServerException(msg)
        }

        log.debug("getSession: returning connected session")

        return oSession
    }

    /**
     * Given an open Peoplesoft session, will call (via the custom component interface) the method specified
     * with the given arguments within PeopleCode.
     * NOTE: Executes with Interactive Mode and Edit History Items both turned off (set to false)
     * @param oSession an Open Peoplesoft session (typically from the getSession function)
     * @param packageName the CCTC application package within Peoplesoft containing the class/method to execute
     * @param className the CCTC class within Peoplesoft that contains the method to execute
     * @param methodName the CCTC method within Peoplesoft to execute
     * @param dataDelimiter a safe character for this query to combine/split arguments and results with
     * @param methodArguments the arguments to provide to the CCTC method within Peoplesoft to execute against
     * @return straight pass-through result of what the Peoplesoft method returned; typically each field in a single row/record
     * @comment I once tried to refactor the Component Interface to use custom methods so it really would be functional
     *          programming but ran into a problem; Peoplesoft only allows SQLExec calls (how most of our db interaction
     *          works) from certain "peoplesoft program types" e.g. only during certain events. so while a custom method
     *          is possible, all its db interactions must be through ps objects such as records and the component processor.
     *          This limits us too much, so instead we 'execute' during a fieldChange (field is "Exec" in this case) so
     *          that we have a bit more flexibility/power to make quick db calls.
     * @wishfulThinking It'll be nice when PS CI support arrays across java/c++ calls. if they did, we wouldn't have to do
     *                  so much string parsing with delimiters.
     */
    static String[] callMethod(ISession oSession, String packageName, String className, String methodName, String dataDelimiter, String[] methodArguments) {
        log.debug("callMethod: Calling a Method [" + packageName + ":" + className + ":" + methodName + "]")

        //****** Validate parameters ******
        if (oSession == null) {
            throw new InternalServerException("Peoplesoft Session Object cannot be null")
        }
        if (methodName == null || methodName.length() <= 0) {
            throw new InternalServerException("callMethod methodName cannot be null nor blank")
        }

        log.debug("callMethod: getting the Adapter Component Interface")
        //****** Get Component Interface ******
        String ciName = "CCTC_RPC_CI"
        def oCctcRpcCi = (ICctcRpcCi) oSession.getCompIntfc(ciName)
        if (oCctcRpcCi == null) {
            throw new InternalServerException("Unable to Get Component Interface " + ciName)
        }

        log.debug("callMethod: got the Adapter Component Interface - setting the CI mode")
        //****** Set the Component Interface Mode ******
        oCctcRpcCi.setInteractiveMode(false)
        oCctcRpcCi.setEditHistoryItems(false)

        //****** Get an instance of the Component (using the component interface) at the specified record ******
        log.debug("callMethod: gonna get the instance of the component to load the derived record")
        if (!oCctcRpcCi.get()) {
            throw new InternalServerException("get method on Component Interface [" + ciName + "] failed.")
        }

        log.debug("callMethod: setting package")
        //****** Note that if the packageName does not start with CCTC_, our peoplesoft code will prepend it ******
        //****** This is meant as a security measure so we don't go executing just any ol' method ******
        oCctcRpcCi.setCctcRpcPackage(packageName)

        log.debug("callMethod: setting class")
        //****** Note that if the className does not start with CCTC_, our peoplesoft code will prepend it ******
        //****** This is meant as a security measure so we don't go executing just any ol' method ******
        oCctcRpcCi.setCctcRpcClass(className)

        log.debug("callMethod: setting method name")
        oCctcRpcCi.setCctcRpcMethod(methodName)

        log.debug("callMethod: setting data delimiter")
        //****** the args passed to the CI will be in form of a string with args separated by this delimiter ******
        //****** also; data returned will be in form of a string with fields separated by this delimiter ******
        oCctcRpcCi.setCctcRpcDatadelim(dataDelimiter)

        log.debug("callMethod: building the args")
        //****** Build args as '?' (assuming dataDelimiter is '?') separated string in form of: ******
        //****** arg1?arg2?arg3....?argN ******
        String args = ""
        if(methodArguments != null && methodArguments.length > 0) {
            methodArguments.each {
                args += dataDelimiter + (it ? it : " ")
            }
            args = args.substring(1)
        }
        oCctcRpcCi.setCctcRpcArgs(args)

        log.debug("callMethod: executing method")
        //****** There exists a PeopleCode trigger to Parse/Execute the specified Package:Class:method when Exec field is changed ******
        //****** results are stored as string variable (columns separated by dataDelimiter field) into Results field ******
        //****** We just need to change the variable for the trigger to execute, doesn't matter what it is set to so 'x' is good as any ******
        oCctcRpcCi.setCctcRpcExec("X")

        log.debug("callMethod: getting result")
        String result = oCctcRpcCi.getCctcRpcResult()

        log.debug("callMethod: method executed, parsing result")

        String[] results = []
        if (result) {
            results = result.split(dataDelimiter)

            // result.split function has a bug!
            // when we have the string "12<34<<56<89<<"
            // split should give us: [12, 34, , 56, 89, , ]
            // but instead, erroneously, gives us: [12, 34, , 56, 89 ]
            // which is missing the last two elements (that are null)
            // so we must adjust/compensate for the bug. this code below
            // wont hurt anything once bug is resolved.
            Integer dataDelimCount = result.count(dataDelimiter)
            while (results.length <= dataDelimCount) {
                results += ""
            }
        }

        log.debug("callMethod: returning results")

        return results
    }

    /**
     * Thin convenience method around the callMethod function to abstract away the names of the trigger package and class
     * @param oSession an Open Peoplesoft session (typically from the getSession function)
     * @param eventName the CCTC Data Change Event within Peoplesoft to execute
     * @param dataDelimiter a safe character for this query to combine/split arguments and results with
     * @param eventArguments the arguments to provide to the callMethod function as methodArguments
     * @return straight pass-through result of what the Peoplesoft method returned; typically each field in a single row/record
     */
    static String[] triggerEvent(ISession oSession, String eventName, String dataDelimiter, String[] eventArguments)
    {
        return callMethod(oSession, "CCTC_COLLEGE_PKG", "CCTC_CollegeEventHandlers", eventName, dataDelimiter, eventArguments)
    }

    /**
     * Given a Peoplesoft session, will get any/all pending error messages in the session
     * @param oSession a Peoplesoft session
     * @return string array of pending error message
     */
    static String[] getErrorMessages(ISession oSession){
        String[] messages = []
        if (oSession != null) {
            if (oSession.getErrorPending() || oSession.getWarningPending()) {
                def oPSMessageCollection = oSession.getPSMessages()
                if(oPSMessageCollection != null) {
                    for (int i = 0; i < oPSMessageCollection.getCount(); i++) {
                        def oPSMessage = oPSMessageCollection.item(i)
                        if (oPSMessage != null) {
                            messages += oPSMessage.getText()
                        }
                    }
                    oPSMessageCollection.deleteAll()
                }
            }
        }
        return messages
    }
}