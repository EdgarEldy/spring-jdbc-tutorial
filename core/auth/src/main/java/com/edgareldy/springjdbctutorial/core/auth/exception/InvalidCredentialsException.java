package com.edgareldy.springjdbctutorial.core.auth.exception;

/**
 * Unknown email or wrong password: one message for both so an email's existence is never revealed.
 * <p>
 * Created edgar.muhamyangabo on 9/19/26
 * Author : edgar.muhamyangabo
 * Date : 9/19/26
 * Project : spring-jdbc-tutorial
 */
public class InvalidCredentialsException extends AuthenticationFailedException {

    private static final long serialVersionUID = 1L;

    public InvalidCredentialsException() {
        super("Invalid email or password");
    }
}
