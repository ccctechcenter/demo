package com.ccctc.adaptor.model.mock

import com.ccctc.adaptor.model.College
import com.ccctc.adaptor.model.Course
import spock.lang.Specification

/**
 * Created by zekeo on 7/21/2017.
 */
class BaseMockDataDBSpec extends Specification {

    def "loadData"() {
        setup:
        def c = new CollegeDB()
        c.init()
        c.add(new College(misCode: "001", districtMisCode: "000"))
        c.add(new College(misCode: "002", districtMisCode: "000"))

        def t1 = new TermDB(c)
        t1.dataDir = "./api/mock/data/dev"
        t1.misCode = "001"
        t1.init()

        def t2 = new TermDB(c)
        t1.dataDir = "./api/mock/data/dev"
        t1.misCode = "002"
        t1.init()

        when:
        def terms1 = t1.getAll()
        def terms2 = t1.getAll()

        then:
        terms1.size() > 0
        terms2.size() > 0
    }

    def "missing PrimaryKey"() {
        when:
        def a = new BaseMockDataDB<String>("filename", String.class) {
            @Override
            def void registerHooks() {

            }
        }

        then:
        thrown IllegalArgumentException
    }

    def "invalid file"() {
        when:
        def a = new BaseMockDataDB<Course>("bad file name", Course.class) {
            @Override
            def void registerHooks() {

            }
        }

        then:
        a.getAll().size() == 0
    }
}