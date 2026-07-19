package com.hyperlocal.tantra.modules.master.repository;

import com.hyperlocal.tantra.modules.master.entity.ModuleCategory;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;
import java.util.List;

@Repository
public interface ModuleCategoryRepository extends JpaRepository<ModuleCategory, Integer> {
    List<ModuleCategory> findByModuleIdOrderByDisplayOrderAsc(Integer moduleId);
    List<ModuleCategory> findByModuleIdAndIsActiveTrueOrderByDisplayOrderAsc(Integer moduleId); // For Mobile App Grid
}