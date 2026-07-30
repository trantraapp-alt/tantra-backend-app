package com.hyperlocal.tantra.modules.listing.service;

import com.hyperlocal.tantra.constants.MessageConstants;
import com.hyperlocal.tantra.exception.LocalizedException;
import com.hyperlocal.tantra.modules.auth.entity.User;
import com.hyperlocal.tantra.modules.auth.repository.UserRepository;
import com.hyperlocal.tantra.modules.forms.entity.FormDefinition;
import com.hyperlocal.tantra.modules.forms.model.FormField;
import com.hyperlocal.tantra.modules.forms.model.ListingType;
import com.hyperlocal.tantra.modules.forms.model.LocalizedText;
import com.hyperlocal.tantra.modules.forms.repository.FormDefinitionRepository;
import com.hyperlocal.tantra.modules.address.entity.UserAddress;
import com.hyperlocal.tantra.modules.address.service.AddressService;
import com.hyperlocal.tantra.modules.audit.service.AuditService;
import com.hyperlocal.tantra.modules.listing.dto.ListingPatchRequest;
import com.hyperlocal.tantra.modules.listing.dto.ListingRequest;
import com.hyperlocal.tantra.modules.listing.dto.ListingResponse;
import com.hyperlocal.tantra.modules.listing.entity.Listing;
import com.hyperlocal.tantra.modules.listing.model.Address;
import com.hyperlocal.tantra.modules.listing.model.ListingStatus;
import com.hyperlocal.tantra.modules.listing.repository.ListingRepository;
import com.hyperlocal.tantra.modules.upload.service.StorageService;
import com.hyperlocal.tantra.utils.IdGeneratorUtil;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;

/**
 * Create / read / update / delete for listings. Update respects the form metadata flags:
 * {@code editableOnUpdate=false} fields are locked on a full edit, and only {@code inlineEditable}
 * fields can be changed via the quick grid PATCH. Every write returns a compact bilingual response
 * with listingId + userId. Owner is always resolved from the JWT, never the body.
 */
@Service
public class ListingService {

    @Autowired private ListingRepository listingRepository;
    @Autowired private UserRepository userRepository;
    @Autowired private FormDefinitionRepository formRepository;
    @Autowired private StorageService storageService;
    @Autowired private AddressService addressService;
    @Autowired private AuditService auditService;

    // ---------------- CREATE ----------------

    @Transactional
    public ListingResponse createListing(ListingRequest req, String mobileNumber) {
        User user = currentUser(mobileNumber);
        if (req.getCategoryId() == null) {
            throw new LocalizedException(
                    MessageConstants.LISTING_CATEGORY_REQUIRED_EN, MessageConstants.LISTING_CATEGORY_REQUIRED_HI);
        }

        Listing listing = new Listing();
        listing.setListingId(generateUniqueListingId());
        listing.setUserId(user.getUserId());
        listing.setUserMobileNumber(user.getMobileNumber());
        listing.setCreatedBy(user.getUserId());
        listing.setUpdatedBy(user.getUserId());
        listing.setCategoryId(req.getCategoryId());
        listing.setModuleId(req.getModuleId());
        // A listing is always concrete (RENT/SELL), never BOTH — derive it from the availableFor choice.
        listing.setListingType(resolveListingType(req.getListingType(), req.getAttributes()));

        listing.setActualPrice(req.getActualPrice());
        listing.setOfferedPrice(req.getOfferedPrice());
        // Rent listings have no actualPrice — discount is computed against rentPerHour (from attributes).
        listing.setDiscountPct(computeDiscount(
                discountBase(req.getActualPrice(), listing.getListingType(), req.getAttributes()), req.getOfferedPrice()));
        listing.setQuantity(req.getQuantity());
        listing.setUnit(req.getUnit());
        listing.setIsNegotiable(Boolean.TRUE.equals(req.getIsNegotiable()));
        listing.setContactNumber(req.getContactNumber());
        listing.setShowContact(Boolean.TRUE.equals(req.getShowContact()));
        if (req.getImages() != null) listing.setImages(req.getImages());
        if (req.getAttributes() != null) listing.setAttributes(req.getAttributes());
        applyAddress(listing, req, user);

        // Stamp the form version this listing was created against (best-effort) for deterministic edits.
        resolveFormDefinition(listing.getCategoryId(), listing.getListingType())
                .ifPresent(form -> {
                    listing.setFormId(form.getId());
                    listing.setFormVersion(form.getVersion());
                });

        Listing saved = listingRepository.save(listing);
        auditService.record("LISTING_CREATED", "LISTING", saved.getListingId(), user.getUserId());
        return respond(saved, MessageConstants.LISTING_CREATE_SUCCESS_EN, MessageConstants.LISTING_CREATE_SUCCESS_HI);
    }

