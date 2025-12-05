package com.hcmute.codesphere_server.repository.common;

import com.hcmute.codesphere_server.model.entity.UserProblemBestEntity;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface UserProblemBestRepository extends JpaRepository<UserProblemBestEntity, Long> {
    
    /**
     * Tìm best submission của một user cho một problem
     */
    Optional<UserProblemBestEntity> findByUserIdAndProblemId(Long userId, Long problemId);
    
    /**
     * Lấy tất cả best submissions của một problem, sắp xếp theo điểm cao nhất
     */
    @Query("SELECT upb FROM UserProblemBestEntity upb " +
           "WHERE upb.problem.id = :problemId " +
           "ORDER BY upb.bestScore DESC, upb.bestSubmission.createdAt ASC")
    List<UserProblemBestEntity> findAllByProblemIdOrderByBestScoreDesc(@Param("problemId") Long problemId);
    
    /**
     * Lấy tất cả best submissions của một user (chỉ những bài đã giải đúng)
     */
    @Query("SELECT upb FROM UserProblemBestEntity upb " +
           "WHERE upb.user.id = :userId AND upb.bestSubmission.isAccepted = true")
    List<UserProblemBestEntity> findAllByUserIdAndAccepted(@Param("userId") Long userId);
    
    /**
     * Đếm số bài đã giải đúng của một user theo độ khó
     */
    @Query("SELECT COUNT(upb) FROM UserProblemBestEntity upb " +
           "WHERE upb.user.id = :userId AND upb.bestSubmission.isAccepted = true AND upb.problem.level = :level")
    Long countSolvedByUserIdAndLevel(@Param("userId") Long userId, @Param("level") String level);
    
    /**
     * Đếm tổng số bài đã giải đúng của một user
     */
    @Query("SELECT COUNT(upb) FROM UserProblemBestEntity upb " +
           "WHERE upb.user.id = :userId AND upb.bestSubmission.isAccepted = true")
    Long countSolvedByUserId(@Param("userId") Long userId);
}

