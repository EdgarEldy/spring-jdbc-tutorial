package com.edgareldy.springjdbctutorial.ws.payload.auth;

import java.util.List;
import java.util.Objects;

/**
 * Public view of a user account (never the password).
 * <p>
 * Created edgar.muhamyangabo on 9/19/26
 * Author : edgar.muhamyangabo
 * Date : 9/19/26
 * Project : spring-jdbc-tutorial
 */
public class UserResponse {

    private Long id;

    private String firstName;

    private String lastName;

    private String email;

    private boolean enabled;

    private List<String> roles;

    private List<String> permissions;

    public UserResponse() {
    }

    public UserResponse(Long id, String firstName, String lastName, String email, boolean enabled, List<String> roles, List<String> permissions) {
        this.id = id;
        this.firstName = firstName;
        this.lastName = lastName;
        this.email = email;
        this.enabled = enabled;
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

    public boolean isEnabled() {
        return enabled;
    }

    public void setEnabled(boolean enabled) {
        this.enabled = enabled;
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
        UserResponse that = (UserResponse) o;
        return Objects.equals(id, that.id)
                && Objects.equals(firstName, that.firstName)
                && Objects.equals(lastName, that.lastName)
                && Objects.equals(email, that.email)
                && enabled == that.enabled
                && Objects.equals(roles, that.roles)
                && Objects.equals(permissions, that.permissions);
    }

    @Override
    public int hashCode() {
        return Objects.hash(id, firstName, lastName, email, enabled, roles, permissions);
    }

    @Override
    public String toString() {
        return "UserResponse{" + "id=" + id + ", firstName=" + firstName + ", lastName=" + lastName + ", email=" + email + ", enabled=" + enabled + ", roles=" + roles + ", permissions=" + permissions + "}";
    }
}
