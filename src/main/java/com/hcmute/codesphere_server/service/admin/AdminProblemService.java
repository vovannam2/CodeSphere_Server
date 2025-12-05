package com.hcmute.codesphere_server.service.admin;

import com.hcmute.codesphere_server.model.entity.*;
import com.hcmute.codesphere_server.model.payload.request.CreateProblemRequest;
import com.hcmute.codesphere_server.model.payload.response.ProblemDetailResponse;
import com.hcmute.codesphere_server.repository.common.*;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
        
import java.time.Instant;
import java.util.HashSet;
import java.util.Set;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
public class AdminProblemService {

    private final ProblemRepository problemRepository;
    private final UserRepository userRepository;
    private final CategoryRepository categoryRepository;
    private final TagRepository tagRepository;
    private final LanguageRepository languageRepository;

    @Transactional
    public ProblemDetailResponse createProblem(CreateProblemRequest request, Long authorId) {
        // Kiểm tra code đã tồn tại chưa
        if (problemRepository.existsByCode(request.getCode())) {
            throw new RuntimeException("Problem với code '" + request.getCode() + "' đã tồn tại");
        }

        // Kiểm tra slug đã tồn tại chưa
        if (problemRepository.existsBySlug(request.getSlug())) {
            throw new RuntimeException("Problem với slug '" + request.getSlug() + "' đã tồn tại");
        }

        // Lấy author
        UserEntity author = userRepository.findById(authorId)
                .orElseThrow(() -> new RuntimeException("Không tìm thấy user"));

        // Validate và lấy categories
        Set<CategoryEntity> categories = new HashSet<>();
        if (request.getCategoryIds() != null && !request.getCategoryIds().isEmpty()) {
            for (Long categoryId : request.getCategoryIds()) {
                CategoryEntity category = categoryRepository.findById(categoryId)
                        .orElseThrow(() -> new RuntimeException("Category với ID " + categoryId + " không tồn tại"));
                categories.add(category);
            }
        }

        // Validate và lấy tags (optional)
        Set<TagEntity> tags = new HashSet<>();
        if (request.getTagIds() != null && !request.getTagIds().isEmpty()) {
            for (Long tagId : request.getTagIds()) {
                TagEntity tag = tagRepository.findById(tagId)
                        .orElseThrow(() -> new RuntimeException("Tag với ID " + tagId + " không tồn tại"));
                tags.add(tag);
            }
        }

        // Validate và lấy languages
        Set<LanguageEntity> languages = new HashSet<>();
        if (request.getLanguageIds() != null && !request.getLanguageIds().isEmpty()) {
            for (Long languageId : request.getLanguageIds()) {
                LanguageEntity language = languageRepository.findById(languageId)
                        .orElseThrow(() -> new RuntimeException("Language với ID " + languageId + " không tồn tại"));
                languages.add(language);
            }
        }

        // Validate level
        String level = request.getLevel().toUpperCase();
        if (!level.equals("EASY") && !level.equals("MEDIUM") && !level.equals("HARD")) {
            throw new RuntimeException("Level phải là EASY, MEDIUM hoặc HARD");
        }

        // Tạo problem mới
        Instant now = Instant.now();
        ProblemEntity problem = ProblemEntity.builder()
                .code(request.getCode().toUpperCase())
                .title(request.getTitle())
                .slug(request.getSlug().toLowerCase())
                .content(request.getContent())
                .level(level)
                .sampleInput(request.getSampleInput()) // Dùng để hiển thị ở description frontend
                .sampleOutput(request.getSampleOutput()) // Dùng để hiển thị ở description frontend
                .timeLimitMs(request.getTimeLimitMs() != null ? request.getTimeLimitMs() : 2000)
                .memoryLimitMb(request.getMemoryLimitMb() != null ? request.getMemoryLimitMb() : 256)
                .author(author)
                .status(true)
                .createdAt(now)
                .updatedAt(now)
                .categories(categories)
                .tags(tags)
                .languages(languages)
                .build();

        problem = problemRepository.save(problem);

        // Map sang response
        return mapToProblemDetailResponse(problem);
    }

    @Transactional(readOnly = true)
        public ProblemDetailResponse getProblem(Long id) {
            ProblemEntity problem = problemRepository.findById(id)
                    .orElseThrow(() -> new RuntimeException("Problem với ID " + id + " không tồn tại"));
            return mapToProblemDetailResponse(problem);
        }

