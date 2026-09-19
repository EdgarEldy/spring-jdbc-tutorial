package com.edgareldy.springjdbctutorial.core.common.service.impl;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.when;

import com.edgareldy.springjdbctutorial.core.common.dao.HealthDao;
import com.edgareldy.springjdbctutorial.core.common.dto.HealthDto;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.dao.DataAccessResourceFailureException;

/**
 * Unit tests of HealthServiceImpl with the DAO mocked (no database).
 * <p>
 * Created edgar.muhamyangabo on 9/19/26
 * Author : edgar.muhamyangabo
 * Date : 9/19/26
 * Project : spring-jdbc-tutorial
 */
// MockitoExtension creates the @Mock objects and injects them into the @InjectMocks service, so the
// service is tested alone: the DAO answers are scripted with when(...).thenReturn/thenThrow.
@ExtendWith(MockitoExtension.class)
class HealthServiceImplTest {

    @Mock
    private HealthDao healthDao;

    @InjectMocks
    private HealthServiceImpl service;

    @Test
    void _01_ShouldReturnUp_WhenDatabaseIsReachable() {
        when(healthDao.isDatabaseReachable()).thenReturn(true);

        assertThat(service.check()).isEqualTo(new HealthDto("UP", "UP"));
    }

    @Test
    void _02_ShouldReturnDown_WhenDaoThrowsDataAccessException() {
        when(healthDao.isDatabaseReachable()).thenThrow(new DataAccessResourceFailureException("db down"));

        assertThat(service.check()).isEqualTo(new HealthDto("DOWN", "DOWN"));
    }

    @Test
    void _03_ShouldReturnDown_WhenDatabaseIsNotReachable() {
        when(healthDao.isDatabaseReachable()).thenReturn(false);

        assertThat(service.check()).isEqualTo(new HealthDto("DOWN", "DOWN"));
    }
}
