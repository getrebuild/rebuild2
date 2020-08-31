/*
Copyright (c) REBUILD <https://getrebuild.com/> and/or its owners. All rights reserved.

rebuild is dual-licensed under commercial and open source licenses (GPLv3).
See LICENSE and COMMERCIAL in the project root for license information.
*/

package com.rebuild.web;

import cn.devezhao.commons.web.ServletUtils;
import cn.devezhao.persist4j.engine.ID;
import com.alibaba.fastjson.JSON;
import com.alibaba.fastjson.serializer.SerializerFeature;
import com.rebuild.api.Controller;
import com.rebuild.core.Application;
import com.rebuild.core.helper.i18n.Language;
import com.rebuild.core.helper.i18n.LanguageBundle;
import com.rebuild.utils.AppUtils;
import org.apache.commons.lang.BooleanUtils;
import org.apache.commons.lang.StringUtils;
import org.apache.commons.lang.math.NumberUtils;
import org.springframework.web.servlet.ModelAndView;

import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

/**
 * 基础 Controller
 *
 * @author zhaofang123@gmail.com
 * @since 05/21/2017
 */
public abstract class BaseController extends Controller {

    /**
     * @param request
     * @return
     */
    protected ID getRequestUser(HttpServletRequest request) {
        ID user = AppUtils.getRequestUser(request);
        if (user == null) {
            user = AppUtils.getRequestUserViaRbMobile(request, false);
        }

        if (user == null) {
            throw new InvalidParameterException("无效请求用户");
        }
        return user;
    }

    /**
     * @param request
     * @param key
     * @param phKey
     * @return
     */
    protected String getLang(HttpServletRequest request, String key, String...phKey) {
        String locale = (String) ServletUtils.getSessionAttribute(request, AppUtils.SK_LOCALE);
        LanguageBundle bundle = Application.getBean(Language.class).getBundle(locale);
        return bundle.getLang(key, phKey);
    }

    /**
     * @param response
     */
    protected void writeSuccess(HttpServletResponse response) {
        writeSuccess(response, null);
    }

    /**
     * @param response
     * @param data
     */
    protected void writeSuccess(HttpServletResponse response, Object data) {
        writeJSON(response, formatSuccess(data));
    }

    /**
     * @param response
     */
    protected void writeFailure(HttpServletResponse response) {
        writeFailure(response, null);
    }

    /**
     * @param response
     * @param message
     */
    protected void writeFailure(HttpServletResponse response, String message) {
        writeJSON(response, formatFailure(message));
    }

    /**
     * @param response
     * @param aJson
     */
    protected void writeJSON(HttpServletResponse response, Object aJson) {
        if (aJson == null) {
            throw new IllegalArgumentException();
        }

        String aJsonString;
        if (aJson instanceof String) {
            aJsonString = (String) aJson;
        } else {
            // fix: $ref.xxx
            aJsonString = JSON.toJSONString(aJson,
                    SerializerFeature.DisableCircularReferenceDetect, SerializerFeature.WriteMapNullValue);
        }
        ServletUtils.writeJson(response, aJsonString);
    }

    /**
     * @param req
     * @param name
     * @return
     */
    protected String getParameter(HttpServletRequest req, String name) {
        return req.getParameter(name);
    }

    /**
     * @param req
     * @param name
     * @param defaultValue
     * @return
     */
    protected String getParameter(HttpServletRequest req, String name, String defaultValue) {
        return StringUtils.defaultIfBlank(getParameter(req, name), defaultValue);
    }

    /**
     * @param req
     * @param name
     * @return
     */
    protected String getParameterNotNull(HttpServletRequest req, String name) {
        String v = req.getParameter(name);
        if (StringUtils.isEmpty(v)) {
            throw new InvalidParameterException("无效参数 [" + name + "=" + v + "]");
        }
        return v;
    }

    /**
     * @param req
     * @param name
     * @return
     */
    protected Integer getIntParameter(HttpServletRequest req, String name) {
        return getIntParameter(req, name, null);
    }

    /**
     * @param req
     * @param name
     * @param defaultValue
     * @return
     */
    protected Integer getIntParameter(HttpServletRequest req, String name, Integer defaultValue) {
        String v = req.getParameter(name);
        if (v == null) {
            return defaultValue;
        }
        return NumberUtils.toInt(v, defaultValue);
    }

    /**
     * @param req
     * @param name
     * @return
     */
    protected boolean getBoolParameter(HttpServletRequest req, String name) {
        String v = req.getParameter(name);
        return v != null && BooleanUtils.toBoolean(v);
    }

    /**
     * @param req
     * @param name
     * @param defaultValue
     * @return
     */
    protected boolean getBoolParameter(HttpServletRequest req, String name, boolean defaultValue) {
        String v = req.getParameter(name);
        return v == null ? defaultValue : BooleanUtils.toBoolean(v);
    }

    /**
     * @param req
     * @param name
     * @return
     */
    protected ID getIdParameter(HttpServletRequest req, String name) {
        String v = req.getParameter(name);
        return ID.isId(v) ? ID.valueOf(v) : null;
    }

    /**
     * @param req
     * @param name
     * @return
     */
    protected ID getIdParameterNotNull(HttpServletRequest req, String name) {
        String v = req.getParameter(name);
        if (ID.isId(v)) {
            return ID.valueOf(v);
        }
        throw new InvalidParameterException("无效ID参数 [" + name + "=" + v + "]");
    }

    /**
     * @param page
     * @return
     */
    protected ModelAndView createModelAndView(String page) {
        return new ModelAndView(page);
    }
}
