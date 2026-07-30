package com.hyperlocal.tantra.modules.master.repository;

import com.hyperlocal.tantra.modules.master.entity.ModuleCategory;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;
import java.util.List;

@Repository
public interface ModuleCategoryRepository extends JpaRepository<ModuleCategory, Integer> {
    List<ModuleCategory> findByModuleIdOrderByDisplayOrderAsc(Integer moduleId);
    List<ModuleCategory> findByModuleIdAndIsActiveTrueOrderByDisplayOrderAsc(Integer moduleId); // For Mobile App Grid

    // Tree: top-level categories under a module (parent is null)
    List<ModuleCategory> findByModuleIdAndParentIdIsNullOrderByDisplayOrderAsc(Integer moduleId);
    List<ModuleCategory> findByModuleIdAndParentIdIsNullAndIsActiveTrueOrderByDisplayOrderAsc(Integer moduleId);

    // Tree: subcategories of a category
    List<ModuleCategory> findByParentIdOrderByDisplayOrderAsc(Integer parentId);
    List<ModuleCategory> findByParentIdAndIsActiveTrueOrderByDisplayOrderAsc(Integer parentId);
}