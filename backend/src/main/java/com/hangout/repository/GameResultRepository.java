package com.hangout.repository;

import com.hangout.model.GameResult;
import org.springframework.data.mongodb.repository.MongoRepository;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface GameResultRepository extends MongoRepository<GameResult, String> {
    List<GameResult> findByUserId(String userId);
    List<GameResult> findByUserIdOrderByPlayedAtDesc(String userId);
}
