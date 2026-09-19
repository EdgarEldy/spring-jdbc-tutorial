package com.edgareldy.springjdbctutorial.core.auth.entity;

import jakarta.persistence.Column;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import java.util.Objects;

/**
 * Row of the permissions table: an action (READ, WRITE) allowed on a resource (USER, ROLE...). Annotations are documentation only.
 * <p>
 * Created edgar.muhamyangabo on 9/19/26
 * Author : edgar.muhamyangabo
 * Date : 9/19/26
 * Project : spring-jdbc-tutorial
 */
@Table(name = "permissions")
public class Permission {

    @Id
    @Column(name = "id")
    private Long id;

    @Column(name = "resource")
    private String resource;

    @Column(name = "action")
    private String action;

    public Permission() {
    }

    public Permission(Long id, String resource, String action) {
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
        Permission that = (Permission) o;
        return Objects.equals(id, that.id) && Objects.equals(resource, that.resource)
                && Objects.equals(action, that.action);
    }

    @Override
    public int hashCode() {
        return Objects.hash(id, resource, action);
    }

    @Override
    public String toString() {
        return "Permission{" +
                "id=" + id +
                ", resource=" + resource +
                ", action=" + action +
                "}";
    }
}
