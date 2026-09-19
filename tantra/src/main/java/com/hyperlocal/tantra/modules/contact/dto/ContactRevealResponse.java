package com.hyperlocal.tantra.modules.contact.dto;

import com.fasterxml.jackson.annotation.JsonInclude;
import lombok.Builder;
import lombok.Data;

/**
 * Response returned after a buyer taps "View Contact Details".
 *
 * <p>Two possible actions:</p>
 * <ul>
 *   <li>{@code REVEALED_FULL}    — seller allows direct contact;
 *       {@code mobileNumber} and optionally {@code altMobileNumber} are populated.</li>
 *   <li>{@code WHATSAPP_ONLY}    — seller prefers not to expose number directly;
 *       only {@code whatsappUrl} is returned. Frontend opens the link without
 *       displaying the raw number.</li>
 * </ul>
 */
@Data
@Builder
@JsonInclude(JsonInclude.Include.NON_NULL)
public class ContactRevealResponse {

    /**
     * One of: {@code REVEALED_FULL} | {@code WHATSAPP_ONLY}
     * Frontend switches UI based on this value.
     */
    private String action;

    /** Seller's primary contact number. Present only when action = REVEALED_FULL. */
    private String mobileNumber;

    /** Seller's alternate contact number. Present only when action = REVEALED_FULL and alt exists. */
    private String altMobileNumber;

    /**
     * Pre-constructed WhatsApp deep-link. Always present.
     * Format: https://wa.me/91XXXXXXXXXX?text=<encoded-message>
     */
    private String whatsappUrl;

    /** Listing title — used to pre-fill WhatsApp message. */
    private String listingTitle;

    /**
     * true  = this reveal was freshly counted (first time buyer reveals this listing).
     * false = buyer already revealed this listing within 24 h; not double-counted.
     */
    private boolean freshReveal;
}
