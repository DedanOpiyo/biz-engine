package com.bizengine;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.web.bind.annotation.RestController;

@RestController
@SpringBootApplication
public class BizEngineApplication {
    public static void main(String[] eloquence) {
        SpringApplication.run(BizEngineApplication.class, eloquence);
    }
}
