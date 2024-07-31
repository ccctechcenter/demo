package api.banner

import com.ccctc.adaptor.exception.EntityNotFoundException
import com.ccctc.adaptor.model.placement.StudentPlacementData
import groovy.sql.GroovyRowResult
import groovy.sql.Sql
import oracle.sql.TIMESTAMP
import org.springframework.core.env.Environment
import spock.lang.Specification

import java.sql.Date
import java.sql.Timestamp


class StudentPlacementSpec extends Specification {
    def static misCode = "001"
    def static cccId = "12345"
    def static ssId = "111111"

    StudentPlacement StudentPlacement
    StudentPlacementData placementData
    Sql sql
    Environment environment
    TIMESTAMP dateTime = new TIMESTAMP()


    def setup() {
        sql = Mock(Sql)
        environment = Mock(Environment)

        StudentPlacement = Spy(new StudentPlacement())
        studentPlacement.environment = environment

        GroovyMock(BannerConnection, global: true)
        BannerConnection.getSession(*_) >> sql

        placementData = new StudentPlacementData(
                                                  miscode: misCode,
                                                  californiaCommunityCollegeId: cccId,
                                                  statewideStudentId: "55555",
                                                  dataSource: 3,
                                                  english: 2,
                                                  slam: 2,
                                                  stem: 2,
                                                  isAlgI: true,
                                                  isAlgII: true,
                                                  trigonometry: true,
                                                  preCalculus: false,
                                                  calculus: true,
                                                  completedEleventhGrade: true,
                                                  cumulativeGradePointAverage: 3.0,
                                                  englishCompletedCourseId: 123,
                                                  englishCompletedCourseGrade: "A",
                                                  mathematicsCompletedCourseId: 234,
                                                  mathematicsCompletedCourseGrade: "B",
                                                  mathematicsPassedCourseId: 233,
                                                  mathematicsPassedCourseGrade: "A",
                                                  placementStatus: "placed",
                                                  tstmpERPTransmit: 1602088983000,
                                                  sisProcessedFlag: null,
                                                  sisProcessedNotes: "note",
                                                  appId: 123456789,
                                                  highestGradeCompleted: "ABC"
        )

    }


    def "get placement" () {

        when:
        def result = StudentPlacement.get(misCode, cccId, ssId)

        then:
        1 * sql.firstRow(*_) >> new GroovyRowResult(["placementSequence": 1,
                                                     "pidm": 222222,
                                                     "misCode": misCode,
                                                     "californiaCommunityCollegeId": cccId,
                                                     "statewideStudentId": placementData.statewideStudentId,
                                                     "dataSource": placementData.dataSource,
                                                     "english": placementData.english,
                                                     "slam": placementData.slam,
                                                     "stem": placementData.stem,
                                                     "isAlgI": "Y",
                                                     "isAlgII": "Y",
                                                     "trigonometry": "Y",
                                                     "preCalculus": "N",
                                                     "calculus": "Y",
                                                     "completedEleventhGrade": "Y",
                                                     "cumulativeGradePointAverage": placementData.cumulativeGradePointAverage,
                                                     "englishCompletedCourseId": placementData.englishCompletedCourseId,
                                                     "englishCompletedCourseGrade": placementData.englishCompletedCourseGrade,
                                                     "mathematicsCompletedCourseId": placementData.mathematicsCompletedCourseId,
                                                     "mathematicsCompletedCrseGrade": placementData.mathematicsCompletedCourseGrade,
                                                     "mathematicsPassedCourseId": 233,
                                                     "mathematicsPassedCourseGrade": placementData.mathematicsPassedCourseGrade,
                                                     "placementStatus": placementData.placementStatus,
                                                     "tstmpERPTransmit": new Timestamp(1602088983000),
                                                     "tstmpSISTransmit": dateTime,
                                                     "sisProcessedFlag": null,
                                                     "tstmpSISProcessed": dateTime,
                                                     "sisProcessedNotes": placementData.sisProcessedNotes,
                                                     "appId": placementData.appId,
                                                     "highestGradeCompleted": placementData.highestGradeCompleted
        ])
        result.californiaCommunityCollegeId == cccId
        result.tstmpERPTransmit == 1602088983000
        result.statewideStudentId == placementData.statewideStudentId
        result.dataSource == placementData.dataSource
        result.english == placementData.english
        result.slam == placementData.slam
        result.stem == placementData.stem
        result.isAlgI == placementData.isAlgI
        result.isAlgII == placementData.isAlgII
        result.trigonometry == placementData.trigonometry
        result.preCalculus == placementData.preCalculus
        result.completedEleventhGrade == placementData.completedEleventhGrade
        result.cumulativeGradePointAverage == placementData.cumulativeGradePointAverage
        result.englishCompletedCourseId == placementData.englishCompletedCourseId
        result.englishCompletedCourseGrade == placementData.englishCompletedCourseGrade
        result.mathematicsCompletedCourseId == placementData.mathematicsCompletedCourseId
        result.mathematicsCompletedCourseGrade == placementData.mathematicsCompletedCourseGrade
        result.mathematicsPassedCourseId == placementData.mathematicsPassedCourseId
        result.mathematicsPassedCourseGrade == placementData.mathematicsPassedCourseGrade
        result.placementStatus == placementData.placementStatus
        result.sisProcessedNotes == placementData.sisProcessedNotes
        result.appId == placementData.appId
        result.highestGradeCompleted == placementData.highestGradeCompleted

    }
    
