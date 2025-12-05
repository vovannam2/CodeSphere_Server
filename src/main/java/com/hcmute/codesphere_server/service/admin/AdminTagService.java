package com.hcmute.codesphere_server.service.admin;

import com.hcmute.codesphere_server.model.entity.TagEntity;
import com.hcmute.codesphere_server.model.enums.TagType;
import com.hcmute.codesphere_server.model.payload.request.CreateTagRequest;
import com.hcmute.codesphere_server.model.payload.response.TagResponse;
import com.hcmute.codesphere_server.repository.common.TagRepository;
import com.hcmute.codesphere_server.repository.common.ProblemRepository;
import com.hcmute.codesphere_server.exception.ResourceConflictException;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
public class AdminTagService {

    private final TagRepository tagRepository;
    private final ProblemRepository problemRepository;

    @Transactional
    public TagResponse createTag(CreateTagRequest request) {
        // Admin tạo tag cho problems, nên type mặc định là PROBLEM
        TagType tagType = TagType.PROBLEM;
        
        // Kiểm tra slug đã tồn tại với type này chưa
        if (tagRepository.existsBySlugAndType(request.getSlug().toLowerCase(), tagType)) {
            throw new RuntimeException("Tag với slug '" + request.getSlug() + "' đã tồn tại cho loại " + tagType);
        }

        // Tạo tag mới
        TagEntity tag = TagEntity.builder()
                .name(request.getName())
                .slug(request.getSlug().toLowerCase()) // Chuyển về lowercase
                .type(tagType) // Tag cho problems
                .build();

        tag = tagRepository.save(tag);

        // Map sang response
        return TagResponse.builder()
                .id(tag.getId())
                .name(tag.getName())
                .slug(tag.getSlug())
                .build();
    }
    
    @Transactional
    public TagResponse updateTag(Long id, CreateTagRequest req) {
        TagEntity e = tagRepository.findById(id)
                .orElseThrow(() -> new RuntimeException("Tag với ID " + id + " không tồn tại"));
        if (req.getName() != null) e.setName(req.getName());
        if (req.getSlug() != null) e.setSlug(req.getSlug());
        TagEntity saved = tagRepository.save(e);
        return mapToTagResponse(saved);
    }

     @Transactional
    public void deleteTag(Long id) {
        TagEntity tag = tagRepository.findById(id)
                .orElseThrow(() -> new RuntimeException("Tag với ID " + id + " không tồn tại"));

        // Nếu còn problem tham chiếu tới tag -> chặn xóa
        long usedCount = problemRepository.countByTags_Id(id);
        if (usedCount > 0) {
            throw new ResourceConflictException("Không thể xóa tag này vì đang được sử dụng bởi " + usedCount + " problem(s). Vui lòng gỡ tag khỏi các problem trước khi xóa.");
        }

        // Xóa tag
        tagRepository.deleteById(id);
    }

    private TagResponse mapToTagResponse(TagEntity e) {
        TagResponse r = new TagResponse();
        r.setId(e.getId());
        r.setName(e.getName());
        r.setSlug(e.getSlug());
        return r;
    }
}

