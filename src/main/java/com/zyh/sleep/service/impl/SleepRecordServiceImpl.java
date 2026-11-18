package com.zyh.sleep.service.impl;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import com.zyh.sleep.dto.SleepStats;
import com.zyh.sleep.entity.SleepRecord;
import com.zyh.sleep.mapper.SleepRecordMapper;
import com.zyh.sleep.service.SleepRecordService;
import org.springframework.stereotype.Service;

import java.time.Duration;
import java.time.LocalDateTime;
import java.util.Comparator;
import java.util.List;
import java.util.Objects;

@Service
public class SleepRecordServiceImpl extends ServiceImpl<SleepRecordMapper, SleepRecord> implements SleepRecordService {

    @Override
    public List<SleepRecord> listWithDuration(Long userId) {
        List<SleepRecord> list = list(new LambdaQueryWrapper<SleepRecord>()
                .eq(SleepRecord::getUserId, userId)
                .orderByDesc(SleepRecord::getBedTime));
        list.forEach(this::fillDuration);
        return list;
    }

    @Override
    public SleepRecord saveWithDuration(SleepRecord record) {
        save(record);
        fillDuration(record);
        return record;
    }

    @Override
    public SleepRecord updateWithDuration(SleepRecord record, Long userId) {
        SleepRecord existing = getById(record.getId());
        if (existing == null || !userId.equals(existing.getUserId())) {
            return null;
        }
        record.setUserId(userId);
        updateById(record);
        SleepRecord saved = getById(record.getId());
        fillDuration(saved);
        return saved;
    }

    @Override
    public boolean deleteOwn(Long id, Long userId) {
        return remove(new LambdaQueryWrapper<SleepRecord>()
                .eq(SleepRecord::getId, id)
                .eq(SleepRecord::getUserId, userId));
    }

    private void fillDuration(SleepRecord r) {
        r.setFallAsleepMinutes(minutesBetween(r.getBedTime(), r.getSleepTime()));
        r.setSleepMinutes(minutesBetween(r.getSleepTime(), r.getWakeTime()));
    }

    private Long minutesBetween(LocalDateTime from, LocalDateTime to) {
        if (from == null || to == null) {
            return null;
        }
        long minutes = Duration.between(from, to).toMinutes();
        if (minutes < 0) {
            minutes += 24 * 60;
        }
        return minutes;
    }

    @Override
    public SleepStats stats(Long userId) {
        List<SleepRecord> list = listWithDuration(userId);
        SleepStats s = new SleepStats();
        s.setRecordCount(list.size());
        if (list.isEmpty()) {
            return s;
        }
        s.setAvgSleepMinutes(round(list.stream()
                .map(SleepRecord::getSleepMinutes)
                .filter(Objects::nonNull)
                .mapToLong(Long::longValue)
                .average().orElse(0)));
        s.setAvgFallAsleepMinutes(round(list.stream()
                .map(SleepRecord::getFallAsleepMinutes)
                .filter(Objects::nonNull)
                .mapToLong(Long::longValue)
                .average().orElse(0)));
        s.setAvgQuality(round(list.stream()
                .map(SleepRecord::getQuality)
                .filter(Objects::nonNull)
                .mapToInt(Integer::intValue)
                .average().orElse(0)));
        s.setAvgWakeCount(round(list.stream()
                .map(SleepRecord::getWakeCount)
                .filter(Objects::nonNull)
                .mapToInt(Integer::intValue)
                .average().orElse(0)));
        s.setEarliestSleepTime(list.stream()
                .map(SleepRecord::getSleepTime)
                .filter(Objects::nonNull)
                .min(Comparator.comparing(t -> t.toLocalTime()))
                .map(t -> t.toLocalTime().toString())
                .orElse(null));
        s.setLatestSleepTime(list.stream()
                .map(SleepRecord::getSleepTime)
                .filter(Objects::nonNull)
                .max(Comparator.comparing(t -> t.toLocalTime()))
                .map(t -> t.toLocalTime().toString())
                .orElse(null));
        return s;
    }

    private double round(double value) {
        return Math.round(value * 10) / 10.0;
    }
}
