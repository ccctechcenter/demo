package api.banner

import com.ccctc.adaptor.exception.InvalidRequestException
import com.ccctc.adaptor.model.TermType
import com.ccctc.adaptor.model.transcript.AcademicGPACreditUnits
import com.ccctc.adaptor.model.transcript.SchoolCodeTypes
import com.ccctc.adaptor.util.MisEnvironment
import com.ccctc.adaptor.util.impl.TranscriptService
import com.ccctc.adaptor.util.transcript.CollegeTranscriptBuilder
import com.ccctc.core.coremain.v1_14.*
import com.ccctc.core.coremain.v1_14.impl.UserDefinedExtensionsTypeImpl
import com.ccctc.message.collegetranscript.v1_6.impl.CollegeTranscriptImpl
import com.ccctc.sector.academicrecord.v1_9.*
import com.ccctc.sector.academicrecord.v1_9.impl.AcademicRecordTypeImpl
import com.xap.ccctran.AcademicRecordCCCExtensions
import com.xap.ccctran.CourseCCCExtensions
import groovy.sql.Sql
import org.springframework.core.env.Environment

import java.sql.SQLException
import java.text.SimpleDateFormat

class Transcript {

    Environment environment

    def get(String misCode,
            String firstName,
            String lastName,
            String birthDate,
            String ssn,
            String partialSSN,
            String emailAddress,
            String schoolAssignedStudentID,
            String cccID,
            TranscriptService transcriptService) {
        CollegeTranscriptBuilder builder = new CollegeTranscriptBuilder(transcriptService.getCccJAXBContext());

        Sql sql = BannerConnection.getSession(environment, misCode)
        try {

            def query = MisEnvironment.getProperty(environment, misCode, "banner.transcript.student.person.getQuery")

            //Convert the first and last name to lower case.  The SQL used to search uses a SQL function to
            //search using lower case.
            if (firstName != null) {
                firstName = firstName.toLowerCase();
            }
            if (lastName != null) {
                lastName = lastName.toLowerCase();
            }
            def student = sql.firstRow(query, [
                    firstName:firstName,
                    lastName:lastName,
                    birthDate:birthDate,
                    ssn:ssn,
                    partialSSN: partialSSN != null ? "%"+partialSSN : partialSSN,
                    emailAddress:emailAddress,
                    studentID: schoolAssignedStudentID,
                    cccID: cccID])
            if(!student) {
                throw new InvalidRequestException(InvalidRequestException.Errors.studentNotFound, "Could not find the student with the provided parameters.")
            }
            setStudentProperties(misCode, student, sql, builder)

            query = MisEnvironment.getProperty(environment, misCode, "banner.transcript.college.identity.getQuery")
            def collegeIdentities = sql.rows(query)

            if(!collegeIdentities) {
                throw new InvalidRequestException(InvalidRequestException.Errors.studentNotFound, "Could not find the student with the provided parameters.")
            }

            query = MisEnvironment.getProperty(environment, misCode, "banner.transcript.student.sessions.getQuery")
            def sessionList = sql.rows(query, [pidm:student.PIDM])
            if (!sessionList) {
                throw new InvalidRequestException(InvalidRequestException.Errors.studentNotFound, "Could not find any student sessions.")
            }



            AcademicRecordType academicRecord = builder.createAcademicRecord(convertBannerClassLevelToPESC(sessionList.last().classLevel));


            def recordHolderFICE = MisEnvironment.getProperty(environment, misCode, "fice")

            def recordHolderSchool = null

            for(school in collegeIdentities){
                if (school.fice == recordHolderFICE) {
                    recordHolderSchool = school
                    com.ccctc.sector.academicrecord.v1_9.SchoolType schoolType = builder.createSchool()
                    schoolType.setOrganizationName((String)school.name)
                    schoolType.setFICE((String)school.fice)
                    academicRecord.setSchool(schoolType)
                }
            }

            query = MisEnvironment.getProperty(environment, misCode, "banner.transcript.student.person.address.getQuery")
            def studentAddress = sql.firstRow(query, [pidm:student.PIDM])
            if(!studentAddress) {
                throw new InvalidRequestException(InvalidRequestException.Errors.studentNotFound, "Could not find the student address.")
            }
            setStudentAddressProperties(studentAddress, builder)


            academicRecord.setStudentLevel()

            query =  MisEnvironment.getProperty(environment, misCode, "banner.transcript.courses.getQuery")
            def courseList = sql.rows(query, [pidm:student.PIDM])

            if (!courseList) {
                throw new InvalidRequestException(InvalidRequestException.Errors.studentNotFound, "Unable to retrieve course information.")
            }


            def cumulativeGPA = setStudentSessions(recordHolderSchool, collegeIdentities, courseList, sessionList, academicRecord, builder)


            def gradeSchemesCodeGroup = MisEnvironment.getProperty(environment,misCode, "transcript.grade.schemes.codegroup")
            query = MisEnvironment.getProperty(environment, misCode, "banner.transcript.grade.scheme.getQuery")
            def gradeScheme = sql.rows(query, [group:gradeSchemesCodeGroup])

            addGradeScheme(gradeScheme, academicRecord, builder)


            query = MisEnvironment.getProperty(environment, misCode, "banner.transcript.student.awards.getQuery")
            def awards = sql.rows(query, [pidm:student.pidm])

            setAcademicAwards(awards, academicRecord, builder)

            query = MisEnvironment.getProperty(environment, misCode, "banner.transcript.student.degrees.getQuery")

            def degrees = sql.rows(query,  [pidm:student.pidm])
            setAcademicDegrees(degrees, academicRecord, builder )

            setAcademicSummary(cumulativeGPA, academicRecord, builder)
            builder.getAcademicRecords().add(academicRecord);
        } finally {
            sql.close()
        }

        return builder.build()
    }

