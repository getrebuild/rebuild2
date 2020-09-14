/*
Copyright (c) REBUILD <https://getrebuild.com/> and/or its owners. All rights reserved.

rebuild is dual-licensed under commercial and open source licenses (GPLv3).
See LICENSE and COMMERCIAL in the project root for license information.
*/

package com.rebuild.core.support.i18n;

import cn.devezhao.commons.EncryptUtils;
import com.alibaba.fastjson.JSON;
import com.alibaba.fastjson.JSONObject;
import com.rebuild.utils.AppUtils;
import com.rebuild.utils.JSONable;
import org.apache.commons.lang.ArrayUtils;
import org.apache.commons.lang.StringUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.text.MessageFormat;
import java.util.Locale;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * 语言包
 * 为安全考虑语言文件不支持 HTML（会被转义），但支持部分 MD 语法：
 * - [] 换行 <br>
 * - [TEXT](URL) 链接
 *
 * @author ZHAO
 * @since 2019/10/31
 */
public class LanguageBundle implements JSONable {

    private static final Logger LOG = LoggerFactory.getLogger(LanguageBundle.class);

    // 链接
    private static final Pattern LINK_PATT = Pattern.compile("\\[(.*?)]\\((.*?)\\)");
    // 换行
    private static final Pattern BR_PATT = Pattern.compile("\\[]");
    // 加粗
    private static final Pattern BOLD_PATT = Pattern.compile("\\*\\*(.*?)\\*\\*");

    private String locale;
    private JSONObject bundle;
    private String bundleHash;

    private Language parent;

    /**
     * @param locale
     * @param bundle
     * @param parent
     */
    protected LanguageBundle(String locale, JSONObject bundle, Language parent) {
        this.locale = locale;
        this.parent = parent;
        this.bundle = this.merge(bundle);
    }

    /**
     * 合并语言
     *
     * @param bundle
     * @return
     */
    private JSONObject merge(JSONObject bundle) {
        String bundleString = bundle.toJSONString();

        bundleString = BR_PATT.matcher(bundleString).replaceAll("<br/>");

        Matcher matcher = LINK_PATT.matcher(bundleString);
        while (matcher.find()) {
            String text = matcher.group(1);
            String url = matcher.group(2);

            String link = "<a href='%s'>%s</a>";
            if (url.startsWith("http:") || url.startsWith("https:")) {
                link = "<a target='_blank' href='%s'>%s</a>";
            } else if (url.startsWith("/")) {
                link = "<a href='" + AppUtils.getContextPath() + "%s'>%s</a>";
            }

            bundleString = bundleString.replace(
                    String.format("[%s](%s)", text, url),
                    String.format(link, url, text));
        }

        matcher = BOLD_PATT.matcher(bundleString);
        while (matcher.find()) {
            String text = matcher.group(1);
            String bold = "<b>%s</b>";
            bundleString = bundleString.replace(String.format("**%s**", text), String.format(bold, text));
        }

        this.bundleHash = EncryptUtils.toMD5Hex(bundleString);
        return JSON.parseObject(bundleString);
    }

    /**
     * @return
     * @see Locale#forLanguageTag(String)
     */
    public String getLocale() {
        return locale;
    }

    /**
     * @return
     */
    public String getBundleHash() {
        return bundleHash;
    }

    /**
     * @param key
     * @param phValues
     * @return
     * @see String#format(String, Object...)
     */
    public String formatLang(String key, Object... phValues) {
        return String.format(getLang(key), phValues);
    }

    /**
     * @param key
     * @return
     */
    public String getLang(String key, String... phKeys) {
        String lang = getLangBase(key);
        if (lang == null && parent != null) {
            lang = parent.getDefaultBundle().getLangBase(key);
        }

        if (lang == null) {
            LOG.warn("Missing lang-key `{}` for `{}`", key, getLocale());
            return String.format("[%s]", key.toUpperCase());
        }

        if (phKeys.length > 0) {
            Object[] phLangs = new Object[phKeys.length];
            for (int i = 0; i < phKeys.length; i++) {
                phLangs[i] = getLang(phKeys[i]);
            }
            return MessageFormat.format(lang, phLangs);
        } else {
            return lang;
        }
    }

    /**
     * 直接获取不做任何加工处理
     *
     * @param key
     * @return
     */
    public String getLangBase(String key) {
        return bundle.getString(key);
    }

    /**
     * for client short
     *
     * @param mixkey
     * @return
     * @see #getLang(String, String...)
     */
    public String lang(String mixkey) {
        if (mixkey.contains(",")) {
            String[] keys = mixkey.split(",");
            String[] phKeys = (String[]) ArrayUtils.subarray(keys, 1, keys.length);
            return getLang(keys[0], phKeys);
        } else {
            return getLang(mixkey);
        }
    }

    @Override
    public JSON toJSON() {
        return bundle;
    }

    @Override
    public String toString() {
        return super.toString() + "#" + getLocale() + ":" + bundle.size();
    }
}
