package com.hcmute.codesphere_server.repository.common;

import com.hcmute.codesphere_server.model.entity.ProblemEntity;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.JpaSpecificationExecutor;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface ProblemRepository extends JpaRepository<ProblemEntity, Long>, JpaSpecificationExecutor<ProblemEntity> {
    Optional<ProblemEntity> findBySlug(String slug);
    Optional<ProblemEntity> findByCode(String code);
    boolean existsBySlug(String slug);
    boolean existsByCode(String code);
    
    @Query("SELECT p FROM ProblemEntity p WHERE p.id = :id AND p.status = true")
    Optional<ProblemEntity> findByIdAndStatusTrue(@Param("id") Long id);
    //Method để kiểm tra số problem tham chiếu tới tag
    long countByTags_Id(Long tagId);

    // (nếu bạn đã dùng findAllByTags_Id ở nơi khác, giữ nguyên)
    List<ProblemEntity> findAllByTags_Id(Long tagId);
    
    // Method để kiểm tra số problem tham chiếu tới category
    long countByCategories_Id(Long categoryId);

    // (tùy chọn) tiện ích: lấy tất cả problem tham chiếu tới category
    List<ProblemEntity> findAllByCategories_Id(Long categoryId);
}

