package com.hyperlocal.tantra.modules.carousel.repository;

import com.hyperlocal.tantra.modules.carousel.entity.CarouselItem;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface CarouselItemRepository extends JpaRepository<CarouselItem, Integer> {

    List<CarouselItem> findByIsActiveTrueOrderByDisplayOrderAsc();

    List<CarouselItem> findAllByOrderByDisplayOrderAsc();
}
