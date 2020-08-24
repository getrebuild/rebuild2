/*
Copyright (c) REBUILD <https://getrebuild.com/> and its owners. All rights reserved.

rebuild is dual-licensed under commercial and open source licenses (GPLv3).
See LICENSE and COMMERCIAL in the project root for license information.
*/

package com.rebuild.web.general;

import cn.devezhao.persist4j.Entity;
import cn.devezhao.persist4j.Field;
import cn.devezhao.persist4j.dialect.FieldType;
import cn.devezhao.persist4j.engine.ID;
import com.alibaba.fastjson.JSON;
import com.rebuild.core.RebuildApplication;
import com.rebuild.core.configuration.general.ClassificationManager;
import com.rebuild.core.configuration.general.DataListManager;
import com.rebuild.core.helper.general.FieldValueWrapper;
import com.rebuild.core.helper.general.ProtocolFilterParser;
import com.rebuild.core.metadata.EntityHelper;
import com.rebuild.core.metadata.MetadataHelper;
import com.rebuild.core.metadata.MetadataSorter;
import com.rebuild.core.metadata.impl.DisplayType;
import com.rebuild.core.metadata.impl.EasyMeta;
import com.rebuild.core.privileges.UserHelper;
import com.rebuild.core.privileges.UserService;
import com.rebuild.core.service.general.RecentlyUsedCache;
import com.rebuild.utils.JSONUtils;
import com.rebuild.web.EntityController;
import org.apache.commons.lang.ArrayUtils;
import org.apache.commons.lang.StringEscapeUtils;
import org.apache.commons.lang.StringUtils;
import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.servlet.ModelAndView;

import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import java.io.IOException;
import java.text.MessageFormat;
import java.util.*;

/**
 * 引用字段搜索
 *
 * @author zhaofang123@gmail.com
 * @see RecentlyUsedSearch
 * @since 08/24/2018
 */
@Controller
@RequestMapping("/commons/search/")
public class ReferenceSearch extends EntityController {

    // 快速搜索引用字段
    @RequestMapping({"reference", "quick"})
    public void referenceSearch(HttpServletRequest request, HttpServletResponse response) {
        final ID user = getRequestUser(request);
        final String entity = getParameterNotNull(request, "entity");
        final String field = getParameterNotNull(request, "field");

        Entity metaEntity = MetadataHelper.getEntity(entity);
        Field referenceField = metaEntity.getField(field);
        if (referenceField.getType() != FieldType.REFERENCE) {
            writeSuccess(response, JSONUtils.EMPTY_ARRAY);
            return;
        }

        Entity referenceEntity = referenceField.getReferenceEntity();
        Field referenceNameField = MetadataHelper.getNameField(referenceEntity);
        if (referenceNameField == null) {
            LOG.warn("No name-field found : " + referenceEntity.getName());
            writeSuccess(response, JSONUtils.EMPTY_ARRAY);
            return;
        }

        // 引用字段数据过滤仅在搜索时有效
        // 启用数据过滤后最近搜索将不可用
        final String protocolFilter = new ProtocolFilterParser(null).parseRef(field + "." + entity);

        String q = getParameter(request, "q");
        // 为空则加载最近使用的
        if (StringUtils.isBlank(q)) {
            ID[] recently = null;
            if (protocolFilter == null) {
                String type = getParameter(request, "type");
                recently = RebuildApplication.getBean(RecentlyUsedCache.class).gets(user, referenceEntity.getName(), type);
            }

            if (recently == null || recently.length == 0) {
                writeSuccess(response, JSONUtils.EMPTY_ARRAY);
            } else {
                writeSuccess(response, RecentlyUsedSearch.formatSelect2(recently, null));
            }
            return;
        }
        q = StringEscapeUtils.escapeSql(q);

        // 可搜索字符
        Set<String> searchFields = new HashSet<>();
        DisplayType referenceNameFieldType = EasyMeta.getDisplayType(referenceNameField);
        if (!(referenceNameFieldType == DisplayType.DATETIME || referenceNameFieldType == DisplayType.DATE
                || referenceNameFieldType == DisplayType.NUMBER || referenceNameFieldType == DisplayType.DECIMAL
                || referenceNameFieldType == DisplayType.ID)) {
            searchFields.add(referenceNameField.getName());
        }
        if (referenceEntity.containsField(EntityHelper.QuickCode) && StringUtils.isAlphanumericSpace(q)) {
            searchFields.add(EntityHelper.QuickCode);
        }
        for (Field seriesField : MetadataSorter.sortFields(referenceEntity, DisplayType.SERIES)) {
            searchFields.add(seriesField.getName());
        }

        if (searchFields.isEmpty()) {
            LOG.warn("No fields of search found : " + referenceEntity);
            writeSuccess(response, JSONUtils.EMPTY_ARRAY);
            return;
        }

        String like = " like '%" + q + "%'";
        String searchWhere = StringUtils.join(searchFields.iterator(), like + " or ") + like;
        if (protocolFilter != null) {
            searchWhere = "(" + searchWhere + ") and (" + protocolFilter + ')';
        }

        String sql = MessageFormat.format("select {0},{1} from {2} where ( {3} )",
                referenceEntity.getPrimaryField().getName(), referenceNameField.getName(), referenceEntity.getName(), searchWhere);
        if (referenceEntity.containsField(EntityHelper.ModifiedOn)) {
            sql += " order by modifiedOn desc";
        }

        List<Object> result = resultSearch(sql, metaEntity, referenceNameField);
        writeSuccess(response, result);
    }