    def convertBannerAwardLevelToPESC(String awardLevel) {
        if ("AA".equals(awardLevel) || "AS".equals(awardLevel)){
            return "2.3"
        }
        return null
    }

    def setAcademicDegrees(degrees, AcademicRecordType academicRecord, CollegeTranscriptBuilder builder) {
        SimpleDateFormat format = new SimpleDateFormat("yyyyMMdd")
        for(degree in degrees) {
            AcademicAwardType award = builder.createAcademicAward((String)degree.degreeTitle,
                    convertBannerAwardLevelToPESC((String)degree.degreeType), format.parse((String)degree.degreeDate), degree.completionIndicator == 1,
            format.parse((String)degree.degreeDate))

            academicRecord.getAcademicAwards().add(award)
        }
    }

    def setAcademicAwards(awards, AcademicRecordType academicRecord, CollegeTranscriptBuilder builder) {

        RAPType rap = builder.createRAPType()
        com.ccctc.core.coremain.v1_14.AdditionalStudentAchievementsType awardType = builder.createAdditionalStudentAchievement()

        for(award in awards){

            rap.getRAPNames().add((String)award.awardTitle)

        }

        awardType.getProficiencies().add(rap)
        academicRecord.getAdditionalStudentAchievements().add(awardType)

    }

    def addSchool(collegeIdentity, AcademicRecordType academicRecord, CollegeTranscriptBuilder transcriptBuilder) {
        SchoolType recordHolderSchool = transcriptBuilder.createSchool((String)collegeIdentity.name,
                (String)collegeIdentity.fice,
                SchoolCodeTypes.FICE);
        academicRecord.setSchool(recordHolderSchool)
    }

