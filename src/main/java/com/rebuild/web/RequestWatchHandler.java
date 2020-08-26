/*
Copyright (c) REBUILD <https://getrebuild.com/> and/or its owners. All rights reserved.

rebuild is dual-licensed under commercial and open source licenses (GPLv3).
See LICENSE and COMMERCIAL in the project root for license information.
*/

package com.rebuild.web;

import cn.devezhao.commons.CodecUtils;
import cn.devezhao.commons.ThrowableUtils;
import cn.devezhao.commons.web.ServletUtils;
import cn.devezhao.persist4j.engine.ID;
import com.rebuild.core.RebuildApplication;
import com.rebuild.core.helper.setup.InstallState;
import com.rebuild.utils.AppUtils;
import org.apache.commons.lang.StringUtils;
import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.springframework.stereotype.Component;
import org.springframework.web.servlet.handler.HandlerInterceptorAdapter;

import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import java.io.IOException;

/**
 * Controller 请求拦截
 *
 * @author Zhao Fangfang
 * @since 1.0, 2013-6-24
 */
@Component
public class RequestWatchHandler extends HandlerInterceptorAdapter implements InstallState {

    private static final Log LOG = LogFactory.getLog(RequestWatchHandler.class);

    // 设置页面无缓存
    // 如果使用了第三方缓存策略（如 nginx），可以将此值设为 false
    private boolean noCache = true;

    /**
     * @param noCache
     */
    public void setNoCache(boolean noCache) {
        this.noCache = noCache;
    }

    /**
     * @return
     */
    public boolean isNoCache() {
        return noCache;
    }

    @Override
    public boolean preHandle(HttpServletRequest request, HttpServletResponse response,
                             Object handler) throws Exception {
        response.setCharacterEncoding("utf-8");
        request.getSession(true);

        final String requestUrl = request.getRequestURI();

        // 无缓存
        if (isNoCache() && !isSpecCache(requestUrl)) {
            ServletUtils.setNoCacheHeaders(response);
        }

        // If server status is not passed
        if (!RebuildApplication.serversReady()) {
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
            if (!(isIgnoreActive(requestUrl) || ServletUtils.isAjaxRequest(request))) {
                RebuildApplication.getSessionStore().storeLastActive(request);
            }
        }

        boolean chain = super.preHandle(request, response, handler);
        if (chain) {
            return verfiyPass(request, response);
        }
        return false;
    }

    /**
     * @see RebuildExceptionResolver
     */
    @Override
    public void afterCompletion(HttpServletRequest request,
                                HttpServletResponse response, Object handler, Exception exception)
            throws Exception {
        super.afterCompletion(request, response, handler, exception);

        final ID caller = RebuildApplication.serversReady() ? RebuildApplication.getSessionStore().get(true) : null;
        if (caller != null) {
            RebuildApplication.getSessionStore().clean();
        }

        logProgressTime(request);

        if (exception != null) {
            Throwable rootCause = ThrowableUtils.getRootCause(exception);
            StringBuffer sb = new StringBuffer()
                    .append("\n++ EXECUTE REQUEST ERROR(s) TRACE +++++++++++++++++++++++++++++++++++++++++++++")
                    .append("\nUser      : ").append(caller == null ? "-" : caller)
                    .append("\nHandler   : ").append(request.getRequestURI()).append(" [ ").append(handler).append(" ]")
                    .append("\nIP        : ").append(ServletUtils.getRemoteAddr(request))
                    .append("\nReferer   : ").append(StringUtils.defaultIfEmpty(ServletUtils.getReferer(request), "-"))
                    .append("\nUserAgent : ").append(StringUtils.defaultIfEmpty(request.getHeader("user-agent"), "-"))
                    .append("\nCause     : ").append(rootCause.getClass().getName())
                    .append("\nMessage   : ").append(StringUtils.defaultIfBlank(rootCause.getLocalizedMessage(), "-"));
            LOG.error(sb, rootCause);
        }
    }

