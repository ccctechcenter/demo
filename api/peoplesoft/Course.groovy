package api.peoplesoft

import com.ccctc.adaptor.exception.EntityNotFoundException
import com.ccctc.adaptor.exception.InternalServerException
import com.ccctc.adaptor.exception.InvalidRequestException
import com.ccctc.adaptor.model.Course
import com.ccctc.adaptor.util.MisEnvironment
import org.apache.commons.lang.StringUtils
import org.slf4j.Logger
import org.slf4j.LoggerFactory

import java.text.SimpleDateFormat
import com.ccctc.adaptor.model.CourseContact
import com.ccctc.adaptor.model.TransferStatus
import com.ccctc.adaptor.model.CreditStatus
import com.ccctc.adaptor.model.GradingMethod
import com.ccctc.adaptor.model.InstructionalMethod
import com.ccctc.adaptor.model.CourseStatus
import org.springframework.core.env.Environment

/**
 * <h1>Course Peoplesoft Interface</h1>
 * <summary>
 *     <p>Provides a Wrapper around the native peoplesoft java API for:</p>
 *     <ol>
 *         <li>retrieving course details from the built-in PS tables</li>
 *     <ol>
 *     <p>Most logic for College Adaptor's Peoplesoft support is found within each Peoplesoft instance itself as
 *        this renders the highest level of transparency to each college for code-reviews and deployment authorization.
 *        See App Designer/PeopleTools projects latest API info
 *     </p>
 * </summary>
 *
 * @version 3.3.0
 *
 */
class Course {

    protected final static Logger log = LoggerFactory.getLogger(Course.class)

    //****** Populated by injection in Groovy Service Implementation ****
    Environment environment

    /**
     * Default Constructor needed since we also specify an alternative Constructor
     */
    Course(){
        // do nothing, except be available
    }

    /**
     * Alternative Constructor to set the environment during initialization.
     * Useful for when other groovy PS APIs need to use these Functions that depend on environment
     */
    Course(Environment e) {
        this.environment = e
    }

