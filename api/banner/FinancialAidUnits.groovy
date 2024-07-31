/**
 * Created by exp02269 on 4/19/2017.
 */
package api.banner

import com.ccctc.adaptor.exception.EntityConflictException
import com.ccctc.adaptor.exception.EntityNotFoundException

import com.ccctc.adaptor.exception.InvalidRequestException
import com.ccctc.adaptor.model.CourseExchangeEnrollment
import com.ccctc.adaptor.model.Section
import com.ccctc.adaptor.util.MisEnvironment
import groovy.sql.Sql
import org.springframework.core.env.Environment

class FinancialAidUnits {

    Environment environment

    def getUnitsList(String misCode, String cccid, String sisTermId) {

        Sql sql = BannerConnection.getSession(environment, misCode)
        def list = []
        try {
            def query = MisEnvironment.getProperty(environment, misCode, "banner.cccid.getQuery")

            def row = sql.firstRow(query, [cccid: cccid])
            if (!row) {
                throw new InvalidRequestException(InvalidRequestException.Errors.studentNotFound, "Student with the specified CCCID does not exist.")
            }
            query = MisEnvironment.getProperty(environment, misCode, "banner.term.getQuery")
            row = sql.firstRow(query, [sisTermId: sisTermId])
            if (!row) {
                throw new InvalidRequestException(InvalidRequestException.Errors.termNotFound, "Term does not exist.")
            }
            query = MisEnvironment.getProperty(environment, misCode, "banner.finaid.units.stage.getAllQuery")
            sql.eachRow(query, [misCode: misCode, cccid: cccid, sisTermId: sisTermId]) { stagingUnitsRow ->

           def  ceEnrollment = new CourseExchangeEnrollment.Builder()
                ceEnrollment.misCode(stagingUnitsRow.sectionMisCode)
                ceEnrollment.collegeName(stagingUnitsRow.collegeName)
                ceEnrollment.c_id(stagingUnitsRow.cid)
                ceEnrollment.units(stagingUnitsRow.units)

                def section = new Section.Builder()
                section.title(stagingUnitsRow.title)
                section.sisSectionId(stagingUnitsRow.sectionId)
                section.sisTermId(stagingUnitsRow.sectionSisTermId)
                ceEnrollment.section(section.build())

                list << new FinancialAidUnits.Builder()
                        .cccid(cccid)
                        .sisTermId(sisTermId)
                        .ceEnrollment(ceEnrollment.build())
                        .build()

            }
        } finally {
            sql?.close()
        }
        return list
    }

    def post(String misCode, com.ccctc.adaptor.model.FinancialAidUnits faUnits ) {

        def query = MisEnvironment.getProperty(environment, misCode, "banner.cccid.getQuery")
        Sql sql = BannerConnection.getSession(environment, misCode)
        try {
            def student = sql.firstRow(query, [cccid: faUnits.cccid])
            if (!student) {
                throw new InvalidRequestException(InvalidRequestException.Errors.studentNotFound, "Student with the specified CCCID does not exist.")
            }
            query = MisEnvironment.getProperty(environment, misCode, "banner.term.getQuery")
            def row = sql.firstRow(query, [sisTermId: faUnits.sisTermId])
            if (!row) {
                throw new InvalidRequestException(InvalidRequestException.Errors.termNotFound, "Term does not exist.")
            }
            query = MisEnvironment.getProperty(environment, misCode, "banner.finaid.units.stage.getQuery")
            row = sql.firstRow(query, [cccid    : faUnits.cccid, misCode: misCode, sectionMisCode: faUnits.ceEnrollment.misCode,
                                       sisTermId: faUnits.sisTermId, cid:faUnits.ceEnrollment.c_id])
            if (row?.enrollmentUnits != null) {
                throw new EntityConflictException("FinAid enrollment units already exists.")
            } else {
                sql.withTransaction {
                    adjustUnits(faUnits.ceEnrollment.units, student.pidm, faUnits.sisTermId, misCode, "Y", sql)
                    createExchangeEnrollment(faUnits, misCode, sql)
                }
            }
        } finally {
            sql?.close()
        }
        return faUnits
    }

