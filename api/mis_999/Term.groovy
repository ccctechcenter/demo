package api.mis_999

import com.ccctc.adaptor.model.TermSession
import com.ccctc.adaptor.model.TermType

def environment

def get( String misCode, String term ) {

    user = environment.getProperty("sisuser")
    password = environment.getProperty("sispassword")
    System.out.println( "User=" + user )
    System.out.println( "Password=" + password )
    System.out.println( "MisCode=" + misCode )
    System.out.println( "term=" + term )

    builder = new com.ccctc.adaptor.model.Term.Builder();
    builder.misCode(misCode)
    .addDeadline(new Date(2016-1900,01,23,12,12))
    .censusDate(new Date(2016-1900,01,23,12,12))
    .description("Term Description")
    .dropDeadline(new Date(2016-1900,01,23,12,12))
    .end(new Date(2016-1900,01,23,12,12))
    .feeDeadline(new Date(2016-1900,01,23,12,12))
    .preRegistrationEnd(new Date(2016-1900,01,23,12,12))
    .registrationStart(new Date(2016-1900,01,23,12,12))
    .sisTermId(term)
    .start(new Date(2016-1900,01,23,12,12))
    .type(TermType.Semester)
    .registrationEnd(new Date(2016-1900,01,23,12,12))
    .withdrawalDeadline(new Date(2016-1900,01,23,12,12))
    .year(2016)
    .session(TermSession.Spring)

    return builder.build()

    return null
}

