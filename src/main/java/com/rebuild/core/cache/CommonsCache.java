/*
rebuild - Building your business-systems freely.
Copyright (C) 2018 devezhao <zhaofang123@gmail.com>

This program is free software: you can redistribute it and/or modify
it under the terms of the GNU General Public License as published by
the Free Software Foundation, either version 3 of the License, or
(at your option) any later version.

This program is distributed in the hope that it will be useful,
but WITHOUT ANY WARRANTY; without even the implied warranty of
MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE. See the
GNU General Public License for more details.

You should have received a copy of the GNU General Public License
along with this program. If not, see <https://www.gnu.org/licenses/>.
*/

package com.rebuild.core.cache;

import com.rebuild.core.RebuildApplication;
import org.springframework.cache.Cache;
import org.springframework.cache.CacheManager;
import org.springframework.stereotype.Service;
import redis.clients.jedis.JedisPool;

import java.io.Serializable;

/**
 * Cache for all
 *
 * @author devezhao
 * @since 12/24/2018
 */
@Service
public class CommonsCache extends BaseCacheTemplate<Serializable> {

    protected CommonsCache(JedisPool jedisPool, CacheManager cacheManager) {
        super(jedisPool, cacheManager, null);
    }

    /**
     * @return
     * @see #isUseRedis()
     */
    public JedisPool getJedisPool() {
        return ((RedisDriver<?>) RebuildApplication.getCommonsCache().getCacheTemplate()).getJedisPool();
    }

    /**
     * @return
     */
    public Cache getEhcacheCache() {
        return ((EhcacheDriver<?>) RebuildApplication.getCommonsCache().getCacheTemplate()).cache();
    }
}
