package com.example.bakersbackend.domain.ocr;

public record OcrResponse(
        String date,      // yyyy-MM-dd
        Double distance,  // 9.82
        String pace,      // 6'04''
        String duration   // 59:37
) {}
