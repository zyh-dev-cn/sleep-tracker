package com.zyh.sleep.controller;

import com.zyh.sleep.context.UserContext;
import com.zyh.sleep.dto.AiAdvice;
import com.zyh.sleep.dto.SleepStats;
import com.zyh.sleep.entity.SleepRecord;
import com.zyh.sleep.service.AiAnalysisService;
import com.zyh.sleep.service.SleepRecordService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.tags.Tag;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.util.Comparator;
import java.util.List;
import java.util.Map;

@Tag(name = "睡眠记录", description = "睡眠记录的增删改查、统计汇总与 AI 分析")
@RestController
@RequestMapping("/api/records")
public class SleepRecordController {

    private final SleepRecordService service;
    private final AiAnalysisService aiAnalysisService;

    public SleepRecordController(SleepRecordService service, AiAnalysisService aiAnalysisService) {
        this.service = service;
        this.aiAnalysisService = aiAnalysisService;
    }

    @Operation(summary = "新增记录")
    @PostMapping
    public SleepRecord create(@RequestBody SleepRecord record) {
        record.setUserId(UserContext.get());
        return service.saveWithDuration(record);
    }

    @Operation(summary = "删除记录（校验归属）")
    @DeleteMapping("/{id}")
    public boolean delete(@PathVariable Long id) {
        return service.deleteOwn(id, UserContext.get());
    }

    @Operation(summary = "修改记录（校验归属）")
    @PutMapping("/{id}")
    public SleepRecord update(@PathVariable Long id, @RequestBody SleepRecord record) {
        record.setId(id);
        return service.updateWithDuration(record, UserContext.get());
    }

    @Operation(summary = "查询记录列表")
    @GetMapping
    public List<SleepRecord> list(
            @Parameter(description = "只返回最近 N 天，不传则返回全部")
            @RequestParam(required = false) Integer days) {
        return service.listWithDuration(UserContext.get(), days);
    }

    @Operation(summary = "统计汇总")
    @GetMapping("/stats")
    public SleepStats stats(
            @Parameter(description = "只统计最近 N 天，不传则统计全部")
            @RequestParam(required = false) Integer days) {
        return service.stats(UserContext.get(), days);
    }

    @Operation(summary = "AI 睡眠建议（命中缓存时 cached=true）")
    @GetMapping("/advice")
    public Map<String, Object> advice(
            @Parameter(description = "只分析最近 N 天，不传则分析全部")
            @RequestParam(required = false) Integer days) {
        Long userId = UserContext.get();
        SleepStats s = service.stats(userId, days);
        if (s.getRecordCount() == 0) {
            return Map.of("advice", "还没有睡眠记录，先记录几天再来分析吧。", "cached", false);
        }
        String summary = buildSummary(s, service.listWithDuration(userId, days), days);
        AiAdvice result = aiAnalysisService.analyze(userId, summary);
        return Map.of("advice", result.advice(), "cached", result.cached());
    }

    private String buildSummary(SleepStats s, List<SleepRecord> list, Integer days) {
        StringBuilder sb = new StringBuilder();
        if (days != null && days > 0) {
            sb.append("以下数据来自最近 ").append(days).append(" 天。\n");
        }
        sb.append("用户共记录了 ").append(s.getRecordCount()).append(" 天睡眠数据。\n");
        sb.append("平均睡眠 ").append(fmtMinutes(s.getAvgSleepMinutes()))
                .append("，平均入睡耗时 ").append(fmtMinutes(s.getAvgFallAsleepMinutes()))
                .append("，平均质量 ").append(s.getAvgQuality()).append(" 分（满分5）")
                .append("，平均夜醒 ").append(s.getAvgWakeCount()).append(" 次。\n");
        sb.append("最早入睡 ").append(s.getEarliestSleepTime())
                .append("，最晚入睡 ").append(s.getLatestSleepTime()).append("。\n");
        sb.append("最近记录：\n");
        list.stream()
                .filter(r -> r.getBedTime() != null)
                .sorted(Comparator.comparing(SleepRecord::getBedTime).reversed())
                .limit(7)
                .forEach(r -> sb.append("- ").append(r.getBedTime().toLocalDate())
                        .append(" 上床 ").append(r.getBedTime().toLocalTime())
                        .append("，睡着 ").append(r.getSleepTime() == null ? "-" : r.getSleepTime().toLocalTime())
                        .append("，睡眠 ").append(r.getSleepMinutes() == null ? "-" : fmtMinutes(r.getSleepMinutes()))
                        .append("，质量 ").append(r.getQuality() == null ? "-" : r.getQuality() + " 分")
                        .append("，夜醒 ").append(r.getWakeCount() == null ? "-" : r.getWakeCount() + " 次")
                        .append("\n"));
        return sb.toString();
    }

    private String fmtMinutes(double minutes) {
        if (minutes <= 0) {
            return "未知";
        }
        long m = Math.round(minutes);
        return (m / 60) + " 小时 " + (m % 60) + " 分";
    }
}
