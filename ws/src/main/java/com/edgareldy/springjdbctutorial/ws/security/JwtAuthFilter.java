package com.edgareldy.springjdbctutorial.ws.security;

import com.edgareldy.springjdbctutorial.core.auth.service.AuthService;
import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.GrantedAuthority;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.web.filter.OncePerRequestFilter;

import java.io.IOException;
import java.util.List;
import java.util.Optional;

/**
 * Authenticates a request from its Bearer JWT: verifies it, rejects revoked tokens, and fills the SecurityContext. Never throws for a bad token.
 * <p>
 * Created edgar.muhamyangabo on 9/19/26
 * Author : edgar.muhamyangabo
 * Date : 9/19/26
 * Project : spring-jdbc-tutorial
 */
// OncePerRequestFilter guarantees one execution per request even with forwards or error dispatches.
// It is created inside the SecurityFilterChain (not a bean, not registered in the servlet container),
// so it runs only inside the Spring Security chain. When the token is missing, invalid or revoked the
// context is left empty and the AuthenticationEntryPoint answers 401 for protected routes.
public class JwtAuthFilter extends OncePerRequestFilter {

    private static final String BEARER_PREFIX = "Bearer ";

    private final JwtService jwtService;
    private final AuthService authService;

    public JwtAuthFilter(JwtService jwtService, AuthService authService) {
        this.jwtService = jwtService;
        this.authService = authService;
    }

    @Override
    protected void doFilterInternal(HttpServletRequest request, HttpServletResponse response,
                                    FilterChain filterChain) throws ServletException, IOException {
        String header = request.getHeader("Authorization");
        if (header != null && header.startsWith(BEARER_PREFIX)) {
            String jwt = header.substring(BEARER_PREFIX.length()).trim();
            Optional<ParsedToken> parsed = jwtService.parse(jwt);
            if (parsed.isPresent()) {
                String tokenHash = JwtService.sha256Hex(jwt);
                if (!authService.isTokenBlacklisted(tokenHash)) {
                    ParsedToken token = parsed.get();
                    AuthenticatedUser principal = new AuthenticatedUser(token.getUserId(), token.getEmail(),
                            token.getJti(), tokenHash, token.getIssuedAt(), token.getExpiresAt());
                    List<GrantedAuthority> authorities = token.getPermissions().stream()
                            .<GrantedAuthority>map(SimpleGrantedAuthority::new)
                            .toList();
                    SecurityContextHolder.getContext().setAuthentication(
                            new UsernamePasswordAuthenticationToken(principal, null, authorities));
                }
            }
        }
        filterChain.doFilter(request, response);
    }
}
