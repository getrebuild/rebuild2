/*
Copyright (c) REBUILD <https://getrebuild.com/> and its owners. All rights reserved.

rebuild is dual-licensed under commercial and open source licenses (GPLv3).
See LICENSE and COMMERCIAL in the project root for license information.
*/

package com.rebuild.web.configuration;

import cn.devezhao.commons.web.ServletUtils;
import cn.devezhao.persist4j.Entity;
import cn.devezhao.persist4j.Field;
import cn.devezhao.persist4j.Record;
import cn.devezhao.persist4j.engine.ID;
import com.alibaba.fastjson.JSON;
import com.alibaba.fastjson.JSONObject;
import com.rebuild.core.RebuildApplication;
import com.rebuild.core.configuration.ConfigBean;
import com.rebuild.core.configuration.general.BaseLayoutManager;
import com.rebuild.core.configuration.general.DataListManager;
import com.rebuild.core.configuration.general.LayoutConfigService;
import com.rebuild.core.configuration.general.ShareToManager;
import com.rebuild.core.metadata.EntityHelper;
import com.rebuild.core.metadata.MetadataHelper;
import com.rebuild.core.metadata.MetadataSorter;
import com.rebuild.core.metadata.impl.DisplayType;
import com.rebuild.core.metadata.impl.EasyMeta;
import com.rebuild.core.privileges.RoleService;
import com.rebuild.core.privileges.UserHelper;
import com.rebuild.core.privileges.bizz.ZeroEntry;
import com.rebuild.utils.JSONUtils;
import com.rebuild.web.BaseController;
import org.apache.commons.lang.StringUtils;
import org.springframework.stereotype.Controller;
import org.springframework.util.Assert;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestMethod;

import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import java.io.IOException;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * 列表配置
 *
 * @author devezhao
 * @since 01/07/2019
 */
@Controller
@RequestMapping("/app/{entity}/")
public class DataListSettingsControll extends BaseController implements ShareTo {

    @RequestMapping(value = "list-fields", method = RequestMethod.POST)
    public void sets(@PathVariable String entity,
                     HttpServletRequest request, HttpServletResponse response) throws IOException {
        final ID user = getRequestUser(request);
        Assert.isTrue(RebuildApplication.getPrivilegesManager().allow(user, ZeroEntry.AllowCustomDataList), "没有权限");

        ID cfgid = getIdParameter(request, "id");
        // 普通用户只能有一个
        if (cfgid != null && !ShareToManager.isSelf(user, cfgid)) {
            ID useList = DataListManager.instance.detectUseConfig(user, entity, DataListManager.TYPE_DATALIST);
            if (useList != null && ShareToManager.isSelf(user, useList)) {
                cfgid = useList;
            } else {
                cfgid = null;
            }
        }

        JSON config = ServletUtils.getRequestJson(request);

        Record record;
        if (cfgid == null) {
            record = EntityHelper.forNew(EntityHelper.LayoutConfig, user);
            record.setString("belongEntity", entity);
            record.setString("applyType", BaseLayoutManager.TYPE_DATALIST);
            record.setString("shareTo", BaseLayoutManager.SHARE_SELF);
        } else {
            record = EntityHelper.forUpdate(cfgid, user);
        }
        record.setString("config", config.toJSONString());
        putCommonsFields(request, record);
        RebuildApplication.getBean(LayoutConfigService.class).createOrUpdate(record);

        writeSuccess(response);
    }

    @RequestMapping(value = "list-fields", method = RequestMethod.GET)
    public void gets(@PathVariable String entity,
                     HttpServletRequest request, HttpServletResponse response) {
        final ID user = getRequestUser(request);
        final Entity entityMeta = MetadataHelper.getEntity(entity);

        List<Map<String, Object>> fieldList = new ArrayList<>();
        for (Field field : MetadataSorter.sortFields(entityMeta)) {
            if (canUseField(field)) {
                fieldList.add(DataListManager.instance.formatField(field));
            }
        }

        // 明细关联字段
        final Field stmField = entityMeta.getMasterEntity() == null ? null : MetadataHelper.getSlaveToMasterField(entityMeta);

        // 引用实体的字段
        for (Field field : MetadataSorter.sortFields(entityMeta, DisplayType.REFERENCE)) {
            // 过滤所属用户/所属部门等系统字段（除了明细引用（主实体）字段）
            if (EasyMeta.valueOf(field).isBuiltin() && (stmField == null || !stmField.equals(field))) {
                continue;
            }

            Entity refEntity = field.getReferenceEntity();
            // 无权限的不返回
            if (!RebuildApplication.getPrivilegesManager().allowRead(user, refEntity.getEntityCode())) {
                continue;
            }

            for (Field fieldOfRef : MetadataSorter.sortFields(refEntity)) {
                if (canUseField(fieldOfRef)) {
                    fieldList.add(DataListManager.instance.formatField(fieldOfRef, field));
                }
            }
        }

        ConfigBean raw;
        String cfgid = request.getParameter("id");
        if ("NEW".equalsIgnoreCase(cfgid)) {
            raw = new ConfigBean();
            raw.set("config", JSONUtils.EMPTY_ARRAY);
        } else if (ID.isId(cfgid)) {
            raw = DataListManager.instance.getLayoutById(ID.valueOf(cfgid));
        } else {
            raw = DataListManager.instance.getLayoutOfDatalist(user, entity);
        }

        JSONObject config = (JSONObject) DataListManager.instance.formatFieldsLayout(entity, user, false, raw);

        Map<String, Object> ret = new HashMap<>();
        ret.put("fieldList", fieldList);
        ret.put("configList", config.getJSONArray("fields"));
        if (raw != null) {
            ret.put("configId", raw.getID("id"));
            ret.put("shareTo", raw.getString("shareTo"));
        }
        writeSuccess(response, ret);
    }

    /**
     * @see NavSettings#getsList(HttpServletRequest, HttpServletResponse)
     */
    @RequestMapping(value = "list-fields/alist", method = RequestMethod.GET)
    public void getsList(@PathVariable String entity,
                         HttpServletRequest request, HttpServletResponse response) {
        final ID user = getRequestUser(request);

        String sql = "select configId,configName,shareTo,createdBy from LayoutConfig where ";
        if (UserHelper.isAdmin(user)) {
            sql += String.format("belongEntity = '%s' and applyType = '%s' and createdBy.roleId = '%s' order by configName",
                    entity, DataListManager.TYPE_DATALIST, RoleService.ADMIN_ROLE);
        } else {
            // 普通用户可用的
            ID[] uses = DataListManager.instance.getUsesDataListId(entity, user);
            sql += "configId in ('" + StringUtils.join(uses, "', '") + "')";
        }

        Object[][] list = RebuildApplication.createQueryNoFilter(sql).array();
        writeSuccess(response, list);
    }

    /**
     * @param field
     * @return
     */
    private boolean canUseField(Field field) {
        return field.isQueryable() && EasyMeta.getDisplayType(field) != DisplayType.BARCODE;
    }
}