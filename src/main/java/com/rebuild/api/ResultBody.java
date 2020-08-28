/*
Copyright (c) REBUILD <https://getrebuild.com/> and/or its owners. All rights reserved.

rebuild is dual-licensed under commercial and open source licenses (GPLv3).
See LICENSE and COMMERCIAL in the project root for license information.
*/

package com.rebuild.api;

import com.alibaba.fastjson.JSON;
import com.alibaba.fastjson.JSONObject;
import com.rebuild.utils.JSONUtils;
import com.rebuild.utils.JSONable;
import org.apache.commons.lang.StringUtils;

/**
 * 请求返回消息
 *
 * @author ZHAO
 * @since 2020/8/28
 */
public class ResultBody implements JSONable {

    private int errorCode;
    private String errorMsg;
    private Object data;

    public ResultBody(int errorCode, String errorMsg, Object data) {
        this.errorCode = errorCode;
        this.errorMsg = errorMsg;
        this.data = data;
    }

    @Override
    public JSON toJSON() {
        JSONObject result = JSONUtils.toJSONObject(
                new String[]{"error_code", "error_msg"}, new Object[]{errorCode, errorMsg});
        if (data != null) {
            result.put("data", data);
        }
        return result;
    }

    @Override
    public String toString() {
        return toJSON().toString();
    }

    // --

    public static ResultBody error(String errorMsg) {
        return error(errorMsg, Controller.CODE_ERROR);
    }

    public static ResultBody error(String errorMsg, int errorCode) {
        return new ResultBody(errorCode, StringUtils.defaultIfBlank(errorMsg, "系统繁忙，请稍后重试"), null);
    }

    public static ResultBody ok() {
        return ok(null);
    }

    public static ResultBody ok(Object data) {
        return new ResultBody(Controller.CODE_OK, "调用成功", data);
    }
}
