package com.ccctc.adaptor.exception

import com.ccctc.adaptor.exception.ApiExceptionHandler
import com.ccctc.adaptor.exception.EntityConflictException
import com.ccctc.adaptor.exception.EntityNotFoundException
import com.ccctc.adaptor.exception.InternalServerException
import com.ccctc.adaptor.exception.InvalidRequestException
import com.ccctc.adaptor.exception.ServiceUnavailableException
import com.ccctc.adaptor.model.ErrorResource
import org.springframework.web.context.request.WebRequest
import spock.lang.Specification

/**
 * Created by Rasul on 2/1/2016.
 */
class ApiExceptionSpec extends Specification {

    def errorCode = "error.code"
    def errorMessage = "Error message"

    def "Test InvalidRequestException("() {
        setup:
        def exception1
        def exception2
        def request = Mock(WebRequest)
        def handler = new ApiExceptionHandler()
        def errorResource = new ErrorResource()

        when:
        exception1 = new InvalidRequestException(InvalidRequestException.Errors.invalidSearchCriteria, errorMessage)
        exception2 = new InvalidRequestException(InvalidRequestException.Errors.invalidRequest, null)
        def result = handler.handleInvalidRequestException(exception1,request)
        ErrorResource result2 = handler.handleInvalidRequestException(exception2, request).getBody()
        errorResource.setCode(errorCode)
        errorResource.setMessage(errorMessage)

        then:
        exception1.code == InvalidRequestException.Errors.invalidSearchCriteria
        exception1.message == errorMessage
        exception2.code == InvalidRequestException.Errors.invalidRequest
        exception2.message == null
        errorResource.getCode() == errorCode
        errorResource.getMessage() == errorMessage
        result.getStatusCode().value() == 400
        result2.getCode() == InvalidRequestException.Errors.invalidRequest.toString()
        result2.getMessage() == "com.ccctc.adaptor.exception.InvalidRequestException"
     }

    def "Test EntityNotFoundException("() {
        setup:
        def request = Mock(WebRequest)
        def handler = new ApiExceptionHandler()

        when:
        def exception1 = new EntityNotFoundException(errorMessage)
        def exception2 = new EntityNotFoundException(null)
        def result = handler.handleEntityNotFoundException(exception1,request)
        ErrorResource result2 = handler.handleEntityNotFoundException(exception2, request).getBody()

        then:
        exception1.code == "noResultsFound"
        exception1.message == errorMessage
        exception2.code == "noResultsFound"
        exception2.message == null
        result.getStatusCode().value() == 404
        result2.getCode() == "noResultsFound"
        result2.getMessage() == "com.ccctc.adaptor.exception.EntityNotFoundException"
    }

    def "Test InternalServerException("() {
        setup:
        def exception1
        def exception2
        def request = Mock(WebRequest)
        def handler = new ApiExceptionHandler()

        when:
        exception1 = new InternalServerException(InternalServerException.Errors.internalServerError, errorMessage)
        exception2 = new InternalServerException(null)
        def result = handler.handleInternalServerException(exception1,request)
        ErrorResource result2 = handler.handleInternalServerException(exception2, request).getBody()

        then:
        exception1.code == InternalServerException.Errors.internalServerError
        exception1.message == errorMessage
        exception2.code == InternalServerException.Errors.internalServerError
        exception2.message == null
        result.getStatusCode().value() == 500
        result2.getCode() == InternalServerException.Errors.internalServerError.toString()
        result2.getMessage() == "com.ccctc.adaptor.exception.InternalServerException"
    }

    def "Test ServiceUnavailableException("() {
        setup:
        def exception1
        def exception2
        def request = Mock(WebRequest)
        def handler = new ApiExceptionHandler()

        when:
        exception1 = new ServiceUnavailableException(errorMessage)
        exception2 = new ServiceUnavailableException(null)
        def result = handler.handleServiceUnavailableException(exception1,request)
        ErrorResource result2 = handler.handleServiceUnavailableException(exception2, request).getBody()

        then:
        exception1.code == "serviceUnavailable"
        exception1.message == errorMessage
        exception2.code == "serviceUnavailable"
        exception2.message == null
        result.getStatusCode().value() == 503
        result2.getCode() == exception1.code
        result2.getMessage() == "com.ccctc.adaptor.exception.ServiceUnavailableException"
    }

    def "Test RuntimeException"() {
        setup:
        def request = Mock(WebRequest)
        def handler = new ApiExceptionHandler()
        def exception = new RuntimeException()

        when:
        def result = handler.handleRuntimeException(exception, request)

        then:
        result.getStatusCode().value()==500
    }

    def "Test EntityConflictException()"() {
        setup:
        def request = Mock(WebRequest)
        def handler = new ApiExceptionHandler()

        when:
        def exception1 = new EntityConflictException(errorMessage)
        def exception2 = new EntityConflictException(null)
        def result = handler.handleEntityConflictException(exception1,request)
        ErrorResource result2 = handler.handleEntityConflictException(exception2, request).getBody()

        then:
        exception1.code == "entityAlreadyExists"
        exception1.message == errorMessage
        exception2.code == "entityAlreadyExists"
        exception2.message == null
        result.getStatusCode().value() == 409
        result2.getCode() == "entityAlreadyExists"
        result2.getMessage() == "com.ccctc.adaptor.exception.EntityConflictException"
    }
}
