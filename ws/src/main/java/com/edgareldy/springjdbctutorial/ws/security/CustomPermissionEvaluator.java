package com.edgareldy.springjdbctutorial.ws.security;

import org.springframework.security.access.PermissionEvaluator;
import org.springframework.security.authentication.AnonymousAuthenticationToken;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.GrantedAuthority;

import java.io.Serializable;
import java.util.Locale;

/**
 * The only place where permissions are resolved: answers hasPermission(resource, action) from the authorities of the Authentication.
 * <p>
 * Created edgar.muhamyangabo on 9/19/26
 * Author : edgar.muhamyangabo
 * Date : 9/19/26
 * Project : spring-jdbc-tutorial
 */
// A PermissionEvaluator is the Spring Security extension point behind the hasPermission(...) function of
// @PreAuthorize expressions. The authorities ("RESOURCE:ACTION") were rebuilt by JwtAuthFilter from the
// permissions embedded in the JWT at login, so a check is a pure in-memory lookup: no database call per
// request.
public class CustomPermissionEvaluator implements PermissionEvaluator {

    @Override
    public boolean hasPermission(Authentication authentication, Object targetDomainObject, Object permission) {
        try {
            if (authentication == null || !authentication.isAuthenticated()
                    || authentication instanceof AnonymousAuthenticationToken
                    || !(targetDomainObject instanceof String resource) || !(permission instanceof String action)) {
                return false;
            }
            String wanted = normalize(resource) + ":" + normalize(action);
            for (GrantedAuthority authority : authentication.getAuthorities()) {
                String value = authority == null ? null : authority.getAuthority();
                if (value != null && wanted.equals(normalize(value))) {
                    return true;
                }
            }
            return false;
        } catch (RuntimeException e) {
            return false;
        }
    }

    /** Ids are not used by this project's permissions: the (id, type, permission) form always denies. */
    @Override
    public boolean hasPermission(Authentication authentication, Serializable targetId, String targetType,
                                 Object permission) {
        return false;
    }

    private static String normalize(String value) {
        return value.trim().toUpperCase(Locale.ROOT);
    }
}
