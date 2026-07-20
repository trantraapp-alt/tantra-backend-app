package com.hyperlocal.tantra.modules.forms.repository;

import com.hyperlocal.tantra.modules.forms.entity.OptionSet;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface OptionSetRepository extends JpaRepository<OptionSet, Integer> {
    Optional<OptionSet> findBySetKey(String setKey);
    boolean existsBySetKey(String setKey);
    List<OptionSet> findByIsActiveTrue();
}
