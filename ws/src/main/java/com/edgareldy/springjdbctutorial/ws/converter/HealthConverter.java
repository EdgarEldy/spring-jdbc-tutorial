package com.edgareldy.springjdbctutorial.ws.converter;

import com.edgareldy.springjdbctutorial.core.common.dto.HealthDto;
import com.edgareldy.springjdbctutorial.ws.payload.common.HealthResponse;

/**
 * Converts the core HealthDto to the HTTP HealthResponse payload.
 * <p>
 * Created edgar.muhamyangabo on 9/19/26
 * Author : edgar.muhamyangabo
 * Date : 9/19/26
 * Project : spring-jdbc-tutorial
 */
public final class HealthConverter {

    private HealthConverter() {
    }

    public static HealthResponse toResponse(HealthDto dto) {
        return new HealthResponse(dto.getStatus(), dto.getDatabase());
    }
}
