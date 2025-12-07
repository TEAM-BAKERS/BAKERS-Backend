package com.example.bakersbackend.domain.ocr;

import lombok.Getter;
import lombok.Setter;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.context.annotation.Configuration;

@Configuration
@ConfigurationProperties(prefix = "ocr")
@Getter
@Setter
public class OcrProperties {

    private String url;
    private String secretKey;
}
