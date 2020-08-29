/*
Copyright (c) REBUILD <https://getrebuild.com/> and/or its owners. All rights reserved.

rebuild is dual-licensed under commercial and open source licenses (GPLv3).
See LICENSE and COMMERCIAL in the project root for license information.
*/

package com.rebuild.web.admin.setup;

import cn.devezhao.commons.ObjectUtils;
import cn.devezhao.commons.ThrowableUtils;
import cn.devezhao.commons.web.ServletUtils;
import com.alibaba.fastjson.JSONObject;
import com.rebuild.core.Application;
import com.rebuild.core.helper.ConfigurationItem;
import com.rebuild.core.helper.RebuildConfiguration;
import com.rebuild.core.helper.setup.InstallState;
import com.rebuild.core.helper.setup.Installer;
import com.rebuild.utils.JSONUtils;
import com.rebuild.web.BaseController;
import org.apache.commons.io.FileUtils;
import org.apache.commons.lang.StringUtils;
import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.servlet.ModelAndView;
import redis.clients.jedis.Jedis;
import redis.clients.jedis.JedisPool;
import redis.clients.jedis.JedisPoolConfig;

import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import java.io.File;
import java.io.IOException;
import java.sql.Connection;
import java.sql.DatabaseMetaData;
import java.sql.ResultSet;
import java.sql.SQLException;

/**
 * @author devezhao
 * @since 2019/11/25
 */
@Controller
@RequestMapping("/setup/")
public class SetupInstall extends BaseController implements InstallState {

    @RequestMapping("install")
    public ModelAndView pageIndex(HttpServletResponse response) throws IOException {
        if (Application.serversReady() && !Application.devMode()) {
            response.sendError(404);
            return null;
        }

        ModelAndView mv = new ModelAndView("/setup/install.jsp");
        mv.getModel().put("defaultDataDirectory", RebuildConfiguration.getFileOfData(null).getAbsolutePath().replace("\\", "/"));
        mv.getModel().put("defaultAppName", RebuildConfiguration.get(ConfigurationItem.AppName));
        mv.getModel().put("defaultHomeURL", RebuildConfiguration.get(ConfigurationItem.HomeURL));
        return mv;
    }

    @RequestMapping("test-connection")
    public void testConnection(HttpServletRequest request, HttpServletResponse response) throws IOException {
        JSONObject dbProps = (JSONObject) ServletUtils.getRequestJson(request);
        JSONObject props = JSONUtils.toJSONObject("databaseProps", dbProps);

        try (Connection conn = new Installer(props).getConnection(null)) {
            DatabaseMetaData dmd = conn.getMetaData();
            String msg = String.format("连接成功 : %s %s", dmd.getDatabaseProductName(), dmd.getDatabaseProductVersion());

            // 查询表
            try (ResultSet rs = dmd.getTables(null, null, null, new String[]{"TABLE"})) {
                if (rs.next()) {
                    String hasTable = rs.getString("TABLE_NAME");
                    if (hasTable != null) {
                        msg += " (非空数据库，可能导致安装失败)";
                    }
                }
            } catch (SQLException ignored) {
            }

            writeSuccess(response, msg);
        } catch (SQLException e) {
            if (e.getLocalizedMessage().contains("Unknown database")) {
                writeSuccess(response, "连接成功 : 数据库不存在，系统将自动创建");
            } else {
                writeFailure(response, "连接错误 : " + e.getLocalizedMessage());
            }
        }
    }

    @RequestMapping("test-directory")
    public void testDirectory(HttpServletRequest request, HttpServletResponse response) throws IOException {
        String dir = getParameterNotNull(request, "dir");
        File file = new File(dir);
        if (file.exists()) {
            if (!file.isDirectory()) {
                file = null;
            }
        } else {
            try {
                FileUtils.forceMkdir(file);
                if (file.exists()) {
                    FileUtils.deleteDirectory(file);
                } else {
                    file = null;
                }
            } catch (IOException ex) {
                file = null;
            }
        }

        if (file == null) {
            writeFailure(response);
        } else {
            writeSuccess(response, file.getAbsolutePath());
        }
    }

    @RequestMapping("test-cache")
    public void testCache(HttpServletRequest request, HttpServletResponse response) throws IOException {
        JSONObject cacheProps = (JSONObject) ServletUtils.getRequestJson(request);

        JedisPool pool = new JedisPool(new JedisPoolConfig(),
                StringUtils.defaultIfBlank(cacheProps.getString("CacheHost"), "127.0.0.1"),
                ObjectUtils.toInt(cacheProps.getString("CachePort"), 6379),
                3000,
                StringUtils.defaultIfBlank(cacheProps.getString("CachePassword"), null));
        try (Jedis client = pool.getResource()) {
            String info = client.info("server");
            if (info.length() > 80) {
                info = info.substring(0, 80) + "...";
            }
            pool.destroy();
            writeSuccess(response, "连接成功 : " + info);
        } catch (Exception ex) {
            writeFailure(response, "连接失败 : " + ThrowableUtils.getRootCause(ex).getLocalizedMessage());
        }
    }

    @RequestMapping("install-rebuild")
    public void installExec(HttpServletRequest request, HttpServletResponse response) throws IOException {
        JSONObject installProps = (JSONObject) ServletUtils.getRequestJson(request);
        try {
            new Installer(installProps).install();
            writeSuccess(response);
        } catch (Exception e) {
            e.printStackTrace();
            writeFailure(response, "出现错误 : " + e.getLocalizedMessage());
        }
    }
}
