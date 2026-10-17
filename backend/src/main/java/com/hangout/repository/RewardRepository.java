package com.hangout.repository;

import com.hangout.model.Reward;
import org.springframework.data.mongodb.repository.MongoRepository;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface RewardRepository extends MongoRepository<Reward, String> {
    List<Reward> findByActiveTrue();
    List<Reward> findByActiveTrueOrderByPointsCostAsc();
    List<Reward> findByActiveTrueAndRestaurantIdNotNull();
}
