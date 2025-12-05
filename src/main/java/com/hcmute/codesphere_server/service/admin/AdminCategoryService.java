package com.hcmute.codesphere_server.service.admin;

import com.hcmute.codesphere_server.exception.ResourceConflictException;
import com.hcmute.codesphere_server.model.entity.CategoryEntity;
import com.hcmute.codesphere_server.model.payload.request.CreateCategoryRequest;
import com.hcmute.codesphere_server.model.payload.response.CategoryResponse;
import com.hcmute.codesphere_server.repository.common.CategoryRepository;
import com.hcmute.codesphere_server.repository.common.ProblemRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

@Service
@RequiredArgsConstructor
public class AdminCategoryService {

    private final CategoryRepository categoryRepository;
    private final ProblemRepository problemRepository;

    @Transactional
    public CategoryResponse createCategory(CreateCategoryRequest request) {
        // Kiểm tra slug đã tồn tại chưa
        if (categoryRepository.existsBySlug(request.getSlug())) {
            throw new RuntimeException("Category với slug '" + request.getSlug() + "' đã tồn tại");
        }

        // Kiểm tra parent category nếu có
        CategoryEntity parent = null;
        if (request.getParentId() != null) {
            parent = categoryRepository.findById(request.getParentId())
                    .orElseThrow(() -> new RuntimeException("Parent category không tồn tại"));
        }

        // Tạo category mới
        CategoryEntity category = CategoryEntity.builder()
                .name(request.getName())
                .slug(request.getSlug().toLowerCase()) // Chuyển về lowercase
                .parent(parent)
                .build();

        category = categoryRepository.save(category);

        // Map sang response
        return CategoryResponse.builder()
                .id(category.getId())
                .name(category.getName())
                .slug(category.getSlug())
                .parentId(category.getParent() != null ? category.getParent().getId() : null)
                .parentName(category.getParent() != null ? category.getParent().getName() : null)
                .build();
    }
    @Transactional
    public CategoryResponse updateCategory(Long id, CreateCategoryRequest req) {
        CategoryEntity e = categoryRepository.findById(id)
                .orElseThrow(() -> new RuntimeException("Category với ID " + id + " không tồn tại"));

        // nếu slug thay đổi, kiểm tra trùng
        if (req.getSlug() != null && !req.getSlug().isBlank() && !req.getSlug().equals(e.getSlug())) {
            if (categoryRepository.existsBySlug(req.getSlug())) {
                throw new RuntimeException("Category với slug '" + req.getSlug() + "' đã tồn tại");
            }
            e.setSlug(req.getSlug().toLowerCase());
        }

        if (req.getName() != null) e.setName(req.getName());

        // xử lý parent
        if (req.getParentId() != null) {
            if (req.getParentId().equals(id)) {
                throw new RuntimeException("Không thể đặt chính category làm parent");
            }
            CategoryEntity parent = categoryRepository.findById(req.getParentId())
                    .orElseThrow(() -> new RuntimeException("Parent category không tồn tại"));
            e.setParent(parent);
        } else {
            e.setParent(null);
        }
        CategoryEntity saved = categoryRepository.save(e);
        return CategoryResponse.builder()
                .id(saved.getId())
                .name(saved.getName())
                .slug(saved.getSlug())
                .parentId(saved.getParent() != null ? saved.getParent().getId() : null)
                .parentName(saved.getParent() != null ? saved.getParent().getName() : null)
                .build();
    }
    @Transactional
    public void deleteCategory(Long id) {
        CategoryEntity e = categoryRepository.findById(id)
                .orElseThrow(() -> new RuntimeException("Category với ID " + id + " không tồn tại"));
        // Nếu còn problem tham chiếu tới category -> chặn xóa
        long usedCount = problemRepository.countByCategories_Id(id);
        if (usedCount > 0) {
            throw new ResourceConflictException("Không thể xóa category này vì đang được sử dụng bởi " + usedCount + " problem(s). Vui lòng gỡ category khỏi các problem trước khi xóa.");
        }

        // Nếu có categories con, chặn hoặc chuyển parent = null (ở đây chặn)
        List<CategoryEntity> children = categoryRepository.findByParentId(id);
        if (children != null && !children.isEmpty()) {
            throw new ResourceConflictException("Không thể xóa category này vì còn categories con. Xóa/move các category con trước.");
        }

        categoryRepository.deleteById(id);
    }
}

