package com.zyh.sleep.service.impl;

import com.baomidou.mybatisplus.core.conditions.Wrapper;
import com.zyh.sleep.dto.SleepStats;
import com.zyh.sleep.entity.SleepRecord;
import com.zyh.sleep.mapper.SleepRecordMapper;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentMatchers;
import org.mockito.Mockito;
import org.springframework.test.util.ReflectionTestUtils;

import java.time.LocalDateTime;
import java.util.Collections;
import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;

class SleepRecordServiceImplTest {

    private SleepRecordServiceImpl service;
    private SleepRecordMapper mapper;

    @BeforeEach
    void setUp() {
        mapper = Mockito.mock(SleepRecordMapper.class);
        service = new SleepRecordServiceImpl();
        ReflectionTestUtils.setField(service, "baseMapper", mapper);
    }

    private SleepRecord record(LocalDateTime bed, LocalDateTime sleep, LocalDateTime wake,
                               int quality, int wakeCount) {
        SleepRecord r = new SleepRecord();
        r.setUserId(1L);
        r.setBedTime(bed);
        r.setSleepTime(sleep);
        r.setWakeTime(wake);
        r.setQuality(quality);
        r.setWakeCount(wakeCount);
        return r;
    }

    @Test
    @DisplayName("统计：平均值保留一位小数，最早/最晚入睡时间正确")
    void statsAggregation() {
        List<SleepRecord> data = List.of(
                record(LocalDateTime.of(2026, 11, 17, 23, 30),
                        LocalDateTime.of(2026, 11, 18, 0, 10),
                        LocalDateTime.of(2026, 11, 18, 7, 0), 4, 1),
                record(LocalDateTime.of(2026, 11, 18, 22, 0),
                        LocalDateTime.of(2026, 11, 18, 23, 20),
                        LocalDateTime.of(2026, 11, 19, 6, 40), 5, 2)
        );
        Mockito.when(mapper.selectList(ArgumentMatchers.<Wrapper<SleepRecord>>any())).thenReturn(data);

        SleepStats s = service.stats(1L, null);

        assertEquals(2, s.getRecordCount());
        assertEquals(425.0, s.getAvgSleepMinutes());
        assertEquals(60.0, s.getAvgFallAsleepMinutes());
        assertEquals(4.5, s.getAvgQuality());
        assertEquals(1.5, s.getAvgWakeCount());
        assertEquals("23:20", s.getEarliestSleepTime());
        assertEquals("00:10", s.getLatestSleepTime());
    }

    @Test
    @DisplayName("统计：没有记录时返回 0，不抛异常")
    void statsEmpty() {
        Mockito.when(mapper.selectList(ArgumentMatchers.<Wrapper<SleepRecord>>any()))
                .thenReturn(Collections.emptyList());

        SleepStats s = service.stats(1L, 7);

        assertEquals(0, s.getRecordCount());
        assertEquals(0.0, s.getAvgSleepMinutes());
    }

    @Test
    @DisplayName("查询列表时自动补算睡眠时长与入睡耗时")
    void listFillsDuration() {
        List<SleepRecord> data = List.of(
                record(LocalDateTime.of(2026, 11, 18, 22, 0),
                        LocalDateTime.of(2026, 11, 18, 23, 20),
                        LocalDateTime.of(2026, 11, 19, 6, 40), 5, 2)
        );
        Mockito.when(mapper.selectList(ArgumentMatchers.<Wrapper<SleepRecord>>any())).thenReturn(data);

        List<SleepRecord> result = service.listWithDuration(1L, 7);

        assertEquals(1, result.size());
        assertNotNull(result.get(0).getSleepMinutes());
        assertEquals(Long.valueOf(440), result.get(0).getSleepMinutes());
        assertEquals(Long.valueOf(80), result.get(0).getFallAsleepMinutes());
    }
}
