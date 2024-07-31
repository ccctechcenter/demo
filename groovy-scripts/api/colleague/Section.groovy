package api.colleague

import api.colleague.util.CastMisParmsUtil
import api.colleague.util.ColleagueUtils
import api.colleague.util.DataUtils
import api.colleague.util.DmiDataServiceAsync
import com.ccctc.adaptor.exception.EntityNotFoundException
import com.ccctc.adaptor.exception.InvalidRequestException
import com.ccctc.adaptor.model.CrosslistingDetail
import com.ccctc.adaptor.model.InstructionalMethod
import com.ccctc.adaptor.model.Instructor
import com.ccctc.adaptor.model.MeetingTime
import com.ccctc.adaptor.model.Section as CASection
import com.ccctc.adaptor.model.SectionStatus
import com.ccctc.adaptor.util.ClassMap
import org.ccctc.colleaguedmiclient.model.ColleagueData
import org.ccctc.colleaguedmiclient.model.ElfTranslateTable
import org.ccctc.colleaguedmiclient.model.Valcode
import org.ccctc.colleaguedmiclient.service.DmiCTXService
import org.ccctc.colleaguedmiclient.service.DmiDataService
import org.ccctc.colleaguedmiclient.service.DmiService
import org.springframework.cache.Cache
import org.springframework.core.env.Environment

import java.time.LocalDate
import java.time.LocalTime

import static api.colleague.util.ColleagueUtils.fromLocalDate
import static api.colleague.util.ColleagueUtils.fromLocalTime
import static api.colleague.util.ColleagueUtils.getAt
import static api.colleague.util.ColleagueUtils.properCase
import static api.colleague.util.ColleagueUtils.quoteString

class Section {

    // services
    DmiDataService dmiDataService
    DmiDataServiceAsync dmiDataServiceAsync
    DataUtils dataUtils
    Cache cache

    // columns
    static sectionColumns = ["SEC.LOCATION", "SEC.SUBJECT", "SEC.COURSE.NO", "SEC.SYNONYM", "SEC.TERM", "SEC.CAPACITY",
                             "SEC.WAITLIST.MAX", "SEC.MIN.CRED", "SEC.MAX.CRED", "SEC.NO.WEEKS", "SEC.START.DATE",
                             "SEC.END.DATE", "SEC.OVR.PREREG.START.DATE", "SEC.OVR.PREREG.END.DATE", "SEC.OVR.REG.START.DATE",
                             "SEC.OVR.REG.END.DATE", "SEC.OVR.ADD.END.DATE", "SEC.OVR.DROP.END.DATE", "SEC.SHORT.TITLE",
                             "SEC.FACULTY", "SEC.MEETING", "SEC.XLIST", "SEC.OVR.DROP.GR.REQD.DATE", "SEC.OVR.CENSUS.DATES",
                             "SEC.STATUS"]

    static locationsColumns = ["LOC.DESC"]

    static buildingsColumns = ["BLDG.LOCATION"]

    static roomsColumns = ["ROOM.NAME"]

    static secFacultyColumns = ["CSF.FACULTY"]

    static personColumns = ["LAST.NAME", "FIRST.NAME", "PERSON.EMAIL.ADDRESSES", "PERSON.PREFERRED.EMAIL"]

    static secMeetingColumns = ["CSM.MONDAY", "CSM.TUESDAY", "CSM.WEDNESDAY", "CSM.THURSDAY", "CSM.FRIDAY", "CSM.SATURDAY",
                                "CSM.SUNDAY", "CSM.START.DATE", "CSM.END.DATE", "CSM.START.TIME", "CSM.END.TIME",
                                "CSM.BLDG", "CSM.ROOM", "CSM.INSTR.METHOD"]

    static secXlistColumns = ["CSXL.PRIMARY.SECTION", "CSXL.COURSE.SECTIONS"]

    static secSynonymColumns = ["SEC.SYNONYM"]


    /**
     * Initialize
     */
    void colleagueInit(String misCode, Environment environment, ClassMap services) {
        def dmiService = services.get(DmiService.class)
        def dmiCTXService = services.get(DmiCTXService.class)
        this.cache = services.get(Cache.class)
        this.dmiDataService = services.get(DmiDataService.class)
        this.dmiDataServiceAsync = new DmiDataServiceAsync(dmiDataService, cache)
        this.dataUtils = new DataUtils(misCode, environment, dmiService, dmiCTXService, dmiDataService, cache)

        ColleagueUtils.keepAlive(dmiService)
    }

