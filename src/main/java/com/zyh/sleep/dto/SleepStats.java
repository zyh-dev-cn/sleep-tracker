package com.zyh.sleep.dto;

import lombok.Data;

@Data
public class SleepStats {

    private long recordCount;

    private double avgSleepMinutes;

    private double avgFallAsleepMinutes;

    private double avgQuality;

    private double avgWakeCount;

    private String earliestSleepTime;

    private String latestSleepTime;
}
