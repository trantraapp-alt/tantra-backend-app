package com.hyperlocal.tantra.modules.stats.repository;

import com.hyperlocal.tantra.modules.stats.entity.StatTile;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface StatTileRepository extends JpaRepository<StatTile, Long> {

    @Query(value = "SELECT * FROM stat_tiles WHERE is_active = true ORDER BY display_order ASC", nativeQuery = true)
    List<StatTile> findActiveTiles();

    @Query(value = "SELECT COUNT(*) FROM stat_tiles", nativeQuery = true)
    long countAll();
}