    /**
     * Get a section
     *
     * @param misCode      MIS Code
     * @param sisSectionId SIS Section ID
     * @param sisTermId    SIS Term ID
     * @return Section
     */
    CASection get(String misCode, String sisSectionId, String sisTermId) {
        assert misCode != null
        assert sisSectionId != null
        assert sisTermId != null

        if (!dataUtils.validateTerm(sisTermId))
            throw new InvalidRequestException(InvalidRequestException.Errors.termNotFound, "Term not found")

        def criteria = "SEC.TERM = " + quoteString(sisTermId) + " AND SEC.SYNONYM = " + quoteString(sisSectionId)

        def data = readData(criteria, null)
        def parsed = parse(misCode, data)

        if (parsed.size() == 0)
            throw new EntityNotFoundException()

        return parsed[0]
    }

    /**
     * Get a list of sections by sisTermId and optionally sisCourseId
     *
     * @param misCode MIS Code
     * @param sisTermId SIS Term ID
     * @param sisCourseId SIS Course ID
     * @return List of Sections
     */
    List<CASection> getAll(String misCode, String sisTermId, String sisCourseId) {
        assert misCode != null
        assert sisTermId != null

        if (!dataUtils.validateTerm(sisTermId))
            throw new InvalidRequestException(InvalidRequestException.Errors.termNotFound, "Term not found")

        def criteria = "SEC.TERM = " + quoteString(sisTermId)
        if (sisCourseId != null) {
            def split = sisCourseId.split("-")
            if (split.size() == 2) {
                criteria += " AND SEC.SUBJECT = " + quoteString(split[0])
                criteria += " AND SEC.COURSE.NO = " + quoteString(split[1])
            } else {
                throw new InvalidRequestException("Invalid sisCourseId, must be in the format SUBJECT-NO, ie MATH-101")
            }
        }

        def data = readData(criteria, null)
        def parsed = parse(misCode, data)

        return sort(parsed)
    }


