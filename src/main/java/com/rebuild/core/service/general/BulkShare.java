/*
Copyright (c) REBUILD <https://getrebuild.com/> and its owners. All rights reserved.

rebuild is dual-licensed under commercial and open source licenses (GPLv3).
See LICENSE and COMMERCIAL in the project root for license information.
*/

package com.rebuild.core.service.general;

import cn.devezhao.bizz.privileges.impl.BizzPermission;
import cn.devezhao.persist4j.Record;
import cn.devezhao.persist4j.engine.ID;
import com.rebuild.core.RebuildApplication;
import com.rebuild.core.metadata.EntityHelper;
import com.rebuild.core.service.notification.NotificationObserver;
import com.rebuild.core.service.notification.NotificationOnce;

import java.util.Set;

/**
 * 共享
 *
 * @author devezhao
 * @since 09/29/2018
 */
public class BulkShare extends BulkOperator {

    public BulkShare(BulkContext context, GeneralEntityService ges) {
        super(context, ges);
    }

    @Override
    protected Integer exec() {
        final ID[] records = prepareRecords();
        this.setTotal(records.length);

        ID firstShared = null;
        NotificationOnce.begin();
        for (ID id : records) {
            if (RebuildApplication.getPrivilegesManager().allowShare(context.getOpUser(), id)) {
                int a = ges.share(id, context.getToUser(), context.getCascades());
                if (a > 0) {
                    this.addSucceeded();
                    if (firstShared == null) {
                        firstShared = id;
                    }
                }
            } else {
                LOG.warn("No have privileges to SHARE : " + context.getOpUser() + " > " + id);
            }
            this.addCompleted();
        }

        // 合并通知发送
        Set<ID> affected = NotificationOnce.end();
        if (firstShared != null && !affected.isEmpty()) {
            Record notificationNeeds = EntityHelper.forNew(EntityHelper.ShareAccess, context.getOpUser());
            notificationNeeds.setID("shareTo", context.getToUser());
            notificationNeeds.setID("recordId", firstShared);
            // Once notification
            OperatingContext operatingContext = OperatingContext.create(
                    context.getOpUser(), BizzPermission.SHARE, null, notificationNeeds, affected.toArray(new ID[0]));
            new NotificationObserver().update(null, operatingContext);
        }

        return getSucceeded();
    }
}
