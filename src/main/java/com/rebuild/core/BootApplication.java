/*
Copyright (c) REBUILD <https://getrebuild.com/> and/or its owners. All rights reserved.

rebuild is dual-licensed under commercial and open source licenses (GPLv3).
See LICENSE and COMMERCIAL in the project root for license information.
*/

package com.rebuild.core;

import com.rebuild.core.support.RebuildConfiguration;
import org.apache.commons.lang.BooleanUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.boot.Banner;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.WebApplicationType;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.boot.autoconfigure.cache.CacheAutoConfiguration;
import org.springframework.boot.autoconfigure.data.jdbc.JdbcRepositoriesAutoConfiguration;
import org.springframework.boot.autoconfigure.data.redis.RedisAutoConfiguration;
import org.springframework.boot.autoconfigure.jdbc.DataSourceAutoConfiguration;
import org.springframework.boot.autoconfigure.jdbc.JdbcTemplateAutoConfiguration;
import org.springframework.boot.builder.SpringApplicationBuilder;
import org.springframework.boot.web.servlet.support.SpringBootServletInitializer;
import org.springframework.context.annotation.ImportResource;

/**
 * 启动类
 *
 * @author devezhao
 * @since 2020/9/22
 */
@SpringBootApplication(scanBasePackages = {"com.rebuild"}, exclude = {
        DataSourceAutoConfiguration.class,
        JdbcRepositoriesAutoConfiguration.class,
        JdbcTemplateAutoConfiguration.class,
        RedisAutoConfiguration.class,
        CacheAutoConfiguration.class })
@ImportResource("classpath:application-bean.xml")
public class BootApplication extends SpringBootServletInitializer {

    private static final Logger LOG = LoggerFactory.getLogger(RebuildConfiguration.class);

    private static boolean DEBUG = false;

    static {
        if (devMode()) System.setProperty("spring.profiles.active", "dev");
    }

    // 独立 JAR 方式（需要修改 pom.xml）
    public static void main(String[] args) {
        LOG.info("Initializing SpringBoot context ({}) ...", devMode() ? "dev" : "prodution");
        SpringApplication spring = new SpringApplication(BootApplication.class);
        spring.setBannerMode(Banner.Mode.OFF);
        spring.addListeners(new Application());

        DEBUG = args.length > 0 && args[0].contains("rbdev=true");
        if (DEBUG) spring.setWebApplicationType(WebApplicationType.NONE);

        spring.run(args);
    }

    // 外置 TOMCAT
    @Override
    protected SpringApplicationBuilder configure(SpringApplicationBuilder builder) {
        LOG.info("Initializing SpringBoot context ({}) ...", devMode() ? "dev" : "prodution");
        SpringApplicationBuilder spring = builder.sources(BootApplication.class);
        spring.bannerMode(Banner.Mode.OFF);
        spring.listeners(new Application());
        return spring;
    }

    public static boolean devMode() {
        return DEBUG || BooleanUtils.toBoolean(System.getProperty("rbdev"));
    }
}
