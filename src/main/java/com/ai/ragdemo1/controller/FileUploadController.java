package com.ai.ragdemo1.controller;

import com.ai.ragdemo1.util.*;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.multipart.MultipartFile;

import java.io.File;
import java.io.InputStream;
import java.util.List;

@RestController
@RequestMapping("/file")
public class FileUploadController {
    @Autowired
    private EmbeddingUtil embeddingUtil;

    @Autowired
    private MilvusOperateUtil milvusOperateUtil;

    @Autowired
    private MinioUtil minioUtil;

    @PostMapping("/upload")
    public String upload(@RequestParam("file") MultipartFile file) throws Exception{
        // 1.上传minio持久化
        String objectPath = minioUtil.uploadFile(file);
        // 2.获取文件流解析pdf
        InputStream inputStream = minioUtil.getFileStream(objectPath);
        String text = PdfParseUtil.extractText(inputStream);
        // 3.分片、向量化、入库
        List<String> chunkList = ChunkUtil.split(text);
        for(String chunk : chunkList){
            List<Float> vector = embeddingUtil.getEmbedding(chunk);
            milvusOperateUtil.insertData(chunk, vector);
        }
        return "文档入库完成｜MinIO路径："+objectPath+"｜分片数量："+chunkList.size();

//        // 1. 将MultipartFile转成临时File对象
//        File tempFile = File.createTempFile("temp",".pdf");
//        file.transferTo(tempFile);
//        // 2. PdfParseUtil提取全文
//        String text = PdfParseUtil.extractText(tempFile);
//        // 3. ChunkUtil分片
//        List<String> chunkList = ChunkUtil.split(text);
//        // 4. 循环生成向量，批量插入Milvus
//        for(String chunk : chunkList){
//            List<Float> vector = embeddingUtil.getEmbedding(chunk);
//            milvusOperateUtil.insertData( chunk, vector);
//        }
//        tempFile.deleteOnExit();
//        return "文档入库完成，分片数量："+chunkList.size();
    }
}