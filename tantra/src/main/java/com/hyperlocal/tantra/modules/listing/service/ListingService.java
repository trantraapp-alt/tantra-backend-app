package com.hyperlocal.tantra.modules.listing.service;

import com.hyperlocal.tantra.constants.MessageConstants;
import com.hyperlocal.tantra.exception.LocalizedException;
import com.hyperlocal.tantra.modules.address.entity.UserAddress;
import com.hyperlocal.tantra.modules.address.service.AddressService;
import com.hyperlocal.tantra.modules.audit.service.AuditService;
import com.hyperlocal.tantra.modules.auth.entity.User;
import com.hyperlocal.tantra.modules.auth.repository.UserRepository;
import com.hyperlocal.tantra.modules.forms.entity.FormDefinition;
import com.hyperlocal.tantra.modules.forms.model.FormField;
import com.hyperlocal.tantra.modules.forms.model.ListingType;
import com.hyperlocal.tantra.modules.forms.model.LocalizedText;
import com.hyperlocal.tantra.modules.forms.repository.FormDefinitionRepository;
import com.hyperlocal.tantra.modules.listing.dto.ListingCardDTO;
import com.hyperlocal.tantra.modules.listing.dto.ListingFilter;
import com.hyperlocal.tantra.modules.listing.dto.ListingPatchRequest;
import com.hyperlocal.tantra.modules.listing.dto.ListingRequest;
import com.hyperlocal.tantra.modules.listing.dto.ListingResponse;
import com.hyperlocal.tantra.modules.listing.entity.Listing;
import com.hyperlocal.tantra.modules.listing.model.Address;
import com.hyperlocal.tantra.modules.listing.model.ListingStatus;
import com.hyperlocal.tantra.modules.listing.repository.ListingRepository;
import com.hyperlocal.tantra.modules.master.entity.ModuleCategory;
import com.hyperlocal.tantra.modules.master.repository.AppModuleRepository;
import com.hyperlocal.tantra.modules.master.repository.ModuleCategoryRepository;
import com.hyperlocal.tantra.modules.subscription.entity.SubscriptionPlan;
import com.hyperlocal.tantra.modules.subscription.service.SubscriptionService;
import com.hyperlocal.tantra.modules.upload.service.StorageService;
import com.hyperlocal.tantra.utils.GeoUtil;
import com.hyperlocal.tantra.utils.IdGeneratorUtil;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.LocalDateTime;
import java.util.*;
import java.util.stream.Collectors;

/**
 * Listing CRUD + discovery (browse, nearby, search). Writes respect form metadata flags.
 * Every save populates lat/lng (extracted from address) and updates the tsvector search column.
 * Browse and search results are enriched with the seller's subscription badge.
 */
@Service
public class ListingService {

    private static final Logger log = LoggerFactory.getLogger(ListingService.class);

    @Autowired private ListingRepository listingRepository;
    @Autowired private UserRepository userRepository;
    @Autowired private FormDefinitionRepository formRepository;
    @Autowired private StorageService storageService;
    @Autowired private AddressService addressService;
    @Autowired private AuditService auditService;
    @Autowired private SubscriptionService subscriptionService;
    @Autowired private AppModuleRepository moduleRepository;
    @Autowired private ModuleCategoryRepository categoryRepository;
    @Autowired private com.hyperlocal.tantra.modules.business.repository.BusinessProfileRepository businessProfileRepository;
    @Autowired private ListingViewService listingViewService;

    // ─── CREATE ──────────────────────────────────────────────────────────────

