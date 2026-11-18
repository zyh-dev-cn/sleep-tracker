package com.zyh.sleep.entity;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableField;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;

import java.time.LocalDateTime;

@Data
@TableName("sleep_record")
public class SleepRecord {

    @TableId(type = IdType.AUTO)
    private Long id;

    private Long userId;

    private LocalDateTime bedTime;

    private LocalDateTime sleepTime;

    private LocalDateTime wakeTime;

    private Integer wakeCount;

    private Integer quality;

    private String note;

    @TableField(exist = false)
    private Long sleepMinutes;

    @TableField(exist = false)
    private Long fallAsleepMinutes;
}
