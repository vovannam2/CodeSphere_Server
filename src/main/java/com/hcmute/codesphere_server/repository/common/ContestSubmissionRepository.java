package com.hcmute.codesphere_server.repository.common;

import com.hcmute.codesphere_server.model.entity.ContestSubmissionEntity;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface ContestSubmissionRepository extends JpaRepository<ContestSubmissionEntity, Long> {

    @Query("SELECT cs FROM ContestSubmissionEntity cs WHERE cs.contest.id = :contestId ORDER BY cs.submittedAt DESC")
    List<ContestSubmissionEntity> findByContestId(@Param("contestId") Long contestId);

    @Query("SELECT cs FROM ContestSubmissionEntity cs WHERE cs.contest.id = :contestId AND cs.submission.user.id = :userId ORDER BY cs.submittedAt DESC")
    List<ContestSubmissionEntity> findByContestIdAndUserId(@Param("contestId") Long contestId, @Param("userId") Long userId);

    @Modifying
    @Query("DELETE FROM ContestSubmissionEntity cs WHERE cs.contest.id = :contestId AND cs.submission.user.id = :userId")
    void deleteByContestIdAndUserId(@Param("contestId") Long contestId, @Param("userId") Long userId);
}

