package com.edgareldy.springjdbctutorial.core.auth.service.impl;

import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.edgareldy.springjdbctutorial.core.auth.service.AuthService;
import org.junit.jupiter.api.Test;

/**
 * Tests ExpiredTokenCleanupJob: run() is public, so it is called directly instead of waiting for the cron.
 * <p>
 * Created edgar.muhamyangabo on 9/19/26
 * Author : edgar.muhamyangabo
 * Date : 9/19/26
 * Project : spring-jdbc-tutorial
 */
class ExpiredTokenCleanupJobTest {

    @Test
    void _01_ShouldPurgeExpiredTokens_WhenRunIsCalled() {
        AuthService authService = mock(AuthService.class);
        when(authService.purgeExpiredTokens()).thenReturn(2);

        new ExpiredTokenCleanupJob(authService).run();

        verify(authService).purgeExpiredTokens();
    }
}
