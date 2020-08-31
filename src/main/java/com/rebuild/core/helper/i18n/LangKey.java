/*
Copyright (c) REBUILD <https://getrebuild.com/> and its owners. All rights reserved.

rebuild is dual-licensed under commercial and open source licenses (GPLv3).
See LICENSE and COMMERCIAL in the project root for license information.
*/

package com.rebuild.core.helper.i18n;

/**
 * @author ZHAO
 * @since 2020/08/31
 */
public class LangKey {

    protected final String key;

    public LangKey(String key) {
        this.key = key;
    }

    /**
     * @param key
     * @return
     */
    public static LangKey valueOf(String key) {
        return new LangKey(key);
    }

    /**
     * @param fieldName
     * @return
     */
    public static LangKey field(String fieldName) {
        return new LangKey("field." + fieldName);
    }

    /**
     * @param entityName
     * @return
     */
    public static LangKey entity(String entityName) {
        return new LangKey("entity." + entityName);
    }
}
