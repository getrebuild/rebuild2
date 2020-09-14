/*
Copyright (c) REBUILD <https://getrebuild.com/> and/or its owners. All rights reserved.

rebuild is dual-licensed under commercial and open source licenses (GPLv3).
See LICENSE and COMMERCIAL in the project root for license information.
*/

package com.rebuild.api.metadata;

import com.alibaba.fastjson.JSONObject;
import com.rebuild.TestSupport;
import com.rebuild.api.ApiContext;
import com.rebuild.utils.JSONUtils;
import org.junit.Assert;
import org.junit.Test;

import java.util.HashMap;
import java.util.Map;

/**
 * @author devezhao
 * @since 2020/5/14
 */
public class FieldListTest extends TestSupport {

    @Test
    public void execute() {
        Map<String, String> reqParams = new HashMap<>();
        reqParams.put("entity", "TestAllFields");
        ApiContext apiContext = new ApiContext(reqParams, null);

        JSONObject ret = (JSONObject) new FieldList().execute(apiContext);
        System.out.println(JSONUtils.prettyPrint(ret));
        Assert.assertNotNull(ret.get("error_code"));
    }
}