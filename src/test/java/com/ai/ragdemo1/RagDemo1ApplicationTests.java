package com.ai.ragdemo1;

import com.ai.ragdemo1.util.ChunkUtil;
import com.ai.ragdemo1.util.PdfParseUtil;
import org.junit.jupiter.api.Test;
import org.springframework.boot.test.context.SpringBootTest;

import java.io.File;
import java.util.List;

@SpringBootTest
class RagDemo1ApplicationTests {

    @Test
    void contextLoads() {
    }

    @Test
    void testPdfParseAndChunk() throws Exception {
        // 1. 读取resources/file/test.pdf 文件
        // 方式：获取项目resources下的文件
        File pdfFile = new File("src/main/resources/file/test.pdf");

        // 判断文件是否存在，防止路径错误
        if (!pdfFile.exists()) {
            System.err.println("PDF文件不存在！路径：" + pdfFile.getAbsolutePath());
            return;
        }

        // 2. PDF提取全部文本
        String fullText = PdfParseUtil.extractText(pdfFile);
        System.out.println("===== PDF完整文本 =====");
        System.out.println(fullText);
        System.out.println("完整文本总字符数：" + fullText.length());
        System.out.println("\n===== 开始文本分片 =====");

        // 3. 调用分片工具
        List<String> chunkList = ChunkUtil.split(fullText);

        // 4. 循环打印每一块，查看分片+重叠效果
        for (int i = 0; i < chunkList.size(); i++) {
            String chunk = chunkList.get(i);
            System.out.println("【分片" + (i+1) + "】长度=" + chunk.length());
            System.out.println(chunk);
            System.out.println("------------------------");
        }
    }

}
