package com.rebuild.core;

import com.rebuild.core.helper.ConfigurableItem;
import com.rebuild.core.helper.setup.InstallState;
import com.rebuild.utils.AES;
import org.apache.commons.lang.StringUtils;
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

@Component
public class RebuildEnvironmentPostProcessor implements EnvironmentPostProcessor, InstallState {

    private static ConfigurableEnvironment ENV;

    @Override
    public void postProcessEnvironment(ConfigurableEnvironment environment, SpringApplication application) {
        // 从安装文件
        File file = getInstallFile();
        if (file != null && file.exists()) {
            RebuildApplication.LOG.info("Loading file of install : " + file);

            try {
                Properties ps = PropertiesLoaderUtils.loadProperties(new FileSystemResource(file));
                aesDecrypt(ps);
                PropertiesPropertySource propertySource = new PropertiesPropertySource(".rebuild", ps);
                environment.getPropertySources().addLast(propertySource);

            } catch (IOException ex) {
                throw new IllegalStateException("Load file of install failed : " + file.toString(), ex);
            }
        }

        // 解密
        Properties superlativeProperties = new Properties();
        for (ConfigurableItem item : ConfigurableItem.values()) {
            String name = item.name();
            String value = environment.getProperty(name);
            if (StringUtils.isNotBlank(value)) {
                superlativeProperties.put(name, value);
            }
        }
        aesDecrypt(superlativeProperties);
        PropertiesPropertySource propertySource = new PropertiesPropertySource(".rebuild", superlativeProperties);
        environment.getPropertySources().addLast(propertySource);

        ENV = environment;
    }

    /**
     * 解密配置 `AES(xxx)`
     *
     * @param env
     * @see AES
     */
    private void aesDecrypt(Properties env) {
        for (String name : env.stringPropertyNames()) {
            String value = env.getProperty(name);
            if ((value.startsWith("AES(") || value.startsWith("aes(")) && value.endsWith(")")) {
                value = value.substring(4, value.length() - 1);
                value = AES.decryptQuietly(value);
                env.put(name, value);
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
        String value;
        if (ENV == null && ConfigurableItem.DataDirectory.name().equalsIgnoreCase(name)) {
            value = System.getProperty("DataDirectory");
        } else {
            value = ENV == null ? null : ENV.getProperty(name);
        }
        return StringUtils.defaultIfBlank(value, defaultValue);
    }
}
