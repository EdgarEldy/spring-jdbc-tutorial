package com.edgareldy.springjdbctutorial.core.auth.dto;

import java.util.List;
import java.util.Objects;

/**
 * Business shape of a user. On input the password carries the RAW password, on output it is always null.
 * Roles are role names, permissions are "RESOURCE:ACTION" codes; both are filled by the service.
 * <p>
 * Created edgar.muhamyangabo on 9/19/26
 * Author : edgar.muhamyangabo
 * Date : 9/19/26
 * Project : spring-jdbc-tutorial
 */
public class UserDto {

    private Long id;

    private String firstName;

    private String lastName;

    private String email;

    private String password;

    private boolean enabled;

    private boolean accountLocked;

    private List<String> roles;

    private List<String> permissions;

    public UserDto() {
    }

    public UserDto(Long id, String firstName, String lastName, String email, String password, boolean enabled, boolean accountLocked, List<String> roles, List<String> permissions) {
        this.id = id;
        this.firstName = firstName;
        this.lastName = lastName;
        this.email = email;
        this.password = password;
        this.enabled = enabled;
        this.accountLocked = accountLocked;
        this.roles = roles;
        this.permissions = permissions;
    }

    public Long getId() {
        return id;
    }

    public void setId(Long id) {
        this.id = id;
    }

    public String getFirstName() {
        return firstName;
    }

    public void setFirstName(String firstName) {
        this.firstName = firstName;
    }

    public String getLastName() {
        return lastName;
    }

    public void setLastName(String lastName) {
        this.lastName = lastName;
    }

    public String getEmail() {
        return email;
    }

    public void setEmail(String email) {
        this.email = email;
    }

    public String getPassword() {
        return password;
    }

    public void setPassword(String password) {
        this.password = password;
    }

    public boolean isEnabled() {
        return enabled;
    }

    public void setEnabled(boolean enabled) {
        this.enabled = enabled;
    }

    public boolean isAccountLocked() {
        return accountLocked;
    }

    public void setAccountLocked(boolean accountLocked) {
        this.accountLocked = accountLocked;
    }

    public List<String> getRoles() {
        return roles;
    }

    public void setRoles(List<String> roles) {
        this.roles = roles;
    }

    public List<String> getPermissions() {
        return permissions;
    }

    public void setPermissions(List<String> permissions) {
        this.permissions = permissions;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) {
            return true;
        }
        if (o == null || getClass() != o.getClass()) {
            return false;
        }
        UserDto that = (UserDto) o;
        return Objects.equals(id, that.id)
                && Objects.equals(firstName, that.firstName)
                && Objects.equals(lastName, that.lastName)
                && Objects.equals(email, that.email)
                && Objects.equals(password, that.password)
                && enabled == that.enabled
                && accountLocked == that.accountLocked
                && Objects.equals(roles, that.roles)
                && Objects.equals(permissions, that.permissions);
    }

    @Override
    public int hashCode() {
        return Objects.hash(id, firstName, lastName, email, password, enabled, accountLocked, roles, permissions);
    }

    @Override
    public String toString() {
        return "UserDto{" +
                "id=" + id +
                ", firstName=" + firstName +
                ", lastName=" + lastName +
                ", email=" + email +
                ", password=" + (password == null ? "null" : "****") +
                ", enabled=" + enabled +
                ", accountLocked=" + accountLocked +
                ", roles=" + roles +
                ", permissions=" + permissions +
                "}";
    }
}
