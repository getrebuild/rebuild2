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
import com.rebuild.core.helper.language.Languages;
import com.rebuild.core.metadata.EntityHelper;
import com.rebuild.core.privileges.UserService;
import com.rebuild.core.service.DataSpecificationException;
import com.rebuild.web.BaseController;
import com.wf.captcha.utils.CaptchaUtil;
import org.apache.commons.lang.math.RandomUtils;
import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.servlet.ModelAndView;

import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import java.awt.*;
import java.io.IOException;

import static com.rebuild.core.helper.language.Languages.lang;

/**
 * 用户自助注册
 *
 * @author devezhao
 * @since 11/01/2018
 */
@Controller
@RequestMapping("/user/")
public class SignUp extends BaseController {

    @RequestMapping("signup")
    public ModelAndView pageSignup(HttpServletResponse response) throws IOException {
        if (!RebuildConfiguration.getBool(ConfigurableItem.OpenSignUp)) {
            response.sendError(400, lang("SignupNotOpenTip"));
            return null;
        }
        return createModelAndView("/user/signup.jsp");
    }

    @RequestMapping("signup-email-vcode")
    public void signupEmailVcode(HttpServletRequest request, HttpServletResponse response) {
        if (!SMSender.availableMail()) {
            writeFailure(response, lang("EmailAccountUnset"));
            return;
        }

        String email = getParameterNotNull(request, "email");

        if (!RegexUtils.isEMail(email)) {
            writeFailure(response, lang("InputInvalid", "Email"));
            return;
        } else if (RebuildApplication.getUserStore().existsEmail(email)) {
            writeFailure(response, lang("InputExists", "Email"));
            return;
        }

        String vcode = VCode.generate(email, 1);
        String content = String.format(lang("YourVcodeForSignup"), vcode);
        String sentid = SMSender.sendMail(email, lang("SignupVcode"), content);
        LOG.warn(email + " >> " + content);
        if (sentid != null) {
            writeSuccess(response);
        } else {
            writeFailure(response);
        }
    }

    @SuppressWarnings("DuplicatedCode")
    @RequestMapping("signup-confirm")
    public void signupConfirm(HttpServletRequest request, HttpServletResponse response) throws IOException {
        JSONObject data = (JSONObject) ServletUtils.getRequestJson(request);
        String email = data.getString("email");
        String vcode = data.getString("vcode");
        if (!VCode.verfiy(email, vcode, true)) {
            writeFailure(response, lang("InputInvalid", "Vcode"));
            return;
        }

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
            String content = Languages.currentBundle().formatLang("SignupPending",
                    fullName, loginName, passwd, homeUrl, homeUrl);
            SMSender.sendMail(email, lang("AdminReviewSignup"), content);
            writeSuccess(response);
        } catch (DataSpecificationException ex) {
            writeFailure(response, ex.getLocalizedMessage());
        }
    }

    @RequestMapping("checkout-name")
    public void checkoutName(HttpServletRequest request, HttpServletResponse response) throws IOException {
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

    @RequestMapping("captcha")
    public void captcha(HttpServletRequest request, HttpServletResponse response) throws IOException {
        Font font = new Font(Font.SERIF, Font.BOLD & Font.ITALIC, 22 + RandomUtils.nextInt(8));
        int codeLen = 4 + RandomUtils.nextInt(3);
        CaptchaUtil.out(160, 41, codeLen, font, request, response);
    }
}