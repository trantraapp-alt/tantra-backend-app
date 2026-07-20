package com.hyperlocal.tantra.modules.forms.controller;

import com.hyperlocal.tantra.modules.forms.dto.FormRenderResponse;
import com.hyperlocal.tantra.modules.forms.model.ListingType;
import com.hyperlocal.tantra.modules.forms.service.FormRenderService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;

/**
 * App-facing, read-only form rendering. Any user can fetch the form metadata for a category and
 * the values of a cascading dropdown.
 */
@RestController
@RequestMapping("/api/v1")
public class FormRenderController {

    @Autowired private FormRenderService renderService;

    @GetMapping("/categories/{categoryId}/form")
    public ResponseEntity<FormRenderResponse> renderForm(
            @PathVariable Integer categoryId,
            @RequestParam(defaultValue = "SELL") ListingType listingType) {
        return ResponseEntity.ok(renderService.renderForCategory(categoryId, listingType));
    }

    @GetMapping("/option-sets/{setKey}/items")
    public ResponseEntity<List<FormRenderResponse.Option>> getItems(
            @PathVariable String setKey,
            @RequestParam(required = false) Integer parentItemId) {
        return ResponseEntity.ok(renderService.getItemsByKey(setKey, parentItemId));
    }
}
