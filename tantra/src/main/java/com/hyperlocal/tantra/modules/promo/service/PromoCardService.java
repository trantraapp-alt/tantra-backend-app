package com.hyperlocal.tantra.modules.promo.service;

import com.hyperlocal.tantra.exception.LocalizedException;
import com.hyperlocal.tantra.modules.audit.service.AuditService;
import com.hyperlocal.tantra.modules.promo.dto.PromoCardRequest;
import com.hyperlocal.tantra.modules.promo.entity.PromoCard;
import com.hyperlocal.tantra.modules.promo.repository.PromoCardRepository;
import com.hyperlocal.tantra.utils.IdGeneratorUtil;
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
 * Manages admin-created promotional cards shown on the home feed.
 * Active cards are cached; any write operation evicts the home feed cache
 * so the next request rebuilds it with the updated card list.
 */
@Service
public class PromoCardService {

    private static final Logger log = LoggerFactory.getLogger(PromoCardService.class);

    @Autowired private PromoCardRepository promoCardRepository;
    @Autowired private AuditService auditService;

    // ─── PUBLIC READ ──────────────────────────────────────────────────────────

    @Cacheable(value = "promoCards", key = "(#district != null ? #district : 'ALL') + '_' + (#cardType != null ? #cardType : 'ALL')")
    public List<PromoCard> getActiveCards(String district, String cardType) {
        return promoCardRepository.findActiveCards(district, LocalDateTime.now(), cardType);
    }

    /** Admin view: all cards regardless of active/validity state. */
    public List<PromoCard> getAllCards() {
        return promoCardRepository.findAllByOrderByDisplayOrderAsc();
    }

    public PromoCard getById(Long id) {
        return promoCardRepository.findById(id)
                .orElseThrow(() -> new LocalizedException(
                        "Promo card not found.", "प्रमो कार्ड नहीं मिला।"));
    }

    // ─── ADMIN WRITE ─────────────────────────────────────────────────────────

    @Transactional
    @CacheEvict(value = {"promoCards", "homeFeed"}, allEntries = true)
    public PromoCard create(PromoCardRequest req, String adminUserId) {
        log.info("[PROMO] Admin {} creating promo card", adminUserId);

        PromoCard card = new PromoCard();
        card.setCardId(generateUniqueCardId());
        card.setCreatedBy(adminUserId);
        applyRequest(card, req);

        PromoCard saved = promoCardRepository.save(card);
        auditService.record("PROMO_CARD_CREATED", "PROMO_CARD",
                saved.getCardId(), adminUserId,
                Map.of("title", saved.getTitle().toString()));
        log.info("[PROMO] Created promo card {} by admin {}", saved.getCardId(), adminUserId);
        return saved;
    }

    @Transactional
    @CacheEvict(value = {"promoCards", "homeFeed"}, allEntries = true)
    public PromoCard update(Long id, PromoCardRequest req, String adminUserId) {
        log.info("[PROMO] Admin {} updating promo card id={}", adminUserId, id);

        PromoCard card = getById(id);
        applyRequest(card, req);
        card.setUpdatedAt(LocalDateTime.now());

        PromoCard saved = promoCardRepository.save(card);
        auditService.record("PROMO_CARD_UPDATED", "PROMO_CARD",
                saved.getCardId(), adminUserId, Map.of());
        return saved;
    }

    @Transactional
    @CacheEvict(value = {"promoCards", "homeFeed"}, allEntries = true)
    public PromoCard toggle(Long id, String adminUserId) {
        PromoCard card = getById(id);
        card.setIsActive(!Boolean.TRUE.equals(card.getIsActive()));
        card.setUpdatedAt(LocalDateTime.now());
        PromoCard saved = promoCardRepository.save(card);

        auditService.record("PROMO_CARD_TOGGLED", "PROMO_CARD",
                saved.getCardId(), adminUserId,
                Map.of("isActive", String.valueOf(saved.getIsActive())));
        log.info("[PROMO] Card {} toggled to isActive={}", saved.getCardId(), saved.getIsActive());
        return saved;
    }

    @Transactional
    @CacheEvict(value = {"promoCards", "homeFeed"}, allEntries = true)
    public void delete(Long id, String adminUserId) {
        PromoCard card = getById(id);
        auditService.record("PROMO_CARD_DELETED", "PROMO_CARD",
                card.getCardId(), adminUserId, Map.of());
        promoCardRepository.delete(card);
        log.info("[PROMO] Deleted promo card {} by admin {}", card.getCardId(), adminUserId);
    }

    // ─── HELPERS ─────────────────────────────────────────────────────────────

    private void applyRequest(PromoCard card, PromoCardRequest req) {
        if (req.getTitle() != null)          card.setTitle(req.getTitle());
        if (req.getSubtitle() != null)       card.setSubtitle(req.getSubtitle());
        if (req.getImageUrl() != null)       card.setImageUrl(req.getImageUrl());
        if (req.getBgColor() != null)        card.setBgColor(req.getBgColor());
        if (req.getTextColor() != null)      card.setTextColor(req.getTextColor());
        if (req.getCtaLabel() != null)       card.setCtaLabel(req.getCtaLabel());
        if (req.getCtaType() != null)        card.setCtaType(req.getCtaType().toUpperCase());
        if (req.getCtaValue() != null)       card.setCtaValue(req.getCtaValue());
        if (req.getDisplayOrder() != null)   card.setDisplayOrder(req.getDisplayOrder());
        if (req.getTargetDistrict() != null) card.setTargetDistrict(req.getTargetDistrict());
        if (req.getIsActive() != null)       card.setIsActive(req.getIsActive());
        if (req.getValidFrom() != null)        card.setValidFrom(req.getValidFrom());
        if (req.getValidTo() != null)          card.setValidTo(req.getValidTo());
        if (req.getEyebrow() != null)          card.setEyebrow(req.getEyebrow());
        if (req.getCtaBgColor() != null)       card.setCtaBgColor(req.getCtaBgColor());
        if (req.getIllustrationKey() != null)  card.setIllustrationKey(req.getIllustrationKey());
    }

    private String generateUniqueCardId() {
        String id;
        do {
            id = IdGeneratorUtil.generateId("PC");
        } while (promoCardRepository.findByCardId(id).isPresent());
        return id;
    }
}
