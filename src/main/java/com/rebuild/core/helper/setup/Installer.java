/*
Copyright (c) REBUILD <https://getrebuild.com/> and/or its owners. All rights reserved.

rebuild is dual-licensed under commercial and open source licenses (GPLv3).
See LICENSE and COMMERCIAL in the project root for license information.
*/

package com.rebuild.core.helper.setup;

import cn.devezhao.commons.EncryptUtils;
import cn.devezhao.commons.sql.SqlBuilder;
import cn.devezhao.commons.sql.builder.UpdateBuilder;
import com.alibaba.fastjson.JSONObject;
import com.rebuild.core.RebuildApplication;
import com.rebuild.core.RebuildEnvironmentPostProcessor;
import com.rebuild.core.cache.RedisDriver;
import com.rebuild.core.helper.ConfigurationItem;
import com.rebuild.core.helper.License;
import com.rebuild.core.helper.RebuildConfiguration;
import com.rebuild.utils.AES;
import org.apache.commons.io.FileUtils;
import org.apache.commons.lang.StringUtils;
import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.springframework.util.Assert;
import org.springframework.util.ResourceUtils;
import redis.clients.jedis.Jedis;

import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.OutputStream;
import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.SQLException;
import java.sql.Statement;
import java.util.ArrayList;
import java.util.List;
import java.util.Properties;

/**
 * 系统安装
 *
 * @author devezhao
 * @since 2019/11/25
 */
public class Installer implements InstallState {

    private static final Log LOG = LogFactory.getLog(Installer.class);

    // 快速安装模式（H2 数据库）
    private boolean quickMode;

    private JSONObject installProps;

    private Installer() {
    }

    /**
     * @param installProps
     */
    public Installer(JSONObject installProps) {
        this.installProps = installProps;
        this.quickMode = installProps.getIntValue("installType") == 99;
    }

    /**
     * 执行安装
     *
     * @throws Exception
     */
    public void install() throws Exception {
        this.installDatabase();
        this.installAdmin();

        // Save install state (file)
        File dest = RebuildConfiguration.getFileOfData(INSTALL_FILE);
        Properties installProps = buildConnectionProps(null);
        // Redis
        JSONObject cacheProps = this.installProps.getJSONObject("cacheProps");
        if (cacheProps != null && !cacheProps.isEmpty()) {
            installProps.put(ConfigurationItem.CacheHost.name(), cacheProps.getString(ConfigurationItem.CacheHost.name()));
            installProps.put(ConfigurationItem.CachePort.name(), cacheProps.getString(ConfigurationItem.CachePort.name()));
            installProps.put(ConfigurationItem.CachePassword.name(), cacheProps.getString(ConfigurationItem.CachePassword.name()));
        }
        // 加密
        String dbPasswd = (String) installProps.remove("db.passwd");
        installProps.put("db.passwd.aes",
                StringUtils.isBlank(dbPasswd) ? StringUtils.EMPTY : AES.encrypt(dbPasswd));
        String cachePasswd = (String) installProps.remove(ConfigurationItem.CachePassword.name());
        installProps.put(ConfigurationItem.CachePassword.name() + ".aes",
                StringUtils.isBlank(cachePasswd) ? StringUtils.EMPTY : AES.encrypt(cachePasswd));

        try {
            FileUtils.deleteQuietly(dest);
            try (OutputStream os = new FileOutputStream(dest)) {
                installProps.store(os, "INSTALL FILE FOR REBUILD. DON'T DELETE OR MODIFY IT!!!");
                LOG.warn("Stored install file : " + dest);
            }

        } catch (IOException e) {
            throw new SetupException(e);
        }

        // re-init
        try {
            RebuildApplication.init(System.currentTimeMillis());
        } catch (Exception ex) {
            FileUtils.deleteQuietly(dest);
            throw ex;
        }

        // Gen SN
        License.SN();

        // Clean cached
        if (isUseRedis()) {
            try (Jedis jedis = RebuildApplication.getCommonsCache().getJedisPool().getResource()) {
                jedis.flushAll();
            }
        } else {
            RebuildApplication.getCommonsCache().getEhcacheCache().clear();
        }
    }

    /**
     * @param dbName
     * @return
     * @throws SQLException
     */
    public Connection getConnection(String dbName) throws SQLException {
        Properties props = this.buildConnectionProps(dbName);
        return DriverManager.getConnection(
                props.getProperty("db.url"), props.getProperty("db.user"), props.getProperty("db.passwd"));
    }

