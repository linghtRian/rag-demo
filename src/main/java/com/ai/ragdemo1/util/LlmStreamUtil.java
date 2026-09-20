package com.ai.ragdemo1.util;

import com.ai.ragdemo1.config.LlmConfig;
import com.fasterxml.jackson.databind.ObjectMapper;
import jakarta.annotation.Resource;
import okhttp3.*;
import org.springframework.stereotype.Component;
import org.springframework.web.servlet.mvc.method.annotation.SseEmitter;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.nio.charset.StandardCharsets;
import java.util.List;
import java.util.Map;

@Component
public class LlmStreamUtil {

    @Resource
    private LlmConfig llmConfig;

    private final OkHttpClient client = new OkHttpClient();
    private final ObjectMapper objectMapper = new ObjectMapper();

    /**
     * 大模型流式调用
     * @param question 用户提问
     * @param sseEmitter SSE推送对象
     */
    public void streamChat(String question, SseEmitter sseEmitter) {
        try {
            // 构建请求体，开启stream:true
            Map<String, Object> requestBodyMap = Map.of(
                    "model", llmConfig.getModel(),
                    "stream", true,
                    "messages", List.of(
                            Map.of("role", "user", "content", question)
                    )
            );
            String jsonBody = objectMapper.writeValueAsString(requestBodyMap);
            RequestBody requestBody = RequestBody.create(MediaType.get("application/json; charset=utf-8"), jsonBody);

            Request request = new Request.Builder()
                    .url(llmConfig.getEndpoint())
                    .addHeader("Authorization", "Bearer " + llmConfig.getApiKey())
                    .post(requestBody)
                    .build();

            // 异步请求
            client.newCall(request).enqueue(new Callback() {
                @Override
                public void onFailure(Call call, IOException e) {
                    sseEmitter.completeWithError(e);
                }

                @Override
                public void onResponse(Call call, Response response) throws IOException {
                    if (!response.isSuccessful()) {
                        sseEmitter.completeWithError(new RuntimeException("接口调用失败：" + response.code()));
                        return;
                    }
                    ResponseBody body = response.body();
                    if (body == null) {
                        sseEmitter.complete();
                        return;
                    }
                    // 按行读取SSE流
                    try (BufferedReader reader = new BufferedReader(new InputStreamReader(body.byteStream(), StandardCharsets.UTF_8))) {
                        String line;
                        while ((line = reader.readLine()) != null) {
                            // 只处理 data: 开头的分片
                            if (line.startsWith("data: ")) {
                                String data = line.substring("data: ".length()).trim();
                                // 结束标记 [DONE]
                                if ("[DONE]".equals(data)) {
                                    sseEmitter.complete();
                                    return;
                                }
                                // 解析json，提取content
                                Map<String, Object> respMap = objectMapper.readValue(data, Map.class);
                                List<Map<String, Object>> choices = (List<Map<String, Object>>) respMap.get("choices");
                                if (choices != null && !choices.isEmpty()) {
                                    Map<String, Object> choice = choices.get(0);
                                    Map<String, Object> delta = (Map<String, Object>) choice.get("delta");
                                    if (delta != null && delta.containsKey("content")) {
                                        String content = (String) delta.get("content");
                                        // SSE推送到前端
                                        sseEmitter.send(content);
                                    }
                                }
                            }
                        }
                        sseEmitter.complete();
                    } catch (Exception e) {
                        sseEmitter.completeWithError(e);
                    } finally {
                        body.close();
                    }
                }
            });
        } catch (Exception e) {
            sseEmitter.completeWithError(e);
        }
    }
}
