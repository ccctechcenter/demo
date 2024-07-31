package api.test

import com.ccctc.adaptor.util.impl.GroovyServiceImpl
import com.fasterxml.jackson.databind.ObjectMapper
import groovy.json.JsonSlurper
import com.ccctc.adaptor.model.CourseContact
import com.ccctc.adaptor.model.TransferStatus
import com.ccctc.adaptor.model.CreditStatus
import com.ccctc.adaptor.model.GradingMethod
import com.ccctc.adaptor.model.InstructionalMethod


public class Course {
    def environment

// Get a course. term is optional.
    def get(String misCode, String name, String term) {

        def user = (environment.getProperty("sisuser"))
        def password = (environment.getProperty("sispassword"))
        System.out.println("User=" + user)
        System.out.println("Password=" + password)
        System.out.println("Name=" + name)
        System.out.println("term=" + term)
        System.out.println("misCode=" + misCode)
        System.out.println("sisUser=" + environment.getProperty("sisType"))
        System.out.println("college-adaptor.var=" + environment.getProperty("college-adaptor.var"))
        if (name.equals("eng101")) { // read json from a file
            return new ObjectMapper().readValue(new File("api/test/" + name + ".json"), com.ccctc.adaptor.model.Course.class)
        } else if (name.equals("math100")) { // use the Builder pattern to create a Course object
            builder = new com.ccctc.adaptor.model.Course.Builder();
            return builder.id(1).subject("ENG").number("101").description(name).build()
        } else if (name.equals("csc100")) { // make a REST call then use the builder to create a Course object
            def client = new URL('https://cvc.edu/courses.json?id=62612')
            def connection = client.openConnection()
            if (connection.responseCode == 200) {
                def jsonSlurper = new JsonSlurper()
                def responseText = connection.content.text
                def response = jsonSlurper.parseText(responseText)
                System.out.println(responseText)
                System.out.println(response)
                builder = new com.ccctc.adaptor.model.Course.Builder();
                builder
                        .sisCourseId("" + response.id[0])
                        .subject(response.course[0].split(" ")[0])
                        .number(response.course[0].split(" ")[1])
                        .description(response.description)
                        .maximumUnits(Float.parseFloat(response.units[0]))
                        .minimumUnits(Float.parseFloat(response.units[0]))
                return builder.build()
            }
        } else {

            // Construct the URL
            def url = (term != null) ?
                    "course/" + misCode + "/" + name.toUpperCase() + "/" + term.toUpperCase() :
                    "course/current/" + misCode + "/" + name.toUpperCase()

            // Perform the call to the Colleague API
            String[] searchPath = ["./api/colleague/"];
            Object[] args = [url, user, password];
            response = new GroovyServiceImpl().run(searchPath, "Util", "ColleagueRequest", args)

            // Parse the response
            if (response != null) {

                // Use first result (all responses from the Colleague API are arrays)
                response = response[0]

                // Extract out course contacts into the appropriate classes
                def contacts = []
                for (contact in response.courseContacts) {
                    ccbuilder = new CourseContact.Builder()
                    ccbuilder
                            .instructionalMethod(InstructionalMethod.valueOf(contact.instructionalMethod))
                            .hours(contact.hours)
                    contacts += ccbuilder.build()
                }

                // Build course record for return
                builder = new com.ccctc.adaptor.model.Course.Builder();
                builder
                        .misCode(misCode)
                        .sisCourseId(response.sisCourseId)
                        .sisTermId(term) // Term is not returned by Colleague, just return what was passed
                        .c_id(response.c_id)
                        .controlNumber(response.controlNumber)
                        .subject(response.subject)
                        .number(response.number)
                        .title(response.title)
                        .description(response.description)
                        .outline(response.outline)
                        .prerequisites(response.prerequisites)
                        .corequisites(response.corequisites)
                        .minimumUnits(response.minimumUnits)
                        .maximumUnits(response.maximumUnits)
                        .transferStatus(TransferStatus.valueOf(response.transferStatus))
                        .creditStatus(CreditStatus.valueOf(response.creditStatus))
                        .gradingMethod(GradingMethod.valueOf(response.gradingMethod))
                        .courseContacts(contacts)
                        .fee(response.fee)

                return builder.build()
            }
        }
        return null
    }


}