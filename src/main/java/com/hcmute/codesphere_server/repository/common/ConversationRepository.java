package com.hcmute.codesphere_server.repository.common;

import com.hcmute.codesphere_server.model.entity.ConversationEntity;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface ConversationRepository extends JpaRepository<ConversationEntity, Long> {
    
    @Query("SELECT DISTINCT c FROM ConversationEntity c " +
           "JOIN c.participants p WHERE p.id = :userId " +
           "ORDER BY c.updatedAt DESC")
    List<ConversationEntity> findByParticipantId(@Param("userId") Long userId);
    
    @Query("SELECT c FROM ConversationEntity c " +
           "WHERE c.id = :id AND :userId IN (SELECT p.id FROM c.participants p)")
    Optional<ConversationEntity> findByIdAndParticipantId(@Param("id") Long id, @Param("userId") Long userId);
    
    @Query("SELECT c FROM ConversationEntity c " +
           "WHERE c.type = 'DIRECT' " +
           "AND :userId1 IN (SELECT p.id FROM c.participants p) " +
           "AND :userId2 IN (SELECT p.id FROM c.participants p) " +
           "AND SIZE(c.participants) = 2")
    Optional<ConversationEntity> findDirectConversation(@Param("userId1") Long userId1, @Param("userId2") Long userId2);
}

