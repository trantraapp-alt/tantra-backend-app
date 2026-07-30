package com.hyperlocal.tantra.modules.business.controller;

import com.hyperlocal.tantra.modules.business.dto.BusinessProfileRequest;
import com.hyperlocal.tantra.modules.business.dto.BusinessProfileResponse;
import com.hyperlocal.tantra.modules.business.entity.BusinessProfile;
import com.hyperlocal.tantra.modules.business.service.BusinessProfileService;
import com.hyperlocal.tantra.modules.forms.dto.FormRenderResponse;
import com.hyperlocal.tantra.modules.forms.service.FormRenderService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.*;

import java.util.List;

/**
 * Business profile — owner CRUD (Profile ▸ Business Profile) + public visible view.
 * Authenticated via /api/v1/business-profiles/** in SecurityConfig; owner from the JWT.
 */
@RestController
@RequestMapping("/api/v1/business-profiles")
public class BusinessProfileController {

    @Autowired private BusinessProfileService service;
    @Autowired private FormRenderService formRenderService;

    /** The metadata-driven form to render for creating/editing a business profile of this type. */
    @GetMapping("/form")
    public ResponseEntity<FormRenderResponse> form(@RequestParam(required = false) String profileType) {
        return ResponseEntity.ok(formRenderService.renderBusinessProfileForm(profileType));
    }

    @PostMapping
    public ResponseEntity<BusinessProfileResponse> create(@RequestBody BusinessProfileRequest request, Authentication auth) {
        return ResponseEntity.ok(service.create(request, auth.getName()));
    }

    @GetMapping("/mine")
    public ResponseEntity<List<BusinessProfile>> mine(Authentication auth) {
        return ResponseEntity.ok(service.getMine(auth.getName()));
    }

    @GetMapping("/{profileId}")
    public ResponseEntity<BusinessProfile> get(@PathVariable String profileId, Authentication auth) {
        return ResponseEntity.ok(service.getVisible(profileId, auth.getName()));
    }

    @PutMapping("/{profileId}")
    public ResponseEntity<BusinessProfileResponse> update(
            @PathVariable String profileId, @RequestBody BusinessProfileRequest request, Authentication auth) {
        return ResponseEntity.ok(service.update(profileId, request, auth.getName()));
    }

    @DeleteMapping("/{profileId}")
    public ResponseEntity<BusinessProfileResponse> delete(@PathVariable String profileId, Authentication auth) {
        return ResponseEntity.ok(service.delete(profileId, auth.getName()));
    }
}
