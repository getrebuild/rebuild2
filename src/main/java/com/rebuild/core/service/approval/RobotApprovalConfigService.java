/*
Copyright (c) REBUILD <https://getrebuild.com/> and/or its owners. All rights reserved.

rebuild is dual-licensed under commercial and open source licenses (GPLv3).
See LICENSE and COMMERCIAL in the project root for license information.
*/

package com.rebuild.core.service.approval;

import cn.devezhao.persist4j.Entity;
import cn.devezhao.persist4j.PersistManagerFactory;
import cn.devezhao.persist4j.Record;
import cn.devezhao.persist4j.engine.ID;
import com.rebuild.core.RebuildApplication;
import com.rebuild.core.configuration.BaseConfigurationService;
import com.rebuild.core.metadata.EntityHelper;
import com.rebuild.core.metadata.MetadataHelper;
import com.rebuild.core.privileges.AdminGuard;
import com.rebuild.core.service.DataSpecificationException;
import org.springframework.stereotype.Service;

/**
 * 审批流程
 *
 * @author devezhao zhaofang123@gmail.com
 * @since 2019/06/27
 */
@Service
public class RobotApprovalConfigService extends BaseConfigurationService implements AdminGuard {

    protected RobotApprovalConfigService(PersistManagerFactory aPMFactory) {
        super(aPMFactory);
    }

    @Override
    public int getEntityCode() {
        return EntityHelper.RobotApprovalConfig;
    }

    @Override
    public Record create(Record record) {
        String entity = record.getString("belongEntity");
        new ApprovalFields2Schema(RebuildApplication.getCurrentUser())
                .createFields(MetadataHelper.getEntity(entity));
        return super.create(record);
    }

    @Override
    public Record update(Record record) {
        if (record.hasValue("flowDefinition")) {
            int inUsed = ApprovalHelper.checkInUsed(record.getPrimary());
            if (inUsed > 0) {
                throw new DataSpecificationException("有 " + inUsed + " 条记录正在使用此流程，禁止修改");
            }
        }
        return super.update(record);
    }

    @Override
    public int delete(ID recordId) {
        int inUsed = ApprovalHelper.checkInUsed(recordId);
        if (inUsed > 0) {
            throw new DataSpecificationException("有 " + inUsed + " 条记录正在使用此流程，禁止删除");
        }
        return super.delete(recordId);
    }

    @Override
    protected void cleanCache(ID cfgid) {
        Object[] cfg = RebuildApplication.createQueryNoFilter(
                "select belongEntity from RobotApprovalConfig where configId = ?")
                .setParameter(1, cfgid)
                .unique();
        if (cfg != null) {
            Entity entity = MetadataHelper.getEntity((String) cfg[0]);
            RobotApprovalManager.instance.clean(entity);
        }
    }
}
