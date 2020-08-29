/*
Copyright (c) REBUILD <https://getrebuild.com/> and/or its owners. All rights reserved.

rebuild is dual-licensed under commercial and open source licenses (GPLv3).
See LICENSE and COMMERCIAL in the project root for license information.
*/

package com.rebuild.web;

import cn.devezhao.commons.CodecUtils;
import cn.devezhao.commons.web.ServletUtils;
import cn.devezhao.persist4j.engine.ID;
import com.rebuild.api.ResultBody;
import com.rebuild.core.Application;
import com.rebuild.core.helper.setup.InstallState;
import com.rebuild.core.privileges.bizz.ZeroEntry;
import com.rebuild.utils.AppUtils;
import org.apache.commons.lang.StringUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.web.servlet.handler.HandlerInterceptorAdapter;

import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import java.io.IOException;

/**
 * 请求拦截
 *
 * @author Zhao Fangfang
 * @since 1.0, 2013-6-24
 */
public class RebuildAuthInterceptor extends HandlerInterceptorAdapter implements InstallState {

    private static final Logger LOG = LoggerFactory.getLogger(RebuildAuthInterceptor.class);

    private static final String TIMEOUT_KEY = "ErrorHandler_TIMEOUT";

    @Override
    public boolean preHandle(HttpServletRequest request, HttpServletResponse response, Object handler)
            throws Exception {
        request.getSession(true);

        final String requestUrl = request.getRequestURI();

        // If server status is not passed
        if (!Application.serversReady()) {
            if (checkInstalled()) {
                LOG.error("Server Unavailable : " + requestUrl);
                if (!requestUrl.contains("/gw/server-status")) {
                    response.sendRedirect(AppUtils.getContextPath() + "/gw/server-status?s=" + CodecUtils.urlEncode(requestUrl));
                    return false;
                }

            } else if (!requestUrl.contains("/setup/")) {
                response.sendRedirect(AppUtils.getContextPath() + "/setup/install");
                return false;
            }

        } else {
            // Last active
            if (!isIgnoreActive(requestUrl) && AppUtils.isHtmlRequest(request)) {
                Application.getSessionStore().storeLastActive(request);
            }
        }

        request.setAttribute(TIMEOUT_KEY, System.currentTimeMillis());

        return verfiyPass(request, response);
    }

    @Override
    public void afterCompletion(HttpServletRequest request, HttpServletResponse response, Object handler, Exception ex) {
        ID user = Application.serversReady() ? Application.getSessionStore().get(true) : null;
        if (user != null) {
            Application.getSessionStore().clean();
        }

        // 打印处理时间
        Long startTime = (Long) request.getAttribute(TIMEOUT_KEY);
        startTime = System.currentTimeMillis() - startTime;
        if (startTime > 500) {
            String url = request.getRequestURI();
            String qstr = request.getQueryString();
            if (qstr != null) {
                url += '?' + qstr;
            }
            LOG.warn("Method handle time " + startTime + " ms. Request URL [ " + url + " ] from [ "
                    + StringUtils.defaultIfEmpty(ServletUtils.getReferer(request), "-") + " ]");
        }
    }

    // --

    /**
     * 用户验证
     *
     * @param request
     * @param response
     * @return
     * @throws IOException
     */
    public static boolean verfiyPass(HttpServletRequest request, HttpServletResponse response) throws IOException {
        String requestUrl = request.getRequestURI();
        String qstr = request.getQueryString();
        if (StringUtils.isNotBlank(qstr)) {
            requestUrl += "?" + qstr;
        }

        ID user = AppUtils.getRequestUser(request);
        if (user == null) {
            user = AppUtils.getRequestUserViaRbMobile(request, true);
        }

        if (user != null) {
            Application.getSessionStore().set(user);

            // 管理后台访问
            if (requestUrl.contains("/admin/") && !AppUtils.isAdminVerified(request)) {
                if (AppUtils.isHtmlRequest(request)) {
                    response.sendRedirect(AppUtils.getContextPath() + "/user/admin-verify?nexturl=" + CodecUtils.urlEncode(requestUrl));
                } else {
                    ServletUtils.writeJson(response, ResultBody.error("请验证管理员访问权限", 403).toString());
                }
                return false;
            }

            // FIX: 前端使用（可能有更好的地方放置此代码）
            request.setAttribute("user", Application.getUserStore().getUser(user));
            request.setAttribute("AllowCustomNav",
                    Application.getPrivilegesManager().allow(user, ZeroEntry.AllowCustomNav));

        } else if (!isIgnoreAuth(requestUrl)) {
            LOG.warn("Unauthorized access [ " + requestUrl + " ] from "
                    + StringUtils.defaultIfBlank(ServletUtils.getReferer(request), "<unknow>")
                    + " via " + ServletUtils.getRemoteAddr(request));

            if (AppUtils.isHtmlRequest(request)) {
                response.sendRedirect(AppUtils.getContextPath() + "/user/login?nexturl=" + CodecUtils.urlEncode(requestUrl));
            } else {
                ServletUtils.writeJson(response, ResultBody.error("未授权访问", 403).toString());
            }
            return false;
        }

        return true;
    }

    /**
     * 是否忽略用户验证
     *
     * @param requestUrl
     * @return
     */
    static boolean isIgnoreAuth(String requestUrl) {
        if (requestUrl.contains("/user/") && !requestUrl.contains("/user/admin")) {
            return true;
        }

        requestUrl = requestUrl.replaceFirst(AppUtils.getContextPath(), "");
        return requestUrl.startsWith("/gw/") || requestUrl.startsWith("/assets/") || requestUrl.startsWith("/error/")
                || requestUrl.startsWith("/t/") || requestUrl.startsWith("/s/") || requestUrl.startsWith("/public/")
                || requestUrl.startsWith("/setup/") || requestUrl.startsWith("/language/")
                || requestUrl.startsWith("/commons/announcements")
                || requestUrl.startsWith("/commons/url-safe")
                || requestUrl.startsWith("/filex/access/")
                || requestUrl.startsWith("/commons/barcode/render");
    }

    /**
     * SESSION 活跃忽略
     *
     * @param reqUrl
     * @return
     */
    static boolean isIgnoreActive(String reqUrl) {
        return reqUrl.contains("/language/") || reqUrl.contains("/user-avatar");
    }
}