    @Transactional
    public ListingResponse createListing(ListingRequest req, String mobileNumber) {
        log.info("[LISTING] Creating listing — user={} category={}", mobileNumber, req.getCategoryId());
        User user = currentUser(mobileNumber);
        if (req.getCategoryId() == null) {
            throw new LocalizedException(
                    MessageConstants.LISTING_CATEGORY_REQUIRED_EN,
                    MessageConstants.LISTING_CATEGORY_REQUIRED_HI);
        }

        Listing listing = new Listing();
        listing.setListingId(generateUniqueListingId());
        listing.setUserId(user.getUserId());
        listing.setUserMobileNumber(user.getMobileNumber());
        listing.setCreatedBy(user.getUserId());
        listing.setUpdatedBy(user.getUserId());
        listing.setCategoryId(req.getCategoryId());
        // Prefer explicitly provided moduleId; fall back to auto-resolve from category.
        // This ensures moduleId is always populated even when the frontend omits it,
        // which is critical for home feed module sections, nearby filters, and search indexing.
        listing.setModuleId(resolveModuleId(req.getModuleId(), req.getCategoryId()));
        listing.setListingType(resolveListingType(req.getListingType(), req.getAttributes()));
        listing.setActualPrice(req.getActualPrice());
        listing.setOfferedPrice(req.getOfferedPrice());
        listing.setDiscountPct(computeDiscount(
                discountBase(req.getActualPrice(), listing.getListingType(), req.getAttributes()),
                req.getOfferedPrice()));
        listing.setQuantity(req.getQuantity());
        listing.setUnit(req.getUnit());
        listing.setIsNegotiable(Boolean.TRUE.equals(req.getIsNegotiable()));
        listing.setContactNumber(req.getContactNumber());
        listing.setShowContact(Boolean.TRUE.equals(req.getShowContact()));
        // Delivery only applies to SELL listings — always false for RENT
        if (listing.getListingType() == ListingType.SELL) {
            listing.setDeliveryAvailable(Boolean.TRUE.equals(req.getDeliveryAvailable()));
        }
        if (req.getImages() != null) listing.setImages(req.getImages());
        Map<String, Object> attrs = req.getAttributes() != null
                ? new HashMap<>(req.getAttributes()) : new HashMap<>();
        if (req.getListingTitle() != null && !req.getListingTitle().isBlank())
            attrs.put("title", req.getListingTitle().trim());
        listing.setAttributes(attrs);
        applyAddress(listing, req, user);

        // Flash Deal: only premium subscribers with flashDealAllowed = true may enable this.
        if (Boolean.TRUE.equals(req.getFlashDeal())) {
            boolean allowed = subscriptionService.getActivePlan(user.getUserId())
                    .map(plan -> Boolean.TRUE.equals(plan.getFlashDealAllowed()))
                    .orElse(false);
            if (!allowed) {
                log.warn("[LISTING] Flash Deal denied — user={} has no eligible premium plan", user.getUserId());
                throw new LocalizedException(
                        MessageConstants.FLASH_DEAL_PREMIUM_REQUIRED_EN,
                        MessageConstants.FLASH_DEAL_PREMIUM_REQUIRED_HI);
            }
            listing.setFlashDeal(true);
            listing.setDiscountExpiresAt(req.getDiscountExpiresAt());
            log.info("[LISTING] Flash Deal enabled for listing by user={} expires={}", user.getUserId(), req.getDiscountExpiresAt());
        }

        resolveFormDefinition(listing.getCategoryId(), listing.getListingType())
                .ifPresent(form -> {
                    listing.setFormId(form.getId());
                    listing.setFormVersion(form.getVersion());
                });

        Listing saved = listingRepository.save(listing);
        updateSearchVector(saved);
        log.info("[LISTING] Created {} for user {}", saved.getListingId(), user.getUserId());
        auditService.record("LISTING_CREATED", "LISTING", saved.getListingId(), user.getUserId(),
                Map.of("categoryId", String.valueOf(req.getCategoryId()),
                       "listingType", saved.getListingType().name()));
        return respond(saved, MessageConstants.LISTING_CREATE_SUCCESS_EN, MessageConstants.LISTING_CREATE_SUCCESS_HI);
    }

    // ─── FULL UPDATE (PUT) ────────────────────────────────────────────────────

