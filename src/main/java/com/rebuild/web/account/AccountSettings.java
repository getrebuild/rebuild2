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

package com.rebuild.web.account;

import cn.devezhao.commons.CalendarUtils;
import cn.devezhao.commons.EncryptUtils;
import cn.devezhao.persist4j.Record;
import cn.devezhao.persist4j.engine.ID;
import com.rebuild.core.RebuildApplication;
import com.rebuild.core.helper.SMSender;
import com.rebuild.core.helper.VCode;
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
        if (RebuildApplication.getUserStore().existsEmail(email)) {
            writeFailure(response, "邮箱已被占用，请换用其他邮箱");
            return;
        }

        String vcode = VCode.generate(email);
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

        if (!VCode.verfiy(email, vcode)) {
            writeFailure(response, "验证码无效");
            return;
        }
        if (RebuildApplication.getUserStore().existsEmail(email)) {
            writeFailure(response, "邮箱已被占用，请换用其他邮箱");
            return;
        }

        Record record = EntityHelper.forUpdate(user, user);
        record.setString("email", email);
        RebuildApplication.getBean(UserService.class).update(record);
        writeSuccess(response);
    }

    @RequestMapping("/settings/save-passwd")
    public void savePasswd(HttpServletRequest request, HttpServletResponse response) throws IOException {
        ID user = getRequestUser(request);
        String oldp = getParameterNotNull(request, "oldp");
        String newp = getParameterNotNull(request, "newp");

        Object[] o = RebuildApplication.createQuery("select password from User where userId = ?")
                .setParameter(1, user)
                .unique();
        if (o == null || !StringUtils.equals((String) o[0], EncryptUtils.toSHA256Hex(oldp))) {
            writeFailure(response, "原密码输入有误");
            return;
        }

        Record record = EntityHelper.forUpdate(user, user);
        record.setString("password", newp);
        RebuildApplication.getBean(UserService.class).update(record);
        writeSuccess(response);
    }

    @RequestMapping("/settings/login-logs")
    public void loginLogs(HttpServletRequest request, HttpServletResponse response) throws IOException {
        ID user = getRequestUser(request);
        Object[][] logs = RebuildApplication.createQueryNoFilter(
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