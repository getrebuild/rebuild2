/*
rebuild - Building your business-systems freely.
Copyright (C) 2018-2019 devezhao <zhaofang123@gmail.com>

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

package com.rebuild.web.admin.bizz;

import cn.devezhao.bizz.security.member.Team;
import cn.devezhao.commons.web.ServletUtils;
import cn.devezhao.persist4j.engine.ID;
import com.alibaba.fastjson.JSON;
import com.alibaba.fastjson.JSONArray;
import com.rebuild.core.RebuildApplication;
import com.rebuild.core.configuration.general.DataListManager;
import com.rebuild.core.privileges.TeamService;
import com.rebuild.core.privileges.UserHelper;
import com.rebuild.core.privileges.bizz.User;
import com.rebuild.web.EntityController;
import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestMethod;
import org.springframework.web.servlet.ModelAndView;

import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import java.io.IOException;
import java.security.Principal;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.Set;

/**
 * @author devezhao
 * @since 2019/11/13
 */
@Controller
@RequestMapping("/admin/bizuser/")
public class TeamControll extends EntityController {

    @RequestMapping("teams")
    public ModelAndView pageList(HttpServletRequest request) throws IOException {
        ID user = getRequestUser(request);
        ModelAndView mv = createModelAndView("/admin/bizuser/team-list.jsp", "Team", user);
        JSON config = DataListManager.instance.getFieldsLayout("Team", user);
        mv.getModel().put("DataListConfig", JSON.toJSONString(config));
        return mv;
    }

    @RequestMapping(value = "team-members", method = RequestMethod.GET)
    public void getMembers(HttpServletRequest request, HttpServletResponse response) throws IOException {
        ID teamId = getIdParameterNotNull(request, "team");
        Team team = RebuildApplication.getUserStore().getTeam(teamId);

        List<Object[]> members = new ArrayList<>();
        for (Principal p : team.getMembers()) {
            User user = (User) p;
            members.add(new Object[]{
                    user.getId(), user.getFullName(),
                    user.getOwningDept() != null ? user.getOwningDept().getName() : null
            });
        }
        writeSuccess(response, members);
    }

    @RequestMapping(value = "team-members-add", method = RequestMethod.POST)
    public void addMembers(HttpServletRequest request, HttpServletResponse response) throws IOException {
        final ID user = getRequestUser(request);
        final ID teamId = getIdParameterNotNull(request, "team");

        JSON usersDef = ServletUtils.getRequestJson(request);
        Set<ID> users = UserHelper.parseUsers((JSONArray) usersDef, null);

        if (!users.isEmpty()) {
            RebuildApplication.getBean(TeamService.class).createMembers(teamId, users);
        }
        writeSuccess(response);
    }

    @RequestMapping(value = "team-members-del", method = RequestMethod.POST)
    public void deleteMembers(HttpServletRequest request, HttpServletResponse response) throws IOException {
        ID teamId = getIdParameterNotNull(request, "team");
        ID userId = getIdParameterNotNull(request, "user");

        RebuildApplication.getBean(TeamService.class).deleteMembers(teamId, Arrays.asList(userId));
        writeSuccess(response);
    }
}
