package com.zyh.sleep.util;

import java.time.Duration;
import java.time.LocalDateTime;

public final class SleepTimeUtils {

    private SleepTimeUtils() {
    }

    /**
     * 计算两个时间点之间的分钟数，跨天（结果为负）时加一天修正。
     */
    public static Long minutesBetween(LocalDateTime from, LocalDateTime to) {
        if (from == null || to == null) {
            return null;
        }
        long minutes = Duration.between(from, to).toMinutes();
        if (minutes < 0) {
            minutes += 24 * 60;
        }
        return minutes;
    }
}
