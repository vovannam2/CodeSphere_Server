package com.hcmute.codesphere_server.service.common;

import com.hcmute.codesphere_server.model.entity.*;
import com.hcmute.codesphere_server.model.payload.request.CreateSubmissionRequest;
import com.hcmute.codesphere_server.model.payload.response.SubmissionDetailResponse;
import com.hcmute.codesphere_server.model.payload.response.SubmissionResponse;
import com.hcmute.codesphere_server.repository.common.*;
import jakarta.persistence.criteria.Predicate;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.domain.Specification;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import jakarta.persistence.EntityManager;
import org.springframework.transaction.support.TransactionSynchronizationManager;
import org.springframework.transaction.support.TransactionSynchronization;

import java.time.Instant;
import java.util.ArrayList;
import java.util.List;

@Service
@RequiredArgsConstructor
public class SubmissionService {

    private final SubmissionRepository submissionRepository;
    private final ProblemRepository problemRepository;
    private final LanguageRepository languageRepository;
    private final UserRepository userRepository;
    private final JudgeService judgeService;
    private final EntityManager entityManager;

    @Transactional
    public SubmissionDetailResponse createSubmission(CreateSubmissionRequest request, Long userId) {
        // Kiểm tra problem tồn tại và active
        ProblemEntity problem = problemRepository.findByIdAndStatusTrue(request.getProblemId())
                .orElseThrow(() -> new RuntimeException("Không tìm thấy bài tập"));

        // Kiểm tra language tồn tại
        LanguageEntity language = languageRepository.findById(request.getLanguageId())
                .orElseThrow(() -> new RuntimeException("Không tìm thấy ngôn ngữ"));

        // Kiểm tra user tồn tại
        UserEntity user = userRepository.findById(userId)
                .orElseThrow(() -> new RuntimeException("Không tìm thấy user"));

        // Kiểm tra language có được hỗ trợ bởi problem không
        boolean isLanguageSupported = problem.getLanguages().stream()
                .anyMatch(lang -> lang.getId().equals(request.getLanguageId()));
        
        if (!isLanguageSupported) {
            throw new RuntimeException("Ngôn ngữ này không được hỗ trợ cho bài tập này");
        }

        // Tạo submission mới với trạng thái ban đầu
        Instant now = Instant.now();
        SubmissionEntity submission = SubmissionEntity.builder()
                .user(user)
                .problem(problem)
                .language(language)
                .codeContent(request.getCodeContent())
                .isAccepted(false)
                .score(0)
                .statusCode(0) // 0 = PENDING
                .statusRuntime("0 ms")
                .memoryKb(0)
                .displayRuntime("0 ms")
                .totalCorrect(0)
                .totalTestcases(0)
                .statusMemory("0 KB")
                .statusMsg("Đang chờ xử lý...")
                .state("PENDING")
                .isDeleted(false)
                .createdAt(now)
                .updatedAt(now)
                .build();

        submission = submissionRepository.save(submission);
        
        // Flush để đảm bảo submission được lưu vào database trước khi gọi async
        entityManager.flush();
        
        // Lấy ID của submission để truyền vào async method
        Long submissionId = submission.getId();

        // Gọi judge service để chạy code và cập nhật kết quả (async)
        // Đợi transaction commit xong mới gọi judge để tránh lỗi "Submission not found"
        TransactionSynchronizationManager.registerSynchronization(
            new TransactionSynchronization() {
                @Override
                public void afterCommit() {
                    judgeService.judgeSubmission(submissionId);
                }
            }
        );

        return mapToSubmissionDetailResponse(submission);
    }

    @Transactional(readOnly = true)
    public Page<SubmissionResponse> getSubmissions(
            Long userId,
            Long problemId,
            String status,
            Pageable pageable) {
        
        Specification<SubmissionEntity> spec = buildSpecification(userId, problemId, status);
        Page<SubmissionEntity> submissions = submissionRepository.findAll(spec, pageable);
        
        return submissions.map(this::mapToSubmissionResponse);
    }

    @Transactional(readOnly = true)
    public Page<SubmissionResponse> getMySubmissions(Long userId, Pageable pageable) {
        Page<SubmissionEntity> submissions = submissionRepository.findByUserId(userId, pageable);
        return submissions.map(this::mapToSubmissionResponse);
    }

