package com.zyh.sleep.util;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.time.LocalDateTime;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNull;

class SleepTimeUtilsTest {

    @Test
    @DisplayName("同一天内的时间差正常计算")
    void sameDay() {
        LocalDateTime from = LocalDateTime.of(2026, 11, 18, 23, 0);
        LocalDateTime to = LocalDateTime.of(2026, 11, 18, 23, 35);
        assertEquals(35L, SleepTimeUtils.minutesBetween(from, to));
    }

    @Test
    @DisplayName("跨天时结果为负数，加一天修正")
    void crossDay() {
        LocalDateTime from = LocalDateTime.of(2026, 11, 18, 23, 30);
        LocalDateTime to = LocalDateTime.of(2026, 11, 19, 6, 0);
        assertEquals(390L, SleepTimeUtils.minutesBetween(from, to));
    }

    @Test
    @DisplayName("入睡时间在午夜之后同样适用")
    void afterMidnight() {
        LocalDateTime from = LocalDateTime.of(2026, 11, 19, 0, 10);
        LocalDateTime to = LocalDateTime.of(2026, 11, 19, 7, 0);
        assertEquals(410L, SleepTimeUtils.minutesBetween(from, to));
    }

    @Test
    @DisplayName("两个时间相同返回 0")
    void sameInstant() {
        LocalDateTime t = LocalDateTime.of(2026, 11, 18, 23, 0);
        assertEquals(0L, SleepTimeUtils.minutesBetween(t, t));
    }

    @Test
    @DisplayName("任一时间为空返回 null，不抛异常")
    void nullInput() {
        LocalDateTime t = LocalDateTime.of(2026, 11, 18, 23, 0);
        assertNull(SleepTimeUtils.minutesBetween(null, t));
        assertNull(SleepTimeUtils.minutesBetween(t, null));
    }
}
