package api.peoplesoft

import com.ccctc.adaptor.exception.EntityNotFoundException
import com.ccctc.adaptor.exception.InternalServerException
import com.ccctc.adaptor.exception.InvalidRequestException
import com.ccctc.adaptor.model.CrosslistingDetail
import com.ccctc.adaptor.util.MisEnvironment
import org.apache.commons.lang.StringUtils
import org.slf4j.Logger
import org.slf4j.LoggerFactory

import java.text.SimpleDateFormat
import com.ccctc.adaptor.model.MeetingTime
import com.ccctc.adaptor.model.Instructor
import com.ccctc.adaptor.model.SectionStatus
import com.ccctc.adaptor.model.InstructionalMethod
import org.springframework.core.env.Environment

/**
 * <h1>Section Peoplesoft Interface</h1>
 * <summary>
 *     <p>Provides a Wrapper around the native peoplesoft java API for:</p>
 *     <ol>
 *         <li>retrieving section details from the built-in PS tables</li>
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
class Section {

    protected final static Logger log = LoggerFactory.getLogger(Section.class)

    //****** Populated by injection in Groovy Service Implementation ****
    Environment environment

    /**
     * Default Constructor needed since we also specify an alternative Constructor
     */
    Section(){
        // do nothing, except be available
    }

    /**
     * Alternative Constructor to set the environment during initialization.
     * Useful for when other groovy PS APIs need to use these Functions that depend on environment
     */
    Section(Environment e) {
        this.environment = e
    }

    /**
     * Retrieves a Single Course Section based on the section and term provided
     * @param misCode The College Code used for grabbing college specific settings from the environment (required)
     * @param sisSectionId The peoplesoft specific Section Id of the section to retrieve details of; (AKA Class_Nbr) (required)
     * @param sisTermId the Term in which to get details for the given section (required)
     * @return A Section Object representing the matching section based on sectionId and term
     */
    com.ccctc.adaptor.model.Section get(String misCode, String sisSectionId, String sisTermId) {
        log.debug("get: retrieving single/specific section data")

        //****** Validate parameters ****
        if (!misCode) {
            throw new InvalidRequestException(InvalidRequestException.Errors.invalidSearchCriteria, "misCode cannot be null or blank")
        }

        if (!sisSectionId) {
            throw new InvalidRequestException(InvalidRequestException.Errors.invalidSearchCriteria, "section cannot be null or blank")
        }

        if (!sisTermId) {
            throw new InvalidRequestException(InvalidRequestException.Errors.invalidSearchCriteria, "term cannot be null or blank")
        }

        log.debug("get: params ok; retrieving term data")

        api.peoplesoft.Term termAPI = new api.peoplesoft.Term(environment)
        com.ccctc.adaptor.model.Term t = termAPI.get(misCode, sisTermId)
        if(!t) {
            throw new EntityNotFoundException("Term not found")
        }

        Date prereg_begin_dt = t.getRegistrationStart()
        Date prereg_end_dt = t.getRegistrationEnd()
        Date census_dt = t.getCensusDate()
        Date reg_begin_dt = t.getRegistrationStart()
        Date reg_end_dt = t.getRegistrationEnd()
        Date last_withdrawl_dt = t.getWithdrawalDeadline()
        Date add_dt = t.getAddDeadline()
        Date drop_dt = t.getDropDeadline()

        //****** Build parameters to send to our custom Peopletools API ****
        String packageName = "CCTC_COURSE_SECTION_PKG"
        String className = "CCTC_Section"
        String methodName = "getDetails"

        String[] args = [
                PSParameter.ConvertStringToCleanString(sisTermId),
                PSParameter.ConvertStringToCleanString(MisEnvironment.getProperty(environment, misCode,"peoplesoft.global.institution")),
                PSParameter.ConvertStringToCleanString(MisEnvironment.getProperty(environment, misCode,"peoplesoft.global.acad_career")),
                PSParameter.ConvertStringToCleanString(sisSectionId)
        ]

        com.ccctc.adaptor.model.Section result

        log.debug("get: attempting to get a new peoplesoft Session")
        //****** Create Connection to Peoplesoft and call remote method ******
        def peoplesoftSession = api.peoplesoft.PSConnection.getSession(environment, misCode)
        try {
            log.debug("get: calling the remote peoplesoft method")

            String[] sectionData = api.peoplesoft.PSConnection.callMethod(peoplesoftSession, packageName, className, methodName, "<", args)

            log.debug("get: making sure section was found")
            if(sectionData == null || sectionData.length < 20) {
                throw new EntityNotFoundException("Section not found.")
            }

            log.debug("get: parsing sectionData")
            Integer i = 0
            String term_id = sectionData[i++]
            String sisSessionCode = sectionData[i++]

            String intCourseId = sectionData[i++]
            String crse_offer_nbr = sectionData[i++]
            String subject = sectionData[i++]
            String catalog_nbr = sectionData[i++]
            sisSectionId = sectionData[i++]
            String intClassSection = sectionData[i++]

            String class_stat = sectionData[i++]
            String descr = sectionData[i++]

            String enrl_cap = sectionData[i++]
            String wait_cap = sectionData[i++]
            String campus = sectionData[i++]
            String ssr_component = sectionData[i++]
            String instruction_mode = sectionData[i++]

            String unit_min = sectionData[i++]
            String unit_max = sectionData[i++]

            String section_begin_dt = sectionData[i++]
            String section_end_dt = sectionData[i++]
            String section_woi = sectionData[i++]

            com.ccctc.adaptor.model.Section.Builder builder = new com.ccctc.adaptor.model.Section.Builder()
            builder.sisSectionId(sisSectionId)
                   .sisTermId(sisTermId)
                   .sisCourseId(subject + "-" + catalog_nbr)
                   .maxEnrollments(enrl_cap.toInteger() ?: 0)
                   .maxWaitlist(wait_cap.toInteger() ?: 0)
                   .minimumUnits(unit_min.toFloat() ?: 0)
                   .maximumUnits(unit_max.toFloat() ?: 0)
                   .title(descr ?: null)
                   .status(this.parseSectionStatus(misCode, class_stat))
                   .campus(campus ?: null)
                   .start((section_begin_dt != "" ? (new SimpleDateFormat("yyyy-MM-dd")).parse(section_begin_dt) : null))
                   .end((section_end_dt != "" ? (new SimpleDateFormat("yyyy-MM-dd")).parse(section_end_dt) : null))
                   .weeksOfInstruction(section_woi.toInteger() ?: 0)

            builder.preRegistrationStart(prereg_begin_dt)
                   .preRegistrationEnd(prereg_end_dt)
                   .registrationStart(reg_begin_dt)
                   .registrationEnd(reg_end_dt)
                   .addDeadline(add_dt)
                   .dropDeadline(drop_dt)
                   .withdrawalDeadline(last_withdrawl_dt)
                   .feeDeadline()
                   .censusDate(census_dt)

            log.debug("get: retrieving Instructors")
            List<com.ccctc.adaptor.model.Instructor> instructors = this.getSectionInstructors(peoplesoftSession,
                                                                                              intCourseId,
                                                                                              crse_offer_nbr,
                                                                                              sisTermId,
                                                                                              sisSessionCode,
                                                                                              intClassSection)
            builder.instructors(instructors)

            log.debug("get: retrieving meetingPatterns")
            List<com.ccctc.adaptor.model.MeetingTime> meetingPatterns = this.getSectionMeetingPatterns(peoplesoftSession,
                                                                                                       intCourseId,
                                                                                                       crse_offer_nbr,
                                                                                                       sisTermId,
                                                                                                       sisSessionCode,
                                                                                                       intClassSection,
                                                                                                       this.parseInstructionalMethod(misCode, ssr_component, instruction_mode))
            builder.meetingTimes(meetingPatterns)

            log.debug("get: retrieving crossListings")
            com.ccctc.adaptor.model.CrosslistingDetail xListing = this.getSectionCrossListing(peoplesoftSession,
                                                                                              misCode,
                                                                                              sisTermId,
                                                                                              sisSessionCode,
                                                                                              sisSectionId)
            if(xListing) {
                builder.crosslistingDetail(xListing)
            }

            result = builder.build()
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

        log.debug("get: done")
        return result
    }

    /**
     * Retrieves multiple Course Sections based on the course and term provided
     * @param misCode The College Code used for grabbing college specific settings from the environment (required)
     * @param sisTermId the Term in which to get details for the given section (required)
     * @param sisCourseId The college adaptor Course Id of the course to retrieve sections of; usually in form of SUBJ-NBR (required)
     * @return An Array of matching Section Objects representing all the sections offered for the given course and term
     */
    List<com.ccctc.adaptor.model.Section> getAll(String misCode, String sisTermId, String sisCourseId) {
        log.debug("getAll: retrieving all sections for a course/term")

        //****** Validate parameters ****
        if (!misCode) {
            throw new InvalidRequestException(InvalidRequestException.Errors.invalidSearchCriteria, "misCode cannot be null or blank")
        }

        if (!sisTermId) {
            throw new InvalidRequestException(InvalidRequestException.Errors.invalidSearchCriteria, "term cannot be null or blank")
        }

        if (!sisCourseId) {
            throw new InvalidRequestException(InvalidRequestException.Errors.invalidSearchCriteria, "course cannot be null or blank")
        }

        log.debug("getAll: params ok; retrieving term data")

        api.peoplesoft.Term termAPI = new api.peoplesoft.Term(environment)
        com.ccctc.adaptor.model.Term t = termAPI.get(misCode, sisTermId)
        if(!t) {
            throw new EntityNotFoundException("Term not found")
        }

        Date prereg_begin_dt = t.getRegistrationStart()
        Date prereg_end_dt = t.getRegistrationEnd()
        Date census_dt = t.getCensusDate()
        Date reg_begin_dt = t.getRegistrationStart()
        Date reg_end_dt = t.getRegistrationEnd()
        Date last_withdrawl_dt = t.getWithdrawalDeadline()
        Date add_dt = t.getAddDeadline()
        Date drop_dt = t.getDropDeadline()

        //****** Build parameters to send to our custom Peopletools API ****
        String packageName = "CCTC_COURSE_SECTION_PKG"
        String className = "CCTC_Section"
        String methodName = "getSectionsForCourse"

        String[] args = [
                PSParameter.ConvertStringToCleanString(sisTermId),
                PSParameter.ConvertStringToCleanString(MisEnvironment.getProperty(environment, misCode,"peoplesoft.global.institution")),
                PSParameter.ConvertStringToCleanString(MisEnvironment.getProperty(environment, misCode,"peoplesoft.global.acad_career")),
                PSParameter.ConvertStringToCleanString(sisCourseId)
        ]

        List<com.ccctc.adaptor.model.Section> results = []

        log.debug("getAll: attempting to get a new peoplesoft Session")
        //****** Create Connection to Peoplesoft and call remote method ******
        def peoplesoftSession = api.peoplesoft.PSConnection.getSession(environment, misCode)
        try {
            log.debug("getAll: calling the remote peoplesoft method")

            String[] sectionData = api.peoplesoft.PSConnection.callMethod(peoplesoftSession, packageName, className, methodName, "<", args)


            log.debug("getAll: parsing sectionData")
            Integer i = 0
            while(i+ 20 <= sectionData.length) {
                String term_id = sectionData[i++]
                String sisSessionCode = sectionData[i++]

                String intCourseId = sectionData[i++]
                String crse_offer_nbr = sectionData[i++]
                String subject = sectionData[i++]
                String catalog_nbr = sectionData[i++]
                String sisSectionId = sectionData[i++]
                String intClassSection = sectionData[i++]

                String class_stat = sectionData[i++]
                String descr = sectionData[i++]

                String enrl_cap = sectionData[i++]
                String wait_cap = sectionData[i++]
                String campus = sectionData[i++]
                String ssr_component = sectionData[i++]
                String instruction_mode = sectionData[i++]

                String unit_min = sectionData[i++]
                String unit_max = sectionData[i++]

                String section_begin_dt = sectionData[i++]
                String section_end_dt = sectionData[i++]
                String section_woi = sectionData[i++]

                com.ccctc.adaptor.model.Section.Builder builder = new com.ccctc.adaptor.model.Section.Builder()
                builder.sisSectionId(sisSectionId)
                        .sisTermId(sisTermId)
                        .sisCourseId(subject + "-" + catalog_nbr)
                        .maxEnrollments(enrl_cap.toInteger() ?: 0)
                        .maxWaitlist(wait_cap.toInteger() ?: 0)
                        .minimumUnits(unit_min.toFloat() ?: 0)
                        .maximumUnits(unit_max.toFloat() ?: 0)
                        .title(descr ?: null)
                        .status(this.parseSectionStatus(misCode, class_stat))
                        .campus(campus ?: null)
                        .start((section_begin_dt != "" ? (new SimpleDateFormat("yyyy-MM-dd")).parse(section_begin_dt) : null))
                        .end((section_end_dt != "" ? (new SimpleDateFormat("yyyy-MM-dd")).parse(section_end_dt) : null))
                        .weeksOfInstruction(section_woi.toInteger() ?: 0)

                builder.preRegistrationStart(prereg_begin_dt)
                        .preRegistrationEnd(prereg_end_dt)
                        .registrationStart(reg_begin_dt)
                        .registrationEnd(reg_end_dt)
                        .addDeadline(add_dt)
                        .dropDeadline(drop_dt)
                        .withdrawalDeadline(last_withdrawl_dt)
                        .feeDeadline()
                        .censusDate(census_dt)

                log.debug("getAll: retrieving Instructors")
                List<com.ccctc.adaptor.model.Instructor> instructors = this.getSectionInstructors(peoplesoftSession,
                        intCourseId,
                        crse_offer_nbr,
                        sisTermId,
                        sisSessionCode,
                        intClassSection)
                builder.instructors(instructors)

                log.debug("getAll: retrieving meetingPatterns")
                List<com.ccctc.adaptor.model.MeetingTime> meetingPatterns = this.getSectionMeetingPatterns(peoplesoftSession,
                        intCourseId,
                        crse_offer_nbr,
                        sisTermId,
                        sisSessionCode,
                        intClassSection,
                        this.parseInstructionalMethod(misCode, ssr_component, instruction_mode))
                builder.meetingTimes(meetingPatterns)

                log.debug("getAll: retrieving crossListings")
                com.ccctc.adaptor.model.CrosslistingDetail xListing = this.getSectionCrossListing(peoplesoftSession,
                        misCode,
                        sisTermId,
                        sisSessionCode,
                        sisSectionId)
                if (xListing) {
                    builder.crosslistingDetail(xListing)
                }

                results.add(builder.build())
            }
        }
        catch(psft.pt8.joa.JOAException e) {
            String messageToThrow = e.getMessage()
            if(peoplesoftSession != null) {
                String[] msgs = api.peoplesoft.PSConnection.getErrorMessages(peoplesoftSession)
                if(msgs && msgs.length > 0) {
                    messageToThrow = msgs[0]
                    msgs.each {
                        log.error("getAll: retrieval failed: [" + it + "]")
                    }
                }
            }
            throw new InternalServerException(InternalServerException.Errors.internalServerError, messageToThrow)
        }
        finally {
            log.debug("getAll: disconnecting")
            if(peoplesoftSession != null) {
                peoplesoftSession.disconnect()
            }
        }

        log.debug("getAll: done")
        return results
    }

    /**
     * Retrieves multiple Course Sections based on the term and search words provided
     * @param misCode The College Code used for grabbing college specific settings from the environment (required)
     * @param sisTermId The Term in which to get details for the given section (required)
     * @param words Key search phrases to attempt to match to several different fields of a section (required)
     * @return An Array of matching Section Objects representing all the sections matching the given term and search words
     */
    List<com.ccctc.adaptor.model.Section> search(String misCode, String sisTermId, String[] words) {
        log.debug("search: finding matching sections for a term")

        //****** Validate parameters ****
        if (!misCode) {
            throw new InvalidRequestException(InvalidRequestException.Errors.invalidSearchCriteria, "misCode cannot be null or blank")
        }

        if (!sisTermId) {
            throw new InvalidRequestException(InvalidRequestException.Errors.invalidSearchCriteria, "term cannot be null or blank")
        }

        if (!words || words.length < 0) {
            throw new InvalidRequestException(InvalidRequestException.Errors.invalidSearchCriteria, "words cannot be null or blank")
        }

        log.debug("search: params ok; retrieving term data")

        api.peoplesoft.Term termAPI = new api.peoplesoft.Term(environment)
        com.ccctc.adaptor.model.Term t = termAPI.get(misCode, sisTermId)
        if(!t) {
            throw new EntityNotFoundException("Term not found")
        }

        Date prereg_begin_dt = t.getRegistrationStart()
        Date prereg_end_dt = t.getRegistrationEnd()
        Date census_dt = t.getCensusDate()
        Date reg_begin_dt = t.getRegistrationStart()
        Date reg_end_dt = t.getRegistrationEnd()
        Date last_withdrawl_dt = t.getWithdrawalDeadline()
        Date add_dt = t.getAddDeadline()
        Date drop_dt = t.getDropDeadline()

        //****** Build parameters to send to our custom Peopletools API ****
        String packageName = "CCTC_COURSE_SECTION_PKG"
        String className = "CCTC_Section"
        String methodName = "searchForSections"

        String[] args = [
                PSParameter.ConvertStringToCleanString(sisTermId),
                PSParameter.ConvertStringToCleanString(MisEnvironment.getProperty(environment, misCode,"peoplesoft.global.institution")),
                PSParameter.ConvertStringToCleanString(MisEnvironment.getProperty(environment, misCode,"peoplesoft.global.acad_career"))
        ]
        words.each {
            if(StringUtils.isNotEmpty(it)){
                args += PSParameter.ConvertStringToCleanString(it)
            }
        }

        List<com.ccctc.adaptor.model.Section> results = []

        log.debug("search: attempting to get a new peoplesoft Session")
        //****** Create Connection to Peoplesoft and call remote method ******
        def peoplesoftSession = api.peoplesoft.PSConnection.getSession(environment, misCode)
        try {
            log.debug("search: calling the remote peoplesoft method")

            String[] sectionData = api.peoplesoft.PSConnection.callMethod(peoplesoftSession, packageName, className, methodName, "<", args)


            log.debug("search: parsing sectionData")
            Integer i = 0
            while(i+ 20 <= sectionData.length) {
                String term_id = sectionData[i++]
                String sisSessionCode = sectionData[i++]

                String intCourseId = sectionData[i++]
                String crse_offer_nbr = sectionData[i++]
                String subject = sectionData[i++]
                String catalog_nbr = sectionData[i++]
                String sisSectionId = sectionData[i++]
                String intClassSection = sectionData[i++]

                String class_stat = sectionData[i++]
                String descr = sectionData[i++]

                String enrl_cap = sectionData[i++]
                String wait_cap = sectionData[i++]
                String campus = sectionData[i++]
                String ssr_component = sectionData[i++]
                String instruction_mode = sectionData[i++]

                String unit_min = sectionData[i++]
                String unit_max = sectionData[i++]

                String section_begin_dt = sectionData[i++]
                String section_end_dt = sectionData[i++]
                String section_woi = sectionData[i++]

                com.ccctc.adaptor.model.Section.Builder builder = new com.ccctc.adaptor.model.Section.Builder()
                builder.sisSectionId(sisSectionId)
                        .sisTermId(sisTermId)
                        .sisCourseId(subject + "-" + catalog_nbr)
                        .maxEnrollments(enrl_cap.toInteger() ?: 0)
                        .maxWaitlist(wait_cap.toInteger() ?: 0)
                        .minimumUnits(unit_min.toFloat() ?: 0)
                        .maximumUnits(unit_max.toFloat() ?: 0)
                        .title(descr ?: null)
                        .status(this.parseSectionStatus(misCode, class_stat))
                        .campus(campus ?: null)
                        .start((section_begin_dt != "" ? (new SimpleDateFormat("yyyy-MM-dd")).parse(section_begin_dt) : null))
                        .end((section_end_dt != "" ? (new SimpleDateFormat("yyyy-MM-dd")).parse(section_end_dt) : null))
                        .weeksOfInstruction(section_woi.toInteger() ?: 0)

                builder.preRegistrationStart(prereg_begin_dt)
                        .preRegistrationEnd(prereg_end_dt)
                        .registrationStart(reg_begin_dt)
                        .registrationEnd(reg_end_dt)
                        .addDeadline(add_dt)
                        .dropDeadline(drop_dt)
                        .withdrawalDeadline(last_withdrawl_dt)
                        .feeDeadline()
                        .censusDate(census_dt)

                log.debug("search: retrieving Instructors")
                List<com.ccctc.adaptor.model.Instructor> instructors = this.getSectionInstructors(peoplesoftSession,
                        intCourseId,
                        crse_offer_nbr,
                        sisTermId,
                        sisSessionCode,
                        intClassSection)
                builder.instructors(instructors)

                log.debug("search: retrieving meetingPatterns")
                List<com.ccctc.adaptor.model.MeetingTime> meetingPatterns = this.getSectionMeetingPatterns(peoplesoftSession,
                        intCourseId,
                        crse_offer_nbr,
                        sisTermId,
                        sisSessionCode,
                        intClassSection,
                        this.parseInstructionalMethod(misCode, ssr_component, instruction_mode))
                builder.meetingTimes(meetingPatterns)

                log.debug("search: retrieving crossListings")
                com.ccctc.adaptor.model.CrosslistingDetail xListing = this.getSectionCrossListing(peoplesoftSession,
                        misCode,
                        sisTermId,
                        sisSessionCode,
                        sisSectionId)
                if (xListing) {
                    builder.crosslistingDetail(xListing)
                }

                results.add(builder.build())
            }
        }
        catch(psft.pt8.joa.JOAException e) {
            String messageToThrow = e.getMessage()
            if(peoplesoftSession != null) {
                String[] msgs = api.peoplesoft.PSConnection.getErrorMessages(peoplesoftSession)
                if(msgs && msgs.length > 0) {
                    messageToThrow = msgs[0]
                    msgs.each {
                        log.error("search: retrieval failed: [" + it + "]")
                    }
                }
            }
            throw new InternalServerException(InternalServerException.Errors.internalServerError, messageToThrow)
        }
        finally {
            log.debug("search: disconnecting")
            if(peoplesoftSession != null) {
                peoplesoftSession.disconnect()
            }
        }

        log.debug("search: done")
        return results
    }

    /**
     * Using an Existing (and open) peoplesoft session, retrieves the instructors assigned to a given section
     * @param peoplesoftSession an open peoplesoft connection to tunnel through (required)
     * @param intCourseId The peoplesoft specific course id; not to be confused with the College Adaptor course Id (required)
     * @param crseOfferNbr The peoplesoft specific course offering number (required)
     * @param sisTermId The peoplesoft specific Term in which the section is offered (required)
     * @param sisSessionCode The peoplesoft specific Session Code in which the section is offered (required)
     * @param intClassSection The peoplesoft specific section id; not to be confused with the College Adaptor section Id (required)
     * @return A List of Instructors assigned to the given section
     * Note: the peoplesoft session is not closed/disposed within this function. Caller should handle that.
     */
    protected List<com.ccctc.adaptor.model.Instructor> getSectionInstructors(def peoplesoftSession,
                                                                             String intCourseId,
                                                                             String crseOfferNbr,
                                                                             String sisTermId,
                                                                             String sisSessionCode,
                                                                             String intClassSection) {

        String packageName = "CCTC_COURSE_SECTION_PKG"
        String className = "CCTC_Section"
        String methodName = "getInstructors"

        String[] args = [
                PSParameter.ConvertStringToCleanString(intCourseId),
                PSParameter.ConvertStringToCleanString(crseOfferNbr),
                PSParameter.ConvertStringToCleanString(sisTermId),
                PSParameter.ConvertStringToCleanString(sisSessionCode),
                PSParameter.ConvertStringToCleanString(intClassSection),
        ]

        String[] instructorData = api.peoplesoft.PSConnection.callMethod(peoplesoftSession, packageName, className, methodName, "<", args)

        List<com.ccctc.adaptor.model.Instructor> instructors = []

        if(instructorData) {
            Integer j = 0
            while (j + 4 <= instructorData.length) {
                String emplId = instructorData[j++]
                String firstName = instructorData[j++]
                String lastName = instructorData[j++]
                String emailAddr = instructorData[j++]

                com.ccctc.adaptor.model.Instructor.Builder instbuilder = new Instructor.Builder()
                instbuilder.sisPersonId(emplId)
                           .firstName(firstName)
                           .lastName(lastName)
                           .emailAddress(emailAddr)

                instructors.add(instbuilder.build())

            }
        }
        return instructors
    }

    /**
     * Using an Existing (and open) peoplesoft session, retrieves the meeting pattens assigned to a given section
     * @param peoplesoftSession an open peoplesoft connection to tunnel through (required)
     * @param intCourseId The peoplesoft specific course id; not to be confused with the College Adaptor course Id (required)
     * @param crseOfferNbr The peoplesoft specific course offering number (required)
     * @param sisTermId The peoplesoft specific Term in which the section is offered (required)
     * @param sisSessionCode The peoplesoft specific Session Code in which the section is offered (required)
     * @param intClassSection The peoplesoft specific section id; not to be confused with the College Adaptor section Id (required)
     * @param iMethod the Parsed Instructional Method to assign to each/every meeting pattern for the section
     * @return A List of Meeting Patterns assigned to the given section
     * Note: the peoplesoft session is not closed/disposed within this function. Caller should handle that.
     */
    protected List<com.ccctc.adaptor.model.MeetingTime> getSectionMeetingPatterns(def peoplesoftSession,
                                                                                  String intCourseId,
                                                                                  String crseOfferNbr,
                                                                                  String sisTermId,
                                                                                  String sisSessionCode,
                                                                                  String intClassSection,
                                                                                  InstructionalMethod iMethod) {

        String packageName = "CCTC_COURSE_SECTION_PKG"
        String className = "CCTC_Section"
        String methodName = "getMeetingPatterns"

        String[] args = [
                PSParameter.ConvertStringToCleanString(intCourseId),
                PSParameter.ConvertStringToCleanString(crseOfferNbr),
                PSParameter.ConvertStringToCleanString(sisTermId),
                PSParameter.ConvertStringToCleanString(sisSessionCode),
                PSParameter.ConvertStringToCleanString(intClassSection),
        ]

        String[] patternData = api.peoplesoft.PSConnection.callMethod(peoplesoftSession, packageName, className, methodName, "<", args)

        List<com.ccctc.adaptor.model.MeetingTime> meetings = []

        if(patternData) {
            SimpleDateFormat df1 = new SimpleDateFormat("yyyy-MM-dd'T'HH.mm.ss.SSSSSS")
            SimpleDateFormat df2 = new SimpleDateFormat("yyyy-MM-dd")

            Integer j = 0
            while (j + 15 <= patternData.length) {
                String meeting_nbr = patternData[j++]
                String meetingTimeStart = patternData[j++]
                String meetingTimeEnd = patternData[j++]
                String mon = patternData[j++]
                String tue = patternData[j++]
                String wed = patternData[j++]
                String thur = patternData[j++]
                String fri = patternData[j++]
                String sat = patternData[j++]
                String sun = patternData[j++]
                String meeting_start_dt = patternData[j++]
                String meeting_end_dt = patternData[j++]
                String bldg = patternData[j++]
                String room = patternData[j++]
                String loc = patternData[j++]

                com.ccctc.adaptor.model.MeetingTime.Builder mtbuilder = new MeetingTime.Builder()
                mtbuilder.monday(StringUtils.equalsIgnoreCase(mon, "Y"))
                         .tuesday(StringUtils.equalsIgnoreCase(tue, "Y"))
                         .wednesday(StringUtils.equalsIgnoreCase(wed, "Y"))
                         .thursday(StringUtils.equalsIgnoreCase(thur, "Y"))
                         .friday(StringUtils.equalsIgnoreCase(fri, "Y"))
                         .saturday(StringUtils.equalsIgnoreCase(sat, "Y"))
                         .sunday(StringUtils.equalsIgnoreCase(sun, "Y"))
                         .campus(loc ?: null)
                         .building(bldg ?: null)
                         .room(room ?: null)
                         .start((meeting_start_dt != "") ? df2.parse(meeting_start_dt) : null)
                         .end((meeting_end_dt != "") ? df2.parse(meeting_end_dt) : null)
                         .startTime((meetingTimeStart != null) ? df1.parse(meeting_start_dt + "T" + meetingTimeStart) : null)
                         .endTime((meetingTimeEnd != null) ? df1.parse(meeting_end_dt + "T" + meetingTimeEnd) : null)
                         .instructionalMethod(iMethod)
                meetings.add(mtbuilder.build())

            }
        }

        return meetings
    }

    /**
     * Using an Existing (and open) peoplesoft session, retrieves the cross listing details for a given section
     * @param peoplesoftSession an open peoplesoft connection to tunnel through (required)
     * @param misCode The College Code used for grabbing college specific settings from the environment (required)
     * @param sisTermId The peoplesoft specific Term in which the section is offered (required)
     * @param sisSessionCode The peoplesoft specific Session Code in which the section is offered (required)
     * @param sisSectionId The college adaptor section Id (AKA Class_Nbr) (required)
     * @return A Cross Listing Detail object containing all the cross listed section offerings for the given section
     * Note: the peoplesoft session is not closed/disposed within this function. Caller should handle that.
     */
    protected com.ccctc.adaptor.model.CrosslistingDetail getSectionCrossListing(def peoplesoftSession,
                                                                                String misCode,
                                                                                String sisTermId,
                                                                                String sisSessionCode,
                                                                                String sisSectionId) {

        String packageName = "CCTC_COURSE_SECTION_PKG"
        String className = "CCTC_Section"
        String methodName = "getCrossListing"

        String[] args = [
                PSParameter.ConvertStringToCleanString(MisEnvironment.getProperty(environment, misCode,"peoplesoft.global.institution")),
                PSParameter.ConvertStringToCleanString(sisTermId),
                PSParameter.ConvertStringToCleanString(sisSessionCode),
                PSParameter.ConvertStringToCleanString(sisSectionId),
        ]

        String[] xListData = api.peoplesoft.PSConnection.callMethod(peoplesoftSession, packageName, className, methodName, "<", args)

        com.ccctc.adaptor.model.CrosslistingDetail listingDetail = null

        if (xListData) {

            String xlist_id
            String primary_section_id
            List<String> sectionIds = []
            Integer i = 0
            while(i + 3 <= xListData.length) {
                xlist_id = xListData[i++]
                String cur_sectionId = xListData[i++]
                String cur_is_primary = xListData[i++]
                if(StringUtils.equalsIgnoreCase(cur_is_primary, "Y")) {
                    primary_section_id = cur_sectionId
                }
                sectionIds.add(cur_sectionId)
            }

            if(sectionIds && sectionIds.size() > 0){
                com.ccctc.adaptor.model.CrosslistingDetail.Builder xListBuilder = new CrosslistingDetail.Builder()

                if(xlist_id){
                    xListBuilder.name(xlist_id)
                }
                if(primary_section_id){
                    xListBuilder.primarySisSectionId(primary_section_id)
                }
                xListBuilder.sisSectionIds(sectionIds)
                xListBuilder.sisTermId(sisTermId)

                listingDetail = xListBuilder.build()
            }
        }

        return listingDetail
    }

    /**
     * Converts the peoplesoft class_stat values into a valid SectionStatus value
     * @param misCode The College Code used for grabbing college specific settings from the environment (required)
     * @param class_stat The value to convert using the config mappings
     * @return A SectionStatus enum value representing the mapped value given
     */
    protected SectionStatus parseSectionStatus(String misCode, String class_stat) {
        SectionStatus result
        if(class_stat) {
            String configMapping = MisEnvironment.getProperty(environment, misCode, "peoplesoft.section.mappings.sectionStatus.class_stat." + class_stat)
            if (configMapping) {
                SectionStatus found = SectionStatus.values().find {
                    return it.name().equalsIgnoreCase(configMapping)
                }
                if (!found) {
                    log.warn("Could not parse class_stat [" + class_stat + "]")
                } else {
                    result = found
                }
            } else {
                log.warn("class_stat [" + class_stat + "] mapping not found")
            }
        }
        return result
    }

    /**
     * Converts the peoplesoft SSR_COMPONENT and INSTRUCTION_MODE into a valid InstructionalMethod value
     * @param misCode The College Code used for grabbing college specific settings from the environment (required)
     * @param ssr_component The peoplesoft specific ssr_component field to convert
     * @param instruction_mode The peoplesoft specific instruction_mode field to convert
     * @return A InstructionalMethod enum value representing the mapped value given
     */
    protected InstructionalMethod parseInstructionalMethod(String misCode, String ssr_component, String instruction_mode) {
        InstructionalMethod result = InstructionalMethod.Other
        if(ssr_component) {
            InstructionalMethod found = null
            if(instruction_mode) {
                String configMapping = MisEnvironment.getProperty(environment, misCode, "peoplesoft.section.mappings.instructionalMethod.ssr_component." + ssr_component + ".instruction_mode." + instruction_mode)
                if (configMapping) {
                    found = InstructionalMethod.values().find {
                        return it.name().equalsIgnoreCase(configMapping)
                    }
                    if (!found) {
                        log.warn("Could not parse component/instructionMode [" + ssr_component + "." + instruction_mode + "]")
                    }
                }
            }
            if(!found) {
                String configMapping = MisEnvironment.getProperty(environment, misCode, "peoplesoft.section.mappings.instructionalMethod.ssr_component." + ssr_component)
                if (configMapping) {
                    found = InstructionalMethod.values().find {
                        return it.name().equalsIgnoreCase(configMapping)
                    }
                    if (!found) {
                        log.warn("Could not parse component [" + ssr_component + "]")
                    }
                }
            }

            if(found) {
                result = found
            } else {
                log.warn("neither component [" + ssr_component + "] nor component/InstructionMode [" + instruction_mode + "] mappings found")
            }
        }
        return result
    }
}
