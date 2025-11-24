package com.hcmute.codesphere_server.model.entity;

import jakarta.persistence.*;
import lombok.Data;
import lombok.Builder;
import lombok.NoArgsConstructor;
import lombok.AllArgsConstructor;
import java.time.Instant;

@Entity
@Table(name = "follows", uniqueConstraints = {
	@UniqueConstraint(columnNames = {"follower_id", "followee_id"})
})
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class FollowEntity {

	@EmbeddedId
	@lombok.Builder.Default
	private FollowKey id = new FollowKey();

	@ManyToOne(fetch = FetchType.LAZY)
	@MapsId("followerId")
	@JoinColumn(name = "follower_id")
	private UserEntity follower;

	@ManyToOne(fetch = FetchType.LAZY)
	@MapsId("followeeId")
	@JoinColumn(name = "followee_id")
	private UserEntity followee;

	@Column(nullable = false)
	@lombok.Builder.Default
	private Instant createdAt = Instant.now();

}

@Embeddable
@Data
@NoArgsConstructor
@AllArgsConstructor
class FollowKey implements java.io.Serializable {
	@Column(name = "follower_id")
	private Long followerId;
	@Column(name = "followee_id")
	private Long followeeId;
	public Long getFollowerId() { return followerId; }
	public void setFollowerId(Long followerId) { this.followerId = followerId; }
	public Long getFolloweeId() { return followeeId; }
	public void setFolloweeId(Long followeeId) { this.followeeId = followeeId; }
	@Override
	public boolean equals(Object o) {
		if (this == o) return true;
		if (o == null || getClass() != o.getClass()) return false;
		FollowKey that = (FollowKey) o;
		return java.util.Objects.equals(followerId, that.followerId) &&
				java.util.Objects.equals(followeeId, that.followeeId);
	}
	@Override
	public int hashCode() {
		return java.util.Objects.hash(followerId, followeeId);
	}
}


