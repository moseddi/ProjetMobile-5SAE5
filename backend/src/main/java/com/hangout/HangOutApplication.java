package com.hangout;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.data.mongodb.config.EnableMongoAuditing;

@SpringBootApplication
@EnableMongoAuditing
public class HangOutApplication {
    public static void main(String[] args) {
        SpringApplication.run(HangOutApplication.class, args);
    }
}
