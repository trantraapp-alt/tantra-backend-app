package com.hyperlocal.tantra.modules.master.controller;

import com.hyperlocal.tantra.modules.master.entity.AppModule;
import com.hyperlocal.tantra.modules.master.entity.ModuleCategory;
import com.hyperlocal.tantra.modules.master.service.MasterManagementService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/v1/masters")
public class MasterController {

    @Autowired private MasterManagementService masterService;

    // --- MODULE ENDPOINTS ---

    @GetMapping("/modules")
    public ResponseEntity<List<AppModule>> getAllModules(@RequestParam(defaultValue = "false") boolean onlyActive) {
        return ResponseEntity.ok(masterService.getAllModules(onlyActive));
    }

    @GetMapping("/modules/{id}")
    public ResponseEntity<AppModule> getModuleById(@PathVariable Integer id) {
        return ResponseEntity.ok(masterService.getModuleById(id));
    }

    @PostMapping("/modules")
    public ResponseEntity<AppModule> createOrUpdateModule(@RequestBody AppModule appModule) {
        return ResponseEntity.ok(masterService.saveOrUpdateModule(appModule));
    }

    // --- CATEGORY ENDPOINTS ---

    /**
     * All categories flat list — parent + sub across all modules.
     * Optional: ?moduleId=1 to scope to one module, ?onlyActive=true to exclude inactive.
     * Use this for dropdowns, search filters, deal group config, etc.
     */
    @GetMapping("/categories")
    public ResponseEntity<List<ModuleCategory>> getAllCategories(
            @RequestParam(required = false) Integer moduleId,
            @RequestParam(defaultValue = "false") boolean onlyActive) {
        return ResponseEntity.ok(masterService.getAllCategories(moduleId, onlyActive));
    }

    /**
     * All subcategories only (parentId IS NOT NULL) across all modules — flat list.
     * Use when you need leaf-level categories for listing forms or filters.
     */
    @GetMapping("/categories/subcategories")
    public ResponseEntity<List<ModuleCategory>> getAllSubcategories(
            @RequestParam(defaultValue = "false") boolean onlyActive) {
        return ResponseEntity.ok(masterService.getAllSubcategories(onlyActive));
    }

    /** Top-level categories under a module (parentId omitted), or subcategories of a parent (parentId set). */
    @GetMapping("/modules/{moduleId}/categories")
    public ResponseEntity<List<ModuleCategory>> getCategoriesByModule(
            @PathVariable Integer moduleId,
            @RequestParam(required = false) Integer parentId,
            @RequestParam(defaultValue = "false") boolean onlyActive) {
        return ResponseEntity.ok(masterService.getCategories(moduleId, parentId, onlyActive));
    }

    /** Subcategories of a specific parent category (tree children). */
    @GetMapping("/categories/{parentId}/subcategories")
    public ResponseEntity<List<ModuleCategory>> getSubcategories(
            @PathVariable Integer parentId,
            @RequestParam(defaultValue = "false") boolean onlyActive) {
        return ResponseEntity.ok(masterService.getSubcategories(parentId, onlyActive));
    }

    @GetMapping("/categories/{id}")
    public ResponseEntity<ModuleCategory> getCategoryById(@PathVariable Integer id) {
        return ResponseEntity.ok(masterService.getCategoryById(id));
    }

    @PostMapping("/categories")
    public ResponseEntity<ModuleCategory> createOrUpdateCategory(@RequestBody ModuleCategory category) {
        return ResponseEntity.ok(masterService.saveOrUpdateCategory(category));

    }

    @PostMapping("/categories/bulk")
    public ResponseEntity<List<ModuleCategory>> createCategoriesBulk(@RequestBody List<ModuleCategory> categories) {
        List<ModuleCategory> savedCategories = masterService.saveCategoriesBulk(categories);
        return ResponseEntity.ok(savedCategories);
    }
}