    /**
     * Search for sections in a term by keyword(s)
     *
     * Note: multiple words are treated as "AND" logic
     *
     * @param misCode   MIS Code
     * @param sisTermId SIS Term ID
     * @param words     Words to search for
     * @return List of sections
     */
    List<CASection> search(String misCode, String sisTermId, String[] words) {
        assert misCode != null
        assert sisTermId != null
        assert words != null
        assert words.length > 0

        def result = null

        if (!dataUtils.validateTerm(sisTermId))
            throw new InvalidRequestException(InvalidRequestException.Errors.termNotFound, "Term not found")

        // load cached data in the background
        def fLocations = dmiDataServiceAsync.batchSelectAsyncCached("CORE", "LOCATIONS", locationsColumns, null)
        def fBuildings = dmiDataServiceAsync.batchSelectAsyncCached("CORE", "BUILDINGS", buildingsColumns, null)
        def fRooms = dmiDataServiceAsync.batchSelectAsyncCached("CORE", "ROOMS", roomsColumns, null)

        // get a list of keys for all sections in the term. this will be our starting point and we'll whittle it down
        // to the keys that match our criteria
        def sectionIds = dmiDataService.selectKeys("COURSE.SECTIONS", "SEC.TERM = " + quoteString(sisTermId)).toList()

        if (sectionIds) {
            for (def w : words) {
                def wordUc = w.toUpperCase()
                def wordUcQuoted = quoteString(wordUc)

                def nameSearch = null
                if (w.length() >= 2) {
                    // we can generally count on names being proper cased in the system, but just in case, we will allow for three variations:
                    // proper case, upper case and as entered by the user
                    nameSearch = quoteString(w)
                    nameSearch += quoteString(properCase(w))
                    nameSearch += wordUcQuoted
                }

                def titleSearch = null
                if (w.length() >= 5) {
                    // just like name, title search uses three variants but it's a LIKE
                    titleSearch = quoteString("..." + w + "...")
                    titleSearch += quoteString("..." + properCase(w) + "...")
                    titleSearch += quoteString("..." + wordUc + "...")
                }

                // get ids of child records we will be querying (COURSE.SEC.MEETING, COURSE.SEC.FACULTY)
                def fChildKeys = dmiDataServiceAsync.batchKeysAsync("ST", "COURSE.SECTIONS", ["SEC.MEETING", "SEC.FACULTY"], sectionIds)

                //
                // Query COURSE.SECTIONS
                //
                def query = "SEC.SYNONYM = " + wordUcQuoted + " OR SEC.LOCATION = " + wordUcQuoted

                // determine the subject / course no search based on whether there's a dash or not
                if (w.contains('-')) {
                    def s = w.split('-')
                    query += " OR (SEC.SUBJECT = " + quoteString(s[0].toUpperCase()) + " AND SEC.COURSE.NO = " + quoteString(s[1].toUpperCase()) + ")"
                } else {
                    query += " OR SEC.SUBJECT = " + wordUcQuoted + " OR SEC.COURSE.NO = " + wordUcQuoted
                }

                // search location description
                def location = fLocations.get().find { i -> ((String) i.values["LOC.DESC"])?.toUpperCase() == wordUc }
                if (location != null) query += " OR SEC.LOCATION = " + quoteString(location.key)

                // do a "like" search on title if our search term is 5+ characters
                if (titleSearch != null) query += " OR SEC.SHORT.TITLE LIKE " + titleSearch

                def matchingIds = dmiDataService.selectKeys("COURSE.SECTIONS", query, sectionIds).toList()

                //
                // query COURSE.SEC.MEETINGS - but only if the search term is a building or room
                //
                def buildings = fBuildings.get(), rooms = fRooms.get()
                def meetingIds = (List<String>) fChildKeys.get().collect { i -> (String[]) i.values["SEC.MEETING"] }.flatten()
                if (meetingIds && buildings.find { i -> i.key == wordUc } || rooms.find { i -> i.key.contains("*" + wordUc) }) {
                    query = "CSM.BLDG = " + wordUcQuoted + " OR CSM.ROOM = " + wordUcQuoted + " SAVING UNIQUE CSM.COURSE.SECTION"
                    def matchingMeetings = dmiDataService.selectKeys("COURSE.SEC.MEETING", query, meetingIds)
                    matchingIds.addAll(matchingMeetings)
                }

                //
                // query COURSE.SEC.FACULTY and PERSON for matching instructors
                //
                def facAssignIds = (List<String>) fChildKeys.get().collect { i -> (String[]) i.values["SEC.FACULTY"] }.flatten()
                if (facAssignIds && nameSearch != null) {
                    def personIds = dmiDataService.selectKeys("COURSE.SEC.FACULTY", "SAVING CSF.FACULTY", facAssignIds)
                            .toList().unique()

                    if (personIds) {
                        query = "ID = " + wordUcQuoted + " OR FIRST.NAME = " + nameSearch + " OR LAST.NAME = " + nameSearch
                        def matchingPersonIds = dmiDataService.selectKeys("PERSON", query, personIds)
                        // link back to CSF records
                        if (matchingPersonIds) {
                            def ids = matchingPersonIds.collect { quoteString(it) }.join("")

                            query = "CSF.FACULTY = " + ids + " SAVING UNIQUE CSF.COURSE.SECTION"
                            def matchingFacAssign = dmiDataService.selectKeys("COURSE.SEC.FACULTY", query, facAssignIds)
                            matchingIds.addAll(matchingFacAssign)
                        }
                    }
                }

                // remove any ids that weren't in the original list. this could happen if COURSE.SEC.MEETING or
                // COURSE.SEC.FACULTY returned bogus pointers in CSM.COURSE.SECTION or CSF.COURSE.SECTION
                matchingIds = matchingIds.findAll { i -> sectionIds.contains(i) }

                // assign matching IDs back to sectionIds so that the next word is like an "AND" and operates off this new list
                sectionIds = matchingIds.unique()

                if (sectionIds.size() == 0) break
            }

            if (sectionIds) {
                // get the results and sort
                def data = readData(null, sectionIds)
                def parsed = parse(misCode, data)
                result = sort(parsed)
            }

        }

        return result ?: []
    }


