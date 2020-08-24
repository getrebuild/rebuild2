/*
Copyright (c) REBUILD <https://getrebuild.com/> and its owners. All rights reserved.

rebuild is dual-licensed under commercial and open source licenses (GPLv3).
See LICENSE and COMMERCIAL in the project root for license information.
*/

package com.rebuild.core.configuration;

import com.rebuild.core.RebuildApplication;

/**
 * API 鉴权参数
 *
 * @author ZHAO
 * @since 2019-07-23
 */
public class RebuildApiManager implements ConfigManager {

    public static final RebuildApiManager instance = new RebuildApiManager();

    private RebuildApiManager() {
    }

    private static final String CKEY_PREFIX = "RebuildApiManager-";

    /**
     * @param appid
     * @return
     */
    public ConfigBean getApp(String appid) {
        final String ckey = CKEY_PREFIX + appid;
        ConfigBean config = (ConfigBean) RebuildApplication.getCommonsCache().getx(ckey);
        if (config != null) {
            return config;
        }

        Object[] o = RebuildApplication.createQueryNoFilter(
                "select appSecret,bindUser,bindIps from RebuildApi where appId = ?")
                .setParameter(1, appid)
                .unique();
        if (o == null) return null;

        config = new ConfigBean()
                .set("appId", appid)
                .set("appSecret", o[0])
                .set("bindUser", o[1])
                .set("bindIps", o[2]);
        RebuildApplication.getCommonsCache().putx(ckey, config);
        return config;
    }

    @Override
    public void clean(Object appid) {
        RebuildApplication.getCommonsCache().evict(CKEY_PREFIX + appid);
    }
}