    // ---------------- FULL UPDATE (PUT) ----------------

    @Transactional
    public ListingResponse updateListing(String listingId, ListingRequest req, String mobileNumber) {
        User user = currentUser(mobileNumber);
        Listing listing = ownedListing(listingId, user);
        Map<String, FormField> fields = formFields(listing);

        if (req.getActualPrice() != null && editable("actualPrice", fields)) listing.setActualPrice(req.getActualPrice());
        if (req.getOfferedPrice() != null && editable("offeredPrice", fields)) listing.setOfferedPrice(req.getOfferedPrice());
        if (req.getQuantity() != null && editable("quantity", fields)) listing.setQuantity(req.getQuantity());
        if (req.getUnit() != null && editable("unit", fields)) listing.setUnit(req.getUnit());
        if (req.getIsNegotiable() != null && editable("isNegotiable", fields)) listing.setIsNegotiable(req.getIsNegotiable());
        if (req.getContactNumber() != null && editable("contactNumber", fields)) listing.setContactNumber(req.getContactNumber());
        if (req.getShowContact() != null && editable("showContact", fields)) listing.setShowContact(req.getShowContact());
        if (editable("address", fields)) {
            applyAddress(listing, req, user);
        }

        // Images: replace, and delete files the user removed.
        if (req.getImages() != null && editable("images", fields)) {
            List<String> removed = new ArrayList<>(listing.getImages() == null ? List.of() : listing.getImages());
            removed.removeAll(req.getImages());
            if (!removed.isEmpty()) storageService.delete(removed);
            listing.setImages(req.getImages());
        }

        // Attributes: merge, skipping fields locked with editableOnUpdate=false (keep old value).
        if (req.getAttributes() != null) {
            Map<String, Object> merged = listing.getAttributes() == null
                    ? new HashMap<>() : new HashMap<>(listing.getAttributes());
            req.getAttributes().forEach((key, value) -> {
                FormField field = fields.get(key);
                if (field == null || !Boolean.FALSE.equals(field.getEditableOnUpdate())) {
                    merged.put(key, value);
                }
            });
            listing.setAttributes(merged);
        }
        // Recompute discount from the final state (rent uses rentPerHour from the merged attributes).
        listing.setDiscountPct(computeListingDiscount(listing));

        stampUpdate(listing, user);
        Listing saved = listingRepository.save(listing);
        auditService.record("LISTING_UPDATED", "LISTING", saved.getListingId(), user.getUserId());
        return respond(saved, MessageConstants.LISTING_UPDATE_SUCCESS_EN, MessageConstants.LISTING_UPDATE_SUCCESS_HI);
    }

    // ---------------- INLINE / QUICK UPDATE (PATCH) ----------------

