/*
Copyright (c) REBUILD <https://getrebuild.com/> and/or its owners. All rights reserved.

rebuild is dual-licensed under commercial and open source licenses (GPLv3).
See LICENSE and COMMERCIAL in the project root for license information.
*/

package com.rebuild.web.robot.trigger;

import cn.devezhao.persist4j.Entity;
import cn.devezhao.persist4j.engine.ID;
import com.rebuild.core.Application;
import com.rebuild.core.metadata.MetadataHelper;
import com.rebuild.core.metadata.MetadataSorter;
import com.rebuild.core.metadata.impl.EasyMeta;
import com.rebuild.core.service.trigger.ActionFactory;
import com.rebuild.core.service.trigger.ActionType;
import com.rebuild.core.service.trigger.TriggerAction;
import com.rebuild.utils.JSONUtils;
import com.rebuild.web.BaseController;
import com.rebuild.web.admin.data.ReportTemplateControl;
import org.apache.commons.lang.StringUtils;
import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.servlet.ModelAndView;

import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import java.io.IOException;
import java.util.ArrayList;
import java.util.List;

/**
 * @author devezhao zhaofang123@gmail.com
 * @since 2019/05/23
 */
@Controller
@RequestMapping("/admin/robot/")
public class RobotTriggerControl extends BaseController {

    @GetMapping("triggers")
    public ModelAndView pageList() {
        return createModelAndView("/admin/robot/trigger-list");
    }

    @GetMapping("trigger/{id}")
    public ModelAndView pageEditor(@PathVariable String id,
                                   HttpServletResponse response) throws IOException {
        ID configId = ID.valueOf(id);
        Object[] config = Application.createQuery(
                "select belongEntity,actionType,when,whenFilter,actionContent,priority,name,whenTimer from RobotTriggerConfig where configId = ?")
                .setParameter(1, configId)
                .unique();
        if (config == null) {
            response.sendError(404, "触发器不存在");
            return null;
        }

        Entity sourceEntity = MetadataHelper.getEntity((String) config[0]);
        ActionType actionType = ActionType.valueOf((String) config[1]);

        ModelAndView mv = createModelAndView("/admin/robot/trigger-design");
        mv.getModel().put("configId", configId);
        mv.getModel().put("sourceEntity", sourceEntity.getName());
        mv.getModel().put("sourceEntityLabel", EasyMeta.getLabel(sourceEntity));
        mv.getModel().put("actionType", actionType.name());
        mv.getModel().put("actionTypeLabel", actionType.getDisplayName());
        mv.getModel().put("when", config[2]);
        mv.getModel().put("whenTimer", config[7] == null ? StringUtils.EMPTY : config[7]);
        mv.getModel().put("whenFilter", StringUtils.defaultIfBlank((String) config[3], JSONUtils.EMPTY_OBJECT_STR));
        mv.getModel().put("actionContent", StringUtils.defaultIfBlank((String) config[4], JSONUtils.EMPTY_OBJECT_STR));
        mv.getModel().put("priority", config[5]);
        mv.getModel().put("name", config[6]);
        return mv;
    }

    @RequestMapping("trigger/available-actions")
    public void getAvailableActions(HttpServletResponse response) {
        ActionType[] ts = ActionFactory.getAvailableActions();
        List<String[]> list = new ArrayList<>();
        for (ActionType t : ts) {
            list.add(new String[]{t.name(), t.getDisplayName()});
        }
        writeSuccess(response, list);
    }

    @RequestMapping("trigger/available-entities")
    public void getAvailableEntities(HttpServletRequest request, HttpServletResponse response) {
        String actionType = getParameterNotNull(request, "action");
        TriggerAction action = ActionFactory.createAction(actionType);

        List<String[]> list = new ArrayList<>();
        for (Entity e : MetadataSorter.sortEntities()) {
            if (MetadataHelper.isBizzEntity(e.getEntityCode())) {
                continue;
            }

            if (action.isUsableSourceEntity(e.getEntityCode())) {
                list.add(new String[]{e.getName(), EasyMeta.getLabel(e)});
            }
        }
        writeSuccess(response, list);
    }

    @RequestMapping("trigger/list")
    public void triggerList(HttpServletRequest request, HttpServletResponse response) {
        String belongEntity = getParameter(request, "entity");
        String q = getParameter(request, "q");
        String sql = "select configId,belongEntity,belongEntity,name,isDisabled,modifiedOn,when,actionType from RobotTriggerConfig" +
                " where (1=1) and (2=2)" +
                " order by name, modifiedOn desc";

        Object[][] array = ReportTemplateControl.queryListOfConfig(sql, belongEntity, q);
        for (Object[] o : array) {
            o[7] = ActionType.valueOf((String) o[7]).getDisplayName();
        }
        writeSuccess(response, array);
    }
}