    /**
     * Gets Course Data from the legit Peoplesoft table using the CCTC_Course:getCourse peoplesoft method
     * @param misCode The College Code used for grabbing college specific settings from the environment
     * @param sisCourseId The college adaptor Course Id of the course to retrieve details of; usually in form of SUBJ-NBR
     * @param sisTermId the Term in which to get details for the given course
     * @returns A single course matching the course and term provided
     */
    com.ccctc.adaptor.model.Course get(String misCode, String sisCourseId, String sisTermId) {
        log.debug("get: getting course data")

        //****** Validate parameters ****
        if (!misCode) {
            throw new InvalidRequestException(InvalidRequestException.Errors.invalidSearchCriteria, "misCode cannot be null or blank")
        }
        if (!sisCourseId) {
            throw new InvalidRequestException(InvalidRequestException.Errors.invalidSearchCriteria, "course cannot be null or blank")
        }
        if (!sisTermId) {
            throw new InvalidRequestException(InvalidRequestException.Errors.invalidSearchCriteria, "term cannot be null or blank")
        }

        log.debug("get: params ok; building method params")

        //****** Build parameters to send to our custom Peopletools API ****
        String packageName = "CCTC_COURSE_SECTION_PKG"
        String className = "CCTC_Course"
        String methodName = "getCourse"

        String[] args = [
                PSParameter.ConvertStringToCleanString(sisCourseId),
                PSParameter.ConvertStringToCleanString(sisTermId),
                PSParameter.ConvertStringToCleanString(MisEnvironment.getProperty(environment, misCode,"peoplesoft.global.institution")),
                PSParameter.ConvertStringToCleanString(MisEnvironment.getProperty(environment, misCode,"peoplesoft.global.acad_career")),
                PSParameter.ConvertStringToCleanString(MisEnvironment.getProperty(environment, misCode,"peoplesoft.course.transferStatus.crse_attr")),
                PSParameter.ConvertStringToCleanString(MisEnvironment.getProperty(environment, misCode,"peoplesoft.course.creditStatus.crse_attr"))
        ]

        String[] courseData = []

        log.debug("get: attempting to get a new peoplesoft Session")
        //****** Create Connection to Peoplesoft and call remote method ******
        def peoplesoftSession = api.peoplesoft.PSConnection.getSession(environment, misCode)
        try {
            log.debug("get: calling the remote peoplesoft method")

            courseData = api.peoplesoft.PSConnection.callMethod(peoplesoftSession, packageName, className, methodName, "<", args)
        }
        catch(psft.pt8.joa.JOAException e) {
            String messageToThrow = e.getMessage()
            if(peoplesoftSession != null) {
                String[] msgs = api.peoplesoft.PSConnection.getErrorMessages(peoplesoftSession)
                if(msgs && msgs.length > 0) {
                    messageToThrow = msgs[0]
                    msgs.each {
                        log.error("get: retrieval failed: [" + it + "]")
                    }
                }
            }
            throw new InternalServerException(InternalServerException.Errors.internalServerError, messageToThrow)
        }
        finally {
            log.debug("get: disconnecting")
            if(peoplesoftSession != null) {
                peoplesoftSession.disconnect()
            }
        }
        log.debug("get: making sure course was found")
        if(courseData == null || courseData.length < 23) {
            throw new EntityNotFoundException("Search returned no results.")
        }

        log.debug("get: parsing courseData")
        SimpleDateFormat df = new SimpleDateFormat("yyyy-MM-dd")

        Integer i = 0
        String intCourseId = courseData[i++]
        String crseOfferNbr = courseData[i++]
        String effdt = courseData[i++]
        String term = courseData[i++]
        String sessionCode = courseData[i++]
        String classNbr = courseData[i++]
        String courseStatus = courseData[i++]
        String crseDescr = courseData[i++]
        String feeExists = courseData[i++]
        String unitsMinimum = courseData[i++]
        String unitsMaximum = courseData[i++]
        String gradingBasis = courseData[i++]
        String crseTitle = courseData[i++]
        String courseSubject = courseData[i++]
        String catalogNbr = courseData[i++]
        String requirementGroup = courseData[i++]
        String descrLong = courseData[i++]
        String courseStartDate = courseData[i++]
        String courseEndDate = courseData[i++]
        String cid = courseData[i++]
        String misCourseId = courseData[i++]
        String transferStatus = courseData[i++]
        String creditStatus = courseData[i++]

        com.ccctc.adaptor.model.Course.Builder builder = new com.ccctc.adaptor.model.Course.Builder()
        builder.misCode(misCode)
               .sisCourseId(courseSubject + "-" + catalogNbr)
               .sisTermId(sisTermId)
               .c_id(cid.toString() ?: null)
               .controlNumber(misCourseId.toString() ?: null)
               .subject(courseSubject ?: null)
               .number(catalogNbr)
               .title(crseTitle.toString() ?: null)
               .description(crseDescr.toString() ?: null)
               .outline(descrLong.toString().trim() ?: null)
               .minimumUnits(unitsMinimum.toFloat() ?: null)
               .maximumUnits(unitsMaximum.toFloat() ?: null)
               .gradingMethod(this.parseGradeMethod(misCode, gradingBasis))
               .transferStatus(this.parseTransferStatus(misCode, transferStatus))
               .creditStatus(this.parseCreditStatus(misCode, creditStatus))
               .start((courseStartDate.toString() != "") ? df.parse(courseStartDate.toString()) : null)
               .end((courseEndDate.toString() != "") ? df.parse(courseEndDate.toString()) : null)
               .status(courseStatus as CourseStatus)

        Float fees = 0
        if(StringUtils.equalsIgnoreCase(feeExists, "Y")) {
            log.debug("get: retrieving course fee data")
            String[] feeData = []
            peoplesoftSession = api.peoplesoft.PSConnection.getSession(environment, misCode)
            try {
                methodName = "getCourseFees"
                args = [
                        PSParameter.ConvertStringToCleanString(intCourseId),
                        PSParameter.ConvertStringToCleanString(sisTermId),
                        PSParameter.ConvertStringToCleanString(sessionCode),
                        PSParameter.ConvertStringToCleanString(MisEnvironment.getProperty(environment, misCode,"peoplesoft.global.institution"))
                ]
                log.debug("get: calling the remote peoplesoft method")

                feeData = api.peoplesoft.PSConnection.callMethod(peoplesoftSession, packageName, className, methodName, "<", args)
            }
            catch(psft.pt8.joa.JOAException e) {
                String messageToThrow = e.getMessage()
                if(peoplesoftSession != null) {
                    String[] msgs = api.peoplesoft.PSConnection.getErrorMessages(peoplesoftSession)
                    if(msgs && msgs.length > 0) {
                        messageToThrow = msgs[0]
                        msgs.each {
                            log.error("get: fee retrieval failed: [" + it + "]")
                        }
                    }
                }
                throw new InternalServerException(InternalServerException.Errors.internalServerError, messageToThrow)
            }
            finally {
                log.debug("get: disconnecting")
                if(peoplesoftSession != null) {
                    peoplesoftSession.disconnect()
                }
            }

            if(feeData && feeData.length > 0) {
                String crseFees = feeData[0]
                if(crseFees) {
                    fees = crseFees.toFloat() ?: 0
                }
            }
        }
        builder.fee(fees)



        if(intCourseId && effdt) {
            log.debug("get: retrieving contact data")
            String[] contactData = []

            peoplesoftSession = api.peoplesoft.PSConnection.getSession(environment, misCode)
            try {
                methodName = "getCourseComponents"
                args = [
                        PSParameter.ConvertStringToCleanString(intCourseId),
                        PSParameter.ConvertStringToCleanString(effdt)
                ]
                log.debug("get: calling the remote peoplesoft method")

                contactData = api.peoplesoft.PSConnection.callMethod(peoplesoftSession, packageName, className, methodName, "<", args)
            }
            catch(psft.pt8.joa.JOAException e) {
                String messageToThrow = e.getMessage()
                if(peoplesoftSession != null) {
                    String[] msgs = api.peoplesoft.PSConnection.getErrorMessages(peoplesoftSession)
                    if(msgs && msgs.length > 0) {
                        messageToThrow = msgs[0]
                        msgs.each {
                            log.error("get: component retrieval failed: [" + it + "]")
                        }
                    }
                }
                throw new InternalServerException(InternalServerException.Errors.internalServerError, messageToThrow)
            }
            finally {
                log.debug("get: disconnecting")
                if(peoplesoftSession != null) {
                    peoplesoftSession.disconnect()
                }
            }

            if(contactData && contactData.length > 0) {
                i = 0
                List<CourseContact> contacts = []
                while(i + 2 <= contactData.length){
                    String ssr_component = contactData[i++]
                    String hours = contactData[i++]

                    InstructionalMethod method = this.parseInstructionalMethod(misCode, ssr_component)
                    CourseContact.Builder ccbuilder = new CourseContact.Builder()
                    ccbuilder.instructionalMethod(method)
                             .hours(hours.toFloat())
                    contacts.add(ccbuilder.build())
                }
                builder.courseContacts(contacts)
            }

        }

        if (requirementGroup && courseStartDate) {
            log.debug("get: retrieving requisite data")
            String[] requisiteData = []
            peoplesoftSession = api.peoplesoft.PSConnection.getSession(environment, misCode)
            try {
                methodName = "getCourseRequisites"
                args = [
                        PSParameter.ConvertStringToCleanString(requirementGroup),
                        PSParameter.ConvertStringToCleanString(courseStartDate),
                        PSParameter.ConvertStringToCleanString(MisEnvironment.getProperty(environment, misCode,"peoplesoft.global.institution")),
                        PSParameter.ConvertStringToCleanString(MisEnvironment.getProperty(environment, misCode,"peoplesoft.global.acad_career"))
                ]
                log.debug("get: calling the remote peoplesoft method")

                requisiteData = api.peoplesoft.PSConnection.callMethod(peoplesoftSession, packageName, className, methodName, "<", args)
            }
            catch(psft.pt8.joa.JOAException e) {
                String messageToThrow = e.getMessage()
                if(peoplesoftSession != null) {
                    String[] msgs = api.peoplesoft.PSConnection.getErrorMessages(peoplesoftSession)
                    if(msgs && msgs.length > 0) {
                        messageToThrow = msgs[0]
                        msgs.each {
                            log.error("get: requisites retrieval failed: [" + it + "]")
                        }
                    }
                }
                throw new InternalServerException(InternalServerException.Errors.internalServerError, messageToThrow)
            }
            finally {
                log.debug("get: disconnecting after getting requisite data")
                if(peoplesoftSession != null) {
                    peoplesoftSession.disconnect()
                }
            }


            if(requisiteData && requisiteData.length > 0) {
                log.debug("get: parsing requisite data")
                String prereq_desc
                String coreq_desc
                List<com.ccctc.adaptor.model.Course> prereqs = []
                List<com.ccctc.adaptor.model.Course> coreqs = []
                String reqGroupDescr = ""
                i = 0
                while (i + 6 <= requisiteData.length) {
                    String reqGroupId = requisiteData[i++]
                    reqGroupDescr = requisiteData[i++]
                    String reqSubject = requisiteData[i++]
                    String reqCatalogNbr = requisiteData[i++]
                    String reqCourseDescr = requisiteData[i++]
                    String reqType = requisiteData[i++]
                    if(reqCourseDescr) {
                        com.ccctc.adaptor.model.Course.Builder reqbuilder = new com.ccctc.adaptor.model.Course.Builder()
                        if (reqSubject && reqCatalogNbr) {
                            reqbuilder.sisCourseId(reqSubject + "-" + reqCatalogNbr)
                        }
                        reqbuilder.description(reqCourseDescr)

                        if (StringUtils.equalsIgnoreCase(reqType, "CO")) {
                            coreqs += reqbuilder.build()
                        } else {
                            prereqs += reqbuilder.build()
                        }
                    } else if(reqGroupDescr) {
                        if (StringUtils.equalsIgnoreCase(reqType, "CO")) {
                            coreq_desc = reqGroupDescr
                        } else {
                            prereq_desc = reqGroupDescr
                        }
                    }
                }

                if(prereqs && prereqs.size() > 0) {
                    prereq_desc = reqGroupDescr
                }
                if(coreqs && coreqs.size() > 0) {
                    coreq_desc = reqGroupDescr
                }

                builder.prerequisites(prereq_desc ?: null)
                       .corequisites(coreq_desc ?: null)
                       .prerequisiteList(prereqs)
                       .corequisiteList(coreqs)
            }
        }
        log.debug("get: done")

        return builder.build()
    }

