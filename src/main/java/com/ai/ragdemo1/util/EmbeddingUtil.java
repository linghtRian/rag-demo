package com.ai.ragdemo1.util;

import com.ai.ragdemo1.dto.EmbeddingResp;
import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import okhttp3.*;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

import java.io.IOException;
import java.util.List;
import java.util.Map;

@Component
public class EmbeddingUtil {

    private final OkHttpClient okHttpClient;
    private final ObjectMapper objectMapper;

    @Value("${local-embedding.endpoint}")
    private String embeddingUrl;

    public EmbeddingUtil(OkHttpClient okHttpClient, ObjectMapper objectMapper) {
        this.okHttpClient = okHttpClient;
        this.objectMapper = objectMapper;
    }

    /**
     * 请求本地embedding服务，返回向量List<Float>
     */
    public List<Float> getEmbedding(String text) throws IOException {
        Map<String, String> reqBody = Map.of("input", text);
        String json = objectMapper.writeValueAsString(reqBody);

        RequestBody body = RequestBody.create(MediaType.get("application/json; charset=utf-8"), json);
        Request request = new Request.Builder()
                .url(embeddingUrl)
                .post(body)
                .build();

        try (Response response = okHttpClient.newCall(request).execute()) {
            if (!response.isSuccessful()) {
                throw new RuntimeException("embedding服务调用失败：" + response.code());
            }
            String respStr = response.body().string();
            // 使用DTO接收
            EmbeddingResp resp = objectMapper.readValue(respStr, EmbeddingResp.class);
            // 取embedding字段，不是vector！
            return resp.getEmbedding();
        }
    }
}