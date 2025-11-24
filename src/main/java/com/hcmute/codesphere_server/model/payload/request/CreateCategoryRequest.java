package com.hcmute.codesphere_server.model.payload.request;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;
import lombok.Data;

@Data
public class CreateCategoryRequest {
    
    @NotBlank(message = "Tên category không được để trống")
    @Size(max = 120, message = "Tên category không được vượt quá 120 ký tự")
    private String name;
    
    @NotBlank(message = "Slug không được để trống")
    @Size(max = 150, message = "Slug không được vượt quá 150 ký tự")
    private String slug;
    
    private Long parentId; // Optional: null nếu là root category
}

