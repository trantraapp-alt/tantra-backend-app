package com.hyperlocal.tantra.modules.forms.repository;

import com.hyperlocal.tantra.modules.forms.entity.FormDefinition;
import com.hyperlocal.tantra.modules.forms.model.ListingType;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface FormDefinitionRepository extends JpaRepository<FormDefinition, Integer> {
    List<FormDefinition> findByCategoryId(Integer categoryId);

    Optional<FormDefinition> findFirstByCategoryIdAndListingTypeAndIsActiveTrueOrderByVersionDesc(
            Integer categoryId, ListingType listingType);

    Optional<FormDefinition> findFirstByFormTypeAndContextKeyAndIsActiveTrueOrderByVersionDesc(
            String formType, String contextKey);
}
