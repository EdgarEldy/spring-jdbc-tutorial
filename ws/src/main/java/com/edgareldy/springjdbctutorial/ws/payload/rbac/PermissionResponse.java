package com.edgareldy.springjdbctutorial.ws.payload.rbac;

import java.util.Objects;

/**
 * A permission as returned by the API.
 * <p>
 * Created edgar.muhamyangabo on 9/19/26
 * Author : edgar.muhamyangabo
 * Date : 9/19/26
 * Project : spring-jdbc-tutorial
 */
public class PermissionResponse {

    private Long id;

    private String resource;

    private String action;

    public PermissionResponse() {
    }

    public PermissionResponse(Long id, String resource, String action) {
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
        PermissionResponse that = (PermissionResponse) o;
        return Objects.equals(id, that.id) && Objects.equals(resource, that.resource) && Objects.equals(action, that.action);
    }

    @Override
    public int hashCode() {
        return Objects.hash(id, resource, action);
    }

    @Override
    public String toString() {
        return "PermissionResponse{" +
                "id=" + id +
                ", resource=" + resource +
                ", action=" + action +
                "}";
    }
}
