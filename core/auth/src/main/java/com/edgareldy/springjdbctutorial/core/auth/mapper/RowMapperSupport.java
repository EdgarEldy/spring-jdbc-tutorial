package com.edgareldy.springjdbctutorial.core.auth.mapper;

import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Timestamp;
import java.time.Instant;

/**
 * Shared null-safe column readers for the row mappers of this module.
 * <p>
 * Created edgar.muhamyangabo on 9/19/26
 * Author : edgar.muhamyangabo
 * Date : 9/19/26
 * Project : spring-jdbc-tutorial
 */
public final class RowMapperSupport {

    private RowMapperSupport() {
    }

    /**
     * Reads a timestamptz column as an Instant, or null when the column is SQL NULL.
     */
    public static Instant instant(ResultSet rs, String column) throws SQLException {
        Timestamp value = rs.getTimestamp(column);
        return value == null ? null : value.toInstant();
    }
}