    /**
     * Read all records from the DMI and/or cache, set global translations and return a list of SectionData objects.
     *
     * Note: Keys or criteria should be specified, but not both
     */
    ReadResult readData(String criteria, List<String> keys) {
        def result = new ReadResult()
        result.sectionData = []

        List<ColleagueData> sections
        List<ColleagueData> secFaculty = null
        List<ColleagueData> secMeeting = null
        List<ColleagueData> persons = null
        List<ColleagueData> secXlists = null
        List<ColleagueData> secSynonyms = null

        def fSections

        if (keys)
            fSections = dmiDataServiceAsync.batchKeysAsync("ST", "COURSE.SECTIONS", sectionColumns, keys)
        else
            fSections = dmiDataServiceAsync.batchSelectAsync("ST", "COURSE.SECTIONS", sectionColumns, criteria)

        def fSectionStatuses = dmiDataServiceAsync.valcodeAsyncCached("ST", "SECTION.STATUSES")
        def fCastMisParms = CastMisParmsUtil.fromAsync(dmiDataService, cache, false)
        def fLocations = dmiDataServiceAsync.batchSelectAsyncCached("CORE", "LOCATIONS", locationsColumns, null)
        def fBuildings = dmiDataServiceAsync.batchSelectAsyncCached("CORE", "BUILDINGS", buildingsColumns, null)
        def fXf01 = dmiDataServiceAsync.elfTranslationTableAsyncCached("CAST.XF01")

        sections = fSections.get()

        // get child records
        if (sections) {
            def secFacultyIds = (List<String>) sections.collect { i -> (String[]) i.values["SEC.FACULTY"] }.flatten()
            def secMeetingIds = (List<String>) sections.collect { i -> (String[]) i.values["SEC.MEETING"] }.flatten()
            def secXlistIds = (List<String>) sections.findAll { i -> i.values["SEC.XLIST"] != null }.collect { i -> (String) i.values["SEC.XLIST"] }

            def fSecFaculty = secFacultyIds ? dmiDataServiceAsync.batchKeysAsync("ST", "COURSE.SEC.FACULTY", secFacultyColumns, secFacultyIds) : null
            def fSecMeeting = secMeetingIds ? dmiDataServiceAsync.batchKeysAsync("ST", "COURSE.SEC.MEETING", secMeetingColumns, secMeetingIds) : null
            def fSecXlist = secXlistIds ? dmiDataServiceAsync.batchKeysAsync("ST", "COURSE.SEC.XLISTS", secXlistColumns, secXlistIds) : null

            // get person records for instructors
            secFaculty = fSecFaculty?.get()
            def personIds = secFaculty?.collect { i -> (String) i.values["CSF.FACULTY"] }?.unique()
            def fPerson = personIds ? dmiDataServiceAsync.batchKeysAsync("CORE", "PERSON", personColumns, personIds) : null

            // get synonyms for crosslisted sections
            secXlists = fSecXlist?.get()
            def xlistSecIds = (List<String>) secXlists?.collect { i -> (String[]) i.values["CSXL.COURSE.SECTIONS"] }?.flatten()?.unique()
            def fSecSynonyms = xlistSecIds ? dmiDataServiceAsync.batchKeysAsync("ST", "COURSE.SECTIONS", secSynonymColumns, xlistSecIds) : null

            persons = fPerson?.get()
            secMeeting = fSecMeeting?.get()
            secSynonyms = fSecSynonyms?.get()
        }

        // set translations
        result.sectionStatuses = fSectionStatuses.get()
        result.castMisParmsUtil = (CastMisParmsUtil) fCastMisParms.get()
        result.locations = fLocations.get()
        result.buildings = fBuildings.get()
        result.xf01Translation = fXf01.get()

        // combine section data
        for (def s : sections) {
            def r = new SectionData()
            r.section = s

            // add in persons
            def csfIds = (String[]) s.values["SEC.FACULTY"]
            if (csfIds) {
                def csf = secFaculty?.findAll { i -> csfIds.contains(i.key) }
                def pIds = csf?.findAll { i -> i.values["CSF.FACULTY"] != null }?.collect { i -> (String) i.values["CSF.FACULTY"] }
                if (pIds) {
                    r.persons = persons?.findAll { i -> pIds.contains(i.key) }
                }
            }

            // add in meetings
            def csmIds = (String[]) s.values["SEC.MEETING"]
            if (csmIds) {
                r.meetings = secMeeting?.findAll { i -> csmIds.contains(i.key) }
            }

            // add in xlist
            def xlistId = (String) s.values["SEC.XLIST"]
            if (xlistId) {
                r.xlist = secXlists?.find { i -> i.key == xlistId }

                // add in xlist section synonyms
                def sIds = (String[]) r.xlist?.values?.get("CSXL.COURSE.SECTIONS")
                if (sIds) {
                    r.xlistSections = secSynonyms?.findAll { sIds.contains(it.key) }
                }
            }

            result.sectionData << r
        }

        return result
    }