    def addGradeScheme(gradeScheme, AcademicRecordType academicRecord, CollegeTranscriptBuilder transcriptBuilder) {
        AcademicRecordCCCExtensions extensions = transcriptBuilder.createAcademicRecordExtensions()
        ArrayList<AcademicRecordCCCExtensions.GradeSchemeEntry> gradeSchemes = extensions.getGradeSchemeEntries()
        for(gradeSchemeRow in gradeScheme) {
            AcademicRecordCCCExtensions.GradeSchemeEntry gradeSchemeEntry = transcriptBuilder.createGradeSchemeEntry()

            gradeSchemeEntry.setDescription((String)gradeSchemeRow.gradeDescription)
            gradeSchemeEntry.setGrade((String)gradeSchemeRow.gradeCode)
            gradeSchemeEntry.setGradePoints(new BigDecimal((double)gradeSchemeRow.gradePoints))
            gradeSchemeEntry.setEDIGradeQualifier((String)gradeSchemeRow.gradeScaleCode)

            if (gradeSchemeRow.attemptedUnitsCalc) {
                def v = transcriptBuilder.convertAttemptedUnitsCalcFROMCCCASCIIToPESC((char)gradeSchemeRow.attemptedUnitsCalc)
                gradeSchemeEntry.setAttemptedUnitsCalc(AcademicRecordCCCExtensions.GradeSchemeEntry.AttemptedUnitsCalcType.fromValue(v))
            }
            if (gradeSchemeRow.earnedUnitsCalc) {
                def v = transcriptBuilder.convertAttemptedUnitsCalcFROMCCCASCIIToPESC((char)gradeSchemeRow.earnedUnitsCalc)
                gradeSchemeEntry.setEarnedUnitsCalc(AcademicRecordCCCExtensions.GradeSchemeEntry.EarnedUnitsCalcType.fromValue(v))
            }
            if (gradeSchemeRow.gradePointsCalc) {
                def v = transcriptBuilder.convertAttemptedUnitsCalcFROMCCCASCIIToPESC((char)gradeSchemeRow.gradePointsCalc)
                gradeSchemeEntry.setGradePointsCalc(AcademicRecordCCCExtensions.GradeSchemeEntry.GradePointsCalcType.fromValue(v))
            }

            SimpleDateFormat format = new SimpleDateFormat("yyyyMMdd")
            gradeSchemeEntry.setEffectiveFromDate(format.parse((String)gradeSchemeRow.effectiveFrom))
            gradeSchemeEntry.setEffectiveToDate(format.parse((String)gradeSchemeRow.effectiveTo))

            gradeSchemes << gradeSchemeEntry
        }
        extensions.gradeSchemeEntries = gradeSchemes

        if (academicRecord.userDefinedExtensions == null)
            academicRecord.userDefinedExtensions = new UserDefinedExtensionsTypeImpl()

        academicRecord.userDefinedExtensions.academicRecordCCCExtensions = extensions
    }

    def post(String misCode, CollegeTranscriptImpl transcript) {
        Sql sql = BannerConnection.getSession(environment, misCode)
        def query = MisEnvironment.getProperty(environment, misCode, "banner.person.sisPersonId.getQuery")
        query = query.replace(":inList", "('${transcript.student.person.schoolAssignedPersonID}')")
        def student = sql.firstRow(query)
        if(!student) {
            throw new InvalidRequestException(InvalidRequestException.Errors.studentNotFound, "Could not find the student with the provided sisPersonId.")
        }
        transcript.student.academicRecords.each { record ->
            def institution = lookupInstitution(record, misCode, sql)
            checkPriorSchool(student, institution, misCode, sql)
            record.courses?.each { course ->
                checkTransferCourse(student, institution, course, misCode, sql)
            }
            record.academicSessions.each { session ->
                session.courses?.each { course ->
                    if(!course.courseBeginDate)
                        course.courseBeginDate = session.academicSessionDetail.sessionBeginDate
                    if(!course.courseEndDate)
                        course.courseEndDate = session.academicSessionDetail.sessionEndDate
                    checkTransferCourse(student, institution, course, misCode, sql)
                }
            }
        }
        return null
    }


    def setStudentProperties(String misCode, def student, Sql sql, CollegeTranscriptBuilder builder) {
        setPersonProperties(misCode, student, sql, builder)
    }

    def convertTermBasis(termBasis) {
        if("S".equalsIgnoreCase(termBasis)) {
            return SessionTypeType.SEMESTER
        }
        return SessionTypeType.OTHER
    }

    def  setAcademicSummary(gpa, AcademicRecordType academicRecord, CollegeTranscriptBuilder builder) {
        //TODO: where to get credit units?
        AcademicSummaryFType summary = builder.createAcademicSummaryF();
        summary.setGPA(builder.constructGpa(gpa.hoursAttempted, gpa.hoursEarned, AcademicGPACreditUnits.Units, gpa.qualityPoints, gpa.gpaHours, 0D, 4.0D ))
        academicRecord.getAcademicSummaries().add(summary)
    }

