/*
Copyright (c) REBUILD <https://getrebuild.com/> and/or its owners. All rights reserved.

rebuild is dual-licensed under commercial and open source licenses (GPLv3).
See LICENSE and COMMERCIAL in the project root for license information.
*/

package com.rebuild.core.support.i18n;

import cn.devezhao.persist4j.Entity;
import cn.devezhao.persist4j.Field;
import cn.devezhao.persist4j.metadata.BaseMeta;
import com.alibaba.fastjson.JSON;
import com.alibaba.fastjson.JSONObject;
import com.rebuild.core.Application;
import com.rebuild.core.Initialization;
import com.rebuild.core.RebuildException;
import com.rebuild.core.metadata.MetadataHelper;
import com.rebuild.core.support.ConfigurationItem;
import com.rebuild.core.support.RebuildConfiguration;
import com.rebuild.core.support.state.StateSpec;
import com.rebuild.web.OnlineSessionStore;
import org.apache.commons.lang.StringUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;
import org.springframework.util.ResourceUtils;

import java.io.File;
import java.io.FileInputStream;
import java.io.InputStream;
import java.util.HashMap;
import java.util.Map;
import java.util.Objects;
import java.util.Set;

/**
 * 多语言
 *
 * @author ZHAO
 * @since 2019/10/31
 */
@Component
public class Language implements Initialization {

    private static final Logger LOG = LoggerFactory.getLogger(Language.class);

    private static final String LB_PREFIX = "language.";

    private static final String LB_SUFFIX = ".json";

    private Map<String, LanguageBundle> bundleMap = new HashMap<>();

    @Override
    public void init() throws Exception {
        File[] files = ResourceUtils.getFile("classpath:i18n/")
                .listFiles((dir, name) -> name.startsWith(LB_PREFIX) && name.endsWith(LB_SUFFIX));

        for (File file : Objects.requireNonNull(files)) {
            String locale = file.getName().substring(LB_PREFIX.length());
            locale = locale.substring(0, locale.length() - 5);
            LOG.info("Loading language bundle : " + locale);

            try (InputStream is = new FileInputStream(file)) {
                JSONObject o = JSON.parseObject(is, null);
                LanguageBundle bundle = new LanguageBundle(locale, o, this);
                bundleMap.put(locale, bundle);
            }
        }
    }

    /**
     * @param locale
     * @return
     * @see java.util.Locale
     */
    public LanguageBundle getBundle(String locale) {
        if (locale != null) {
            if (bundleMap.containsKey(locale)) {
                return bundleMap.get(locale);
            }

            locale = useLanguageCode(locale);
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
     */
    public LanguageBundle getDefaultBundle() {
        String d = RebuildConfiguration.get(ConfigurationItem.DefaultLanguage);
        if (!available(d)) {
            throw new RebuildException("No default locale found : " + d);
        }
        return bundleMap.get(d);
    }

    /**
     * @param locale
     * @return
     */
    private String useLanguageCode(String locale) {
        String code = locale.split("[_-]")[0];
        for (String key : bundleMap.keySet()) {
            if (key.equals(code) || key.startsWith(code)) {
                return key;
            }
        }
        return null;
    }

    /**
     * 是否为可用语言
     *
     * @param locale
     * @return
     */
    public boolean available(String locale) {
        boolean a = bundleMap.containsKey(locale);
        if (!a && useLanguageCode(locale) != null) {
            return true;
        }
        return a;
    }

    /**
     * @return
     */
    public Set<String> availableList() {
        return bundleMap.keySet();
    }

    // -- Quick Methods

    /**
     * 当前用户语言包（线程量用户）
     *
     * @return
     * @see OnlineSessionStore#getLocale()
     */
    public static LanguageBundle getCurrentBundle() {
        return Application.getLanguage().getBundle(Application.getSessionStore().getLocale());
    }

    /**
     * @param key
     * @param phKeys 可替换语言 Key 中的 {0} {1}
     * @return
     */
    public static String getLang(String key, String... phKeys) {
        return getCurrentBundle().getLang(key, phKeys);
    }

    /**
     * @param key
     * @param phValues 可格式化语言 Key 中的 %s %d
     * @return
     */
    public static String formatLang(String key, Object... phValues) {
        return getCurrentBundle().formatLang(key, phValues);
    }

    /**
     * 元数据语言
     *
     * @param entityOrField
     * @return
     */
    public static String getLang(BaseMeta entityOrField) {
        String langKey;
        if (entityOrField instanceof Entity) {
            langKey = "e." + entityOrField.getName();
        } else {
            Field field = (Field) entityOrField;
            if (MetadataHelper.isCommonsField(field)) {
                langKey = "f." + field.getName();
            } else {
                langKey = "f." + field.getOwnEntity().getName() + "." + field.getName();
            }
        }

        return StringUtils.defaultIfBlank(
                getCurrentBundle().getLangBase(langKey), entityOrField.getDescription());
    }

    /**
     * 状态语言
     *
     * @param state
     * @return
     */
    public static String getLang(StateSpec state) {
        String langKey = "s." + state.getClass().getSimpleName() + "." + ((Enum<?>) state).name();
        return StringUtils.defaultIfBlank(getCurrentBundle().getLangBase(langKey), state.getName());
    }
}
