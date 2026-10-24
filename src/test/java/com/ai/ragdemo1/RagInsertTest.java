package com.ai.ragdemo1;

import com.ai.ragdemo1.util.ChunkUtil;
import com.ai.ragdemo1.util.EmbeddingUtil;
import com.ai.ragdemo1.util.MilvusOperateUtil;
import com.ai.ragdemo1.util.PdfParseUtil;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;

import java.io.File;
import java.io.IOException;
import java.util.Iterator;
import java.util.List;

@SpringBootTest
public class RagInsertTest {

    @Autowired
    private ChunkUtil chunkUtil;
    @Autowired
    private EmbeddingUtil embeddingUtil;
    @Autowired
    private MilvusOperateUtil milvusOperateUtil;

    @Test
    void testInsertPdfToMilvus() throws Exception {
        // 1. 初始化Milvus集合
        milvusOperateUtil.createCollectionIfNotExists();

        // 2. 读取PDF并切片
        // 读取resources/file/test.pdf 文件
        // 方式：获取项目resources下的文件
        File pdfFile = new File("src/main/resources/file/test.pdf");

        // 判断文件是否存在，防止路径错误
        if (!pdfFile.exists()) {
            System.err.println("PDF文件不存在！路径：" + pdfFile.getAbsolutePath());
            return;
        }

        // 2. PDF提取全部文本
        String fullText = PdfParseUtil.extractText(pdfFile);

        // 清洗文本，压缩连续空白字符
        fullText = fullText.replaceAll("\\s+", " ");

        Iterator<String> chunkIter = chunkUtil.splitTextStream(fullText, 500, 80);
        int count = 0;
        while (chunkIter.hasNext()) {
            String chunk = chunkIter.next();
            List<Float> vector = embeddingUtil.getEmbedding(chunk);
            milvusOperateUtil.insertData(chunk, vector);
            count++;
            System.out.println("已入库第" + count + "块");
        }
        System.out.println("全部文档入库完成，总块数：" + count);

//        // 3. 流式迭代，一块一块处理，不在内存保存全部chunk
//        Iterator<String> chunkIter = chunkUtil.splitTextStream(fullText, 500, 80);
//        int count = 0;
//        while (chunkIter.hasNext()) {
//            String chunk = chunkIter.next();
//            List<Float> vector = embeddingUtil.getEmbedding(chunk);
//            milvusOperateUtil.insertData(chunk, vector);
//            count++;
//            System.out.println("已入库第" + count + "块");
//        }
//        System.out.println("全部文档入库完成，总块数：" + count);

//        List<String> chunks = chunkUtil.splitText(fullText, 500, 80);
//        System.out.println("切片总数：" + chunks.size());
//
//        // 3. 循环生成向量并写入Milvus
//        for (String chunk : chunks) {
//            List<Float> vector = embeddingUtil.getEmbedding(chunk);
//            milvusOperateUtil.insertData(chunk, vector);
//        }
//        System.out.println("全部文档入库完成");
    }
}
