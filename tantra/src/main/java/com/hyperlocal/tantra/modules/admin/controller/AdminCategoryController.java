package com.hyperlocal.tantra.modules.admin.controller;

import com.hyperlocal.tantra.exception.LocalizedException;
import com.hyperlocal.tantra.modules.admin.dto.AdminQualityDescRequest;
import com.hyperlocal.tantra.modules.master.entity.ModuleCategory;
import com.hyperlocal.tantra.modules.master.repository.ModuleCategoryRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Map;

/**
 * Admin endpoints for category master management.
 * All routes under /api/v1/admin/** are restricted to ROLE_ADMIN via SecurityConfig.
 *
 *  GET  /api/v1/admin/categories              → list all categories (with quality desc)
 *  PUT  /api/v1/admin/categories/{id}/quality-desc → set quality description for a category
 */
@RestController
@RequestMapping("/api/v1/admin/categories")
public class AdminCategoryController {

    @Autowired private ModuleCategoryRepository categoryRepository;

    // ── List all categories ───────────────────────────────────────────────────

    /**
     * Returns all categories including quality desc fields so admin can see which ones
     * already have descriptions set and which are still empty.
     */
    @GetMapping
    public ResponseEntity<List<ModuleCategory>> listCategories(
            @RequestParam(required = false) Integer moduleId) {

        List<ModuleCategory> categories;
        if (moduleId != null) {
            categories = categoryRepository.findByModuleIdOrderByDisplayOrderAsc(moduleId);
        } else {
            categories = categoryRepository.findAll();
        }
        return ResponseEntity.ok(categories);
    }

    // ── Set / Update quality description ─────────────────────────────────────

    /**
     * Sets the quality-assured description for a category.
     * Send null for either field to clear it.
     *
     * Example:
     *   PUT /api/v1/admin/categories/1/quality-desc
     *   {
     *     "qualityDescEn": "All crop listings are verified for freshness and grade accuracy.",
     *     "qualityDescHi": "सभी फसल लिस्टिंग ताजगी और ग्रेड के लिए सत्यापित हैं।"
     *   }
     */
    @PutMapping("/{categoryId}/quality-desc")
    public ResponseEntity<Map<String, Object>> setQualityDesc(
            @PathVariable Integer categoryId,
            @RequestBody AdminQualityDescRequest req) {

        ModuleCategory category = categoryRepository.findById(categoryId)
                .orElseThrow(() -> new LocalizedException(
                        "Category not found.", "श्रेणी नहीं मिली।"));

        category.setQualityDescEn(req.getQualityDescEn());
        category.setQualityDescHi(req.getQualityDescHi());
        categoryRepository.save(category);

        return ResponseEntity.ok(Map.of(
                "message", "Quality description updated successfully.",
                "categoryId", categoryId,
                "categoryNameEn", category.getCategoryNameEn(),
                "qualityDescEn", req.getQualityDescEn() != null ? req.getQualityDescEn() : "",
                "qualityDescHi", req.getQualityDescHi() != null ? req.getQualityDescHi() : ""
        ));
    }
}
