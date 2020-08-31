/*
Copyright (c) REBUILD <https://getrebuild.com/> and/or its owners. All rights reserved.

rebuild is dual-licensed under commercial and open source licenses (GPLv3).
See LICENSE and COMMERCIAL in the project root for license information.
*/

package com.rebuild.web.commons;

import cn.devezhao.commons.web.ServletUtils;
import com.alibaba.fastjson.JSONArray;
import com.alibaba.fastjson.JSONObject;
import com.rebuild.core.Application;
import com.rebuild.core.ServerStatus;
import com.rebuild.core.ServerStatus.Status;
import com.rebuild.utils.JSONUtils;
import com.rebuild.web.BaseController;
import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.servlet.ModelAndView;

import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

/**
 * @author zhaofang123@gmail.com
 * @since 09/20/2018
 */
@Controller
public class CommonPageResolver extends BaseController {

    @GetMapping("/p/**")
    public ModelAndView page(HttpServletRequest request) {
        String p = request.getRequestURI();
        p = p.split("/p/")[1];
        return createModelAndView("/" + p);
    }

    @GetMapping("/gw/server-status")
    public ModelAndView pageServersStatus(HttpServletRequest request) {
        if ("1".equals(request.getParameter("check"))) {
            ServerStatus.checkAll();
        }

        ModelAndView mv = createModelAndView("/error/server-status");
        mv.getModel().put("ok", ServerStatus.isStatusOK() && Application.serversReady());
        mv.getModel().put("status", ServerStatus.getLastStatus());
        // Loads
        mv.getModel().put("MemoryUsage", ServerStatus.getHeapMemoryUsed());
        mv.getModel().put("SystemLoad", ServerStatus.getSystemLoad());
        return mv;
    }

    @GetMapping("/gw/server-status.json")
    public void apiServersStatus(HttpServletRequest request, HttpServletResponse response) {
        if ("1".equals(request.getParameter("check"))) {
            ServerStatus.checkAll();
        }

        JSONObject state = new JSONObject();
        state.put("ok", ServerStatus.isStatusOK());
        JSONArray stats = new JSONArray();
        state.put("status", stats);
        for (Status s : ServerStatus.getLastStatus()) {
            stats.add(s.toJson());
        }
        // Loads
        stats.add(JSONUtils.toJSONObject("MemoryUsage", ServerStatus.getHeapMemoryUsed()[1]));
        stats.add(JSONUtils.toJSONObject("SystemLoad", ServerStatus.getSystemLoad()));

        ServletUtils.writeJson(response, state.toJSONString());
    }
}