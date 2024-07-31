/**
 * Created by Rasul on 1/18/2016.
 */
package api.banner

import com.ccctc.adaptor.exception.EntityNotFoundException

import com.ccctc.adaptor.exception.InvalidRequestException
import com.ccctc.adaptor.model.CrosslistingDetail
import com.ccctc.adaptor.model.InstructionalMethod
import com.ccctc.adaptor.model.Instructor
import com.ccctc.adaptor.model.MeetingTime
import com.ccctc.adaptor.model.Section
import com.ccctc.adaptor.model.SectionStatus
import com.ccctc.adaptor.util.MisEnvironment
import groovy.sql.Sql
import org.springframework.core.env.Environment

class Section {

    Environment environment

    def get(String misCode, String sisSectionId, String sisTermId) {
        // Use JDBC to make a package call or sql query
        Sql sql = BannerConnection.getSession(environment, misCode)
        try {
            //Section
            def query = MisEnvironment.getProperty(environment, misCode, "banner.section.getQuery")
            def response = sql.rows(query, ["sisSectionId": sisSectionId, "sisTermId": sisTermId])
            if (!response.size()) {
                throw new EntityNotFoundException("Search returned no results.")
            }
            buildSections(misCode, response, sql)[0]
        } finally {
            sql.close()
        }
    }

    def getAll(String misCode, String sisTermId, String sisCourseId) {
        // Use JDBC to make a package call or sql query
        Sql sql = BannerConnection.getSession(environment, misCode)
        try {
            def query
            def sections
            if (sisTermId && !sisCourseId) {
                query = MisEnvironment.getProperty(environment, misCode, "banner.section.listByTerm.getQuery")
                sections = sql.rows(query, ["sisTermId": sisTermId])
            } else if (sisCourseId) {
                def courseId = sisCourseId.split('-')
                if (courseId.size() < 2) {
                    throw new InvalidRequestException(InvalidRequestException.Errors.invalidSearchCriteria, "Invalid sisCourseId provided")
                }
                query = MisEnvironment.getProperty(environment, misCode, "banner.section.listByCourse.getQuery")
                sections = sql.rows(query, ["sisTermId": sisTermId, "subject": courseId[0], "number": courseId[1]])
            } else {
                throw new InvalidRequestException(InvalidRequestException.Errors.invalidSearchCriteria, "A sisTermId or sisCourseId must be provided.")
            }
            buildSections(misCode, sections, sql)
        } finally {
            sql.close()
        }
    }

    def search( String misCode, String sisTermId, String [] words ) {
        def searchQuery = MisEnvironment.getProperty(environment, misCode, "banner.section.search.getQuery")
        def searchParams = [sisTermId:sisTermId]
        for(def i = 0; i < words.size(); i++) {
            if( i > 0 ) { searchQuery += " AND" }
            searchQuery += " (sisCourseId LIKE :word" + i + " OR sisSectionId LIKE :word" + i + " OR firstName LIKE :word" + i +
                    " OR lastName LIKE :word" + i + " OR spriden_id LIKE :word" + i + " OR building LIKE :word" + i +
                    " OR campus LIKE :word" + i + " OR room LIKE :word" + i
            searchParams["word" + i] = '%' + words[i] + '%'
            if( words[i].length() >= 5) { searchQuery += " OR title LIKE :word" + i }
            searchQuery += ")"
        }
        Sql sql = BannerConnection.getSession(environment, misCode)
        try {
            def sectionsToGet = sql.rows(searchQuery, searchParams)
            def query = MisEnvironment.getProperty(environment, misCode, "banner.section.listById.getQuery")
            def sections = []
            def sectionPartition = []
            for(def i=0; i < sectionsToGet.size; i++) {
                sectionPartition << sectionsToGet[i]
                if ((i % 500 == 0) || (i + 1) == sectionsToGet.size()) {
                    sections.addAll(sql.rows(buildInQuery(buildInString(sectionPartition), query), buildInMap(sectionPartition)))
                }
            }
            return buildSections(misCode, sections, sql)
        } finally {
            sql?.close()
        }
    }

