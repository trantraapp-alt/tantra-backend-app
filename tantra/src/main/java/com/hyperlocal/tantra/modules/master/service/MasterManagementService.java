package com.hyperlocal.tantra.modules.master.service;

import com.hyperlocal.tantra.modules.master.entity.AppModule;
import com.hyperlocal.tantra.modules.master.entity.ModuleCategory;
import com.hyperlocal.tantra.modules.master.repository.AppModuleRepository;
import com.hyperlocal.tantra.modules.master.repository.ModuleCategoryRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

@Service
public class MasterManagementService {

    @Autowired private AppModuleRepository moduleRepository;
    @Autowired private ModuleCategoryRepository categoryRepository;

    // ==========================================
    // MODULE CRUD METHODS (Admin & App)
    // ==========================================

    public List<AppModule> getAllModules(boolean onlyActive) {
        return onlyActive ? moduleRepository.findByIsActiveTrue() : moduleRepository.findAll();
    }

    public AppModule getModuleById(Integer id) {
        return moduleRepository.findById(id)
                .orElseThrow(() -> new IllegalArgumentException("App Module not found with ID: " + id));
    }

    @Transactional
    public AppModule saveOrUpdateModule(AppModule appModule) {
        // Safe check for updates to prevent over-writing key fields if needed
        if (appModule.getId() != null) {
            AppModule existing = getModuleById(appModule.getId());
            existing.setModuleNameEn(appModule.getModuleNameEn());
            existing.setModuleNameHi(appModule.getModuleNameHi());
            existing.setIsActive(appModule.getIsActive());
            return moduleRepository.save(existing);
        }
        return moduleRepository.save(appModule);
    }

    // ==========================================
    // CATEGORY CRUD METHODS (Admin & App)
    // ==========================================

    public List<ModuleCategory> getCategoriesByModule(Integer moduleId, boolean onlyActive) {
        // Verifies module rule exists first
        if (!moduleRepository.existsById(moduleId)) {
            throw new IllegalArgumentException("Parent Module ID does not exist");
        }
        return onlyActive
                ? categoryRepository.findByModuleIdAndIsActiveTrueOrderByDisplayOrderAsc(moduleId)
                : categoryRepository.findByModuleIdOrderByDisplayOrderAsc(moduleId);
    }

    /**
     * Tree read. {@code parentId == null} → top-level categories under the module;
     * else the subcategories of that parent category.
     */
    public List<ModuleCategory> getCategories(Integer moduleId, Integer parentId, boolean onlyActive) {
        if (!moduleRepository.existsById(moduleId)) {
            throw new IllegalArgumentException("Parent Module ID does not exist");
        }
        if (parentId == null) {
            return onlyActive
                    ? categoryRepository.findByModuleIdAndParentIdIsNullAndIsActiveTrueOrderByDisplayOrderAsc(moduleId)
                    : categoryRepository.findByModuleIdAndParentIdIsNullOrderByDisplayOrderAsc(moduleId);
        }
        return getSubcategories(parentId, onlyActive);
    }

    public List<ModuleCategory> getSubcategories(Integer parentId, boolean onlyActive) {
        if (!categoryRepository.existsById(parentId)) {
            throw new IllegalArgumentException("Parent category does not exist: " + parentId);
        }
        return onlyActive
                ? categoryRepository.findByParentIdAndIsActiveTrueOrderByDisplayOrderAsc(parentId)
                : categoryRepository.findByParentIdOrderByDisplayOrderAsc(parentId);
    }

    public ModuleCategory getCategoryById(Integer id) {
        return categoryRepository.findById(id)
                .orElseThrow(() -> new IllegalArgumentException("Category not found with ID: " + id));
    }

    /** All subcategories (parentId IS NOT NULL) across all modules — flat list for dropdowns/search filters. */
    public List<ModuleCategory> getAllSubcategories(boolean onlyActive) {
        return onlyActive
                ? categoryRepository.findByParentIdIsNotNullAndIsActiveTrueOrderByModuleIdAscDisplayOrderAsc()
                : categoryRepository.findByParentIdIsNotNullOrderByModuleIdAscDisplayOrderAsc();
    }

    /** Every category (parent + sub) across all modules — flat list. Optional moduleId filter. */
    public List<ModuleCategory> getAllCategories(Integer moduleId, boolean onlyActive) {
        if (moduleId != null) {
            return onlyActive
                    ? categoryRepository.findByModuleIdAndIsActiveTrueOrderByDisplayOrderAsc(moduleId)
                    : categoryRepository.findByModuleIdOrderByDisplayOrderAsc(moduleId);
        }
        return onlyActive
                ? categoryRepository.findByIsActiveTrueOrderByModuleIdAscDisplayOrderAsc()
                : categoryRepository.findAllByOrderByModuleIdAscDisplayOrderAsc();
    }

    @Transactional
    public ModuleCategory saveOrUpdateCategory(ModuleCategory category) {
        if (!moduleRepository.existsById(category.getModuleId())) {
            throw new IllegalArgumentException("Cannot map category. Parent Module ID does not exist.");
        }
        // If it's a subcategory, the parent must exist and belong to the same module.
        if (category.getParentId() != null) {
            ModuleCategory parent = categoryRepository.findById(category.getParentId())
                    .orElseThrow(() -> new IllegalArgumentException("Parent category not found: " + category.getParentId()));
            if (!parent.getModuleId().equals(category.getModuleId())) {
                throw new IllegalArgumentException("Parent category belongs to a different module.");
            }
        }

        if (category.getId() != null) {
            ModuleCategory existing = getCategoryById(category.getId());
            existing.setCategoryNameEn(category.getCategoryNameEn());
            existing.setCategoryNameHi(category.getCategoryNameHi());
            existing.setParentId(category.getParentId());
            existing.setIconUrl(category.getIconUrl());
            existing.setActionType(category.getActionType());
            existing.setLinkKey(category.getLinkKey());
            existing.setDisplayOrder(category.getDisplayOrder());
            existing.setIsActive(category.getIsActive());
            return categoryRepository.save(existing);
        }
        return categoryRepository.save(category);
    }

    /**
     * Saves a list of master categories in a single transaction.
     * Maps default/audit configurations before inserting.
     */
    @Transactional
    public List<ModuleCategory> saveCategoriesBulk(List<ModuleCategory> categories) {
        if (categories == null || categories.isEmpty()) {
            throw new IllegalArgumentException("Category list cannot be null or empty");
        }

        // Processing lifecycle hooks manually for bulk insertion safety
        categories.forEach(category -> {
            if (category.getIsActive() == null) {
                category.setIsActive(true);
            }
            // If you track audit timestamps on master tables:
            // category.setCreatedDate(LocalDateTime.now());
            // category.setUpdatedDate(LocalDateTime.now());
        });

        return categoryRepository.saveAll(categories);
    }
}