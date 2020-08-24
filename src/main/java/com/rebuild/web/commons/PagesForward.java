/*
rebuild - Building your business-systems freely.
Copyright (C) 2018 devezhao <zhaofang123@gmail.com>

This program is free software: you can redistribute it and/or modify
it under the terms of the GNU General Public License as published by
the Free Software Foundation, either version 3 of the License, or
(at your option) any later version.

This program is distributed in the hope that it will be useful,
but WITHOUT ANY WARRANTY; without even the implied warranty of
MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE. See the
GNU General Public License for more details.

You should have received a copy of the GNU General Public License
along with this program. If not, see <https://www.gnu.org/licenses/>.
*/

package com.rebuild.web.commons;

import cn.devezhao.commons.web.ServletUtils;
import com.alibaba.fastjson.JSONArray;
import com.alibaba.fastjson.JSONObject;
import com.rebuild.core.ServerStatus;
import com.rebuild.core.ServerStatus.Status;
import com.rebuild.utils.JSONUtils;
import com.rebuild.web.BaseController;
import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestMethod;
import org.springframework.web.servlet.ModelAndView;

import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

/**
 * @author zhaofang123@gmail.com
 * @since 09/20/2018
 */
@Controller
public class PagesForward extends BaseController {

    @RequestMapping(value = "/gw/server-status", method = RequestMethod.GET)
    public ModelAndView pageServersStatus(HttpServletRequest request) {
        if ("1".equals(request.getParameter("check"))) {
            ServerStatus.checkAll();
        }
        return createModelAndView("/server-status.jsp");
    }

    @RequestMapping(value = "/gw/server-status.json", method = RequestMethod.GET)
    public void apiServersStatus(HttpServletRequest request, HttpServletResponse response) {
        if ("1".equals(request.getParameter("check"))) {
            ServerStatus.checkAll();
        }

        JSONObject state = new JSONObject();
        state.put("ok", ServerStatus.isStatusOK());
        JSONArray statuses = new JSONArray();
        state.put("status", statuses);
        for (Status s : ServerStatus.getLastStatus()) {
            statuses.add(s.toJson());
        }
        statuses.add(JSONUtils.toJSONObject("MemoryUsage", ServerStatus.getHeapMemoryUsed()[1]));
        ServletUtils.writeJson(response, state.toJSONString());
    }

    @RequestMapping(value = {"/error/*"}, method = RequestMethod.GET)
    public ModelAndView pageError(HttpServletRequest request) {
        return createModelAndView("/error40x.jsp");
    }
}