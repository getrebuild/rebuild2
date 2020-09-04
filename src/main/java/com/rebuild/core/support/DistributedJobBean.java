/*
Copyright (c) REBUILD <https://getrebuild.com/> and/or its owners. All rights reserved.

rebuild is dual-licensed under commercial and open source licenses (GPLv3).
See LICENSE and COMMERCIAL in the project root for license information.
*/

package com.rebuild.core.support;

import com.rebuild.core.Application;
import com.rebuild.core.support.setup.Installer;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import redis.clients.jedis.Jedis;
import redis.clients.jedis.JedisPool;

/**
 * 分布式环境下（多 RB 实例），避免一个 Job 多个实例都运行。
 * 利用 redis 加锁，因此仅启用 redis 的情况下有效。
 *
 * @author ZHAO
 * @since 2020/4/5
 */
public abstract class DistributedJobBean {

    protected final Logger LOG = LoggerFactory.getLogger(getClass());

    private static final String SET_IF_NOT_EXIST = "NX";
    private static final String SET_WITH_EXPIRE_TIME = "EX";

    private static final String LOCK_KEY = "#RBJOBLOCK";
    private static final int LOCK_TIME = 2;  // 2s offset

    /**
     * 是否已在运行中，即并发判断（分布式环境）
     *
     * @return
     */
    protected boolean isRunning() {
        if (Installer.isUseRedis()) {
            JedisPool pool = Application.getCommonsCache().getJedisPool();
            String jobKey = getClass().getName() + LOCK_KEY;

            try (Jedis jedis = pool.getResource()) {
                String tryLock = jedis.set(jobKey, LOCK_KEY, SET_IF_NOT_EXIST, SET_WITH_EXPIRE_TIME, LOCK_TIME);
                if (tryLock == null) {
                    LOG.info("The job has been executed by another instance : " + getClass().getName());
                    return true;
                }
            }
        }

        if (LOG.isDebugEnabled()) {
            LOG.error("Job " + getClass().getName() + " can be safe execution");
        }
        return false;
    }
}
