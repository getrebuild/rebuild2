/*
Copyright (c) REBUILD <https://getrebuild.com/> and/or its owners. All rights reserved.

rebuild is dual-licensed under commercial and open source licenses (GPLv3).
See LICENSE and COMMERCIAL in the project root for license information.
*/

package com.rebuild.core.service.project;

import cn.devezhao.persist4j.Record;
import cn.devezhao.persist4j.engine.ID;
import com.rebuild.TestSupport;
import com.rebuild.core.Application;
import com.rebuild.core.configuration.ConfigBean;
import com.rebuild.core.metadata.EntityHelper;
import com.rebuild.core.privileges.UserService;
import org.junit.Assert;
import org.junit.BeforeClass;
import org.junit.Test;

/**
 * @author devezhao
 * @since 2020/7/28
 */
public class ProjectManagerTest extends TestSupport {

    static {
        Application.getSessionStore().set(UserService.ADMIN_USER);
    }

    private static final String USE_CODE = "RBTEST";

    private static ID _LastSavedProject;

    @BeforeClass
    public static void createNewProject() {
        ProjectConfigService pcs = (ProjectConfigService) Application.getService(EntityHelper.ProjectConfig);

        Object[] exists = Application.createQueryNoFilter(
                "select configId from ProjectConfig where projectCode = ?")
                .setParameter(1, USE_CODE)
                .unique();
        if (exists == null) {
            Record pc = EntityHelper.forNew(EntityHelper.ProjectConfig, UserService.ADMIN_USER);
            pc.setString("projectName", "TEST PROJECT");
            pc.setString("projectCode", USE_CODE);
            pc.setString("members", SIMPLE_USER + "," + UserService.ADMIN_USER);

            Application.getSessionStore().set(UserService.ADMIN_USER);
            try {
                pc = pcs.createProject(pc, 1);
                _LastSavedProject = pc.getPrimary();
            } finally {
                Application.getSessionStore().clean();
            }
        } else {
            _LastSavedProject = (ID) exists[0];
        }
    }

    @Test
    public void testGetProject() {
        ConfigBean[] uses = ProjectManager.instance.getAvailable(SIMPLE_USER);
        Assert.assertTrue(uses.length > 0);

        ProjectManager.instance.getProject(uses[0].getID("id"), null);
    }

    @Test
    public void testGetPlansOfProject() {
        ConfigBean[] plans = ProjectManager.instance.getPlansOfProject(_LastSavedProject);
        Assert.assertTrue(plans.length > 0);
    }

    @Test
    public void testCreateTask() {
        ConfigBean[] plans = ProjectManager.instance.getPlansOfProject(_LastSavedProject);

        Record task = EntityHelper.forNew(EntityHelper.ProjectTask, SIMPLE_USER);
        task.setID("projectId", _LastSavedProject);
        task.setID("projectPlanId", plans[0].getID("id"));
        task.setString("taskName", "任务" + System.currentTimeMillis());

        task = Application.getBean(ProjectTaskService.class).create(task);
        System.out.println("New task created : " + task.getPrimary());

        Assert.assertTrue(ProjectHelper.checkReadable(task.getPrimary(), SIMPLE_USER));
        Assert.assertTrue(ProjectHelper.isManageable(task.getPrimary(), SIMPLE_USER));

        // DELETE
        Application.getBean(ProjectTaskService.class).delete(task.getPrimary());
    }
}