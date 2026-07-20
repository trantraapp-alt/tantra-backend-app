package com.hyperlocal.tantra.modules.forms.controller;

import com.hyperlocal.tantra.constants.MessageConstants;
import com.hyperlocal.tantra.modules.forms.dto.ApiResponse;
import com.hyperlocal.tantra.modules.forms.entity.FormDefinition;
import com.hyperlocal.tantra.modules.forms.model.LocalizedText;
import com.hyperlocal.tantra.modules.forms.service.FormService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;

/**
 * Form-metadata CRUD. Admin only (locked to ROLE_ADMIN via /api/v1/admin/** in SecurityConfig).
 * Writes return a compact { success, message{en,hi}, id } envelope; reads return full data.
 */
@RestController
@RequestMapping("/api/v1/admin/forms")
public class AdminFormController {

    @Autowired private FormService service;

    @GetMapping
    public ResponseEntity<List<FormDefinition>> getByCategory(@RequestParam Integer categoryId) {
        return ResponseEntity.ok(service.getByCategory(categoryId));
    }

    @GetMapping("/{id}")
    public ResponseEntity<FormDefinition> getOne(@PathVariable Integer id) {
        return ResponseEntity.ok(service.getById(id));
    }

    @PostMapping
    public ResponseEntity<ApiResponse> createOrUpdate(@RequestBody FormDefinition form) {
        FormDefinition saved = service.saveOrUpdate(form);
        return ResponseEntity.ok(ApiResponse.ok(saved.getId(),
                LocalizedText.of(MessageConstants.ADMIN_SAVE_SUCCESS_EN, MessageConstants.ADMIN_SAVE_SUCCESS_HI)));
    }
}
