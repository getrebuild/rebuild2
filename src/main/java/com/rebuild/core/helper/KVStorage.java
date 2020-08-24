/*
Copyright (c) REBUILD <https://getrebuild.com/> and its owners. All rights reserved.

rebuild is dual-licensed under commercial and open source licenses (GPLv3).
See LICENSE and COMMERCIAL in the project root for license information.
*/

package com.rebuild.core.helper;

import cn.devezhao.persist4j.Record;
import cn.devezhao.persist4j.engine.ID;
import com.rebuild.core.RebuildApplication;
import com.rebuild.core.RebuildEnvironmentPostProcessor;
import com.rebuild.core.metadata.EntityHelper;
import com.rebuild.core.privileges.UserService;
import org.apache.commons.lang.StringUtils;
import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;

/**
 * K/V 对存储
 *
 * @author devezhao
 * @since 2019/11/22
 */
public class KVStorage {

    protected static final Log LOG = LogFactory.getLog(KVStorage.class);

    /**
     * 存储
     *
     * @param key 会自动加 `custom.` 前缀
     * @return
     */
    public static String getCustomValue(String key) {
        return getValue("custom." + key, false, null);
    }

    /**
     * 获取
     *
     * @param key   会自动加 `custom.` 前缀
     * @param value
     */
    public static void setCustomValue(String key, Object value) {
        setValue("custom." + key, value);
    }

    /**
     * @param key
     * @param value
     */
    protected static void setValue(final String key, Object value) {
        Object[] exists = RebuildApplication.createQueryNoFilter(
                "select configId from SystemConfig where item = ?")
                .setParameter(1, key)
                .unique();

        Record record;
        if (exists == null) {
            record = EntityHelper.forNew(EntityHelper.SystemConfig, UserService.SYSTEM_USER);
            record.setString("item", key);
        } else {
            record = EntityHelper.forUpdate((ID) exists[0], UserService.SYSTEM_USER);
        }
        record.setString("value", String.valueOf(value));

        RebuildApplication.getCommonsService().createOrUpdate(record);
        RebuildApplication.getCommonsCache().evict(key);
    }

    /**
     * @param key
     * @param reload
     * @param defaultValue
     * @return
     */
    protected static String getValue(final String key, boolean reload, Object defaultValue) {

        String value = null;

        if (RebuildApplication.serversReady()) {
            value = RebuildApplication.getCommonsCache().get(key);
            if (value != null && !reload) {
                return value;
            }

            // 1. 首先从数据库
            Object[] fromDb = RebuildApplication.createQueryNoFilter(
                    "select value from SystemConfig where item = ?")
                    .setParameter(1, key)
                    .unique();
            value = fromDb == null ? null : StringUtils.defaultIfBlank((String) fromDb[0], null);
        }

        // 2. 从配置文件/命令行加载
        if (value == null) {
            value = RebuildEnvironmentPostProcessor.getProperty(key);
        }

        // 3. 默认值
        if (value == null && defaultValue != null) {
            value = defaultValue.toString();
        }

        if (RebuildApplication.serversReady()) {
            if (value == null) {
                RebuildApplication.getCommonsCache().evict(key);
            } else {
                RebuildApplication.getCommonsCache().put(key, value);
            }
        }

        return value;
    }
}
