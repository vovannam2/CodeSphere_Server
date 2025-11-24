package com.hcmute.codesphere_server.service.common;

import com.hcmute.codesphere_server.model.entity.ConversationEntity;
import com.hcmute.codesphere_server.model.entity.ConversationParticipantEntity;
import com.hcmute.codesphere_server.model.entity.UserEntity;
import com.hcmute.codesphere_server.model.payload.request.AddMemberRequest;
import com.hcmute.codesphere_server.model.payload.request.CreateConversationRequest;
import com.hcmute.codesphere_server.model.payload.request.UpdateConversationRequest;
import com.hcmute.codesphere_server.model.payload.response.ConversationParticipantResponse;
import com.hcmute.codesphere_server.model.payload.response.ConversationResponse;
import com.hcmute.codesphere_server.model.payload.response.MessageResponse;
import com.hcmute.codesphere_server.repository.common.ConversationParticipantRepository;
import com.hcmute.codesphere_server.repository.common.ConversationRepository;
import com.hcmute.codesphere_server.repository.common.MessageRepository;
import com.hcmute.codesphere_server.repository.common.UserRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Instant;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
public class ConversationService {

    private final ConversationRepository conversationRepository;
    private final ConversationParticipantRepository conversationParticipantRepository;
    private final MessageRepository messageRepository;
    private final UserRepository userRepository;

    @Transactional
    public ConversationResponse createConversation(CreateConversationRequest request, Long userId) {
        UserEntity creator = userRepository.findById(userId)
                .orElseThrow(() -> new RuntimeException("Không tìm thấy user"));

        ConversationEntity.ConversationType type;
        try {
            type = ConversationEntity.ConversationType.valueOf(request.getType().toUpperCase());
        } catch (IllegalArgumentException e) {
            throw new RuntimeException("Type không hợp lệ. Phải là DIRECT hoặc GROUP");
        }

        // Validate DIRECT conversation
        if (type == ConversationEntity.ConversationType.DIRECT) {
            if (request.getParticipantIds().size() != 1) {
                throw new RuntimeException("DIRECT conversation chỉ có thể có 1 participant (người nhận)");
            }
            
            // Kiểm tra đã có direct conversation chưa
            Long otherUserId = request.getParticipantIds().get(0);
            var existing = conversationRepository.findDirectConversation(userId, otherUserId);
            if (existing.isPresent()) {
                return mapToConversationResponse(existing.get(), userId);
            }
        }

        // Validate GROUP conversation
        if (type == ConversationEntity.ConversationType.GROUP) {
            if (request.getName() == null || request.getName().isEmpty()) {
                throw new RuntimeException("GROUP conversation cần có tên");
            }
            if (request.getParticipantIds().isEmpty()) {
                throw new RuntimeException("GROUP conversation cần có ít nhất 1 participant");
            }
        }

        ConversationEntity conversation = ConversationEntity.builder()
                .type(type)
                .name(request.getName())
                .avatar(request.getAvatar())
                .createdBy(creator)
                .participants(new HashSet<>())
                .createdAt(Instant.now())
                .updatedAt(Instant.now())
                .build();

        // Thêm creator vào participants
        conversation.getParticipants().add(creator);

        // Thêm các participants khác
        Set<UserEntity> participants = request.getParticipantIds().stream()
                .map(participantId -> userRepository.findById(participantId)
                        .orElseThrow(() -> new RuntimeException("Không tìm thấy user với ID: " + participantId)))
                .collect(Collectors.toSet());
        conversation.getParticipants().addAll(participants);

        conversation = conversationRepository.save(conversation);

        // Tạo ConversationParticipantEntity cho từng participant
        for (UserEntity participant : conversation.getParticipants()) {
            ConversationParticipantEntity.ParticipantRole role = 
                    participant.getId().equals(userId) && type == ConversationEntity.ConversationType.GROUP
                            ? ConversationParticipantEntity.ParticipantRole.ADMIN
                            : ConversationParticipantEntity.ParticipantRole.MEMBER;
            
            ConversationParticipantEntity cp = ConversationParticipantEntity.builder()
                    .conversation(conversation)
                    .user(participant)
                    .role(role)
                    .joinedAt(Instant.now())
                    .build();
            conversationParticipantRepository.save(cp);
        }

        return mapToConversationResponse(conversation, userId);
    }