    def convertBannerCourseCreditUnitsToPESC(String termBasis) {
        if (termBasis == null) {
            return "Unreported"
        }
        else if ("S".equalsIgnoreCase(termBasis)){
            return "Semester"
        }
        else if ("Q".equalsIgnoreCase(termBasis)) {
            return "Quarter"
        }

        return "Other"
    }

    def addCoursesToSession(courseList, session, recordHolderSchool, schools, CollegeTranscriptBuilder transcriptBuilder) {
        for(course in courseList) {

            CourseType courseType = transcriptBuilder.createCourse();

            if (recordHolderSchool.campusCode != course.campusCode) {
                def overrideSchool = findSchool(course.campusCode, schools)
                courseType.setCourseOverrideSchool(transcriptBuilder.createSchool((String)overrideSchool.name, (String) overrideSchool.fice, SchoolCodeTypes.FICE))
            }

            courseType.setCourseCreditBasis(transcriptBuilder.convertCCCASCIICourseCreditBasisToPESC(null))
            courseType.setCourseTitle((String)course.courseTitle)
            courseType.setCourseAcademicGrade((String)course.finalGrade)
            courseType.setCourseAcademicGradeScaleCode((String)course.gradeScaleCode)
            courseType.setCourseCreditEarned(new BigDecimal((double)course.earnedUnits))
            courseType.setCourseCreditValue(new BigDecimal((double)course.gpaUnits))
            courseType.setCourseCreditUnits(convertBannerCourseCreditUnitsToPESC((String)course.termBasis))
            courseType.setCourseQualityPointsEarned(new BigDecimal((double)course.gradePoints))
            courseType.setCourseNumber((String)course.courseNumber)
            courseType.setCourseSubjectAbbreviation((String)course.subject.trim())
            if (course.repeatCourseIndicator) {
                def v = transcriptBuilder.convertCCCASCIIRepeatStatusToPESC((char)course.repeatCourseIndicator)
                courseType.setCourseRepeatCode(CourseRepeatCodeType.fromValue(v))
            }

            if (course.degreeApplicable) {
                def v = transcriptBuilder.convertCCCASCIICoureApplicabilityToPESC((char)course.degreeApplicable)
                courseType.setCourseApplicability(CourseApplicabilityType.fromValue(v))
            }

            CourseCCCExtensions extensions = transcriptBuilder.createCourseExtensions()
            extensions.setGradeBracket(course.gradeBracket == 'Y' ? CourseCCCExtensions.GradeBracketType.GRADE_IS_BRACKETED
                    : CourseCCCExtensions.GradeBracketType.GRADE_IS_NOT_BRACKETED)
            if (course.attemptedUnitsCalc) {
                def v = transcriptBuilder.convertAttemptedUnitsCalcFROMCCCASCIIToPESC((char)course.attemptedUnitsCalc)
                extensions.setAttemptedUnitsCalc(CourseCCCExtensions.AttemptedUnitsCalcType.fromValue(v))
            }
            /*
            if (course.earnedUnitsCalc) {
                def v = transcriptBuilder.convertAttemptedUnitsCalcFROMCCCASCIIToPESC((char)course.earnedUnitsCalc)
                extensions.setEarnedUnitsCalc(CourseCCCExtensions.EarnedUnitsCalcType.fromValue(v))
            }
            */
            if (course.gradePointsCalc) {
                def v = transcriptBuilder.convertAttemptedUnitsCalcFROMCCCASCIIToPESC((char)course.gradePointsCalc)
                extensions.setGradePointsCalc(CourseCCCExtensions.GradePointsCalcType.fromValue(v))
            }

            if (courseType.userDefinedExtensions == null)
                courseType.userDefinedExtensions = new UserDefinedExtensionsTypeImpl()

            courseType.userDefinedExtensions.courseCCCExtensions = extensions

            session.getCourses().add(courseType)
        }
    }

    def findGPA(termCode, gpaList){
        for(row in gpaList) {
            if (row.termCode == termCode) {
                return row
            }
        }
        return null
    }

