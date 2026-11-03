package com.hangout.repository;

import com.hangout.model.HangOut;
import org.springframework.data.mongodb.repository.MongoRepository;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface HangOutRepository extends MongoRepository<HangOut, String> {
    List<HangOut> findByCreatorId(String creatorId);
    List<HangOut> findByRestaurantId(String restaurantId);
    List<HangOut> findByStatus(HangOut.HangOutStatus status);
}
