package com.ai.ragdemo1;

import com.ai.ragdemo1.util.ChunkUtil;
import com.ai.ragdemo1.util.MinioUtil;
import com.ai.ragdemo1.util.PdfParseUtil;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.mock.web.MockMultipartFile;

import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.List;

import static org.junit.jupiter.api.Assertions.*;

@SpringBootTest
class RagDemo1ApplicationTests {

    @Autowired
    private MinioUtil minioUtil;

    @Test
    void contextLoads() throws IOException {
        Path pdfPath = Paths.get("src/main/resources/file/test.pdf");
        assertTrue(Files.exists(pdfPath), "测试文件不存在: " + pdfPath.toAbsolutePath());
        byte[] content = Files.readAllBytes(pdfPath);

        MockMultipartFile file = new MockMultipartFile(
                "file", "test.pdf", "application/pdf", content);

        // 1. 上传，拿到 objectName
        String objectName = minioUtil.uploadFile(file);
        assertNotNull(objectName);
        assertTrue(objectName.endsWith(".pdf"));
        System.out.println("objectName = " + objectName);

        // 2. 读取流校验
        try (InputStream in = minioUtil.getFileStream(objectName)) {
            byte[] readBytes = in.readAllBytes();
            assertTrue(readBytes.length > 0, "读取到的文件流为空");
            assertEquals(content.length, readBytes.length, "读取大小与原文件不一致");
        }
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
