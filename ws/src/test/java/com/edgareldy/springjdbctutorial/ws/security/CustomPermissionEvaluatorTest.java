package com.edgareldy.springjdbctutorial.ws.security;

import static org.assertj.core.api.Assertions.assertThat;

import java.util.List;
import org.junit.jupiter.api.Test;
import org.springframework.security.authentication.AnonymousAuthenticationToken;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.GrantedAuthority;
import org.springframework.security.core.authority.AuthorityUtils;
import org.springframework.security.core.authority.SimpleGrantedAuthority;

/**
 * Unit tests of CustomPermissionEvaluator on hand-built Authentication objects (no Spring context, no mocks).
 * <p>
 * Created edgar.muhamyangabo on 9/19/26
 * Author : edgar.muhamyangabo
 * Date : 9/19/26
 * Project : spring-jdbc-tutorial
 */
class CustomPermissionEvaluatorTest {

    private final CustomPermissionEvaluator evaluator = new CustomPermissionEvaluator();

    // The 3-argument constructor of UsernamePasswordAuthenticationToken marks the token as authenticated,
    // like JwtAuthFilter does once the JWT is verified
    private static Authentication authenticated(String... authorities) {
        return new UsernamePasswordAuthenticationToken("user", null, AuthorityUtils.createAuthorityList(authorities));
    }

    @Test
    void _01_ShouldAllow_WhenAuthorityMatchesResourceAndAction() {
        assertThat(evaluator.hasPermission(authenticated("USER:READ", "ROLE:WRITE"), "ROLE", "WRITE")).isTrue();
    }

    @Test
    void _02_ShouldDeny_WhenAuthorityIsMissing() {
        assertThat(evaluator.hasPermission(authenticated("USER:READ"), "ROLE", "WRITE")).isFalse();
    }

    @Test
    void _03_ShouldDenyWrite_WhenOnlyReadIsGranted() {
        assertThat(evaluator.hasPermission(authenticated("USER:READ"), "USER", "WRITE")).isFalse();
    }

    @Test
    void _04_ShouldDenyOtherResource_WhenSameActionIsGrantedOnAnotherResource() {
        assertThat(evaluator.hasPermission(authenticated("ROLE:WRITE"), "USER", "WRITE")).isFalse();
    }

    @Test
    void _05_ShouldAllow_WhenCaseOrSurroundingSpacesDiffer() {
        assertThat(evaluator.hasPermission(authenticated("user:read"), "USER", "READ")).isTrue();
        assertThat(evaluator.hasPermission(authenticated("USER:READ"), " user ", " Read ")).isTrue();
    }

    @Test
    void _06_ShouldDeny_WhenAuthenticationIsNull() {
        assertThat(evaluator.hasPermission(null, "USER", "READ")).isFalse();
    }

    @Test
    void _07_ShouldDeny_WhenAuthenticationIsAnonymousEvenWithAMatchingAuthority() {
        Authentication anonymous = new AnonymousAuthenticationToken("key", "anonymousUser",
                AuthorityUtils.createAuthorityList("USER:READ"));

        assertThat(evaluator.hasPermission(anonymous, "USER", "READ")).isFalse();
    }

    @Test
    void _08_ShouldDeny_WhenAuthenticationIsNotAuthenticated() {
        // The 2-argument constructor builds an UNauthenticated token, even if authorities were added later
        Authentication notAuthenticated = new UsernamePasswordAuthenticationToken("user", "credentials");

        assertThat(evaluator.hasPermission(notAuthenticated, "USER", "READ")).isFalse();
    }

    @Test
    void _09_ShouldDeny_WhenTargetOrPermissionIsNotAString() {
        Authentication auth = authenticated("USER:READ");

        assertThat(evaluator.hasPermission(auth, 42L, "READ")).isFalse();
        assertThat(evaluator.hasPermission(auth, "USER", 42L)).isFalse();
        assertThat(evaluator.hasPermission(auth, new Object(), new Object())).isFalse();
    }

    @Test
    void _10_ShouldDeny_WhenTargetOrPermissionIsNull() {
        Authentication auth = authenticated("USER:READ");

        assertThat(evaluator.hasPermission(auth, null, "READ")).isFalse();
        assertThat(evaluator.hasPermission(auth, "USER", null)).isFalse();
    }

    @Test
    void _11_ShouldDenyWithoutThrowing_WhenAnAuthorityHasANullValue() {
        GrantedAuthority nullAuthority = () -> null;
        Authentication auth = new UsernamePasswordAuthenticationToken("user", null,
                List.of(nullAuthority, new SimpleGrantedAuthority("ROLE:READ")));

        assertThat(evaluator.hasPermission(auth, "USER", "READ")).isFalse();
        assertThat(evaluator.hasPermission(auth, "ROLE", "READ")).isTrue();
    }

    @Test
    void _12_ShouldDeny_WhenThereIsNoAuthority() {
        assertThat(evaluator.hasPermission(authenticated(), "USER", "READ")).isFalse();
    }

    @Test
    void _13_ShouldAlwaysDeny_WhenUsingTheIdAndTypeOverload() {
        Authentication auth = authenticated("USER:READ");

        assertThat(evaluator.hasPermission(auth, 1L, "USER", "READ")).isFalse();
    }
}
