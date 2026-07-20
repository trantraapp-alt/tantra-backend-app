package com.hyperlocal.tantra.modules.forms.service;

import com.hyperlocal.tantra.modules.forms.entity.FormDefinition;
import com.hyperlocal.tantra.modules.forms.repository.FormDefinitionRepository;
import com.hyperlocal.tantra.modules.master.repository.ModuleCategoryRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

/**
 * Admin CRUD over form definitions. The field list itself is a jsonb document on the entity,
 * so editing a form's fields is just saving the definition again.
 */
@Service
public class FormService {

    @Autowired private FormDefinitionRepository formRepository;
    @Autowired private ModuleCategoryRepository categoryRepository;

    public List<FormDefinition> getByCategory(Integer categoryId) {
        return formRepository.findByCategoryId(categoryId);
    }

    public FormDefinition getById(Integer id) {
        return formRepository.findById(id)
                .orElseThrow(() -> new IllegalArgumentException("Form definition not found with ID: " + id));
    }

    @Transactional
    public FormDefinition saveOrUpdate(FormDefinition form) {
        if (form.getCategoryId() == null || !categoryRepository.existsById(form.getCategoryId())) {
            throw new IllegalArgumentException("Cannot map form. Category ID does not exist: " + form.getCategoryId());
        }

        if (form.getId() != null) {
            FormDefinition existing = getById(form.getId());
            existing.setTitle(form.getTitle());
            existing.setListingType(form.getListingType());
            existing.setFields(form.getFields());
            if (form.getVersion() != null) existing.setVersion(form.getVersion());
            if (form.getIsActive() != null) existing.setIsActive(form.getIsActive());
            return formRepository.save(existing);
        }
        return formRepository.save(form);
    }
}
