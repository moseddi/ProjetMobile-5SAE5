package com.hangout.repository;

import com.hangout.model.Redemption;
import org.springframework.data.mongodb.repository.MongoRepository;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface RedemptionRepository extends MongoRepository<Redemption, String> {
    List<Redemption> findByUserIdOrderByCreatedAtDesc(String userId);
}
