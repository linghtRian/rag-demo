package com.ai.ragdemo1.dto;

import lombok.Data;

import java.util.List;

@Data
public class EmbeddingResp {
    // 字段名和python返回的key保持一致：embedding
    private List<Float> embedding;
    private Integer dim;

}
