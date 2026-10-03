package com.hangout.repository;

import com.hangout.model.Order;
import org.springframework.data.mongodb.repository.MongoRepository;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface OrderRepository extends MongoRepository<Order, String> {
    List<Order> findByUserId(String userId);
    List<Order> findByRestaurantId(String restaurantId);
    List<Order> findByReservationId(String reservationId);
}
