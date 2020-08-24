/*
Copyright (c) REBUILD <https://getrebuild.com/> and its owners. All rights reserved.

rebuild is dual-licensed under commercial and open source licenses (GPLv3).
See LICENSE and COMMERCIAL in the project root for license information.
*/

package com.rebuild.web.admin.base;

import cn.devezhao.commons.web.ServletUtils;
import cn.devezhao.persist4j.Entity;
import cn.devezhao.persist4j.Record;
import cn.devezhao.persist4j.engine.ID;
import com.alibaba.fastjson.JSON;
import com.alibaba.fastjson.JSONObject;
import com.rebuild.core.RebuildApplication;
import com.rebuild.core.helper.RebuildConfiguration;
import com.rebuild.core.helper.task.TaskExecutors;
import com.rebuild.core.metadata.EntityHelper;
import com.rebuild.core.metadata.MetadataHelper;
import com.rebuild.core.metadata.MetadataSorter;
import com.rebuild.core.metadata.impl.EasyMeta;
import com.rebuild.core.metadata.impl.Entity2Schema;
import com.rebuild.core.privileges.UserHelper;
import com.rebuild.core.rbstore.MetaSchemaGenerator;
import com.rebuild.core.service.general.QuickCodeReindexTask;
import com.rebuild.utils.CommonsUtils;
import com.rebuild.utils.JSONUtils;
import com.rebuild.web.BaseController;
import com.rebuild.web.commons.FileDownloader;
import org.apache.commons.io.FileUtils;
import org.apache.commons.lang.StringUtils;
import org.springframework.stereotype.Controller;
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
public class MetaEntityControll extends BaseController {

    @RequestMapping("entities")
    public ModelAndView page(HttpServletRequest request) throws IOException {
        ModelAndView mv = createModelAndView("/admin/entityhub/entity-grid.jsp");
        mv.getModel().put("isSuperAdmin", UserHelper.isSuperAdmin(getRequestUser(request)));
        return mv;
    }

    @RequestMapping("entity/{entity}/base")
    public ModelAndView pageBase(@PathVariable String entity) throws IOException {
        ModelAndView mv = createModelAndView("/admin/entityhub/entity-edit.jsp");
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

        return mv;
    }

    @RequestMapping("entity/{entity}/advanced")
    public ModelAndView pageAdvanced(@PathVariable String entity, HttpServletRequest request) throws IOException {
        ModelAndView mv = createModelAndView("/admin/entityhub/entity-advanced.jsp");
        mv.getModel().put("isSuperAdmin", UserHelper.isSuperAdmin(getRequestUser(request)));
        setEntityBase(mv, entity);
        return mv;
    }

    @RequestMapping("entity/entity-list")
    public void listEntity(HttpServletRequest request, HttpServletResponse response) throws IOException {
        // 有无 BIZZ 实体
        final boolean nobizz = getBoolParameter(request, "nobizz", false);
        // 有无明细实体
        final boolean noslave = getBoolParameter(request, "noslave", true);

        List<Map<String, Object>> ret = new ArrayList<>();
        for (Entity entity : MetadataSorter.sortEntities()) {
            if ((noslave && entity.getMasterEntity() != null)
                    || (nobizz && MetadataHelper.isBizzEntity(entity.getEntityCode()))) {
                continue;
            }

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
    public void entityNew(HttpServletRequest request, HttpServletResponse response) throws IOException {
        ID user = getRequestUser(request);
        JSONObject reqJson = (JSONObject) ServletUtils.getRequestJson(request);

        String label = reqJson.getString("label");
        String comments = reqJson.getString("comments");
        String masterEntity = reqJson.getString("masterEntity");
        if (StringUtils.isNotBlank(masterEntity)) {
            if (!MetadataHelper.containsEntity(masterEntity)) {
                writeFailure(response, "无效主实体 : " + masterEntity);
                return;
            }

            Entity master = MetadataHelper.getEntity(masterEntity);
            if (master.getMasterEntity() != null) {
                writeFailure(response, "明细实体不能作为主实体");
                return;
            } else if (master.getSlaveEntity() != null) {
                writeFailure(response, "选择的主实体已被 " + EasyMeta.getLabel(master.getSlaveEntity()) + " 使用");
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
    public void entityUpdate(HttpServletRequest request, HttpServletResponse response) throws IOException {
        ID user = getRequestUser(request);
        JSON formJson = ServletUtils.getRequestJson(request);
        Record record = EntityHelper.parse((JSONObject) formJson, user);

        // 修改了名称字段
        String needReindex = null;
        String nameField = record.getString("nameField");
        if (nameField != null) {
            Object[] nameFieldOld = RebuildApplication.createQueryNoFilter(
                    "select nameField,entityName from MetaEntity where entityId = ?")
                    .setParameter(1, record.getPrimary())
                    .unique();
            if (!nameField.equalsIgnoreCase((String) nameFieldOld[0])) {
                needReindex = (String) nameFieldOld[1];
            }
        }

        RebuildApplication.getCommonsService().update(record);
        RebuildApplication.getMetadataFactory().refresh(false);

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
    public void entityDrop(HttpServletRequest request, HttpServletResponse response) throws IOException {
        ID user = getRequestUser(request);
        Entity entity = getEntityById(getIdParameterNotNull(request, "id"));
        boolean force = getBoolParameter(request, "force", false);

        boolean drop = new Entity2Schema(user).dropEntity(entity, force);
        if (drop) {
            writeSuccess(response);
        } else {
            writeFailure(response, "删除失败，请确认该实体是否可被删除");
        }
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
        Object[] entityRecord = RebuildApplication.createQueryNoFilter(
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
        mv.getModel().put("entityLabel", CommonsUtils.escapeHtml(entityMeta.getLabel()));
        mv.getModel().put("icon", entityMeta.getIcon());
        mv.getModel().put("comments", CommonsUtils.escapeHtml(entityMeta.getComments()));
        return entityMeta;
    }
}
