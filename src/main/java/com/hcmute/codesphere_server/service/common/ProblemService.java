package com.hcmute.codesphere_server.service.common;

import com.hcmute.codesphere_server.model.entity.*;
import com.hcmute.codesphere_server.model.payload.response.*;
import com.hcmute.codesphere_server.repository.common.*;
import jakarta.persistence.criteria.Join;
import jakarta.persistence.criteria.Predicate;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.domain.Specification;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.ArrayList;
import java.util.List;
import java.util.Set;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
public class ProblemService {

    private final ProblemRepository problemRepository;
    private final TestCaseRepository testCaseRepository;
    private final ProblemBookmarkService problemBookmarkService;

    @Transactional(readOnly = true)
    public Page<ProblemResponse> getProblems(
            String level,
            String categorySlug,
            String tagSlug,
            String languageCode,
            String bookmarkStatus, // "bookmarked", "not_bookmarked", "all" hoặc null
            String status, // "NOT_ATTEMPTED", "ATTEMPTED_NOT_COMPLETED", "COMPLETED", "all" hoặc null
            Long userId, // User ID để filter và set trạng thái
            Pageable pageable) {
        
        Specification<ProblemEntity> spec = buildSpecification(
                level, categorySlug, tagSlug, languageCode, 
                bookmarkStatus, status, userId);
        
        Page<ProblemEntity> problems = problemRepository.findAll(spec, pageable);
        
        // Lấy thông tin bookmark và completion status nếu có userId
        Set<Long> bookmarkedProblemIds = null;
        Set<Long> solvedProblemIds = null;
        Set<Long> attemptedProblemIds = null;
        
        if (userId != null) {
            bookmarkedProblemIds = problemBookmarkService.getBookmarkedProblemIds(userId);
            solvedProblemIds = problemBookmarkService.getSolvedProblemIds(userId);
            attemptedProblemIds = problemBookmarkService.getAttemptedProblemIds(userId);
        }
        
        final Set<Long> finalBookmarkedIds = bookmarkedProblemIds;
        final Set<Long> finalSolvedIds = solvedProblemIds;
        final Set<Long> finalAttemptedIds = attemptedProblemIds;
        
        // Map to response và filter theo bookmark status và completion status trong memory
        List<ProblemResponse> allProblems = problems.getContent().stream()
                .map(entity -> mapToProblemResponse(
                        entity, 
                        finalBookmarkedIds, 
                        finalSolvedIds, 
                        finalAttemptedIds))
                .collect(Collectors.toList());
        
        // Filter theo bookmark status và status
        List<ProblemResponse> filteredContent = allProblems.stream()
                .filter(problem -> {
                    // Filter theo bookmark status
                    if (userId != null && bookmarkStatus != null && !bookmarkStatus.isEmpty() && !"all".equalsIgnoreCase(bookmarkStatus)) {
                        if ("bookmarked".equalsIgnoreCase(bookmarkStatus)) {
                            if (!Boolean.TRUE.equals(problem.getIsBookmarked())) {
                                return false;
                            }
                        } else if ("not_bookmarked".equalsIgnoreCase(bookmarkStatus)) {
                            if (Boolean.TRUE.equals(problem.getIsBookmarked())) {
                                return false;
                            }
                        }
                    }
                    
                    // Filter theo status (trạng thái làm bài)
                    if (userId != null && status != null && !status.isEmpty() && !"all".equalsIgnoreCase(status)) {
                        ProblemResponse.ProblemStatus problemStatus = problem.getStatus();
                        if (problemStatus == null) {
                            return false;
                        }
                        
                        if ("COMPLETED".equalsIgnoreCase(status)) {
                            if (problemStatus != ProblemResponse.ProblemStatus.COMPLETED) {
                                return false;
                            }
                        } else if ("ATTEMPTED_NOT_COMPLETED".equalsIgnoreCase(status)) {
                            if (problemStatus != ProblemResponse.ProblemStatus.ATTEMPTED_NOT_COMPLETED) {
                                return false;
                            }
                        } else if ("NOT_ATTEMPTED".equalsIgnoreCase(status)) {
                            if (problemStatus != ProblemResponse.ProblemStatus.NOT_ATTEMPTED) {
                                return false;
                            }
                        }
                    }
                    
                    return true;
                })
                .collect(Collectors.toList());
        
        // Tạo Page mới với filtered content
        return new org.springframework.data.domain.PageImpl<>(
                filteredContent, 
                pageable, 
                filteredContent.size());
    }

