package com.hangout.repository;

import com.hangout.model.Voucher;
import org.springframework.data.mongodb.repository.MongoRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface VoucherRepository extends MongoRepository<Voucher, String> {
    List<Voucher> findByUserId(String userId);

    List<Voucher> findByUserIdAndRestaurantId(String userId, String restaurantId);

    Optional<Voucher> findByCode(String code);

    boolean existsByCode(String code);
}
