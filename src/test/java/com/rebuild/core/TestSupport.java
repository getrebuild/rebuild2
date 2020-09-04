package com.rebuild.core;

import org.junit.AfterClass;
import org.junit.BeforeClass;
import org.junit.runner.RunWith;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.test.context.junit4.SpringJUnit4ClassRunner;

@RunWith(SpringJUnit4ClassRunner.class)
public abstract class TestSupport {

    protected static final Logger LOG = LoggerFactory.getLogger(TestSupport.class);

    @BeforeClass
    public static void setUp() {
        LOG.warn("TESTING Setup ...");

        System.setProperty("rbdev", "true");
        Application.main(new String[]{});
    }

    @AfterClass
    public static void setDown() {
        LOG.warn("TESTING Setdown ...");
    }
}
