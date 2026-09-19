package com.hyperlocal.tantra.modules.deals.service;

import com.hyperlocal.tantra.exception.LocalizedException;
import com.hyperlocal.tantra.modules.audit.service.AuditService;
import com.hyperlocal.tantra.modules.deals.dto.DealCardDTO;
import com.hyperlocal.tantra.modules.deals.dto.DealGroupRequest;
import com.hyperlocal.tantra.modules.deals.entity.DealGroup;
import com.hyperlocal.tantra.modules.deals.repository.DealGroupRepository;
import com.hyperlocal.tantra.modules.listing.dto.ListingCardDTO;
import com.hyperlocal.tantra.modules.listing.entity.Listing;
import com.hyperlocal.tantra.modules.listing.repository.ListingRepository;
import com.hyperlocal.tantra.modules.listing.service.ListingService;
import com.hyperlocal.tantra.modules.master.repository.ModuleCategoryRepository;
import com.hyperlocal.tantra.utils.GeoUtil;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.cache.annotation.CacheEvict;
import org.springframework.cache.annotation.Cacheable;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

@Service
public class DealService {

    private static final Logger log = LoggerFactory.getLogger(DealService.class);

    @Autowired private DealGroupRepository dealGroupRepository;
    @Autowired private ListingRepository listingRepository;
    @Autowired private ModuleCategoryRepository categoryRepository;
    @Autowired private ListingService listingService;
    @Autowired private AuditService auditService;

    // No cache — live today's data, changes as sellers post listings
    public List<DealCardDTO> getDealCards() {
        List<DealGroup> groups = dealGroupRepository.findByIsActiveTrueOrderByDisplayOrderAsc();
        List<DealCardDTO> cards = new ArrayList<>();
        for (DealGroup g : groups) {
            List<Integer> catIds = resolveCategoryIds(g.getCategoryKeysList());
            if (catIds.isEmpty()) continue;
            DealCardDTO dto = new DealCardDTO();
            dto.setId(g.getId());
            dto.setGroupKey(g.getGroupKey());
            dto.setIcon(g.getIcon());
            dto.setLabelEn(g.getLabelEn());
            dto.setLabelHi(g.getLabelHi());
            dto.setBadgeEn(g.getBadgeEn());
            dto.setBadgeHi(g.getBadgeHi());
            dto.setAccentColor(g.getAccentColor());
            dto.setBackendUi(g.getBackendUi());
            dto.setBackgroundImageUrl(g.getBackgroundImageUrl());
            dto.setUnitEn(g.getUnitEn());
            dto.setUnitHi(g.getUnitHi());
            dto.setCtaType(g.getCtaType());
            dto.setCtaValue(g.getCtaValue());
            dto.setDisplayOrder(g.getDisplayOrder());
            int days = g.getFreshDealDays() != null ? g.getFreshDealDays() : 15;
            String cutoff = LocalDateTime.now().minusDays(days).toString();
            long count = listingRepository.countDealGroupListings(catIds, g.getListingType(), cutoff);
            if (count == 0) continue;          // hide cards with no fresh listings
            dto.setListingCount(count);
            dto.setMinPrice(listingRepository.minDealGroupPrice(catIds, g.getListingType(), cutoff));
            cards.add(dto);
        }
        return cards;
    }

    public Page<ListingCardDTO> getDealListings(String groupKey, Double lat, Double lng,
                                                Integer radiusKm, Pageable pageable) {
        DealGroup group = dealGroupRepository.findByGroupKey(groupKey)
                .orElseThrow(() -> new LocalizedException(
                        "Deal group not found.", "Deal group nahi mila."));
        List<Integer> catIds = resolveCategoryIds(group.getCategoryKeysList());
        if (catIds.isEmpty()) return Page.empty(pageable);

        Double latMin = null, latMax = null, lngMin = null, lngMax = null;
        if (lat != null && lng != null && radiusKm != null) {
            latMin = lat - GeoUtil.latOffset(radiusKm);
            latMax = lat + GeoUtil.latOffset(radiusKm);
            lngMin = lng - GeoUtil.lngOffset(radiusKm, lat);
            lngMax = lng + GeoUtil.lngOffset(radiusKm, lat);
        }

        int days = group.getFreshDealDays() != null ? group.getFreshDealDays() : 15;
        String cutoff = LocalDateTime.now().minusDays(days).toString();
        Page<Listing> raw = listingRepository.findDealGroupListings(
                catIds, group.getListingType(), cutoff, latMin, latMax, lngMin, lngMax, pageable);

        List<ListingCardDTO> result = raw.getContent().stream()
                .map(l -> listingService.toCard(l, lat, lng))
                .filter(c -> {
                    if (radiusKm != null && c.getDistanceKm() != null)
                        return c.getDistanceKm() <= radiusKm;
                    return true;
                })
                .collect(Collectors.toList());

        return new PageImpl<>(result, pageable, raw.getTotalElements());
    }

