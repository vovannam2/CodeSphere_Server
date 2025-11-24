package com.hcmute.codesphere_server.service.common;

import com.hcmute.codesphere_server.model.entity.*;
import com.hcmute.codesphere_server.model.payload.request.CreatePostRequest;
import com.hcmute.codesphere_server.model.payload.request.UpdatePostRequest;
import com.hcmute.codesphere_server.model.payload.request.VoteRequest;
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

import java.time.Instant;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
public class PostService {

    private final PostRepository postRepository;
    private final PostLikeRepository postLikeRepository;
    private final CommentRepository commentRepository;
    private final CommentLikeRepository commentLikeRepository;
    private final UserRepository userRepository;
    private final TagRepository tagRepository;
    private final CategoryRepository categoryRepository;
    private final NotificationService notificationService;

    @Transactional
    public PostDetailResponse createPost(CreatePostRequest request, Long userId) {
        UserEntity author = userRepository.findById(userId)
                .orElseThrow(() -> new RuntimeException("Không tìm thấy user"));

        PostEntity post = PostEntity.builder()
                .author(author)
                .title(request.getTitle())
                .content(request.getContent())
                .isAnonymous(request.getIsAnonymous() != null ? request.getIsAnonymous() : false)
                .isBlocked(false)
                .isResolved(false)
                .createdAt(Instant.now())
                .updatedAt(Instant.now())
                .tags(new HashSet<>())
                .categories(new HashSet<>())
                .build();

        // Thêm tags nếu có
        if (request.getTagIds() != null && !request.getTagIds().isEmpty()) {
            Set<TagEntity> tags = request.getTagIds().stream()
                    .map(tagId -> tagRepository.findById(tagId)
                            .orElseThrow(() -> new RuntimeException("Không tìm thấy tag với ID: " + tagId)))
                    .collect(Collectors.toSet());
            post.setTags(tags);
        }

        // Thêm categories nếu có
        if (request.getCategoryIds() != null && !request.getCategoryIds().isEmpty()) {
            Set<CategoryEntity> categories = request.getCategoryIds().stream()
                    .map(categoryId -> categoryRepository.findById(categoryId)
                            .orElseThrow(() -> new RuntimeException("Không tìm thấy category với ID: " + categoryId)))
                    .collect(Collectors.toSet());
            post.setCategories(categories);
        }

        post = postRepository.save(post);
        return mapToPostDetailResponse(post, userId);
    }

    @Transactional(readOnly = true)
    public Page<PostResponse> getPosts(Long authorId, String tagSlug, String categorySlug, 
                                       Boolean isResolved, Long userId, Pageable pageable) {
        Specification<PostEntity> spec = buildSpecification(authorId, tagSlug, categorySlug, isResolved);
        Page<PostEntity> posts = postRepository.findAll(spec, pageable);
        
        return posts.map(post -> mapToPostResponse(post, userId));
    }

    @Transactional(readOnly = true)
    public PostDetailResponse getPostById(Long postId, Long userId) {
        PostEntity post = postRepository.findByIdAndNotBlocked(postId)
                .orElseThrow(() -> new RuntimeException("Không tìm thấy bài thảo luận"));
        
        return mapToPostDetailResponse(post, userId);
    }

    @Transactional
    public PostDetailResponse updatePost(Long postId, UpdatePostRequest request, Long userId) {
        PostEntity post = postRepository.findByIdAndNotBlocked(postId)
                .orElseThrow(() -> new RuntimeException("Không tìm thấy bài thảo luận"));

        // Kiểm tra quyền sở hữu
        if (!post.getAuthor().getId().equals(userId)) {
            throw new RuntimeException("Bạn không có quyền chỉnh sửa bài thảo luận này");
        }

        if (request.getTitle() != null) {
            post.setTitle(request.getTitle());
        }
        if (request.getContent() != null) {
            post.setContent(request.getContent());
        }

        // Cập nhật tags
        if (request.getTagIds() != null) {
            Set<TagEntity> tags = request.getTagIds().stream()
                    .map(tagId -> tagRepository.findById(tagId)
                            .orElseThrow(() -> new RuntimeException("Không tìm thấy tag với ID: " + tagId)))
                    .collect(Collectors.toSet());
            post.setTags(tags);
        }

        // Cập nhật categories
        if (request.getCategoryIds() != null) {
            Set<CategoryEntity> categories = request.getCategoryIds().stream()
                    .map(categoryId -> categoryRepository.findById(categoryId)
                            .orElseThrow(() -> new RuntimeException("Không tìm thấy category với ID: " + categoryId)))
                    .collect(Collectors.toSet());
            post.setCategories(categories);
        }

        post.setUpdatedAt(Instant.now());
        post = postRepository.save(post);
        return mapToPostDetailResponse(post, userId);
    }

