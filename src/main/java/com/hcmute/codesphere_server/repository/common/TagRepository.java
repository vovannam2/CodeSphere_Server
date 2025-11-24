package com.hcmute.codesphere_server.repository.common;

import com.hcmute.codesphere_server.model.entity.TagEntity;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.Optional;

@Repository
public interface TagRepository extends JpaRepository<TagEntity, Long> {
    Optional<TagEntity> findBySlug(String slug);
    boolean existsBySlug(String slug);
}

