package api.mock

import com.ccctc.adaptor.model.mock.SectionDB
import org.springframework.core.env.Environment

class Section {
    Environment environment
    // injected using GroovyServiceImpl on creation of class
    private SectionDB sectionDB

    def get(String misCode, String sisSectionId, String sisTermId) {
        return sectionDB.get(misCode, sisTermId, sisSectionId)
    }

    def getAll(String misCode, String sisTermId, String sisCourseId) {
        def map = [misCode: misCode]
        if (sisTermId) map.put("sisTermId", sisTermId)
        if (sisCourseId) map.put("sisCourseId", sisCourseId)

        return sectionDB.findSorted(map)
    }

    def search(String misCode, String sisTermId, String[] words) {
        def map = [misCode: misCode]
        if (sisTermId) map.put("sisTermId", sisTermId)

        def sections = sectionDB.findSorted(map)

        for (String word : words) {
            word = word.toLowerCase()

            sections = sections.findAll { i ->
                // construct list of terms to search for
                def terms = new ArrayList<String>()
                if (i.sisCourseId) terms.add(i.sisCourseId.toLowerCase())
                if (i.sisSectionId) terms.add(i.sisSectionId.toLowerCase())

                if (i.instructors) {
                    i.instructors.each { j ->
                        if (j.firstName) terms.add(j.firstName.toLowerCase())
                        if (j.lastName) terms.add(j.lastName.toLowerCase())
                        if (j.sisPersonId) terms.add(j.sisPersonId.toLowerCase())
                    }
                }

                if (i.meetingTimes) {
                    i.meetingTimes.each { j ->
                        if (j.building) terms.add(j.building.toLowerCase())
                        if (j.campus) terms.add(j.campus.toLowerCase())
                        if (j.room) terms.add(j.room.toLowerCase())
                    }
                }

                // split out any terms that have a non-word character and add to list
                terms.addAll(terms.collect { it.split(/\W/) }.flatten())
                terms = terms.unique()

                if (terms.find { j -> j == word }) return true

                // if we didn't find an exact match and the word >= 5 characters, search anywhere in title
                if (word.length() >= 5 && i.title && i.title.toLowerCase().contains(word)) return true
            }
        }

        return sections
    }
}