package com.hyperlocal.tantra.modules.listing.service;

import com.hyperlocal.tantra.modules.listing.repository.ListingRepository;
import com.hyperlocal.tantra.modules.listing.repository.ListingViewRepository;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/**
 * Tracks unique listing views for logged-in users only.
 *
 * Design:
 *  - Runs @Async so the listing detail API response is never blocked by a view write.
 *  - Uses INSERT ... ON CONFLICT DO NOTHING — DB guarantees dedup without a SELECT round-trip.
 *  - incrementViewCount is a single atomic UPDATE — safe under concurrent traffic.
 *  - Anonymous users (viewerUserId = null) are silently ignored; their open rate is irrelevant
 *    for seller analytics because they cannot contact the seller anyway.
 *  - Seller viewing their own listing is ignored (filtered by the caller before this is invoked).
 */
@Service
public class ListingViewService {

    private static final Logger log = LoggerFactory.getLogger(ListingViewService.class);

    @Autowired private ListingViewRepository listingViewRepository;
    @Autowired private ListingRepository     listingRepository;

    /**
     * Records a unique view for the given viewer on the given listing.
     * Must be called ONLY when viewerUserId is non-null and the viewer is NOT the listing owner.
     *
     * @param listingId    public listing ID (e.g. "LT1a2b3c4")
     * @param viewerUserId userId of the authenticated viewer
     */
    @Async
    @Transactional
    public void trackView(String listingId, String viewerUserId) {
        try {
            // insertIfNotExists returns 1 (new row) or 0 (conflict = already seen).
            int inserted = listingViewRepository.insertIfNotExists(listingId, viewerUserId);
            if (inserted == 1) {
                // Genuinely new viewer — increment the denormalized count on the listing.
                listingRepository.incrementViewCount(listingId);
                log.debug("[VIEW] New view recorded — listing={} viewer={}", listingId, viewerUserId);
            }
            // inserted == 0 → repeat visit, nothing to do
        } catch (Exception e) {
            // View tracking is analytics — never let it fail the main request flow.
            log.warn("[VIEW] Failed to track view for listing={} viewer={} — {}",
                    listingId, viewerUserId, e.getMessage());
        }
    }
}
