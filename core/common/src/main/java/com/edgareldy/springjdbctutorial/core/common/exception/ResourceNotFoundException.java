package com.edgareldy.springjdbctutorial.core.common.exception;

/**
 * Thrown by a service when a requested resource does not exist (mapped to HTTP 404 by the web layer).
 * It lives in core so no module depends on anything web-related.
 * <p>
 * Created edgar.muhamyangabo on 9/19/26
 * Author : edgar.muhamyangabo
 * Date : 9/19/26
 * Project : spring-jdbc-tutorial
 */
public class ResourceNotFoundException extends RuntimeException {

    private static final long serialVersionUID = 1L;

    public ResourceNotFoundException(String message) {
        super(message);
    }

    public ResourceNotFoundException(String message, Throwable cause) {
        super(message, cause);
    }
}