    /**
     * Parse / convert / filter Colleague data
     */
    List<CASection> parse(String misCode, ReadResult readResult) {
        def result = []

        def castMisParmsUtil = readResult.castMisParmsUtil
        def sectionStatuses = readResult.sectionStatuses
        def buildings = readResult.buildings
        def locations = readResult.locations
        def xf01Translation = readResult.xf01Translation

        for(def data : readResult.sectionData) {
            def s = data.section

            // filter out sections that aren't part of this college
            if (castMisParmsUtil?.castMisParms?.multiCollege && !castMisParmsUtil.checkLocation(misCode, (String) s.values["SEC.LOCATION"])) {
                continue
            }

            def wDate = ((LocalDate) s.values["SEC.OVR.DROP.GR.REQD.DATE"])?.minusDays(1)
            def cDate = ((LocalDate[]) s.values["SEC.OVR.CENSUS.DATES"])?.first()
            def loc = locations?.find { i -> i.key == s.values["SEC.LOCATION"] }
            def vc = sectionStatuses?.entries?.find { i -> i.internalCode == ((String[]) s.values["SEC.STATUS"])?.first() }

            def status
            switch (vc?.action1) {
                case "1":
                    status = SectionStatus.Active
                    break
                case "2":
                    status = SectionStatus.Cancelled
                    break
                default:
                    status = SectionStatus.Pending
            }

            def section = new CASection.Builder()
                    .misCode(misCode)
                    .sisCourseId((String) s.values["SEC.SUBJECT"] + "-" + s.values["SEC.COURSE.NO"])
                    .sisSectionId((String) s.values["SEC.SYNONYM"])
                    .sisTermId((String) s.values["SEC.TERM"])
                    .maxEnrollments((Integer) s.values["SEC.CAPACITY"])
                    .maxWaitlist((Integer) s.values["SEC.WAITLIST.MAX"])
                    .minimumUnits(((BigDecimal) s.values["SEC.MIN.CRED"])?.floatValue())
                    .maximumUnits(((BigDecimal) s.values["SEC.MAX.CRED"])?.floatValue())
                    .weeksOfInstruction((Integer) s.values["SEC.NO.WEEKS"])
                    .start(fromLocalDate((LocalDate) s.values["SEC.START.DATE"]))
                    .end(fromLocalDate((LocalDate) s.values["SEC.END.DATE"]))
                    .preRegistrationStart(fromLocalDate((LocalDate) s.values["SEC.OVR.PREREG.START.DATE"]))
                    .preRegistrationEnd(fromLocalDate((LocalDate) s.values["SEC.OVR.PREREG.END.DATE"]))
                    .registrationStart(fromLocalDate((LocalDate) s.values["SEC.OVR.REG.START.DATE"]))
                    .registrationEnd(fromLocalDate((LocalDate) s.values["SEC.OVR.REG.END.DATE"]))
                    .addDeadline(fromLocalDate((LocalDate) s.values["SEC.OVR.ADD.END.DATE"]))
                    .dropDeadline(fromLocalDate((LocalDate) s.values["SEC.OVR.DROP.END.DATE"]))
                    .withdrawalDeadline(fromLocalDate(wDate))
                    .censusDate(fromLocalDate(cDate))
                    .feeDeadline(null) // not supported
                    .title((String) s.values["SEC.SHORT.TITLE"])
                    .status(status)
                    .campus((String) loc?.values?.get("LOC.DESC"))
                    .build()

            if (section.maximumUnits == null)
                section.maximumUnits = section.minimumUnits

            section.instructors = data.persons?.collect { parseInstructor(it) }
            section.meetingTimes = data.meetings?.collect { parseMeetingTime(it, buildings, locations, xf01Translation) }
            section.crosslistingDetail = parseCrosslisting(s, data.xlist, data.xlistSections)

            result << section
        }

        return result
    }


    /**
     * Sort by sisCourseId, sisSectionId
     */
    List<CASection> sort(List<CASection> sections) {
        return sections.sort { x, y ->
            if (x.sisCourseId != y.sisCourseId)
                return x.sisCourseId <=> y.sisCourseId

            if (x.sisSectionId != y.sisSectionId)
                return x.sisSectionId <=> y.sisSectionId
        }
    }


