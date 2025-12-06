package com.example.bakersbackend;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.scheduling.annotation.EnableScheduling;

@SpringBootApplication
@EnableScheduling
public class BakersBackendApplication {

    public static void main(String[] args) {
        SpringApplication.run(BakersBackendApplication.class, args);
    }

}
