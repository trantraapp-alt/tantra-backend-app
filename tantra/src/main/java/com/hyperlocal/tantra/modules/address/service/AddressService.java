package com.hyperlocal.tantra.modules.address.service;

import com.hyperlocal.tantra.constants.MessageConstants;
import com.hyperlocal.tantra.exception.LocalizedException;
import com.hyperlocal.tantra.modules.address.dto.AddressRequest;
import com.hyperlocal.tantra.modules.address.dto.AddressResponse;
import com.hyperlocal.tantra.modules.address.entity.UserAddress;
import com.hyperlocal.tantra.modules.address.repository.UserAddressRepository;
import com.hyperlocal.tantra.modules.audit.service.AuditService;
import com.hyperlocal.tantra.modules.auth.entity.User;
import com.hyperlocal.tantra.modules.auth.repository.UserRepository;
import com.hyperlocal.tantra.modules.forms.model.LocalizedText;
import com.hyperlocal.tantra.utils.IdGeneratorUtil;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.List;

/**
 * Address book management. Enforces max 10 per user and exactly one default, with owner checks and
 * bilingual responses. Also exposes owner-scoped lookups the listing flow uses to snapshot an
 * address.
 */
@Service
public class AddressService {

    private static final int MAX_ADDRESSES = 10;

    @Autowired private UserAddressRepository addressRepository;
    @Autowired private UserRepository userRepository;
    @Autowired private AuditService auditService;

    // ---------------- reads (controller) ----------------

    public List<UserAddress> list(String mobileNumber) {
        User user = currentUser(mobileNumber);
        return addressRepository.findByUserIdAndIsDeletedFalseOrderByIsDefaultDescCreatedAtDesc(user.getUserId());
    }

    /** The user's default address, or null if none saved yet. */
    public UserAddress getDefault(String mobileNumber) {
        User user = currentUser(mobileNumber);
        return findDefault(user.getUserId());
    }

    // ---------------- writes (controller) ----------------

    @Transactional
    public AddressResponse create(AddressRequest req, String mobileNumber) {
        User user = currentUser(mobileNumber);
        long count = addressRepository.countByUserIdAndIsDeletedFalse(user.getUserId());
        if (count >= MAX_ADDRESSES) {
            throw new LocalizedException(
                    MessageConstants.ADDRESS_LIMIT_REACHED_EN, MessageConstants.ADDRESS_LIMIT_REACHED_HI);
        }

        UserAddress address = new UserAddress();
        address.setAddressId(generateUniqueAddressId());
        address.setUserId(user.getUserId());
        apply(address, req);

        // First address is default automatically; or if the client asked for default.
        boolean makeDefault = Boolean.TRUE.equals(req.getIsDefault()) || count == 0;
        if (makeDefault) {
            addressRepository.clearDefault(user.getUserId());
            address.setIsDefault(true);
        }

        UserAddress saved = addressRepository.save(address);
        auditService.record("ADDRESS_CREATED", "ADDRESS", saved.getAddressId(), user.getUserId());
        return AddressResponse.ok(saved.getAddressId(),
                LocalizedText.of(MessageConstants.ADDRESS_CREATE_SUCCESS_EN, MessageConstants.ADDRESS_CREATE_SUCCESS_HI));
    }

    @Transactional
    public AddressResponse update(String addressId, AddressRequest req, String mobileNumber) {
        User user = currentUser(mobileNumber);
        UserAddress address = ownedAddress(addressId, user.getUserId());
        apply(address, req);
        if (Boolean.TRUE.equals(req.getIsDefault())) {
            addressRepository.clearDefault(user.getUserId());
            address.setIsDefault(true);
        }
        address.setUpdatedAt(LocalDateTime.now());
        UserAddress saved = addressRepository.save(address);
        auditService.record("ADDRESS_UPDATED", "ADDRESS", saved.getAddressId(), user.getUserId());
        return AddressResponse.ok(saved.getAddressId(),
                LocalizedText.of(MessageConstants.ADDRESS_UPDATE_SUCCESS_EN, MessageConstants.ADDRESS_UPDATE_SUCCESS_HI));
    }

