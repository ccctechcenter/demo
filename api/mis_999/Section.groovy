package api.mis_999

import com.ccctc.adaptor.model.Instructor

def environment

def get(String sisSectionId, String sisTermId, String sisCourseId, String misCode) {

    user = environment.getProperty("sisuser")
    password = environment.getProperty("sispassword")
    System.out.println( "User=" + user )
    System.out.println( "Password=" + password )
    System.out.println( "MisCode=" + misCode )
    System.out.println( "term=" + sisTermId )
    System.out.println( "course=" + sisCourseId)
    System.out.println( "section=" + sisSectionId)

    instructorBuilder = new com.ccctc.adaptor.model.Instructor.Builder();
    instructorBuilder.firstName("Fred")
        .lastName("Flinstone")
        .emailAddress("FredFlinstone@bedrock.com");
    List<Instructor> instructorList = new ArrayList<>();
    instructorList.add(instructorBuilder.build())

    builder = new com.ccctc.adaptor.model.Section.Builder();
    builder
    .sisSectionId(sisSectionId)
    .sisTermId(sisTermId)
    .sisCourseId(sisCourseId)
    .instructors(instructorList)
    .maxEnrollments(10)
    .maxWaitlist(12)
    .minimumUnits(9)
    .maximumUnits(11)
    //.meetingTimes()
    .weeksOfInstruction(20)
    .campus("Some Campus")
    .start(new Date(2016-1900,01,23,12,12))
    .end(new Date(2016-1900,01,23,12,12))
    .preRegistrationStart(new Date(2016-1900,01,23,12,12))
    .preRegistrationEnd(new Date(2016-1900,01,23,12,12))
    .registrationStart(new Date(2016-1900,01,23,12,12))
    .registrationEnd(new Date(2016-1900,01,23,12,12))
    .addDeadline(new Date(2016-1900,01,23,12,12))
    .dropDeadline(new Date(2016-1900,01,23,12,12))
    .withdrawalDeadline(new Date(2016-1900,01,23,12,12))
    .feeDeadline(new Date(2016-1900,01,23,12,12))
    .censusDate(new Date(2016-1900,01,23,12,12))
    return builder.build()

}

