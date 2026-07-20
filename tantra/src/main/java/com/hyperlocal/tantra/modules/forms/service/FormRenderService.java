package com.hyperlocal.tantra.modules.forms.service;

import com.hyperlocal.tantra.modules.forms.dto.FormRenderResponse;
import com.hyperlocal.tantra.modules.forms.entity.FormDefinition;
import com.hyperlocal.tantra.modules.forms.entity.OptionItem;
import com.hyperlocal.tantra.modules.forms.model.FormField;
import com.hyperlocal.tantra.modules.forms.model.ListingType;
import com.hyperlocal.tantra.modules.forms.repository.FormDefinitionRepository;
import com.hyperlocal.tantra.modules.forms.repository.OptionItemRepository;
import com.hyperlocal.tantra.modules.forms.repository.OptionSetRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.LinkedHashMap;
import java.util.List;

/**
 * Builds the app-facing render payload from stored metadata: groups fields into sections (in
 * display order) and resolves each dropdown field's option set into inline, multilingual values.
 */
@Service
public class FormRenderService {

    @Autowired private FormDefinitionRepository formRepository;
    @Autowired private OptionSetRepository setRepository;
    @Autowired private OptionItemRepository itemRepository;

    public FormRenderResponse renderForCategory(Integer categoryId, ListingType listingType) {
        FormDefinition def = formRepository
                .findFirstByCategoryIdAndListingTypeAndIsActiveTrueOrderByVersionDesc(categoryId, listingType)
                .orElseThrow(() -> new IllegalArgumentException(
                        "No active form for category " + categoryId + " and listing type " + listingType));
        return buildResponse(def);
    }

    private FormRenderResponse buildResponse(FormDefinition def) {
        FormRenderResponse resp = new FormRenderResponse();
        resp.setFormId(def.getId());
        resp.setVersion(def.getVersion());
        resp.setListingType(def.getListingType());
        resp.setCategoryId(def.getCategoryId());
        resp.setTitle(def.getTitle());

        List<FormField> fields = def.getFields() == null ? Collections.emptyList() : def.getFields();

        // Group into sections, preserving first-seen section order and per-field display order.
        LinkedHashMap<String, FormRenderResponse.Section> sections = new LinkedHashMap<>();
        fields.stream()
                .sorted(Comparator.comparing(f -> f.getDisplayOrder() == null ? 0 : f.getDisplayOrder()))
                .forEach(field -> {
                    String sectionKey = field.getSectionKey() == null ? "default" : field.getSectionKey();
                    FormRenderResponse.Section section = sections.computeIfAbsent(sectionKey, k -> {
                        FormRenderResponse.Section s = new FormRenderResponse.Section();
                        s.setKey(k);
                        s.setTitle(field.getSectionTitle());
                        s.setFields(new ArrayList<>());
                        return s;
                    });
                    section.getFields().add(toRenderField(field));
                });

        resp.setSections(new ArrayList<>(sections.values()));
        return resp;
    }

    private FormRenderResponse.RenderField toRenderField(FormField field) {
        FormRenderResponse.RenderField rf = new FormRenderResponse.RenderField();
        rf.setFieldKey(field.getFieldKey());
        rf.setType(field.getType());
        rf.setLabel(field.getLabel());
        rf.setRequired(Boolean.TRUE.equals(field.getRequired()));
        rf.setReadOnly(Boolean.TRUE.equals(field.getReadOnly()));
        rf.setFieldLength(field.getFieldLength());
        rf.setPlaceholder(field.getPlaceholder());
        rf.setHelp(field.getHelp());
        rf.setDisplayOrder(field.getDisplayOrder());
        rf.setAllowOther(Boolean.TRUE.equals(field.getAllowOther()));
        rf.setMultiple(Boolean.TRUE.equals(field.getMultiple()));
        rf.setCommon(Boolean.TRUE.equals(field.getCommon()));
        rf.setValidation(field.getValidation());
        rf.setComputed(field.getComputed());
        rf.setVisibleWhen(field.getVisibleWhen());
        rf.setOptionSetKey(field.getOptionSetKey());

        if (field.getOptionSetKey() != null && !field.getOptionSetKey().isBlank()) {
            setRepository.findBySetKey(field.getOptionSetKey()).ifPresent(set -> {
                List<OptionItem> items =
                        itemRepository.findByOptionSetIdAndIsActiveTrueOrderByDisplayOrderAsc(set.getId());
                rf.setOptions(items.stream().map(this::toOption).toList());
            });
        }
        return rf;
    }

    /**
     * App endpoint for cascading dropdowns: fetch the children of a selected parent option item,
     * or the full set when no parent is given.
     */
    public List<FormRenderResponse.Option> getItemsByKey(String setKey, Integer parentItemId) {
        var set = setRepository.findBySetKey(setKey)
                .orElseThrow(() -> new IllegalArgumentException("Option set not found: " + setKey));
        List<OptionItem> items = parentItemId != null
                ? itemRepository.findByParentItemIdAndIsActiveTrueOrderByDisplayOrderAsc(parentItemId)
                : itemRepository.findByOptionSetIdAndIsActiveTrueOrderByDisplayOrderAsc(set.getId());
        return items.stream().map(this::toOption).toList();
    }

    private FormRenderResponse.Option toOption(OptionItem item) {
        FormRenderResponse.Option option = new FormRenderResponse.Option();
        option.setValue(item.getItemKey());
        option.setLabel(item.getLabel());
        option.setParent(item.getParentItemId());
        return option;
    }
}
