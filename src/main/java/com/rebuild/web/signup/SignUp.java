/*
Copyright (c) REBUILD <https://getrebuild.com/> and its owners. All rights reserved.

rebuild is dual-licensed under commercial and open source licenses (GPLv3).
See LICENSE and COMMERCIAL in the project root for license information.
*/

package com.rebuild.web.signup;

import cn.devezhao.commons.RegexUtils;
import cn.devezhao.commons.web.ServletUtils;
import cn.devezhao.persist4j.Record;
import com.alibaba.fastjson.JSONObject;
import com.hankcs.hanlp.HanLP;
import com.rebuild.core.RebuildApplication;
import com.rebuild.core.helper.*;
import com.rebuild.core.metadata.EntityHelper;
import com.rebuild.core.privileges.UserService;
import com.rebuild.core.service.DataSpecificationException;
import com.rebuild.web.BaseController;
import com.wf.captcha.utils.CaptchaUtil;
import org.apache.commons.lang.math.RandomUtils;
import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.servlet.ModelAndView;

import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import java.awt.*;
import java.io.IOException;

/**
 * 用户自助注册
 *
 * @author devezhao
 * @since 11/01/2018
 */
@Controller
@RequestMapping("/user/")
public class SignUp extends BaseController {

    @GetMapping("signup")
    public ModelAndView pageSignup(HttpServletResponse response) throws IOException {
        if (!RebuildConfiguration.getBool(ConfigurableItem.OpenSignUp)) {
            response.sendError(400, "管理员未开放公开注册");
            return null;
        }
        return createModelAndView("/signup/signup.html");
    }

    @PostMapping("signup-email-vcode")
    public void signupEmailVcode(HttpServletRequest request, HttpServletResponse response) {
        if (!SMSender.availableMail()) {
            writeFailure(response, "邮件服务账户未配置，请联系管理员配置");
            return;
        }

        String email = getParameterNotNull(request, "email");

        if (!RegexUtils.isEMail(email)) {
            writeFailure(response, "邮箱无效");
            return;
        } else if (RebuildApplication.getUserStore().existsEmail(email)) {
            writeFailure(response, "邮箱已存在");
            return;
        }

        String vcode = VCode.generate(email, 1);
        String content = "你的注册邮箱验证码是：" + vcode;
        String sentid = SMSender.sendMail(email, "注册验证码", content);
        LOG.warn(email + " >> " + content);
        if (sentid != null) {
            writeSuccess(response);
        } else {
            writeFailure(response);
        }
    }

    @PostMapping("signup-confirm")
    public void signupConfirm(HttpServletRequest request, HttpServletResponse response) {
        JSONObject data = (JSONObject) ServletUtils.getRequestJson(request);
        String hasError = checkVCode(data);
        if (hasError != null) {
            writeFailure(response, hasError);
            return;
        }

        String email = data.getString("email");
        String loginName = data.getString("loginName");
        String fullName = data.getString("fullName");
        String passwd = VCode.generate(loginName, 2) + "!8";
        VCode.clean(loginName);

        Record userNew = EntityHelper.forNew(EntityHelper.User, UserService.SYSTEM_USER);
        userNew.setString("email", email);
        userNew.setString("loginName", loginName);
        userNew.setString("fullName", fullName);
        userNew.setString("password", passwd);
        userNew.setBoolean("isDisabled", true);
        try {
            RebuildApplication.getBean(UserService.class).txSignUp(userNew);

            String homeUrl = RebuildConfiguration.getHomeUrl();
            String content = String.format(
                    "%s 欢迎注册！以下为你的登录信息，请妥善保管。<br><br>登录账号：%s <br>登录密码：%s <br>登录地址：[%s](%s) <br><br>目前你还无法登录系统，因为系统管理员正在审核你的注册信息。完成后会通过邮件通知你，请耐心等待。",
                    fullName, loginName, passwd, homeUrl, homeUrl);
            SMSender.sendMail(email, "管理员正在审核你的注册信息", content);
            writeSuccess(response);
        } catch (DataSpecificationException ex) {
            writeFailure(response, ex.getLocalizedMessage());
        }
    }

    @RequestMapping("checkout-name")
    public void checkoutName(HttpServletRequest request, HttpServletResponse response) {
        String fullName = getParameterNotNull(request, "fullName");

        fullName = fullName.replaceAll("[^a-zA-Z0-9\u4e00-\u9fa5]", "");
        String loginName = HanLP.convertToPinyinString(fullName, "", false);
        if (loginName.length() > 20) {
            loginName = loginName.substring(0, 20);
        }
        if (BlackList.isBlack(loginName)) {
            writeSuccess(response);
            return;
        }

        for (int i = 0; i < 100; i++) {
            if (RebuildApplication.getUserStore().existsName(loginName)) {
                loginName += RandomUtils.nextInt(99);
            } else {
                break;
            }
        }

        loginName = loginName.toLowerCase();
        writeSuccess(response, loginName);
    }

    @GetMapping("captcha")
    public void captcha(HttpServletRequest request, HttpServletResponse response) throws IOException {
        Font font = new Font(Font.SERIF, Font.BOLD & Font.ITALIC, 22 + RandomUtils.nextInt(8));
        int codeLen = 4 + RandomUtils.nextInt(3);
        CaptchaUtil.out(160, 41, codeLen, font, request, response);
    }

    static String checkVCode(JSONObject data) {
        String email = data.getString("email");
        String vcode = data.getString("vcode");
        if (!VCode.verfiy(email, vcode, true)) {
            return "验证码无效";
        }
        return null;
    }
}