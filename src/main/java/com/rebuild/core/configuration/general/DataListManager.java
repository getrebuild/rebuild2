/*
Copyright (c) REBUILD <https://getrebuild.com/> and/or its owners. All rights reserved.

rebuild is dual-licensed under commercial and open source licenses (GPLv3).
See LICENSE and COMMERCIAL in the project root for license information.
*/

package com.rebuild.core.configuration.general;

import cn.devezhao.persist4j.Entity;
import cn.devezhao.persist4j.Field;
import cn.devezhao.persist4j.engine.ID;
import com.alibaba.fastjson.JSON;
import com.alibaba.fastjson.JSONArray;
import com.alibaba.fastjson.JSONObject;
import com.rebuild.core.Application;
import com.rebuild.core.configuration.ConfigBean;
import com.rebuild.core.metadata.EntityHelper;
import com.rebuild.core.metadata.MetadataHelper;
import com.rebuild.core.metadata.impl.EasyMeta;
import com.rebuild.utils.JSONUtils;
import org.apache.commons.lang.StringUtils;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;

/**
 * 数据列表
 *
 * @author zhaofang123@gmail.com
 * @since 08/30/2018
 */
public class DataListManager extends BaseLayoutManager {

    public static final DataListManager instance = new DataListManager();

    private DataListManager() {
    }

    /**
     * @param entity
     * @param user
     * @return
     */
    public JSON getFieldsLayout(String entity, ID user) {
        return getFieldsLayout(entity, user, true);
    }

    /**
     * @param entity
     * @param user
     * @param filter 过滤无读取权限的字段
     * @return
     */
    public JSON getFieldsLayout(String entity, ID user, boolean filter) {
        return formatFieldsLayout(entity, user, filter, getLayoutOfDatalist(user, entity));
    }

    /**
     * @param entity
     * @param user
     * @param filter 过滤无读取权限的字段
     * @param config
     * @return
     */
    public JSON formatFieldsLayout(String entity, ID user, boolean filter, ConfigBean config) {
        List<Map<String, Object>> columnList = new ArrayList<>();
        Entity entityMeta = MetadataHelper.getEntity(entity);
        Field namedField = MetadataHelper.getNameField(entityMeta);

        // 默认配置
        if (config == null) {
            columnList.add(formatField(namedField));

            String namedFieldName = namedField.getName();
            if (!StringUtils.equalsIgnoreCase(namedFieldName, EntityHelper.CreatedBy)
                    && entityMeta.containsField(EntityHelper.CreatedBy)) {
                columnList.add(formatField(entityMeta.getField(EntityHelper.CreatedBy)));
            }
            if (!StringUtils.equalsIgnoreCase(namedFieldName, EntityHelper.CreatedOn)
                    && entityMeta.containsField(EntityHelper.CreatedOn)) {
                columnList.add(formatField(entityMeta.getField(EntityHelper.CreatedOn)));
            }
        } else {
            for (Object o : (JSONArray) config.getJSON("config")) {
                JSONObject item = (JSONObject) o;
                String field = item.getString("field");
                Field lastField = MetadataHelper.getLastJoinField(entityMeta, field);
                if (lastField == null) {
                    LOG.warn("Unknow field '" + field + "' in '" + entity + "'");
                    continue;
                }

                String[] fieldPath = field.split("\\.");
                Map<String, Object> formatted = null;
                if (fieldPath.length == 1) {
                    formatted = formatField(lastField);
                } else {

                    // 如果没有引用实体的读权限，则直接过滤掉字段

                    Field parentField = entityMeta.getField(fieldPath[0]);
                    if (!filter) {
                        formatted = formatField(lastField, parentField);
                    } else if (Application.getPrivilegesManager().allowRead(user, lastField.getOwnEntity().getEntityCode())) {
                        formatted = formatField(lastField, parentField);
                    }
                }

                if (formatted != null) {
                    Integer width = item.getInteger("width");
                    if (width != null) {
                        formatted.put("width", width);
                    }
                    columnList.add(formatted);
                }
            }
        }

        return JSONUtils.toJSONObject(
                new String[] { "entity", "nameField", "fields" },
                new Object[] { entity, namedField.getName(), columnList });
    }

    /**
     * @param field
     * @return
     */
    public Map<String, Object> formatField(Field field) {
        return formatField(field, null);
    }

    /**
     * @param field
     * @param parent
     * @return
     */
    public Map<String, Object> formatField(Field field, Field parent) {
        String parentField = parent == null ? "" : (parent.getName() + ".");
        String parentLabel = parent == null ? "" : (EasyMeta.getLabel(parent) + ".");
        EasyMeta easyField = new EasyMeta(field);
        return JSONUtils.toJSONObject(
                new String[] { "field", "label", "type" },
                new Object[] { parentField + easyField.getName(), parentLabel + easyField.getLabel(), easyField.getDisplayType(false)});
    }

    /**
     * 获取可用列显示ID
     *
     * @param entity
     * @param user
     * @return
     */
    public ID[] getUsesDataListId(String entity, ID user) {
        Object[][] uses = getUsesConfig(user, entity, TYPE_DATALIST);
        List<ID> array = new ArrayList<>();
        for (Object[] c : uses) {
            array.add((ID) c[0]);
        }
        return array.toArray(new ID[0]);
    }
}
