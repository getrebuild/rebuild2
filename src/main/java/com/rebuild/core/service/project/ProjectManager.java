/*
Copyright (c) REBUILD <https://getrebuild.com/> and its owners. All rights reserved.

rebuild is dual-licensed under commercial and open source licenses (GPLv3).
See LICENSE and COMMERCIAL in the project root for license information.
*/

package com.rebuild.core.service.project;

import cn.devezhao.bizz.security.AccessDeniedException;
import cn.devezhao.persist4j.engine.ID;
import com.alibaba.fastjson.JSON;
import com.alibaba.fastjson.JSONObject;
import com.rebuild.core.RebuildApplication;
import com.rebuild.core.configuration.ConfigBean;
import com.rebuild.core.configuration.ConfigManager;
import com.rebuild.core.configuration.ConfigurationException;
import com.rebuild.core.metadata.EntityHelper;
import com.rebuild.core.privileges.UserHelper;
import com.rebuild.utils.JSONUtils;
import org.apache.commons.lang.StringUtils;
import org.springframework.util.Assert;

import java.util.*;

/**
 * 项目管理
 *
 * @author devezhao
 * @since 2020/6/29
 */
public class ProjectManager implements ConfigManager {

    public static final ProjectManager instance = new ProjectManager();

    private ProjectManager() {
    }

    private static final String CKEY_PROJECTS = "ProjectManager";
    private static final String CKEY_PLAN = "ProjectPlan-";
    private static final String CKEY_TASK = "Task2Project-";

    /**
     * 获取指定用户可用项目
     *
     * @param user
     * @return
     */
    public ConfigBean[] getAvailable(ID user) {
        ConfigBean[] projects = getAllProjects();

        // TODO 管理员可见全部？

        List<ConfigBean> alist = new ArrayList<>();
        for (ConfigBean e : projects) {
            if (e.getInteger("scope") == ProjectConfigService.SCOPE_ALL
                    || e.get("members", Set.class).contains(user)) {
                alist.add(e.clone());
            }
        }
        return alist.toArray(new ConfigBean[0]);
    }

    /**
     * 获取全部项目
     *
     * @return
     */
    private ConfigBean[] getAllProjects() {
        ConfigBean[] projects = (ConfigBean[]) RebuildApplication.getCommonsCache().getx(CKEY_PROJECTS);

        if (projects == null) {
            Object[][] array = RebuildApplication.createQueryNoFilter(
                    "select configId,projectCode,projectName,iconName,scope,members,principal,extraDefinition from ProjectConfig")
                    .array();

            List<ConfigBean> alist = new ArrayList<>();
            for (Object[] o : array) {
                String members = (String) o[5];
                if (o[6] != null) {
                    members = StringUtils.isBlank(members) ? o[6].toString() : members + "," + o[6];
                }

                ConfigBean e = new ConfigBean()
                        .set("id", o[0])
                        .set("projectCode", o[1])
                        .set("projectName", o[2])
                        .set("iconName", StringUtils.defaultIfBlank((String) o[3], "texture"))
                        .set("scope", o[4])
                        .set("_members", members)
                        .set("principal", o[6]);

                // 扩展配置
                String extraDefinition = (String) o[7];
                if (JSONUtils.wellFormat(extraDefinition)) {
                    JSONObject extraDefinitionJson = JSON.parseObject(extraDefinition);
                    for (String name : extraDefinitionJson.keySet()) {
                        e.set(name, extraDefinitionJson.get(name));
                    }
                }
                alist.add(e);
            }

            projects = alist.toArray(new ConfigBean[0]);
            RebuildApplication.getCommonsCache().putx(CKEY_PROJECTS, projects);
        }

        for (ConfigBean p : projects) {
            Set<ID> members = Collections.emptySet();
            String userDefs = p.getString("_members");
            if (StringUtils.isNotBlank(userDefs) && userDefs.length() >= 20) {
                members = UserHelper.parseUsers(Arrays.asList(userDefs.split(",")), null);
            }
            p.set("members", members);
        }
        return projects;
    }

