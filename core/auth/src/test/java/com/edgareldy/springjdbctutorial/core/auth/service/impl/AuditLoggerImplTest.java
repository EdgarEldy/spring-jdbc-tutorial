package com.edgareldy.springjdbctutorial.core.auth.service.impl;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.edgareldy.springjdbctutorial.core.auth.dao.AuditLogDao;
import com.edgareldy.springjdbctutorial.core.auth.entity.AuditLog;
import com.edgareldy.springjdbctutorial.core.auth.service.ActorProvider;
import java.time.Clock;
import java.time.Instant;
import java.time.ZoneOffset;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;

/**
 * Unit tests of AuditLoggerImpl: the DAO and the actor provider are mocks and the clock is fixed, so the
 * persisted entry (actor, REJECTED_ prefix, timestamp) is asserted exactly.
 * <p>
 * Created edgar.muhamyangabo on 9/19/26
 * Author : edgar.muhamyangabo
 * Date : 9/19/26
 * Project : spring-jdbc-tutorial
 */
@ExtendWith(MockitoExtension.class)
class AuditLoggerImplTest {

    private static final Instant NOW = Instant.parse("2026-09-19T12:00:00Z");

    @Mock
    private AuditLogDao auditLogDao;
    @Mock
    private ActorProvider actorProvider;

    private AuditLoggerImpl logger;

    @BeforeEach
    void setUp() {
        // Clock.fixed always answers the same instant: the created_at of the entry is then deterministic
        logger = new AuditLoggerImpl(auditLogDao, actorProvider, Clock.fixed(NOW, ZoneOffset.UTC));
    }

    private AuditLog inserted() {
        ArgumentCaptor<AuditLog> captor = ArgumentCaptor.forClass(AuditLog.class);
        verify(auditLogDao).insert(captor.capture());
        return captor.getValue();
    }

    @Test
    void _01_ShouldInsertEntryWithActorActionAndClockTime_WhenLoggingASuccess() {
        when(actorProvider.currentUserId()).thenReturn(42L);

        logger.log("CREATE_ROLE", "ROLE", 9L, "roleName=EDITOR");

        AuditLog entry = inserted();
        assertThat(entry.getActorUserId()).isEqualTo(42L);
        assertThat(entry.getAction()).isEqualTo("CREATE_ROLE");
        assertThat(entry.getEntityType()).isEqualTo("ROLE");
        assertThat(entry.getEntityId()).isEqualTo(9L);
        assertThat(entry.getDetails()).isEqualTo("roleName=EDITOR");
        assertThat(entry.getCreatedAt()).isEqualTo(NOW);
    }

    @Test
    void _02_ShouldInsertNullActor_WhenThereIsNoAuthenticatedActor() {
        when(actorProvider.currentUserId()).thenReturn(null);

        logger.log("BOOTSTRAP_ADMIN", "USER", 1L, "email=admin@example.com");

        assertThat(inserted().getActorUserId()).isNull();
    }

    @Test
    void _03_ShouldKeepNullEntityId_WhenTheOperationTargetsNoExistingRow() {
        when(actorProvider.currentUserId()).thenReturn(42L);

        logger.logRejected("CREATE_ROLE", "ROLE", null, "roleName=EDITOR, reason=Role name already exists");

        assertThat(inserted().getEntityId()).isNull();
    }

    @Test
    void _04_ShouldPrefixTheActionWithRejected_WhenLoggingARefusal() {
        when(actorProvider.currentUserId()).thenReturn(42L);

        logger.logRejected("DELETE_ROLE", "ROLE", 3L, "roleName=EDITOR, reason=Role is still assigned to users");

        AuditLog entry = inserted();
        assertThat(entry.getAction()).isEqualTo("REJECTED_DELETE_ROLE");
        assertThat(entry.getActorUserId()).isEqualTo(42L);
        assertThat(entry.getCreatedAt()).isEqualTo(NOW);
        assertThat(entry.getDetails()).contains("reason=");
    }

    @Test
    void _05_ShouldNeverPrefixTheAction_WhenLoggingASuccess() {
        when(actorProvider.currentUserId()).thenReturn(42L);

        logger.log("DELETE_ROLE", "ROLE", 3L, "d");

        assertThat(inserted().getAction()).isEqualTo("DELETE_ROLE");
    }

    // Reflection on the annotations proves the transaction attributes the Spring proxy will apply. The real
    // effect (the refusal row surviving the rollback) is proved end to end by the ws integration tests.
    @Test
    void _06_ShouldJoinTheCallersTransactionForSuccessAndOpenANewOneForRefusals_WhenReadingTheAnnotations()
            throws NoSuchMethodException {
        Transactional success = AuditLoggerImpl.class
                .getMethod("log", String.class, String.class, Long.class, String.class)
                .getAnnotation(Transactional.class);
        Transactional refusal = AuditLoggerImpl.class
                .getMethod("logRejected", String.class, String.class, Long.class, String.class)
                .getAnnotation(Transactional.class);

        assertThat(success.propagation()).isEqualTo(Propagation.REQUIRED);
        assertThat(refusal.propagation()).isEqualTo(Propagation.REQUIRES_NEW);
    }
}
