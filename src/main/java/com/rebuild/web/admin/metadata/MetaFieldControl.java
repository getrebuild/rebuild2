/*
Copyright (c) REBUILD <https://getrebuild.com/> and/or its owners. All rights reserved.

rebuild is dual-licensed under commercial and open source licenses (GPLv3).
See LICENSE and COMMERCIAL in the project root for license information.
*/

package com.rebuild.web.admin.metadata;

import cn.devezhao.commons.web.ServletUtils;
import cn.devezhao.persist4j.Entity;
import cn.devezhao.persist4j.Field;
import cn.devezhao.persist4j.Record;
import cn.devezhao.persist4j.dialect.FieldType;
import cn.devezhao.persist4j.dialect.Type;
import cn.devezhao.persist4j.engine.ID;
import com.alibaba.fastjson.JSON;
import com.alibaba.fastjson.JSONObject;
import com.rebuild.core.Application;
import com.rebuild.core.metadata.impl.*;
import com.rebuild.core.support.state.StateHelper;
import com.rebuild.core.metadata.EntityHelper;
import com.rebuild.core.metadata.MetadataHelper;
import com.rebuild.core.metadata.MetadataSorter;
import com.rebuild.core.privileges.UserHelper;
import com.rebuild.utils.JSONUtils;
import com.rebuild.web.BaseController;
import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.servlet.ModelAndView;

import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import java.io.IOException;
import java.util.*;

/**
 * @author zhaofang123@gmail.com
 * @since 08/19/2018
 */
@Controller
@RequestMapping("/admin/entity/")
public class MetaFieldControl extends BaseController {

    @GetMapping("{entity}/fields")
    public ModelAndView page(@PathVariable String entity, HttpServletRequest request) {
        ModelAndView mv = createModelAndView("/admin/metadata/fields");
        MetaEntityControl.setEntityBase(mv, entity);

        String nameField = MetadataHelper.getNameField(entity).getName();
        mv.getModel().put("nameField", nameField);
        mv.getModel().put("isSuperAdmin", UserHelper.isSuperAdmin(getRequestUser(request)));
        return mv;
    }

    @RequestMapping("list-field")
    public void listField(HttpServletRequest request, HttpServletResponse response) {
        Entity entity = MetadataHelper.getEntity(getParameterNotNull(request, "entity"));

        List<Field> allFields = new ArrayList<>();
        Collections.addAll(allFields, entity.getFields());

        List<Map<String, Object>> ret = new ArrayList<>();
        for (Field field : MetadataSorter.sort(allFields)) {
            if (MetadataHelper.isSystemField(field)) continue;

            EasyMeta easyMeta = EasyMeta.valueOf(field);
            Map<String, Object> map = new HashMap<>();
            if (easyMeta.getMetaId() != null) {
                map.put("fieldId", easyMeta.getMetaId());
            }
            map.put("fieldName", easyMeta.getName());
            map.put("fieldLabel", easyMeta.getLabel());
            map.put("comments", easyMeta.getComments());
            map.put("displayType", easyMeta.getDisplayType().getDisplayName());
            map.put("nullable", field.isNullable());
            map.put("builtin", easyMeta.isBuiltin());
            map.put("creatable", field.isCreatable());
            ret.add(map);
        }

        writeSuccess(response, ret);
    }