    /**
     * 获取指定项目
     *
     * @param projectId
     * @param user
     * @return
     * @throws ConfigurationException If not found
     */
    public ConfigBean getProject(ID projectId, ID user) throws ConfigurationException {
        ConfigBean[] ee = user == null ? getAllProjects() : getAvailable(user);
        for (ConfigBean e : ee) {
            if (projectId.equals(e.getID("id"))) {
                return e.clone();
            }
        }
        throw new ConfigurationException("无权访问该项目或项目已删除");
    }

    /**
     * @param taskId
     * @param user
     * @return
     * @throws ConfigurationException
     * @throws AccessDeniedException
     */
    public ConfigBean getProjectByTask(ID taskId, ID user) throws ConfigurationException, AccessDeniedException {
        final String ckey = CKEY_TASK + taskId;
        ID projectId = (ID) RebuildApplication.getCommonsCache().getx(ckey);

        if (projectId == null) {
            Object[] task = RebuildApplication.createQueryNoFilter(
                    "select projectId from ProjectTask where taskId = ?")
                    .setParameter(1, taskId)
                    .unique();

            projectId = task == null ? null : (ID) task[0];
            if (projectId != null) {
                RebuildApplication.getCommonsCache().putx(ckey, projectId);
            }
        }

        if (projectId == null) {
            throw new ConfigurationException("项目任务不存在或已被删除");
        }

        try {
            return getProject(projectId, user);
        } catch (ConfigurationException ex) {
            throw new AccessDeniedException("无权访问该项目任务", ex);
        }
    }

    /**
     * 获取项目的任务面板
     *
     * @param projectId
     * @return
     */
    public ConfigBean[] getPlansOfProject(ID projectId) {
        Assert.notNull(projectId, "[projectId] not be null");

        final String ckey = CKEY_PLAN + projectId;
        ConfigBean[] cache = (ConfigBean[]) RebuildApplication.getCommonsCache().getx(ckey);

        if (cache == null) {
            Object[][] array = RebuildApplication.createQueryNoFilter(
                    "select configId,planName,flowStatus,flowNexts from ProjectPlanConfig where projectId = ? order by seq")
                    .setParameter(1, projectId)
                    .array();

            List<ConfigBean> alist = new ArrayList<>();
            for (Object[] o : array) {
                ConfigBean e = new ConfigBean()
                        .set("id", o[0])
                        .set("planName", o[1])
                        .set("flowStatus", o[2]);

                if (StringUtils.isNotBlank((String) o[3])) {
                    List<ID> nexts = new ArrayList<>();
                    for (String s : ((String) o[3]).split(",")) {
                        nexts.add(ID.valueOf(s));
                    }
                    e.set("flowNexts", nexts);
                }
                alist.add(e);
            }

            cache = alist.toArray(new ConfigBean[0]);
            RebuildApplication.getCommonsCache().putx(ckey, cache);
        }
        return cache.clone();
    }

    /**
     * @param planId
     * @param projectId
     * @return
     */
    public ConfigBean getPlanOfProject(ID planId, ID projectId) {
        if (projectId == null) {
            Object[] o = RebuildApplication.getQueryFactory().uniqueNoFilter(planId, "projectId");
            projectId = o != null ? (ID) o[0] : null;
        }

        ConfigBean[] eee = getPlansOfProject(projectId);
        for (ConfigBean e : eee) {
            if (e.getID("id").equals(planId)) return e;
        }
        throw new ConfigurationException("无效任务面板 (" + planId + ")");
    }

    @Override
    public void clean(Object nullOrAnyProjectId) {
        int ec = nullOrAnyProjectId == null ? -1 : ((ID) nullOrAnyProjectId).getEntityCode();
        // 清理项目
        if (ec == -1) {
            RebuildApplication.getCommonsCache().evict(CKEY_PROJECTS);
        }
        // 清理面板
        else if (ec == EntityHelper.ProjectConfig) {
            RebuildApplication.getCommonsCache().evict(CKEY_PLAN + nullOrAnyProjectId);
        }
        // 清理任务
        else if (ec == EntityHelper.ProjectTask) {
            RebuildApplication.getCommonsCache().evict(CKEY_TASK + nullOrAnyProjectId);
        }
    }
}