    def findSessionCourses(termCode, courseList){
        def sessionCourses = []
        def iter = courseList.iterator()
        while (iter.hasNext()) {
            def course = iter.next()
            if (course.termCode == termCode) {
                sessionCourses.add(course)
                //Remove the course from the course list so that future searches are more efficient.
                iter.remove()
            }
        }
        return sessionCourses
    }

    def findSchool(campusCode, collegeIdentities) {
        for(school in collegeIdentities) {
            if (campusCode == school.campusCode) {
                return school
            }
        }

        throw new InvalidRequestException(InvalidRequestException.Errors.schoolNotFound, "Could not find school with campus code " + (String) campusCode)

    }
    def convertBannerClassLevelToPESC(String classLevel){
        StudentLevelCodeType studentLevel = StudentLevelCodeType.POSTSECONDARY;

        if (classLevel != null) {
            switch (Integer.valueOf(classLevel)){
                case 0:
                case 1: studentLevel = StudentLevelCodeType.COLLEGE_FIRST_YEAR; break;
                case 2: studentLevel = StudentLevelCodeType.COLLEGE_SOPHOMORE; break;
                case 3: studentLevel = StudentLevelCodeType.COLLEGE_JUNIOR; break;
                case 4: studentLevel = StudentLevelCodeType.COLLEGE_SENIOR; break;
            }
        }

        return studentLevel
    }

    def convertBannerStudentLevelToPESC(String bannerLevel){
        /*
            00 = Undeclared
            XG = Gifted and Talented
            UG = Undergraduate
            GR = Graduate
            DC = Doctoral Student
            CE = Continuing Education
         */
        if ("UG".equals(bannerLevel)){
            return StudentLevelCodeType.POSTSECONDARY
        }

        //JHW: there doesn't seem to be a mapping here, and nobody seems to know how to obtain a student's
        //class level.
        return StudentLevelCodeType.POSTSECONDARY

    }
    def setStudentSessions(recordHolderSchool, schools, courseList, sessionList, AcademicRecordType academicRecord, CollegeTranscriptBuilder builder) {

        println "course list size: " + courseList.size()

        SimpleDateFormat dateFormat = new SimpleDateFormat("yyyyMMdd")

        def cumulativeGPA = [ hoursEarned: 0.0d, hoursAttempted: 0.0d, qualityPoints: 0.0d, gpaHours: 0.0d]


        for (session in sessionList) {
            AcademicSessionType sessionType = builder.createAcademicSession(convertBannerClassLevelToPESC(session.classLevel))
            sessionType.academicSessionDetail = builder.constructSessionDetail(dateFormat.parse((String)session.startDate),
                    dateFormat.parse((String)session.endDate), (String)session.termName + " " + session.termYear, "", convertTermBasis(session.termBasis))
            def sessionCourses = findSessionCourses(session.termCode, courseList)
            addCoursesToSession(sessionCourses,sessionType, recordHolderSchool, schools, builder)


            if (session.gpa != null) {
                AcademicSummaryFType summary = builder.createAcademicSummaryF();
                GPAType gpa = builder.createGPA()
                gpa.setCreditHoursAttempted(new BigDecimal((double)session.hoursAttempted))
                gpa.setCreditHoursEarned(new BigDecimal((double)session.hoursEarned))
                gpa.setCreditHoursforGPA(new BigDecimal((double)session.qualityPoints))
                cumulativeGPA.qualityPoints +=(double)session.qualityPoints
                cumulativeGPA.hoursEarned += (double)session.hoursEarned
                cumulativeGPA.hoursAttempted += (double)session.hoursAttempted
                cumulativeGPA.gpaHours += (double)session.gpaHours
                /*
                GPA- Grade Point Average. The number of Quality Points divided by GPA hours.
                Quality Points- the grade awarded multiplied by hours (credit hours) for the course
                 */
                gpa.setGradePointAverage(new BigDecimal((double)session.gpa).setScale(1, BigDecimal.ROUND_HALF_UP))
                summary.setGPA(gpa)
                sessionType.getAcademicSummaries().add(summary)
            }

            academicRecord.getAcademicSessions().add(sessionType)
        }

        cumulativeGPA.gpa = new BigDecimal(cumulativeGPA.qualityPoints / cumulativeGPA.gpaHours).setScale(2, BigDecimal.ROUND_HALF_UP)
        return cumulativeGPA

    }

