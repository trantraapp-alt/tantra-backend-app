package com.hyperlocal.tantra.modules.deals.repository;

import com.hyperlocal.tantra.modules.deals.entity.DealGroup;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface DealGroupRepository extends JpaRepository<DealGroup, Integer> {
    List<DealGroup> findByIsActiveTrueOrderByDisplayOrderAsc();
    List<DealGroup> findAllByOrderByDisplayOrderAsc();
    Optional<DealGroup> findByGroupKey(String groupKey);
}
