/*
Copyright (c) REBUILD <https://getrebuild.com/> and/or its owners. All rights reserved.

rebuild is dual-licensed under commercial and open source licenses (GPLv3).
See LICENSE and COMMERCIAL in the project root for license information.
*/

package com.rebuild.core.service.trigger.impl;

import cn.devezhao.bizz.privileges.impl.BizzPermission;
import cn.devezhao.persist4j.Entity;
import cn.devezhao.persist4j.engine.ID;
import com.alibaba.fastjson.JSONArray;
import com.alibaba.fastjson.JSONObject;
import com.rebuild.core.Application;
import com.rebuild.core.metadata.EntityHelper;
import com.rebuild.core.metadata.MetadataHelper;
import com.rebuild.core.privileges.PrivilegesGuardInterceptor;
import com.rebuild.core.privileges.UserHelper;
import com.rebuild.core.service.general.OperatingContext;
import com.rebuild.core.service.trigger.ActionContext;
import com.rebuild.core.service.trigger.ActionType;
import com.rebuild.core.service.trigger.TriggerAction;
import com.rebuild.core.service.trigger.TriggerException;
import com.rebuild.core.support.KVStorage;
import org.apache.commons.lang.StringUtils;
import org.apache.commons.lang.math.RandomUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.Set;

/**
 * @author devezhao
 * @since 2019/8/23
 */
public class AutoAssign implements TriggerAction {

    private static final Logger LOG = LoggerFactory.getLogger(AutoAssign.class);

    final protected ActionContext context;

    // 允许无权限分派
    final private boolean allowNoPermissionAssign;

    /**
     * @param context
     */
    public AutoAssign(ActionContext context) {
        this(context, Boolean.TRUE);
    }

    /**
     * @param context
     * @param allowNoPermissionAssign
     */
    public AutoAssign(ActionContext context, boolean allowNoPermissionAssign) {
        this.context = context;
        this.allowNoPermissionAssign = allowNoPermissionAssign;
    }

    @Override
    public ActionType getType() {
        return ActionType.AUTOASSIGN;
    }

    @Override
    public boolean isUsableSourceEntity(int entityCode) {
        Entity entity = MetadataHelper.getEntity(entityCode);
        return entity.containsField(EntityHelper.OwningUser) && entity.containsField(EntityHelper.OwningDept);
    }

    @Override
    public void execute(OperatingContext operatingContext) throws TriggerException {
        final JSONObject content = (JSONObject) context.getActionContent();
        final ID recordId = operatingContext.getAnyRecord().getPrimary();

        if (!allowNoPermissionAssign
                && !Application.getPrivilegesManager().allow(operatingContext.getOperator(), recordId, BizzPermission.ASSIGN)) {
            LOG.warn("No permission to assign record of target: " + recordId);
            return;
        }

        JSONArray assignTo = content.getJSONArray("assignTo");
        Set<ID> toUsers = UserHelper.parseUsers(assignTo, recordId, true);
        if (toUsers.isEmpty()) {
            return;
        }

        ID toUser = null;

        final boolean orderedAssign = ((JSONObject) context.getActionContent()).getIntValue("assignRule") == 1;
        final String orderedAssignKey = "AutoAssignLastAssignTo" + context.getConfigId();
        if (orderedAssign) {
            String lastAssignTo = KVStorage.getCustomValue(orderedAssignKey);
            ID lastAssignToUser = ID.isId(lastAssignTo) ? ID.valueOf(lastAssignTo) : null;

            ID firstUser = null;
            boolean isNext = false;
            for (ID u : toUsers) {
                if (firstUser == null) {
                    firstUser = u;
                }

                if (lastAssignToUser == null || isNext) {
                    toUser = u;
                    break;
                } else if (lastAssignToUser.equals(u)) {
                    isNext = true;
                }
            }

            if (toUser == null) {
                toUser = firstUser;
            }

        } else {
            int rnd = RandomUtils.nextInt(toUsers.size());
            for (ID u : toUsers) {
                if (--rnd == 0) {
                    toUser = u;
                    break;
                }
            }
        }

        String hasCascades = ((JSONObject) context.getActionContent()).getString("cascades");
        String[] cascades = null;
        if (StringUtils.isNotBlank(hasCascades)) {
            cascades = hasCascades.split("[,]");
        }

        if (allowNoPermissionAssign) {
            PrivilegesGuardInterceptor.setNoPermissionPassOnce(recordId);
        }
        Application.getEntityService(context.getSourceEntity().getEntityCode()).assign(recordId, toUser, cascades);

        // 当前分派人
        if (orderedAssign) {
            KVStorage.setCustomValue(orderedAssignKey, toUser);
        }
    }
}