    @Transactional(readOnly = true)
    public SubmissionDetailResponse getSubmissionById(Long id) {
        SubmissionEntity submission = submissionRepository.findByIdAndNotDeleted(id)
                .orElseThrow(() -> new RuntimeException("Không tìm thấy submission"));
        
        return mapToSubmissionDetailResponse(submission);
    }

    private Specification<SubmissionEntity> buildSpecification(
            Long userId,
            Long problemId,
            String status) {
        
        return (root, query, cb) -> {
            List<Predicate> predicates = new ArrayList<>();
            
            // Chỉ lấy submission chưa bị xóa
            predicates.add(cb.equal(root.get("isDeleted"), false));
            
            // Filter theo userId
            if (userId != null) {
                predicates.add(cb.equal(root.get("user").get("id"), userId));
            }
            
            // Filter theo problemId
            if (problemId != null) {
                predicates.add(cb.equal(root.get("problem").get("id"), problemId));
            }
            
            // Filter theo status (isAccepted)
            if (status != null && !status.isEmpty()) {
                if (status.equalsIgnoreCase("ACCEPTED") || status.equalsIgnoreCase("AC")) {
                    predicates.add(cb.equal(root.get("isAccepted"), true));
                } else if (status.equalsIgnoreCase("REJECTED") || status.equalsIgnoreCase("REJECT") || status.equalsIgnoreCase("WA")) {
                    predicates.add(cb.equal(root.get("isAccepted"), false));
                }
            }
            
            return cb.and(predicates.toArray(new Predicate[0]));
        };
    }

    private SubmissionResponse mapToSubmissionResponse(SubmissionEntity entity) {
        return SubmissionResponse.builder()
                .id(entity.getId())
                .problemId(entity.getProblem() != null ? entity.getProblem().getId() : null)
                .problemTitle(entity.getProblem() != null ? entity.getProblem().getTitle() : null)
                .problemCode(entity.getProblem() != null ? entity.getProblem().getCode() : null)
                .languageId(entity.getLanguage() != null ? entity.getLanguage().getId() : null)
                .languageName(entity.getLanguage() != null ? entity.getLanguage().getName() : null)
                .languageCode(entity.getLanguage() != null ? entity.getLanguage().getCode() : null)
                .isAccepted(entity.getIsAccepted())
                .score(entity.getScore())
                .statusMsg(entity.getStatusMsg())
                .statusRuntime(entity.getStatusRuntime())
                .statusMemory(entity.getStatusMemory())
                .totalCorrect(entity.getTotalCorrect())
                .totalTestcases(entity.getTotalTestcases())
                .state(entity.getState())
                .createdAt(entity.getCreatedAt())
                .build();
    }

    private SubmissionDetailResponse mapToSubmissionDetailResponse(SubmissionEntity entity) {
        return SubmissionDetailResponse.builder()
                .id(entity.getId())
                .userId(entity.getUser() != null ? entity.getUser().getId() : null)
                .username(entity.getUser() != null ? entity.getUser().getUsername() : null)
                .problemId(entity.getProblem() != null ? entity.getProblem().getId() : null)
                .problemTitle(entity.getProblem() != null ? entity.getProblem().getTitle() : null)
                .problemCode(entity.getProblem() != null ? entity.getProblem().getCode() : null)
                .languageId(entity.getLanguage() != null ? entity.getLanguage().getId() : null)
                .languageName(entity.getLanguage() != null ? entity.getLanguage().getName() : null)
                .languageCode(entity.getLanguage() != null ? entity.getLanguage().getCode() : null)
                .codeContent(entity.getCodeContent())
                .isAccepted(entity.getIsAccepted())
                .score(entity.getScore())
                .statusCode(entity.getStatusCode())
                .statusRuntime(entity.getStatusRuntime())
                .displayRuntime(entity.getDisplayRuntime())
                .memoryKb(entity.getMemoryKb())
                .statusMemory(entity.getStatusMemory())
                .statusMsg(entity.getStatusMsg())
                .state(entity.getState())
                .totalCorrect(entity.getTotalCorrect())
                .totalTestcases(entity.getTotalTestcases())
                .compileError(entity.getCompileError())
                .fullCompileError(entity.getFullCompileError())
                .createdAt(entity.getCreatedAt())
                .updatedAt(entity.getUpdatedAt())
                .build();
    }
}

