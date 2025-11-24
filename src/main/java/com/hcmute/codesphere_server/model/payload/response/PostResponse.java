package com.hcmute.codesphere_server.model.payload.response;

import com.fasterxml.jackson.annotation.JsonInclude;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.Instant;
import java.util.List;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
@JsonInclude(JsonInclude.Include.NON_NULL)
public class PostResponse {
    private Long id;
    private String title;
    private String content;
    private Boolean isAnonymous;
    private Boolean isResolved;
    private Long authorId;
    private String authorName;
    private String authorAvatar;
    private Long totalVotes;
    private Long upvotes;
    private Long downvotes;
    private Long commentCount;
    private Integer userVote; // null, 1 (upvote), -1 (downvote)
    private List<TagResponse> tags;
    private List<CategoryResponse> categories;
    private Instant createdAt;
    private Instant updatedAt;
}

