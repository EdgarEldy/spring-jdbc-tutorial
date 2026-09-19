package com.edgareldy.springjdbctutorial.core.auth.dao;

import com.edgareldy.springjdbctutorial.core.auth.entity.BlacklistedToken;

import java.time.Instant;

/**
 * Data access for revoked JWTs (stored as SHA-256 hash). Entity types only.
 * <p>
 * Created edgar.muhamyangabo on 9/19/26
 * Author : edgar.muhamyangabo
 * Date : 9/19/26
 * Project : spring-jdbc-tutorial
 */
public interface BlacklistedTokenDao {

    BlacklistedToken insert(BlacklistedToken token);

    boolean existsByTokenHash(String tokenHash);

    /** Deletes the rows whose expires_at is strictly before the given instant and returns how many. */
    int deleteExpiredBefore(Instant instant);
}
