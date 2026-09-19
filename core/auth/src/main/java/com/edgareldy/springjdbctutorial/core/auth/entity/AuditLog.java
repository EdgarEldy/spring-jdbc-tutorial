package com.edgareldy.springjdbctutorial.core.auth.entity;

import jakarta.persistence.Column;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import java.time.Instant;
import java.util.Objects;

/**
 * Row of the audit_logs table: one RBAC mutation or refusal. Annotations are documentation only.
 * <p>
 * Created edgar.muhamyangabo on 9/19/26
 * Author : edgar.muhamyangabo
 * Date : 9/19/26
 * Project : spring-jdbc-tutorial
 */
@Table(name = "audit_logs")
public class AuditLog {

    @Id
    @Column(name = "id")
    private Long id;

    @Column(name = "actor_user_id")
    private Long actorUserId;

    @Column(name = "action")
    private String action;

    @Column(name = "entity_type")
    private String entityType;

    @Column(name = "entity_id")
    private Long entityId;

    @Column(name = "details")
    private String details;

    @Column(name = "created_at")
    private Instant createdAt;

    public AuditLog() {
    }

    public AuditLog(Long id, Long actorUserId, String action, String entityType, Long entityId, String details, Instant createdAt) {
        this.id = id;
        this.actorUserId = actorUserId;
        this.action = action;
        this.entityType = entityType;
        this.entityId = entityId;
        this.details = details;
        this.createdAt = createdAt;
    }

    public Long getId() {
        return id;
    }

    public void setId(Long id) {
        this.id = id;
    }

    public Long getActorUserId() {
        return actorUserId;
    }

    public void setActorUserId(Long actorUserId) {
        this.actorUserId = actorUserId;
    }

    public String getAction() {
        return action;
    }

    public void setAction(String action) {
        this.action = action;
    }

    public String getEntityType() {
        return entityType;
    }

    public void setEntityType(String entityType) {
        this.entityType = entityType;
    }

    public Long getEntityId() {
        return entityId;
    }

    public void setEntityId(Long entityId) {
        this.entityId = entityId;
    }

    public String getDetails() {
        return details;
    }

    public void setDetails(String details) {
        this.details = details;
    }

    public Instant getCreatedAt() {
        return createdAt;
    }

    public void setCreatedAt(Instant createdAt) {
        this.createdAt = createdAt;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) {
            return true;
        }
        if (o == null || getClass() != o.getClass()) {
            return false;
        }
        AuditLog that = (AuditLog) o;
        return Objects.equals(id, that.id)
                && Objects.equals(actorUserId, that.actorUserId)
                && Objects.equals(action, that.action)
                && Objects.equals(entityType, that.entityType)
                && Objects.equals(entityId, that.entityId)
                && Objects.equals(details, that.details)
                && Objects.equals(createdAt, that.createdAt);
    }

    @Override
    public int hashCode() {
        return Objects.hash(id, actorUserId, action, entityType, entityId, details, createdAt);
    }

    @Override
    public String toString() {
        return "AuditLog{" +
                "id=" + id +
                ", actorUserId=" + actorUserId +
                ", action=" + action +
                ", entityType=" + entityType +
                ", entityId=" + entityId +
                ", details=" + details +
                ", createdAt=" + createdAt +
                "}";
    }
}