    @Transactional(readOnly = true)
    public List<ConversationResponse> getConversations(Long userId) {
        List<ConversationEntity> conversations = conversationRepository.findByParticipantId(userId);
        return conversations.stream()
                .map(conv -> mapToConversationResponse(conv, userId))
                .collect(Collectors.toList());
    }

    @Transactional(readOnly = true)
    public ConversationResponse getConversationById(Long conversationId, Long userId) {
        ConversationEntity conversation = conversationRepository.findByIdAndParticipantId(conversationId, userId)
                .orElseThrow(() -> new RuntimeException("Không tìm thấy conversation hoặc bạn không có quyền truy cập"));
        
        return mapToConversationResponse(conversation, userId);
    }

    @Transactional
    public ConversationResponse updateConversation(Long conversationId, UpdateConversationRequest request, Long userId) {
        ConversationEntity conversation = conversationRepository.findByIdAndParticipantId(conversationId, userId)
                .orElseThrow(() -> new RuntimeException("Không tìm thấy conversation hoặc bạn không có quyền truy cập"));

        // Chỉ GROUP conversation mới có thể update
        if (conversation.getType() != ConversationEntity.ConversationType.GROUP) {
            throw new RuntimeException("Chỉ GROUP conversation mới có thể chỉnh sửa");
        }

        // Kiểm tra quyền ADMIN
        var participant = conversationParticipantRepository.findByConversationIdAndUserId(conversationId, userId);
        if (participant.isEmpty() || 
            participant.get().getRole() != ConversationParticipantEntity.ParticipantRole.ADMIN) {
            throw new RuntimeException("Chỉ ADMIN mới có quyền chỉnh sửa conversation");
        }

        if (request.getName() != null) {
            conversation.setName(request.getName());
        }
        if (request.getAvatar() != null) {
            conversation.setAvatar(request.getAvatar());
        }

        conversation.setUpdatedAt(Instant.now());
        conversation = conversationRepository.save(conversation);
        return mapToConversationResponse(conversation, userId);
    }

    @Transactional
    public ConversationResponse addMember(Long conversationId, AddMemberRequest request, Long userId) {
        ConversationEntity conversation = conversationRepository.findByIdAndParticipantId(conversationId, userId)
                .orElseThrow(() -> new RuntimeException("Không tìm thấy conversation hoặc bạn không có quyền truy cập"));

        // Chỉ GROUP conversation mới có thể thêm member
        if (conversation.getType() != ConversationEntity.ConversationType.GROUP) {
            throw new RuntimeException("Chỉ GROUP conversation mới có thể thêm thành viên");
        }

        // Kiểm tra quyền ADMIN
        var participant = conversationParticipantRepository.findByConversationIdAndUserId(conversationId, userId);
        if (participant.isEmpty() || 
            participant.get().getRole() != ConversationParticipantEntity.ParticipantRole.ADMIN) {
            throw new RuntimeException("Chỉ ADMIN mới có quyền thêm thành viên");
        }

        for (Long newUserId : request.getUserIds()) {
            // Kiểm tra đã là participant chưa
            var existingParticipant = conversationParticipantRepository.findByConversationIdAndUserId(conversationId, newUserId);
            if (existingParticipant.isPresent()) {
                continue; // Đã là member rồi
            }

            UserEntity newUser = userRepository.findById(newUserId)
                    .orElseThrow(() -> new RuntimeException("Không tìm thấy user với ID: " + newUserId));

            conversation.getParticipants().add(newUser);

            ConversationParticipantEntity cp = ConversationParticipantEntity.builder()
                    .conversation(conversation)
                    .user(newUser)
                    .role(ConversationParticipantEntity.ParticipantRole.MEMBER)
                    .joinedAt(Instant.now())
                    .build();
            conversationParticipantRepository.save(cp);
        }

        conversation.setUpdatedAt(Instant.now());
        conversation = conversationRepository.save(conversation);
        return mapToConversationResponse(conversation, userId);
    }