    @Transactional(readOnly = true)
    public ProblemDetailResponse getProblemById(Long id) {
        ProblemEntity problem = problemRepository.findByIdAndStatusTrue(id)
                .orElseThrow(() -> new RuntimeException("Không tìm thấy bài tập"));
        
        return mapToProblemDetailResponse(problem);
    }

    @Transactional(readOnly = true)
    public List<TestCaseResponse> getSampleTestCases(Long problemId) {
        // Kiểm tra problem tồn tại
        ProblemEntity problem = problemRepository.findByIdAndStatusTrue(problemId)
                .orElseThrow(() -> new RuntimeException("Không tìm thấy bài tập"));
        
        // Lấy sample testcases (isSample = true, isHidden = false)
        List<TestCaseEntity> testCases = testCaseRepository.findAllTestCasesByProblemId(problemId)
                .stream()
                .filter(tc -> tc.getIsSample() != null && tc.getIsSample() && 
                             tc.getIsHidden() != null && !tc.getIsHidden())
                .collect(Collectors.toList());
        
        return testCases.stream()
                .map(this::mapToTestCaseResponse)
                .collect(Collectors.toList());
    }

    @Transactional(readOnly = true)
    public List<TestCaseResponse> getTestCases(Long problemId) {
        // Kiểm tra problem tồn tại
        problemRepository.findByIdAndStatusTrue(problemId)
                .orElseThrow(() -> new RuntimeException("Không tìm thấy bài tập"));
        
        // Chỉ admin mới gọi method này - lấy tất cả test cases (bao gồm hidden)
        List<TestCaseEntity> testCases = testCaseRepository.findAllTestCasesByProblemId(problemId);
        
        return testCases.stream()
                .map(this::mapToTestCaseResponse)
                .collect(Collectors.toList());
    }

    private Specification<ProblemEntity> buildSpecification(
            String level,
            String categorySlug,
            String tagSlug,
            String languageCode,
            String bookmarkStatus,
            String status,
            Long userId) {
        
        return (root, query, cb) -> {
            List<Predicate> predicates = new ArrayList<>();
            
            // Chỉ lấy bài tập active
            predicates.add(cb.equal(root.get("status"), true));
            
            // Filter theo level
            if (level != null && !level.isEmpty()) {
                predicates.add(cb.equal(root.get("level"), level.toUpperCase()));
            }
            
            // Filter theo category
            if (categorySlug != null && !categorySlug.isEmpty()) {
                Join<ProblemEntity, CategoryEntity> categoryJoin = root.join("categories");
                predicates.add(cb.equal(categoryJoin.get("slug"), categorySlug));
            }
            
            // Filter theo tag
            if (tagSlug != null && !tagSlug.isEmpty()) {
                Join<ProblemEntity, TagEntity> tagJoin = root.join("tags");
                predicates.add(cb.equal(tagJoin.get("slug"), tagSlug));
            }
            
            // Filter theo language
            if (languageCode != null && !languageCode.isEmpty()) {
                Join<ProblemEntity, LanguageEntity> languageJoin = root.join("languages");
                predicates.add(cb.equal(languageJoin.get("code"), languageCode));
            }
            
            // Note: Filter theo bookmark status và completion status sẽ được xử lý trong memory
            // sau khi lấy dữ liệu từ database để tránh phức tạp với subquery
            
            return cb.and(predicates.toArray(new Predicate[0]));
        };
    }

    private ProblemResponse mapToProblemResponse(ProblemEntity entity) {
        return mapToProblemResponse(entity, null, null, null);
    }

