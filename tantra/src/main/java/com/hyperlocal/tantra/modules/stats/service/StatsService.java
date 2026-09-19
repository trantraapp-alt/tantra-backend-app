package com.hyperlocal.tantra.modules.stats.service;

import com.hyperlocal.tantra.modules.business.model.VerificationStatus;
import com.hyperlocal.tantra.modules.business.repository.BusinessProfileRepository;
import com.hyperlocal.tantra.modules.listing.repository.ListingRepository;
import com.hyperlocal.tantra.modules.stats.dto.StatTileDTO;
import com.hyperlocal.tantra.modules.stats.entity.StatTile;
import com.hyperlocal.tantra.modules.stats.repository.StatTileRepository;
import jakarta.annotation.PostConstruct;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

/**
 * Builds the home screen stats ribbon from DB-configured tiles.
 * Each tile has admin-editable label/icon/order; this service computes the live value.
 *
 * To add a new stat: seed a row in stat_tiles with a new stat_key,
 * then add the corresponding branch in computeValue().
 */
@Service
public class StatsService {

    @Autowired private StatTileRepository tileRepository;
    @Autowired private ListingRepository  listingRepository;
    @Autowired private BusinessProfileRepository businessProfileRepository;

    /**
     * Seeds default stat tiles on startup if the table is empty.
     * Flyway migrations can fail silently on Windows due to encoding; this is the fallback.
     */
    @PostConstruct
    public void seedDefaultTilesIfEmpty() {
        if (tileRepository.countAll() > 0) return;

        StatTile t1 = new StatTile();
        t1.setStatKey("ACTIVE_LISTINGS");
        t1.setLabelEn("Active Listings");
        t1.setLabelHi("Active Listings");
        t1.setIcon("AL");
        t1.setDisplayOrder(1);
        t1.setIsActive(true);

        StatTile t2 = new StatTile();
        t2.setStatKey("DISTRICTS");
        t2.setLabelEn("Districts");
        t2.setLabelHi("Districts");
        t2.setIcon("DI");
        t2.setDisplayOrder(2);
        t2.setIsActive(true);

        StatTile t3 = new StatTile();
        t3.setStatKey("VERIFIED_SELLERS");
        t3.setLabelEn("Verified Sellers");
        t3.setLabelHi("Verified Sellers");
        t3.setIcon("VS");
        t3.setDisplayOrder(3);
        t3.setIsActive(true);

        StatTile t4 = new StatTile();
        t4.setStatKey("TODAY_NEW");
        t4.setLabelEn("New Today");
        t4.setLabelHi("New Today");
        t4.setIcon("TN");
        t4.setDisplayOrder(4);
        t4.setIsActive(true);

        tileRepository.saveAll(Arrays.asList(t1, t2, t3, t4));
    }

    /**
     * Returns active stat tiles with live computed values.
     * No cache — COUNT queries are cheap and stats must stay real-time.
     * If load increases, add Caffeine/Redis with a 5-min TTL instead of ConcurrentMapCache.
     */
    public List<StatTileDTO> getStatTiles() {
        List<StatTile> tiles = tileRepository.findActiveTiles();
        List<StatTileDTO> result = new ArrayList<>();
        for (StatTile tile : tiles) {
            result.add(new StatTileDTO(
                    tile.getStatKey(),
                    tile.getLabelEn(),
                    tile.getLabelHi(),
                    tile.getIcon(),
                    computeValue(tile.getStatKey()),
                    tile.getDisplayOrder()
            ));
        }
        return result;
    }

    // ─── Value computation ────────────────────────────────────────────────────

    private String computeValue(String statKey) {
        if ("ACTIVE_LISTINGS".equals(statKey)) {
            return String.valueOf(listingRepository.countActiveListings());
        }
        if ("DISTRICTS".equals(statKey)) {
            return String.valueOf(listingRepository.countDistinctDistricts());
        }
        if ("VERIFIED_SELLERS".equals(statKey)) {
            return String.valueOf(
                    businessProfileRepository.countByVerificationStatusAndIsDeletedFalse(VerificationStatus.APPROVED));
        }
        if ("TODAY_NEW".equals(statKey)) {
            return String.valueOf(listingRepository.countTodayNew());
        }
        if ("TOTAL_SELLERS".equals(statKey)) {
            return String.valueOf(listingRepository.countDistinctSellers());
        }
        return "—";
    }
}
