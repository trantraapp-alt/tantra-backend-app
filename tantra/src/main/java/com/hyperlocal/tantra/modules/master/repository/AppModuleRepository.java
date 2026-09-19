package com.hyperlocal.tantra.modules.master.repository;

import com.hyperlocal.tantra.modules.master.entity.AppModule;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;
import java.util.List;
import java.util.Optional;

@Repository
public interface AppModuleRepository extends JpaRepository<AppModule, Integer> {
    List<AppModule> findByIsActiveTrue();
    Optional<AppModule> findByModuleKeyIgnoreCase(String moduleKey);
}