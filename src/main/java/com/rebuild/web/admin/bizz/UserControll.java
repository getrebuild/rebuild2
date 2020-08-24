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

package com.rebuild.web.admin.bizz;

import cn.devezhao.bizz.security.member.Role;
import cn.devezhao.commons.ObjectUtils;
import cn.devezhao.commons.web.ServletUtils;
import cn.devezhao.persist4j.Record;
import cn.devezhao.persist4j.engine.ID;
import com.alibaba.fastjson.JSON;
import com.alibaba.fastjson.JSONObject;
import com.rebuild.core.RebuildApplication;
import com.rebuild.core.configuration.general.DataListManager;
import com.rebuild.core.helper.RebuildConfiguration;
import com.rebuild.core.helper.SMSender;
import com.rebuild.core.helper.language.Languages;
import com.rebuild.core.metadata.EntityHelper;
import com.rebuild.core.privileges.UserService;
import com.rebuild.core.privileges.bizz.Department;
import com.rebuild.core.privileges.bizz.User;
import com.rebuild.utils.JSONUtils;
import com.rebuild.web.EntityController;
import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestMethod;
import org.springframework.web.servlet.ModelAndView;

import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import javax.servlet.http.HttpSession;
import java.io.IOException;
import java.util.HashMap;
import java.util.Map;

/**
 * @author devezhao
 * @since 10/08/2018
 */
@Controller
@RequestMapping("/admin/bizuser/")
public class UserControll extends EntityController {

    @RequestMapping("users")
    public ModelAndView pageList(HttpServletRequest request) throws IOException {
        ID user = getRequestUser(request);
        ModelAndView mv = createModelAndView("/admin/bizuser/user-list.jsp", "User", user);
        JSON config = DataListManager.instance.getFieldsLayout("User", user);
        mv.getModel().put("DataListConfig", JSON.toJSONString(config));
        return mv;
    }

    @RequestMapping("check-user-status")
    public void checkUserStatus(HttpServletRequest request, HttpServletResponse response) throws IOException {
        ID id = getIdParameterNotNull(request, "id");
        if (!RebuildApplication.getUserStore().existsUser(id)) {
            writeFailure(response);
            return;
        }

        User checkedUser = RebuildApplication.getUserStore().getUser(id);

        Map<String, Object> ret = new HashMap<>();
        ret.put("active", checkedUser.isActive());
        ret.put("system", "system".equals(checkedUser.getName()) || "admin".equals(checkedUser.getName()));

        ret.put("disabled", checkedUser.isDisabled());
        if (checkedUser.getOwningRole() != null) {
            ret.put("role", checkedUser.getOwningRole().getIdentity());
            ret.put("roleDisabled", checkedUser.getOwningRole().isDisabled());
        }
        if (checkedUser.getOwningDept() != null) {
            ret.put("dept", checkedUser.getOwningDept().getIdentity());
            ret.put("deptDisabled", checkedUser.getOwningDept().isDisabled());
        }

        writeSuccess(response, ret);
    }

    @RequestMapping("enable-user")
    public void enableUser(HttpServletRequest request, HttpServletResponse response) throws IOException {
        JSONObject data = (JSONObject) ServletUtils.getRequestJson(request);

        ID user = ID.valueOf(data.getString("user"));
        User u = RebuildApplication.getUserStore().getUser(user);
        final boolean beforeDisabled = u.isDisabled();

        ID deptNew = null;
        ID roleNew = null;
        if (data.containsKey("dept")) {
            deptNew = ID.valueOf(data.getString("dept"));
            if (u.getOwningDept() != null && u.getOwningDept().getIdentity().equals(deptNew)) {
                deptNew = null;
            }
        }
        if (data.containsKey("role")) {
            roleNew = ID.valueOf(data.getString("role"));
            if (u.getOwningRole() != null && u.getOwningRole().getIdentity().equals(roleNew)) {
                roleNew = null;
            }
        }

        Boolean enableNew = null;
        if (data.containsKey("enable")) {
            enableNew = data.getBoolean("enable");
        }

        RebuildApplication.getBean(UserService.class).updateEnableUser(user, deptNew, roleNew, enableNew);

        // 是否需要发送激活通知
        u = RebuildApplication.getUserStore().getUser(user);
        if (beforeDisabled && u.isActive() && SMSender.availableMail() && u.getEmail() != null) {
            Object did = RebuildApplication.createQuery(
                    "select logId from LoginLog where user = ?")
                    .setParameter(1, u.getId())
                    .unique();
            if (did == null) {
                String homeUrl = RebuildConfiguration.getHomeUrl();
                String content = Languages.defaultBundle().formatLang("NewUserAccountActive",
                        u.getFullName(), homeUrl, homeUrl);
                SMSender.sendMailAsync(u.getEmail(),
                        Languages.defaultBundle().lang("YourAccountActive"), content);
            }
        }

        // 登录失效
        if (!u.isActive()) {
            HttpSession s = RebuildApplication.getSessionStore().getSession(u.getId());
            if (s != null) {
                LOG.warn("Force destroy user session : " + u.getId());
                s.invalidate();
            }
        }

        writeSuccess(response);
    }

    @RequestMapping("delete-checks")
    public void deleteChecks(HttpServletRequest request, HttpServletResponse response) throws IOException {
        // 用户/部门/角色
        final ID bizz = getIdParameterNotNull(request, "id");

        int hasMember = 0;
        int hasChild = 0;
        if (bizz.getEntityCode() == EntityHelper.Department) {
            Department dept = RebuildApplication.getUserStore().getDepartment(bizz);
            hasMember = dept.getMembers().size();
            hasChild = dept.getChildren().size();
        } else if (bizz.getEntityCode() == EntityHelper.Role) {
            Role role = RebuildApplication.getUserStore().getRole(bizz);
            hasMember = role.getMembers().size();
        } else if (bizz.getEntityCode() == EntityHelper.User) {
            // 仅检查是否登陆过。严谨些还应该检查是否有其他业务数据
            Object[] hasLogin = RebuildApplication.createQueryNoFilter(
                    "select count(logId) from LoginLog where user = ?")
                    .setParameter(1, bizz)
                    .unique();
            hasMember = ObjectUtils.toInt(hasLogin[0]);
        }

        JSONObject ret = JSONUtils.toJSONObject(
                new String[]{"hasMember", "hasChild"},
                new Object[]{hasMember, hasChild});
        writeSuccess(response, ret);
    }

    @RequestMapping(value = "user-delete", method = RequestMethod.POST)
    public void userDelete(HttpServletRequest request, HttpServletResponse response) throws IOException {
        ID user = getIdParameterNotNull(request, "id");
        RebuildApplication.getBean(UserService.class).delete(user);
        writeSuccess(response);
    }

    @RequestMapping(value = "user-resetpwd", method = RequestMethod.POST)
    public void userResetpwd(HttpServletRequest request, HttpServletResponse response) throws IOException {
        ID user = getIdParameterNotNull(request, "id");
        String newp = getParameterNotNull(request, "newp");

        Record record = EntityHelper.forUpdate(user, user);
        record.setString("password", newp);
        RebuildApplication.getBean(UserService.class).update(record);
        writeSuccess(response);
    }
}