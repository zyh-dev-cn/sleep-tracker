package com.zyh.sleep.service;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.MediaType;
import org.springframework.stereotype.Service;
import org.springframework.web.client.RestClient;

import java.util.List;
import java.util.Map;

@Service
public class AiAnalysisService {

    @Value("${ai.api-key:}")
    private String apiKey;

    @Value("${ai.base-url:https://api.deepseek.com}")
    private String baseUrl;

    @Value("${ai.model:deepseek-chat}")
    private String model;

    private final RestClient restClient = RestClient.create();
    private final ObjectMapper objectMapper = new ObjectMapper();

    public String analyze(String summary) {
        if (apiKey == null || apiKey.isBlank()) {
            return "还没有配置 AI API Key，请在 application.yml 里填写 ai.api-key 后重启服务。";
        }

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
}