    def setStudentAddressProperties(def studentAddress, CollegeTranscriptBuilder builder) {
        builder.getStudentAddressLines().add(studentAddress.street1)
        builder.getStudentAddressLines().add(studentAddress.street2)
        builder.getStudentAddressLines().add(studentAddress.street3)
        builder.setStudentCity(studentAddress.city)
        builder.setStudentStateOrProvince(studentAddress.state)
        builder.setStudentZipCode(studentAddress.postalCode)
        builder.setStudentResidentStateOrProvince(studentAddress.state)
        builder.setHighSchoolName((String)studentAddress.highSchoolName)
        builder.setHighSchoolGraduationDate((String)studentAddress.graduationDate)

    }

    def setPersonProperties(String misCode, def student, Sql sql, CollegeTranscriptBuilder builder) {

        builder.setStudentFirstname(student.firstName)
        builder.setStudentLastName(student.lastName)
        builder.setStudentMiddleName(student.middleName)
        builder.setStudentNameSuffix(student.nameSuffix)

        // @TODO alternateNames?  Legal, etc. Also what is correct nameCode for base name.

        builder.setSchoolAssignedStudentID(student.schoolAssignedID)
       // builder.setStudentSSN(student.SSN)
        builder.setStudentPartialSSN(student.partialSSN)

        builder.setStudentEmailAddress(student.emailAddress)

        if (student.cccid) {
            builder.setAgencyAssignedID((String)student.cccid) //deprecated as of v1.6

            builder.addAgencyIdentifier((String)student.cccid, "CCC Technology Center", AgencyCodeType.STATE, CountryCodeType.US, StateProvinceCodeType.CA)
        }

        builder.setStudentGender(builder.createGenderCodeType(student.gender))
        builder.setStudentBirthDate(new SimpleDateFormat("yyyyMMdd").parse(student.birthDate))
        builder.setStudentBirthCity(student.birthCity)
        if (student.birthState) {
            builder.setStudentBirthStateProvince(StateProvinceCodeType.fromValue(student.birthState));
        }
    }


    //Look up stvsbgi_code for the transcript school.
    def lookupInstitution(AcademicRecordTypeImpl record, String misCode, Sql sql) {
        def institution
        if( record.school.FICE ) {
            if( MisEnvironment.checkPropertyMatch(environment, misCode, "banner.transcript.supportedSchoolCodes", "FICE")) {
                def query = MisEnvironment.getProperty(environment, misCode, "banner.transcript.institution.FICE.getQuery")
                institution = sql.firstRow(query, [schoolCode: record.school.FICE])
            }
        } //@TODO Check more school code types?  Build queries for them if necessary to get stvsbgi_code.
        if(!institution) {
            throw new InvalidRequestException(InvalidRequestException.Errors.invalidSchoolCode, "Transcripts sending school could not be found.")
        }
        return institution
    }

    //Check and populate SORPCOL record if required.
    void checkPriorSchool( def student, def institution, String misCode, Sql sql) {
        def plsql = """
            DECLARE
                p_rowid_out        gb_common.internal_record_id_type;
                p_pidm           sorpcol.sorpcol_pidm%TYPE;
                p_sbgi_code      sorpcol.sorpcol_sbgi_code%TYPE;
                p_official_trans sorpcol.sorpcol_official_trans%TYPE;
                p_data_origin    sorpcol.sorpcol_data_origin%TYPE;

            BEGIN

            p_pidm := ?;
            p_sbgi_code := ?;
            p_official_trans := ?;
            p_data_origin := ?;

            IF (gb_prior_college.f_exists(p_pidm, p_sbgi_code) = 'N') THEN
                GB_PRIOR_COLLEGE.p_create(
                    p_pidm => p_pidm,
                    p_sbgi_code => p_sbgi_code,
                    p_trans_recv_date => SYSDATE,
                    p_official_trans => p_official_trans,
                    p_data_origin => p_data_origin,
                    p_rowid_out => p_rowid_out
                 );
            END IF;
            END;
"""
        try {
            sql.call(plsql, [student.pidm,
                             institution.sbgiCode,
                             MisEnvironment.getProperty(environment, misCode, "banner.transcript.official"),
                             MisEnvironment.getProperty(environment, misCode, "banner.transcript.dataOrigin")
            ])
        } catch( SQLException e ) {
            throw new InvalidRequestException(InvalidRequestException.Errors.sisQueryError, "Error creating prior college record(SOAPCOL): " +
                    e.getMessage())
        }
    }

