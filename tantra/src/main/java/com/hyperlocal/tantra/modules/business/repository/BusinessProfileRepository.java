package com.hyperlocal.tantra.modules.business.repository;

import com.hyperlocal.tantra.modules.business.entity.BusinessProfile;
import com.hyperlocal.tantra.modules.business.model.VerificationStatus;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface BusinessProfileRepository extends JpaRepository<BusinessProfile, Long> {

    List<BusinessProfile> findByUserIdAndIsDeletedFalseOrderByCreatedAtDesc(String userId);
    Optional<BusinessProfile> findByProfileIdAndIsDeletedFalse(String profileId);
    Page<BusinessProfile> findByVerificationStatusAndIsDeletedFalse(VerificationStatus status, Pageable pageable);
    Page<BusinessProfile> findByVerificationStatusInAndIsDeletedFalse(List<VerificationStatus> statuses, Pageable pageable);
    List<BusinessProfile> findByUserIdAndVerificationStatusAndIsDeletedFalse(String userId, VerificationStatus status);
    boolean existsByProfileId(String profileId);

    // Approval Tracker counts.
    long countByIsDeletedFalse();
    long countByVerificationStatusAndIsDeletedFalse(VerificationStatus status);
    long countByVerifiedByAndVerificationStatusAndIsDeletedFalse(String verifiedBy, VerificationStatus status);
}
