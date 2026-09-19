package com.hyperlocal.tantra.modules.contact.service;

import com.hyperlocal.tantra.constants.MessageConstants;
import com.hyperlocal.tantra.exception.LocalizedException;
import com.hyperlocal.tantra.modules.audit.service.AuditService;
import com.hyperlocal.tantra.modules.contact.dto.ContactRevealResponse;
import com.hyperlocal.tantra.modules.contact.entity.ContactReveal;
import com.hyperlocal.tantra.modules.contact.repository.ContactRevealRepository;
import com.hyperlocal.tantra.modules.listing.entity.Listing;
import com.hyperlocal.tantra.modules.listing.model.ListingStatus;
import com.hyperlocal.tantra.modules.listing.repository.ListingRepository;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.net.URLEncoder;
import java.nio.charset.StandardCharsets;
import java.time.LocalDateTime;
import java.util.Map;

/**
 * Handles the "View Contact Details" flow on a listing detail page.
 *
 * <p><b>Decision flow:</b></p>
 * <ol>
 *   <li>Validate listing is active and not deleted.</li>
 *   <li>Dedup: same buyer revealing same listing within 24 h is not double-counted.</li>
 *   <li>Log the reveal event + increment listing.contactRevealCount (first reveal only).</li>
 *   <li>Check {@code listing.showContact}:
 *       <ul>
 *         <li>{@code true}  → return full number (REVEALED_FULL).</li>
 *         <li>{@code false} → return WhatsApp-only URL, number not exposed (WHATSAPP_ONLY).</li>
 *       </ul>
 *   </li>
 * </ol>
 *
 * <p>Buyers are never blocked. No quota, no approval flow.
 * {@code showContact=false} redirects to WhatsApp instead of throwing an error.</p>
 */
@Service
public class ContactRevealService {

    private static final Logger log = LoggerFactory.getLogger(ContactRevealService.class);

    @Autowired private ListingRepository         listingRepository;
    @Autowired private ContactRevealRepository   contactRevealRepository;
    @Autowired private AuditService              auditService;

    /**
     * Reveal contact for a listing.
     *
     * @param buyerUserId user ID extracted from the JWT (auth.getName())
     * @param listingId   listing being viewed
     * @param ipAddress   caller's IP (from request header, may be null)
     */
    @Transactional
    public ContactRevealResponse reveal(String buyerUserId, String listingId, String ipAddress) {

        // 1. Validate listing
        Listing listing = listingRepository.findByListingId(listingId)
                .orElseThrow(() -> new LocalizedException(
                        MessageConstants.CONTACT_LISTING_INACTIVE_EN,
                        MessageConstants.CONTACT_LISTING_INACTIVE_HI));

        if (Boolean.TRUE.equals(listing.getIsDeleted())
                || listing.getStatus() == ListingStatus.DELETED
                || listing.getStatus() == ListingStatus.INACTIVE) {
            throw new LocalizedException(
                    MessageConstants.CONTACT_LISTING_INACTIVE_EN,
                    MessageConstants.CONTACT_LISTING_INACTIVE_HI);
        }

        // 2. Dedup — same buyer + listing within 24 h is not re-counted
        LocalDateTime since = LocalDateTime.now().minusHours(24);
        boolean alreadyRevealed = contactRevealRepository
                .findRecentReveal(listingId, buyerUserId, since)
                .isPresent();

        // 3. Log reveal + increment counter (first reveal only)
        if (!alreadyRevealed) {
            ContactReveal reveal = new ContactReveal();
            reveal.setListingId(listingId);
            reveal.setSellerUserId(listing.getUserId());
            reveal.setBuyerUserId(buyerUserId);
            reveal.setContactNumber(resolveContactNumber(listing));
            reveal.setIpAddress(ipAddress);
            contactRevealRepository.save(reveal);
            listingRepository.incrementContactRevealCount(listingId);

            log.info("[CONTACT_REVEAL] buyer={} listing={} seller={} fresh=true",
                    buyerUserId, listingId, listing.getUserId());

            auditService.record("CONTACT_REVEALED", "LISTING", listingId, buyerUserId,
                    Map.of("sellerUserId", listing.getUserId(),
                            "contactMasked", mask(resolveContactNumber(listing))));
        } else {
            log.debug("[CONTACT_REVEAL] buyer={} listing={} — duplicate within 24h, not counted",
                    buyerUserId, listingId);
        }

        // 4. Always return full contact — Call + WhatsApp + Copy always available
        String mobile = resolveContactNumber(listing);
        String alt    = resolveAltNumber(listing);
        String title  = deriveTitle(listing);
        String waUrl  = buildWhatsAppUrl(mobile, title);

        return ContactRevealResponse.builder()
                .action("REVEALED_FULL")
                .mobileNumber(mobile)
                .altMobileNumber(alt)
                .whatsappUrl(waUrl)
                .listingTitle(title)
                .freshReveal(!alreadyRevealed)
                .build();
    }

    // ─── Helpers ─────────────────────────────────────────────────────────────

    /**
     * Resolves the contact number for the listing.
     * Priority: listing.contactNumber → listing.address.mobileNumber
     */
    private String resolveContactNumber(Listing listing) {
        if (listing.getContactNumber() != null && !listing.getContactNumber().isBlank()) {
            return listing.getContactNumber();
        }
        if (listing.getAddress() != null && listing.getAddress().getMobileNumber() != null) {
            return listing.getAddress().getMobileNumber();
        }
        return null;
    }

    /** Resolves alternate contact number from address snapshot. */
    private String resolveAltNumber(Listing listing) {
        if (listing.getAddress() != null) {
            String alt = listing.getAddress().getAltMobileNumber();
            return (alt != null && !alt.isBlank()) ? alt : null;
        }
        return null;
    }

    /**
     * Derives a human-readable listing title from the attributes jsonb.
     * Mirrors ListingService.deriveTitle logic — keeps contact module self-contained.
     */
    private String deriveTitle(Listing listing) {
        Map<String, Object> attrs = listing.getAttributes();
        if (attrs != null) {
            Object explicit = attrs.get("title");
            if (explicit != null && !explicit.toString().isBlank()) return explicit.toString();
            String[] preferred = {"variety", "breed", "brand", "serviceType", "species", "model", "type"};
            for (String key : preferred) {
                Object val = attrs.get(key);
                if (val != null && !val.toString().isBlank()) return val.toString();
            }
        }
        return "your listing";
    }

    /** Builds a WhatsApp deep-link with a pre-filled message about the listing. */
    private String buildWhatsAppUrl(String mobile, String listingTitle) {
        String number = (mobile != null) ? mobile.replaceAll("[^0-9]", "") : "";
        String message = "Hi, I am interested in your listing: " + listingTitle;
        String encoded = URLEncoder.encode(message, StandardCharsets.UTF_8);
        return "https://wa.me/91" + number + "?text=" + encoded;
    }

    /** Masks a phone number for safe logging: 9876543210 → 98****3210 */
    private String mask(String number) {
        if (number == null || number.length() < 6) return "****";
        return number.substring(0, 2) + "****" + number.substring(number.length() - 4);
    }
}
