package com.rebuild.core;

import org.junit.jupiter.api.Test;

public class RebuildApplicationTest {

    @Test
    void run() {
        System.setProperty("rbdev", "true");
        RebuildApplication.main(new String[0]);
    }
}