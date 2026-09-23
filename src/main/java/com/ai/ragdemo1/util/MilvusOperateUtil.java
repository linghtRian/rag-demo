package com.ai.ragdemo1.util;

import io.milvus.client.MilvusServiceClient;
import io.milvus.grpc.DataType;
import io.milvus.param.*;
import io.milvus.param.collection.*;
import io.milvus.param.dml.InsertParam;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

import java.util.ArrayList;
import java.util.List;

@Component
public class MilvusOperateUtil {

    private final MilvusServiceClient milvusClient;

    @Value("${milvus.collectionName}")
    private String collectionName;

    // 向量维度，和本地embedding模型保持一致（bge-large 1024）
    private static final int VECTOR_DIM = 1024;

    public MilvusOperateUtil(MilvusServiceClient milvusClient) {
        this.milvusClient = milvusClient;
    }

    /**
     * 如果集合不存在，则创建集合
     */
    public void createCollectionIfNotExists() {
        R<Boolean> hasCollR = milvusClient.hasCollection(HasCollectionParam.newBuilder()
                .withCollectionName(collectionName).build());
        if (hasCollR.getData()) {
            System.out.println("集合【" + collectionName + "】已存在，跳过创建");
            return;
        }

        // 定义字段
        List<FieldType> fieldTypes = new ArrayList<>();
        // id 主键，自增
        fieldTypes.add(FieldType.newBuilder()
                .withName("id")
                .withDataType(DataType.Int64)
                .withPrimaryKey(true)
                .withAutoID(true)
                .build());
        // chunk文本
        fieldTypes.add(FieldType.newBuilder()
                .withName("content")
                .withDataType(DataType.VarChar)
                .withMaxLength(4096)
                .build());
        // 向量字段
        fieldTypes.add(FieldType.newBuilder()
                .withName("vector")
                .withDataType(DataType.FloatVector)
                .withDimension(VECTOR_DIM)
                .build());

        CreateCollectionParam createParam = CreateCollectionParam.newBuilder()
                .withCollectionName(collectionName)
                .withFieldTypes(fieldTypes)
                .build();
        R<RpcStatus> resp = milvusClient.createCollection(createParam);
        if (resp.getStatus() != R.Status.Success.getCode()) {
            throw new RuntimeException("创建集合失败: " + resp.getMessage());
        }
        System.out.println("集合【" + collectionName + "】创建成功");
    }

    /**
     * 单条插入：content文本 + vector向量
     */
    public void insertData(String content, List<Float> vector) {
        List<InsertParam.Field> fields = new ArrayList<>();
        fields.add(new InsertParam.Field("content", List.of(content)));
        fields.add(new InsertParam.Field("vector", List.of(vector)));

        InsertParam insertParam = InsertParam.newBuilder()
                .withCollectionName(collectionName)
                .withFields(fields)
                .build();
        milvusClient.insert(insertParam);
        System.out.println("入库成功：" + content.substring(0, Math.min(50, content.length())));
    }
}

