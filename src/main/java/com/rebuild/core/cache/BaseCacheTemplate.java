/*
Copyright (c) REBUILD <https://getrebuild.com/> and/or its owners. All rights reserved.

rebuild is dual-licensed under commercial and open source licenses (GPLv3).
See LICENSE and COMMERCIAL in the project root for license information.
*/

package com.rebuild.core.cache;

import cn.devezhao.commons.ThrowableUtils;
import org.apache.commons.io.IOUtils;
import org.apache.commons.lang.StringUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.cache.CacheManager;
import org.springframework.util.Assert;
import redis.clients.jedis.Jedis;
import redis.clients.jedis.JedisPool;

import java.io.Serializable;

/**
 * 缓存模板
 *
 * @author devezhao
 * @since 01/02/2019
 */
public abstract class BaseCacheTemplate<V extends Serializable> implements CacheTemplate<V> {

    protected final Logger LOG = LoggerFactory.getLogger(getClass());

    /**
     * 默认缓存时间（90天）
     */
    private static final int TS_DEFAULT = 60 * 60 * 24 * 90;

    final private CacheTemplate<V> delegate;

    final private String keyPrefix;

    /**
     * @param jedisPool
     * @param backup    The ehcache for backup
     * @param keyPrefix
     */
    protected BaseCacheTemplate(JedisPool jedisPool, CacheManager backup, String keyPrefix) {
        if (testJedisPool(jedisPool)) {
            this.delegate = new RedisDriver<>(jedisPool);
        } else {
            this.delegate = new EhcacheDriver<>(backup);
        }

        String fix = StringUtils.defaultIfBlank(System.getProperty("cache.keyprefix"), "RB.");
        this.keyPrefix = fix + StringUtils.defaultIfBlank(keyPrefix, StringUtils.EMPTY);
    }

    @Override
    public String get(String key) {
        return delegate.get(unityKey(key));
    }

    @Override
    public void put(String key, String value) {
        this.put(key, value, TS_DEFAULT);
    }

    @Override
    public void put(String key, String value, int seconds) {
        if (value == null) {
            LOG.warn("Can't set `" + key + "` to null");
            return;
        }
        delegate.put(unityKey(key), value, seconds);
    }

    @Override
    public V getx(String key) {
        return delegate.getx(unityKey(key));
    }

    @Override
    public void putx(String key, V value) {
        this.putx(key, value, TS_DEFAULT);
    }

    @Override
    public void putx(String key, V value, int seconds) {
        if (value == null) {
            LOG.warn("Can't set `" + key + "` to null");
            return;
        }
        delegate.putx(unityKey(key), value, seconds);
    }

    @Override
    public void evict(String key) {
        delegate.evict(unityKey(key));
    }

    /**
     * @return
     */
    public CacheTemplate<V> getCacheTemplate() {
        return delegate;
    }

    private boolean testJedisPool(JedisPool jedisPool) {
        try {
            Jedis jedis = jedisPool.getResource();
            IOUtils.closeQuietly(jedis);
            return true;
        } catch (Exception ex) {
            LOG.warn("Acquisition J/Redis failed : " + ThrowableUtils.getRootCause(ex).getLocalizedMessage()
                    + " !!! falling back to EhCache");
        }
        return false;
    }

    private String unityKey(String key) {
        Assert.isTrue(StringUtils.isNotBlank(key), "[key] not be null");
        return (keyPrefix + key).toLowerCase();
    }
}