    def buildSections(String misCode, def sections, Sql sql) {

        def sectionList = [:]
        sections.each { section ->
            buildSection(section, sectionList)
        }

        def titleQuery = MisEnvironment.getProperty(environment, misCode, "banner.section.titleList.getQuery")
        // These three must be ordered by term, crn
        def instructorQuery = MisEnvironment.getProperty(environment, misCode, "banner.section.instructorList.getQuery")
        def meetingQuery = MisEnvironment.getProperty(environment, misCode, "banner.section.meetingList.getQuery")
        def crosslistQuery = MisEnvironment.getProperty(environment, misCode, "banner.section.crosslist.getQuery")
        def listParams = []
        sectionList.eachWithIndex { sectionItem, i ->
            listParams << sectionItem.value.section
            if ((i % 500 == 0) || (i + 1) == sectionList.size()) {
                def inList = buildInString(listParams)
                def params = buildInMap(listParams)

                //Meetings
                def sectionMeetings = []
                def currentSectionId, previousSectionId
                sql.rows(buildInQuery(inList, meetingQuery), params).each { meeting ->
                    currentSectionId = meeting.sisTermId + '-' + meeting.sisSectionId
                    if (!previousSectionId) previousSectionId = currentSectionId
                    if (previousSectionId != currentSectionId) {
                        buildMeetings(sectionMeetings, sectionList[previousSectionId], misCode)
                        sectionMeetings = []
                        sectionMeetings << meeting
                        previousSectionId = currentSectionId
                    } else {
                        sectionMeetings << meeting
                    }
                }
                if (sectionMeetings.size()) buildMeetings(sectionMeetings, sectionList[currentSectionId], misCode)

                //Titles
                sql.eachRow(buildInQuery(inList, titleQuery), params) { title ->
                    sectionList[title.sisTermId + '-' + title.sisSectionId].section.title(title.title)
                }

                if( crosslistQuery ) { // Backwards compatibility
                // Cross Listings
                    def sectionCrossListings = []
                    currentSectionId = previousSectionId = null
                    sql.rows(buildInQuery(inList, crosslistQuery), params).each { crosslist ->
                        currentSectionId = crosslist.sisTermId + '-' + crosslist.sisSectionId
                        if (!previousSectionId) previousSectionId = currentSectionId
                        if (previousSectionId != currentSectionId) {
                            buildCrosslist(sectionCrossListings, sectionList[previousSectionId])
                            sectionCrossListings = []
                            sectionCrossListings << crosslist
                            previousSectionId = currentSectionId
                        } else {
                            sectionCrossListings << crosslist
                        }
                    }
                    if (sectionCrossListings.size()) buildCrosslist(sectionCrossListings, sectionList[currentSectionId])
                }

                //Instructors
                def sectionInstructors = []
                currentSectionId = previousSectionId = null
                params["emailCode"] = MisEnvironment.getProperty(environment, misCode, "banner.section.instructor.emailCode")
                sql.rows(buildInQuery(inList, instructorQuery), params).each { instructor ->
                    currentSectionId = instructor.sisTermId + '-' + instructor.sisSectionId
                    if (!previousSectionId) previousSectionId = currentSectionId
                    if (previousSectionId != currentSectionId) {
                        buildInstructors(sectionInstructors, sectionList[previousSectionId])
                        sectionInstructors = []
                        sectionInstructors << instructor
                        previousSectionId = currentSectionId
                    } else {
                        sectionInstructors << instructor
                    }

                }
                if (sectionInstructors.size()) buildInstructors(sectionInstructors, sectionList[currentSectionId])

                listParams = []
            }
        }

        def completedSections = []
        sectionList.each { section ->
            completedSections << section.value.section.build()
        }
        return completedSections
    }

    def buildSection(def section, Map sectionList) {
        def builder = new Section.Builder()
        builder.sisSectionId(section.sisSectionId)
                .sisTermId(section.sisTermId)
                .sisCourseId(section.sisCourseId)
                .maxEnrollments(section.maxEnrollments?.intValueExact())
                .maxWaitlist(section.maxWaitlist?.intValueExact())
                .minimumUnits(section.minimumUnits)
                .maximumUnits(section.maximumUnits)
                .weeksOfInstruction(section.weeksOfInstruction?.intValueExact())
                .campus(section.campus)
                .start(section.start)
                .end(section.end)
        // .preRegistrationStart(section?.preRegistrationStart)
        // .preRegistrationEnd(section?.preRegistrationEnd)
                .registrationStart(section.registrationStart)
                .registrationEnd(section.registrationEnd)
                .addDeadline(section.addDeadline)
                .dropDeadline(section.dropDeadline)
                .withdrawalDeadline(section.withdrawalDeadline)
                .feeDeadline(section.feeDeadline)
                .censusDate(section.censusDate)
                .status(section.status == 'Y' ? SectionStatus.Active : SectionStatus.Cancelled)
        sectionList[section.sisTermId + '-' + section.sisSectionId] =
                ["section": builder, "instructionalMethod": section.instructionalMethod]

    }