    @Transactional
    public ListingResponse updateListing(String listingId, ListingRequest req, String mobileNumber) {
        log.info("[LISTING] Updating listing {} — user={}", listingId, mobileNumber);
        User user = currentUser(mobileNumber);
        Listing listing = ownedListing(listingId, user);
        Map<String, FormField> fields = formFields(listing);

        if (req.getActualPrice()   != null && editable("actualPrice",   fields)) listing.setActualPrice(req.getActualPrice());
        if (req.getOfferedPrice()  != null && editable("offeredPrice",  fields)) listing.setOfferedPrice(req.getOfferedPrice());
        if (req.getQuantity()      != null && editable("quantity",      fields)) listing.setQuantity(req.getQuantity());
        if (req.getUnit()          != null && editable("unit",          fields)) listing.setUnit(req.getUnit());
        if (req.getIsNegotiable()  != null && editable("isNegotiable",  fields)) listing.setIsNegotiable(req.getIsNegotiable());
        if (req.getContactNumber() != null && editable("contactNumber", fields)) listing.setContactNumber(req.getContactNumber());
        if (req.getShowContact()   != null && editable("showContact",   fields)) listing.setShowContact(req.getShowContact());
        if (req.getDeliveryAvailable() != null && listing.getListingType() == ListingType.SELL)
            listing.setDeliveryAvailable(req.getDeliveryAvailable());
        if (editable("address", fields)) applyAddress(listing, req, user);

        if (req.getImages() != null && editable("images", fields)) {
            List<String> removed = new ArrayList<>(listing.getImages() == null ? List.of() : listing.getImages());
            removed.removeAll(req.getImages());
            if (!removed.isEmpty()) storageService.delete(removed);
            listing.setImages(req.getImages());
        }

        if (req.getAttributes() != null) {
            Map<String, Object> merged = listing.getAttributes() == null
                    ? new HashMap<>() : new HashMap<>(listing.getAttributes());
            req.getAttributes().forEach((key, value) -> {
                FormField field = fields.get(key);
                if (field == null || !Boolean.FALSE.equals(field.getEditableOnUpdate())) merged.put(key, value);
            });
            listing.setAttributes(merged);
        }
        // Flash Deal toggle: turning on requires a premium plan with flashDealAllowed = true.
        if (req.getFlashDeal() != null) {
            if (Boolean.TRUE.equals(req.getFlashDeal())) {
                boolean allowed = subscriptionService.getActivePlan(user.getUserId())
                        .map(plan -> Boolean.TRUE.equals(plan.getFlashDealAllowed()))
                        .orElse(false);
                if (!allowed) {
                    log.warn("[LISTING] Flash Deal update denied — user={} has no eligible premium plan", user.getUserId());
                    throw new LocalizedException(
                            MessageConstants.FLASH_DEAL_PREMIUM_REQUIRED_EN,
                            MessageConstants.FLASH_DEAL_PREMIUM_REQUIRED_HI);
                }
                listing.setFlashDeal(true);
                listing.setDiscountExpiresAt(req.getDiscountExpiresAt());
                log.info("[LISTING] Flash Deal updated for {} by user={} expires={}", listingId, user.getUserId(), req.getDiscountExpiresAt());
            } else {
                // Seller is turning off their own flash deal
                listing.setFlashDeal(false);
                listing.setDiscountExpiresAt(null);
            }
        }

        listing.setDiscountPct(computeListingDiscount(listing));
        stampUpdate(listing, user);
        Listing saved = listingRepository.save(listing);
        updateSearchVector(saved);
        log.info("[LISTING] Updated {} by user {}", listingId, user.getUserId());
        auditService.record("LISTING_UPDATED", "LISTING", saved.getListingId(), user.getUserId());
        return respond(saved, MessageConstants.LISTING_UPDATE_SUCCESS_EN, MessageConstants.LISTING_UPDATE_SUCCESS_HI);
    }

    // ─── INLINE / QUICK UPDATE (PATCH) ───────────────────────────────────────

    @Transactional
    public ListingResponse patchListing(String listingId, ListingPatchRequest req, String mobileNumber) {
        log.info("[LISTING] Patching listing {} — user={}", listingId, mobileNumber);
        User user = currentUser(mobileNumber);
        Listing listing = ownedListing(listingId, user);
        Map<String, FormField> fields = formFields(listing);

        boolean priceChanged = false;
        if (req.getActualPrice()  != null && inline("actualPrice",  fields)) { listing.setActualPrice(req.getActualPrice());  priceChanged = true; }
        if (req.getOfferedPrice() != null && inline("offeredPrice", fields)) { listing.setOfferedPrice(req.getOfferedPrice()); priceChanged = true; }
        if (priceChanged) listing.setDiscountPct(computeListingDiscount(listing));
        if (req.getQuantity()    != null && inline("quantity",    fields)) listing.setQuantity(req.getQuantity());
        if (req.getUnit()        != null && inline("unit",        fields)) listing.setUnit(req.getUnit());
        if (req.getIsNegotiable()!= null && inline("isNegotiable",fields)) listing.setIsNegotiable(req.getIsNegotiable());
        if (req.getDeliveryAvailable() != null && listing.getListingType() == ListingType.SELL)
            listing.setDeliveryAvailable(req.getDeliveryAvailable());
        if (req.getStatus() != null) listing.setStatus(req.getStatus());

        if (req.getAttributes() != null) {
            Map<String, Object> merged = listing.getAttributes() == null
                    ? new HashMap<>() : new HashMap<>(listing.getAttributes());
            req.getAttributes().forEach((key, value) -> {
                FormField field = fields.get(key);
                if (field != null && Boolean.TRUE.equals(field.getInlineEditable())) merged.put(key, value);
            });
            listing.setAttributes(merged);
        }
        stampUpdate(listing, user);
        Listing saved = listingRepository.save(listing);
        auditService.record("LISTING_PATCHED", "LISTING", saved.getListingId(), user.getUserId());
        return respond(saved, MessageConstants.LISTING_UPDATE_SUCCESS_EN, MessageConstants.LISTING_UPDATE_SUCCESS_HI);
    }

