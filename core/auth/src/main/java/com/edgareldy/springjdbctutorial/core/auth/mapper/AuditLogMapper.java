package com.edgareldy.springjdbctutorial.core.auth.mapper;

import com.edgareldy.springjdbctutorial.core.auth.dto.AuditLogDto;
import com.edgareldy.springjdbctutorial.core.auth.entity.AuditLog;

/**
 * Converts between the AuditLog entity and the AuditLogDto.
 * <p>
 * Created edgar.muhamyangabo on 9/19/26
 * Author : edgar.muhamyangabo
 * Date : 9/19/26
 * Project : spring-jdbc-tutorial
 */
public class AuditLogMapper {

    public AuditLogDto toDto(AuditLog log) {
        if (log == null) {
            return null;
        }
        return new AuditLogDto(log.getId(), log.getActorUserId(), log.getAction(), log.getEntityType(),
                log.getEntityId(), log.getDetails(), log.getCreatedAt());
    }

    public AuditLog toEntity(AuditLogDto dto) {
        if (dto == null) {
            return null;
        }
        return new AuditLog(dto.getId(), dto.getActorUserId(), dto.getAction(), dto.getEntityType(),
                dto.getEntityId(), dto.getDetails(), dto.getCreatedAt());
    }
}
