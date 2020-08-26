/*
Copyright (c) REBUILD <https://getrebuild.com/> and/or its owners. All rights reserved.

rebuild is dual-licensed under commercial and open source licenses (GPLv3).
See LICENSE and COMMERCIAL in the project root for license information.
*/

package com.rebuild.web.general;

import cn.devezhao.commons.web.ServletUtils;
import cn.devezhao.persist4j.Entity;
import cn.devezhao.persist4j.engine.ID;
import com.alibaba.fastjson.JSON;
import com.alibaba.fastjson.JSONObject;
import com.rebuild.core.RebuildApplication;
import com.rebuild.core.configuration.general.DataListManager;
import com.rebuild.core.helper.general.DataListControl;
import com.rebuild.core.helper.general.DefaultDataListControl;
import com.rebuild.core.metadata.MetadataHelper;
import com.rebuild.core.privileges.bizz.ZeroEntry;
import com.rebuild.web.EntityController;
import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.servlet.ModelAndView;

import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import java.io.IOException;

/**
 * 数据列表
 *
 * @author zhaofang123@gmail.com
 * @since 08/22/2018
 */
@Controller
@RequestMapping("/app/")
public class GeneralDataListControll extends EntityController {

    @RequestMapping("{entity}/list")
    public ModelAndView pageList(@PathVariable String entity,
                                 HttpServletRequest request, HttpServletResponse response) throws IOException {
        final ID user = getRequestUser(request);
        if (!MetadataHelper.containsEntity(entity) || MetadataHelper.isBizzEntity(entity)) {
            response.sendError(404);
            return null;
        }

        final Entity thatEntity = MetadataHelper.getEntity(entity);

        if (!thatEntity.isQueryable()) {
            response.sendError(404);
            return null;
        }

        if (!RebuildApplication.getPrivilegesManager().allowRead(user, thatEntity.getEntityCode())) {
            response.sendError(403, "你没有访问此实体的权限");
            return null;
        }

        ModelAndView mv;
        if (thatEntity.getMasterEntity() != null) {
            mv = createModelAndView("/general-entity/slave-list.jsp", entity, user);
        } else {
            mv = createModelAndView("/general-entity/record-list.jsp", entity, user);
        }

        JSON config = DataListManager.instance.getFieldsLayout(entity, user);
        mv.getModel().put("DataListConfig", JSON.toJSONString(config));

        // 列表相关权限
        mv.getModel().put(ZeroEntry.AllowCustomDataList.name(),
                RebuildApplication.getPrivilegesManager().allow(user, ZeroEntry.AllowCustomDataList));
        mv.getModel().put(ZeroEntry.AllowDataExport.name(),
                RebuildApplication.getPrivilegesManager().allow(user, ZeroEntry.AllowDataExport));
        mv.getModel().put(ZeroEntry.AllowBatchUpdate.name(),
                RebuildApplication.getPrivilegesManager().allow(user, ZeroEntry.AllowBatchUpdate));

        // 展开 WIDGET 面板
        String asideCollapsed = ServletUtils.readCookie(request, "rb.asideCollapsed");
        if (!"false".equals(asideCollapsed)) {
            mv.getModel().put("asideCollapsed", true);
        }

        return mv;
    }

    @RequestMapping("{entity}/data-list")
    public void dataList(@PathVariable String entity,
                         HttpServletRequest request, HttpServletResponse response) throws IOException {
        JSONObject query = (JSONObject) ServletUtils.getRequestJson(request);

        DataListControl control = new DefaultDataListControl(query, getRequestUser(request));
        if ("TheSpecEntity".equalsIgnoreCase(entity)) {
            // Use spec
        }

        JSON result = control.getJSONResult();
        writeSuccess(response, result);
    }
}
