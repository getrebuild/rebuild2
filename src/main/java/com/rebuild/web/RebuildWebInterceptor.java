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
import com.rebuild.core.helper.ConfigurationItem;
import com.rebuild.core.helper.RebuildConfiguration;
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
 * - 检查授权
 * - 设置前端页面变量
 * - 设置请求用户（线程量）
 *
 * @author Zhao Fangfang
 * @since 1.0, 2013-6-24
 */
public class RebuildWebInterceptor extends HandlerInterceptorAdapter implements InstallState {

    private static final Logger LOG = LoggerFactory.getLogger(RebuildWebInterceptor.class);

    private static final String TIMEOUT_KEY = "ErrorHandler_TIMEOUT";

    @Override
    public boolean preHandle(HttpServletRequest request, HttpServletResponse response, Object handler)
            throws Exception {
        request.getSession(true);

        request.setAttribute(TIMEOUT_KEY, System.currentTimeMillis());

        final String requestUri = request.getRequestURI();
        final boolean htmlRequest = AppUtils.isHtmlRequest(request);

        // Locale
        String locale = (String) ServletUtils.getSessionAttribute(request, AppUtils.SK_LOCALE);
        if (locale == null) {
            locale = RebuildConfiguration.get(ConfigurationItem.DefaultLanguage);
        }
        Application.getSessionStore().setLocale(locale);

        if (htmlRequest) {
            request.setAttribute("locale", locale);
            if (Application.serversReady()) {
                request.setAttribute("bundle", Application.getCurrentBundle());
            }
        }

        // If server status is not passed
        if (!Application.serversReady()) {
            if (checkInstalled()) {
                LOG.error("Server Unavailable : " + requestUri);
                if (!requestUri.contains("/gw/server-status")) {
                    sendRedirect(response, "/gw/server-status", null);
                    return false;
                }

            } else if (!requestUri.contains("/setup/")) {
                sendRedirect(response, "/setup/install", null);
                return false;
            }
        }

        return verfiyPass(request, response);
    }

    @Override
    public void afterCompletion(HttpServletRequest request, HttpServletResponse response, Object handler, Exception ex) {
        // 清理用户
        ID user = Application.serversReady() ? Application.getSessionStore().get(true) : null;
        if (user != null) {
            Application.getSessionStore().clean();
        }

        // 打印处理时间
        Long time = (Long) request.getAttribute(TIMEOUT_KEY);
        time = System.currentTimeMillis() - time;
        if (time > 1000) {
            LOG.warn("Method handle time {} ms. Request URL {} ref {}",
                    time, ServletUtils.getFullRequestUrl(request), StringUtils.defaultIfBlank(ServletUtils.getReferer(request), "-"));
        }
    }

    /**
     * 用户验证
     *
     * @param request
     * @param response
     * @return
     * @throws IOException
     */
    protected boolean verfiyPass(HttpServletRequest request, HttpServletResponse response) throws IOException {
        final String requestUri = request.getRequestURI();
        final boolean isHtmlRequest = AppUtils.isHtmlRequest(request);

        ID requestUser = AppUtils.getRequestUser(request);
        if (requestUser == null) {
            requestUser = AppUtils.getRequestUserViaRbMobile(request, true);
        }

        if (requestUser != null) {
            // 管理后台访问
            if (requestUri.contains("/admin/") && !AppUtils.isAdminVerified(request)) {
                if (isHtmlRequest) {
                    sendRedirect(response, "/user/admin-verify", requestUri);
                } else {
                    ServletUtils.writeJson(response, ResultBody.error(401).toString());
                }
                return false;
            }

            Application.getSessionStore().set(requestUser);

            // 前端使用（fix: 可能有更好的地方放置此代码）
            if (isHtmlRequest) {
                Application.getSessionStore().storeLastActive(request);

                request.setAttribute("user", Application.getUserStore().getUser(requestUser));
                request.setAttribute("AllowCustomNav",
                        Application.getPrivilegesManager().allow(requestUser, ZeroEntry.AllowCustomNav));
            }

        } else if (!isIgnoreAuth(requestUri)) {
            LOG.warn("Unauthorized access {} from {} via {}",
                    requestUri, StringUtils.defaultIfBlank(ServletUtils.getReferer(request), "-"), ServletUtils.getRemoteAddr(request));

            if (isHtmlRequest) {
                sendRedirect(response, "/user/login", requestUri);
            } else {
                ServletUtils.writeJson(response, ResultBody.error(403).toString());
            }
            return false;
        }

        return true;
    }

    private void sendRedirect(HttpServletResponse response, String url, String nexturl) throws IOException {
        String fullUrl = AppUtils.getContextPath() + url;
        if (nexturl != null) fullUrl += "?nexturl=" + CodecUtils.urlEncode(nexturl);
        response.sendRedirect(fullUrl);
    }

    /**
     * 忽略认证
     *
     * @param requestUri
     * @return
     */
    private boolean isIgnoreAuth(String requestUri) {
        if (requestUri.contains("/user/") && !requestUri.contains("/user/admin")) return true;

        requestUri = requestUri.replaceFirst(AppUtils.getContextPath(), "");
        return requestUri.length() < 3 || requestUri.startsWith("/t/") || requestUri.startsWith("/s/")
                || requestUri.startsWith("/setup/")
                || requestUri.startsWith("/language/")
                || requestUri.startsWith("/filex/access/")
                || requestUri.startsWith("/commons/announcements")
                || requestUri.startsWith("/commons/url-safe")
                || requestUri.startsWith("/commons/barcode/render");
    }
}
