/*
Copyright (c) REBUILD <https://getrebuild.com/> and/or its owners. All rights reserved.

rebuild is dual-licensed under commercial and open source licenses (GPLv3).
See LICENSE and COMMERCIAL in the project root for license information.
*/

package com.rebuild.core;

import cn.devezhao.persist4j.PersistManagerFactory;
import cn.devezhao.persist4j.Query;
import com.rebuild.core.service.CommonsService;
import com.rebuild.core.service.SqlExecutor;
import com.rebuild.core.service.query.QueryFactory;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.context.ConfigurableApplicationContext;
import org.springframework.context.annotation.ImportResource;
import org.springframework.context.annotation.Lazy;

/**
 * @author devezhao
 * @since 2020/07/29
 */
@SpringBootApplication
@ImportResource("classpath:application-bean.xml")
@Lazy
public class Application {

    private static final Logger LOGGER = LoggerFactory.getLogger(Application.class);

    private static ConfigurableApplicationContext applicationContext;

    public static void main(String[] args) {
        applicationContext = SpringApplication.run(Application.class, args);
    }

    public static Logger getLogger() {
        return LOGGER;
    }

    public static ConfigurableApplicationContext getApplicationContext() {
        if (applicationContext == null) throw new RebuildException("Rebuild unstarted");
        return applicationContext;
    }

    public static <T> T getBean(Class<T> clazz) {
        return getApplicationContext().getBean(clazz);
    }

    public static Object getBean(String name) {
        return getApplicationContext().getBean(name);
    }

    public static PersistManagerFactory getPersistManagerFactory() {
        return getBean(PersistManagerFactory.class);
    }

    public static SqlExecutor getSQLExecutor() {
        return getBean(SqlExecutor.class);
    }

    public static QueryFactory getQueryFactory() {
        return getBean(QueryFactory.class);
    }

    public static Query createQuery(String ajql) {
        return getQueryFactory().createQuery(ajql);
    }

    public static Query createQueryNoFilter(String ajql) {
        return getQueryFactory().createQueryNoFilter(ajql);
    }

    public static CommonsService getCommonsService() {
        return getBean(CommonsService.class);
    }

}
