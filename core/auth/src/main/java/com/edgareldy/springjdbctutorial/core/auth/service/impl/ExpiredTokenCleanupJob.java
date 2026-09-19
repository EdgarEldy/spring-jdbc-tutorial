package com.edgareldy.springjdbctutorial.core.auth.service.impl;

import com.edgareldy.springjdbctutorial.core.auth.service.AuthService;
import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.springframework.scheduling.annotation.Scheduled;

/**
 * Daily job purging expired blacklisted tokens. run() is public so tests can call it directly.
 * <p>
 * Created edgar.muhamyangabo on 9/19/26
 * Author : edgar.muhamyangabo
 * Date : 9/19/26
 * Project : spring-jdbc-tutorial
 */
public class ExpiredTokenCleanupJob {

    private static final Log LOG = LogFactory.getLog(ExpiredTokenCleanupJob.class);

    private final AuthService authService;

    public ExpiredTokenCleanupJob(AuthService authService) {
        this.authService = authService;
    }

    // @Scheduled runs this method by itself at 03:00 every day (Spring cron: second minute hour day
    // month weekday). It only fires because @EnableScheduling is on ServiceConfig and this class is a
    // bean. Expired rows are useless (an expired JWT is refused anyway), so deleting them keeps the
    // blacklist small and its lookups fast.
    @Scheduled(cron = "0 0 3 * * *")
    public void run() {
        int removed = authService.purgeExpiredTokens();
        LOG.info("Expired blacklisted tokens purged: " + removed);
    }
}