    def buildInstructors(def instructors, Map section) {
        def instructorList = []
        instructors.each { instructor ->
            def builder = new Instructor.Builder()
            builder.firstName(instructor?.firstName)
                    .lastName(instructor?.lastName)
                    .emailAddress(instructor?.emailAddress)
                    .sisPersonId(instructor?.sisPersonId)
            instructorList << builder.build()
        }
        section.section.instructors(instructorList)
    }

    def buildMeetings(def meetings, Map section, String misCode) {
        def meetingList = []
        meetings.each { meeting ->
            def builder = new MeetingTime.Builder()
            builder.monday(meeting.monday ? true : false)
                    .tuesday(meeting.tuesday ? true : false)
                    .wednesday(meeting.wednesday ? true : false)
                    .thursday(meeting.thursday ? true : false)
                    .friday(meeting.friday ? true : false)
                    .saturday(meeting.saturday ? true : false)
                    .sunday(meeting.sunday ? true : false)
                    .start(meeting.start)
                    .end(meeting.end)
                    .startTime(getDateTime(meeting.start, meeting.startTime))
                    .endTime(getDateTime(meeting.start, meeting.endTime))
                    .instructionalMethod(meeting.instructionalMethod ?
                    getInstructionalMethod(misCode, meeting.instructionalMethod) :
                    getInstructionalMethod(misCode, section.instructionalMethod))
                    .campus(meeting.campus)
                    .building(meeting.building)
                    .room(meeting.room)
            meetingList << builder.build()
        }
        section.section.meetingTimes(meetingList)
    }

    def buildCrosslist( def crosslistings, Map section ) {
        def primary
        def sectionList = []
        crosslistings.each { crosslist ->
            if(!primary) { primary = crosslist }
            if( crosslist.primary == 'Y' ) {   // A primary with lower sectionId is found
                primary = crosslist
            }
            else if ( primary.primary != 'Y' ) {
                primary = crosslist            // No section is marked as primary yet, but a lower sectionId exists
            }
            sectionList << crosslist.xlistSectionId
        }
        def crossListDetail = new CrosslistingDetail.Builder()
        crossListDetail.sisSectionIds = sectionList
        crossListDetail.name(primary.group)
        crossListDetail.primarySisSectionId(primary.xlistSectionId)
        crossListDetail.sisTermId(primary.xlistTermId)
        section.section.crosslistingDetail(crossListDetail.build())
    }

    def buildInString(List params) {
        def inString = '('
        params.eachWithIndex { param, i ->
            inString += "( :sisTermId" + i + ", :sisSectionId" + i + ") "
            if (i + 1 < params.size())
                inString += ', '
        }
        inString += ')'
        return inString
    }

    def buildInQuery(def inList, def sqlQuery) {
        sqlQuery.replace(':inList', inList)
    }

    def buildInMap(List params) {
        def returnMap = [:]
        params.eachWithIndex { param, i ->
            returnMap["sisTermId" + i] = param.sisTermId
            returnMap["sisSectionId" + i] = param.sisSectionId
        }
        return returnMap
    }

    def getInstructionalMethod(String misCode, String instructionalMethod) {
        switch (instructionalMethod) {
            case { MisEnvironment.checkPropertyMatch(environment, misCode, "banner.instructionalMethod.Lab", it) }:
                return InstructionalMethod.Lab
                break
            case { MisEnvironment.checkPropertyMatch(environment, misCode, "banner.instructionalMethod.Lecture", it) }:
                return InstructionalMethod.Lecture
                break
            case {
                MisEnvironment.checkPropertyMatch(environment, misCode, "banner.instructionalMethod.LectureLab", it)
            }:
                return InstructionalMethod.LectureLab
                break
            default:
                return InstructionalMethod.Other
        }
    }

    def getDateTime(Date date, String time) {
        if (!time) return date
        def hours, minutes
        hours = time.substring(0, 1)
        minutes = time.substring(2)
        Calendar cal = Calendar.getInstance()
        cal.setTime(date)
        cal.add(Calendar.HOUR_OF_DAY, Integer.parseInt(hours))
        cal.add(Calendar.MINUTE, Integer.parseInt(minutes))
        cal.getTime()
    }

}