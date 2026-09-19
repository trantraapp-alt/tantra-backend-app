package com.hyperlocal.tantra.modules.carousel.service;

import com.hyperlocal.tantra.exception.LocalizedException;
import com.hyperlocal.tantra.modules.audit.service.AuditService;
import com.hyperlocal.tantra.modules.carousel.dto.CarouselItemRequest;
import com.hyperlocal.tantra.modules.carousel.entity.CarouselItem;
import com.hyperlocal.tantra.modules.carousel.repository.CarouselItemRepository;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.cache.annotation.CacheEvict;
import org.springframework.cache.annotation.Cacheable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Map;

/**
 * Manages the DB-driven carousel shown on the home screen.
 * Active items are cached; any write evicts both the carousel and home-feed caches.
 */
@Service
public class CarouselItemService {

    private static final Logger log = LoggerFactory.getLogger(CarouselItemService.class);

    @Autowired private CarouselItemRepository repo;
    @Autowired private AuditService auditService;

    // ─── PUBLIC READ ──────────────────────────────────────────────────────────

    @Cacheable("carouselItems")
    public List<CarouselItem> getActiveItems() {
        return repo.findByIsActiveTrueOrderByDisplayOrderAsc();
    }

    // ─── ADMIN READ ───────────────────────────────────────────────────────────

    public List<CarouselItem> getAllItems() {
        return repo.findAllByOrderByDisplayOrderAsc();
    }

    public CarouselItem getById(Integer id) {
        return repo.findById(id)
                .orElseThrow(() -> new LocalizedException(
                        "Carousel item not found.", "कैरोसेल आइटम नहीं मिला।"));
    }

    // ─── ADMIN WRITE ─────────────────────────────────────────────────────────

    @Transactional
    @CacheEvict(value = {"carouselItems", "homeFeed"}, allEntries = true)
    public CarouselItem create(CarouselItemRequest req, String adminId) {
        CarouselItem item = new CarouselItem();
        item.setUpdatedBy(adminId);
        applyRequest(item, req);
        CarouselItem saved = repo.save(item);
        auditService.record("CAROUSEL_CREATED", "CAROUSEL_ITEM",
                saved.getId().toString(), adminId,
                Map.of("label", saved.getLabel().toString()));
        log.info("[CAROUSEL] Created item id={} by admin {}", saved.getId(), adminId);
        return saved;
    }

    @Transactional
    @CacheEvict(value = {"carouselItems", "homeFeed"}, allEntries = true)
    public CarouselItem update(Integer id, CarouselItemRequest req, String adminId) {
        CarouselItem item = getById(id);
        item.setUpdatedBy(adminId);
        item.setUpdatedAt(LocalDateTime.now());
        applyRequest(item, req);
        CarouselItem saved = repo.save(item);
        auditService.record("CAROUSEL_UPDATED", "CAROUSEL_ITEM",
                saved.getId().toString(), adminId, Map.of());
        log.info("[CAROUSEL] Updated item id={} by admin {}", saved.getId(), adminId);
        return saved;
    }

    @Transactional
    @CacheEvict(value = {"carouselItems", "homeFeed"}, allEntries = true)
    public CarouselItem toggle(Integer id, String adminId) {
        CarouselItem item = getById(id);
        item.setIsActive(!Boolean.TRUE.equals(item.getIsActive()));
        item.setUpdatedBy(adminId);
        item.setUpdatedAt(LocalDateTime.now());
        CarouselItem saved = repo.save(item);
        auditService.record("CAROUSEL_TOGGLED", "CAROUSEL_ITEM",
                saved.getId().toString(), adminId,
                Map.of("isActive", String.valueOf(saved.getIsActive())));
        log.info("[CAROUSEL] Item id={} toggled to isActive={}", saved.getId(), saved.getIsActive());
        return saved;
    }

    @Transactional
    @CacheEvict(value = {"carouselItems", "homeFeed"}, allEntries = true)
    public void delete(Integer id, String adminId) {
        CarouselItem item = getById(id);
        auditService.record("CAROUSEL_DELETED", "CAROUSEL_ITEM",
                item.getId().toString(), adminId, Map.of());
        repo.delete(item);
        log.info("[CAROUSEL] Deleted item id={} by admin {}", item.getId(), adminId);
    }

    // ─── HELPERS ─────────────────────────────────────────────────────────────

    private void applyRequest(CarouselItem item, CarouselItemRequest req) {
        if (req.getLabel() != null)        item.setLabel(req.getLabel());
        if (req.getIconUrl() != null)      item.setIconUrl(req.getIconUrl());
        if (req.getCtaType() != null)      item.setCtaType(req.getCtaType().toUpperCase());
        if (req.getCtaValue() != null)     item.setCtaValue(req.getCtaValue());
        if (req.getListingType() != null)  item.setListingType(req.getListingType().toUpperCase());
        if (req.getBgColor() != null)      item.setBgColor(req.getBgColor());
        if (req.getTextColor() != null)    item.setTextColor(req.getTextColor());
        if (req.getDisplayOrder() != null) item.setDisplayOrder(req.getDisplayOrder());
        if (req.getIsActive() != null)     item.setIsActive(req.getIsActive());
    }
}
