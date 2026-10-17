package com.hangout.repository;

import com.hangout.model.MemorySession;
import org.springframework.data.mongodb.repository.MongoRepository;
import org.springframework.stereotype.Repository;

@Repository
public interface MemorySessionRepository extends MongoRepository<MemorySession, String> {
}