    /**
     * @param dbName
     * @return
     */
    private Properties buildConnectionProps(String dbName) {
        final JSONObject dbProps = installProps.getJSONObject("databaseProps");
        if (dbName == null) {
            dbName = dbProps == null ? null : dbProps.getString("dbName");
        }

        if (quickMode) {
            Properties props = new Properties();
            dbName = StringUtils.defaultIfBlank(dbName, "H2DB");
            File dbFile = RebuildConfiguration.getFileOfData(dbName);
            LOG.warn("Use H2 database : " + dbFile);

            props.put("db.url", String.format("jdbc:h2:file:%s;MODE=MYSQL;DATABASE_TO_LOWER=TRUE;IGNORECASE=TRUE",
                    dbFile.getAbsolutePath()));
            props.put("db.user", "rebuild");
            props.put("db.passwd", "rebuild");
            return props;
        }

        Assert.notNull(dbProps, "[databaseProps] must be null");
        String dbUrl = String.format(
                "jdbc:mysql://%s:%d/%s?useUnicode=true&characterEncoding=UTF8&zeroDateTimeBehavior=convertToNull&useSSL=false&sessionVariables=default_storage_engine=InnoDB",
                dbProps.getString("dbHost"),
                dbProps.getIntValue("dbPort"),
                dbName);
        String dbUser = dbProps.getString("dbUser");
        String dbPassword = dbProps.getString("dbPassword");

        // @see jdbc.properties
        Properties props = new Properties();
        props.put("db.url", dbUrl);
        props.put("db.user", dbUser);
        props.put("db.passwd", dbPassword);
        return props;
    }

    /**
     * 数据库
     */
    protected void installDatabase() {
        if (!quickMode) {
            // 创建数据库（如果需要）
            // noinspection EmptyTryBlock
            try (Connection ignored = getConnection(null)) {
                // NOOP
            } catch (SQLException e) {
                if (!e.getLocalizedMessage().contains("Unknown database")) {
                    throw new SetupException(e);
                }

                // 创建
                String createDb = String.format("CREATE DATABASE `%s` COLLATE utf8mb4_general_ci",
                        installProps.getJSONObject("databaseProps").getString("dbName"));
                try (Connection conn = getConnection("mysql")) {
                    try (Statement stmt = conn.createStatement()) {
                        stmt.executeUpdate(createDb);
                        LOG.warn("Database created : " + createDb);
                    }

                } catch (SQLException sqlex) {
                    throw new SetupException(sqlex);
                }
            }
        }

        // 初始化数据库
        try (Connection conn = getConnection(null)) {
            int affetced = 0;
            for (String sql : getDbInitScript()) {
                try (Statement stmt = conn.createStatement()) {
                    stmt.execute(sql);
                    affetced++;
                }
            }
            LOG.info("Schemes of database created : " + affetced);

        } catch (SQLException | IOException e) {
            throw new SetupException(e);
        }
    }

    /**
     * @return
     * @throws IOException
     */
    protected String[] getDbInitScript() throws IOException {
        File script = ResourceUtils.getFile("classpath:scripts/db-init.sql");
        List<?> LS = FileUtils.readLines(script, "utf-8");

        List<String> SQLS = new ArrayList<>();
        StringBuilder SQL = new StringBuilder();
        boolean ignoreTerms = false;
        for (Object L : LS) {
            String L2 = L.toString().trim();

            // NOTE double 字段也不支持
            boolean H2Unsupported = quickMode
                    && (L2.startsWith("fulltext ") || L2.startsWith("unique ") || L2.startsWith("index "));

            // Ignore comments and line of blank
            if (StringUtils.isEmpty(L2) || L2.startsWith("--") || H2Unsupported) {
                continue;
            }
            if (L2.startsWith("/*") || L2.endsWith("*/")) {
                ignoreTerms = L2.startsWith("/*");
                continue;
            } else if (ignoreTerms) {
                continue;
            }

            SQL.append(L2);
            if (L2.endsWith(";")) {  // SQL ends
                SQLS.add(SQL.toString().replace(",\n)Engine=", "\n)Engine="));
                SQL = new StringBuilder();
            } else {
                SQL.append('\n');
            }
        }
        return SQLS.toArray(new String[0]);
    }

    /**
     * 管理员
     */
    protected void installAdmin() {
        JSONObject adminProps = installProps.getJSONObject("adminProps");
        if (adminProps == null || adminProps.isEmpty()) {
            return;
        }

        String adminPasswd = adminProps.getString("adminPasswd");
        String adminMail = adminProps.getString("adminMail");

        UpdateBuilder ub = SqlBuilder.buildUpdate("user");
        if (StringUtils.isNotBlank(adminPasswd)) {
            ub.addColumn("PASSWORD", EncryptUtils.toSHA256Hex(adminPasswd));
        }
        if (StringUtils.isNotBlank(adminMail)) {
            ub.addColumn("EMAIL", adminMail);
        }
        if (!ub.hasColumn()) {
            return;
        }

        ub.setWhere("LOGIN_NAME = 'admin'");
        executeSql(ub.toSql());
    }

    /**
     * @param sql
     */
    private void executeSql(String sql) {
        try (Connection conn = getConnection(null)) {
            try (Statement stmt = conn.createStatement()) {
                stmt.execute(sql);
            }

        } catch (SQLException sqlex) {
            LOG.error("Couldn't execute SQL : " + sql, sqlex);
        }
    }

    // --

    /**
     * 是否 H2 数据库
     *
     * @return
     */
    public static boolean isUseH2() {
        String dbUrl = RebuildEnvironmentPostProcessor.getProperty("db.url");
        return dbUrl != null && dbUrl.startsWith("jdbc:h2:");
    }

    /**
     * @return
     */
    public static boolean isUseRedis() {
        return RebuildApplication.getCommonsCache().getCacheTemplate() instanceof RedisDriver;
    }

    /**
     * 是否已安装
     *
     * @return
     */
    public static boolean isInstalled() {
        return new Installer().checkInstalled();
    }
}
