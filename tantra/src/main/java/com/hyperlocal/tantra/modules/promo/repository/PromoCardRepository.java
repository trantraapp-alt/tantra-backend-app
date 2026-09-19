package com.hyperlocal.tantra.modules.promo.repository;

import com.hyperlocal.tantra.modules.promo.entity.PromoCard;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

@Repository
public interface PromoCardRepository extends JpaRepository<PromoCard, Long> {

    Optional<PromoCard> findByCardId(String cardId);

    /**
     * Active cards visible right now for a given district (or nationwide cards).
     * Nationwide = targetDistrict IS NULL. District-specific cards are additive.
     * Pass cardType (e.g. "CAROUSEL", "MINI", "SCHEME") to filter by visual type; null = all types.
     */
    @Query("select p from PromoCard p " +
           "where p.isActive = true " +
           "and (p.validFrom is null or p.validFrom <= :now) " +
           "and (p.validTo   is null or p.validTo   >= :now) " +
           "and (p.targetDistrict is null " +
           "     or :district is null " +
           "     or p.targetDistrict = :district) " +
           "and (:cardType is null or p.cardType = :cardType) " +
           "order by p.displayOrder asc")
    List<PromoCard> findActiveCards(@Param("district") String district,
                                    @Param("now") LocalDateTime now,
                                    @Param("cardType") String cardType);

    List<PromoCard> findAllByOrderByDisplayOrderAsc();
}
