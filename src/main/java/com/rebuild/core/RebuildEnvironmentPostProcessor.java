/*
Copyright (c) REBUILD <https://getrebuild.com/> and/or its owners. All rights reserved.

rebuild is dual-licensed under commercial and open source licenses (GPLv3).
See LICENSE and COMMERCIAL in the project root for license information.
*/

package com.rebuild.core;

import com.rebuild.core.helper.ConfigurationItem;
import com.rebuild.core.helper.setup.InstallState;
import com.rebuild.utils.AES;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.env.EnvironmentPostProcessor;
import org.springframework.core.env.ConfigurableEnvironment;
import org.springframework.core.env.PropertiesPropertySource;
import org.springframework.core.io.FileSystemResource;
import org.springframework.core.io.support.PropertiesLoaderUtils;
import org.springframework.stereotype.Component;

import java.io.File;
import java.io.IOException;
import java.util.Properties;

/**
 * @author zhaofang123@gmail.com
 * @since 08/28/2020
 */
@Component
public class RebuildEnvironmentPostProcessor implements EnvironmentPostProcessor, InstallState {

    private static final String V2_PREFIX = "rebuild.";

    private static ConfigurableEnvironment ENV_HOLD;

    @Override
    public void postProcessEnvironment(ConfigurableEnvironment environment, SpringApplication application) {
        // 从安装文件
        File file = getInstallFile();
        if (file != null && file.exists()) {
            Application.LOGGER.info("Loading file of install : " + file);

            try {
                Properties temp = PropertiesLoaderUtils.loadProperties(new FileSystemResource(file));
                Properties ps = new Properties();
                // 兼容 V1
                for (String name : temp.stringPropertyNames()) {
                    String value = temp.getProperty(name);
                    if (name.startsWith("db.") || name.startsWith("rebuild.")) {
                        ps.put(name, value);
                    } else {
                        ps.put(V2_PREFIX + name, value);
                    }
                }

                aesDecrypt(ps);
                PropertiesPropertySource installedSource = new PropertiesPropertySource(".rebuild", temp);
                environment.getPropertySources().addFirst(installedSource);

            } catch (IOException ex) {
                throw new IllegalStateException("Load file of install failed : " + file.toString(), ex);
            }
        }

        // 解密
        Properties decryptedProperties = new Properties();
        for (ConfigurationItem item : ConfigurationItem.values()) {
            String name = V2_PREFIX + item.name();
            String value = environment.getProperty(name);
            if (value != null) {
                decryptedProperties.put(name, value);
            }
        }
        aesDecrypt(decryptedProperties);
        PropertiesPropertySource propertySource = new PropertiesPropertySource(".decrypted", decryptedProperties);
        environment.getPropertySources().addFirst(propertySource);

        ENV_HOLD = environment;
    }

    /**
     * 解密配置 `AES(xxx)`
     *
     * @param ps
     * @see AES
     */
    private void aesDecrypt(Properties ps) {
        for (String name : ps.stringPropertyNames()) {
            String value = ps.getProperty(name);
            if ((value.startsWith("AES(") || value.startsWith("aes(")) && value.endsWith(")")) {
                value = value.substring(4, value.length() - 1);
                value = AES.decryptQuietly(value);
                ps.put(name, value);
            }
        }
    }

    /**
     * @param name
     * @return
     */
    public static String getProperty(String name) {
        return getProperty(name, null);
    }

    /**
     * @param name
     * @param defaultValue
     * @return
     */
    public static String getProperty(String name, String defaultValue) {
        String value = null;
        if (ENV_HOLD == null && ConfigurationItem.DataDirectory.name().equalsIgnoreCase(name)) {
            value = System.getProperty("DataDirectory");
        } else if (ENV_HOLD != null) {
            if (!(name.startsWith(V2_PREFIX) || name.contains("."))) {
                name = V2_PREFIX + name;
            }
            value = ENV_HOLD.getProperty(name);
        }
        return value == null ? defaultValue : value;
    }
}
