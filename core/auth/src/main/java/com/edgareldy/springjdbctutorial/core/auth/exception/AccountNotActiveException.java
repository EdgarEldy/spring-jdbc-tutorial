package com.edgareldy.springjdbctutorial.core.auth.exception;

/**
 * The password was correct but the account is not activated or is locked. Only thrown after the
 * password check, so it never reveals whether an email exists.
 * <p>
 * Created edgar.muhamyangabo on 9/19/26
 * Author : edgar.muhamyangabo
 * Date : 9/19/26
 * Project : spring-jdbc-tutorial
 */
public class AccountNotActiveException extends AuthenticationFailedException {

    private static final long serialVersionUID = 1L;

    public AccountNotActiveException() {
        super("Account is not activated or is locked");
    }
}
