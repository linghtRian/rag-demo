package com.ai.ragdemo1.service;

import com.ai.ragdemo1.util.EmbeddingUtil;
import com.ai.ragdemo1.util.LlmStreamUtil;
import com.ai.ragdemo1.util.MilvusOperateUtil;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.web.servlet.mvc.method.annotation.SseEmitter;

import java.util.List;

@Service
@Slf4j
public class RagService {

    @Autowired
    private EmbeddingUtil embeddingUtil;

    @Autowired
    private MilvusOperateUtil milvusOperateUtil;

    @Autowired
    private LlmStreamUtil llmStreamUtil;

    public void ragChat(String question, SseEmitter emitter) {
        try {
            List<Float> qVector = embeddingUtil.getEmbedding(question);   // 1. 问题向量
            List<String> ctx = milvusOperateUtil
                    .searchSimilarText(qVector, 3);       // 2. 召回Top3

            String prompt = "你是知识库问答助手，请严格基于下面参考内容回答用户问题，"
                    + "如果参考内容没有答案就如实说明不知道。\n"
                    + "【参考上下文】\n" + String.join("\n", ctx)
                    + "\n【用户问题】\n" + question;

            llmStreamUtil.streamChat(prompt, emitter);  // 3. 把完整 prompt 流式推送
        } catch (Exception e) {
            // 坑：必须捕获异常并通知前端，否则 emitter 永远挂起
            emitter.completeWithError(e);
        }
    }
}
