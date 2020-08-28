/*
Copyright (c) REBUILD <https://getrebuild.com/> and/or its owners. All rights reserved.

rebuild is dual-licensed under commercial and open source licenses (GPLv3).
See LICENSE and COMMERCIAL in the project root for license information.
*/

package com.rebuild.web;

import cn.devezhao.commons.web.ServletUtils;
import com.alibaba.fastjson.support.spring.FastJsonJsonView;
import com.rebuild.core.Application;
import com.rebuild.core.helper.ConfigurationItem;
import com.rebuild.core.helper.RebuildConfiguration;
import com.rebuild.utils.AppUtils;
import org.apache.commons.lang.ObjectUtils;
import org.apache.commons.lang.StringUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.boot.autoconfigure.web.servlet.error.ErrorViewResolver;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Component;
import org.springframework.util.Assert;
import org.springframework.web.servlet.HandlerExceptionResolver;
import org.springframework.web.servlet.ModelAndView;
import org.springframework.web.servlet.config.annotation.InterceptorRegistry;
import org.springframework.web.servlet.config.annotation.ViewResolverRegistry;
import org.springframework.web.servlet.config.annotation.WebMvcConfigurer;
import org.thymeleaf.spring5.view.ThymeleafViewResolver;

import javax.annotation.Resource;
import javax.servlet.http.HttpServletRequest;
import java.util.List;
import java.util.Map;

/**
 * MVC 配置
 *
 * @author devezhao
 * @since 2020/8/26
 */
@Component
public class RebuildWebConfigurer implements WebMvcConfigurer, ErrorViewResolver {

    private static final Logger LOG = LoggerFactory.getLogger(RebuildWebConfigurer.class);

    @Resource(name = "thymeleafViewResolver")
    private ThymeleafViewResolver thymeleafViewResolver;

    private static ThymeleafViewResolver thymeleafViewResolverHold;

    @Override
    public void configureViewResolvers(ViewResolverRegistry registry) {
        if (thymeleafViewResolver != null) {
            thymeleafViewResolverHold = thymeleafViewResolver;
            resetStaticVariables();
        }
        WebMvcConfigurer.super.configureViewResolvers(registry);
    }

    /**
     * 设置全局 Web 上下文变量
     */
    public static void resetStaticVariables() {
        Assert.notNull(thymeleafViewResolverHold, "[thymeleafViewResolverHold] not be bull");

        thymeleafViewResolverHold.addStaticVariable("baseUrl", AppUtils.getContextPath());
        thymeleafViewResolverHold.addStaticVariable("env", Application.devMode() ? "dev" : "prodution");
        thymeleafViewResolverHold.addStaticVariable("appName", RebuildConfiguration.get(ConfigurationItem.AppName));
        thymeleafViewResolverHold.addStaticVariable("storageUrl", RebuildConfiguration.get(ConfigurationItem.StorageURL));
        thymeleafViewResolverHold.addStaticVariable("fileSharable", RebuildConfiguration.get(ConfigurationItem.FileSharable));
    }

    /**
     * 请求拦截
     *
     * @param registry
     */
    @Override
    public void addInterceptors(InterceptorRegistry registry) {
        registry.addInterceptor(new RebuildAuthHandler())
                .excludePathPatterns("/gw/**")
                .excludePathPatterns("/assets/**")
                .excludePathPatterns("/error");
    }

    /**
     * 异常处理
     *
     * @param resolvers
     */
    @Override
    public void configureHandlerExceptionResolvers(List<HandlerExceptionResolver> resolvers) {
        resolvers.add((request, response, handler, ex) -> {
            request.setAttribute(ServletUtils.ERROR_STATUS_CODE, HttpStatus.INTERNAL_SERVER_ERROR.value());
            logError(request, ex, handler);
            return createError(request, ex);
        });
    }

    @Override
    public ModelAndView resolveErrorView(HttpServletRequest request, HttpStatus status, Map<String, Object> model) {
        request.setAttribute(ServletUtils.ERROR_STATUS_CODE, status.value());
        logError(request, null, model);
        return createError(request, null);
    }

    /**
     * @param request
     * @param ex
     * @return
     */
    private ModelAndView createError(HttpServletRequest request, Exception ex) {
        ModelAndView error;
        if (ServletUtils.isAjaxRequest(request)) {
            error = new ModelAndView(new FastJsonJsonView());
        } else {
            error = new ModelAndView("/error/error");
        }

        String errorMsg = AppUtils.getErrorMessage(request, ex);
        error.getModel().put("error_code", com.rebuild.api.Controller.CODE_ERROR);
        error.getModel().put("error_msg", errorMsg);
        return error;
    }

    /**
     * @param request
     * @param ex
     * @param details
     */
    private void logError(HttpServletRequest request, Exception ex, Object details) {
        String err = "\n++ EXECUTE REQUEST ERROR(s) TRACE +++++++++++++++++++++++++++++++++++++++++++++" +
                "\nUser      : " + ObjectUtils.defaultIfNull(AppUtils.getRequestUser(request), "-") +
                "\nIP        : " + ServletUtils.getRemoteAddr(request) +
                "\nUserAgent : " + StringUtils.defaultIfEmpty(request.getHeader("user-agent"), "-") +
                "\nReferer   : " + StringUtils.defaultIfEmpty(ServletUtils.getReferer(request), "-") +
                "\nDetails   : " + request.getRequestURI() + " [ " + details + " ]";
        LOG.error(err, ex);
    }
}