    // ─── DELETE (soft) ────────────────────────────────────────────────────────

    @Transactional
    public ListingResponse deleteListing(String listingId, String mobileNumber) {
        log.info("[LISTING] Deleting listing {} — user={}", listingId, mobileNumber);
        User user = currentUser(mobileNumber);
        Listing listing = ownedListing(listingId, user);
        listing.setIsDeleted(true);
        listing.setStatus(ListingStatus.DELETED);
        stampUpdate(listing, user);
        Listing saved = listingRepository.save(listing);
        auditService.record("LISTING_DELETED", "LISTING", saved.getListingId(), user.getUserId());
        return respond(saved, MessageConstants.LISTING_DELETE_SUCCESS_EN, MessageConstants.LISTING_DELETE_SUCCESS_HI);
    }

    // ─── READS ───────────────────────────────────────────────────────────────

    public Page<Listing> getMyListings(String mobileNumber, ListingType listingType,
                                        ListingStatus status, Pageable pageable) {
        User user = currentUser(mobileNumber);
        return listingRepository.findMine(user.getUserId(), listingType, status, pageable);
    }

    /**
     * Fetches a listing by its public ID and increments the view count —
     * but skips the increment when the authenticated viewer is the listing owner
     * (prevents self-views from polluting analytics).
     *
     * @param listingId    public listing ID
     * @param viewerMobile JWT mobile number of the viewer, or null if unauthenticated
     */
    public Listing getByListingId(String listingId, String viewerMobile) {
        Listing listing = listingRepository.findByListingId(listingId)
                .orElseThrow(() -> new LocalizedException(
                        MessageConstants.LISTING_NOT_FOUND_EN, MessageConstants.LISTING_NOT_FOUND_HI));
        String viewerUserId = resolveViewerUserId(viewerMobile);
        boolean isOwner = viewerUserId != null && viewerUserId.equals(listing.getUserId());

        // Track view only for logged-in non-owners. Runs async — does not block this response.
        // Dedup is guaranteed by the unique constraint in listing_views table.
        if (viewerUserId != null && !isOwner) {
            listingViewService.trackView(listingId, viewerUserId);
        }

        return listing;
    }

    /**
     * Browse listings with full filter support — category, module, price, geo, subscription sort.
     * When lat/lng+radius provided: bounding-box pre-filter in SQL + exact Haversine in Java.
     *
     * @param filter        filter criteria (userId = show only this seller; excludeUserId = hide this seller)
     * @param pageable      pagination
     * @param viewerMobile  JWT mobile of the authenticated viewer, or null if unauthenticated.
     *                      When provided, the viewer's own listings are excluded from the results
     *                      so sellers do not see their own listings in the buyer-facing feed.
     */
    public Page<ListingCardDTO> browseListings(ListingFilter filter, Pageable pageable, String viewerMobile) {
        log.debug("[LISTING] Browse — filter={}", filter);

        // Exclude the authenticated seller's own listings from buyer-facing feeds.
        // Not applied when filter.userId is set (i.e. /by-seller page — shows a specific seller's listings).
        if (viewerMobile != null && filter.getUserId() == null) {
            filter.setExcludeUserId(resolveViewerUserId(viewerMobile));
        }

        Double lat = filter.getLatitude(), lng = filter.getLongitude();
        boolean hasCoords = lat != null && lng != null;

        Double latMin = null, latMax = null, lngMin = null, lngMax = null;
        if (hasCoords && filter.getRadiusKm() != null) {
            double r = filter.getRadiusKm();
            latMin = lat - GeoUtil.latOffset(r); latMax = lat + GeoUtil.latOffset(r);
            lngMin = lng - GeoUtil.lngOffset(r, lat); lngMax = lng + GeoUtil.lngOffset(r, lat);
        }

        String listingTypeStr = filter.getListingType() != null ? filter.getListingType().name() : null;
        LocalDateTime after = postedAfter(filter.getPostedWithin());
        boolean withPhoto = Boolean.TRUE.equals(filter.getWithPhoto());
        boolean verified  = Boolean.TRUE.equals(filter.getVerifiedSeller());

        // Strip sort from Pageable — native queries own ORDER BY entirely;
        // Pageable sort would cause Spring to append a duplicate ORDER BY clause → 500.
        Pageable cleanPageable = PageRequest.of(pageable.getPageNumber(), pageable.getPageSize());

        Page<Listing> raw;
        if (hasCoords) {
            raw = listingRepository.findWithFiltersNearby(
                    filter.getCategoryId(), filter.getModuleId(), listingTypeStr,
                    filter.getMinPrice(), filter.getMaxPrice(),
                    filter.getDistrict(), filter.getState(), filter.getUserId(),
                    filter.getExcludeUserId(), filter.getAttrFilter(),
                    filter.getSortBy(), filter.getSortDir(),
                    after, latMin, latMax, lngMin, lngMax,
                    filter.getSellerType(), withPhoto, verified,
                    lat, lng, cleanPageable);
        } else {
            raw = listingRepository.findWithFilters(
                    filter.getCategoryId(), filter.getModuleId(), listingTypeStr,
                    filter.getMinPrice(), filter.getMaxPrice(),
                    filter.getDistrict(), filter.getState(), filter.getUserId(),
                    filter.getExcludeUserId(), filter.getAttrFilter(),
                    filter.getSortBy(), filter.getSortDir(),
                    after, latMin, latMax, lngMin, lngMax,
                    filter.getSellerType(), withPhoto, verified,
                    cleanPageable);
        }

        List<ListingCardDTO> cards = raw.getContent().stream()
                .map(l -> toCard(l, lat, lng))
                .filter(c -> {
                    if (filter.getRadiusKm() != null && c.getDistanceKm() != null) {
                        return c.getDistanceKm() <= filter.getRadiusKm();
                    }
                    return true;
                })
                .collect(Collectors.toList());

        return new PageImpl<>(cards, pageable, raw.getTotalElements());
    }

