package com.hyperlocal.tantra.modules.carousel;

import com.hyperlocal.tantra.modules.carousel.entity.CarouselItem;
import com.hyperlocal.tantra.modules.carousel.repository.CarouselItemRepository;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.ApplicationArguments;
import org.springframework.boot.ApplicationRunner;
import org.springframework.stereotype.Component;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;

/**
 * Seeds the carousel_items table with default category buttons on first startup.
 * Only runs when the table is empty, so re-deploys never overwrite admin edits.
 */
@Component
public class CarouselSeeder implements ApplicationRunner {

    private static final Logger log = LoggerFactory.getLogger(CarouselSeeder.class);

    @Autowired private CarouselItemRepository repo;

    @Override
    public void run(ApplicationArguments args) {
        if (repo.count() > 0) return;

        log.info("[CAROUSEL] Seeding default carousel items...");

        List<CarouselItem> items = new ArrayList<>();
        items.add(item(Map.of("en", "Browse Crops",       "hi", "फसल देखें"),        "BROWSE_CATEGORY", "crop",        null,   "#E8F5E9", "#1B5E20", 1));
        items.add(item(Map.of("en", "Browse Seeds",       "hi", "बीज देखें"),         "BROWSE_CATEGORY", "seed",        null,   "#FFF8E1", "#F57F17", 2));
        items.add(item(Map.of("en", "Browse Fertilizers", "hi", "खाद देखें"),         "BROWSE_CATEGORY", "fertilizer",  null,   "#E3F2FD", "#0D47A1", 3));
        items.add(item(Map.of("en", "Rent Equipment",     "hi", "उपकरण किराए पर"),    "BROWSE_CATEGORY", "equipment",   "RENT", "#F3E5F5", "#4A148C", 4));
        items.add(item(Map.of("en", "Buy Equipment",      "hi", "उपकरण खरीदें"),      "BROWSE_CATEGORY", "equipment",   "SELL", "#FBE9E7", "#BF360C", 5));
        items.add(item(Map.of("en", "Browse Animals",     "hi", "पशु देखें"),          "BROWSE_CATEGORY", "animal",      null,   "#FCE4EC", "#880E4F", 6));
        items.add(item(Map.of("en", "Agri Repair",        "hi", "कृषि मरम्मत"),       "BROWSE_CATEGORY", "repair",      null,   "#E8EAF6", "#1A237E", 7));

        repo.saveAll(items);
        log.info("[CAROUSEL] Seeded {} carousel items.", items.size());
    }

    private CarouselItem item(Map<String, String> label, String ctaType, String ctaValue,
                              String listingType, String bgColor, String textColor, int order) {
        CarouselItem i = new CarouselItem();
        i.setLabel(label);
        i.setCtaType(ctaType);
        i.setCtaValue(ctaValue);
        i.setListingType(listingType);
        i.setBgColor(bgColor);
        i.setTextColor(textColor);
        i.setDisplayOrder(order);
        i.setIsActive(true);
        i.setUpdatedBy("SYSTEM");
        return i;
    }
}
