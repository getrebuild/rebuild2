/*
Copyright (c) REBUILD <https://getrebuild.com/> and/or its owners. All rights reserved.

rebuild is dual-licensed under commercial and open source licenses (GPLv3).
See LICENSE and COMMERCIAL in the project root for license information.
*/

package com.rebuild.core.service;

import com.rebuild.core.support.ConfigurationItem;
import com.rebuild.core.support.DistributedJobBean;
import com.rebuild.core.support.RebuildConfiguration;
import com.rebuild.core.support.setup.DatabaseBackup;
import com.rebuild.utils.FileFilterByLastModified;
import org.springframework.scheduling.annotation.Scheduled;

import java.util.Calendar;

/**
 * 每小时执行一次的 Job
 *
 * @author devezhao
 * @since 2020/2/4
 */
public class PerHourJob extends DistributedJobBean {

    @Scheduled(cron = "0 0 * * * ?")
    protected void executeJob() {
        final int hour = Calendar.getInstance().get(Calendar.HOUR_OF_DAY);

        if (hour == 0 && RebuildConfiguration.getBool((ConfigurationItem.DBBackupsEnable))) {
            doDatabaseBackup();
        } else if (hour == 1) {
            doCleanTempFiles();
        }

        // DO OTHERS HERE ...

    }

    /**
     * 数据库备份
     */
    protected void doDatabaseBackup() {
        try {
            new DatabaseBackup().backup();
        } catch (Exception e) {
            LOG.error("Executing [DatabaseBackup] failed : " + e);
        }
    }

    /**
     * 清理临时目录
     */
    protected void doCleanTempFiles() {
        FileFilterByLastModified.deletes(RebuildConfiguration.getFileOfTemp(null), 7);
    }

}