    public List<DealGroup> getAllGroups() {
        return dealGroupRepository.findAllByOrderByDisplayOrderAsc();
    }

    public DealGroup getById(Integer id) {
        return dealGroupRepository.findById(id)
                .orElseThrow(() -> new LocalizedException(
                        "Deal group not found.", "Deal group nahi mila."));
    }

    @Transactional
    @CacheEvict(value = {"dealCards", "homeFeed"}, allEntries = true)
    public DealGroup create(DealGroupRequest req, String adminId) {
        DealGroup g = new DealGroup();
        g.setUpdatedBy(adminId);
        applyRequest(g, req);
        DealGroup saved = dealGroupRepository.save(g);
        auditService.record("DEAL_GROUP_CREATED", "DEAL_GROUP",
                saved.getGroupKey(), adminId, Map.of("label", saved.getLabelEn()));
        return saved;
    }

    @Transactional
    @CacheEvict(value = {"dealCards", "homeFeed"}, allEntries = true)
    public DealGroup update(Integer id, DealGroupRequest req, String adminId) {
        DealGroup g = getById(id);
        g.setUpdatedBy(adminId);
        g.setUpdatedAt(LocalDateTime.now());
        applyRequest(g, req);
        return dealGroupRepository.save(g);
    }

    @Transactional
    @CacheEvict(value = {"dealCards", "homeFeed"}, allEntries = true)
    public DealGroup toggle(Integer id, String adminId) {
        DealGroup g = getById(id);
        g.setIsActive(!Boolean.TRUE.equals(g.getIsActive()));
        g.setUpdatedBy(adminId);
        g.setUpdatedAt(LocalDateTime.now());
        return dealGroupRepository.save(g);
    }

    @Transactional
    @CacheEvict(value = {"dealCards", "homeFeed"}, allEntries = true)
    public void delete(Integer id, String adminId) {
        DealGroup g = getById(id);
        auditService.record("DEAL_GROUP_DELETED", "DEAL_GROUP", g.getGroupKey(), adminId, Map.of());
        dealGroupRepository.delete(g);
    }

    private List<Integer> resolveCategoryIds(List<String> keys) {
        if (keys == null || keys.isEmpty()) return List.of();
        List<Integer> ids = new ArrayList<>();
        for (String key : keys) {
            categoryRepository.findByCategoryKey(key).ifPresent(c -> ids.add(c.getId()));
        }
        return ids;
    }

    private void applyRequest(DealGroup g, DealGroupRequest req) {
        if (req.getGroupKey() != null)     g.setGroupKey(req.getGroupKey().toUpperCase());
        if (req.getIcon() != null)         g.setIcon(req.getIcon());
        if (req.getLabelEn() != null)      g.setLabelEn(req.getLabelEn());
        if (req.getLabelHi() != null)      g.setLabelHi(req.getLabelHi());
        if (req.getBadgeEn() != null)      g.setBadgeEn(req.getBadgeEn());
        if (req.getBadgeHi() != null)      g.setBadgeHi(req.getBadgeHi());
        if (req.getAccentColor() != null)       g.setAccentColor(req.getAccentColor());
        if (req.getBackendUi() != null)         g.setBackendUi(req.getBackendUi());
        if (req.getBackgroundImageUrl() != null) g.setBackgroundImageUrl(req.getBackgroundImageUrl());
        if (req.getCategoryKeys() != null) g.setCategoryKeys(String.join(",", req.getCategoryKeys()));
        if (req.getListingType() != null)  g.setListingType(req.getListingType().toUpperCase());
        if (req.getUnitEn() != null)       g.setUnitEn(req.getUnitEn());
        if (req.getUnitHi() != null)       g.setUnitHi(req.getUnitHi());
        if (req.getCtaType() != null)      g.setCtaType(req.getCtaType().toUpperCase());
        if (req.getCtaValue() != null)     g.setCtaValue(req.getCtaValue());
        if (req.getDisplayOrder() != null)  g.setDisplayOrder(req.getDisplayOrder());
        if (req.getIsActive() != null)      g.setIsActive(req.getIsActive());
        if (req.getFreshDealDays() != null) g.setFreshDealDays(req.getFreshDealDays());
        if ("DEAL_GROUP".equals(g.getCtaType()) && g.getCtaValue() == null && g.getGroupKey() != null)
            g.setCtaValue(g.getGroupKey());
    }
}
