package com.edgareldy.springjdbctutorial.ws.security;

import com.edgareldy.springjdbctutorial.core.auth.service.ActorProvider;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;

/**
 * Tells the core audit logger who the current actor is, read from the SecurityContext (null when anonymous).
 * <p>
 * Created edgar.muhamyangabo on 9/19/26
 * Author : edgar.muhamyangabo
 * Date : 9/19/26
 * Project : spring-jdbc-tutorial
 */
// core has no web or security dependency: it only knows the ActorProvider interface, and ws supplies
// this implementation so audit rows carry the id of the authenticated user.
public class SecurityContextActorProvider implements ActorProvider {

    @Override
    public Long currentUserId() {
        Authentication authentication = SecurityContextHolder.getContext().getAuthentication();
        if (authentication != null && authentication.getPrincipal() instanceof AuthenticatedUser user) {
            return user.getUserId();
        }
        return null;
    }
}
