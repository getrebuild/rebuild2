/*
Copyright (c) REBUILD <https://getrebuild.com/> and its owners. All rights reserved.

rebuild is dual-licensed under commercial and open source licenses (GPLv3).
See LICENSE and COMMERCIAL in the project root for license information.
*/

package com.rebuild.core.helper.general;

import cn.devezhao.commons.ObjectUtils;
import cn.devezhao.persist4j.Entity;
import cn.devezhao.persist4j.Query;
import cn.devezhao.persist4j.engine.ID;
import com.alibaba.fastjson.JSON;
import com.alibaba.fastjson.JSONObject;
import com.rebuild.core.RebuildApplication;
import com.rebuild.core.metadata.EntityHelper;
import com.rebuild.core.metadata.MetadataHelper;
import com.rebuild.core.privileges.UserService;

/**
 * 数据列表控制器
 *
 * @author Zhao Fangfang
 * @since 1.0, 2013-6-20
 */
public class DefaultDataListControl implements DataListControl {

    final protected Entity entity;
    final protected QueryParser queryParser;
    final protected ID user;

    /**
     * @param query
     * @param user
     */
    public DefaultDataListControl(JSONObject query, ID user) {
        this.entity = MetadataHelper.getEntity(query.getString("entity"));
        this.queryParser = new QueryParser(query, this);
        this.user = user;
    }

    @Override
    public Entity getEntity() {
        return entity;
    }

    /**
     * @return
     */
    public QueryParser getQueryParser() {
        return queryParser;
    }

    @Override
    public String getDefaultFilter() {
        // 隐藏系统用户
        if (queryParser.getEntity().getEntityCode() == EntityHelper.User) {
            return String.format("userId <> '%s'", UserService.SYSTEM_USER);
        }

        return null;
    }

    @Override
    public JSON getJSONResult() {
        int totalRows = 0;
        if (queryParser.isNeedReload()) {
            Object[] count = RebuildApplication.getQueryFactory().createQuery(queryParser.toCountSql(), user).unique();
            totalRows = ObjectUtils.toInt(count[0]);
        }

        Query query = RebuildApplication.getQueryFactory().createQuery(queryParser.toSql(), user);
        int[] limits = queryParser.getSqlLimit();
        Object[][] data = query.setLimit(limits[0], limits[1]).array();

        return createDataListWrapper(totalRows, data, query)
                .toJson();
    }

    /**
     * @param totalRows
     * @param data
     * @param query
     * @return
     */
    protected DataListWrapper createDataListWrapper(int totalRows, Object[][] data, Query query) {
        DataListWrapper wrapper = new DataListWrapper(
                totalRows, data, query.getSelectItems(), query.getRootEntity());
        wrapper.setPrivilegesFilter(user, queryParser.getQueryJoinFields());
        return wrapper;
    }
}
