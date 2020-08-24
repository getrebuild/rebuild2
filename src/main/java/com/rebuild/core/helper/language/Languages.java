/*
Copyright (c) REBUILD <https://getrebuild.com/> and its owners. All rights reserved.

rebuild is dual-licensed under commercial and open source licenses (GPLv3).
See LICENSE and COMMERCIAL in the project root for license information.
*/

package com.rebuild.core.helper.language;

import com.alibaba.fastjson.JSON;
import com.alibaba.fastjson.JSONObject;
import com.rebuild.core.RebuildApplication;
import com.rebuild.core.RebuildException;
import com.rebuild.core.helper.ConfigurableItem;
import com.rebuild.core.helper.RebuildConfiguration;
import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.springframework.util.ResourceUtils;

import java.io.File;
import java.io.FileInputStream;
import java.io.InputStream;
import java.util.HashMap;
import java.util.Locale;
import java.util.Map;
import java.util.Objects;

/**
 * 多语言
 *
 * @author ZHAO
 * @since 2019/10/31
 */
public class Languages {

    private static final Log LOG = LogFactory.getLog(Languages.class);

    public static final Languages instance = new Languages();

    private Languages() {
        this.reset();
    }

    /**
     * 语言文件前缀
     */
    private static final String LB_PREFIX = "language_";

    private Map<String, LanguageBundle> bundleMap = new HashMap<>();

    /**
     *
     */
    public void reset() {
        try {
            File[] files = ResourceUtils.getFile("classpath:locales/")
                    .listFiles((dir, name) -> name.startsWith(LB_PREFIX) && name.endsWith(".json"));
            for (File file : Objects.requireNonNull(files)) {
                String locale = file.getName().substring(LB_PREFIX.length());
                locale = locale.split("\\.")[0];

                try (InputStream is = new FileInputStream(file)) {
                    LOG.info("Loading language bundle : " + locale);
                    JSONObject o = JSON.parseObject(is, null);
                    bundleMap.remove(locale);
                    bundleMap.put(locale, new LanguageBundle(locale, o, this));
                }
            }
        } catch (Exception ex) {
            throw new RebuildException("Load language bundle failure!", ex);
        }
    }

    /**
     * @param locale
     * @return
     */
    public LanguageBundle getBundle(Locale locale) {
        return getBundle(locale == null ? null : locale.toString());
    }

    /**
     * @param locale
     * @return
     */
    public LanguageBundle getBundle(String locale) {
        if (locale != null) {
            locale = locale.replace("_", "-");
            if (bundleMap.containsKey(locale)) {
                return bundleMap.get(locale);
            }

            locale = useCode(locale);
            if (locale != null) {
                return bundleMap.get(locale);
            }
        }
        return getDefaultBundle();
    }

    /**
     * 默认语言包
     *
     * @return
     * @see ConfigurableItem#DefaultLanguage
     */
    public LanguageBundle getDefaultBundle() {
        return bundleMap.get(RebuildConfiguration.get(ConfigurableItem.DefaultLanguage));
    }

    /**
     * 当前用户语言包
     *
     * @return
     */
    public LanguageBundle getCurrentBundle() {
        return getBundle(RebuildApplication.getSessionStore().getLocale());
    }

    /**
     * 是否为可用语言
     *
     * @param locale
     * @return
     */
    public boolean isAvailable(String locale) {
        locale = locale.replace("_", "-");
        boolean a = bundleMap.containsKey(locale);
        if (!a && useCode(locale) != null) {
            return true;
        }
        return a;
    }

    /**
     * @param locale
     * @return
     */
    private String useCode(String locale) {
        String code = locale.split("-")[0];
        for (String key : bundleMap.keySet()) {
            if (key.equals(code) || key.startsWith(code)) {
                return key;
            }
        }
        return null;
    }

    // -- Quick Methods

    /**
     * @param key
     * @param insideKeys
     * @return
     * @see #currentBundle()
     */
    public static String lang(String key, String... insideKeys) {
        return currentBundle().lang(key, insideKeys);
    }

    /**
     * 当前用户语言
     *
     * @return
     */
    public static LanguageBundle currentBundle() {
        return instance.getCurrentBundle();
    }

    /**
     * 默认语言
     *
     * @return
     */
    public static LanguageBundle defaultBundle() {
        return instance.getDefaultBundle();
    }
}
