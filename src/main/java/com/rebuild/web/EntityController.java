/*
Copyright (c) REBUILD <https://getrebuild.com/> and its owners. All rights reserved.

rebuild is dual-licensed under commercial and open source licenses (GPLv3).
See LICENSE and COMMERCIAL in the project root for license information.
*/

package com.rebuild.web;

import cn.devezhao.bizz.privileges.Permission;
import cn.devezhao.bizz.privileges.Privileges;
import cn.devezhao.bizz.privileges.impl.BizzPermission;
import cn.devezhao.persist4j.Entity;
import cn.devezhao.persist4j.engine.ID;
import com.alibaba.fastjson.JSON;
import com.rebuild.core.RebuildApplication;
import com.rebuild.core.metadata.MetadataHelper;
import com.rebuild.core.metadata.impl.EasyMeta;
import com.rebuild.utils.JSONUtils;
import org.springframework.web.servlet.ModelAndView;

import java.util.HashMap;
import java.util.Map;

/**
 * 页面上需要某个实体信息的 Controller
 *
 * @author devezhao
 * @since 01/10/2019
 */
public abstract class EntityController extends BaseController {

    private static final String PLAIN_ENTITY_PRIVILEGES = "{C:true,D:true,U:true,R:true}";

    /**
     * @param page
     * @param entity
     * @param user
     * @return
     */
    protected ModelAndView createModelAndView(String page, String entity, ID user) {
        ModelAndView mv = createModelAndView(page);
        Entity entityMeta = MetadataHelper.getEntity(entity);
        putEntityMeta(mv, entityMeta);

        if (MetadataHelper.hasPrivilegesField(entityMeta)) {
            Privileges priv = RebuildApplication.getPrivilegesManager().getPrivileges(user, entityMeta.getEntityCode());
            Permission[] actions = new Permission[]{
                    BizzPermission.CREATE,
                    BizzPermission.DELETE,
                    BizzPermission.UPDATE,
                    BizzPermission.READ,
                    BizzPermission.ASSIGN,
                    BizzPermission.SHARE,
            };
            Map<String, Boolean> actionMap = new HashMap<>();
            for (Permission act : actions) {
                actionMap.put(act.getName(), priv.allowed(act));
            }
            mv.getModel().put("entityPrivileges", JSON.toJSONString(actionMap));
        } else if (EasyMeta.valueOf(entityMeta).isPlainEntity()) {
            mv.getModel().put("entityPrivileges", PLAIN_ENTITY_PRIVILEGES);
        } else {
            mv.getModel().put("entityPrivileges", JSONUtils.EMPTY_OBJECT_STR);
        }
        return mv;
    }

    /**
     * @param page
     * @param record
     * @param user
     * @return
     */
    protected ModelAndView createModelAndView(String page, ID record, ID user) {
        ModelAndView mv = createModelAndView(page);
        Entity entity = MetadataHelper.getEntity(record.getEntityCode());
        putEntityMeta(mv, entity);

        // 使用主实体权限
        if (entity.getMasterEntity() != null) {
            entity = entity.getMasterEntity();
        }
        if (MetadataHelper.hasPrivilegesField(entity)) {
            Permission[] actions = new Permission[]{
                    BizzPermission.CREATE,
                    BizzPermission.DELETE,
                    BizzPermission.UPDATE,
                    BizzPermission.READ,
                    BizzPermission.ASSIGN,
                    BizzPermission.SHARE,
            };
            Map<String, Boolean> actionMap = new HashMap<>();
            for (Permission act : actions) {
                actionMap.put(act.getName(), RebuildApplication.getPrivilegesManager().allow(user, record, act));
            }
            mv.getModel().put("entityPrivileges", JSON.toJSONString(actionMap));
        } else if (EasyMeta.valueOf(entity).isPlainEntity()) {
            mv.getModel().put("entityPrivileges", PLAIN_ENTITY_PRIVILEGES);
        } else {
            mv.getModel().put("entityPrivileges", JSONUtils.EMPTY_OBJECT_STR);
        }
        return mv;
    }

    /**
     * @param into
     * @param entity
     */
    protected void putEntityMeta(ModelAndView into, Entity entity) {
        EasyMeta easyMeta = EasyMeta.valueOf(entity);
        into.getModel().put("entityName", easyMeta.getName());
        into.getModel().put("entityLabel", easyMeta.getLabel());
        into.getModel().put("entityIcon", easyMeta.getIcon());

        EasyMeta master = null;
        EasyMeta slave = null;
        if (entity.getMasterEntity() != null) {
            master = EasyMeta.valueOf(entity.getMasterEntity());
            slave = EasyMeta.valueOf(entity);
        } else if (entity.getSlaveEntity() != null) {
            master = EasyMeta.valueOf(entity);
            slave = EasyMeta.valueOf(entity.getSlaveEntity());
        } else {
            into.getModel().put("masterEntity", easyMeta.getName());
        }

        if (master != null) {
            into.getModel().put("masterEntity", master.getName());
            into.getModel().put("masterEntityLabel", master.getLabel());
            into.getModel().put("masterEntityIcon", master.getIcon());
            into.getModel().put("slaveEntity", slave.getName());
            into.getModel().put("slaveEntityLabel", slave.getLabel());
            into.getModel().put("slaveEntityIcon", slave.getIcon());
        }
    }
}
