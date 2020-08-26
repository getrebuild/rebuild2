/*
Copyright (c) REBUILD <https://getrebuild.com/> and/or its owners. All rights reserved.

rebuild is dual-licensed under commercial and open source licenses (GPLv3).
See LICENSE and COMMERCIAL in the project root for license information.
*/

package com.rebuild.core.service.general.recyclebin;

import cn.devezhao.persist4j.Entity;
import cn.devezhao.persist4j.Record;
import cn.devezhao.persist4j.engine.ID;
import cn.devezhao.persist4j.engine.PersistManagerImpl;
import com.alibaba.fastjson.JSON;
import com.alibaba.fastjson.JSONArray;
import com.alibaba.fastjson.JSONObject;
import com.rebuild.core.RebuildApplication;
import com.rebuild.core.RebuildException;
import com.rebuild.core.metadata.EntityHelper;
import com.rebuild.core.metadata.MetadataHelper;
import com.rebuild.core.privileges.UserService;
import com.rebuild.core.service.TransactionManual;
import com.rebuild.core.service.files.AttachmentAwareObserver;
import com.rebuild.core.service.general.OperatingContext;
import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.springframework.transaction.TransactionStatus;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

/**
 * 数据恢复
 *
 * @author devezhao
 * @since 2019/8/21
 */
public class RecycleRestore {

    private static final Log LOG = LogFactory.getLog(RecycleRestore.class);

    private ID recycleId;

    /**
     * @param recycleId
     */
    public RecycleRestore(ID recycleId) {
        this.recycleId = recycleId;
    }

    /**
     * 恢复数据
     *
     * @return
     */
    public int restore() {
        return restore(false);
    }

    /**
     * 恢复数据
     *
     * @param cascade 恢复关联删除的数据
     * @return
     */
    public int restore(boolean cascade) {
        Object[] main = RebuildApplication.createQueryNoFilter(
                "select recordContent,recordId,recycleId from RecycleBin where recycleId = ?")
                .setParameter(1, this.recycleId)
                .unique();
        // 可能已经（关联）恢复了
        if (main == null) {
            LOG.warn("No recycle found! Maybe restored : " + this.recycleId);
            return 0;
        }

        final List<ID> recycleIds = new ArrayList<>();

        final List<Record> willRestores = new ArrayList<>(conver2Record(JSON.parseObject((String) main[0]), (ID) main[1]));
        if (willRestores.isEmpty()) {
            throw new RebuildException("记录实体已经不存在");
        }
        recycleIds.add((ID) main[2]);

        if (cascade) {
            Object[][] array = RebuildApplication.createQueryNoFilter(
                    "select recordContent,recordId,recycleId from RecycleBin where channelWith = ?")
                    .setParameter(1, main[1])
                    .array();
            for (Object[] o : array) {
                List<Record> records = conver2Record(JSON.parseObject((String) o[0]), (ID) o[1]);
                if (!records.isEmpty()) {
                    willRestores.addAll(records);
                    recycleIds.add((ID) o[2]);
                }
            }
        }

        // 启动事物
        final TransactionStatus status = TransactionManual.newTransaction();

        int restored = 0;
        PersistManagerImpl PM = (PersistManagerImpl) RebuildApplication.getPersistManagerFactory().createPersistManager();
        try {
            for (Record r : willRestores) {
                String primaryName = r.getEntity().getPrimaryField().getName();
                ID primaryId = (ID) r.removeValue(primaryName);
                PM.saveInternal(r, primaryId);
                restoreAttachment(PM, primaryId);

                restored++;
            }

            // 从回收站删除
            PM.delete(recycleIds.toArray(new ID[0]));

            TransactionManual.commit(status);
            return restored;

        } catch (Throwable ex) {
            TransactionManual.rollback(status);
            throw new RebuildException("恢复数据失败", ex);
        }
    }

    /**
     * 转换成 Record 对象，返回多条是可能存在明细
     *
     * @param content
     * @param recordId
     * @return
     */
    private List<Record> conver2Record(JSONObject content, ID recordId) {
        if (!MetadataHelper.containsEntity(recordId.getEntityCode())) {
            return Collections.emptyList();
        }

        JSONArray slaveList = content.getJSONArray(RecycleBean.NAME_SLAVELIST);
        if (slaveList != null) {
            content.remove(RecycleBean.NAME_SLAVELIST);
        }

        List<Record> records = new ArrayList<>();

        Entity entity = MetadataHelper.getEntity(recordId.getEntityCode());
        Record record = new RestoreRecordCreator(entity, content).create(true);
        records.add(record);

        Entity slaveEntity = entity.getSlaveEntity();
        if (slaveList != null && slaveEntity != null) {
            for (Object o : slaveList) {
                Record slave = new RestoreRecordCreator(slaveEntity, (JSONObject) o).create(true);
                records.add(slave);
            }
        }
        return records;
    }

    /**
     * @param PM
     * @param recordId
     * @see AttachmentAwareObserver#onDelete(OperatingContext)
     */
    private void restoreAttachment(PersistManagerImpl PM, ID recordId) {
        Object[][] array = RebuildApplication.createQueryNoFilter(
                "select attachmentId from Attachment where relatedRecord = ?")
                .setParameter(1, recordId)
                .array();
        for (Object[] o : array) {
            Record u = EntityHelper.forUpdate((ID) o[0], UserService.SYSTEM_USER, false);
            u.setBoolean(EntityHelper.IsDeleted, false);
            PM.update(u);
        }
    }
}
