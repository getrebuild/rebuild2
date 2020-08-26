/*
Copyright (c) REBUILD <https://getrebuild.com/> and/or its owners. All rights reserved.

rebuild is dual-licensed under commercial and open source licenses (GPLv3).
See LICENSE and COMMERCIAL in the project root for license information.
*/

package com.rebuild.core.service.approval;

import cn.devezhao.commons.ObjectUtils;
import cn.devezhao.persist4j.Entity;
import cn.devezhao.persist4j.engine.ID;
import com.rebuild.core.RebuildApplication;
import com.rebuild.core.metadata.EntityHelper;
import com.rebuild.core.metadata.MetadataHelper;
import com.rebuild.core.service.NoRecordFoundException;
import org.springframework.util.Assert;

/**
 * @author devezhao
 * @since 2019/10/23
 */
public class ApprovalHelper {

    /**
     * 获取提交人
     *
     * @param record
     * @return
     */
    public static ID getSubmitter(ID record) {
        Object[] approvalId = RebuildApplication.getQueryFactory().uniqueNoFilter(record, EntityHelper.ApprovalId);
        Assert.notNull(approvalId, "Couldn't found approval of record : " + record);
        return getSubmitter(record, (ID) approvalId[0]);
    }

    /**
     * 获取提交人
     *
     * @param record
     * @param approval
     * @return
     */
    public static ID getSubmitter(ID record, ID approval) {
        return RebuildApplication.getBean(ApprovalStepService.class).getSubmitter(record, approval);
    }

    /**
     * @param recordId
     * @return
     * @throws NoRecordFoundException
     */
    public static ApprovalStatus getApprovalStatus(ID recordId) throws NoRecordFoundException {
        Object[] o = RebuildApplication.getQueryFactory().uniqueNoFilter(recordId,
                EntityHelper.ApprovalId, EntityHelper.ApprovalId + ".name", EntityHelper.ApprovalState, EntityHelper.ApprovalStepNode);
        if (o == null) {
            throw new NoRecordFoundException("记录不存在或你无权查看");
        }
        return new ApprovalStatus((ID) o[0], (String) o[1], (Integer) o[2], (String) o[3], recordId);
    }

    /**
     * @param recordId
     * @return
     * @throws NoRecordFoundException
     * @see #getApprovalStatus(ID)
     */
    public static ApprovalState getApprovalState(ID recordId) throws NoRecordFoundException {
        return getApprovalStatus(recordId).getCurrentState();
    }

    /**
     * 流程是否正在使用中（处于审核中）
     *
     * @param approvalId
     * @return
     */
    public static int checkInUsed(ID approvalId) {
        Object[] belongEntity = RebuildApplication.createQueryNoFilter(
                "select belongEntity from RobotApprovalConfig where configId = ?")
                .setParameter(1, approvalId)
                .unique();
        Entity entity = MetadataHelper.getEntity((String) belongEntity[0]);

        String sql = String.format(
                "select count(%s) from %s where approvalId = ? and approvalState = ?",
                entity.getPrimaryField().getName(), entity.getName());
        Object[] inUsed = RebuildApplication.createQueryNoFilter(sql)
                .setParameter(1, approvalId)
                .setParameter(2, ApprovalState.PROCESSING.getState())
                .unique();

        return inUsed != null ? ObjectUtils.toInt(inUsed[0]) : 0;
    }
}
