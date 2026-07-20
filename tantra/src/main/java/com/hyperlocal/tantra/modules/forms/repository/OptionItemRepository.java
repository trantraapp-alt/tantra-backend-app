package com.hyperlocal.tantra.modules.forms.repository;

import com.hyperlocal.tantra.modules.forms.entity.OptionItem;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface OptionItemRepository extends JpaRepository<OptionItem, Integer> {
    List<OptionItem> findByOptionSetIdOrderByDisplayOrderAsc(Integer optionSetId);
    List<OptionItem> findByOptionSetIdAndIsActiveTrueOrderByDisplayOrderAsc(Integer optionSetId);
    List<OptionItem> findByParentItemIdAndIsActiveTrueOrderByDisplayOrderAsc(Integer parentItemId);
}
