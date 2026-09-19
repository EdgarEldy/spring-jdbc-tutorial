package com.edgareldy.springjdbctutorial.ws.payload.rbac;

import java.util.List;
import java.util.Objects;

/**
 * A role with its permissions as returned by the API.
 * <p>
 * Created edgar.muhamyangabo on 9/19/26
 * Author : edgar.muhamyangabo
 * Date : 9/19/26
 * Project : spring-jdbc-tutorial
 */
public class RoleResponse {

    private Long id;

    private String roleName;

    private List<PermissionResponse> permissions;

    public RoleResponse() {
    }

    public RoleResponse(Long id, String roleName, List<PermissionResponse> permissions) {
        this.id = id;
        this.roleName = roleName;
        this.permissions = permissions;
    }

    public Long getId() {
        return id;
    }

    public void setId(Long id) {
        this.id = id;
    }

    public String getRoleName() {
        return roleName;
    }

    public void setRoleName(String roleName) {
        this.roleName = roleName;
    }

    public List<PermissionResponse> getPermissions() {
        return permissions;
    }

    public void setPermissions(List<PermissionResponse> permissions) {
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
        RoleResponse that = (RoleResponse) o;
        return Objects.equals(id, that.id) && Objects.equals(roleName, that.roleName) && Objects.equals(permissions, that.permissions);
    }

    @Override
    public int hashCode() {
        return Objects.hash(id, roleName, permissions);
    }

    @Override
    public String toString() {
        return "RoleResponse{" +
                "id=" + id +
                ", roleName=" + roleName +
                ", permissions=" + permissions +
                "}";
    }
}