    /**
     * Translates/Parses the course attribute value into a CreditStatus value using the mapping config values
     * @param misCode The College Code used for grabbing college specific settings from the environment
     * @param crse_attr_value The Peoplesoft specific code describing the course credit status grouping
     * @return A CreditStatus enum value that maps to the crse_attr_value provided
     */
    protected CreditStatus parseCreditStatus(String misCode, String crse_attr_value) {
        CreditStatus result
        if(crse_attr_value) {
            String configMapping = MisEnvironment.getProperty(environment, misCode, "peoplesoft.course.mappings.creditStatus.crse_attr_value." + crse_attr_value)
            if (configMapping) {
                CreditStatus found = CreditStatus.values().find {
                    return it.name().equalsIgnoreCase(configMapping)
                }
                if (!found) {
                    log.warn("Could not parse creditStatus [" + crse_attr_value + "]")
                } else {
                    result = found
                }
            } else {
                log.warn("creditStatus [" + crse_attr_value + "] mapping not found")
            }
        }
        return result
    }

    /**
     * Translates/Parses the course attribute value into a TransferStatus value using the mapping config values
     * @param misCode The College Code used for grabbing college specific settings from the environment
     * @param crse_attr_value The peoplesoft specific value used to represent the ability for course credit to transfer
     * @return A TransferStatus enum value that maps to the crse_attr_value provided
     */
    protected TransferStatus parseTransferStatus(String misCode, String crse_attr_value) {
        TransferStatus result
        if(crse_attr_value) {
            String configMapping = MisEnvironment.getProperty(environment, misCode, "peoplesoft.course.mappings.transferStatus.crse_attr_value." + crse_attr_value)
            if (configMapping) {
                TransferStatus found = TransferStatus.values().find {
                    return it.name().equalsIgnoreCase(configMapping)
                }
                if (!found) {
                    log.warn("Could not parse transferStatus [" + crse_attr_value + "]")
                } else {
                    result = found
                }
            } else {
                log.warn("transferStatus [" + crse_attr_value + "] mapping not found")
            }
        }
        return result
    }

