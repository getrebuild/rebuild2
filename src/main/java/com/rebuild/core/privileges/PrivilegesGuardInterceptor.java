/*
Copyright (c) REBUILD <https://getrebuild.com/> and/or its owners. All rights reserved.

rebuild is dual-licensed under commercial and open source licenses (GPLv3).
See LICENSE and COMMERCIAL in the project root for license information.
*/

package com.rebuild.core.privileges;

import cn.devezhao.bizz.privileges.Permission;
import cn.devezhao.bizz.privileges.PrivilegesException;
import cn.devezhao.bizz.privileges.impl.BizzPermission;
import cn.devezhao.bizz.security.AccessDeniedException;
import cn.devezhao.persist4j.Entity;
import cn.devezhao.persist4j.Field;
import cn.devezhao.persist4j.Record;
import cn.devezhao.persist4j.engine.ID;
import com.rebuild.core.Application;
import com.rebuild.core.metadata.MetadataHelper;
import com.rebuild.core.metadata.impl.EasyMeta;
import com.rebuild.core.service.CommonsService;
import com.rebuild.core.service.ServiceSpec;
import com.rebuild.core.service.general.BulkContext;
import com.rebuild.core.service.general.EntityService;
import org.aopalliance.intercept.MethodInterceptor;
import org.aopalliance.intercept.MethodInvocation;
import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.springframework.util.Assert;

import java.lang.reflect.Method;
import java.security.Guard;

/**
 * 权限验证 - 拦截所有 *Service 方法
 *
 * @author devezhao
 * @since 10/12/2018
 */
public class PrivilegesGuardInterceptor implements MethodInterceptor, Guard {

    private static final Log LOG = LogFactory.getLog(PrivilegesGuardInterceptor.class);

    @Override
    public Object invoke(MethodInvocation invocation) throws Throwable {
        checkGuard(invocation);
        return invocation.proceed();
    }

    @Override
    public void checkGuard(Object object) throws SecurityException {
        final MethodInvocation invocation = (MethodInvocation) object;
        if (!isGuardMethod(invocation)) {
            return;
        }

        final ID caller = Application.getSessionStore().get();
        if (Application.devMode()) {
            LOG.info("User [ " + caller + " ] calls : " + invocation.getMethod());
        }

        Class<?> invocationClass = invocation.getThis().getClass();
        // 验证管理员操作
        if (AdminGuard.class.isAssignableFrom(invocationClass) && !UserHelper.isAdmin(caller)) {
            throw new AccessDeniedException("非法操作请求 (E" + ((ServiceSpec) invocation.getThis()).getEntityCode() + ")");
        }
        // 仅 EntityService 或子类会验证角色权限
        if (!EntityService.class.isAssignableFrom(invocationClass)) {
            return;
        }

        boolean isBulk = invocation.getMethod().getName().startsWith("bulk");
        if (isBulk) {
            Object firstArgument = invocation.getArguments()[0];
            if (!BulkContext.class.isAssignableFrom(firstArgument.getClass())) {
                throw new IllegalArgumentException("First argument must be BulkContext!");
            }

            BulkContext context = (BulkContext) firstArgument;
            Entity entity = context.getMainEntity();
            if (!Application.getPrivilegesManager().allow(caller, entity.getEntityCode(), context.getAction())) {
                LOG.error("User [ " + caller + " ] not allowed execute action [ " + context.getAction() + " ]. Entity : " + context.getMainEntity());
                throw new AccessDeniedException(formatHumanMessage(context.getAction(), entity, null));
            }
            return;
        }

        Object idOrRecord = invocation.getArguments()[0];

        ID recordId;
        Entity entity;

        if (Record.class.isAssignableFrom(idOrRecord.getClass())) {
            recordId = ((Record) idOrRecord).getPrimary();
            entity = ((Record) idOrRecord).getEntity();
        } else if (ID.class.isAssignableFrom(idOrRecord.getClass())) {
            recordId = (ID) idOrRecord;
            entity = MetadataHelper.getEntity(recordId.getEntityCode());
        } else {
            throw new IllegalArgumentException("First argument must be Record or ID : " + idOrRecord);
        }

        // 忽略权限检查
        if (EasyMeta.valueOf(entity).isPlainEntity()) return;

        Permission action = getPermissionByMethod(invocation.getMethod(), recordId == null);

        boolean allowed;
        if (action == BizzPermission.CREATE) {
            // 明细实体
            if (entity.getMasterEntity() != null) {
                Assert.isTrue(Record.class.isAssignableFrom(idOrRecord.getClass()), "First argument must be Record!");

                Field stmField = MetadataHelper.getSlaveToMasterField(entity);
                ID masterId = ((Record) idOrRecord).getID(stmField.getName());
                if (masterId == null || !Application.getPrivilegesManager().allowUpdate(caller, masterId)) {
                    throw new AccessDeniedException("你没有添加明细的权限");
                }
                allowed = true;

            } else {
                allowed = Application.getPrivilegesManager().allow(caller, entity.getEntityCode(), action);
            }

        } else {
            Assert.notNull(recordId, "No primary in record!");
            allowed = Application.getPrivilegesManager().allow(caller, recordId, action);
        }

        // 无权限操作
        if (!allowed && IN_NOPERMISSION_PASS.get() != null) {
            allowed = true;
            IN_NOPERMISSION_PASS.remove();
            LOG.warn("Allow no permission(" + action.getName() + ") passed once : " + recordId);
        }

        if (!allowed) {
            LOG.error("User [ " + caller + " ] not allowed execute action [ " + action + " ]. " + (recordId == null ? "Entity : " + entity : "Record : " + recordId));
            throw new AccessDeniedException(formatHumanMessage(action, entity, recordId));
        }
    }