    @Transactional
    public ListingResponse patchListing(String listingId, ListingPatchRequest req, String mobileNumber) {
        User user = currentUser(mobileNumber);
        Listing listing = ownedListing(listingId, user);
        Map<String, FormField> fields = formFields(listing);

        boolean priceChanged = false;
        if (req.getActualPrice() != null && inline("actualPrice", fields)) {
            listing.setActualPrice(req.getActualPrice());
            priceChanged = true;
        }
        if (req.getOfferedPrice() != null && inline("offeredPrice", fields)) {
            listing.setOfferedPrice(req.getOfferedPrice());
            priceChanged = true;
        }
        if (priceChanged) listing.setDiscountPct(computeListingDiscount(listing));
        if (req.getQuantity() != null && inline("quantity", fields)) listing.setQuantity(req.getQuantity());
        if (req.getUnit() != null && inline("unit", fields)) listing.setUnit(req.getUnit());
        if (req.getIsNegotiable() != null && inline("isNegotiable", fields)) listing.setIsNegotiable(req.getIsNegotiable());
        if (req.getStatus() != null) listing.setStatus(req.getStatus()); // owner may mark SOLD/INACTIVE

        if (req.getAttributes() != null) {
            Map<String, Object> merged = listing.getAttributes() == null
                    ? new HashMap<>() : new HashMap<>(listing.getAttributes());
            req.getAttributes().forEach((key, value) -> {
                FormField field = fields.get(key);
                if (field != null && Boolean.TRUE.equals(field.getInlineEditable())) {
                    merged.put(key, value);
                }
            });
            listing.setAttributes(merged);
        }

        stampUpdate(listing, user);
        Listing saved = listingRepository.save(listing);
        auditService.record("LISTING_UPDATED", "LISTING", saved.getListingId(), user.getUserId());
        return respond(saved, MessageConstants.LISTING_UPDATE_SUCCESS_EN, MessageConstants.LISTING_UPDATE_SUCCESS_HI);
    }

    // ---------------- DELETE (soft) ----------------

    @Transactional
    public ListingResponse deleteListing(String listingId, String mobileNumber) {
        User user = currentUser(mobileNumber);
        Listing listing = ownedListing(listingId, user);
        listing.setIsDeleted(true);
        listing.setStatus(ListingStatus.DELETED);
        stampUpdate(listing, user);
        Listing saved = listingRepository.save(listing);
        auditService.record("LISTING_DELETED", "LISTING", saved.getListingId(), user.getUserId());
        return respond(saved, MessageConstants.LISTING_DELETE_SUCCESS_EN, MessageConstants.LISTING_DELETE_SUCCESS_HI);
    }

    // ---------------- READS ----------------

    /** My-Listings page: the caller's own listings, paginated, optionally filtered by type/status. */
    public Page<Listing> getMyListings(String mobileNumber, ListingType listingType, ListingStatus status, Pageable pageable) {
        User user = currentUser(mobileNumber);
        return listingRepository.findMine(user.getUserId(), listingType, status, pageable);
    }

    /** Public category browse: active, non-deleted listings in a category, paginated. */
    public Page<Listing> getByCategory(Integer categoryId, ListingType listingType, Pageable pageable) {
        return listingRepository.findByCategory(categoryId, ListingStatus.ACTIVE, listingType, pageable);
    }

    public Listing getByListingId(String listingId) {
        return listingRepository.findByListingId(listingId)
                .orElseThrow(() -> new LocalizedException(
                        MessageConstants.LISTING_NOT_FOUND_EN, MessageConstants.LISTING_NOT_FOUND_HI));
    }

    // ---------------- helpers ----------------

