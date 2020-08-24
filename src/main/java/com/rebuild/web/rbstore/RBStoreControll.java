/*
rebuild - Building your business-systems freely.
Copyright (C) 2019 devezhao <zhaofang123@gmail.com>

This program is free software: you can redistribute it and/or modify
it under the terms of the GNU General Public License as published by
the Free Software Foundation, either version 3 of the License, or
(at your option) any later version.

This program is distributed in the hope that it will be useful,
but WITHOUT ANY WARRANTY; without even the implied warranty of
MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE. See the
GNU General Public License for more details.

You should have received a copy of the GNU General Public License
along with this program. If not, see <https://www.gnu.org/licenses/>.
*/

package com.rebuild.web.rbstore;

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