    /**
     * Create an Instructor object from PERSON data
     */
    Instructor parseInstructor(ColleagueData person) {
        // use "preferred" email or first email listed (or none if there ain't no emails!)
        def emails =(String[]) person.values["PERSON.EMAIL.ADDRESSES"]
        def index = ((String[]) person.values["PERSON.PREFERRED.EMAIL"])?.findIndexOf { it == "Y" } ?: 0
        def email = getAt(emails, index)

        def i = new Instructor.Builder()
                .sisPersonId(person.key)
                .firstName((String) person.values["FIRST.NAME"])
                .lastName((String) person.values["LAST.NAME"])
                .emailAddress(email)
                .build()

        return i
    }


    /**
     * Create a meeting time from COURSE.SEC.MEETING data
     */
    MeetingTime parseMeetingTime(ColleagueData meeting, List<ColleagueData> buildings, List<ColleagueData> locations,
                                 ElfTranslateTable xf01Translation) {
        def v = meeting.values

        // translate instructional method through XF01 to determine lecture, lab or other
        def instructionalMethod
        def xf01 = xf01Translation?.asMap()?.get((String) v["CSM.INSTR.METHOD"])?.newCode
        if (xf01 == "02") instructionalMethod = InstructionalMethod.Lecture
        else if (xf01 == "04") instructionalMethod = InstructionalMethod.Lab
        else instructionalMethod = InstructionalMethod.Other

        // get building location
        def building = buildings?.find { i -> i.key == v["CSM.BLDG"] }
        def location = locations?.find { i -> i.key == building?.values?.get("BLDG.LOCATION") }

        def m = new MeetingTime.Builder()
                .monday(v["CSM.MONDAY"] == "Y")
                .tuesday(v["CSM.TUESDAY"] == "Y")
                .wednesday(v["CSM.WEDNESDAY"] == "Y")
                .thursday(v["CSM.THURSDAY"] == "Y")
                .friday(v["CSM.FRIDAY"] == "Y")
                .saturday(v["CSM.SATURDAY"] == "Y")
                .sunday(v["CSM.SUNDAY"] == "Y")
                .start(fromLocalDate((LocalDate) v["CSM.START.DATE"]))
                .end(fromLocalDate((LocalDate) v["CSM.END.DATE"]))
                .startTime(fromLocalTime((LocalTime) v["CSM.START.TIME"]))
                .endTime(fromLocalTime((LocalTime) v["CSM.END.TIME"]))
                .building((String) v["CSM.BLDG"])
                .room((String) v["CSM.ROOM"])
                .instructionalMethod(instructionalMethod)
                .campus((String) location?.values?.get("LOC.DESC"))
                .build()

        return m
    }


    /**
     * Parse crosslisting data
     */
    CrosslistingDetail parseCrosslisting(ColleagueData section, ColleagueData xlist, List<ColleagueData> sections) {
        if (!xlist || !sections) return null

        CrosslistingDetail result = new CrosslistingDetail()

        result.name = xlist.key // name is arbitrary. to keep it unique we use the primary key of the crosslisting.
        result.sisTermId = (String) section.values["SEC.TERM"]
        result.primarySisSectionId = (String) sections.find { i -> i.key == xlist.values["CSXL.PRIMARY.SECTION"] }?.values?.get("SEC.SYNONYM")
        result.sisSectionIds = sections.collect { i -> (String) i.values["SEC.SYNONYM"] }

        // don't return bad data (no primary or less than two sections in crosslisting)
        if (result.primarySisSectionId == null || result.sisSectionIds.size() < 2)
            return null

        return result
    }


    /**
     * All data read and grouped by section
     */
    private static class ReadResult {
        List<SectionData> sectionData

        // translations / cached data
        Valcode sectionStatuses
        CastMisParmsUtil castMisParmsUtil
        List<ColleagueData> locations
        List<ColleagueData> buildings
        ElfTranslateTable xf01Translation
    }

    /**
     * Data associated with one section
     */
    private static class SectionData {
        ColleagueData section
        List<ColleagueData> persons
        List<ColleagueData> meetings
        ColleagueData xlist
        List<ColleagueData> xlistSections
    }
}
