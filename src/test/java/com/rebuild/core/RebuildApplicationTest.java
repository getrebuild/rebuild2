package com.rebuild.core;

import org.junit.jupiter.api.Test;

public class RebuildApplicationTest {

    @Test
    void run() {
        RebuildApplication.main(new String[]{"-Drbdev=true"});
    }
}