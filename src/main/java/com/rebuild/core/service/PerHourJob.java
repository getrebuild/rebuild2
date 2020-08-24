/*
Copyright (c) REBUILD <https://getrebuild.com/> and its owners. All rights reserved.

rebuild is dual-licensed under commercial and open source licenses (GPLv3).
See LICENSE and COMMERCIAL in the project root for license information.
*/

package com.rebuild.core.service;

import com.rebuild.core.helper.ConfigurableItem;
import com.rebuild.core.helper.DistributedJobBean;
import com.rebuild.core.helper.RebuildConfiguration;
import com.rebuild.core.helper.setup.DatabaseBackup;
import com.rebuild.utils.FileFilterByLastModified;
import org.quartz.JobExecutionException;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

import java.util.Calendar;

/**
 * 每小时执行一次的 Job
 *
 * @author devezhao
 * @since 2020/2/4
 */
@Component
public class PerHourJob extends DistributedJobBean {

    @Scheduled(cron = "0 0 * * * ?")
    @Override
    protected void executeInternalSafe() throws JobExecutionException {
        final int hour = Calendar.getInstance().get(Calendar.HOUR_OF_DAY);

        if (hour == 0 && RebuildConfiguration.getBool((ConfigurableItem.DBBackupsEnable))) {
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
