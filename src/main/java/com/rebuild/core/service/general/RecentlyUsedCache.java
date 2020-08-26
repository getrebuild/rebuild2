/*
Copyright (c) REBUILD <https://getrebuild.com/> and/or its owners. All rights reserved.

rebuild is dual-licensed under commercial and open source licenses (GPLv3).
See LICENSE and COMMERCIAL in the project root for license information.
*/

package com.rebuild.core.service.general;

import cn.devezhao.persist4j.engine.ID;
import com.rebuild.core.RebuildApplication;
import com.rebuild.core.cache.BaseCacheTemplate;
import com.rebuild.core.helper.ConfigurableItem;
import com.rebuild.core.helper.RebuildConfiguration;
import com.rebuild.core.helper.general.FieldValueWrapper;
import com.rebuild.core.metadata.EntityHelper;
import com.rebuild.core.metadata.MetadataHelper;
import com.rebuild.core.service.NoRecordFoundException;
import org.apache.commons.lang.StringUtils;
import org.springframework.cache.CacheManager;
import org.springframework.stereotype.Service;
import redis.clients.jedis.JedisPool;

import java.io.Serializable;
import java.util.*;

/**
 * 最近使用的数据（引用型字段搜索）
 *
 * @author devezhao zhaofang123@gmail.com
 * @since 2019/04/25
 */
@Service
public class RecentlyUsedCache extends BaseCacheTemplate<Serializable> {

    // 最大缓存数量
    private static final int MAXNUM_PRE_ENTITY = 50;

    protected RecentlyUsedCache(JedisPool jedisPool, CacheManager cacheManager) {
        super(jedisPool, cacheManager, "RS.");
    }

    /**
     * 获取最近使用（最多10个）
     *
     * @param user
     * @param entity
     * @param type
     * @return
     */
    public ID[] gets(ID user, String entity, String type) {
        return gets(user, entity, type, 10);
    }

    /**
     * 获取最近使用
     *
     * @param user
     * @param entity
     * @param type
     * @param limit
     * @return
     */
    public ID[] gets(ID user, String entity, String type, int limit) {
        if (!isEnabled()) {
            return ID.EMPTY_ID_ARRAY;
        }

        final String key = formatKey(user, entity, type);
        @SuppressWarnings("unchecked")
        LinkedList<ID> exists = (LinkedList<ID>) getx(key);
        if (exists == null) {
            return ID.EMPTY_ID_ARRAY;
        }

        Set<ID> missed = new HashSet<>();
        List<ID> data = new ArrayList<>();
        for (int i = 0; i < limit && i < exists.size(); i++) {
            final ID raw = exists.get(i);
            if (!(raw.getEntityCode() == EntityHelper.ClassificationData || RebuildApplication.getPrivilegesManager().allowRead(user, raw))) {
                continue;
            }

            try {
                String label = FieldValueWrapper.getLabel(raw);
                ID clone = ID.valueOf(raw.toLiteral());
                clone.setLabel(label);
                data.add(clone);
            } catch (NoRecordFoundException ex) {
                missed.add(raw);
            }
        }

        if (!missed.isEmpty()) {
            exists.removeAll(missed);
            putx(key, exists);
        }

        return data.toArray(new ID[0]);
    }

    /**
     * 添加搜索缓存
     *
     * @param user
     * @param id
     * @param type
     */
    public void addOne(ID user, ID id, String type) {
        if (!isEnabled()) {
            return;
        }

        final String key = formatKey(user, MetadataHelper.getEntityName(id), type);
        @SuppressWarnings("unchecked")
        LinkedList<ID> exists = (LinkedList<ID>) getx(key);
        if (exists == null) {
            exists = new LinkedList<>();
        } else {
            exists.remove(id);
        }

        if (exists.size() > MAXNUM_PRE_ENTITY) {
            exists.removeLast();
        }
        exists.addFirst(id);
        putx(key, exists);
    }

    /**
     * 清理缓存
     *
     * @param user
     * @param entity
     * @param type
     */
    public void clean(ID user, String entity, String type) {
        evict(formatKey(user, entity, type));
    }

    private String formatKey(ID user, String entity, String type) {
        return String.format("%s-%s-%s", user, entity, StringUtils.defaultIfBlank(type, StringUtils.EMPTY));
    }

    private boolean isEnabled() {
        return RebuildConfiguration.getBool(ConfigurableItem.EnableRecentlyUsed);
    }
}
