/*
Copyright (c) REBUILD <https://getrebuild.com/> and/or its owners. All rights reserved.

rebuild is dual-licensed under commercial and open source licenses (GPLv3).
See LICENSE and COMMERCIAL in the project root for license information.
*/

package com.rebuild.web.admin.rbstore;

import com.alibaba.fastjson.JSON;
import com.alibaba.fastjson.JSONArray;
import com.alibaba.fastjson.JSONObject;
import com.rebuild.core.metadata.MetadataHelper;
import com.rebuild.core.rbstore.RBStore;
import com.rebuild.web.BaseController;
import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.RequestMapping;

import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import java.io.IOException;

/**
 * @author devezhao-mbp zhaofang123@gmail.com
 * @since 2019/04/28
 */
@Controller
@RequestMapping("/admin/rbstore")
public class RBStoreControll extends BaseController {

    @RequestMapping("load-index")
    public void loadDataIndex(HttpServletRequest request, HttpServletResponse response) throws IOException {
        String type = getParameterNotNull(request, "type");
        JSON index = RBStore.fetchRemoteJson(type + "/index.json");
        writeSuccess(response, index);
    }

    @RequestMapping("load-metaschemas")
    public void loadMetaschemas(HttpServletResponse response) throws IOException {
        JSONArray index = (JSONArray) RBStore.fetchMetaschema("index.json");

        for (Object o : index) {
            JSONObject item = (JSONObject) o;
            String key = item.getString("key");
            if (MetadataHelper.containsEntity(key)) {
                item.put("exists", true);
            }
        }
        writeSuccess(response, index);
    }
}