    // 搜索指定实体的指定字段
    @RequestMapping("search")
    public void search(HttpServletRequest request, HttpServletResponse response) {
        final ID user = getRequestUser(request);
        final String entity = getParameterNotNull(request, "entity");

        String q = getParameter(request, "q");
        // 为空则加载最近使用的
        if (StringUtils.isBlank(q)) {
            String type = getParameter(request, "type");
            ID[] recently = RecentlyUsedSearch.cache().gets(user, entity, type);
            if (recently.length == 0) {
                writeSuccess(response, JSONUtils.EMPTY_ARRAY);
            } else {
                writeSuccess(response, RecentlyUsedSearch.formatSelect2(recently, null));
            }
            return;
        }
        q = StringEscapeUtils.escapeSql(q);

        Entity metaEntity = MetadataHelper.getEntity(entity);
        Field nameField = MetadataHelper.getNameField(metaEntity);
        if (nameField == null) {
            LOG.warn("No name-field found : " + entity);
            writeSuccess(response, ArrayUtils.EMPTY_STRING_ARRAY);
            return;
        }

        // 查询字段，未指定则使用名称字段和 quickCode
        String qfields = getParameter(request, "qfields");
        if (StringUtils.isBlank(qfields)) {
            qfields = nameField.getName();
            if (metaEntity.containsField(EntityHelper.QuickCode)) {
                qfields += "," + EntityHelper.QuickCode;
            }
        }

        List<String> or = new ArrayList<>();
        for (String field : qfields.split(",")) {
            if (!metaEntity.containsField(field)) {
                LOG.warn("No field for search : " + field);
            } else {
                or.add(field + " like '%" + q + "%'");
            }
        }
        if (or.isEmpty()) {
            writeSuccess(response, ArrayUtils.EMPTY_STRING_ARRAY);
            return;
        }

        String sql = "select {0},{1} from {2} where ({3})";
        sql = MessageFormat.format(sql,
                metaEntity.getPrimaryField().getName(), nameField.getName(), metaEntity.getName(), StringUtils.join(or, " or "));
        if (metaEntity.containsField(EntityHelper.ModifiedOn)) {
            sql += " order by modifiedOn desc";
        }

        List<Object> result = resultSearch(sql, metaEntity, nameField);
        writeSuccess(response, result);
    }

    // 获取记录的名称字段值
    @RequestMapping("read-labels")
    public void referenceLabel(HttpServletRequest request, HttpServletResponse response) {
        String ids = getParameter(request, "ids", null);
        if (ids == null) {
            writeSuccess(response);
            return;
        }

        Map<String, String> labels = new HashMap<>();
        for (String id : ids.split("\\|")) {
            if (!ID.isId(id)) {
                continue;
            }
            String label = FieldValueWrapper.getLabelNotry(ID.valueOf(id));
            labels.put(id, label);
        }
        writeSuccess(response, labels);
    }