    @Transactional
    public void deletePost(Long postId, Long userId) {
        PostEntity post = postRepository.findByIdAndNotBlocked(postId)
                .orElseThrow(() -> new RuntimeException("Không tìm thấy bài thảo luận"));

        // Kiểm tra quyền sở hữu
        if (!post.getAuthor().getId().equals(userId)) {
            throw new RuntimeException("Bạn không có quyền xóa bài thảo luận này");
        }

        post.setIsBlocked(true);
        postRepository.save(post);
    }

    @Transactional
    public VoteResponse toggleVote(Long postId, VoteRequest request, Long userId) {
        PostEntity post = postRepository.findByIdAndNotBlocked(postId)
                .orElseThrow(() -> new RuntimeException("Không tìm thấy bài thảo luận"));

        UserEntity user = userRepository.findById(userId)
                .orElseThrow(() -> new RuntimeException("Không tìm thấy user"));

        var existingVote = postLikeRepository.findByPostIdAndUserId(postId, userId);

        Integer voteValue = request.getVote();
        if (voteValue == null || (voteValue != 1 && voteValue != -1 && voteValue != 0)) {
            throw new RuntimeException("Vote phải là 1 (upvote), -1 (downvote), hoặc 0 (remove)");
        }

        if (existingVote.isPresent()) {
            PostLikeEntity vote = existingVote.get();
            if (voteValue == 0) {
                // Xóa vote
                postLikeRepository.delete(vote);
            } else if (vote.getVote().equals(voteValue)) {
                // Đã vote cùng loại -> xóa vote
                postLikeRepository.delete(vote);
                voteValue = 0;
            } else {
                // Đổi vote
                vote.setVote(voteValue);
                postLikeRepository.save(vote);
            }
        } else {
            if (voteValue != 0) {
                // Tạo vote mới
                PostLikeEntity newVote = PostLikeEntity.builder()
                        .post(post)
                        .user(user)
                        .vote(voteValue)
                        .createdAt(Instant.now())
                        .build();
                postLikeRepository.save(newVote);
            }
        }

        // Tính lại tổng votes
        Long totalVotes = postLikeRepository.getTotalVotesByPostId(postId);
        Long upvotes = postLikeRepository.countUpvotesByPostId(postId);
        Long downvotes = postLikeRepository.countDownvotesByPostId(postId);

        // Gửi notification nếu upvote (không gửi cho chính mình)
        if (voteValue == 1 && !post.getAuthor().getId().equals(userId)) {
            try {
                notificationService.notifyPostLike(
                        post.getAuthor().getId(),
                        userId,
                        user.getUsername(),
                        postId
                );
            } catch (Exception e) {
                // Log error nhưng không throw để không ảnh hưởng đến vote
            }
        }

        return VoteResponse.builder()
                .id(postId)
                .vote(voteValue)
                .totalVotes(totalVotes)
                .upvotes(upvotes)
                .downvotes(downvotes)
                .message(voteValue == 0 ? "Đã bỏ vote" : (voteValue == 1 ? "Đã upvote" : "Đã downvote"))
                .build();
    }

    @Transactional
    public PostDetailResponse markAsResolved(Long postId, Long userId) {
        PostEntity post = postRepository.findByIdAndNotBlocked(postId)
                .orElseThrow(() -> new RuntimeException("Không tìm thấy bài thảo luận"));

        // Chỉ author mới có thể đánh dấu đã giải quyết
        if (!post.getAuthor().getId().equals(userId)) {
            throw new RuntimeException("Chỉ tác giả mới có thể đánh dấu đã giải quyết");
        }

        post.setIsResolved(!post.getIsResolved());
        post.setUpdatedAt(Instant.now());
        post = postRepository.save(post);
        return mapToPostDetailResponse(post, userId);
    }

    private Specification<PostEntity> buildSpecification(Long authorId, String tagSlug, 
                                                         String categorySlug, Boolean isResolved) {
        return (root, query, cb) -> {
            List<Predicate> predicates = new ArrayList<>();
            
            // Chỉ lấy posts chưa bị block
            predicates.add(cb.equal(root.get("isBlocked"), false));
            
            if (authorId != null) {
                predicates.add(cb.equal(root.get("author").get("id"), authorId));
            }
            
            if (tagSlug != null && !tagSlug.isEmpty()) {
                Join<PostEntity, TagEntity> tagJoin = root.join("tags");
                predicates.add(cb.equal(tagJoin.get("slug"), tagSlug));
            }
            
            if (categorySlug != null && !categorySlug.isEmpty()) {
                Join<PostEntity, CategoryEntity> categoryJoin = root.join("categories");
                predicates.add(cb.equal(categoryJoin.get("slug"), categorySlug));
            }
            
            if (isResolved != null) {
                predicates.add(cb.equal(root.get("isResolved"), isResolved));
            }
            
            return cb.and(predicates.toArray(new Predicate[0]));
        };
    }