    @Transactional
    public void removeMember(Long conversationId, Long memberId, Long userId) {
        ConversationEntity conversation = conversationRepository.findByIdAndParticipantId(conversationId, userId)
                .orElseThrow(() -> new RuntimeException("Không tìm thấy conversation hoặc bạn không có quyền truy cập"));

        // Chỉ GROUP conversation mới có thể xóa member
        if (conversation.getType() != ConversationEntity.ConversationType.GROUP) {
            throw new RuntimeException("Chỉ GROUP conversation mới có thể xóa thành viên");
        }

        // Kiểm tra quyền ADMIN hoặc tự xóa chính mình
        var participant = conversationParticipantRepository.findByConversationIdAndUserId(conversationId, userId);
        boolean isAdmin = participant.isPresent() && 
                         participant.get().getRole() == ConversationParticipantEntity.ParticipantRole.ADMIN;
        
        if (!isAdmin && !memberId.equals(userId)) {
            throw new RuntimeException("Chỉ ADMIN mới có quyền xóa thành viên khác");
        }

        // Không thể xóa creator
        if (conversation.getCreatedBy().getId().equals(memberId)) {
            throw new RuntimeException("Không thể xóa người tạo conversation");
        }

        var memberParticipant = conversationParticipantRepository.findByConversationIdAndUserId(conversationId, memberId);
        if (memberParticipant.isPresent()) {
            conversation.getParticipants().remove(memberParticipant.get().getUser());
            conversationParticipantRepository.delete(memberParticipant.get());
            conversation.setUpdatedAt(Instant.now());
            conversationRepository.save(conversation);
        }
    }

    private ConversationResponse mapToConversationResponse(ConversationEntity entity, Long userId) {
        // Lấy last message
        MessageResponse lastMessage = null;
        var lastMessages = messageRepository.findAllByConversationId(entity.getId());
        if (!lastMessages.isEmpty()) {
            lastMessage = mapToMessageResponse(lastMessages.get(0));
        }

        // Lấy participants
        List<ConversationParticipantResponse> participants = conversationParticipantRepository
                .findByConversationId(entity.getId())
                .stream()
                .map(this::mapToParticipantResponse)
                .collect(Collectors.toList());

        return ConversationResponse.builder()
                .id(entity.getId())
                .type(entity.getType() != null ? entity.getType().name() : null)
                .name(entity.getName())
                .avatar(entity.getAvatar())
                .createdById(entity.getCreatedBy() != null ? entity.getCreatedBy().getId() : null)
                .createdByName(entity.getCreatedBy() != null ? entity.getCreatedBy().getUsername() : null)
                .participants(participants)
                .lastMessage(lastMessage)
                .unreadCount(0L) // TODO: Implement unread count logic
                .createdAt(entity.getCreatedAt())
                .updatedAt(entity.getUpdatedAt())
                .build();
    }

    private ConversationParticipantResponse mapToParticipantResponse(ConversationParticipantEntity entity) {
        return ConversationParticipantResponse.builder()
                .userId(entity.getUser() != null ? entity.getUser().getId() : null)
                .username(entity.getUser() != null ? entity.getUser().getUsername() : null)
                .avatar(entity.getUser() != null ? entity.getUser().getAvatar() : null)
                .role(entity.getRole() != null ? entity.getRole().name() : null)
                .joinedAt(entity.getJoinedAt())
                .build();
    }

    private MessageResponse mapToMessageResponse(com.hcmute.codesphere_server.model.entity.MessageEntity entity) {
        return MessageResponse.builder()
                .id(entity.getId())
                .conversationId(entity.getConversation() != null ? entity.getConversation().getId() : null)
                .senderId(entity.getSender() != null ? entity.getSender().getId() : null)
                .senderName(entity.getSender() != null ? entity.getSender().getUsername() : null)
                .senderAvatar(entity.getSender() != null ? entity.getSender().getAvatar() : null)
                .content(entity.getContent())
                .messageType(entity.getMessageType() != null ? entity.getMessageType().name() : null)
                .imageUrl(entity.getImageUrl())
                .isDeleted(entity.getIsDeleted())
                .createdAt(entity.getCreatedAt())
                .updatedAt(entity.getUpdatedAt())
                .build();
    }
}