    @Transactional
    public ProblemDetailResponse updateProblem(Long id, CreateProblemRequest request) {
        ProblemEntity p = problemRepository.findById(id)
                .orElseThrow(() -> new RuntimeException("Problem với ID " + id + " không tồn tại"));

        // Cập nhật các field cơ bản (chỉ cập nhật khi có giá trị mới)
        if (request.getTitle() != null) p.setTitle(request.getTitle());
        if (request.getSlug() != null) p.setSlug(request.getSlug().toLowerCase());
        if (request.getCode() != null) p.setCode(request.getCode().toUpperCase());
        if (request.getContent() != null) p.setContent(request.getContent());
        if (request.getSampleInput() != null) p.setSampleInput(request.getSampleInput());
        if (request.getSampleOutput() != null) p.setSampleOutput(request.getSampleOutput());
        if (request.getTimeLimitMs() != null) p.setTimeLimitMs(request.getTimeLimitMs());
        if (request.getMemoryLimitMb() != null) p.setMemoryLimitMb(request.getMemoryLimitMb());

        if (request.getLevel() != null) {
            String level = request.getLevel().toUpperCase();
            if (!level.equals("EASY") && !level.equals("MEDIUM") && !level.equals("HARD")) {
                throw new RuntimeException("Level phải là EASY, MEDIUM hoặc HARD");
            }
            p.setLevel(level);
        }

        // Categories (nhiều)
        if (request.getCategoryIds() != null) {
            Set<CategoryEntity> categories = new HashSet<>();
            for (Long categoryId : request.getCategoryIds()) {
                CategoryEntity category = categoryRepository.findById(categoryId)
                        .orElseThrow(() -> new RuntimeException("Category với ID " + categoryId + " không tồn tại"));
                categories.add(category);
            }
            p.setCategories(categories);
        }

        // Tags (nhiều)
        if (request.getTagIds() != null) {
            Set<TagEntity> tags = new HashSet<>();
            for (Long tagId : request.getTagIds()) {
                TagEntity tag = tagRepository.findById(tagId)
                        .orElseThrow(() -> new RuntimeException("Tag với ID " + tagId + " không tồn tại"));
                tags.add(tag);
            }
            p.setTags(tags);
        }

        // Languages (nhiều)
        if (request.getLanguageIds() != null) {
            Set<LanguageEntity> languages = new HashSet<>();
            for (Long languageId : request.getLanguageIds()) {
                LanguageEntity language = languageRepository.findById(languageId)
                        .orElseThrow(() -> new RuntimeException("Language với ID " + languageId + " không tồn tại"));
                languages.add(language);
            }
            p.setLanguages(languages);
        }

        p.setUpdatedAt(Instant.now());
        ProblemEntity saved = problemRepository.save(p);
        return mapToProblemDetailResponse(saved);
    }

    @Transactional
    public void deleteProblem(Long id) {
        if (!problemRepository.existsById(id)) {
            throw new RuntimeException("Problem với ID " + id + " không tồn tại");
        }
        problemRepository.deleteById(id);
    }

    private ProblemDetailResponse mapToProblemDetailResponse(ProblemEntity entity) {
        // sampleInput và sampleOutput trong ProblemEntity chỉ dùng để hiển thị ở description frontend
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
                        .map(cat -> com.hcmute.codesphere_server.model.payload.response.CategoryResponse.builder()
                                .id(cat.getId())
                                .name(cat.getName())
                                .slug(cat.getSlug())
                                .parentId(cat.getParent() != null ? cat.getParent().getId() : null)
                                .parentName(cat.getParent() != null ? cat.getParent().getName() : null)
                                .build())
                        .collect(Collectors.toList()))
                .tags(entity.getTags().stream()
                        .map(tag -> com.hcmute.codesphere_server.model.payload.response.TagResponse.builder()
                                .id(tag.getId())
                                .name(tag.getName())
                                .slug(tag.getSlug())
                                .build())
                        .collect(Collectors.toList()))
                .languages(entity.getLanguages().stream()
                        .map(lang -> com.hcmute.codesphere_server.model.payload.response.LanguageResponse.builder()
                                .id(lang.getId())
                                .code(lang.getCode())
                                .name(lang.getName())
                                .version(lang.getVersion())
                                .build())
                        .collect(Collectors.toList()))
                .build();
    }
}

