package com.edgareldy.springjdbctutorial.core.auth.dao;

import com.edgareldy.springjdbctutorial.core.auth.entity.ActivationToken;

import java.time.Instant;
import java.util.Optional;

/**
 * Data access for activation tokens (stored as SHA-256 hash). Entity types only.
 * <p>
 * Created edgar.muhamyangabo on 9/19/26
 * Author : edgar.muhamyangabo
 * Date : 9/19/26
 * Project : spring-jdbc-tutorial
 */
public interface ActivationTokenDao {

    ActivationToken insert(ActivationToken token);

    Optional<ActivationToken> findByTokenHash(String tokenHash);

    void markValidated(Long id, Instant validatedAt);

    void deleteByUserId(Long userId);
}
