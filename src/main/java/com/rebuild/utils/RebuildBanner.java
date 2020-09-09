/*
Copyright (c) REBUILD <https://getrebuild.com/> and/or its owners. All rights reserved.

rebuild is dual-licensed under commercial and open source licenses (GPLv3).
See LICENSE and COMMERCIAL in the project root for license information.
*/

package com.rebuild.utils;

import com.rebuild.core.Application;
import org.apache.commons.lang.SystemUtils;

/**
 * @see org.springframework.boot.Banner
 */
public class RebuildBanner {

    static final String COMMON_BANNER = "" +
            "\n  Version : " + Application.VER +
            "\n  OS      : " + SystemUtils.OS_NAME + " (" + SystemUtils.OS_ARCH + ")" +
            "\n  JVM     : " + SystemUtils.JAVA_VM_NAME + " (" + SystemUtils.JAVA_VERSION + ")" +
            "\n" +
            "\n  Report an issue :" +
            "\n  https://getrebuild.com/report-issue?title=boot";

    static final String FLAG_LINE = "####################################################################";

    public static String formatBanner(String... texts) {
        return formatBanner(true, texts);
    }

    /**
     * @param hasCommon
     * @param texts
     * @return
     */
    public static String formatBanner(boolean hasCommon, String... texts) {
        StringBuilder banner = new StringBuilder()
                .append("\n").append(FLAG_LINE).append("\n\n");

        for (String t : texts) {
            banner.append("  ").append(t).append("\n");
        }

        if (hasCommon) {
            banner.append(COMMON_BANNER).append("\n");
        }

        return banner.append("\n").append(FLAG_LINE).toString();
    }
}