    /**
     * Similar listings in the same category, premium-first, excluding the current listing.
     * Also excludes the authenticated viewer's own listings so a seller browsing similar items
     * does not see their own other listings.
     *
     * @param viewerMobile JWT mobile of the authenticated viewer, or null if unauthenticated
     */
    public List<ListingCardDTO> getSimilarListings(String listingId, Integer categoryId, int limit, String viewerMobile) {
        String excludeUserId = resolveViewerUserId(viewerMobile);
        return listingRepository.findSimilar(categoryId, listingId, excludeUserId, limit)
                .stream().map(l -> toCard(l, null, null)).collect(Collectors.toList());
    }

    /** Legacy: simple category browse (used internally by home feed). */
    public Page<Listing> getByCategory(Integer categoryId, ListingType listingType, Pageable pageable) {
        return listingRepository.findByCategory(categoryId, ListingStatus.ACTIVE, listingType, pageable);
    }

    // ─── SEARCH VECTOR UPDATE ─────────────────────────────────────────────────

    /**
     * Builds and persists the tsvector for full-text search.
     * Category + module names provide weight A (most important); location B; attribute values C.
     * Uses 'simple' dictionary so both Hindi and English text are indexed without stemming distortion.
     */
    public void updateSearchVector(Listing listing) {
        try {
            String categoryText = buildCategoryText(listing.getCategoryId(), listing.getModuleId());
            String locationText = buildLocationText(listing.getAddress());
            String attrText     = buildAttributeText(listing.getAttributes());
            listingRepository.updateSearchVector(listing.getListingId(), categoryText, locationText, attrText);
        } catch (Exception e) {
            log.warn("[LISTING] Failed to update search_vector for {} — {}", listing.getListingId(), e.getMessage());
        }
    }

    // ─── CARD ENRICHMENT (subscription badge) ────────────────────────────────

