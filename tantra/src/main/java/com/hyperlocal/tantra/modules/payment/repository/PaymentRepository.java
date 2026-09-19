package com.hyperlocal.tantra.modules.payment.repository;

import com.hyperlocal.tantra.modules.payment.entity.PaymentRecord;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface PaymentRepository extends JpaRepository<PaymentRecord, Long> {

    Optional<PaymentRecord> findByPaymentId(String paymentId);

    Optional<PaymentRecord> findByGatewayOrderId(String gatewayOrderId);

    List<PaymentRecord> findByUserIdOrderByCreatedAtDesc(String userId);

    @Query("select p from PaymentRecord p " +
           "where (:status is null or p.status = :status) " +
           "order by p.createdAt desc")
    Page<PaymentRecord> findAll(@Param("status") String status, Pageable pageable);
}
