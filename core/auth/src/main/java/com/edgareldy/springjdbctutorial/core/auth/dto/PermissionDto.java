package com.edgareldy.springjdbctutorial.core.auth.dto;

import java.util.Objects;

/**
 * Business shape of a permission (a resource and an action).
 * <p>
 * Created edgar.muhamyangabo on 9/19/26
 * Author : edgar.muhamyangabo
 * Date : 9/19/26
 * Project : spring-jdbc-tutorial
 */
public class PermissionDto {

    private Long id;

    private String resource;

    private String action;

    public PermissionDto() {
    }

    public PermissionDto(Long id, String resource, String action) {
        this.id = id;
        this.resource = resource;
        this.action = action;
    }

    public Long getId() {
        return id;
    }

    public void setId(Long id) {
        this.id = id;
    }

    public String getResource() {
        return resource;
    }

    public void setResource(String resource) {
        this.resource = resource;
    }

    public String getAction() {
        return action;
    }

    public void setAction(String action) {
        this.action = action;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) {
            return true;
        }
        if (o == null || getClass() != o.getClass()) {
            return false;
        }
        PermissionDto that = (PermissionDto) o;
        return Objects.equals(id, that.id) && Objects.equals(resource, that.resource)
                && Objects.equals(action, that.action);
    }

    @Override
    public int hashCode() {
        return Objects.hash(id, resource, action);
    }

    @Override
    public String toString() {
        return "PermissionDto{" +
                "id=" + id +
                ", resource=" + resource +
                ", action=" + action +
                "}";
    }
}
