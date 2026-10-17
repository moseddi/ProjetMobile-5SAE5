package com.hangout.config;

import org.springframework.context.annotation.Configuration;
import org.springframework.data.mongodb.config.EnableMongoAuditing;

// Kept out of HangOutApplication so @WebMvcTest slices can start without a Mongo mapping context
@Configuration
@EnableMongoAuditing
public class MongoConfig {
}