    /**
     * @param invocation
     * @return
     */
    private boolean isGuardMethod(MethodInvocation invocation) {
        if (CommonsService.class.isAssignableFrom(invocation.getThis().getClass())) {
            return false;
        }

        String action = invocation.getMethod().getName();
        return action.startsWith("create") || action.startsWith("update") || action.startsWith("delete")
                || action.startsWith("assign") || action.startsWith("share") || action.startsWith("unshare")
                || action.startsWith("bulk");
    }

    /**
     * @param method
     * @param isNew
     * @return
     */
    private Permission getPermissionByMethod(Method method, boolean isNew) {
        String action = method.getName();
        if (action.startsWith("createOrUpdate")) {
            return isNew ? BizzPermission.CREATE : BizzPermission.UPDATE;
        } else if (action.startsWith("create")) {
            return BizzPermission.CREATE;
        } else if (action.startsWith("update")) {
            return BizzPermission.UPDATE;
        } else if (action.startsWith("delete")) {
            return BizzPermission.DELETE;
        } else if (action.startsWith("assign")) {
            return BizzPermission.ASSIGN;
        } else if (action.startsWith("share")) {
            return BizzPermission.SHARE;
        } else if (action.startsWith("unshare")) {
            return EntityService.UNSHARE;
        }
        throw new PrivilegesException("No matchs Permission found : " + action);
    }

    /**
     * @param action
     * @param entity
     * @param target
     * @return
     */
    private String formatHumanMessage(Permission action, Entity entity, ID target) {
        String actionHuman = null;
        if (action == BizzPermission.CREATE) {
            actionHuman = "新建";
        } else if (action == BizzPermission.UPDATE) {
            actionHuman = "修改";
        } else if (action == BizzPermission.DELETE) {
            actionHuman = "删除";
        } else if (action == BizzPermission.ASSIGN) {
            actionHuman = "分派";
        } else if (action == BizzPermission.SHARE) {
            actionHuman = "共享";
        } else if (action == EntityService.UNSHARE) {
            actionHuman = "取消共享";
        }

        if (target == null) {
            return String.format("你没有%s%s权限", actionHuman, EasyMeta.getLabel(entity));
        }
        return String.format("你没有%s此记录的权限", actionHuman);
    }

    // --

    private static final ThreadLocal<ID> IN_NOPERMISSION_PASS = new ThreadLocal<>();

    /**
     * 允许无权限操作一次
     *
     * @param record
     */
    public static void setNoPermissionPassOnce(ID record) {
        Assert.notNull(record, "'record' not be null");
        IN_NOPERMISSION_PASS.set(record);
    }
}
