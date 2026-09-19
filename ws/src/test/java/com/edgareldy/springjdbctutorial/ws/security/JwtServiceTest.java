package com.edgareldy.springjdbctutorial.ws.security;

import static org.assertj.core.api.Assertions.assertThat;

import com.edgareldy.springjdbctutorial.core.auth.dto.UserDto;
import com.edgareldy.springjdbctutorial.ws.support.JwtTestSupport;
import java.security.KeyPair;
import java.security.PublicKey;
import java.time.Clock;
import java.time.Instant;
import java.time.ZoneOffset;
import java.util.List;
import java.util.Optional;
import org.junit.jupiter.api.Test;
import org.springframework.core.io.DefaultResourceLoader;

/**
 * Tests JwtService (RS256 issue and parse) with a fixed clock: round trip, expiry, tampering, wrong issuer,
 * foreign key, algorithm confusion and unsigned tokens.
 * <p>
 * Created edgar.muhamyangabo on 9/19/26
 * Author : edgar.muhamyangabo
 * Date : 9/19/26
 * Project : spring-jdbc-tutorial
 */
class JwtServiceTest {

    private static final Instant NOW = Instant.parse("2026-09-19T12:00:00Z");

    private final JwtService service = JwtTestSupport.devJwtService(Clock.fixed(NOW, ZoneOffset.UTC));

    private static UserDto user() {
        UserDto dto = new UserDto();
        dto.setId(5L);
        dto.setEmail("alice@example.com");
        dto.setPermissions(List.of("USER:READ", "ROLE:WRITE"));
        return dto;
    }

    @Test
    void _01_ShouldParseBackTheSameClaims_WhenTokenIsIssuedThenParsed() {
        IssuedToken issued = service.issue(user());

        ParsedToken parsed = service.parse(issued.getToken()).orElseThrow();

        assertThat(parsed.getUserId()).isEqualTo(5L);
        assertThat(parsed.getEmail()).isEqualTo("alice@example.com");
        assertThat(parsed.getPermissions()).containsExactly("USER:READ", "ROLE:WRITE");
        assertThat(parsed.getJti()).isNotBlank();
        assertThat(parsed.getIssuedAt()).isEqualTo(NOW);
        assertThat(parsed.getExpiresAt()).isEqualTo(NOW.plusSeconds(3600));
        assertThat(issued.getExpiresAt()).isEqualTo(NOW.plusSeconds(3600));
    }

    @Test
    void _02_ShouldIssueDifferentJti_WhenTwoTokensAreIssuedForTheSameUser() {
        ParsedToken first = service.parse(service.issue(user()).getToken()).orElseThrow();
        ParsedToken second = service.parse(service.issue(user()).getToken()).orElseThrow();

        assertThat(first.getJti()).isNotEqualTo(second.getJti());
    }

    @Test
    void _03_ShouldReturnEmpty_WhenSignatureIsTampered() {
        String token = service.issue(user()).getToken();
        int position = token.length() - 20;
        char original = token.charAt(position);
        String tampered = token.substring(0, position) + (original == 'A' ? 'B' : 'A') + token.substring(position + 1);

        assertThat(service.parse(tampered)).isEmpty();
    }

    @Test
    void _04_ShouldReturnEmpty_WhenPayloadIsReplacedButSignatureKept() {
        String[] parts = service.issue(user()).getToken().split("\\.");
        String otherPayload = service.issue(user()).getToken().split("\\.")[1];
        String swapped = parts[0] + "." + otherPayload + "." + parts[2];

        assertThat(service.parse(swapped)).isEmpty();
    }

    @Test
    void _05_ShouldReturnEmpty_WhenTokenIsExpiredAccordingToTheClock() {
        String token = service.issue(user()).getToken();
        JwtService later = JwtTestSupport.devJwtService(Clock.fixed(NOW.plusSeconds(3601), ZoneOffset.UTC));

        assertThat(later.parse(token)).isEmpty();
        assertThat(JwtTestSupport.devJwtService(Clock.fixed(NOW.plusSeconds(3599), ZoneOffset.UTC)).parse(token))
                .isPresent();
    }

