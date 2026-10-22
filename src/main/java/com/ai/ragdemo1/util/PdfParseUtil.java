package com.ai.ragdemo1.util;

import org.apache.pdfbox.pdmodel.PDDocument;
import org.apache.pdfbox.text.PDFTextStripper;
import java.io.File;
import java.io.IOException;
import java.io.InputStream;

public class PdfParseUtil {
    public static String extractText(File pdfFile) throws Exception{
        try(PDDocument document = PDDocument.load(pdfFile)){
            PDFTextStripper stripper = new PDFTextStripper();
            return stripper.getText(document);
        }
    }

    /** 新增重载：直接解析流，不落地临时文件。注意：本方法不关闭传入的 is */
    public static String extractText(InputStream is) throws IOException {
        if (is == null) {
            throw new IllegalArgumentException("PDF 输入流不能为空");
        }
        try (PDDocument document = PDDocument.load(is)) {
            PDFTextStripper stripper = new PDFTextStripper();
            return stripper.getText(document);
        }
    }
}
