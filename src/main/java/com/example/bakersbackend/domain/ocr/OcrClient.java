package com.example.bakersbackend.domain.ocr;

import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.*;
import org.springframework.stereotype.Component;
import org.springframework.web.client.RestTemplate;
import org.springframework.web.multipart.MultipartFile;

import java.util.Base64;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;

@Slf4j
@Component
@RequiredArgsConstructor
public class OcrClient {

    private final OcrProperties ocrProperties;
    private final ObjectMapper objectMapper;

    private final RestTemplate restTemplate = new RestTemplate();

    public String requestOcr(MultipartFile image) {
        try {
            // 1) 이미지 Base64 인코딩
            String base64Image = Base64.getEncoder()
                    .encodeToString(image.getBytes());

            // 2) CLOVA 요청 바디 구성 (Docs 형식에 맞게 필요하면 필드명 조정)
            Map<String, Object> imageObj = new HashMap<>();
            imageObj.put("format", getExtension(image.getOriginalFilename()));
            imageObj.put("name", image.getOriginalFilename());
            imageObj.put("data", base64Image);

            Map<String, Object> body = new HashMap<>();
            body.put("version", "V2");
            body.put("requestId", UUID.randomUUID().toString());
            body.put("timestamp", System.currentTimeMillis());
            body.put("images", List.of(imageObj));

            String jsonBody = objectMapper.writeValueAsString(body);

            // 3) 헤더 세팅
            HttpHeaders headers = new HttpHeaders();
            headers.setContentType(MediaType.APPLICATION_JSON);
            headers.add("X-OCR-SECRET", ocrProperties.getSecretKey());

            HttpEntity<String> request = new HttpEntity<>(jsonBody, headers);

            // 4) 호출
            ResponseEntity<String> response = restTemplate.exchange(
                    ocrProperties.getUrl(),
                    HttpMethod.POST,
                    request,
                    String.class
            );

            log.info("OCR response status: {}", response.getStatusCode());

            return response.getBody();
        } catch (Exception e) {
            log.error("OCR 요청 실패", e);
            throw new RuntimeException("OCR 요청 중 오류가 발생했습니다.", e);
        }
    }

    private String getExtension(String filename) {
        if (filename == null) return "jpg";
        int idx = filename.lastIndexOf('.');
        if (idx < 0) return "jpg";
        return filename.substring(idx + 1);
    }
}
