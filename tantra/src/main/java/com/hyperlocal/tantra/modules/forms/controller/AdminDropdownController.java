package com.hyperlocal.tantra.modules.forms.controller;

import com.hyperlocal.tantra.constants.MessageConstants;
import com.hyperlocal.tantra.modules.forms.dto.ApiResponse;
import com.hyperlocal.tantra.modules.forms.entity.OptionItem;
import com.hyperlocal.tantra.modules.forms.entity.OptionSet;
import com.hyperlocal.tantra.modules.forms.model.LocalizedText;
import com.hyperlocal.tantra.modules.forms.service.OptionSetService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Map;

/**
 * Dropdown-maintenance API. Admin only (locked to ROLE_ADMIN via /api/v1/admin/** in SecurityConfig).
 * Writes return a compact { success, message{en,hi}, id } envelope; reads return full data.
 */
@RestController
@RequestMapping("/api/v1/admin/option-sets")
public class AdminDropdownController {

    @Autowired private OptionSetService service;

    private LocalizedText savedMsg() {
        return LocalizedText.of(MessageConstants.ADMIN_SAVE_SUCCESS_EN, MessageConstants.ADMIN_SAVE_SUCCESS_HI);
    }

    private LocalizedText deletedMsg() {
        return LocalizedText.of(MessageConstants.ADMIN_DELETE_SUCCESS_EN, MessageConstants.ADMIN_DELETE_SUCCESS_HI);
    }

    @GetMapping
    public ResponseEntity<List<OptionSet>> getAllSets(@RequestParam(defaultValue = "false") boolean onlyActive) {
        return ResponseEntity.ok(service.getAllSets(onlyActive));
    }

    @GetMapping("/{id}")
    public ResponseEntity<OptionSet> getSet(@PathVariable Integer id) {
        return ResponseEntity.ok(service.getSetById(id));
    }

    @PostMapping
    public ResponseEntity<ApiResponse> createOrUpdateSet(@RequestBody OptionSet set) {
        OptionSet saved = service.saveOrUpdateSet(set);
        return ResponseEntity.ok(ApiResponse.ok(saved.getId(), savedMsg()));
    }

    @GetMapping("/{id}/items")
    public ResponseEntity<List<OptionItem>> getItems(
            @PathVariable Integer id,
            @RequestParam(defaultValue = "false") boolean onlyActive) {
        return ResponseEntity.ok(service.getItems(id, onlyActive));
    }

    @PostMapping("/{id}/items")
    public ResponseEntity<ApiResponse> addOrUpdateItem(@PathVariable Integer id, @RequestBody OptionItem item) {
        OptionItem saved = service.saveOrUpdateItem(id, item);
        return ResponseEntity.ok(ApiResponse.ok(saved.getId(), savedMsg()));
    }

    @PostMapping("/{id}/items/bulk")
    public ResponseEntity<ApiResponse> bulkAddItems(@PathVariable Integer id, @RequestBody List<OptionItem> items) {
        List<OptionItem> saved = service.bulkAddItems(id, items);
        List<Integer> ids = saved.stream().map(OptionItem::getId).toList();
        return ResponseEntity.ok(ApiResponse.okData(Map.of("ids", ids, "count", ids.size()), savedMsg()));
    }

    @DeleteMapping("/items/{itemId}")
    public ResponseEntity<ApiResponse> deleteItem(@PathVariable Integer itemId) {
        service.deleteItem(itemId);
        return ResponseEntity.ok(ApiResponse.ok(deletedMsg()));
    }
}
