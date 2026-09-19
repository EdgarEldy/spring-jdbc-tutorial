package com.edgareldy.springjdbctutorial.ws.security;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import com.edgareldy.springjdbctutorial.ws.support.JwtTestSupport;
import java.nio.file.Files;
import java.nio.file.Path;
import java.security.KeyPair;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;
import org.springframework.core.io.DefaultResourceLoader;
import org.springframework.core.io.ResourceLoader;

/**
 * Tests JwtKeyLoader: valid PEM keys load, and every misconfiguration fails with a clear message.
 * <p>
 * Created edgar.muhamyangabo on 9/19/26
 * Author : edgar.muhamyangabo
 * Date : 9/19/26
 * Project : spring-jdbc-tutorial
 */
class JwtKeyLoaderTest {

    private static final String PRIVATE_PROPERTY = "app.jwt.private-key-location";
    private static final String PUBLIC_PROPERTY = "app.jwt.public-key-location";

    private final ResourceLoader loader = new DefaultResourceLoader();

    @TempDir
    Path tempDir;

    private String write(String name, String content) throws Exception {
        return "file:" + Files.writeString(tempDir.resolve(name), content);
    }

    @Test
    void _01_ShouldLoadBothKeys_WhenPemFilesAreValid() throws Exception {
        KeyPair pair = JwtTestSupport.generateKeyPair();
        String privateLocation = write("private.pem", JwtTestSupport.privatePem(pair.getPrivate()));
        String publicLocation = write("public.pem", JwtTestSupport.publicPem(pair.getPublic()));

        assertThat(JwtKeyLoader.loadPrivateKey(loader, PRIVATE_PROPERTY, privateLocation)).isEqualTo(pair.getPrivate());
        assertThat(JwtKeyLoader.loadPublicKey(loader, PUBLIC_PROPERTY, publicLocation)).isEqualTo(pair.getPublic());
    }

    @Test
    void _02_ShouldFailNamingTheProperty_WhenLocationIsMissing() {
        assertThatThrownBy(() -> JwtKeyLoader.loadPrivateKey(loader, PRIVATE_PROPERTY, null))
                .isInstanceOf(IllegalStateException.class)
                .hasMessageContaining("Missing required property " + PRIVATE_PROPERTY);
        assertThatThrownBy(() -> JwtKeyLoader.loadPublicKey(loader, PUBLIC_PROPERTY, "  "))
                .isInstanceOf(IllegalStateException.class)
                .hasMessageContaining("Missing required property " + PUBLIC_PROPERTY);
    }

    @Test
    void _03_ShouldFailWithWrongTypeMessage_WhenPublicKeyPemIsGivenAsPrivateKey() throws Exception {
        KeyPair pair = JwtTestSupport.generateKeyPair();
        String location = write("public.pem", JwtTestSupport.publicPem(pair.getPublic()));

        assertThatThrownBy(() -> JwtKeyLoader.loadPrivateKey(loader, PRIVATE_PROPERTY, location))
                .isInstanceOf(IllegalStateException.class)
                .hasMessageContaining("must be a PEM 'PRIVATE KEY' block");
    }

    @Test
    void _04_ShouldFailWithWrongTypeMessage_WhenPrivateKeyPemIsGivenAsPublicKey() throws Exception {
        KeyPair pair = JwtTestSupport.generateKeyPair();
        String location = write("private.pem", JwtTestSupport.privatePem(pair.getPrivate()));

        assertThatThrownBy(() -> JwtKeyLoader.loadPublicKey(loader, PUBLIC_PROPERTY, location))
                .isInstanceOf(IllegalStateException.class)
                .hasMessageContaining("must be a PEM 'PUBLIC KEY' block");
    }

    @Test
    void _05_ShouldFailWithReadMessage_WhenFileDoesNotExist() {
        String location = "file:" + tempDir.resolve("absent.pem");

        assertThatThrownBy(() -> JwtKeyLoader.loadPrivateKey(loader, PRIVATE_PROPERTY, location))
                .isInstanceOf(IllegalStateException.class)
                .hasMessageContaining("Cannot read the key of " + PRIVATE_PROPERTY);
    }

    @Test
    void _06_ShouldFailWithInvalidKeyMessage_WhenPemBodyIsNotAKey() throws Exception {
        String garbage = JwtTestSupport.pem("PRIVATE KEY", new byte[]{1, 2, 3, 4});
        String location = write("garbage.pem", garbage);

        assertThatThrownBy(() -> JwtKeyLoader.loadPrivateKey(loader, PRIVATE_PROPERTY, location))
                .isInstanceOf(IllegalStateException.class)
                .hasMessageContaining("Invalid RSA private key");
    }

    @Test
    void _07_ShouldFailWithBase64Message_WhenPemBodyIsNotBase64() throws Exception {
        String location = write("bad.pem", "-----BEGIN PUBLIC KEY-----\n***not base64***\n-----END PUBLIC KEY-----\n");

        assertThatThrownBy(() -> JwtKeyLoader.loadPublicKey(loader, PUBLIC_PROPERTY, location))
                .isInstanceOf(IllegalStateException.class)
                .hasMessageContaining("not valid Base64");
    }
}
