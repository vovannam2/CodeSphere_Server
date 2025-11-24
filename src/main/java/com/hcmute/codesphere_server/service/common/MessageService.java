package com.hcmute.codesphere_server.service.common;

import com.hcmute.codesphere_server.model.entity.ConversationEntity;
import com.hcmute.codesphere_server.model.entity.MessageEntity;
import com.hcmute.codesphere_server.model.entity.UserEntity;
import com.hcmute.codesphere_server.model.payload.request.SendMessageRequest;
import com.hcmute.codesphere_server.model.payload.response.MessageResponse;
import com.hcmute.codesphere_server.repository.common.ConversationRepository;
import com.hcmute.codesphere_server.repository.common.MessageRepository;
import com.hcmute.codesphere_server.repository.common.UserRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Instant;

@Service
@RequiredArgsConstructor
public class MessageService {

    private final MessageRepository messageRepository;
    private final ConversationRepository conversationRepository;
    private final UserRepository userRepository;
    private final NotificationService notificationService;

    @Transactional
    public MessageResponse sendMessage(Long conversationId, SendMessageRequest request, Long userId) {
        ConversationEntity conversation = conversationRepository.findByIdAndParticipantId(conversationId, userId)
                .orElseThrow(() -> new RuntimeException("Không tìm thấy conversation hoặc bạn không có quyền truy cập"));

        UserEntity sender = userRepository.findById(userId)
                .orElseThrow(() -> new RuntimeException("Không tìm thấy user"));

        MessageEntity.MessageType messageType;
        try {
            messageType = MessageEntity.MessageType.valueOf(request.getMessageType().toUpperCase());
        } catch (IllegalArgumentException e) {
            throw new RuntimeException("MessageType không hợp lệ. Phải là TEXT hoặc IMAGE");
        }

        // Validate
        if (messageType == MessageEntity.MessageType.TEXT) {
            if (request.getContent() == null || request.getContent().isEmpty()) {
                throw new RuntimeException("TEXT message cần có content");
            }
        } else if (messageType == MessageEntity.MessageType.IMAGE) {
            if (request.getImageUrl() == null || request.getImageUrl().isEmpty()) {
                throw new RuntimeException("IMAGE message cần có imageUrl");
            }
        }

        MessageEntity message = MessageEntity.builder()
                .conversation(conversation)
                .sender(sender)
                .content(request.getContent())
                .messageType(messageType)
                .imageUrl(request.getImageUrl())
                .isDeleted(false)
                .createdAt(Instant.now())
                .updatedAt(Instant.now())
                .build();

        message = messageRepository.save(message);

        // Cập nhật updatedAt của conversation
        conversation.setUpdatedAt(Instant.now());
        conversationRepository.save(conversation);

        // Gửi notification cho tất cả participants (trừ sender)
        try {
            for (com.hcmute.codesphere_server.model.entity.UserEntity participant : conversation.getParticipants()) {
                if (!participant.getId().equals(userId)) {
                    notificationService.notifyMessage(
                            participant.getId(),
                            userId,
                            sender.getUsername(),
                            conversationId
                    );
                }
            }
        } catch (Exception e) {
            // Log error nhưng không throw
        }

        return mapToMessageResponse(message);
    }

    @Transactional(readOnly = true)
    public Page<MessageResponse> getMessages(Long conversationId, Long userId, Pageable pageable) {
        ConversationEntity conversation = conversationRepository.findByIdAndParticipantId(conversationId, userId)
                .orElseThrow(() -> new RuntimeException("Không tìm thấy conversation hoặc bạn không có quyền truy cập"));

        Page<MessageEntity> messages = messageRepository.findByConversationId(conversationId, pageable);
        return messages.map(this::mapToMessageResponse);
    }

    @Transactional
    public void deleteMessage(Long messageId, Long userId) {
        MessageEntity message = messageRepository.findByIdAndNotDeleted(messageId)
                .orElseThrow(() -> new RuntimeException("Không tìm thấy tin nhắn"));

        // Kiểm tra quyền sở hữu
        if (!message.getSender().getId().equals(userId)) {
            throw new RuntimeException("Bạn không có quyền xóa tin nhắn này");
        }

        message.setIsDeleted(true);
        message.setDeletedAt(Instant.now());
        message.setUpdatedAt(Instant.now());
        messageRepository.save(message);
        
        // Cập nhật updatedAt của conversation
        if (message.getConversation() != null) {
            message.getConversation().setUpdatedAt(Instant.now());
            conversationRepository.save(message.getConversation());
        }
    }

    private MessageResponse mapToMessageResponse(MessageEntity entity) {
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