    private ProblemResponse mapToProblemResponse(
            ProblemEntity entity,
            Set<Long> bookmarkedProblemIds,
            Set<Long> solvedProblemIds,
            Set<Long> attemptedProblemIds) {
        
        Boolean isBookmarked = null;
        ProblemResponse.ProblemStatus status = null;
        
        // Xác định isBookmarked
        if (bookmarkedProblemIds != null) {
            isBookmarked = bookmarkedProblemIds.contains(entity.getId());
        }
        
        // Xác định status theo thứ tự ưu tiên:
        // 1. COMPLETED: có trong solvedProblemIds (đã giải đúng)
        // 2. ATTEMPTED_NOT_COMPLETED: có trong attemptedProblemIds nhưng không có trong solvedProblemIds
        // 3. NOT_ATTEMPTED: không có trong cả hai
        if (solvedProblemIds != null && solvedProblemIds.contains(entity.getId())) {
            status = ProblemResponse.ProblemStatus.COMPLETED;
        } else if (attemptedProblemIds != null && attemptedProblemIds.contains(entity.getId())) {
            status = ProblemResponse.ProblemStatus.ATTEMPTED_NOT_COMPLETED;
        } else {
            status = ProblemResponse.ProblemStatus.NOT_ATTEMPTED;
        }
        
        return ProblemResponse.builder()
                .id(entity.getId())
                .code(entity.getCode())
                .title(entity.getTitle())
                .slug(entity.getSlug())
                .level(entity.getLevel())
                .timeLimitMs(entity.getTimeLimitMs())
                .memoryLimitMb(entity.getMemoryLimitMb())
                .authorId(entity.getAuthor() != null ? entity.getAuthor().getId() : null)
                .authorName(entity.getAuthor() != null ? entity.getAuthor().getUsername() : null)
                .categories(entity.getCategories().stream()
                        .map(this::mapToCategoryResponse)
                        .collect(Collectors.toList()))
                .tags(entity.getTags().stream()
                        .map(this::mapToTagResponse)
                        .collect(Collectors.toList()))
                .languages(entity.getLanguages().stream()
                        .map(this::mapToLanguageResponse)
                        .collect(Collectors.toList()))
                .isBookmarked(isBookmarked)
                .status(status)
                .build();
    }

    private ProblemDetailResponse mapToProblemDetailResponse(ProblemEntity entity) {
        // sampleInput và sampleOutput trong ProblemEntity chỉ dùng để hiển thị ở description frontend
        // Các testcases thực tế được lưu trong TestCaseEntity
        
        return ProblemDetailResponse.builder()
                .id(entity.getId())
                .code(entity.getCode())
                .title(entity.getTitle())
                .slug(entity.getSlug())
                .content(entity.getContent())
                .level(entity.getLevel())
                .sampleInput(entity.getSampleInput())
                .sampleOutput(entity.getSampleOutput())
                .timeLimitMs(entity.getTimeLimitMs())
                .memoryLimitMb(entity.getMemoryLimitMb())
                .authorId(entity.getAuthor() != null ? entity.getAuthor().getId() : null)
                .authorName(entity.getAuthor() != null ? entity.getAuthor().getUsername() : null)
                .categories(entity.getCategories().stream()
                        .map(this::mapToCategoryResponse)
                        .collect(Collectors.toList()))
                .tags(entity.getTags().stream()
                        .map(this::mapToTagResponse)
                        .collect(Collectors.toList()))
                .languages(entity.getLanguages().stream()
                        .map(this::mapToLanguageResponse)
                        .collect(Collectors.toList()))
                .build();
    }

    private TestCaseResponse mapToTestCaseResponse(TestCaseEntity entity) {
        return TestCaseResponse.builder()
                .id(entity.getId())
                .input(entity.getInput())
                .expectedOutput(entity.getExpectedOutput())
                .isSample(entity.getIsSample())
                .isHidden(entity.getIsHidden())
                .weight(entity.getWeight())
                .build();
    }

    private CategoryResponse mapToCategoryResponse(CategoryEntity entity) {
        return CategoryResponse.builder()
                .id(entity.getId())
                .name(entity.getName())
                .slug(entity.getSlug())
                .parentId(entity.getParent() != null ? entity.getParent().getId() : null)
                .parentName(entity.getParent() != null ? entity.getParent().getName() : null)
                .build();
    }

    private TagResponse mapToTagResponse(TagEntity entity) {
        return TagResponse.builder()
                .id(entity.getId())
                .name(entity.getName())
                .slug(entity.getSlug())
                .build();
    }

    private LanguageResponse mapToLanguageResponse(LanguageEntity entity) {
        return LanguageResponse.builder()
                .id(entity.getId())
                .code(entity.getCode())
                .name(entity.getName())
                .version(entity.getVersion())
                .build();
    }
}

