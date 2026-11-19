package com.zyh.sleep.service;

import com.baomidou.mybatisplus.extension.service.IService;
import com.zyh.sleep.dto.SleepStats;
import com.zyh.sleep.entity.SleepRecord;

import java.util.List;

public interface SleepRecordService extends IService<SleepRecord> {

    List<SleepRecord> listWithDuration(Long userId, Integer days);

    SleepRecord saveWithDuration(SleepRecord record);

    SleepRecord updateWithDuration(SleepRecord record, Long userId);

    boolean deleteOwn(Long id, Long userId);

    SleepStats stats(Long userId, Integer days);
}
