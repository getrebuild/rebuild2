/*
Copyright (c) REBUILD <https://getrebuild.com/> and/or its owners. All rights reserved.

rebuild is dual-licensed under commercial and open source licenses (GPLv3).
See LICENSE and COMMERCIAL in the project root for license information.
*/

package com.rebuild.core.support;

import com.alibaba.fastjson.JSON;
import com.alibaba.fastjson.JSONArray;
import com.rebuild.utils.JSONUtils;
import org.apache.commons.io.FileUtils;
import org.apache.commons.lang.ArrayUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.util.ResourceUtils;

import java.io.File;
import java.io.IOException;

/**
 * 黑名单词 src/main/resources/blacklist.json
 * More details https://github.com/fighting41love/funNLP
 *
 * @author devezhao
 * @since 01/31/2019
 */
public class BlackList {

    private static final Logger LOG = LoggerFactory.getLogger(BlackList.class);

    private static JSONArray BLACKLIST = null;

    /**
     * 是否黑名单关键词
     *
     * @param text
     * @return
     */
    public static boolean isBlacked(String text) {
        if (BLACKLIST == null) {
            try {
                File file = ResourceUtils.getFile("classpath:blacklist.json");
                String s = FileUtils.readFileToString(file, "UTF-8");
                BLACKLIST = JSON.parseArray(s);

            } catch (IOException e) {
                LOG.error("Couldn't load [blacklist.json] file! This feature is missed : " + e);
                BLACKLIST = JSONUtils.EMPTY_ARRAY;
            }
        }

        return BLACKLIST.contains(text.toLowerCase())
                || isSqlKeyword(text);
    }

    /**
     * 是否 SQL 关键词
     *
     * @param text
     * @return
     */
    public static boolean isSqlKeyword(String text) {
        return ArrayUtils.contains(SQL_KWS, text.toUpperCase());
    }

    // SQL 关键字
    private static final String[] SQL_KWS = new String[]{
            "SELECT", "DISTINCT", "MAX", "MIN", "AVG", "SUM", "COUNT", "FROM",
            "WHERE", "AND", "OR", "ORDER", "BY", "ASC", "DESC", "GROUP", "HAVING",
            "WITH", "ROLLUP", "IS", "NOT", "NULL", "IN", "LIKE", "EXISTS", "BETWEEN", "TRUE", "FALSE"
    };
}