    private User currentUser(String mobileNumber) {
        return userRepository.findByMobileNumber(mobileNumber)
                .orElseThrow(() -> new LocalizedException(
                        MessageConstants.LISTING_USER_NOT_FOUND_EN, MessageConstants.LISTING_USER_NOT_FOUND_HI));
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

    /** Loads the form the listing maps to (by stamped formId, else the active form) as fieldKey -> field. */
    private Map<String, FormField> formFields(Listing listing) {
        FormDefinition form = null;
        if (listing.getFormId() != null) {
            form = formRepository.findById(listing.getFormId()).orElse(null);
        }
        if (form == null) {
            form = resolveFormDefinition(listing.getCategoryId(), listing.getListingType()).orElse(null);
        }
        if (form == null || form.getFields() == null) return Collections.emptyMap();
        Map<String, FormField> map = new HashMap<>();
        for (FormField field : form.getFields()) map.put(field.getFieldKey(), field);
        return map;
    }

    /**
     * Finds the active form for a category. A RENT/SELL listing may map to a single BOTH form, so
     * fall back to BOTH when no form is defined for the concrete listing type.
     */
    private Optional<FormDefinition> resolveFormDefinition(Integer categoryId, ListingType listingType) {
        Optional<FormDefinition> form = formRepository
                .findFirstByCategoryIdAndListingTypeAndIsActiveTrueOrderByVersionDesc(categoryId, listingType);
        if (form.isEmpty() && listingType != ListingType.BOTH) {
            form = formRepository
                    .findFirstByCategoryIdAndListingTypeAndIsActiveTrueOrderByVersionDesc(categoryId, ListingType.BOTH);
        }
        return form;
    }

    /**
     * A listing is always concrete. For a BOTH form the real type comes from the availableFor choice;
     * otherwise the requested concrete type; never BOTH.
     */
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

    /**
     * Snapshots the address onto the listing AND records the source addressId reference:
     * a chosen saved address (addressId) or the user's default (useDefaultAddress) is copied from the
     * address book keeping its addressId; a raw inline address is stored as-is with addressId = null.
     * The snapshot keeps the listing independent of later address-book edits. No-op when the request
     * carries no address input, so an update that omits address leaves the existing one unchanged.
     */
    private void applyAddress(Listing listing, ListingRequest req, User user) {
        if (req.getAddressId() != null && !req.getAddressId().isBlank()) {
            UserAddress src = addressService.requireOwned(req.getAddressId(), user.getUserId());
            listing.setAddress(toAddress(src));
            listing.setAddressId(src.getAddressId());
            return;
        }
        if (Boolean.TRUE.equals(req.getUseDefaultAddress())) {
            UserAddress def = addressService.findDefault(user.getUserId());
            if (def != null) {
                listing.setAddress(toAddress(def));
                listing.setAddressId(def.getAddressId());
            }
            return;
        }
        if (req.getAddress() != null) {
            listing.setAddress(req.getAddress());
            listing.setAddressId(null);
        }
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
        return actual.subtract(offered)
                .multiply(BigDecimal.valueOf(100))
                .divide(actual, 2, RoundingMode.HALF_UP);
    }

    /** Discount for a listing against the right base: actualPrice for SELL, rentPerHour for RENT. */
    private BigDecimal computeListingDiscount(Listing listing) {
        return computeDiscount(
                discountBase(listing.getActualPrice(), listing.getListingType(), listing.getAttributes()),
                listing.getOfferedPrice());
    }

    /** The base price the discount is computed against: actualPrice when present, else the rent listing's
     *  rentPerHour (stored in attributes, since rent has no actualPrice). */
    private BigDecimal discountBase(BigDecimal actualPrice, ListingType type, Map<String, Object> attributes) {
        if (actualPrice != null) return actualPrice;
        if (type == ListingType.RENT && attributes != null) {
            Object rentPerHour = attributes.get("rentPerHour");
            if (rentPerHour != null) {
                try {
                    return new BigDecimal(rentPerHour.toString());
                } catch (NumberFormatException ignored) {
                    return null;
                }
            }
        }
        return null;
    }

    /** Public listing id: opaque, 10-char, type-prefixed — LT + 8 (e.g. "LT9F3KD2P1"). */
    private String generateUniqueListingId() {
        String id;
        do {
            id = IdGeneratorUtil.generateId("LT");
        } while (listingRepository.existsByListingId(id));
        return id;
    }

    private ListingResponse respond(Listing listing, String messageEn, String messageHi) {
        return ListingResponse.ok(listing.getListingId(), listing.getUserId(), LocalizedText.of(messageEn, messageHi));
    }
}
