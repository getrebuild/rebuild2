/*
Copyright (c) REBUILD <https://getrebuild.com/> and its owners. All rights reserved.

rebuild is dual-licensed under commercial and open source licenses (GPLv3).
See LICENSE and COMMERCIAL in the project root for license information.
*/

package com.rebuild.core.service.project;

import cn.devezhao.persist4j.PersistManagerFactory;
import cn.devezhao.persist4j.Record;
import cn.devezhao.persist4j.engine.ID;
import com.rebuild.core.RebuildApplication;
import com.rebuild.core.configuration.BaseConfigurationService;
import com.rebuild.core.metadata.EntityHelper;
import com.rebuild.core.privileges.AdminGuard;
import com.rebuild.core.service.DataSpecificationException;
import org.apache.commons.lang.StringUtils;
import org.springframework.stereotype.Service;

/**
 * 项目管理
 *
 * @author devezhao
 * @since 2020/6/30
 */
@Service
public class ProjectConfigService extends BaseConfigurationService implements AdminGuard {

    /**
     * 项目范围-公开
     */
    public static final int SCOPE_ALL = 1;
    /**
     * 项目范围-私有（成员）
     */
    public static final int SCOPE_MEMBER = 2;
    /**
     * 模板-基础模板
     */
    public static final int TEMPLATE_DEFAULT = 1;

    protected ProjectConfigService(PersistManagerFactory aPMFactory) {
        super(aPMFactory);
    }

    @Override
    public int getEntityCode() {
        return EntityHelper.ProjectConfig;
    }

    @Override
    public int delete(ID projectId) {
        Object[] count = RebuildApplication.createQuery(
                "select count(taskId) from ProjectTask where projectId = ?")
                .setParameter(1, projectId)
                .unique();
        if ((Long) count[0] > 0) {
            throw new DataSpecificationException("项目下有 " + count[0] + " 个任务，无法删除");
        }
        return super.delete(projectId);
    }

    @Override
    protected void cleanCache(ID cfgid) {
        ProjectManager.instance.clean(null);
    }

    /**
     * @param project
     * @param useTemplate
     */
    public Record createProject(Record project, int useTemplate) {
        project = super.createOnly(project);

        // 使用模板
        if (useTemplate == TEMPLATE_DEFAULT) {
            ID id1 = createPlan(project.getPrimary(), "待处理", 1000, ProjectPlanConfigService.FLOW_STATUS_START, null);
            ID id2 = createPlan(project.getPrimary(), "进行中", 2000, ProjectPlanConfigService.FLOW_STATUS_PROCESSING, null);
            ID id3 = createPlan(project.getPrimary(), "已完成", 3000, ProjectPlanConfigService.FLOW_STATUS_END, new ID[]{id1, id2});
            updateFlowNexts(id1, new ID[]{id2, id3});
            updateFlowNexts(id2, new ID[]{id1, id3});
        }

        this.cleanCache(null);
        this.cleanCache(project.getPrimary());
        return project;
    }

    /**
     * @see ProjectPlanConfigService
     */
    private ID createPlan(ID projectId, String planName, int seq, int flowStatus, ID[] flowNexts) {
        Record plan = EntityHelper.forNew(EntityHelper.ProjectPlanConfig, RebuildApplication.getCurrentUser());
        plan.setID("projectId", projectId);
        plan.setString("planName", planName);
        plan.setInt("seq", seq);
        plan.setInt("flowStatus", flowStatus);
        if (flowNexts != null) {
            plan.setString("flowNexts", StringUtils.join(flowNexts, ","));
        }
        return super.createOnly(plan).getPrimary();
    }

    private void updateFlowNexts(ID planId, ID[] flowNexts) {
        Record plan = EntityHelper.forUpdate(planId, RebuildApplication.getCurrentUser(), false);
        plan.setString("flowNexts", StringUtils.join(flowNexts, ","));
        super.updateOnly(plan);
    }
}