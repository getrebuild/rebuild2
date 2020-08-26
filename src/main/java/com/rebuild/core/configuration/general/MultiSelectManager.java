/*
Copyright (c) REBUILD <https://getrebuild.com/> and/or its owners. All rights reserved.

rebuild is dual-licensed under commercial and open source licenses (GPLv3).
See LICENSE and COMMERCIAL in the project root for license information.
*/

package com.rebuild.core.configuration.general;

import cn.devezhao.persist4j.Field;
import cn.devezhao.persist4j.engine.ID;
import com.alibaba.fastjson.JSONArray;
import com.rebuild.core.RebuildApplication;
import com.rebuild.core.configuration.ConfigBean;
import com.rebuild.utils.JSONUtils;

import java.util.ArrayList;
import java.util.List;

/**
 * 多选字段
 *
 * @author ZHAO
 * @since 2019/9/27
 */
public class MultiSelectManager extends PickListManager {

    public static final MultiSelectManager instance = new MultiSelectManager();

    protected MultiSelectManager() {
    }

    /**
     * @param field
     * @return
     */
    public JSONArray getSelectList(Field field) {
        ConfigBean[] entries = getPickListRaw(field, false);
        for (ConfigBean e : entries) {
            e.set("hide", null).set("id", null);
        }
        return JSONUtils.toJSONArray(entries);
    }

    /**
     * @param maskValue
     * @param field
     * @return
     */
    public String[] getLabel(long maskValue, Field field) {
        if (maskValue <= 0) {
            return new String[0];
        }

        List<String> labels = new ArrayList<>();
        ConfigBean[] entries = getPickListRaw(field, false);
        for (ConfigBean e : entries) {
            long m = e.get("mask", Long.class);
            if ((maskValue & m) != 0) {
                labels.add(e.getString("text"));
            }
        }
        return labels.toArray(new String[0]);
    }

    /**
     * @param label
     * @param field
     * @return
     */
    public Long findByLabel(String label, Field field) {
        Object[] o = RebuildApplication.createQueryNoFilter(
                "select maskValue from PickList where belongEntity = ? and belongField = ? and text = ?")
                .setParameter(1, field.getOwnEntity().getName())
                .setParameter(2, field.getName())
                .setParameter(3, label)
                .unique();
        return o == null ? null : (Long) o[0];
    }

    /**
     * 获取默认值
     *
     * @param field
     * @return
     */
    public Long getDefaultValue(Field field) {
        long maskValue = 0;
        for (ConfigBean e : getPickListRaw(field, false)) {
            if (e.getBoolean("default")) {
                maskValue += e.get("mask", Long.class);
            }
        }
        return maskValue == 0 ? null : maskValue;
    }

    @Override
    public void clean(Object idOrField) {
        if (idOrField instanceof ID) {
            // Nothings
        } else {
            super.clean(idOrField);
        }
    }
}