    public ListingCardDTO toCard(Listing l, Double userLat, Double userLng) {
        ListingCardDTO card = new ListingCardDTO();
        card.setListingId(l.getListingId());
        card.setUserId(l.getUserId());
        card.setModuleId(l.getModuleId());
        card.setCategoryId(l.getCategoryId());

        // Fetch category once — used for title derivation and quality description
        com.hyperlocal.tantra.modules.master.entity.ModuleCategory category =
                categoryRepository.findById(l.getCategoryId()).orElse(null);

        card.setListingTitle(deriveTitleFromCategory(l, category));
        card.setListingType(l.getListingType() != null ? l.getListingType().name() : null);
        card.setActualPrice(l.getActualPrice());
        card.setOfferedPrice(l.getOfferedPrice());
        card.setDiscountPct(l.getDiscountPct());
        card.setQuantity(l.getQuantity());
        card.setUnit(l.getUnit());
        card.setIsNegotiable(l.getIsNegotiable());
        card.setShowContact(l.getShowContact());
        card.setDeliveryAvailable(l.getDeliveryAvailable());
        card.setImages(l.getImages());
        card.setAddress(l.getAddress());
        card.setAttributes(l.getAttributes());
        card.setStatus(l.getStatus() != null ? l.getStatus().name() : null);
        card.setViewCount(l.getViewCount());
        card.setContactRevealCount(l.getContactRevealCount());
        card.setCreatedAt(l.getCreatedAt());
        card.setFlashDeal(Boolean.TRUE.equals(l.getFlashDeal()));
        card.setDiscountExpiresAt(l.getDiscountExpiresAt());

        // isNew: posted within last 24 hours
        card.setIsNew(l.getCreatedAt() != null &&
                l.getCreatedAt().isAfter(LocalDateTime.now().minusHours(24)));

        // Distance calculation
        if (userLat != null && userLng != null && l.getLatitude() != null && l.getLongitude() != null) {
            card.setDistanceKm(GeoUtil.roundKm(
                    GeoUtil.distanceKm(userLat, userLng, l.getLatitude(), l.getLongitude())));
        }

        // Subscription badge enrichment
        subscriptionService.getActivePlan(l.getUserId()).ifPresentOrElse(plan -> {
            card.setIsHighlighted(plan.getListingHighlight());
            card.setHighlightColor(plan.getHighlightColor());
            card.setSellerBadge(plan.getBadgeLabel());
            card.setSellerPlanKey(plan.getPlanKey());
        }, () -> {
            card.setIsHighlighted(false);
        });

        // sellerVerified: seller has an APPROVED BusinessProfile
        boolean verified = !businessProfileRepository
                .findByUserIdAndVerificationStatusAndIsDeletedFalse(
                        l.getUserId(),
                        com.hyperlocal.tantra.modules.business.model.VerificationStatus.APPROVED)
                .isEmpty();
        card.setSellerVerified(verified);

        // Quality description — set from category master (null-safe: null if admin hasn't set it yet)
        if (category != null) {
            card.setQualityDescEn(category.getQualityDescEn());
            card.setQualityDescHi(category.getQualityDescHi());
        }

        return card;
    }

    /**
     * Flash deals: listings marked flash_deal=true with an active discount.
     * Excludes the authenticated viewer's own flash-deal listings.
     *
     * @param viewerMobile JWT mobile of the authenticated viewer, or null if unauthenticated
     */
    public List<ListingCardDTO> getFlashDeals(String district, int limit, String viewerMobile) {
        String excludeUserId = resolveViewerUserId(viewerMobile);
        return listingRepository.findFlashDeals(district, excludeUserId, limit)
                .stream().map(l -> toCard(l, null, null)).collect(Collectors.toList());
    }

    // ─── PRIVATE HELPERS ─────────────────────────────────────────────────────

    private String deriveTitle(Listing l) {
        return deriveTitleFromCategory(l, categoryRepository.findById(l.getCategoryId()).orElse(null));
    }

    /** Derives listing title using a pre-fetched category — avoids extra DB call when category is already loaded. */
    private String deriveTitleFromCategory(Listing l,
            com.hyperlocal.tantra.modules.master.entity.ModuleCategory category) {
        Map<String, Object> attrs = l.getAttributes();
        String catName = category != null ? category.getCategoryNameEn() : "";
        if (attrs != null) {
            // Explicit title wins
            Object explicit = attrs.get("title");
            if (explicit != null && !explicit.toString().isBlank()) return explicit.toString();
            // Fall back to first notable attribute value + category name
            String[] preferred = {"variety", "breed", "brand", "serviceType", "species", "model", "type"};
            for (String key : preferred) {
                Object val = attrs.get(key);
                if (val != null && !val.toString().isBlank()) {
                    String attrPart = val.toString();
                    return catName.isBlank() ? attrPart : attrPart + " — " + catName;
                }
            }
        }
        // Last resort: category name only
        return catName.isBlank() ? null : catName;
    }

    private User currentUser(String mobileNumber) {
        return userRepository.findByMobileNumber(mobileNumber)
                .orElseThrow(() -> new LocalizedException(
                        MessageConstants.LISTING_USER_NOT_FOUND_EN, MessageConstants.LISTING_USER_NOT_FOUND_HI));
    }

