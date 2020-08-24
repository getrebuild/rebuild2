package com.rebuild.web.configuration;

import cn.devezhao.persist4j.Record;
import io.micrometer.core.instrument.util.StringUtils;

import javax.servlet.http.HttpServletRequest;

public interface ShareTo {

    /**
     * 公共字段
     *
     * @param request
     * @param record
     */
    default void putCommonsFields(HttpServletRequest request, Record record) {
        String shareTo = request.getParameter("shareTo");
        if (StringUtils.isNotBlank(shareTo)) {
            record.setString("shareTo", shareTo);
        }

        String configName = request.getParameter("configName");
        if (StringUtils.isNotBlank(configName)) {
            record.setString("configName", configName);
        }
    }
}
