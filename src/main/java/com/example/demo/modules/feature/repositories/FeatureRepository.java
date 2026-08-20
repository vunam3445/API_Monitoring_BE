package com.example.demo.modules.feature.repositories;

import com.example.demo.modules.feature.entities.Feature;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;
import java.util.Optional;
import java.util.UUID;

@Repository
public interface FeatureRepository extends JpaRepository<Feature, UUID> {
    Optional<Feature> findByKey(String key);
    boolean existsByKey(String key);
}