    @GetMapping("{entity}/field/{field}")
    public ModelAndView pageEntityField(@PathVariable String entity, @PathVariable String field,
                                        HttpServletRequest request, HttpServletResponse response) throws IOException {
        if (!MetadataHelper.checkAndWarnField(entity, field)) {
            response.sendError(404, getLang(request, "SomeInvalid", "Field"));
            return null;
        }

        ModelAndView mv = createModelAndView("/admin/metadata/field-edit");
        EasyMeta easyEntity = MetaEntityControl.setEntityBase(mv, entity);

        Field fieldMeta = ((Entity) easyEntity.getBaseMeta()).getField(field);
        EasyMeta easyField = new EasyMeta(fieldMeta);

        mv.getModel().put("fieldMetaId", easyField.getMetaId());
        mv.getModel().put("fieldName", easyField.getName());
        mv.getModel().put("fieldLabel", easyField.getLabel());
        mv.getModel().put("fieldComments", easyField.getComments());
        mv.getModel().put("fieldType", easyField.getDisplayType(false));
        mv.getModel().put("fieldTypeLabel", easyField.getDisplayType(true));
        mv.getModel().put("fieldNullable", fieldMeta.isNullable());
        mv.getModel().put("fieldCreatable", fieldMeta.isCreatable());
        mv.getModel().put("fieldUpdatable", fieldMeta.isUpdatable());
        mv.getModel().put("fieldRepeatable", fieldMeta.isRepeatable());
        mv.getModel().put("fieldQueryable", fieldMeta.isQueryable());
        mv.getModel().put("fieldBuildin", easyField.isBuiltin());
        mv.getModel().put("fieldDefaultValue", fieldMeta.getDefaultValue());
        mv.getModel().put("isSuperAdmin", UserHelper.isSuperAdmin(getRequestUser(request)));

        // 明细实体
        if (((Entity) easyEntity.getBaseMeta()).getMasterEntity() != null) {
            Field stmField = MetadataHelper.getSlaveToMasterField((Entity) easyEntity.getBaseMeta());
            mv.getModel().put("isSlaveToMasterField", stmField.equals(fieldMeta));
        } else {
            mv.getModel().put("isSlaveToMasterField", false);
        }

        // 字段类型相关
        Type ft = fieldMeta.getType();
        if (ft == FieldType.REFERENCE) {
            Entity refentity = fieldMeta.getReferenceEntities()[0];
            mv.getModel().put("fieldRefentity", refentity.getName());
            mv.getModel().put("fieldRefentityLabel", new EasyMeta(refentity).getLabel());
        }
        mv.getModel().put("fieldExtConfig", easyField.getExtraAttrs(true));

        return mv;
    }

    @RequestMapping("field-new")
    public void fieldNew(HttpServletRequest request, HttpServletResponse response) {
        ID user = getRequestUser(request);
        JSONObject reqJson = (JSONObject) ServletUtils.getRequestJson(request);

        String entityName = reqJson.getString("entity");
        String label = reqJson.getString("label");
        String type = reqJson.getString("type");
        String comments = reqJson.getString("comments");
        String refEntity = reqJson.getString("refEntity");
        String refClassification = reqJson.getString("refClassification");
        String stateClass = reqJson.getString("stateClass");

        Entity entity = MetadataHelper.getEntity(entityName);
        DisplayType dt = DisplayType.valueOf(type);

        JSON extConfig = null;
        if (dt == DisplayType.CLASSIFICATION) {
            ID dataId = ID.valueOf(refClassification);
            extConfig = JSONUtils.toJSONObject(FieldExtConfigProps.CLASSIFICATION_USE, dataId);
        } else if (dt == DisplayType.STATE) {
            if (!StateHelper.isStateClass(stateClass)) {
                writeFailure(response, getLang(request, "SomeInvalid,StateClass"));
                return;
            }
            extConfig = JSONUtils.toJSONObject(FieldExtConfigProps.STATE_STATECLASS, stateClass);
        }

        String fieldName;
        try {
            fieldName = new Field2Schema(user).createField(entity, label, dt, comments, refEntity, extConfig);
            writeSuccess(response, fieldName);
        } catch (Exception ex) {
            writeFailure(response, ex.getLocalizedMessage());
        }
    }

    @RequestMapping("field-update")
    public void fieldUpdate(HttpServletRequest request, HttpServletResponse response) {
        ID user = getRequestUser(request);
        JSON formJson = ServletUtils.getRequestJson(request);
        Record record = EntityHelper.parse((JSONObject) formJson, user);

        Application.getBean(MetaFieldService.class).update(record);
        writeSuccess(response);
    }

    @RequestMapping("field-drop")
    public void fieldDrop(HttpServletRequest request, HttpServletResponse response) {
        ID user = getRequestUser(request);
        ID fieldId = getIdParameterNotNull(request, "id");

        Object[] fieldRecord = Application.createQueryNoFilter(
                "select belongEntity,fieldName from MetaField where fieldId = ?")
                .setParameter(1, fieldId)
                .unique();
        Field field = MetadataHelper.getEntity((String) fieldRecord[0]).getField((String) fieldRecord[1]);

        boolean drop = new Field2Schema(user).dropField(field, false);
        if (drop) writeSuccess(response);
        else writeFailure(response);
    }
}