    //Check and Populate transcript transfer course record
    void checkTransferCourse(def student, def institution, def course, String misCode, Sql sql ) {
        def sisTerm = getSisTermId(course, misCode, sql)
        def query = MisEnvironment.getProperty(environment, misCode, "banner.transcript.catalog.getQuery")
        def result = sql.firstRow(query, [sbgiCode:institution.sbgiCode,
                                          sisTermId:sisTerm?.sisTermId,
                                          courseSubjectAbbreviation:course.courseSubjectAbbreviation,
                                          courseNumber:course.courseNumber
        ])
        if( result?.exists != 'Y' ) {
            throw new InvalidRequestException(InvalidRequestException.Errors.courseNotFound, "This course was not found in the transfer catalog for the " +
                    "transfer institution: " + course.courseSubjectAbbreviation + "-" + course.courseNumber)
        }
        query = MisEnvironment.getProperty(environment, misCode, "banner.transcript.course.getQuery")
        result = sql.firstRow(query, [pidm:student.pidm,
                                      sbgiCode:institution.sbgiCode,
                                      courseCreditLevel:getCourseLevelSis(course.courseLevel, misCode),
                                      sisTermId:sisTerm?.sisTermId,
                                      courseSubjectAbbreviation:course.courseSubjectAbbreviation,
                                      courseNumber:course.courseNumber,
                                      courseCreditEarned:course.courseCreditEarned,
                                      courseAcademicGrade:course.courseAcademicGrade])
        if( !result ) {
            query = MisEnvironment.getProperty(environment, misCode, "banner.transcript.course.insertQuery")
            try {
                def tramSeqNo = checkTransferAttendancePeriod(student, institution, course, sisTerm, misCode, sql)
                def arguments = [pidm                        : student.pidm,
                                 sbgiCode                    : institution.sbgiCode,
                                 courseCreditLevel           : getCourseLevelSis(course.courseLevel, misCode),
                                 sisTermId                   : sisTerm?.sisTermId,
                                 courseSubjectAbbreviation   : course.courseSubjectAbbreviation,
                                 courseNumber                : course.courseNumber,
                                 courseCreditEarned          : course.courseCreditEarned,
                                 courseAcademicGradeScaleCode: course.courseAcademicGradeScaleCode,
                                 courseAcademicGrade         : course.courseAcademicGrade,
                                 tramSeqNo                   : tramSeqNo,
                                 courseTitle                 : course.courseTitle,
                                 program                     : '......'
                ]
                sql.execute(query, arguments)
            } catch (SQLException e) {
                throw new InvalidRequestException(InvalidRequestException.Errors.sisQueryError, "Error creating course transfer record: " +
                        e.getMessage())
            }
        }
    }

