package com.ai.ragdemo1.controller;

import com.ai.ragdemo1.util.ChunkUtil;
import com.ai.ragdemo1.util.EmbeddingUtil;
import com.ai.ragdemo1.util.MilvusOperateUtil;
import com.ai.ragdemo1.util.PdfParseUtil;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.multipart.MultipartFile;

import java.io.File;
import java.util.List;

@RestController
@RequestMapping("/file")
public class FileUploadController {
    @Autowired
    private EmbeddingUtil embeddingUtil;

    @Autowired
    private MilvusOperateUtil milvusOperateUtil;

    @PostMapping("/upload")
    public String upload(@RequestParam("file") MultipartFile file) throws Exception{
        // 1. 将MultipartFile转成临时File对象
        File tempFile = File.createTempFile("temp",".pdf");
        file.transferTo(tempFile);
        // 2. PdfParseUtil提取全文
        String text = PdfParseUtil.extractText(tempFile);
        // 3. ChunkUtil分片
        List<String> chunkList = ChunkUtil.split(text);
        // 4. 循环生成向量，批量插入Milvus
        for(String chunk : chunkList){
            List<Float> vector = embeddingUtil.getEmbedding(chunk);
            milvusOperateUtil.insertData( chunk, vector);
        }
        tempFile.deleteOnExit();
        return "文档入库完成，分片数量："+chunkList.size();
    }
}