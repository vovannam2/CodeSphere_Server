package com.hcmute.codesphere_server.model.entity;

import jakarta.persistence.*;
import lombok.Data;
import lombok.Builder;
import lombok.NoArgsConstructor;
import lombok.AllArgsConstructor;
import java.time.Instant;

@Entity
@Table(name = "post_likes", uniqueConstraints = {
	@UniqueConstraint(columnNames = {"post_id", "user_id"})
})
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class PostLikeEntity {

	@EmbeddedId
	private PostLikeKey id = new PostLikeKey();

	@ManyToOne(fetch = FetchType.LAZY)
	@MapsId("postId")
	@JoinColumn(name = "post_id")
	private PostEntity post;

	@ManyToOne(fetch = FetchType.LAZY)
	@MapsId("userId")
	@JoinColumn(name = "user_id")
	private UserEntity user;

	@Column(nullable = false)
	private Integer vote; // +1/-1

	@Column(nullable = false)
	private Instant createdAt = Instant.now();

	public PostLikeKey getId() { return id; }
	public void setId(PostLikeKey id) { this.id = id; }
	public PostEntity getPost() { return post; }
	public void setPost(PostEntity post) { this.post = post; }
	public UserEntity getUser() { return user; }
	public void setUser(UserEntity user) { this.user = user; }
	public Integer getVote() { return vote; }
	public void setVote(Integer vote) { this.vote = vote; }
	public Instant getCreatedAt() { return createdAt; }
}

@Embeddable
@Data
@NoArgsConstructor
@AllArgsConstructor
class PostLikeKey implements java.io.Serializable {
	@Column(name = "post_id")
	private Long postId;
	@Column(name = "user_id")
	private Long userId;
	public Long getPostId() { return postId; }
	public void setPostId(Long postId) { this.postId = postId; }
	public Long getUserId() { return userId; }
	public void setUserId(Long userId) { this.userId = userId; }
	@Override
	public boolean equals(Object o) {
		if (this == o) return true;
		if (o == null || getClass() != o.getClass()) return false;
		PostLikeKey that = (PostLikeKey) o;
		return java.util.Objects.equals(postId, that.postId) &&
				java.util.Objects.equals(userId, that.userId);
	}
	@Override
	public int hashCode() { return java.util.Objects.hash(postId, userId); }
}


