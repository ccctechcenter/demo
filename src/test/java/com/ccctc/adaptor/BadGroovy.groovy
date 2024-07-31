package com.ccctc.adaptor

import com.ccctc.adaptor.model.Enrollment

/**
 * THIS IS NOT A UNIT TEST.
 *
 * This is used by the unit test GroovyServiceImplSpec to test out dynamic groovy scripts.
 */

class BadGroovy {

    def environment

    def run() {

    }

    def wrongType() {
        return new Enrollment()
    }
}
