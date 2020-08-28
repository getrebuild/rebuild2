/*
Copyright (c) REBUILD <https://getrebuild.com/> and/or its owners. All rights reserved.

rebuild is dual-licensed under commercial and open source licenses (GPLv3).
See LICENSE and COMMERCIAL in the project root for license information.
*/

package com.rebuild.web.account;

import cn.devezhao.commons.CalendarUtils;
import cn.devezhao.commons.EncryptUtils;
import cn.devezhao.persist4j.Record;
import cn.devezhao.persist4j.engine.ID;
import com.rebuild.core.Application;
import com.rebuild.core.helper.SMSender;
import com.rebuild.core.helper.VerfiyCode;
import com.rebuild.core.metadata.EntityHelper;
import com.rebuild.core.privileges.UserService;
import com.rebuild.web.EntityController;
import org.apache.commons.lang.StringUtils;
import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.servlet.ModelAndView;

import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import java.io.IOException;

/**
 * User account
 *
 * @author devezhao
 * @since 10/08/2018
 */
@RequestMapping("/account")
@Controller
public class AccountSettings extends EntityController {

    @RequestMapping("/settings")
    public ModelAndView pageView(HttpServletRequest request) throws IOException {
        return createModelAndView("/account/settings.jsp", "User", getRequestUser(request));
    }

    @RequestMapping("/settings/send-email-vcode")
    public void sendEmailVcode(HttpServletRequest request, HttpServletResponse response) throws IOException {
        String email = getParameterNotNull(request, "email");
        if (Application.getUserStore().existsEmail(email)) {
            writeFailure(response, "邮箱已被占用，请换用其他邮箱");
            return;
        }

        String vcode = VerfiyCode.generate(email);
        String content = "<p>你的邮箱验证码是 <b>" + vcode + "</b><p>";
        String sentid = SMSender.sendMail(email, "邮箱验证码", content);
        if (sentid != null) {
            writeSuccess(response);
        } else {
            writeFailure(response, "验证码发送失败，请稍后重试");
        }
    }

    @RequestMapping("/settings/save-email")
    public void saveEmail(HttpServletRequest request, HttpServletResponse response) throws IOException {
        ID user = getRequestUser(request);
        String email = getParameterNotNull(request, "email");
        String vcode = getParameterNotNull(request, "vcode");

        if (!VerfiyCode.verfiy(email, vcode)) {
            writeFailure(response, "验证码无效");
            return;
        }
        if (Application.getUserStore().existsEmail(email)) {
            writeFailure(response, "邮箱已被占用，请换用其他邮箱");
            return;
        }

        Record record = EntityHelper.forUpdate(user, user);
        record.setString("email", email);
        Application.getBean(UserService.class).update(record);
        writeSuccess(response);
    }

    @RequestMapping("/settings/save-passwd")
    public void savePasswd(HttpServletRequest request, HttpServletResponse response) throws IOException {
        ID user = getRequestUser(request);
        String oldp = getParameterNotNull(request, "oldp");
        String newp = getParameterNotNull(request, "newp");

        Object[] o = Application.createQuery("select password from User where userId = ?")
                .setParameter(1, user)
                .unique();
        if (o == null || !StringUtils.equals((String) o[0], EncryptUtils.toSHA256Hex(oldp))) {
            writeFailure(response, "原密码输入有误");
            return;
        }

        Record record = EntityHelper.forUpdate(user, user);
        record.setString("password", newp);
        Application.getBean(UserService.class).update(record);
        writeSuccess(response);
    }

    @RequestMapping("/settings/login-logs")
    public void loginLogs(HttpServletRequest request, HttpServletResponse response) throws IOException {
        ID user = getRequestUser(request);
        Object[][] logs = Application.createQueryNoFilter(
                "select loginTime,userAgent,ipAddr,logoutTime from LoginLog where user = ? order by loginTime desc")
                .setParameter(1, user)
                .setLimit(100)
                .array();
        for (Object[] o : logs) {
            o[0] = CalendarUtils.getUTCDateTimeFormat().format(o[0]);
            if (o[3] != null) {
                o[3] = CalendarUtils.getUTCDateTimeFormat().format(o[3]);
            }
        }
        writeSuccess(response, logs);
    }
}
