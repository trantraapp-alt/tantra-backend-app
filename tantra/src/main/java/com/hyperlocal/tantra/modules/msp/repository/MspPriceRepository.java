package com.hyperlocal.tantra.modules.msp.repository;

import com.hyperlocal.tantra.modules.msp.entity.MspPrice;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface MspPriceRepository extends JpaRepository<MspPrice, Long> {

    /** All active MSP rates for the given season+year, ordered by crop. */
    List<MspPrice> findBySeasonAndYearAndIsActiveTrueOrderByCropKey(String season, Integer year);

    /** All active MSP entries regardless of season — for the full MSP table view. */
    List<MspPrice> findByYearAndIsActiveTrueOrderBySeasonAscCropKeyAsc(Integer year);

    /** Delete all MSP entries for a given year — used by bulk-replace import. */
    void deleteByYear(Integer year);
}
