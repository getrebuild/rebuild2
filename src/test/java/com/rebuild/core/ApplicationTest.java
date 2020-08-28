/*
Copyright (c) REBUILD <https://getrebuild.com/> and/or its owners. All rights reserved.

rebuild is dual-licensed under commercial and open source licenses (GPLv3).
See LICENSE and COMMERCIAL in the project root for license information.
*/

package com.rebuild.core;

import org.junit.jupiter.api.Test;

/**
 * @author devezhao
 * @since 2020/8/26
 */
public class ApplicationTest {

    @Test
    void run() {
        Application.main(new String[]{"-Drbdev=true"});
    }
}