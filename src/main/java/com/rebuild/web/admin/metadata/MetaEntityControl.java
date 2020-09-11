/*
Copyright (c) REBUILD <https://getrebuild.com/> and/or its owners. All rights reserved.

rebuild is dual-licensed under commercial and open source licenses (GPLv3).
See LICENSE and COMMERCIAL in the project root for license information.
*/

package com.rebuild.web.admin.metadata;

import cn.devezhao.commons.web.ServletUtils;
import cn.devezhao.persist4j.Entity;
import cn.devezhao.persist4j.Record;
import cn.devezhao.persist4j.engine.ID;
import com.alibaba.fastjson.JSON;
import com.alibaba.fastjson.JSONObject;
import com.rebuild.core.Application;
import com.rebuild.core.metadata.impl.MetaEntityService;
import com.rebuild.core.support.RebuildConfiguration;
import com.rebuild.core.support.task.TaskExecutors;
import com.rebuild.core.metadata.EntityHelper;
import com.rebuild.core.metadata.MetadataHelper;
import com.rebuild.core.metadata.MetadataSorter;
import com.rebuild.core.metadata.impl.EasyMeta;
import com.rebuild.core.metadata.impl.Entity2Schema;
import com.rebuild.core.privileges.UserHelper;
import com.rebuild.core.rbstore.MetaSchemaGenerator;
import com.rebuild.core.service.general.QuickCodeReindexTask;
import com.rebuild.utils.JSONUtils;
import com.rebuild.web.BaseController;
import com.rebuild.web.commons.FileDownloader;
import org.apache.commons.io.FileUtils;
import org.apache.commons.lang.StringUtils;
import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.servlet.ModelAndView;

import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import java.io.File;
import java.io.IOException;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * @author zhaofang123@gmail.com
 * @since 08/03/2018
 */
@Controller
@RequestMapping("/admin/")
public class MetaEntityControl extends BaseController {

    @GetMapping("entities")
    public ModelAndView page(HttpServletRequest request) {
        ModelAndView mv = createModelAndView("/admin/metadata/entities");
        mv.getModel().put("isSuperAdmin", UserHelper.isSuperAdmin(getRequestUser(request)));
        return mv;
    }

    @RequestMapping("entity/{entity}/base")
    public ModelAndView pageBase(@PathVariable String entity) {
        ModelAndView mv = createModelAndView("/admin/metadata/entity-edit");
        setEntityBase(mv, entity);

        Entity entityMeta = MetadataHelper.getEntity(entity);
        mv.getModel().put("nameField", MetadataHelper.getNameField(entityMeta).getName());

        if (entityMeta.getMasterEntity() != null) {
            mv.getModel().put("masterEntity", entityMeta.getMasterEntity().getName());
            mv.getModel().put("slaveEntity", entityMeta.getName());
        } else if (entityMeta.getSlaveEntity() != null) {
            mv.getModel().put("masterEntity", entityMeta.getName());
            mv.getModel().put("slaveEntity", entityMeta.getSlaveEntity().getName());
        }

        // 扩展配置
        mv.getModel().put("entityExtConfig", EasyMeta.valueOf(entityMeta).getExtraAttrs(true));

        return mv;
    }

    @RequestMapping("entity/{entity}/advanced")
    public ModelAndView pageAdvanced(@PathVariable String entity, HttpServletRequest request) {
        ModelAndView mv = createModelAndView("/admin/metadata/entity-advanced");
        mv.getModel().put("isSuperAdmin", UserHelper.isSuperAdmin(getRequestUser(request)));
        setEntityBase(mv, entity);
        return mv;
    }

    @RequestMapping("entity/entity-list")
    public void listEntity(HttpServletRequest request, HttpServletResponse response) {
        // 默认无BIZZ实体
        final boolean usesBizz = getBoolParameter(request, "bizz", false);
        // 默认无明细实体
        final boolean usesSlave = getBoolParameter(request, "slave", false);

        List<Map<String, Object>> ret = new ArrayList<>();
        for (Entity entity : MetadataSorter.sortEntities(null, usesBizz, usesSlave)) {
            EasyMeta easyMeta = new EasyMeta(entity);
            Map<String, Object> map = new HashMap<>();
            map.put("entityName", easyMeta.getName());
            map.put("entityLabel", easyMeta.getLabel());
            map.put("comments", easyMeta.getComments());
            map.put("icon", easyMeta.getIcon());
            map.put("builtin", easyMeta.isBuiltin());
            if (entity.getSlaveEntity() != null) {
                map.put("slaveEntity", entity.getSlaveEntity().getName());
            }
            if (entity.getMasterEntity() != null) {
                map.put("masterEntity", entity.getMasterEntity().getName());
            }
            ret.add(map);
        }
        writeSuccess(response, ret);
    }

