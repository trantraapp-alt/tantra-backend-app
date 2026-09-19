package com.hyperlocal.tantra.modules.deals.service;

import com.hyperlocal.tantra.modules.deals.repository.DealGroupRepository;
import jakarta.persistence.EntityManager;
import jakarta.persistence.PersistenceContext;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.context.event.ApplicationReadyEvent;
import org.springframework.context.event.EventListener;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

/**
 * Seeds default deal groups on first startup.
 *
 * Uses ApplicationReadyEvent (not PostConstruct) so the Spring transaction
 * proxy is fully active and executeUpdate() works without errors.
 *
 * Category IDs resolved via category_key join -- no hardcoded integer IDs.
 * Adding a future module = insert deal_groups + deal_group_categories rows.
 */
@Component
public class DealGroupSeeder {

    @PersistenceContext
    private EntityManager em;

    @Autowired
    private DealGroupRepository dealGroupRepository;

    @EventListener(ApplicationReadyEvent.class)
    @Transactional
    public void seedIfEmpty() {
        if (dealGroupRepository.count() > 0) return;

        // Fasal (Crops)
        insertGroup("CROPS",           "Crops",              "फसल",
                    "🌾",    "/quintal",           "/क्विंटल", 1);

        // Yantra (Equipment)
        insertGroup("EQUIPMENT",       "Equipment",          "यंत्र",
                    "🚜",    "/day",               "/दिन", 2);

        // Pashu (Animal)
        insertGroup("ANIMAL",          "Animal",             "पशु",
                    "🐄",    "",                   "", 3);

        // Murgi + Machhli (Poultry + Fishery combined)
        insertGroup("POULTRY_FISHERY", "Poultry / Fishery",
                    "मुर्गी / मछली",
                    "🐓🐟",
                    "/kg",            "/किलो", 4);

        // Map categories -> groups by category_key (not by hardcoded IDs)
        em.createNativeQuery(
            "INSERT INTO deal_group_categories (deal_group_id, category_id) " +
            "SELECT dg.id, mc.id FROM deal_groups dg, module_categories mc WHERE " +
            "  (dg.group_key = 'CROPS'           AND mc.category_key = 'crop')      OR " +
            "  (dg.group_key = 'EQUIPMENT'       AND mc.category_key = 'equipment') OR " +
            "  (dg.group_key = 'ANIMAL'          AND mc.category_key = 'animal')    OR " +
            "  (dg.group_key = 'POULTRY_FISHERY' AND mc.category_key IN ('poultry','fishery'))"
        ).executeUpdate();
    }

    private void insertGroup(String groupKey, String labelEn, String labelHi,
                             String icon, String unitEn, String unitHi, int order) {
        em.createNativeQuery(
            "INSERT INTO deal_groups " +
            "  (group_key, label_en, label_hi, icon, unit_en, unit_hi, display_order, is_active) " +
            "VALUES (:gk, :le, :lh, :ic, :ue, :uh, :ord, true)"
        )
        .setParameter("gk",  groupKey)
        .setParameter("le",  labelEn)
        .setParameter("lh",  labelHi)
        .setParameter("ic",  icon)
        .setParameter("ue",  unitEn)
        .setParameter("uh",  unitHi)
        .setParameter("ord", order)
        .executeUpdate();
    }
}
