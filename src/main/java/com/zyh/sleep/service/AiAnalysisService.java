package com.zyh.sleep.service;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.zyh.sleep.dto.AiAdvice;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.MediaType;
import org.springframework.stereotype.Service;
import org.springframework.web.client.RestClient;

import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

@Service
public class AiAnalysisService {

    private static final long TTL_MILLIS = 6 * 60 * 60 * 1000L;
    private static final int MAX_ENTRIES = 200;

    @Value("${ai.api-key:}")
    private String apiKey;

    @Value("${ai.base-url:https://api.deepseek.com}")
    private String baseUrl;

    @Value("${ai.model:deepseek-chat}")
    private String model;

    private final RestClient restClient = RestClient.create();
    private final ObjectMapper objectMapper = new ObjectMapper();

    private final Map<String, CacheEntry> cache = Collections.synchronizedMap(
            new LinkedHashMap<>(16, 0.75f, true) {
                @Override
                protected boolean removeEldestEntry(Map.Entry<String, CacheEntry> eldest) {
                    return size() > MAX_ENTRIES;
                }
            });

    public AiAdvice analyze(Long userId, String summary) {
        if (apiKey == null || apiKey.isBlank()) {
            return new AiAdvice("还没有配置 AI API Key，请在 application.yml 里填写 ai.api-key 后重启服务。", false);
        }

        String key = userId + ":" + Integer.toHexString(summary.hashCode()) + ":" + summary.length();
        CacheEntry hit = cache.get(key);
        if (hit != null) {
            if (hit.expireAt > System.currentTimeMillis()) {
                return new AiAdvice(hit.advice, true);
            }
            cache.remove(key);
        }

        String advice = requestModel(summary);
        if (!advice.startsWith("AI 分析失败")) {
            cache.put(key, new CacheEntry(advice, System.currentTimeMillis() + TTL_MILLIS));
        }
        return new AiAdvice(advice, false);
    }

    private String requestModel(String summary) {
        Map<String, Object> body = Map.of(
                "model", model,
                "messages", List.of(
                        Map.of("role", "system", "content",
                                "你是一名睡眠健康顾问。根据用户的睡眠数据，用简洁中文给出 3 条具体、可执行的建议。"
                                        + "不要复述数据，不要空话套话，每条不超过 40 字。"),
                        Map.of("role", "user", "content", summary)
                ),
                "temperature", 0.7,
                "max_tokens", 500
        );

        try {
            String response = restClient.post()
                    .uri(baseUrl + "/chat/completions")
                    .header("Authorization", "Bearer " + apiKey)
                    .contentType(MediaType.APPLICATION_JSON)
                    .body(body)
                    .retrieve()
                    .body(String.class);

            JsonNode root = objectMapper.readTree(response);
            return root.path("choices").path(0).path("message").path("content")
                    .asText("AI 没有返回内容");
        } catch (Exception e) {
            return "AI 分析失败：" + e.getMessage();
        }
    }

    private record CacheEntry(String advice, long expireAt) {
    }
}
