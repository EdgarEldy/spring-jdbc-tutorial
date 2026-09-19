package com.edgareldy.springjdbctutorial.ws.security;

import static org.assertj.core.api.Assertions.assertThat;

import java.time.Instant;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;
import org.springframework.security.authentication.AnonymousAuthenticationToken;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.authority.AuthorityUtils;
import org.springframework.security.core.context.SecurityContextHolder;

/**
 * Unit tests of SecurityContextActorProvider: the actor id comes from the AuthenticatedUser principal of the
 * SecurityContext, and is null whenever nobody real is authenticated.
 * <p>
 * Created edgar.muhamyangabo on 9/19/26
 * Author : edgar.muhamyangabo
 * Date : 9/19/26
 * Project : spring-jdbc-tutorial
 */
class SecurityContextActorProviderTest {

    private final SecurityContextActorProvider provider = new SecurityContextActorProvider();

    // SecurityContextHolder is thread-local static state: always clear it so no test leaks into another
    @AfterEach
    void clearContext() {
        SecurityContextHolder.clearContext();
    }

    @Test
    void _01_ShouldReturnTheUserId_WhenAnAuthenticatedUserIsInTheContext() {
        AuthenticatedUser principal = new AuthenticatedUser(42L, "a@example.com", "jti", "hash",
                Instant.now(), Instant.now().plusSeconds(60));
        SecurityContextHolder.getContext().setAuthentication(
                new UsernamePasswordAuthenticationToken(principal, null, AuthorityUtils.NO_AUTHORITIES));

        assertThat(provider.currentUserId()).isEqualTo(42L);
    }

    @Test
    void _02_ShouldReturnNull_WhenThereIsNoAuthentication() {
        assertThat(provider.currentUserId()).isNull();
    }

    @Test
    void _03_ShouldReturnNull_WhenTheAuthenticationIsAnonymous() {
        SecurityContextHolder.getContext().setAuthentication(new AnonymousAuthenticationToken("key",
                "anonymousUser", AuthorityUtils.createAuthorityList("ROLE_ANONYMOUS")));

        assertThat(provider.currentUserId()).isNull();
    }

    @Test
    void _04_ShouldReturnNull_WhenThePrincipalIsNotAnAuthenticatedUser() {
        SecurityContextHolder.getContext().setAuthentication(
                new UsernamePasswordAuthenticationToken("just-a-name", null, AuthorityUtils.NO_AUTHORITIES));

        assertThat(provider.currentUserId()).isNull();
    }
}
