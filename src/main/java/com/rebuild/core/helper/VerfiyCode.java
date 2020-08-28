/*
Copyright (c) REBUILD <https://getrebuild.com/> and/or its owners. All rights reserved.

rebuild is dual-licensed under commercial and open source licenses (GPLv3).
See LICENSE and COMMERCIAL in the project root for license information.
*/

package com.rebuild.core.helper;

import cn.devezhao.commons.CodecUtils;
import com.rebuild.core.Application;
import com.rebuild.core.cache.CommonsCache;
import org.apache.commons.lang.math.RandomUtils;

/**
 * 验证码助手
 *
 * @author devezhao
 * @since 11/05/2018
 */
public class VerfiyCode {

    /**
     * @param key
     * @return
     */
    public static String generate(String key) {
        return generate(key, 1);
    }

    /**
     * 生成验证码
     *
     * @param key
     * @param level complexity 1<2<3
     * @return
     */
    public static String generate(String key, int level) {
        String vcode;
        if (level == 3) {
            vcode = CodecUtils.randomCode(20);
        } else if (level == 2) {
            vcode = CodecUtils.randomCode(8);
        } else {
            vcode = RandomUtils.nextInt(999999999) + "888888";
            vcode = vcode.substring(0, 6);
        }

        // 缓存 10 分钟
        Application.getCommonsCache().put("VCode-" + key, vcode, CommonsCache.TS_HOUR / 6);
        return vcode;
    }

    /**
     * @param key
     * @param vcode
     * @return
     * @see #verfiy(String, String, boolean)
     */
    public static boolean verfiy(String key, String vcode) {
        return verfiy(key, vcode, false);
    }

    /**
     * 验证是否有效
     *
     * @param key
     * @param vcode
     * @param keepAlive
     * @return
     * @see #clean(String)
     */
    public static boolean verfiy(String key, String vcode, boolean keepAlive) {
        if (Application.devMode() && "rebuild".equalsIgnoreCase(vcode)) {
            return true;
        }

        final String ckey = "VCode-" + key;
        String exists = Application.getCommonsCache().get(ckey);
        if (exists == null) {
            return false;
        }

        if (exists.equalsIgnoreCase(vcode)) {
            if (!keepAlive) {
                Application.getCommonsCache().evict(ckey);
            }
            return true;
        }
        return false;
    }

    /**
     * 清除验证码
     *
     * @param key
     * @return
     */
    public static void clean(String key) {
        Application.getCommonsCache().evict("VCode-" + key);
    }
}
