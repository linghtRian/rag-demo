package com.ai.ragdemo1.util;

import com.ai.ragdemo1.config.MinioProperties;
import io.minio.*;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;
import org.springframework.web.multipart.MultipartFile;

import java.io.InputStream;
import java.time.LocalDate;
import java.time.format.DateTimeFormatter;
import java.util.UUID;

@Slf4j
@Component
public class MinioUtil {

    private final MinioClient minioClient;
    private final MinioProperties minioProperties;

    public MinioUtil(MinioClient minioClient, MinioProperties minioProperties) {
        this.minioClient = minioClient;
        this.minioProperties = minioProperties;
    }

    /**
     * 上传文件到桶内，返回文件唯一对象路径
     */
    public String uploadFile(MultipartFile file) {
        if (file == null || file.isEmpty()) {
            throw new IllegalArgumentException("上传文件不能为空");
        }
        String bucketName = minioProperties.getBucketName();
        String extension = getExtension(file.getOriginalFilename());
        String datePath = LocalDate.now().format(DateTimeFormatter.ofPattern("yyyy/MM/dd"));
        String objectName = datePath + "/" + UUID.randomUUID().toString().replace("-", "") + extension;

        try {
            ensureBucketExists(bucketName);
            String contentType = file.getContentType() == null
                    ? "application/octet-stream" : file.getContentType();
            try (InputStream in = file.getInputStream()) {
                minioClient.putObject(
                        PutObjectArgs.builder()
                                .bucket(bucketName)
                                .object(objectName)
                                .stream(in, file.getSize(), -1)   // size 已知，partSize 传 -1 由 SDK 自动计算
                                .contentType(contentType)
                                .build()
                );
            }
            log.info("文件上传成功，objectName={}", objectName);
            return objectName;
        } catch (Exception e) {
            log.error("文件上传 MinIO 失败", e);
            throw new RuntimeException("文件上传 MinIO 失败: " + e.getMessage(), e);
        }
    }

    /**
     * 根据 objectName 读取文件流（调用方负责关闭）
     */
    public InputStream getFileStream(String objectName) {
        if (objectName == null || objectName.isBlank()) {
            throw new IllegalArgumentException("objectName 不能为空");
        }
        String bucketName = minioProperties.getBucketName();
        try {
            // 先 stat 一次，对象不存在时能给出更清晰的错误，而不是空的流
            minioClient.statObject(
                    StatObjectArgs.builder().bucket(bucketName).object(objectName).build()
            );
            return minioClient.getObject(
                    GetObjectArgs.builder().bucket(bucketName).object(objectName).build()
            );
        } catch (Exception e) {
            log.error("读取文件流失败，objectName={}", objectName, e);
            throw new RuntimeException("从 MinIO 读取文件失败: " + e.getMessage(), e);
        }
    }

    /** 桶不存在时自动创建，避免首次运行时手工建桶 */
    private void ensureBucketExists(String bucketName) throws Exception {
        boolean exists = minioClient.bucketExists(
                BucketExistsArgs.builder().bucket(bucketName).build()
        );
        if (!exists) {
            minioClient.makeBucket(MakeBucketArgs.builder().bucket(bucketName).build());
            log.info("Bucket 不存在，已自动创建: {}", bucketName);
        }
    }

    private String getExtension(String filename) {
        if (filename == null) return "";
        int idx = filename.lastIndexOf('.');
        return idx < 0 ? "" : filename.substring(idx);
    }
}