    private PostResponse mapToPostResponse(PostEntity entity, Long userId) {
        Long totalVotes = postLikeRepository.getTotalVotesByPostId(entity.getId());
        Long upvotes = postLikeRepository.countUpvotesByPostId(entity.getId());
        Long downvotes = postLikeRepository.countDownvotesByPostId(entity.getId());
        Long commentCount = commentRepository.countByPostId(entity.getId());
        
        Integer userVote = null;
        if (userId != null) {
            var userVoteOpt = postLikeRepository.findByPostIdAndUserId(entity.getId(), userId);
            if (userVoteOpt.isPresent()) {
                userVote = userVoteOpt.get().getVote();
            }
        }

        return PostResponse.builder()
                .id(entity.getId())
                .title(entity.getTitle())
                .content(entity.getContent())
                .isAnonymous(entity.getIsAnonymous())
                .isResolved(entity.getIsResolved())
                .authorId(entity.getAuthor() != null ? entity.getAuthor().getId() : null)
                .authorName(entity.getIsAnonymous() ? null : (entity.getAuthor() != null ? entity.getAuthor().getUsername() : null))
                .authorAvatar(entity.getIsAnonymous() ? null : (entity.getAuthor() != null ? entity.getAuthor().getAvatar() : null))
                .totalVotes(totalVotes)
                .upvotes(upvotes)
                .downvotes(downvotes)
                .commentCount(commentCount)
                .userVote(userVote)
                .tags(entity.getTags().stream()
                        .map(this::mapToTagResponse)
                        .collect(Collectors.toList()))
                .categories(entity.getCategories().stream()
                        .map(this::mapToCategoryResponse)
                        .collect(Collectors.toList()))
                .createdAt(entity.getCreatedAt())
                .updatedAt(entity.getUpdatedAt())
                .build();
    }

    private PostDetailResponse mapToPostDetailResponse(PostEntity entity, Long userId) {
        PostResponse baseResponse = mapToPostResponse(entity, userId);
        
        // Lấy comments
        List<CommentResponse> comments = commentRepository.findTopLevelCommentsByPostId(
                entity.getId(), 
                org.springframework.data.domain.Pageable.unpaged())
                .getContent()
                .stream()
                .map(comment -> mapToCommentResponse(comment, userId))
                .collect(Collectors.toList());

        return PostDetailResponse.builder()
                .id(baseResponse.getId())
                .title(baseResponse.getTitle())
                .content(baseResponse.getContent())
                .isAnonymous(baseResponse.getIsAnonymous())
                .isResolved(baseResponse.getIsResolved())
                .authorId(baseResponse.getAuthorId())
                .authorName(baseResponse.getAuthorName())
                .authorAvatar(baseResponse.getAuthorAvatar())
                .totalVotes(baseResponse.getTotalVotes())
                .upvotes(baseResponse.getUpvotes())
                .downvotes(baseResponse.getDownvotes())
                .commentCount(baseResponse.getCommentCount())
                .userVote(baseResponse.getUserVote())
                .tags(baseResponse.getTags())
                .categories(baseResponse.getCategories())
                .comments(comments)
                .createdAt(baseResponse.getCreatedAt())
                .updatedAt(baseResponse.getUpdatedAt())
                .build();
    }

    private CommentResponse mapToCommentResponse(CommentEntity entity, Long userId) {
        Long totalVotes = commentLikeRepository.getTotalVotesByCommentId(entity.getId());
        Long upvotes = commentLikeRepository.countUpvotesByCommentId(entity.getId());
        Long downvotes = commentLikeRepository.countDownvotesByCommentId(entity.getId());
        
        // Đếm replies
        Long replyCount = (long) commentRepository.findRepliesByParentId(entity.getId()).size();
        
        Integer userVote = null;
        if (userId != null) {
            var userVoteOpt = commentLikeRepository.findByCommentIdAndUserId(entity.getId(), userId);
            if (userVoteOpt.isPresent()) {
                userVote = userVoteOpt.get().getVote();
            }
        }

        // Lấy replies
        List<CommentResponse> replies = commentRepository.findRepliesByParentId(entity.getId())
                .stream()
                .map(reply -> mapToCommentResponse(reply, userId))
                .collect(Collectors.toList());

        return CommentResponse.builder()
                .id(entity.getId())
                .content(entity.getContent())
                .isAccepted(entity.getIsAccepted())
                .authorId(entity.getAuthor() != null ? entity.getAuthor().getId() : null)
                .authorName(entity.getAuthor() != null ? entity.getAuthor().getUsername() : null)
                .authorAvatar(entity.getAuthor() != null ? entity.getAuthor().getAvatar() : null)
                .parentCommentId(entity.getParent() != null ? entity.getParent().getId() : null)
                .totalVotes(totalVotes)
                .upvotes(upvotes)
                .downvotes(downvotes)
                .userVote(userVote)
                .replyCount(replyCount)
                .replies(replies)
                .createdAt(entity.getCreatedAt())
                .updatedAt(entity.getUpdatedAt())
                .build();
    }

    private TagResponse mapToTagResponse(TagEntity entity) {
        return TagResponse.builder()
                .id(entity.getId())
                .name(entity.getName())
                .slug(entity.getSlug())
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
}

