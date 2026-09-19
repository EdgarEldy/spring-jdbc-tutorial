package com.edgareldy.springjdbctutorial.core.auth.mapper;

import static org.assertj.core.api.Assertions.assertThat;

import com.edgareldy.springjdbctutorial.core.auth.dto.UserDto;
import com.edgareldy.springjdbctutorial.core.auth.entity.User;
import java.util.List;
import org.junit.jupiter.api.Test;

/**
 * Tests UserMapper (entity to dto and back) on hand-built objects, no mocks.
 * <p>
 * Created edgar.muhamyangabo on 9/19/26
 * Author : edgar.muhamyangabo
 * Date : 9/19/26
 * Project : spring-jdbc-tutorial
 */
class UserMapperTest {

    private final UserMapper mapper = new UserMapper();

    @Test
    void _01_ShouldCopyProfileFieldsButNeverThePassword_WhenConvertingEntityToDto() {
        User user = new User(1L, "Alice", "Martin", "alice@example.com", "secret-hash", true, false);

        UserDto dto = mapper.toDto(user);

        assertThat(dto.getId()).isEqualTo(1L);
        assertThat(dto.getFirstName()).isEqualTo("Alice");
        assertThat(dto.getLastName()).isEqualTo("Martin");
        assertThat(dto.getEmail()).isEqualTo("alice@example.com");
        assertThat(dto.isEnabled()).isTrue();
        assertThat(dto.isAccountLocked()).isFalse();
        assertThat(dto.getPassword()).isNull();
        assertThat(dto.getRoles()).isEmpty();
        assertThat(dto.getPermissions()).isEmpty();
    }

    @Test
    void _02_ShouldReturnNull_WhenEntityIsNull() {
        assertThat(mapper.toDto(null)).isNull();
    }

    @Test
    void _03_ShouldUseTheGivenHashAndIgnoreTheDtoPassword_WhenConvertingDtoToEntity() {
        UserDto dto = new UserDto(2L, "Bob", "Durand", "bob@example.com", "clear-password", false, true,
                List.of("ADMIN"), List.of("USER:READ"));

        User user = mapper.toEntity(dto, "the-bcrypt-hash");

        assertThat(user.getId()).isEqualTo(2L);
        assertThat(user.getFirstName()).isEqualTo("Bob");
        assertThat(user.getLastName()).isEqualTo("Durand");
        assertThat(user.getEmail()).isEqualTo("bob@example.com");
        assertThat(user.getPassword()).isEqualTo("the-bcrypt-hash");
        assertThat(user.isEnabled()).isFalse();
        assertThat(user.isAccountLocked()).isTrue();
    }

    @Test
    void _04_ShouldReturnNull_WhenDtoIsNull() {
        assertThat(mapper.toEntity(null, "hash")).isNull();
    }
}