    /**
     * Translates/Parses the grading basis code into a GradingMethod value using the mapping config values
     * @param misCode The College Code used for grabbing college specific settings from the environment
     * @param gradingBasis the Peoplesoft specific code representing how the course is to be graded
     * @return A GradingMethod enum value that maps to the gradingBasis provided
     */
    protected GradingMethod parseGradeMethod(String misCode, String gradingBasis){
        GradingMethod result
        if(gradingBasis) {
            String configMapping = MisEnvironment.getProperty(environment, misCode, "peoplesoft.course.mappings.gradingMethod.grading_basis." + gradingBasis)
            if (configMapping) {
                GradingMethod found = GradingMethod.values().find {
                    return it.name().equalsIgnoreCase(configMapping)
                }
                if (!found) {
                    log.warn("Could not parse gradingBasis [" + gradingBasis + "]")
                } else {
                    result = found
                }
            } else {
                log.warn("gradingBasis [" + gradingBasis + "] mapping not found")
            }
        }
        return result
    }

    /**
     * Translates/Parses the ssr_component code into a InstructionalMethod value using the mapping config values
     * @param misCode The College Code used for grabbing college specific settings from the environment
     * @param component The Peoplesoft specific main ssr component for the course
     * @return A InstructionalMethod enum value that maps to the component provided
     * Note: the Course-level mapping for Instructional Method is more generic than
     *       the class (aka section) Instructional Method mapping
     */
    protected InstructionalMethod parseInstructionalMethod(String misCode, String component) {
        InstructionalMethod result = InstructionalMethod.Other
        if(component) {
            String configMapping = MisEnvironment.getProperty(environment, misCode, "peoplesoft.course.mappings.instructionalMethod.ssr_component." + component)
            if (configMapping) {
                InstructionalMethod found = InstructionalMethod.values().find {
                    return it.name().equalsIgnoreCase(configMapping)
                }
                if (!found) {
                    log.warn("Could not parse component [" + component + "]")
                } else {
                    result = found
                }
            } else {
                log.warn("component [" + component + "] mapping not found")
            }
        }
        return result
    }
}