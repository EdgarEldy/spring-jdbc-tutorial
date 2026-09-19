package com.edgareldy.springjdbctutorial.ws.payload.rbac;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;

import java.util.Objects;

/**
 * Body of the create and update permission calls (resource and action, letters and underscore only).
 * <p>
 * Created edgar.muhamyangabo on 9/19/26
 * Author : edgar.muhamyangabo
 * Date : 9/19/26
 * Project : spring-jdbc-tutorial
 */
public class PermissionRequest {

    @NotBlank
    @Size(max = 100)
    @Pattern(regexp = "[A-Za-z_]+")
    private String resource;

    @NotBlank
    @Size(max = 100)
    @Pattern(regexp = "[A-Za-z_]+")
    private String action;

    public PermissionRequest() {
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
        PermissionRequest that = (PermissionRequest) o;
        return Objects.equals(resource, that.resource) && Objects.equals(action, that.action);
    }

    @Override
    public int hashCode() {
        return Objects.hash(resource, action);
    }

    @Override
    public String toString() {
        return "PermissionRequest{" +
                "resource=" + resource +
                ", action=" + action +
                "}";
    }
}
