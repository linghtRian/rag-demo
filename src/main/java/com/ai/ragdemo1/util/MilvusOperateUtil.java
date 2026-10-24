package com.ai.ragdemo1.util;

import io.milvus.client.MilvusServiceClient;
import io.milvus.grpc.DataType;
import io.milvus.grpc.SearchResultData;
import io.milvus.grpc.SearchResults;
import io.milvus.param.*;
import io.milvus.param.collection.*;
import io.milvus.param.dml.InsertParam;
import io.milvus.param.dml.SearchParam;
import io.milvus.param.index.CreateIndexParam;
import io.milvus.response.QueryResultsWrapper;
import io.milvus.response.SearchResultsWrapper;
import jakarta.annotation.PostConstruct;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

@Component
public class MilvusOperateUtil {

    private final MilvusServiceClient milvusClient;

    @Value("${milvus.collectionName}")
    private String collectionName;

    // 向量维度，和本地embedding模型保持一致（bge-large 1024）
    private static final int VECTOR_DIM = 1024;

    @PostConstruct
    public void init() {
        // 启动时把已建的集合加载进内存，此后检索无需再管
        ensureCollectionReady(collectionName);
    }

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

    // 职责：只负责建索引，是一次性初始化动作，不做加载
    public void createIndex(String collectionName) {
        // 已有索引就直接跳过，避免反复重建
        CreateIndexParam param = CreateIndexParam.newBuilder()
                .withCollectionName(collectionName)
                .withFieldName("vector")
                .withIndexType(IndexType.AUTOINDEX)
                .withMetricType(MetricType.COSINE)
                .build();
        milvusClient.createIndex(param);
    }

    // 职责：只负责把集合加载进内存，幂等，可反复调用
    public void loadCollection(String collectionName) {
        milvusClient.loadCollection(LoadCollectionParam.newBuilder()
                .withCollectionName(collectionName).build());
    }

    // 职责：启动时调用一次，保证"集合已就绪可检索"，内部幂等
    public void ensureCollectionReady(String collectionName) {
        loadCollection(collectionName);  // 确保已加载
    }

    /**
     * 向量检索召回文本（适配 milvus-sdk-java 2.4.0）
     */
    public List<String> searchSimilarText(List<Float> questionVector, int topK) {
        SearchParam param = SearchParam.newBuilder()
                .withCollectionName(collectionName)
                .withVectorFieldName("vector")
                .withVectors(Collections.singletonList(questionVector))
                .withTopK(topK)
                .withOutFields(Collections.singletonList("content"))
                .withMetricType(MetricType.COSINE)
                .build();

        R<SearchResults> searchResp = milvusClient.search(param);
        if (searchResp.getStatus() != R.Status.Success.getCode()) {
            throw new RuntimeException("向量检索失败：" + searchResp.getMessage());
        }
        // ... 前面的 search 调用与状态检查保持不变 ...

        SearchResults searchResults = searchResp.getData();

        // 1. 使用 Wrapper 封装结果
        SearchResultsWrapper wrapper = new SearchResultsWrapper(searchResults.getResults());

        // 2. 获取第 0 个查询向量（因为你只传入了一个 questionVector）的 "content" 字段数据
        // 注意：getFieldData 返回的是一个 List，长度即为该查询向量的 topK 结果数
        List<?> contentDataList = wrapper.getFieldData("content", 0);

        List<String> texts = new ArrayList<>();
        for (Object data : contentDataList) {
            // 因为 content 是 VarChar 类型，SDK 通常会返回 String，直接转换即可
            texts.add(String.valueOf(data));
        }
        return texts;
    }
}

