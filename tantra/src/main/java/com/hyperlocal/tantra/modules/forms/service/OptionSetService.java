package com.hyperlocal.tantra.modules.forms.service;

import com.hyperlocal.tantra.modules.forms.entity.OptionItem;
import com.hyperlocal.tantra.modules.forms.entity.OptionSet;
import com.hyperlocal.tantra.modules.forms.repository.OptionItemRepository;
import com.hyperlocal.tantra.modules.forms.repository.OptionSetRepository;
import com.hyperlocal.tantra.config.CacheConfig;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.cache.annotation.CacheEvict;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

/**
 * The dropdown-maintenance service (admin only). Handles CRUD on reusable option sets and their
 * individual values, including cascading (parent) links.
 */
@Service
public class OptionSetService {

    @Autowired private OptionSetRepository setRepository;
    @Autowired private OptionItemRepository itemRepository;

    // ---------- Option sets ----------

    public List<OptionSet> getAllSets(boolean onlyActive) {
        return onlyActive ? setRepository.findByIsActiveTrue() : setRepository.findAll();
    }

    public OptionSet getSetById(Integer id) {
        return setRepository.findById(id)
                .orElseThrow(() -> new IllegalArgumentException("Option set not found with ID: " + id));
    }

    @Transactional
    @CacheEvict(cacheNames = {CacheConfig.FORMS, CacheConfig.OPTION_ITEMS}, allEntries = true)
    public OptionSet saveOrUpdateSet(OptionSet set) {
        if (set.getId() != null) {
            OptionSet existing = getSetById(set.getId());
            existing.setName(set.getName());
            if (set.getIsActive() != null) existing.setIsActive(set.getIsActive());
            return setRepository.save(existing);
        }
        if (set.getSetKey() == null || set.getSetKey().isBlank()) {
            throw new IllegalArgumentException("setKey is required");
        }
        if (setRepository.existsBySetKey(set.getSetKey())) {
            throw new IllegalArgumentException("Option set key already exists: " + set.getSetKey());
        }
        return setRepository.save(set);
    }

    // ---------- Option items (the actual dropdown values) ----------

    public List<OptionItem> getItems(Integer setId, boolean onlyActive) {
        getSetById(setId); // validate parent exists
        return onlyActive
                ? itemRepository.findByOptionSetIdAndIsActiveTrueOrderByDisplayOrderAsc(setId)
                : itemRepository.findByOptionSetIdOrderByDisplayOrderAsc(setId);
    }

    @Transactional
    @CacheEvict(cacheNames = {CacheConfig.FORMS, CacheConfig.OPTION_ITEMS}, allEntries = true)
    public OptionItem saveOrUpdateItem(Integer setId, OptionItem item) {
        getSetById(setId);
        item.setOptionSetId(setId);

        if (item.getId() != null) {
            OptionItem existing = itemRepository.findById(item.getId())
                    .orElseThrow(() -> new IllegalArgumentException("Option item not found with ID: " + item.getId()));
            existing.setItemKey(item.getItemKey());
            existing.setLabel(item.getLabel());
            existing.setParentItemId(item.getParentItemId());
            existing.setDisplayOrder(item.getDisplayOrder());
            if (item.getIsActive() != null) existing.setIsActive(item.getIsActive());
            return itemRepository.save(existing);
        }
        if (item.getIsActive() == null) item.setIsActive(true);
        return itemRepository.save(item);
    }

    @Transactional
    @CacheEvict(cacheNames = {CacheConfig.FORMS, CacheConfig.OPTION_ITEMS}, allEntries = true)
    public List<OptionItem> bulkAddItems(Integer setId, List<OptionItem> items) {
        getSetById(setId);
        if (items == null || items.isEmpty()) {
            throw new IllegalArgumentException("Item list cannot be null or empty");
        }
        items.forEach(item -> {
            item.setOptionSetId(setId);
            if (item.getIsActive() == null) item.setIsActive(true);
        });
        return itemRepository.saveAll(items);
    }

    @Transactional
    @CacheEvict(cacheNames = {CacheConfig.FORMS, CacheConfig.OPTION_ITEMS}, allEntries = true)
    public void deleteItem(Integer itemId) {
        if (!itemRepository.existsById(itemId)) {
            throw new IllegalArgumentException("Option item not found with ID: " + itemId);
        }
        itemRepository.deleteById(itemId);
    }
}