    /**
     * 打印处理时间
     *
     * @param request
     */
    private void logProgressTime(HttpServletRequest request) {
        Long startTime = (Long) request.getAttribute(TIMEOUT_KEY);
        startTime = System.currentTimeMillis() - startTime;
        if (startTime > 500) {
            String url = request.getRequestURI();
            String qstr = request.getQueryString();
            if (qstr != null) {
                url += '?' + qstr;
            }
            LOG.warn("Method handle time " + startTime + " ms. Request URL [ " + url + " ] from [ " + StringUtils.defaultIfEmpty(ServletUtils.getReferer(request), "-") + " ]");
        }
    }

    // --

    private static final String TIMEOUT_KEY = "ErrorHandler_TIMEOUT";

    /**
     * 用户验证
     *
     * @param request
     * @param response
     * @return
     * @throws IOException
     */
    public static boolean verfiyPass(HttpServletRequest request, HttpServletResponse response) throws IOException {
        request.setAttribute(TIMEOUT_KEY, System.currentTimeMillis());

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
            RebuildApplication.getSessionStore().set(user);

            // 管理后台访问
            if (requestUrl.contains("/admin/") && !AppUtils.isAdminVerified(request)) {
                if (ServletUtils.isAjaxRequest(request)) {
                    ServletUtils.writeJson(response, AppUtils.formatControllerMessage(403, "请验证管理员访问权限"));
                } else {
                    response.sendRedirect(AppUtils.getContextPath() + "/user/admin-entry?nexturl=" + CodecUtils.urlEncode(requestUrl));
                }
                return false;
            }

        } else if (!inIgnoreRes(requestUrl)) {
            LOG.warn("Unauthorized access [ " + requestUrl + " ] from "
                    + StringUtils.defaultIfBlank(ServletUtils.getReferer(request), "<unknow>")
                    + " via " + ServletUtils.getRemoteAddr(request));

            if (ServletUtils.isAjaxRequest(request)) {
                ServletUtils.writeJson(response, AppUtils.formatControllerMessage(403, "未授权访问"));
            } else {
                response.sendRedirect(AppUtils.getContextPath() + "/user/login?nexturl=" + CodecUtils.urlEncode(requestUrl));
            }
            return false;
        }

        return true;
    }

    /**
     * 是否忽略用户验证
     *
     * @param reqUrl
     * @return
     */
    private static boolean inIgnoreRes(String reqUrl) {
        if (reqUrl.contains("/user/") && !reqUrl.contains("/user/admin")) {
            return true;
        }

        reqUrl = reqUrl.replaceFirst(AppUtils.getContextPath(), "");
        return reqUrl.startsWith("/gw/") || reqUrl.startsWith("/assets/") || reqUrl.startsWith("/error/")
                || reqUrl.startsWith("/t/") || reqUrl.startsWith("/s/") || reqUrl.startsWith("/public/")
                || reqUrl.startsWith("/setup/") || reqUrl.startsWith("/language/")
                || reqUrl.startsWith("/commons/announcements")
                || reqUrl.startsWith("/commons/url-safe")
                || reqUrl.startsWith("/filex/access/")
                || reqUrl.startsWith("/commons/barcode/render");
    }

    /**
     * SESSION 活跃忽略
     *
     * @param reqUrl
     * @return
     */
    private static boolean isIgnoreActive(String reqUrl) {
        return reqUrl.contains("/language/") || reqUrl.contains("/user-avatar");
    }

    /**
     * 是否特定缓存策略
     *
     * @param reqUrl
     * @return
     */
    private static boolean isSpecCache(String reqUrl) {
        reqUrl = reqUrl.replaceFirst(AppUtils.getContextPath(), "");
        return reqUrl.startsWith("/filex/img/") || reqUrl.startsWith("/account/user-avatar/")
                || reqUrl.startsWith("/language/")
                || reqUrl.startsWith("/commons/barcode/");
    }
}
