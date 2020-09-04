/*
Copyright (c) REBUILD <https://getrebuild.com/> and/or its owners. All rights reserved.

rebuild is dual-licensed under commercial and open source licenses (GPLv3).
See LICENSE and COMMERCIAL in the project root for license information.
*/

package com.rebuild.web.admin.bizz;

import cn.devezhao.bizz.security.member.Role;
import cn.devezhao.commons.ObjectUtils;
import cn.devezhao.commons.web.ServletUtils;
import cn.devezhao.persist4j.Record;
import cn.devezhao.persist4j.engine.ID;
import com.alibaba.fastjson.JSON;
import com.alibaba.fastjson.JSONObject;
import com.rebuild.core.Application;
import com.rebuild.core.configuration.general.DataListManager;
import com.rebuild.core.support.RebuildConfiguration;
import com.rebuild.core.support.SMSender;
import com.rebuild.core.metadata.EntityHelper;
import com.rebuild.core.privileges.UserService;
import com.rebuild.core.privileges.bizz.Department;
import com.rebuild.core.privileges.bizz.User;
import com.rebuild.utils.JSONUtils;
import com.rebuild.web.EntityController;
import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.servlet.ModelAndView;

import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import javax.servlet.http.HttpSession;
import java.util.HashMap;
import java.util.Map;

/**
 * @author devezhao
 * @since 10/08/2018
 */
@Controller
@RequestMapping("/admin/bizuser/")
public class UserControl extends EntityController {

    @GetMapping("users")
    public ModelAndView pageList(HttpServletRequest request) {
        ID user = getRequestUser(request);
        ModelAndView mv = createModelAndView("/admin/bizuser/user-list", "User", user);
        JSON config = DataListManager.instance.getFieldsLayout("User", user);
        mv.getModel().put("DataListConfig", JSON.toJSONString(config));
        return mv;
    }

    @RequestMapping("check-user-status")
    public void checkUserStatus(HttpServletRequest request, HttpServletResponse response) {
        ID id = getIdParameterNotNull(request, "id");
        if (!Application.getUserStore().existsUser(id)) {
            writeFailure(response);
            return;
        }

        User checkedUser = Application.getUserStore().getUser(id);

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

    @PostMapping("enable-user")
    public void enableUser(HttpServletRequest request, HttpServletResponse response) {
        JSONObject data = (JSONObject) ServletUtils.getRequestJson(request);

        ID user = ID.valueOf(data.getString("user"));
        User u = Application.getUserStore().getUser(user);
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

        Application.getBean(UserService.class).updateEnableUser(user, deptNew, roleNew, enableNew);

        // 是否需要发送激活通知
        u = Application.getUserStore().getUser(user);
        if (beforeDisabled && u.isActive() && SMSender.availableMail() && u.getEmail() != null) {
            Object did = Application.createQuery(
                    "select logId from LoginLog where user = ?")
                    .setParameter(1, u.getId())
                    .unique();
            if (did == null) {
                String homeUrl = RebuildConfiguration.getHomeUrl();
                String content = String.format(
                        "%s 你的账户已激活！现在你可以登陆并使用系统。登录地址 [%s](%s) <br>首次登陆，建议你立即修改密码！如有任何登陆或使用问题，请与系统管理员联系。",
                        u.getFullName(), homeUrl, homeUrl);
                SMSender.sendMailAsync(u.getEmail(), "你的账户已激活", content);
            }
        }

        // 登录失效
        if (!u.isActive()) {
            HttpSession s = Application.getSessionStore().getSession(u.getId());
            if (s != null) {
                LOG.warn("Force destroy user session : " + u.getId());
                s.invalidate();
            }
        }

        writeSuccess(response);
    }

    @RequestMapping("delete-checks")
    public void deleteChecks(HttpServletRequest request, HttpServletResponse response) {
        // 用户/部门/角色
        final ID bizz = getIdParameterNotNull(request, "id");

        int hasMember = 0;
        int hasChild = 0;
        if (bizz.getEntityCode() == EntityHelper.Department) {
            Department dept = Application.getUserStore().getDepartment(bizz);
            hasMember = dept.getMembers().size();
            hasChild = dept.getChildren().size();
        } else if (bizz.getEntityCode() == EntityHelper.Role) {
            Role role = Application.getUserStore().getRole(bizz);
            hasMember = role.getMembers().size();
        } else if (bizz.getEntityCode() == EntityHelper.User) {
            // 仅检查是否登陆过。严谨些还应该检查是否有其他业务数据
            Object[] hasLogin = Application.createQueryNoFilter(
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

    @PostMapping("user-delete")
    public void userDelete(HttpServletRequest request, HttpServletResponse response) {
        ID user = getIdParameterNotNull(request, "id");
        Application.getBean(UserService.class).delete(user);
        writeSuccess(response);
    }

    @PostMapping("user-resetpwd")
    public void userResetpwd(HttpServletRequest request, HttpServletResponse response) {
        ID user = getIdParameterNotNull(request, "id");
        String newp = getParameterNotNull(request, "newp");

        Record record = EntityHelper.forUpdate(user, user);
        record.setString("password", newp);
        Application.getBean(UserService.class).update(record);
        writeSuccess(response);
    }
}