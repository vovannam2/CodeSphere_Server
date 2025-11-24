package com.hcmute.codesphere_server.model.entity;

import jakarta.persistence.*;
import lombok.Data;
import lombok.Builder;
import lombok.NoArgsConstructor;
import lombok.AllArgsConstructor;
import java.time.Instant;
import java.util.HashSet;
import java.util.Set;

@Entity
@Table(name = "problems", indexes = {
	@Index(name = "idx_problem_slug", columnList = "slug"),
	@Index(name = "idx_problem_code", columnList = "code"),
	@Index(name = "idx_problem_author", columnList = "author_id"),
	@Index(name = "idx_problem_level", columnList = "level")
})
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class ProblemEntity {

	@Id
	@GeneratedValue(strategy = GenerationType.IDENTITY)
	private Long id;

	@Column(unique = true, length = 40)
	private String code;

	@Column(nullable = false, length = 200)
	private String title;

	@Column(nullable = false, unique = true, length = 220)
	private String slug;

	@Lob
	@Column(columnDefinition = "MEDIUMTEXT")
	private String content;

	@Column(nullable = false, length = 10)
	private String level; // EASY/MEDIUM/HARD

	@Lob
	private String sampleInput; // Dùng để hiển thị ở description frontend

	@Lob
	private String sampleOutput; // Dùng để hiển thị ở description frontend

	@Column(nullable = false)
	private Integer timeLimitMs = 2000;

	@Column(nullable = false)
	private Integer memoryLimitMb = 256;

	@ManyToOne(fetch = FetchType.LAZY)
	@JoinColumn(name = "author_id", nullable = false)
	private UserEntity author;

	@Column(nullable = false)
	private Boolean status = true;

	@Column(nullable = false, updatable = false)
	private Instant createdAt = Instant.now();

	@Column(nullable = false)
	private Instant updatedAt = Instant.now();

	@ManyToMany
	@JoinTable(name = "problem_tags",
			joinColumns = @JoinColumn(name = "problem_id"),
			inverseJoinColumns = @JoinColumn(name = "tag_id"))
	private Set<TagEntity> tags = new HashSet<>();

	@ManyToMany
	@JoinTable(name = "problem_categories",
			joinColumns = @JoinColumn(name = "problem_id"),
			inverseJoinColumns = @JoinColumn(name = "category_id"))
	private Set<CategoryEntity> categories = new HashSet<>();

	@ManyToMany
	@JoinTable(name = "problem_languages",
			joinColumns = @JoinColumn(name = "problem_id"),
			inverseJoinColumns = @JoinColumn(name = "language_id"))
	private Set<LanguageEntity> languages = new HashSet<>();

	@PreUpdate
	public void touchUpdatedAt() { this.updatedAt = Instant.now(); }

	public Long getId() { return id; }
	public void setId(Long id) { this.id = id; }
	public String getCode() { return code; }
	public void setCode(String code) { this.code = code; }
	public String getTitle() { return title; }
	public void setTitle(String title) { this.title = title; }
	public String getSlug() { return slug; }
	public void setSlug(String slug) { this.slug = slug; }
	public String getContent() { return content; }
	public void setContent(String content) { this.content = content; }
	public String getLevel() { return level; }
	public void setLevel(String level) { this.level = level; }
	public String getSampleInput() { return sampleInput; }
	public void setSampleInput(String sampleInput) { this.sampleInput = sampleInput; }
	public String getSampleOutput() { return sampleOutput; }
	public void setSampleOutput(String sampleOutput) { this.sampleOutput = sampleOutput; }
	public Integer getTimeLimitMs() { return timeLimitMs; }
	public void setTimeLimitMs(Integer timeLimitMs) { this.timeLimitMs = timeLimitMs; }
	public Integer getMemoryLimitMb() { return memoryLimitMb; }
	public void setMemoryLimitMb(Integer memoryLimitMb) { this.memoryLimitMb = memoryLimitMb; }
	public UserEntity getAuthor() { return author; }
	public void setAuthor(UserEntity author) { this.author = author; }
	public Boolean getStatus() { return status; }
	public void setStatus(Boolean status) { this.status = status; }
	public Instant getCreatedAt() { return createdAt; }
	public Instant getUpdatedAt() { return updatedAt; }
	public Set<TagEntity> getTags() { return tags; }
	public void setTags(Set<TagEntity> tags) { this.tags = tags; }
	public Set<CategoryEntity> getCategories() { return categories; }
	public void setCategories(Set<CategoryEntity> categories) { this.categories = categories; }
	public Set<LanguageEntity> getLanguages() { return languages; }
	public void setLanguages(Set<LanguageEntity> languages) { this.languages = languages; }
}


