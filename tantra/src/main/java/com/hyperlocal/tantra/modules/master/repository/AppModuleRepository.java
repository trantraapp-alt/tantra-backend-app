package com.hyperlocal.tantra.modules.master.repository;

import com.hyperlocal.tantra.modules.master.entity.AppModule;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;
import java.util.List;

@Repository
public interface AppModuleRepository extends JpaRepository<AppModule, Integer> {
    List<AppModule> findByIsActiveTrue(); // For Mobile App usage
}