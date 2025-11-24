package com.hcmute.codesphere_server.model.payload.response;

import com.fasterxml.jackson.annotation.JsonInclude;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.List;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
@JsonInclude(JsonInclude.Include.NON_NULL)
public class ProblemDetailResponse {
    private Long id;
    private String code;
    private String title;
    private String slug;
    private String content; // Đề bài đầy đủ
    private String level; // EASY/MEDIUM/HARD
    private String sampleInput;
    private String sampleOutput;
    private Integer timeLimitMs;
    private Integer memoryLimitMb;
    private Long authorId;
    private String authorName;
    private List<CategoryResponse> categories;
    private List<TagResponse> tags;
    private List<LanguageResponse> languages;
    // Không cần sampleTestCases - đã có sampleInput và sampleOutput ở trên
}

