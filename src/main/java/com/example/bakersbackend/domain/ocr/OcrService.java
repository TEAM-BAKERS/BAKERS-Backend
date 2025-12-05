package com.example.bakersbackend.domain.ocr;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.web.multipart.MultipartFile;

import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.util.*;

@Slf4j
@Service
@RequiredArgsConstructor
public class OcrService {

    private final OcrParser ocrParser;
    private final OcrProperties properties;

    private final ObjectMapper objectMapper = new ObjectMapper();

    public OcrResponse process(MultipartFile image) {

        // 1. Clova OCR 호출 -> 텍스트 목록 추출
        List<String> extractedTexts = callClovaAndExtractTexts(image);

        // 2. 파서로 필요한 정보 추출
        return ocrParser.parse(extractedTexts);
    }

    private List<String> callClovaAndExtractTexts(MultipartFile image) {
        try {
            // 1. Base64 인코딩
            String base64Image = Base64.getEncoder().encodeToString(image.getBytes());

            // 2. 요청 JSON 생성
            Map<String, Object> imageObj = Map.of(
                    "format", "jpg",
                    "data", base64Image,
                    "name", "input"
            );

            Map<String, Object> requestBody = Map.of(
                    "version", "V2",
                    "requestId", UUID.randomUUID().toString(),
                    "timestamp", System.currentTimeMillis(),
                    "images", List.of(imageObj)
            );

            String jsonBody = objectMapper.writeValueAsString(requestBody);

            // 3. HTTP 요청 생성
            HttpRequest request = HttpRequest.newBuilder()
                    .uri(URI.create(properties.getUrl()))
                    .header("Content-Type", "application/json")
                    .header("X-OCR-SECRET", properties.getSecretKey())
                    .POST(HttpRequest.BodyPublishers.ofString(jsonBody))
                    .build();

            // 4. 요청 실행
            HttpClient client = HttpClient.newHttpClient();
            HttpResponse<String> response = client.send(request, HttpResponse.BodyHandlers.ofString());

            // 5. 응답 파싱
            JsonNode root = objectMapper.readTree(response.body());
            JsonNode fields = root.get("images").get(0).get("fields");

            // 6. inferText 리스트 추출
            List<String> texts = new ArrayList<>();
            for (JsonNode field : fields) {
                texts.add(field.get("inferText").asText());
            }

            return texts;

        } catch (Exception e) {
            log.error("OCR 호출 중 오류 발생", e);
            throw new RuntimeException("OCR 서버 호출 실패", e);
        }
    }
}
