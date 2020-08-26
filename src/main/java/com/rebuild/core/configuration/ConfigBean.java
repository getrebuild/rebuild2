/*
Copyright (c) REBUILD <https://getrebuild.com/> and/or its owners. All rights reserved.

rebuild is dual-licensed under commercial and open source licenses (GPLv3).
See LICENSE and COMMERCIAL in the project root for license information.
*/

package com.rebuild.core.configuration;

import cn.devezhao.persist4j.engine.ID;
import com.alibaba.fastjson.JSON;
import com.alibaba.fastjson.JSONObject;
import com.rebuild.utils.JSONUtils;
import com.rebuild.utils.JSONable;
import org.springframework.util.Assert;

import java.io.Serializable;
import java.util.HashMap;
import java.util.Map;

/**
 * @author devezhao zhaofang123@gmail.com
 * @since 2019/05/20
 */
public class ConfigBean implements Serializable, Cloneable, JSONable {
    private static final long serialVersionUID = -2618040374508703332L;

    private Map<String, Object> entryMap;

    public ConfigBean() {
        this.entryMap = new HashMap<>();
    }

    /**
     * @param name
     * @param value Remove if null
     * @return
     */
    public ConfigBean set(String name, Object value) {
        Assert.notNull(name, "'name' must not be null");
        if (value == null) {
            entryMap.remove(name);
        } else {
            entryMap.put(name, value);
        }
        return this;
    }

    public ID getID(String name) {
        return (ID) entryMap.get(name);
    }

    public String getString(String name) {
        return (String) entryMap.get(name);
    }

    public Boolean getBoolean(String name) {
        return (Boolean) entryMap.get(name);
    }

    public Integer getInteger(String name) {
        return (Integer) entryMap.get(name);
    }

    public JSON getJSON(String name) {
        return (JSON) entryMap.get(name);
    }

    @SuppressWarnings({"unchecked", "unused"})
    public <T> T get(String name, Class<T> returnType) {
        return (T) entryMap.get(name);
    }

    @Override
    public ConfigBean clone() {
        try {
            super.clone();
        } catch (CloneNotSupportedException ignored) {
        }

        ConfigBean c = new ConfigBean();
        for (Map.Entry<String, Object> e : this.entryMap.entrySet()) {
            Object v = e.getValue();
            if (v instanceof JSON) {
                v = JSONUtils.clone((JSON) v);
            }
            c.set(e.getKey(), v);
        }
        return c;
    }

    @Override
    public JSON toJSON() {
        return (JSONObject) JSON.toJSON(this.entryMap);
    }

    @Override
    public JSON toJSON(String... specFields) {
        Map<String, Object> map = new HashMap<>();
        for (String s : specFields) {
            map.put(s, entryMap.get(s));
        }
        return (JSONObject) JSON.toJSON(map);
    }
}