    // 搜索分类字段
    @RequestMapping("classification")
    public void searchClassification(HttpServletRequest request, HttpServletResponse response) {
        final ID user = getRequestUser(request);
        final String entity = getParameterNotNull(request, "entity");
        final String field = getParameterNotNull(request, "field");

        Field fieldMeta = MetadataHelper.getField(entity, field);
        ID useClassification = ClassificationManager.instance.getUseClassification(fieldMeta, false);

        String q = getParameter(request, "q");
        // 为空则加载最近使用的
        if (StringUtils.isBlank(q)) {
            String type = "d" + useClassification;
            ID[] recently = RecentlyUsedSearch.cache().gets(user, "ClassificationData", type);
            if (recently.length == 0) {
                writeSuccess(response, JSONUtils.EMPTY_ARRAY);
            } else {
                writeSuccess(response, RecentlyUsedSearch.formatSelect2(recently, null));
            }
            return;
        }
        q = StringEscapeUtils.escapeSql(q);

        int openLevel = ClassificationManager.instance.getOpenLevel(fieldMeta);

        String sql = "select itemId,fullName from ClassificationData" +
                " where dataId = '%s' and level = %d and (fullName like '%%%s%%' or quickCode like '%%%s%%') order by fullName";
        sql = String.format(sql, useClassification.toLiteral(), openLevel, q, q);
        List<Object> result = resultSearch(sql, null, null);
        writeSuccess(response, result);
    }

    /**
     * 封装查询结果
     *
     * @param sql
     * @param entity    不指定则使用无权限查询
     * @param nameField
     * @return
     */
    private List<Object> resultSearch(String sql, Entity entity, Field nameField) {
        Object[][] array = (entity == null ? RebuildApplication.createQueryNoFilter(sql) : RebuildApplication.createQuery(sql))
                .setLimit(10)
                .array();

        List<Object> result = new ArrayList<>();
        for (Object[] o : array) {
            final ID recordId = (ID) o[0];
            final Object nameValue = o[1];
            if (entity != null
                    && MetadataHelper.isBizzEntity(entity.getEntityCode())
                    && (!UserHelper.isActive(recordId) || recordId.equals(UserService.SYSTEM_USER))) {
                continue;
            }

            String label;
            if (nameField == null) {
                if (nameValue == null || StringUtils.isBlank(nameValue.toString())) {
                    label = FieldValueWrapper.NO_LABEL_PREFIX + recordId.toLiteral().toUpperCase();
                } else {
                    label = nameValue.toString();
                }
            } else {
                label = (String) FieldValueWrapper.instance.wrapFieldValue(o[1], nameField, true);
            }

            if (StringUtils.isBlank(label)) {
                label = FieldValueWrapper.NO_LABEL_PREFIX + recordId.toLiteral().toUpperCase();
            }
            result.add(FieldValueWrapper.wrapMixValue(recordId, label));
        }
        return result;
    }

    /**
     * @see com.rebuild.web.general.GeneralDataListControll#pageList(String, HttpServletRequest, HttpServletResponse)
     */
    @RequestMapping("reference-search-list")
    public ModelAndView pageListSearch(HttpServletRequest request, HttpServletResponse response) throws IOException {
        String[] fieldAndEntity = getParameterNotNull(request, "field").split("\\.");
        if (!MetadataHelper.checkAndWarnField(fieldAndEntity[1], fieldAndEntity[0])) {
            response.sendError(404);
            return null;
        }

        Entity entity = MetadataHelper.getEntity(fieldAndEntity[1]);
        Field field = entity.getField(fieldAndEntity[0]);
        Entity searchEntity = field.getReferenceEntity();

        ModelAndView mv = createModelAndView("/general-entity/reference-search.jsp");
        putEntityMeta(mv, searchEntity);

        JSON config = DataListManager.instance.getFieldsLayout(searchEntity.getName(), getRequestUser(request));
        mv.getModel().put("DataListConfig", JSON.toJSONString(config));

        // 是否启用了字段过滤
        String referenceDataFilter = EasyMeta.valueOf(field).getExtraAttr("referenceDataFilter");
        if (referenceDataFilter != null && referenceDataFilter.length() > 10) {
            mv.getModel().put("referenceFilter", "ref:" + getParameter(request, "field"));
        } else {
            mv.getModel().put("referenceFilter", StringUtils.EMPTY);
        }
        return mv;
    }
}
