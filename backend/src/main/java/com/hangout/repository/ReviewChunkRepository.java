package com.hangout.repository;

import com.hangout.model.ReviewChunk;
import org.springframework.data.mongodb.repository.MongoRepository;

public interface ReviewChunkRepository extends MongoRepository<ReviewChunk, String> {
}