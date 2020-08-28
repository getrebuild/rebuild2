/*
Copyright (c) REBUILD <https://getrebuild.com/> and/or its owners. All rights reserved.

rebuild is dual-licensed under commercial and open source licenses (GPLv3).
See LICENSE and COMMERCIAL in the project root for license information.
*/

package com.rebuild.web.configuration;

import cn.devezhao.commons.web.ServletUtils;
import cn.devezhao.persist4j.Record;
import cn.devezhao.persist4j.engine.ID;
import com.alibaba.fastjson.JSON;
import com.rebuild.core.Application;
import com.rebuild.core.configuration.ConfigBean;
import com.rebuild.core.configuration.general.BaseLayoutManager;
import com.rebuild.core.configuration.general.LayoutConfigService;
import com.rebuild.core.configuration.general.ShareToManager;
import com.rebuild.core.metadata.EntityHelper;
import com.rebuild.web.BaseController;
import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestMethod;

import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import java.io.IOException;

/**
 * @author devezhao
 * @since 2019/10/18
 */
@Controller
@RequestMapping("/app/{entity}/")
public class WidgetControll extends BaseController implements ShareTo {

    @RequestMapping(value = "/widget-charts", method = RequestMethod.POST)
    public void sets(@PathVariable String entity,
                     HttpServletRequest request, HttpServletResponse response) throws IOException {
        ID user = getRequestUser(request);
        JSON config = ServletUtils.getRequestJson(request);
        ID cfgid = getIdParameter(request, "id");
        if (cfgid != null && !ShareToManager.isSelf(user, cfgid)) {
            cfgid = null;
        }

        Record record;
        if (cfgid == null) {
            record = EntityHelper.forNew(EntityHelper.LayoutConfig, user);
            record.setString("belongEntity", entity);
            record.setString("applyType", BaseLayoutManager.TYPE_WCHARTS);
            record.setString("shareTo", BaseLayoutManager.SHARE_SELF);
        } else {
            record = EntityHelper.forUpdate(cfgid, user);
        }
        record.setString("config", config.toJSONString());
        putCommonsFields(request, record);
        record = Application.getBean(LayoutConfigService.class).createOrUpdate(record);

        writeSuccess(response, record.getPrimary());
    }

    @RequestMapping(value = "/widget-charts", method = RequestMethod.GET)
    public void gets(@PathVariable String entity,
                     HttpServletRequest request, HttpServletResponse response) throws IOException {
        ID user = getRequestUser(request);
        ConfigBean config = BaseLayoutManager.instance.getWidgetCharts(user, entity);
        writeSuccess(response, config == null ? null : config.toJSON());
    }
}
