/*
Copyright (c) REBUILD <https://getrebuild.com/> and its owners. All rights reserved.

rebuild is dual-licensed under commercial and open source licenses (GPLv3).
See LICENSE and COMMERCIAL in the project root for license information.
*/

package com.rebuild.core.helper.i18n;

import cn.devezhao.commons.EncryptUtils;
import com.alibaba.fastjson.JSON;
import com.alibaba.fastjson.JSONObject;
import com.rebuild.utils.JSONable;

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

    final private String locale;
    final private JSONObject bundle;
    private String bundleHash;

    final private Language parent;

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

    private static final Pattern VARS_PATT = Pattern.compile("\\{([0-9a-zA-Z]+)}");
    private static final Pattern LINK_PATT = Pattern.compile("\\[(.*?)]\\((.*?)\\)");
    /**
     * 合并语言
     *
     * @param bundle
     * @return
     */
    private JSONObject merge(JSONObject bundle) {
        // 变量
        String bundleString = bundle.toJSONString();
        Matcher matcher = VARS_PATT.matcher(bundleString);
        while (matcher.find()) {
            String var = matcher.group(1);
            String lang = bundle.getString(var);
            if (lang != null) {
                bundleString = bundleString.replace("{" + var +"}", lang);
            }
        }

        // 换行
        bundleString = bundleString.replaceAll("\\[]", "<br/>");

        // 链接
        matcher = LINK_PATT.matcher(bundleString);
        while (matcher.find()) {
            String text = matcher.group(1);
            String url = matcher.group(2);

            bundleString = bundleString.replace(
                    String.format("[%s](%s)", text, url),
                    String.format("<a href='%s'>%s</a>", url, text));
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
     * @param args
     * @return
     * @see MessageFormat#format(String, Object...)
     */
    public String formatLang(String key, Object... args) {
        String lang = getLang(key);
        return MessageFormat.format(lang, args);
    }

    /**
     * @param key
     * @return
     */
    public String getLang(String key) {
        String lang = bundle.getString(key);
        if (lang == null && parent != null) {
            lang = parent.getDefaultBundle().getLang(key);
        }

        if (lang == null) {
            return String.format("[%s]", key.toUpperCase());
        } else {
            return lang;
        }
    }

    /**
     * @param key
     * @return
     */
    public String lang(String key) {
        return getLang(key);
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
