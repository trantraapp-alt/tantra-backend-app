package com.hyperlocal.tantra.modules.address.repository;

import com.hyperlocal.tantra.modules.address.entity.UserAddress;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface UserAddressRepository extends JpaRepository<UserAddress, Long> {

    List<UserAddress> findByUserIdAndIsDeletedFalseOrderByIsDefaultDescCreatedAtDesc(String userId);
    Optional<UserAddress> findByUserIdAndIsDefaultTrueAndIsDeletedFalse(String userId);
    Optional<UserAddress> findByAddressIdAndIsDeletedFalse(String addressId);
    long countByUserIdAndIsDeletedFalse(String userId);
    boolean existsByAddressId(String addressId);

    /** Clears the current default for a user (so exactly one address stays default). */
    @Modifying
    @Query("update UserAddress a set a.isDefault = false " +
            "where a.userId = :userId and a.isDefault = true and a.isDeleted = false")
    void clearDefault(@Param("userId") String userId);
}
