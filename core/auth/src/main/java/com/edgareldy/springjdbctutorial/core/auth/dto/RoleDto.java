package com.edgareldy.springjdbctutorial.core.auth.dto;

import java.util.List;
import java.util.Objects;

/**
 * Business shape of a role. On output the permissions are filled by the service, on input only roleName is read.
 * <p>
 * Created edgar.muhamyangabo on 9/19/26
 * Author : edgar.muhamyangabo
 * Date : 9/19/26
 * Project : spring-jdbc-tutorial
 */
public class RoleDto {

    private Long id;

    private String roleName;

    private List<PermissionDto> permissions;

    public RoleDto() {
    }

    public RoleDto(Long id, String roleName, List<PermissionDto> permissions) {
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

    public List<PermissionDto> getPermissions() {
        return permissions;
    }

    public void setPermissions(List<PermissionDto> permissions) {
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
        RoleDto that = (RoleDto) o;
        return Objects.equals(id, that.id) && Objects.equals(roleName, that.roleName)
                && Objects.equals(permissions, that.permissions);
    }

    @Override
    public int hashCode() {
        return Objects.hash(id, roleName, permissions);
    }

    @Override
    public String toString() {
        return "RoleDto{" +
                "id=" + id +
                ", roleName=" + roleName +
                ", permissions=" + permissions +
                "}";
    }
}
