/*
Copyright (c) REBUILD <https://getrebuild.com/> and/or its owners. All rights reserved.

rebuild is dual-licensed under commercial and open source licenses (GPLv3).
See LICENSE and COMMERCIAL in the project root for license information.
*/

package com.rebuild.utils;

import cn.devezhao.commons.ThrowableUtils;
import cn.devezhao.commons.web.ServletUtils;
import cn.devezhao.commons.web.WebUtils;
import cn.devezhao.persist4j.engine.ID;
import com.rebuild.api.login.AuthTokenManager;
import com.rebuild.core.Application;
import com.rebuild.core.RebuildEnvironmentPostProcessor;
import com.rebuild.core.privileges.bizz.User;
import com.rebuild.web.admin.AdminEntryController;
import org.apache.commons.lang.StringUtils;

import javax.servlet.http.HttpServletRequest;
import java.nio.file.AccessDeniedException;
import java.sql.DataTruncation;

/**
 * 封裝一些有用的工具方法
 *
 * @author zhaofang123@gmail.com
 * @since 05/19/2018
 */
public class AppUtils {

    /**
     * 移动端 UA 前缀
     */
    public static final String MOILE_UA_PREFIX = "RB/MOBILE-";

    /**
     * 移动端 Token Header
     */
    public static final String MOBILE_HF_AUTHTOKEN = "X-AuthToken";

    /**
     * @return
     */
    public static String getContextPath() {
        return RebuildEnvironmentPostProcessor.getProperty("server.servlet.context-path", "");
    }

    /**
     * 获取当前请求用户
     *
     * @param request
     * @return null or UserID
     * @see #getRequestUserViaRbMobile(HttpServletRequest, boolean)
     */
    public static ID getRequestUser(HttpServletRequest request) {
        Object user = request.getSession(true).getAttribute(WebUtils.CURRENT_USER);
        return user == null ? null : (ID) user;
    }

    /**
     * 获取 APP 请求用户
     *
     * @param request
     * @param refreshToken 是否需要刷新 Token 有效期
     * @return
     * @see #isRbMobile(HttpServletRequest)
     */
    public static ID getRequestUserViaRbMobile(HttpServletRequest request, boolean refreshToken) {
        if (isRbMobile(request)) {
            String xAuthToken = request.getHeader(MOBILE_HF_AUTHTOKEN);
            ID user = AuthTokenManager.verifyToken(xAuthToken, false);
            if (user != null && refreshToken) {
                AuthTokenManager.refreshToken(xAuthToken, AuthTokenManager.TOKEN_EXPIRES);
            }
            return user;
        }
        return null;
    }

    /**
     * @param request
     * @return
     */
    public static User getRequestUserBean(HttpServletRequest request) {
        ID user = getRequestUser(request);
        if (user == null) user = getRequestUserViaRbMobile(request, false);
        if (user == null) return null;
        return Application.getUserStore().getUser(user);
    }

    /**
     * @param request
     * @return
     */
    public static boolean isAdminVerified(HttpServletRequest request) {
        Object verified = ServletUtils.getSessionAttribute(request, AdminEntryController.KEY_VERIFIED);
        return verified != null;
    }

    /**
     * 获取后台抛出的错误消息
     *
     * @param request
     * @param exception
     * @return
     */
    public static String getErrorMessage(HttpServletRequest request, Throwable exception) {
        if (exception == null && request != null) {
            String errorMsg = (String) request.getAttribute(ServletUtils.ERROR_MESSAGE);
            if (StringUtils.isNotBlank(errorMsg)) {
                return errorMsg;
            }

            Integer code = (Integer) request.getAttribute(ServletUtils.ERROR_STATUS_CODE);
            if (code != null && code == 404) {
                return "访问的地址/资源不存在";
            } else if (code != null && code == 403) {
                return "权限不足，访问被阻止";
            }

            exception = (Throwable) request.getAttribute(ServletUtils.ERROR_EXCEPTION);
            if (exception == null) {
                exception = (Throwable) request.getAttribute(ServletUtils.JSP_JSP_EXCEPTION);
            }
        }

        // 已知异常
        if (exception != null) {
            Throwable know = ThrowableUtils.getRootCause(exception);
            if (know instanceof DataTruncation) {
                return "字段长度超出限制";
            } else if (know instanceof AccessDeniedException) {
                return "权限不足，访问被阻止";
            }
        }

        if (exception == null) {
            return "系统繁忙，请稍后重试";
        } else {
            exception = ThrowableUtils.getRootCause(exception);
            String errorMsg = StringUtils.defaultIfBlank(exception.getLocalizedMessage(), "系统繁忙，请稍后重试");
            if (Application.devMode()) {
                errorMsg += " (" + exception.getClass().getSimpleName() + ")";
            }
            return errorMsg;
        }
    }

    /**
     * 是否 APP
     *
     * @param request
     * @return
     */
    public static boolean isRbMobile(HttpServletRequest request) {
        String UA = request.getHeader("user-agent");
        return UA != null && UA.toUpperCase().startsWith(MOILE_UA_PREFIX);
    }
}
