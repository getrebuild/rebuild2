/*
Copyright (c) REBUILD <https://getrebuild.com/> and/or its owners. All rights reserved.

rebuild is dual-licensed under commercial and open source licenses (GPLv3).
See LICENSE and COMMERCIAL in the project root for license information.
*/

package com.rebuild.core.service.approval;

import cn.devezhao.persist4j.Entity;
import cn.devezhao.persist4j.Field;
import cn.devezhao.persist4j.engine.ID;
import cn.devezhao.persist4j.metadata.CascadeModel;
import com.rebuild.core.RebuildApplication;
import com.rebuild.core.metadata.EntityHelper;
import com.rebuild.core.metadata.MetadataHelper;
import com.rebuild.core.metadata.impl.DisplayType;
import com.rebuild.core.metadata.impl.Field2Schema;
import com.rebuild.core.metadata.impl.MetadataException;

/**
 * 审批流程字段
 *
 * @author devezhao-mbp zhaofang123@gmail.com
 * @since 2019/07/04
 */
public class ApprovalFields2Schema extends Field2Schema {

    public ApprovalFields2Schema(ID user) {
        super(user);
    }

    /**
     * @param approvalEntity
     * @return Returns true if successful
     * @throws MetadataException
     */
    public boolean createFields(Entity approvalEntity) throws MetadataException {
        if (MetadataHelper.hasApprovalField(approvalEntity)) {
            return false;
        }

        Field apporvalId = createUnsafeField(approvalEntity, EntityHelper.ApprovalId, "审批流程",
                DisplayType.REFERENCE, true, false, false, true, null, "RobotApprovalConfig", CascadeModel.Ignore, null, null);
        Field apporvalState = createUnsafeField(approvalEntity, EntityHelper.ApprovalState, "审批状态",
                DisplayType.STATE, true, false, false, true, null, null, null, null, ApprovalState.DRAFT.getName());
        Field apporvalStepId = createUnsafeField(approvalEntity, EntityHelper.ApprovalStepNode, "当前步骤",
                DisplayType.TEXT, true, false, false, true, null, null, null, null, null);

        boolean schemaReady = schema2Database(approvalEntity, new Field[]{apporvalId, apporvalState, apporvalStepId});
        if (!schemaReady) {
            RebuildApplication.getCommonsService().delete(tempMetaId.toArray(new ID[0]));
            throw new MetadataException("无法创建审批流程字段到数据库");
        }

        RebuildApplication.getMetadataFactory().refresh(false);
        return true;
    }
}
