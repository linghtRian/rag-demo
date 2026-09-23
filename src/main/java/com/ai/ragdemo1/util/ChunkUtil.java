package com.ai.ragdemo1.util;

import org.apache.pdfbox.pdmodel.PDDocument;
import org.apache.pdfbox.text.PDFTextStripper;
import org.springframework.stereotype.Component;

import java.io.File;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;

@Component
public class ChunkUtil {
    private static final int CHUNK_SIZE = 500;
    private static final int OVERLAP_SIZE = 100;

    public static List<String> split(String text){
        List<String> chunkList = new ArrayList<>();
        int start = 0;
        int len = text.length();
        while(start < len){
            int end = Math.min(start + CHUNK_SIZE, len);
            String chunk = text.substring(start, end);
            chunkList.add(chunk);
            start = start + CHUNK_SIZE - OVERLAP_SIZE;
        }
        return chunkList;
    }

    /**
     * 简单文本分块，按字符长度切，可根据需求加重叠窗口
     * @param rawText 全文
     * @param chunkSize 块大小
     * @param overlap 重叠字符
     */
    public List<String> splitText(String rawText, int chunkSize, int overlap) {
        List<String> chunks = new ArrayList<>();
        int start = 0;
        while (start < rawText.length()) {
            int end = Math.min(start + chunkSize, rawText.length());
            chunks.add(rawText.substring(start, end));
            start = end - overlap;
        }
        return chunks;
    }

    /**
     * 流式迭代切片，不一次性加载所有chunk到内存
     * @param rawText 完整文本
     * @param chunkSize 块大小
     * @param overlap 重叠字符
     * @return 迭代器，遍历的时候才生成chunk
     */
    public Iterator<String> splitTextStream(String rawText, int chunkSize, int overlap) {
        return new Iterator<>() {
            private int start = 0;

            @Override
            public boolean hasNext() {
                return start < rawText.length();
            }

            @Override
            public String next() {
                int end = Math.min(start + chunkSize, rawText.length());
                String chunk = rawText.substring(start, end);
                // 修复：防止start回退！新的start不能小于当前start
                int nextStart = end - overlap;
                if(nextStart <= start){
                    nextStart = end; // 不再重叠，直接跳到末尾，跳出循环
                }
                start = nextStart;
                return chunk;
            }
        };
    }


    /**
     * 读取PDF全部文本
     */
    public String readPdf(String pdfPath) throws IOException {
        try (PDDocument document = PDDocument.load(new File(pdfPath))) {
            PDFTextStripper stripper = new PDFTextStripper();
            return stripper.getText(document);
        }
    }
}