    /**
     * Resolves the moduleId for a listing.
     * If the frontend explicitly sent a moduleId, use it.
     * Otherwise, look up the moduleId from the category table — every ModuleCategory knows its parent module.
     * This guarantees moduleId is never null on a saved listing, which is required for:
     *   - home feed module sections (findForModuleSection filters by moduleId)
     *   - nearby/browse moduleId filters
     *   - search vector weight A (includes module name text)
     */
    private Integer resolveModuleId(Integer requestedModuleId, Integer categoryId) {
        if (requestedModuleId != null) return requestedModuleId;
        if (categoryId == null) return null;
        return categoryRepository.findById(categoryId)
                .map(ModuleCategory::getModuleId)
                .orElse(null);
    }

    /**
     * Resolves a viewer's mobile number to their userId for exclusion filtering.
     * Returns null when mobileNumber is blank or the user is not found —
     * null means "no exclusion", which is the safe default for unauthenticated requests.
     */
    private String resolveViewerUserId(String mobileNumber) {
        if (mobileNumber == null || mobileNumber.isBlank()) return null;
        return userRepository.findByMobileNumber(mobileNumber)
                .map(User::getUserId).orElse(null);
    }

    private Listing ownedListing(String listingId, User user) {
        Listing listing = listingRepository.findByListingId(listingId)
                .orElseThrow(() -> new LocalizedException(
                        MessageConstants.LISTING_NOT_FOUND_EN, MessageConstants.LISTING_NOT_FOUND_HI));
        if (!user.getUserId().equals(listing.getUserId())) {
            throw new LocalizedException(
                    MessageConstants.LISTING_NOT_OWNER_EN, MessageConstants.LISTING_NOT_OWNER_HI);
        }
        return listing;
    }

    private Map<String, FormField> formFields(Listing listing) {
        FormDefinition form = null;
        if (listing.getFormId() != null) form = formRepository.findById(listing.getFormId()).orElse(null);
        if (form == null) form = resolveFormDefinition(listing.getCategoryId(), listing.getListingType()).orElse(null);
        if (form == null || form.getFields() == null) return Collections.emptyMap();
        Map<String, FormField> map = new HashMap<>();
        for (FormField field : form.getFields()) map.put(field.getFieldKey(), field);
        return map;
    }

    private Optional<FormDefinition> resolveFormDefinition(Integer categoryId, ListingType listingType) {
        Optional<FormDefinition> form = formRepository
                .findFirstByCategoryIdAndListingTypeAndIsActiveTrueOrderByVersionDesc(categoryId, listingType);
        if (form.isEmpty() && listingType != ListingType.BOTH) {
            form = formRepository
                    .findFirstByCategoryIdAndListingTypeAndIsActiveTrueOrderByVersionDesc(categoryId, ListingType.BOTH);
        }
        return form;
    }

    private ListingType resolveListingType(ListingType requested, Map<String, Object> attributes) {
        if (attributes != null && attributes.get("availableFor") != null) {
            String value = attributes.get("availableFor").toString().trim().toLowerCase();
            if ("rent".equals(value)) return ListingType.RENT;
            if ("sell".equals(value)) return ListingType.SELL;
        }
        if (requested == ListingType.RENT || requested == ListingType.SELL) return requested;
        return ListingType.SELL;
    }

    private boolean editable(String key, Map<String, FormField> fields) {
        FormField field = fields.get(key);
        return field == null || !Boolean.FALSE.equals(field.getEditableOnUpdate());
    }

    private boolean inline(String key, Map<String, FormField> fields) {
        FormField field = fields.get(key);
        return field != null && Boolean.TRUE.equals(field.getInlineEditable());
    }

    private void stampUpdate(Listing listing, User user) {
        listing.setUpdatedBy(user.getUserId());
        listing.setUpdatedAt(LocalDateTime.now());
    }

    private void applyAddress(Listing listing, ListingRequest req, User user) {
        if (req.getAddressId() != null && !req.getAddressId().isBlank()) {
            UserAddress src = addressService.requireOwned(req.getAddressId(), user.getUserId());
            listing.setAddress(toAddress(src));
            listing.setAddressId(src.getAddressId());
            extractGeo(listing, src.getLatitude(), src.getLongitude());
            return;
        }
        if (Boolean.TRUE.equals(req.getUseDefaultAddress())) {
            UserAddress def = addressService.findDefault(user.getUserId());
            if (def != null) {
                listing.setAddress(toAddress(def));
                listing.setAddressId(def.getAddressId());
                extractGeo(listing, def.getLatitude(), def.getLongitude());
            }
            return;
        }
        if (req.getAddress() != null) {
            listing.setAddress(req.getAddress());
            listing.setAddressId(null);
            extractGeo(listing, req.getAddress().getLatitude(), req.getAddress().getLongitude());
        }
    }

