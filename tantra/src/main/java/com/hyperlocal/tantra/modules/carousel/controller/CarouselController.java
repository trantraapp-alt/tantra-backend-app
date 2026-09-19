package com.hyperlocal.tantra.modules.carousel.controller;

import com.hyperlocal.tantra.modules.carousel.entity.CarouselItem;
import com.hyperlocal.tantra.modules.carousel.service.CarouselItemService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

/**
 * Public endpoint: returns the active carousel items in display order.
 * No auth required — called on every home screen load.
 */
@RestController
@RequestMapping("/api/v1/carousel")
public class CarouselController {

    @Autowired private CarouselItemService service;

    @GetMapping
    public ResponseEntity<List<CarouselItem>> getCarousel() {
        return ResponseEntity.ok(service.getActiveItems());
    }
}
