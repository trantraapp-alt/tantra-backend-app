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

    public ModuleCategory getCategoryById(Integer id) {
        return categoryRepository.findById(id)
                .orElseThrow(() -> new IllegalArgumentException("Category not found with ID: " + id));
    }

    @Transactional
    public ModuleCategory saveOrUpdateCategory(ModuleCategory category) {
        if (!moduleRepository.existsById(category.getModuleId())) {
            throw new IllegalArgumentException("Cannot map category. Parent Module ID does not exist.");
        }

        if (category.getId() != null) {
            ModuleCategory existing = getCategoryById(category.getId());
            existing.setCategoryNameEn(category.getCategoryNameEn());
            existing.setCategoryNameHi(category.getCategoryNameHi());
            existing.setIconUrl(category.getIconUrl());
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