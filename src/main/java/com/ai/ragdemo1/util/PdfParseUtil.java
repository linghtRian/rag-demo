package com.ai.ragdemo1.util;

import org.apache.pdfbox.pdmodel.PDDocument;
import org.apache.pdfbox.text.PDFTextStripper;
import java.io.File;

public class PdfParseUtil {
    public static String extractText(File pdfFile) throws Exception{
        try(PDDocument document = PDDocument.load(pdfFile)){
            PDFTextStripper stripper = new PDFTextStripper();
            return stripper.getText(document);
        }
    }
}
