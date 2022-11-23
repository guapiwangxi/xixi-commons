package com.xi.commons;

import com.xi.commons.logger.ZLog;
import org.junit.Test;

public class ZLogTest {

    @Test
    public void testLog() {
        level1();
    }

    private void level1() {
        ZLog.start("level1", true);
        try {
            ZLog.ap("a1", "0").ap("b1", "0");
            level2();
        } finally {
            ZLog.end();
        }
    }

    private void level2() {
        ZLog.start("level2");
        try {
            ZLog.ap("a2", "0").ap("b2", "0");
            level3();
        } finally {
            ZLog.end();
        }
    }

    private void level3() {
        ZLog.start("level3");
        try {
            ZLog.ap("a3", "0").ap("b3", "0");
        } finally {
            ZLog.end();
        }
    }
}