    def "post placement" () {
        
        setup:
        environment.getProperty("sqljdbc.assessment.audit") >> "1"
        environment.getProperty("sqljdbc.assessment.validateId") >> "1"
        environment.getProperty("sqljdbc.cccid.getQuery") >> "cccid"
        environment.getProperty("sqljdbc.assessment.placement.getSequence") >> "sequence"
        environment.getProperty("sqljdbc.assessment.placement.insertQuery") >> "insert"
        environment.getProperty("sqljdbc.assessment.placement.postGetQuery") >> "getPost"
        
        when:
        StudentPlacement.post(misCode, placementData)
        
        then:
        1 * sql.firstRow("cccid", *_) >> new GroovyRowResult(["cccid": placementData.californiaCommunityCollegeId, "pidm": 222222])
        1 * sql.firstRow("sequence", *_) >> new GroovyRowResult(["plmtSequence": 2])
        1 * sql.execute("insert", *_) >> { arguments ->
            HashMap params = arguments[1]
            assert params.plmtSequence == 2
            assert params.pidm == 222222
            assert params.misCode == misCode
            assert params.cccid == placementData.californiaCommunityCollegeId
            assert params.ssid == placementData.statewideStudentId
            assert params.dataSource ==  placementData.dataSource
            assert params.english == placementData.english
            assert params.slamSupport == placementData.slam
            assert params.stemSupport == placementData.stem
            assert params.isAlgI == (placementData.isAlgI as String == "true" ? "Y" : (placementData.isAlgI as String == "false" ? "N" : null ))
            assert params.isAlgII == (placementData.isAlgII as String == "true" ? "Y" : (placementData.isAlgII as String == "false" ? "N" : null ))
            assert params.trigonometry == (placementData.trigonometry as String == "true" ? "Y" : (placementData.trigonometry as String == "false" ? "N" : null ))
            assert params.preCalculus == (placementData.preCalculus as String == "true" ? "Y" : (placementData.preCalculus as String == "false" ? "N" : null ))
            assert params.calculus == (placementData.calculus as String == "true" ? "Y" : (placementData.calculus as String == "false" ? "N" : null ))
            assert params.comp11thGrade == (placementData.completedEleventhGrade as String == "true" ? "Y" : (placementData.completedEleventhGrade as String == "false" ? "N" : null ))
            assert params.cumulativeGPA == placementData.cumulativeGradePointAverage
            assert params.engCompCourseId == placementData.englishCompletedCourseId
            assert params.engCompCourseGrade == placementData.englishCompletedCourseGrade
            assert params.mathCompCourseId == placementData.mathematicsCompletedCourseId
            assert params.mathCompCourseGrade == placementData.mathematicsCompletedCourseGrade
            assert params.mathPassCourseId == placementData.mathematicsPassedCourseId
            assert params.mathPassCourseGrade == placementData.mathematicsPassedCourseGrade
            assert params.placementStatus ==  placementData.placementStatus
            assert params.tstmpSISTransmit == placementData.tstmpSISTransmit
            assert params.appId  == placementData.appId
            assert params.highestGradeCompleted == placementData.highestGradeCompleted
        }
    }

    def "post placement student not found" () {

        setup:
        environment.getProperty("sqljdbc.assessment.audit") >> "1"
        environment.getProperty("sqljdbc.assessment.validateId") >> "1"
        environment.getProperty("sqljdbc.cccid.getQuery") >> "cccid"
        when:
        StudentPlacement.post(misCode, placementData)
        then:
        thrown EntityNotFoundException
    }
}