    @Test
    void _06_ShouldReturnEmpty_WhenIssuerIsWrong() {
        // Same dev key pair, so the signature is valid: only the issuer claim differs
        JwtService otherIssuer = new JwtService(new DefaultResourceLoader(),
                "app.jwt.private-key-location", System.getProperty("app.jwt.private-key-location"),
                "app.jwt.public-key-location", System.getProperty("app.jwt.public-key-location"),
                3600L, "someone-else", Clock.fixed(NOW, ZoneOffset.UTC));
        String token = otherIssuer.issue(user()).getToken();

        assertThat(service.parse(token)).isEmpty();
        assertThat(otherIssuer.parse(token)).isPresent();
    }

    @Test
    void _07_ShouldReturnEmpty_WhenTokenIsSignedWithAnotherRsaKey() {
        KeyPair foreign = JwtTestSupport.generateKeyPair();
        String forged = JwtTestSupport.rs256Token(foreign.getPrivate(), 5L, NOW, NOW.plusSeconds(3600));

        assertThat(service.parse(forged)).isEmpty();
    }

    @Test
    void _08_ShouldReturnEmpty_WhenTokenIsHs256SignedWithThePublicKeyBytes() {
        PublicKey publicKey = JwtTestSupport.generateKeyPair().getPublic();
        String forged = JwtTestSupport.hs256TokenSignedWithPublicKey(publicKey, 5L, NOW, NOW.plusSeconds(3600));

        assertThat(service.parse(forged)).isEmpty();
    }

    @Test
    void _09_ShouldReturnEmpty_WhenTokenIsUnsignedWithAlgNone() {
        String unsigned = JwtTestSupport.unsignedToken(5L, NOW, NOW.plusSeconds(3600));

        assertThat(service.parse(unsigned)).isEmpty();
    }

    @Test
    void _10_ShouldReturnEmpty_WhenAlgorithmIsRs384EvenWithAValidSignature() throws Exception {
        // Only RS256 is ever issued: a token with another RSA algorithm is refused, whoever signed it.
        // Signed here with the dev private key, which is the only key whose signature would be accepted.
        java.security.PrivateKey devPrivate = JwtKeyLoader.loadPrivateKey(new DefaultResourceLoader(),
                "app.jwt.private-key-location", System.getProperty("app.jwt.private-key-location"));
        String rs384 = JwtTestSupport.rs384Token(devPrivate, 5L, NOW, NOW.plusSeconds(3600));

        assertThat(service.parse(rs384)).isEmpty();
    }

    @Test
    void _11_ShouldReturnEmpty_WhenTokenHasNoJti() {
        java.security.PrivateKey devPrivate = JwtKeyLoader.loadPrivateKey(new DefaultResourceLoader(),
                "app.jwt.private-key-location", System.getProperty("app.jwt.private-key-location"));
        String noJti = JwtTestSupport.rs256TokenWithoutJti(devPrivate, 5L, NOW, NOW.plusSeconds(3600));

        assertThat(service.parse(noJti)).isEmpty();
    }

    @Test
    void _12_ShouldReturnEmpty_WhenTokenIsNullBlankOrGarbage() {
        assertThat(service.parse(null)).isEmpty();
        assertThat(service.parse("   ")).isEmpty();
        assertThat(service.parse("not-a-jwt")).isEmpty();
        assertThat(service.parse("a.b.c")).isEmpty();
    }

    @Test
    void _13_ShouldIssueEmptyPermissions_WhenUserHasNone() {
        UserDto noPermissions = user();
        noPermissions.setPermissions(null);

        Optional<ParsedToken> parsed = service.parse(service.issue(noPermissions).getToken());

        assertThat(parsed.orElseThrow().getPermissions()).isEmpty();
    }

    @Test
    void _14_ShouldComputeKnownSha256_WhenHashingAValue() {
        assertThat(JwtService.sha256Hex("abc"))
                .isEqualTo("ba7816bf8f01cfea414140de5dae2223b00361a396177a9cb410ff61f20015ad");
    }
}