    @RequestMapping("entity/entity-new")
    public void entityNew(HttpServletRequest request, HttpServletResponse response) {
        ID user = getRequestUser(request);
        JSONObject reqJson = (JSONObject) ServletUtils.getRequestJson(request);

        String label = reqJson.getString("label");
        String comments = reqJson.getString("comments");
        String masterEntity = reqJson.getString("masterEntity");
        if (StringUtils.isNotBlank(masterEntity)) {
            if (!MetadataHelper.containsEntity(masterEntity)) {
                writeFailure(response,
                        getLang(request,"SomeInvalid", "MasterEntity") + " : " + masterEntity);
                return;
            }

            Entity useMaster = MetadataHelper.getEntity(masterEntity);
            if (useMaster.getMasterEntity() != null) {
                writeFailure(response, getLang(request, "SlaveEntityNotBeMaster"));
                return;
            } else if (useMaster.getSlaveEntity() != null) {
                writeFailure(response,
                        String.format(getLang(request, "SelectMasterEntityBeXUsed"), useMaster.getSlaveEntity()));
                return;
            }
        }

        try {
            String entityName = new Entity2Schema(user)
                    .createEntity(label, comments, masterEntity, getBoolParameter(request, "nameField"));
            writeSuccess(response, entityName);
        } catch (Exception ex) {
            LOG.error(null, ex);
            writeFailure(response, ex.getLocalizedMessage());
        }
    }

    @RequestMapping("entity/entity-update")
    public void entityUpdate(HttpServletRequest request, HttpServletResponse response) {
        ID user = getRequestUser(request);
        JSON formJson = ServletUtils.getRequestJson(request);
        Record record = EntityHelper.parse((JSONObject) formJson, user);

        // 修改了名称字段
        String needReindex = null;
        String nameField = record.getString("nameField");
        if (nameField != null) {
            Object[] nameFieldOld = Application.createQueryNoFilter(
                    "select nameField,entityName from MetaEntity where entityId = ?")
                    .setParameter(1, record.getPrimary())
                    .unique();
            if (!nameField.equalsIgnoreCase((String) nameFieldOld[0])) {
                needReindex = (String) nameFieldOld[1];
            }
        }

        Application.getBean(MetaEntityService.class).update(record);

        if (needReindex != null) {
            Entity entity = MetadataHelper.getEntity(needReindex);
            if (entity.containsField(EntityHelper.QuickCode)) {
                QuickCodeReindexTask reindexTask = new QuickCodeReindexTask(entity);
                TaskExecutors.submit(reindexTask, user);
            }
        }

        writeSuccess(response);
    }

    @RequestMapping("entity/entity-drop")
    public void entityDrop(HttpServletRequest request, HttpServletResponse response) {
        ID user = getRequestUser(request);
        Entity entity = getEntityById(getIdParameterNotNull(request, "id"));
        boolean force = getBoolParameter(request, "force", false);

        boolean drop = new Entity2Schema(user).dropEntity(entity, force);
        if (drop) writeSuccess(response);
        else writeFailure(response);
    }

    @RequestMapping("entity/entity-export")
    public void entityExport(HttpServletRequest request, HttpServletResponse response) throws IOException {
        Entity entity = getEntityById(getIdParameterNotNull(request, "id"));

        File dest = RebuildConfiguration.getFileOfTemp("schema-" + entity.getName() + ".json");
        if (dest.exists()) {
            FileUtils.deleteQuietly(dest);
        }
        new MetaSchemaGenerator(entity).generate(dest);

        if (ServletUtils.isAjaxRequest(request)) {
            writeSuccess(response, JSONUtils.toJSONObject("file", dest.getName()));
        } else {
            FileDownloader.setDownloadHeaders(request, response, dest.getName());
            FileDownloader.writeLocalFile(dest.getName(), true, response);
        }
    }

    /**
     * @param metaId
     * @return
     */
    private Entity getEntityById(ID metaId) {
        Object[] entityRecord = Application.createQueryNoFilter(
                "select entityName from MetaEntity where entityId = ?")
                .setParameter(1, metaId)
                .unique();
        String entityName = (String) entityRecord[0];
        return MetadataHelper.getEntity(entityName);
    }

    /**
     * 设置实体信息
     *
     * @param mv
     * @param entity
     * @return
     */
    protected static EasyMeta setEntityBase(ModelAndView mv, String entity) {
        EasyMeta entityMeta = EasyMeta.valueOf(entity);
        mv.getModel().put("entityMetaId", entityMeta.getMetaId());
        mv.getModel().put("entityName", entityMeta.getName());
        mv.getModel().put("entityLabel", entityMeta.getLabel());
        mv.getModel().put("icon", entityMeta.getIcon());
        mv.getModel().put("comments", entityMeta.getComments());
        return entityMeta;
    }
}
