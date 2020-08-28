/*
Copyright (c) REBUILD <https://getrebuild.com/> and/or its owners. All rights reserved.

rebuild is dual-licensed under commercial and open source licenses (GPLv3).
See LICENSE and COMMERCIAL in the project root for license information.
*/

package com.rebuild.web.admin.base;

import cn.devezhao.commons.web.ServletUtils;
import cn.devezhao.persist4j.Entity;
import cn.devezhao.persist4j.Field;
import cn.devezhao.persist4j.Record;
import cn.devezhao.persist4j.engine.ID;
import com.alibaba.fastjson.JSON;
import com.rebuild.core.Application;
import com.rebuild.core.configuration.ConfigBean;
import com.rebuild.core.configuration.general.LayoutConfigService;
import com.rebuild.core.configuration.general.ViewAddonsManager;
import com.rebuild.core.metadata.EntityHelper;
import com.rebuild.core.metadata.MetadataHelper;
import com.rebuild.core.metadata.impl.EasyMeta;
import com.rebuild.utils.JSONUtils;
import com.rebuild.web.BaseController;
import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestMethod;

import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import java.io.IOException;
import java.util.HashSet;
import java.util.Set;

/**
 * 视图-相关项显示
 *
 * @author devezhao
 * @since 10/23/2018
 */
@Controller
@RequestMapping("/admin/entity/")
public class ViewAddonsControll extends BaseController {

    @RequestMapping(value = "{entity}/view-addons", method = RequestMethod.POST)
    public void sets(@PathVariable String entity,
                     HttpServletRequest request, HttpServletResponse response) throws IOException {
        ID user = getRequestUser(request);
        String applyType = getParameter(request, "type", ViewAddonsManager.TYPE_TAB);
        JSON config = ServletUtils.getRequestJson(request);

        ID configId = ViewAddonsManager.instance.detectUseConfig(user, entity, applyType);
        Record record;
        if (configId == null) {
            record = EntityHelper.forNew(EntityHelper.LayoutConfig, user);
            record.setString("belongEntity", entity);
            record.setString("applyType", applyType);
            record.setString("shareTo", ViewAddonsManager.SHARE_ALL);
        } else {
            record = EntityHelper.forUpdate(configId, user);
        }
        record.setString("config", config.toJSONString());
        Application.getBean(LayoutConfigService.class).createOrUpdate(record);

        writeSuccess(response);
    }

    @RequestMapping(value = "{entity}/view-addons", method = RequestMethod.GET)
    public void gets(@PathVariable String entity,
                     HttpServletRequest request, HttpServletResponse response) throws IOException {
        ID user = getRequestUser(request);
        String applyType = getParameter(request, "type", ViewAddonsManager.TYPE_TAB);

        Entity entityMeta = MetadataHelper.getEntity(entity);
        ConfigBean config = ViewAddonsManager.instance.getLayout(user, entity, applyType);

        Set<Entity> mfRefs = ViewAddonsManager.hasMultiFieldsReferenceTo(entityMeta);

        Set<String[]> refs = new HashSet<>();
        for (Field field : entityMeta.getReferenceToFields(true)) {
            Entity e = field.getOwnEntity();
            if (e.getMasterEntity() != null) {
                continue;
            }

            String label = EasyMeta.getLabel(e);
            if (mfRefs.contains(e)) {
                label = EasyMeta.getLabel(field) + " (" + label + ")";
            }
            refs.add(new String[]{e.getName() + ViewAddonsManager.EF_SPLIT + field.getName(), label});
        }

        // 跟进（动态）
        if (ViewAddonsManager.TYPE_TAB.equalsIgnoreCase(applyType)) {
            refs.add(new String[]{"Feeds.relatedRecord", "跟进"});
        }

        JSON ret = JSONUtils.toJSONObject(
                new String[]{"config", "refs"},
                new Object[]{config == null ? null : config.getJSON("config"), refs});
        writeSuccess(response, ret);
    }
}