    /** Pulls lat/lng out of the address and stores them as top-level indexed columns. */
    private void extractGeo(Listing listing, Double lat, Double lng) {
        listing.setLatitude(lat);
        listing.setLongitude(lng);
    }

    private Address toAddress(UserAddress source) {
        Address address = new Address();
        address.setFullAddress(source.getFullAddress());
        address.setCountry(source.getCountry());
        address.setState(source.getState());
        address.setDistrict(source.getDistrict());
        address.setCity(source.getCity());
        address.setVillage(source.getVillage());
        address.setPinCode(source.getPinCode());
        address.setLatitude(source.getLatitude());
        address.setLongitude(source.getLongitude());
        address.setMobileNumber(source.getMobileNumber());
        address.setAltMobileNumber(source.getAltMobileNumber());
        return address;
    }

    private BigDecimal computeDiscount(BigDecimal actual, BigDecimal offered) {
        if (actual == null || offered == null || actual.compareTo(BigDecimal.ZERO) <= 0) return null;
        return actual.subtract(offered).multiply(BigDecimal.valueOf(100))
                .divide(actual, 2, RoundingMode.HALF_UP);
    }

    private BigDecimal computeListingDiscount(Listing listing) {
        return computeDiscount(
                discountBase(listing.getActualPrice(), listing.getListingType(), listing.getAttributes()),
                listing.getOfferedPrice());
    }

    private BigDecimal discountBase(BigDecimal actualPrice, ListingType type, Map<String, Object> attributes) {
        if (actualPrice != null) return actualPrice;
        if (type == ListingType.RENT && attributes != null) {
            Object v = attributes.get("rentPerHour");
            if (v != null) { try { return new BigDecimal(v.toString()); } catch (NumberFormatException ignored) {} }
        }
        return null;
    }

    private String generateUniqueListingId() {
        String id;
        do { id = IdGeneratorUtil.generateId("LT"); } while (listingRepository.existsByListingId(id));
        return id;
    }

    private ListingResponse respond(Listing listing, String msgEn, String msgHi) {
        return ListingResponse.ok(listing.getListingId(), listing.getUserId(), LocalizedText.of(msgEn, msgHi));
    }

    // ─── Search vector builders ───────────────────────────────────────────────

    private String buildCategoryText(Integer categoryId, Integer moduleId) {
        StringBuilder sb = new StringBuilder();
        if (categoryId != null) {
            categoryRepository.findById(categoryId).ifPresent(c -> {
                if (c.getCategoryNameEn() != null) sb.append(c.getCategoryNameEn()).append(" ");
                if (c.getCategoryNameHi() != null) sb.append(c.getCategoryNameHi()).append(" ");
            });
        }
        if (moduleId != null) {
            moduleRepository.findById(moduleId).ifPresent(m -> {
                if (m.getModuleNameEn() != null) sb.append(m.getModuleNameEn()).append(" ");
                if (m.getModuleNameHi() != null) sb.append(m.getModuleNameHi()).append(" ");
            });
        }
        return sb.toString().trim();
    }

    private String buildLocationText(Address address) {
        if (address == null) return "";
        StringBuilder sb = new StringBuilder();
        if (address.getDistrict() != null) sb.append(address.getDistrict()).append(" ");
        if (address.getState()    != null) sb.append(address.getState()).append(" ");
        if (address.getVillage()  != null) sb.append(address.getVillage()).append(" ");
        if (address.getCity()     != null) sb.append(address.getCity()).append(" ");
        return sb.toString().trim();
    }

    private String buildAttributeText(Map<String, Object> attributes) {
        if (attributes == null || attributes.isEmpty()) return "";
        return attributes.values().stream()
                .filter(v -> v instanceof String)
                .map(Object::toString)
                .collect(Collectors.joining(" "));
    }

    private LocalDateTime postedAfter(String postedWithin) {
        if (postedWithin == null) return null;
        switch (postedWithin.toUpperCase()) {
            case "TODAY": return LocalDateTime.now().minusDays(1);
            case "WEEK":  return LocalDateTime.now().minusWeeks(1);
            case "MONTH": return LocalDateTime.now().minusMonths(1);
            default:      return null;
        }
    }
}
