package com.hcmute.codesphere_server.service.admin;

import com.hcmute.codesphere_server.model.entity.CategoryEntity;
import com.hcmute.codesphere_server.model.payload.request.CreateCategoryRequest;
import com.hcmute.codesphere_server.model.payload.response.CategoryResponse;
import com.hcmute.codesphere_server.repository.common.CategoryRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
public class AdminCategoryService {

    private final CategoryRepository categoryRepository;

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
}

