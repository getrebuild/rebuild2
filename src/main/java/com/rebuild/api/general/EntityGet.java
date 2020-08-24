/*
Copyright (c) REBUILD <https://getrebuild.com/> and its owners. All rights reserved.

rebuild is dual-licensed under commercial and open source licenses (GPLv3).
See LICENSE and COMMERCIAL in the project root for license information.
*/

package com.rebuild.api.general;

import cn.devezhao.persist4j.Entity;
import cn.devezhao.persist4j.Query;
import cn.devezhao.persist4j.engine.ID;
import com.alibaba.fastjson.JSON;
import com.rebuild.api.ApiContext;
import com.rebuild.api.ApiInvokeException;
import com.rebuild.api.BaseApi;
import com.rebuild.core.RebuildApplication;
import com.rebuild.core.metadata.MetadataHelper;
import org.apache.commons.lang.StringUtils;

import java.util.ArrayList;
import java.util.List;

/**
 * 获取单条记录
 *
 * @author devezhao
 * @since 2020/5/21
 */
public class EntityGet extends BaseApi {

    @Override
    protected String getApiName() {
        return "entity/get";
    }

    @Override
    public JSON execute(ApiContext context) throws ApiInvokeException {
        final ID queryId = context.getParameterAsId("id");
        final Entity useEntity = MetadataHelper.getEntity(queryId.getEntityCode());
        if (!useEntity.isQueryable()) {
            throw new ApiInvokeException(ApiInvokeException.ERR_BIZ, "Unsupportted operation for entity/id : " + queryId);
        }

        if (!RebuildApplication.getPrivilegesManager().allowRead(context.getBindUser(), queryId)) {
            return formatFailure("无权读取记录或记录不存在 : " + queryId);
        }

        String[] fields = context.getParameterNotBlank("fields").split(",");
        fields = getValidFields(useEntity, fields);

        String sql = String.format("select %s from %s where %s = ?",
                StringUtils.join(fields, ","), useEntity.getName(), useEntity.getPrimaryField().getName());

        Query query = RebuildApplication.getQueryFactory().createQueryNoFilter(sql);
        Object[] queryed = query.setParameter(1, queryId).unique();
        if (queryed == null) {
            return formatFailure("记录不存在 : " + queryId);
        }

        return formatSuccess(ApiDataListWrapper.buildItem(query.getSelectItems(), queryed));
    }

    /**
     * @param entity
     * @param fields
     * @return
     */
    protected String[] getValidFields(Entity entity, String[] fields) {
        // TODO 限制连接查询

        List<String> validFields = new ArrayList<>();
        for (String field : fields) {
            if (entity.containsField(field) && !MetadataHelper.isSystemField(field)) {
                validFields.add(field);
            } else {
                LOG.warn("Filtered invalid field of query : " + field);
            }
        }

        if (validFields.isEmpty()) {
            validFields.add(entity.getPrimaryField().getName());
        }
        return validFields.toArray(new String[0]);
    }
}