    void delete(String misCode, String cccid, String sisTermId, String enrolledMisCode, String cid) {

        def query = MisEnvironment.getProperty(environment, misCode, "banner.cccid.getQuery")
        Sql sql = BannerConnection.getSession(environment, misCode)
        try {
            def student = sql.firstRow(query, [cccid: cccid])
            if (!student) {
                throw new InvalidRequestException(InvalidRequestException.Errors.studentNotFound, "Student with the specified CCCID does not exist.")
            }
            query = MisEnvironment.getProperty(environment, misCode, "banner.term.getQuery")
            def row = sql.firstRow(query, [sisTermId: sisTermId])
            if (!row) {
                throw new InvalidRequestException(InvalidRequestException.Errors.termNotFound, "Term does not exist.")
            }
            query = MisEnvironment.getProperty(environment, misCode, "banner.finaid.units.stage.getQuery")
            row = sql.firstRow(query, [cccid: cccid, misCode: misCode, sectionMisCode: enrolledMisCode,
                                       sisTermId: sisTermId, cid:cid])
            if (row?.enrollmentUnits == null) {
                throw new EntityNotFoundException("FinAid enrollment does not exist.")
            } else {
                sql.withTransaction {
                    adjustUnits(row.enrollmentUnits * -1.0, student.pidm, sisTermId, misCode, (row.enrollmentsInTerm == 1 ? "N" : "Y"), sql)
                    deleteExchangeEnrollment(misCode, cccid, sisTermId, enrolledMisCode, cid, sql)
                }
            }

        } finally {
            sql?.close()
        }
    }

    void adjustUnits(Float units, def pidm, String sisTermId, String misCode, String consortiumFlag, Sql sql) {

        def query = MisEnvironment.getProperty(environment, misCode, "banner.finaid.units.getQuery")
        def row = sql.firstRow(query, [pidm: pidm, sisTermId: sisTermId])
        if (!row) {
            throw new InvalidRequestException(InvalidRequestException.Errors.noFinancialAidRecord, "No Financial Aid Enrollment record exists yet")
        }

        def newUnits = units + row.units
        query = MisEnvironment.getProperty(environment, misCode, "banner.finaid.units.updateQuery")
        sql.execute(query, [pidm: pidm, sisTermId: sisTermId, newUnits: newUnits, conInd: consortiumFlag])

    }


    void createExchangeEnrollment(com.ccctc.adaptor.model.FinancialAidUnits faUnits, String misCode, Sql sql) {

        def query = MisEnvironment.getProperty(environment, misCode, "banner.finaid.units.stage.insertQuery")

       sql.execute(query, [misCode           : misCode,
                           sisTermId         : faUnits.sisTermId,
                           cccid             : faUnits.cccid,
                           sectionMisCode    : faUnits.ceEnrollment.misCode,
                           cid               : faUnits.ceEnrollment.c_id,
                           sectionSisTermId  : faUnits.ceEnrollment.section.sisTermId,
                           sisSectionId      : faUnits.ceEnrollment.section.sisSectionId,
                           sectionCollegeName: faUnits.ceEnrollment.collegeName,
                           sectionTitle      : faUnits.ceEnrollment.section.title,
                           sectionUnits      : faUnits.ceEnrollment.units
       ])
    }

    void deleteExchangeEnrollment( String misCode, String cccid, String sisTermId, String enrolledMisCode, String cid, Sql sql ) {

        def query = MisEnvironment.getProperty(environment, misCode, "banner.finaid.units.stage.deleteQuery")
        sql.execute(query, [cccid: cccid, misCode: misCode, sectionMisCode: enrolledMisCode,
                        sisTermId: sisTermId, cid:cid])
   }

}
