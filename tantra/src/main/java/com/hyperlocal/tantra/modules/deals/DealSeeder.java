package com.hyperlocal.tantra.modules.deals;

import com.hyperlocal.tantra.modules.deals.entity.DealGroup;
import com.hyperlocal.tantra.modules.deals.repository.DealGroupRepository;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.ApplicationArguments;
import org.springframework.boot.ApplicationRunner;
import org.springframework.stereotype.Component;

import java.util.ArrayList;
import java.util.List;

/**
 * Seeds 4 default deal groups on first boot. Never runs if rows already exist.
 */
@Component
public class DealSeeder implements ApplicationRunner {

    private static final Logger log = LoggerFactory.getLogger(DealSeeder.class);

    @Autowired private DealGroupRepository repo;

    @Override
    public void run(ApplicationArguments args) {
        if (repo.count() > 0) return;
        log.info("[DEAL] Seeding default deal groups...");

        List<DealGroup> groups = new ArrayList<>();
        groups.add(group("CROPS",          "Crops",             "Fasal",         "Fresh harvest",     "Taza fasal",   "#22C55E", "crop",            null,   "/quintal", "/quintal", 1));
        groups.add(group("EQUIPMENT_RENT", "Equipment",         "Yantra",        "Available on rent", "Kirae par",    "#3B82F6", "equipment",        "RENT", "/day",     "/din",     2));
        groups.add(group("ANIMALS",        "Animals",           "Pashu",         "Healthy livestock", "Swasth pashu", "#EF4444", "animal",           null,   null,       null,       3));
        groups.add(group("POULTRY_FISHERY","Poultry & Fishery", "Murgi/Machali", "Fresh stock",       "Taza stock",   "#06B6D4", "poultry,fishery",  null,   "/kg",      "/kg",      4));
        repo.saveAll(groups);
        log.info("[DEAL] Seeded 4 deal groups.");
    }

    private DealGroup group(String key, String labelEn, String labelHi,
                            String badgeEn, String badgeHi, String color,
                            String catKeys, String listingType,
                            String unitEn, String unitHi, int order) {
        DealGroup g = new DealGroup();
        g.setGroupKey(key);
        g.setIcon(iconFor(key));
        g.setLabelEn(labelEn);
        g.setLabelHi(labelHi);
        g.setBadgeEn(badgeEn);
        g.setBadgeHi(badgeHi);
        g.setAccentColor(color);
        g.setCategoryKeys(catKeys);
        g.setListingType(listingType);
        g.setUnitEn(unitEn);
        g.setUnitHi(unitHi);
        g.setCtaType("DEAL_GROUP");
        g.setCtaValue(key);
        g.setDisplayOrder(order);
        g.setIsActive(true);
        g.setUpdatedBy("SYSTEM");
        return g;
    }

    private String iconFor(String key) {
        switch (key) {
            case "CROPS":          return "🌾";
            case "EQUIPMENT_RENT": return "🚜";
            case "ANIMALS":        return "🐄";
            case "POULTRY_FISHERY":return "🐓";
            default:               return "";
        }
    }
}
