/*
Copyright (c) REBUILD <https://getrebuild.com/> and/or its owners. All rights reserved.

rebuild is dual-licensed under commercial and open source licenses (GPLv3).
See LICENSE and COMMERCIAL in the project root for license information.
*/

package com.rebuild.api;

import cn.devezhao.persist4j.Record;
import com.alibaba.fastjson.JSONObject;
import org.apache.commons.lang.StringUtils;
import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;

import java.util.HashMap;
import java.util.Iterator;
import java.util.Map;

/**
 * 响应前端/外部请求
 *
 * @author devezhao
 * @since 01/10/2019
 */
public abstract class Controller {

    // OK
    public static final int CODE_OK = 0;
    // 错误
    public static final int CODE_ERROR = 400;

    /**
     * Logging
     */
    protected static final Log LOG = LogFactory.getLog(Controller.class);

    /**
     * @param data
     * @return
     */
    protected JSONObject formatSuccess(Object data) {
        JSONObject map = new JSONObject();
        map.put("error_code", CODE_OK);
        map.put("error_msg", "调用成功");
        if (data != null) {
            if (Record.class.isAssignableFrom(data.getClass())) {
                Record record = (Record) data;
                Map<String, Object> recordMap = new HashMap<>();
                for (Iterator<String> iter = ((Record) data).getAvailableFieldIterator(); iter.hasNext(); ) {
                    String field = iter.next();
                    recordMap.put(field, record.getObjectValue(field));
                }
                data = recordMap;
            }
            map.put("data", data);
        }
        return map;
    }

    /**
     * @param errorMsg
     * @return
     */
    protected JSONObject formatFailure(String errorMsg) {
        return formatFailure(errorMsg, CODE_ERROR);
    }

    /**
     * @param errorMsg
     * @param errorCode
     * @return
     */
    protected JSONObject formatFailure(String errorMsg, int errorCode) {
        JSONObject map = new JSONObject();
        map.put("error_code", errorCode);
        map.put("error_msg", StringUtils.defaultIfBlank(errorMsg, "系統繁忙，请稍后重试"));
        return map;
    }
}
