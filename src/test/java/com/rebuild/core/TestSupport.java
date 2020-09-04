package com.rebuild.core;

import org.junit.AfterClass;
import org.junit.BeforeClass;
import org.junit.runner.RunWith;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.boot.WebApplicationType;
import org.springframework.test.context.junit4.SpringJUnit4ClassRunner;

@RunWith(SpringJUnit4ClassRunner.class)
public class TestSupport {

    protected static final Logger LOG = LoggerFactory.getLogger(TestSupport.class);

    @BeforeClass
    public static void setUp() {
        LOG.warn("TESTING Setup ...");
        Application.debug(WebApplicationType.NONE);
    }

    @AfterClass
    public static void setDown() {
        LOG.warn("TESTING Setdown ...");
    }
}
