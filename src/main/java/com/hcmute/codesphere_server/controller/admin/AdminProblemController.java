package com.hcmute.codesphere_server.controller.admin;

import com.hcmute.codesphere_server.model.payload.request.CreateProblemRequest;
import com.hcmute.codesphere_server.model.payload.response.DataResponse;
import com.hcmute.codesphere_server.model.payload.response.ProblemDetailResponse;
import com.hcmute.codesphere_server.security.authentication.UserPrinciple;
import com.hcmute.codesphere_server.service.admin.AdminProblemService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.*;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import com.hcmute.codesphere_server.model.payload.response.ProblemResponse;
import com.hcmute.codesphere_server.model.entity.ProblemEntity;
import com.hcmute.codesphere_server.repository.common.ProblemRepository;

@RestController
@RequestMapping("${base.url}/admin/problems")
@RequiredArgsConstructor
public class AdminProblemController {

    private final AdminProblemService adminProblemService;
    private final ProblemRepository problemRepository;

    @GetMapping
    public ResponseEntity<DataResponse<Page<ProblemResponse>>> getAllProblems(
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "20") int size,
            Authentication authentication) {
        
        if (authentication == null || !authentication.isAuthenticated()) {
            return ResponseEntity.status(401)
                    .body(DataResponse.error("Unauthorized"));
        }

        UserPrinciple userPrinciple = (UserPrinciple) authentication.getPrincipal();
        boolean isAdmin = userPrinciple.getAuthorities().stream()
                .anyMatch(auth -> auth.getAuthority().equals("ROLE_ADMIN"));
        
        if (!isAdmin) {
            return ResponseEntity.status(403)
                    .body(DataResponse.error("Forbidden"));
        }

        try {
            Pageable pageable = PageRequest.of(page, size, Sort.by("createdAt").descending());
            Page<ProblemEntity> problems = problemRepository.findAll(pageable);
            // Map to ProblemResponse với đầy đủ thông tin
            Page<ProblemResponse> response = problems.map(p -> {
                ProblemResponse.ProblemResponseBuilder builder = ProblemResponse.builder()
                        .id(p.getId())
                        .code(p.getCode())
                        .title(p.getTitle())
                        .level(p.getLevel())
                        .timeLimitMs(p.getTimeLimitMs())
                        .authorId(p.getAuthor() != null ? p.getAuthor().getId() : null)
                        .authorName(p.getAuthor() != null ? p.getAuthor().getUsername() : null);
                
                // Map categories
                if (p.getCategories() != null && !p.getCategories().isEmpty()) {
                    builder.categories(p.getCategories().stream()
                            .map(cat -> com.hcmute.codesphere_server.model.payload.response.CategoryResponse.builder()
                                    .id(cat.getId())
                                    .name(cat.getName())
                                    .slug(cat.getSlug())
                                    .parentId(cat.getParent() != null ? cat.getParent().getId() : null)
                                    .build())
                            .collect(java.util.stream.Collectors.toList()));
                }
                
                // Map languages
                if (p.getLanguages() != null && !p.getLanguages().isEmpty()) {
                    builder.languages(p.getLanguages().stream()
                            .map(lang -> com.hcmute.codesphere_server.model.payload.response.LanguageResponse.builder()
                                    .id(lang.getId())
                                    .code(lang.getCode())
                                    .name(lang.getName())
                                    .version(lang.getVersion())
                                    .build())
                            .collect(java.util.stream.Collectors.toList()));
                }
                
                // Thêm isPublic và isContest cho admin
                builder.isPublic(p.getIsPublic())
                       .isContest(p.getIsContest());
                
                return builder.build();
            });
            return ResponseEntity.ok(DataResponse.success(response));
        } catch (Exception e) {
            return ResponseEntity.badRequest()
                    .body(DataResponse.error(e.getMessage()));
        }
    }

    @PostMapping
    public ResponseEntity<DataResponse<ProblemDetailResponse>> createProblem(
            @Valid @RequestBody CreateProblemRequest request,
            Authentication authentication) {
        
        // Kiểm tra authentication
        if (authentication == null || !authentication.isAuthenticated()) {
            return ResponseEntity.status(401)
                    .body(DataResponse.error("Unauthorized - Token không hợp lệ hoặc thiếu"));
        }

        // Kiểm tra quyền admin
        UserPrinciple userPrinciple = (UserPrinciple) authentication.getPrincipal();
        boolean isAdmin = userPrinciple.getAuthorities().stream()
                .anyMatch(auth -> auth.getAuthority().equals("ROLE_ADMIN"));
        
        if (!isAdmin) {
            return ResponseEntity.status(403)
                    .body(DataResponse.error("Forbidden - Chỉ admin mới có quyền thực hiện thao tác này"));
        }

        try {
            // Lấy userId từ token
            Long authorId = Long.parseLong(userPrinciple.getUserId());
            
            ProblemDetailResponse response = adminProblemService.createProblem(request, authorId);
            return ResponseEntity.ok(DataResponse.success(response));
        } catch (RuntimeException e) {
            return ResponseEntity.badRequest()
                    .body(DataResponse.error(e.getMessage()));
        }
    }

    @GetMapping("/{id}")
    public ResponseEntity<DataResponse<ProblemDetailResponse>> getProblem(
            @PathVariable Long id,
            Authentication authentication) {
        
        if (authentication == null || !authentication.isAuthenticated()) {
            return ResponseEntity.status(401)
                    .body(DataResponse.error("Unauthorized"));
        }

        UserPrinciple userPrinciple = (UserPrinciple) authentication.getPrincipal();
        boolean isAdmin = userPrinciple.getAuthorities().stream()
                .anyMatch(auth -> auth.getAuthority().equals("ROLE_ADMIN"));
        
        if (!isAdmin) {
            return ResponseEntity.status(403)
                    .body(DataResponse.error("Forbidden"));
        }

        try {
            ProblemDetailResponse response = adminProblemService.getProblemById(id);
            return ResponseEntity.ok(DataResponse.success(response));
        } catch (RuntimeException e) {
            return ResponseEntity.badRequest()
                    .body(DataResponse.error(e.getMessage()));
        }
    }

    @PutMapping("/{id}")
    public ResponseEntity<DataResponse<ProblemDetailResponse>> updateProblem(
            @PathVariable Long id,
            @Valid @RequestBody CreateProblemRequest request,
            Authentication authentication) {
        
        if (authentication == null || !authentication.isAuthenticated()) {
            return ResponseEntity.status(401)
                    .body(DataResponse.error("Unauthorized"));
        }

        UserPrinciple userPrinciple = (UserPrinciple) authentication.getPrincipal();
        boolean isAdmin = userPrinciple.getAuthorities().stream()
                .anyMatch(auth -> auth.getAuthority().equals("ROLE_ADMIN"));
        
        if (!isAdmin) {
            return ResponseEntity.status(403)
                    .body(DataResponse.error("Forbidden"));
        }

        try {
            ProblemDetailResponse response = adminProblemService.updateProblem(id, request);
            return ResponseEntity.ok(DataResponse.success(response));
        } catch (RuntimeException e) {
            return ResponseEntity.badRequest()
                    .body(DataResponse.error(e.getMessage()));
        }
    }
}

