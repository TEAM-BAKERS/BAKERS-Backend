package com.example.bakersbackend.domain.ocr;

import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

@RestController
@RequiredArgsConstructor
@RequestMapping("/ocr")
public class OcrController {

    private final OcrService ocrService;

    @PostMapping("/process")
    public ResponseEntity<OcrResponse> process(
            @RequestPart("image") MultipartFile image
    ) {
        OcrResponse res = ocrService.process(image);
        return ResponseEntity
                .status(HttpStatus.OK)
                .body(res);
    }
}