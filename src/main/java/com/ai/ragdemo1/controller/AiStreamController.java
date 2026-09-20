package com.ai.ragdemo1.controller;

import com.ai.ragdemo1.util.LlmStreamUtil;
import jakarta.annotation.Resource;
import org.springframework.http.MediaType;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.servlet.mvc.method.annotation.SseEmitter;
import java.io.IOException;

@RestController
@RequestMapping("/ai")
public class AiStreamController {

    @Resource
    private LlmStreamUtil llmStreamUtil;

    /**
     * SSE流式对话接口
     * @param question 用户问题
     * @return SseEmitter
     */
    @GetMapping("/chat")
    public SseEmitter chat(@RequestParam("question") String question) {
        // 设置超时时间，0代表永不超时
        SseEmitter sseEmitter = new SseEmitter(0L);
        // 调用工具类发起流式请求
        llmStreamUtil.streamChat(question, sseEmitter);
        return sseEmitter;
    }

    // 设置超时时间30秒
    @GetMapping(value = "/stream", produces = MediaType.TEXT_EVENT_STREAM_VALUE)
    public SseEmitter chatStream() {
        SseEmitter sseEmitter = new SseEmitter(30000L);

        new Thread(() -> {
            try {
                String[] contentArr = {"RAG系统", "私有知识库问答", "Java实现SSE流式输出", "大模型应用开发实战"};
                for (String word : contentArr) {
                    sseEmitter.send(SseEmitter.event().data(word));
                    Thread.sleep(800);
                }
                // 流式推送完成，关闭连接
                sseEmitter.complete();
            } catch (IOException | InterruptedException e) {
                sseEmitter.completeWithError(e);
            }
        }).start();
        return sseEmitter;
    }
}