    def checkTransferAttendancePeriod(def student, def institution, def course, def sisTerm, String misCode, Sql sql) {
        def query = MisEnvironment.getProperty(environment, misCode, "banner.transcript.institution.getQuery")
        def tritSeqNo
        def tramSeqNo
        try {
            def result = sql.firstRow(query, [sbgiCode:institution.sbgiCode, pidm: student.pidm])
            if(result) {
                tritSeqNo = result.tritSeqNo
            }
            else {
                query = MisEnvironment.getProperty(environment, misCode, "banner.transcript.institution.sequence.getQuery")
                def tritResult = sql.firstRow(query, [pidm: student.pidm])
                tritSeqNo = tritResult?.tritSeqNo ? tritResult?.tritSeqNo + 1 : 1
                query = MisEnvironment.getProperty(environment, misCode, "banner.transcript.institution.insertQuery")
                sql.execute(query, [pidm     : student.pidm,
                                    tritSeqNo: tritSeqNo,
                                    sbgiCode : institution.sbgiCode
                ])
            }
            query = MisEnvironment.getProperty(environment, misCode, "banner.transcript.attendance.getQuery")
            result = sql.firstRow(query, [tritSeqNo:tritSeqNo, pidm:student.pidm, sisTermId:sisTerm.sisTermId])
            if(result) {
                tramSeqNo = result.tramSeqNo
            }
            else {
                query = MisEnvironment.getProperty(environment, misCode, "banner.transcript.attendance.sequence.getQuery")
                def tramResult = sql.firstRow(query, [pidm: student.pidm])
                tramSeqNo = tramResult?.tramSeqNo ?: 1
                query = MisEnvironment.getProperty(environment, misCode, "banner.transcript.attendance.insertQuery")
                sql.execute(query, [pidm           : student.pidm,
                                    tritSeqNo      : tritSeqNo,
                                    tramSeqNo      : tramSeqNo,
                                    levelCode      : getCourseLevelSis(course.courseLevel, misCode),
                                    attnPeriod     : sisTerm.sisTermId + " Term",
                                    sisTermId      : sisTerm.sisTermId,
                                    termType       : getTermType(sisTerm.type, misCode).name(),
                                    courseBeginDate: sisTerm.startDate.toTimestamp(),
                                    courseEndDate  : sisTerm.endDate.toTimestamp()])
            }
        } catch ( SQLException e ) {
            throw new InvalidRequestException(InvalidRequestException.Errors.sisQueryError, "Error creating attendance period: " +
                    e.getMessage())
        }
        return tramSeqNo
    }

    def getSisTermId(def course, String misCode, Sql sql) {
        def query = MisEnvironment.getProperty(environment, misCode, "banner.term.byEndDate.getQuery")
        try {
            def result = sql.firstRow(query, [courseEndDate:new Date().toTimestamp()])
            return result
        } catch( SQLException e ) {
            throw new InvalidRequestException(InvalidRequestException.Errors.sisQueryError, "Error querying SIS term for transfer courses" +
                    e.getMessage())
        }
    }

    def getTermType( String termType, String misCode ) {
        switch (termType) {
            case {MisEnvironment.checkPropertyMatch( environment, misCode, "banner.term.type.Semester", it)}:
                return TermType.Semester
                break
            case {MisEnvironment.checkPropertyMatch( environment, misCode, "banner.term.type.Quarter", it)}:
                return TermType.Quarter
                break
            default:
                return null
        }
    }

    def getCourseLevelSis(def transcriptCourseLevel, String misCode) {
        for( int i = 0; i <= 30; i++ ) {
            def currentTranscriptLevel = getPropertyName(i, misCode)
            if( currentTranscriptLevel == transcriptCourseLevel.name()) {
                def sisCourseLevels = MisEnvironment.getProperty(environment, misCode,
                        "banner.transcript.courseLevel." + i + "." + currentTranscriptLevel)
                return sisCourseLevels.split(',')?.getAt(0)
            }
        }
        throw new InvalidRequestException(InvalidRequestException.Errors.sisQueryError, "Could not derive a course level for the given PESC " +
                "course level.  Check configuration")
    }

    def getCourseLevelTranscript( String courseLevel, String misCode ) {
        for( int i = 0; i <= 30; i++ ) {
            def currentTranscriptLevel = getPropertyName(i, misCode)
            if( MisEnvironment.checkPropertyMatch(environment, misCode,
                    "banner.transcript.courseLevel." + iteration + "." + currentTranscriptLevel, courseLevel))
                return CourseLevelType.fromValue(currentTranscriptLevel)
        }
    }

    def getPropertyName( int iteration, String misCode) {
        def possibilities = CourseLevelType.values()
        def levelName
        possibilities.each { courseLevel ->
            (MisEnvironment.getProperty(environment, misCode,
                    "banner.transcript.courseLevel." + iteration + "." + courseLevel.name()))
            if (MisEnvironment.getProperty(environment, misCode,
                    "banner.transcript.courseLevel." + iteration + "." + courseLevel.name()) != null) {
                levelName = courseLevel.name()
            }
        }
        return levelName
    }

}