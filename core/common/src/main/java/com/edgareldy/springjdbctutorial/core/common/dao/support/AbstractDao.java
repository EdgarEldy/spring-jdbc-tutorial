package com.edgareldy.springjdbctutorial.core.common.dao.support;

import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.jdbc.core.RowMapper;

import java.util.List;
import java.util.Objects;
import java.util.Optional;

/**
 * Base class of every DAO implementation: holds the shared {@link JdbcTemplate} and a few query helpers.
 * SQL itself always stays in the concrete DAO classes under {@code dao/impl}.
 * <p>
 * Created edgar.muhamyangabo on 9/19/26
 * Author : edgar.muhamyangabo
 * Date : 9/19/26
 * Project : spring-jdbc-tutorial
 */
public abstract class AbstractDao {

    // JdbcTemplate is Spring JDBC's central class: it opens/releases the connection, runs the SQL,
    // binds the "?" parameters and translates SQLException into Spring's unchecked DataAccessException
    // hierarchy. A RowMapper<T> is the callback that turns ONE ResultSet row into one object. Together
    // they replace an ORM: we write the SQL and the mapping ourselves. It is thread-safe, so a single
    // instance (declared once in DataSourceConfig) is shared by all DAOs.
    private final JdbcTemplate jdbcTemplate;

    protected AbstractDao(JdbcTemplate jdbcTemplate) {
        this.jdbcTemplate = Objects.requireNonNull(jdbcTemplate, "jdbcTemplate");
    }

    protected JdbcTemplate getJdbcTemplate() {
        return jdbcTemplate;
    }

    /**
     * Runs a query and returns its first row, or empty when there is none.
     * Never throws {@code EmptyResultDataAccessException}, unlike {@code queryForObject}.
     */
    protected <T> Optional<T> findOne(String sql, RowMapper<T> mapper, Object... args) {
        List<T> rows = jdbcTemplate.query(sql, mapper, args);
        return rows.isEmpty() ? Optional.empty() : Optional.of(rows.get(0));
    }

    /**
     * Runs an INSERT whose SQL ends with {@code RETURNING id} (PostgreSQL) and returns the generated id.
     */
    protected long insertReturningId(String sql, Object... args) {
        Long id = jdbcTemplate.queryForObject(sql, Long.class, args);
        if (id == null) {
            throw new IllegalStateException("INSERT ... RETURNING id returned no id");
        }
        return id;
    }

    /**
     * Runs a {@code SELECT COUNT(*)} style query and returns the number (0 when the result is null).
     */
    protected long count(String sql, Object... args) {
        Long total = jdbcTemplate.queryForObject(sql, Long.class, args);
        return total == null ? 0L : total;
    }
}
