package com.rebuild.web;

import com.rebuild.core.RebuildApplication;
import com.rebuild.core.RebuildEnvironmentPostProcessor;
import com.rebuild.core.helper.ConfigurableItem;
import com.rebuild.core.helper.RebuildConfiguration;
import org.springframework.stereotype.Component;
import org.springframework.web.servlet.config.annotation.ViewResolverRegistry;
import org.springframework.web.servlet.config.annotation.WebMvcConfigurer;
import org.thymeleaf.spring5.view.ThymeleafViewResolver;

import javax.annotation.Resource;

@Component
public class RebuildWebMvcConfigurer implements WebMvcConfigurer {

    @Resource(name="thymeleafViewResolver")
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
        thymeleafViewResolverHold.addStaticVariable("env",
                RebuildApplication.devMode() ? "dev" : "prodution");
        thymeleafViewResolverHold.addStaticVariable("baseUrl",
                RebuildEnvironmentPostProcessor.getProperty("server.servlet.context-path", "/"));

        thymeleafViewResolverHold.addStaticVariable("appName",
                RebuildConfiguration.get(ConfigurableItem.AppName));
        thymeleafViewResolverHold.addStaticVariable("storageUrl",
                RebuildConfiguration.get(ConfigurableItem.StorageURL));
        thymeleafViewResolverHold.addStaticVariable("fileSharable",
                RebuildConfiguration.get(ConfigurableItem.FileSharable));
    }
}
