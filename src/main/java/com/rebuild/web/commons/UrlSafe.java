/*
Copyright (c) REBUILD <https://getrebuild.com/> and/or its owners. All rights reserved.

rebuild is dual-licensed under commercial and open source licenses (GPLv3).
See LICENSE and COMMERCIAL in the project root for license information.
*/

package com.rebuild.web.commons;

import com.rebuild.core.RebuildEnvironmentPostProcessor;
import com.rebuild.core.helper.RebuildConfiguration;
import com.rebuild.web.BaseController;
import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestMethod;
import org.springframework.web.servlet.ModelAndView;

import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import java.io.IOException;
import java.net.MalformedURLException;
import java.net.URL;
import java.util.Arrays;
import java.util.HashSet;
import java.util.Set;

/**
 * 外部 URL 监测跳转
 *
 * @author zhaofang123@gmail.com
 * @since 09/20/2018
 */
@Controller
public class UrlSafe extends BaseController {

    @RequestMapping(value = "/commons/url-safe", method = RequestMethod.GET)
    public ModelAndView safeRedirect(HttpServletRequest request, HttpServletResponse response) throws IOException {
        String url = getParameterNotNull(request, "url");
        if (!url.startsWith("http")) {
            url = "http://" + url;
        }

        if (isTrusted(url)) {
            response.sendRedirect(url);
            return null;
        }

        ModelAndView mv = createModelAndView("/commons/url-safe");
        mv.getModel().put("outerUrl", url);
        return mv;
    }

    private static final Set<String> TRUSTED_URLS = new HashSet<>();

    /**
     * 是否可信 URL
     *
     * @param url
     * @return
     */
    public static boolean isTrusted(String url) {
        url = url.split("\\?")[0];
        if (url.contains(RebuildConfiguration.getHomeUrl())) {
            return true;
        }

        // 首次
        if (TRUSTED_URLS.isEmpty()) {
            TRUSTED_URLS.add("getrebuild.com");

            String trustedUrls = RebuildEnvironmentPostProcessor.getProperty("rebuild.TrustedUrls", "");
            TRUSTED_URLS.addAll(Arrays.asList(trustedUrls.split(" ")));
        }

        String host = url;
        try {
            host = new URL(url).getHost();
        } catch (MalformedURLException ignored) {
        }

        for (String t : TRUSTED_URLS) {
            if (host.equals(t) || host.contains(t)) {
                return true;
            }
        }
        return false;
    }
}
