/*
rebuild - Building your business-systems freely.
Copyright (C) 2018 devezhao <zhaofang123@gmail.com>

This program is free software: you can redistribute it and/or modify
it under the terms of the GNU General Public License as published by
the Free Software Foundation, either version 3 of the License, or
(at your option) any later version.

This program is distributed in the hope that it will be useful,
but WITHOUT ANY WARRANTY; without even the implied warranty of
MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE. See the
GNU General Public License for more details.

You should have received a copy of the GNU General Public License
along with this program. If not, see <https://www.gnu.org/licenses/>.
*/

package com.rebuild.core.helper;

import cn.devezhao.commons.CodecUtils;
import com.rebuild.core.RebuildApplication;
import com.rebuild.core.cache.CommonsCache;
import org.apache.commons.lang.math.RandomUtils;

/**
 * 验证码助手
 *
 * @author devezhao
 * @since 11/05/2018
 */
public class VCode {

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
        RebuildApplication.getCommonsCache().put("VCode-" + key, vcode, CommonsCache.TS_HOUR / 6);
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
        if (RebuildApplication.devMode() && "rebuild".equalsIgnoreCase(vcode)) {
            return true;
        }

        final String ckey = "VCode-" + key;
        String exists = RebuildApplication.getCommonsCache().get(ckey);
        if (exists == null) {
            return false;
        }

        if (exists.equalsIgnoreCase(vcode)) {
            if (!keepAlive) {
                RebuildApplication.getCommonsCache().evict(ckey);
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
        RebuildApplication.getCommonsCache().evict("VCode-" + key);
    }
}
