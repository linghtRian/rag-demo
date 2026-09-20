package com.ai.ragdemo1.util;

import java.util.ArrayList;
import java.util.List;

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
}