    @Transactional
    public AddressResponse setDefault(String addressId, String mobileNumber) {
        User user = currentUser(mobileNumber);
        UserAddress address = ownedAddress(addressId, user.getUserId());
        addressRepository.clearDefault(user.getUserId());
        address.setIsDefault(true);
        address.setUpdatedAt(LocalDateTime.now());
        addressRepository.save(address);
        auditService.record("ADDRESS_SET_DEFAULT", "ADDRESS", address.getAddressId(), user.getUserId());
        return AddressResponse.ok(address.getAddressId(),
                LocalizedText.of(MessageConstants.ADDRESS_DEFAULT_SUCCESS_EN, MessageConstants.ADDRESS_DEFAULT_SUCCESS_HI));
    }

    @Transactional
    public AddressResponse delete(String addressId, String mobileNumber) {
        User user = currentUser(mobileNumber);
        UserAddress address = ownedAddress(addressId, user.getUserId());
        boolean wasDefault = Boolean.TRUE.equals(address.getIsDefault());
        address.setIsDeleted(true);
        address.setIsDefault(false);
        address.setUpdatedAt(LocalDateTime.now());
        addressRepository.save(address);

        // If we removed the default, promote the most recent remaining address.
        if (wasDefault) {
            addressRepository.findByUserIdAndIsDeletedFalseOrderByIsDefaultDescCreatedAtDesc(user.getUserId())
                    .stream().findFirst().ifPresent(next -> {
                        next.setIsDefault(true);
                        addressRepository.save(next);
                    });
        }
        auditService.record("ADDRESS_DELETED", "ADDRESS", address.getAddressId(), user.getUserId());
        return AddressResponse.ok(address.getAddressId(),
                LocalizedText.of(MessageConstants.ADDRESS_DELETE_SUCCESS_EN, MessageConstants.ADDRESS_DELETE_SUCCESS_HI));
    }

    // ---------------- owner-scoped lookups (used by the listing flow) ----------------

    /** Resolves an address owned by the given user, or throws (not found / not owner). */
    public UserAddress requireOwned(String addressId, String userId) {
        return ownedAddress(addressId, userId);
    }

    /** The user's default address by userId, or null. */
    public UserAddress findDefault(String userId) {
        return addressRepository.findByUserIdAndIsDefaultTrueAndIsDeletedFalse(userId).orElse(null);
    }

    // ---------------- helpers ----------------

    private User currentUser(String mobileNumber) {
        return userRepository.findByMobileNumber(mobileNumber)
                .orElseThrow(() -> new LocalizedException(
                        MessageConstants.LISTING_USER_NOT_FOUND_EN, MessageConstants.LISTING_USER_NOT_FOUND_HI));
    }

    private UserAddress ownedAddress(String addressId, String userId) {
        UserAddress address = addressRepository.findByAddressIdAndIsDeletedFalse(addressId)
                .orElseThrow(() -> new LocalizedException(
                        MessageConstants.ADDRESS_NOT_FOUND_EN, MessageConstants.ADDRESS_NOT_FOUND_HI));
        if (!userId.equals(address.getUserId())) {
            throw new LocalizedException(
                    MessageConstants.ADDRESS_NOT_OWNER_EN, MessageConstants.ADDRESS_NOT_OWNER_HI);
        }
        return address;
    }

    private void apply(UserAddress address, AddressRequest req) {
        address.setLabel(req.getLabel());
        address.setFullAddress(req.getFullAddress());
        address.setCountry(req.getCountry());
        address.setState(req.getState());
        address.setDistrict(req.getDistrict());
        address.setCity(req.getCity());
        address.setVillage(req.getVillage());
        address.setPinCode(req.getPinCode());
        address.setLatitude(req.getLatitude());
        address.setLongitude(req.getLongitude());
        address.setMobileNumber(req.getMobileNumber());
        address.setAltMobileNumber(req.getAltMobileNumber());
    }

    private String generateUniqueAddressId() {
        String id;
        do {
            id = IdGeneratorUtil.generateShortUniqueId();
        } while (addressRepository.existsByAddressId(id));
        return id;
    }
}
