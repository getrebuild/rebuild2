/*
Copyright (c) REBUILD <https://getrebuild.com/> and/or its owners. All rights reserved.

rebuild is dual-licensed under commercial and open source licenses (GPLv3).
See LICENSE and COMMERCIAL in the project root for license information.
*/

package com.rebuild.web;

import com.rebuild.core.RebuildApplication;
import com.rebuild.core.helper.ConfigurationItem;
import com.rebuild.core.helper.RebuildConfiguration;
import com.rebuild.utils.AppUtils;
import org.springframework.boot.autoconfigure.web.servlet.error.ErrorViewResolver;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Component;
import org.springframework.util.Assert;
import org.springframework.web.servlet.ModelAndView;
import org.springframework.web.servlet.config.annotation.InterceptorRegistry;
import org.springframework.web.servlet.config.annotation.ViewResolverRegistry;
import org.springframework.web.servlet.config.annotation.WebMvcConfigurer;
import org.thymeleaf.spring5.view.ThymeleafViewResolver;

import javax.annotation.Resource;
import javax.servlet.http.HttpServletRequest;
import java.util.HashMap;
import java.util.Map;

/**
 * @author devezhao
 * @since 2020/8/26
 */
@Component
public class RebuildWebConfigurer implements WebMvcConfigurer, ErrorViewResolver {

    // -- for Thymeleaf

    @Resource(name = "thymeleafViewResolver")
    private ThymeleafViewResolver thymeleafViewResolver;

    public void configureViewResolvers(ViewResolverRegistry registry) {
        if (thymeleafViewResolver != null) {
            thymeleafViewResolverHold = thymeleafViewResolver;
            resetStaticVariables();
        }
        WebMvcConfigurer.super.configureViewResolvers(registry);
    }

    private static ThymeleafViewResolver thymeleafViewResolverHold;

    /**
     * 设置全局 Web 上下文变量
     */
    public static void resetStaticVariables() {
        Assert.notNull(thymeleafViewResolverHold, "[thymeleafViewResolverHold] not be bull");

        thymeleafViewResolverHold.addStaticVariable("baseUrl", AppUtils.getContextPath());
        thymeleafViewResolverHold.addStaticVariable("env", RebuildApplication.devMode() ? "dev" : "prodution");
        thymeleafViewResolverHold.addStaticVariable("appName", RebuildConfiguration.get(ConfigurationItem.AppName));
        thymeleafViewResolverHold.addStaticVariable("storageUrl", RebuildConfiguration.get(ConfigurationItem.StorageURL));
        thymeleafViewResolverHold.addStaticVariable("fileSharable", RebuildConfiguration.get(ConfigurationItem.FileSharable));
    }

    // -- for Interceptor(s)

    @Override
    public void addInterceptors(InterceptorRegistry registry) {
        registry.addInterceptor(new RequestWatchHandler())
                .excludePathPatterns("/gw/**")
                .excludePathPatterns("/assets/**")
                .excludePathPatterns("/error");
    }


    // -- for Error(s)

    @Override
    public ModelAndView resolveErrorView(HttpServletRequest request, HttpStatus status, Map<String, Object> model) {
        Map<String, Object> error = new HashMap<>(model);
        error.put("error_code", status.value());
        switch (status) {
            case NOT_FOUND:
            case METHOD_NOT_ALLOWED:
                error.put("error_message", "访问的地址/资源不存在");
                break;
            case FORBIDDEN:
            case UNAUTHORIZED:
                error.put("error_message", "权限不足，访问被阻止");
                break;
            case INTERNAL_SERVER_ERROR:
                error.put("error_message", "系统繁忙，请稍后重试");
                break;
            default:
                error.put("error_message", model.get("error"));
                break;
        }
        return new ModelAndView("_include/error", error);
    }
}
