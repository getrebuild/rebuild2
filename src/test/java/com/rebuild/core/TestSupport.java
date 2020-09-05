/*
Copyright (c) REBUILD <https://getrebuild.com/> and/or its owners. All rights reserved.

rebuild is dual-licensed under commercial and open source licenses (GPLv3).
See LICENSE and COMMERCIAL in the project root for license information.
*/

package com.rebuild.core;

import org.junit.AfterClass;
import org.junit.BeforeClass;
import org.junit.runner.RunWith;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.test.context.junit4.SpringJUnit4ClassRunner;

/**
 * JUnit4 测试基类
 */
@RunWith(SpringJUnit4ClassRunner.class)
public class TestSupport {

    protected static final Logger LOG = LoggerFactory.getLogger(TestSupport.class);

    @BeforeClass
    public static void setUp() {
        LOG.warn("TESTING Setup ...");
        Application.debug();
    }

    @AfterClass
    public static void setDown() {
        LOG.warn("TESTING Setdown ...");
    }
}
