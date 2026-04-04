package com.backandwhite.domain.repository;

import com.backandwhite.domain.model.TaxRule;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;

import java.util.List;
import java.util.Map;
import java.util.Optional;

public interface TaxRuleRepository {
    TaxRule save(TaxRule rule);

    TaxRule update(TaxRule rule);

    Optional<TaxRule> findById(String id);

    Page<TaxRule> findAll(Map<String, Object> filters, Pageable pageable);

    List<TaxRule> findByCountryAndRegion(String country, String region);

    void delete(String id);
}
