package com.hangout.repository;

import com.hangout.model.OwnerApplication;
import org.springframework.data.mongodb.repository.MongoRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface OwnerApplicationRepository extends MongoRepository<OwnerApplication, String> {
    List<OwnerApplication> findByStatus(OwnerApplication.Status status);

    Optional<OwnerApplication> findFirstByEmailIgnoreCaseOrderByCreatedAtDesc(String email);

    boolean existsByEmailIgnoreCaseAndStatus(String email, OwnerApplication.Status status);

    long countByStatus(OwnerApplication.Status status);
}
