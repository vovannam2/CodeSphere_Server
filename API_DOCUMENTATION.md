# Tài liệu API - CodeSphere Server

## Base URL
```
${base.url} = /api/v1
```

---

## 1. AUTHENTICATION APIs

### 1.1. Đăng ký tài khoản
- **Endpoint:** `POST /api/v1/auth/register`
- **Authentication:** Không cần
- **Request Body:**
```json
{
  "email": "user@example.com",
  "password": "password123",
  "username": "username"
}
```
- **Response:** `DataResponse<AuthResponse>`

### 1.2. Đăng nhập
- **Endpoint:** `POST /api/v1/auth/login`
- **Authentication:** Không cần
- **Request Body:**
```json
{
  "email": "user@example.com",
  "password": "password123"
}
```
- **Response:** `DataResponse<AuthResponse>`

### 1.3. Google OAuth2
- **Endpoint:** `GET /api/v1/auth/google`
- **Authentication:** Không cần
- **Response:** `DataResponse<String>` (Redirect URL: `/oauth2/authorization/google`)

---

## 2. CATEGORY APIs

### 2.1. Lấy danh sách categories
- **Endpoint:** `GET /api/v1/categories`
- **Authentication:** Không cần
- **Query Parameters:**
  - `rootOnly` (optional, default: false): Chỉ lấy root categories
- **Response:** `DataResponse<List<CategoryResponse>>`

---

## 3. LANGUAGE APIs

### 3.1. Lấy danh sách languages
- **Endpoint:** `GET /api/v1/languages`
- **Authentication:** Không cần
- **Response:** `DataResponse<List<LanguageResponse>>`

---

## 4. TAG APIs

### 4.1. Lấy danh sách tags
- **Endpoint:** `GET /api/v1/tags`
- **Authentication:** Không cần
- **Response:** `DataResponse<List<TagResponse>>`

---

## 5. PROBLEM APIs

### 5.1. Lấy danh sách problems (có phân trang)
- **Endpoint:** `GET /api/v1/problems`
- **Authentication:** Không cần (cần khi dùng bookmarkStatus hoặc status filter)
- **Query Parameters:**
  - `level` (optional): Lọc theo level (EASY/MEDIUM/HARD)
  - `category` (optional): Lọc theo category
  - `tag` (optional): Lọc theo tag
  - `language` (optional): Lọc theo language
  - `bookmarkStatus` (optional): "bookmarked", "not_bookmarked", "all" (cần authentication)
  - `status` (optional): "NOT_ATTEMPTED", "ATTEMPTED_NOT_COMPLETED", "COMPLETED", "all" (cần authentication)
  - `page` (default: 0): Số trang
  - `size` (default: 20): Số items mỗi trang
  - `sortBy` (default: "createdAt"): Trường sắp xếp
  - `sortDir` (default: "DESC"): Hướng sắp xếp (ASC/DESC)
- **Response:** `DataResponse<Page<ProblemResponse>>`

**Request Example:**
```
GET /api/v1/problems?level=EASY&category=algorithms&tag=dp&language=java&page=0&size=20&sortBy=createdAt&sortDir=DESC
GET /api/v1/problems?bookmarkStatus=bookmarked&status=COMPLETED
```

**Response Example:**
```json
{
  "status": "success",
  "message": "",
  "data": {
    "content": [
      {
        "id": 1,
        "code": "PROB001",
        "title": "Two Sum",
        "status": "COMPLETED",
        "level": "EASY",
        "tags": [...],
        "categories": [...]
      }
    ],
    "totalElements": 100,
    "totalPages": 5,
    "size": 20,
    "number": 0
  }
}
```

### 5.2. Lấy chi tiết problem
- **Endpoint:** `GET /api/v1/problems/{id}`
- **Authentication:** Không cần
- **Path Parameters:**
  - `id`: Problem ID
- **Response:** `DataResponse<ProblemDetailResponse>`

### 5.3. Lấy test cases của problem (Admin only)
- **Endpoint:** `GET /api/v1/problems/{id}/testcases`
- **Authentication:** Required (JWT Token)
- **Authorization:** ROLE_ADMIN
- **Path Parameters:**
  - `id`: Problem ID
- **Response:** `DataResponse<List<TestCaseResponse>>`

---

## 6. PROBLEM BOOKMARK APIs

### 6.1. Toggle bookmark (thêm/xóa bookmark)
- **Endpoint:** `POST /api/v1/problems/{problemId}/bookmark`
- **Authentication:** Required (JWT Token)
- **Path Parameters:**
  - `problemId`: Problem ID
- **Response:** `DataResponse<BookmarkResponse>`

**Response Example:**
```json
{
  "status": "success",
  "message": "",
  "data": {
    "problemId": 1,
    "isBookmarked": true,
    "message": "Đã đánh dấu sao bài tập"
  }
}
```

### 6.2. Kiểm tra bookmark status
- **Endpoint:** `GET /api/v1/problems/{problemId}/bookmark`
- **Authentication:** Required (JWT Token)
- **Path Parameters:**
  - `problemId`: Problem ID
- **Response:** `DataResponse<BookmarkResponse>`

---

## 7. SUBMISSION APIs

### 7.1. Tạo submission
- **Endpoint:** `POST /api/v1/submissions`
- **Authentication:** Required (JWT Token)
- **Request Body:**
```json
{
  "problemId": 1,
  "languageId": 1,
  "codeContent": "print('Hello World')"
}
```
- **Response:** `DataResponse<SubmissionDetailResponse>`

### 7.2. Lấy danh sách submissions (có phân trang)
- **Endpoint:** `GET /api/v1/submissions`
- **Authentication:** Không cần
- **Query Parameters:**
  - `userId` (optional): Lọc theo user ID
  - `problemId` (optional): Lọc theo problem ID
  - `status` (optional): Lọc theo status
  - `page` (default: 0): Số trang
  - `size` (default: 20): Số items mỗi trang
  - `sortBy` (default: "createdAt"): Trường sắp xếp
  - `sortDir` (default: "DESC"): Hướng sắp xếp (ASC/DESC)
- **Response:** `DataResponse<Page<SubmissionResponse>>`

### 7.3. Lấy chi tiết submission
- **Endpoint:** `GET /api/v1/submissions/{id}`
- **Authentication:** Không cần
- **Path Parameters:**
  - `id`: Submission ID
- **Response:** `DataResponse<SubmissionDetailResponse>`

### 7.4. Lấy submissions của user hiện tại
- **Endpoint:** `GET /api/v1/submissions/my-submissions`
- **Authentication:** Required (JWT Token)
- **Query Parameters:**
  - `problemId` (optional): Lọc theo problem ID
  - `status` (optional): Lọc theo status (ACCEPTED/REJECTED)
  - `page` (default: 0): Số trang
  - `size` (default: 20): Số items mỗi trang
  - `sortBy` (default: "createdAt"): Trường sắp xếp
  - `sortDir` (default: "DESC"): Hướng sắp xếp (ASC/DESC)
- **Response:** `DataResponse<Page<SubmissionResponse>>`

---

## 8. STATISTICS APIs

### 8.1. Thống kê cá nhân
- **Endpoint:** `GET /api/v1/stats/my-stats`
- **Authentication:** Required (JWT Token)
- **Response:** `DataResponse<MyStatsResponse>`

**Response Example:**
```json
{
  "status": "success",
  "message": "",
  "data": {
    "totalSolved": 50,
    "solvedByLevel": {
      "EASY": 20,
      "MEDIUM": 25,
      "HARD": 5
    },
    "totalSubmissions": 200,
    "acceptedSubmissions": 150,
    "acceptanceRate": 75.0,
    "totalProblemsAttempted": 80,
    "totalProblemsSolved": 50
  }
}
```

### 8.2. Thống kê bài tập cụ thể
- **Endpoint:** `GET /api/v1/stats/problems/{problemId}`
- **Authentication:** Không cần
- **Path Parameters:**
  - `problemId`: Problem ID
- **Response:** `DataResponse<ProblemStatsResponse>`

**Response Example:**
```json
{
  "status": "success",
  "message": "",
  "data": {
    "problemId": 1,
    "totalSubmissions": 100,
    "acceptedSubmissions": 60,
    "acceptanceRate": 60.0,
    "usersAttempted": 80,
    "usersSolved": 60
  }
}
```

---

## 9. POST APIs (Bài thảo luận)

### 9.1. Tạo bài thảo luận
- **Endpoint:** `POST /api/v1/posts`
- **Authentication:** Required (JWT Token)
- **Request Body:**
```json
{
  "title": "Làm sao để giải bài Two Sum?",
  "content": "Tôi đang gặp khó khăn với bài này...",
  "isAnonymous": false,
  "tagIds": [1, 2],
  "categoryIds": [1]
}
```
- **Response:** `DataResponse<PostDetailResponse>`

### 9.2. Lấy danh sách bài thảo luận
- **Endpoint:** `GET /api/v1/posts`
- **Authentication:** Không cần (optional để xem userVote)
- **Query Parameters:**
  - `authorId` (optional): Lọc theo author ID
  - `tag` (optional): Lọc theo tag slug
  - `category` (optional): Lọc theo category slug
  - `isResolved` (optional): Lọc theo trạng thái đã giải quyết (true/false)
  - `page` (default: 0): Số trang
  - `size` (default: 20): Số items mỗi trang
  - `sortBy` (default: "createdAt"): Trường sắp xếp
  - `sortDir` (default: "DESC"): Hướng sắp xếp (ASC/DESC)
- **Response:** `DataResponse<Page<PostResponse>>`

**Response Example:**
```json
{
  "status": "success",
  "message": "",
  "data": {
    "content": [
      {
        "id": 1,
        "title": "Làm sao để giải bài Two Sum?",
        "content": "Tôi đang gặp khó khăn...",
        "isAnonymous": false,
        "isResolved": false,
        "authorId": 1,
        "authorName": "user1",
        "authorAvatar": "https://...",
        "totalVotes": 10,
        "upvotes": 12,
        "downvotes": 2,
        "commentCount": 5,
        "userVote": 1,
        "tags": [...],
        "categories": [...],
        "createdAt": "2024-01-15T10:00:00Z",
        "updatedAt": "2024-01-15T10:00:00Z"
      }
    ],
    "totalElements": 50,
    "totalPages": 3
  }
}
```

### 9.3. Lấy chi tiết bài thảo luận
- **Endpoint:** `GET /api/v1/posts/{id}`
- **Authentication:** Không cần (optional để xem userVote)
- **Path Parameters:**
  - `id`: Post ID
- **Response:** `DataResponse<PostDetailResponse>`

**Response Example:**
```json
{
  "status": "success",
  "message": "",
  "data": {
    "id": 1,
    "title": "Làm sao để giải bài Two Sum?",
    "content": "Tôi đang gặp khó khăn...",
    "isResolved": false,
    "comments": [
      {
        "id": 1,
        "content": "Bạn có thể dùng HashMap...",
        "authorName": "user2",
        "replies": [...]
      }
    ],
    "createdAt": "2024-01-15T10:00:00Z"
  }
}
```

### 9.4. Cập nhật bài thảo luận
- **Endpoint:** `PUT /api/v1/posts/{id}`
- **Authentication:** Required (JWT Token)
- **Path Parameters:**
  - `id`: Post ID
- **Request Body:**
```json
{
  "title": "Updated title",
  "content": "Updated content",
  "tagIds": [1, 2, 3],
  "categoryIds": [1]
}
```
- **Response:** `DataResponse<PostDetailResponse>`

### 9.5. Xóa bài thảo luận
- **Endpoint:** `DELETE /api/v1/posts/{id}`
- **Authentication:** Required (JWT Token)
- **Path Parameters:**
  - `id`: Post ID
- **Response:** `DataResponse<String>`

### 9.6. Vote bài thảo luận
- **Endpoint:** `POST /api/v1/posts/{id}/vote`
- **Authentication:** Required (JWT Token)
- **Path Parameters:**
  - `id`: Post ID
- **Request Body:**
```json
{
  "vote": 1
}
```
- **Vote values:** 1 (upvote), -1 (downvote), 0 (remove vote)
- **Response:** `DataResponse<VoteResponse>`

**Response Example:**
```json
{
  "status": "success",
  "message": "",
  "data": {
    "id": 1,
    "vote": 1,
    "totalVotes": 11,
    "upvotes": 13,
    "downvotes": 2,
    "message": "Đã upvote"
  }
}
```

### 9.7. Đánh dấu đã giải quyết
- **Endpoint:** `POST /api/v1/posts/{id}/resolve`
- **Authentication:** Required (JWT Token)
- **Path Parameters:**
  - `id`: Post ID
- **Response:** `DataResponse<PostDetailResponse>`

---

## 10. COMMENT APIs

### 10.1. Tạo comment
- **Endpoint:** `POST /api/v1/posts/{postId}/comments`
- **Authentication:** Required (JWT Token)
- **Path Parameters:**
  - `postId`: Post ID
- **Request Body:**
```json
{
  "content": "Bạn có thể dùng HashMap để giải bài này",
  "parentCommentId": null
}
```
- **Response:** `DataResponse<CommentResponse>`

### 10.2. Lấy danh sách comments của post
- **Endpoint:** `GET /api/v1/posts/{postId}/comments`
- **Authentication:** Không cần (optional để xem userVote)
- **Path Parameters:**
  - `postId`: Post ID
- **Query Parameters:**
  - `page` (default: 0): Số trang
  - `size` (default: 20): Số items mỗi trang
  - `sortBy` (default: "createdAt"): Trường sắp xếp
  - `sortDir` (default: "ASC"): Hướng sắp xếp (ASC/DESC)
- **Response:** `DataResponse<Page<CommentResponse>>`

**Response Example:**
```json
{
  "status": "success",
  "message": "",
  "data": {
    "content": [
      {
        "id": 1,
        "content": "Bạn có thể dùng HashMap...",
        "authorId": 2,
        "authorName": "user2",
        "authorAvatar": "https://...",
        "parentCommentId": null,
        "totalVotes": 5,
        "upvotes": 6,
        "downvotes": 1,
        "userVote": null,
        "replyCount": 2,
        "replies": [
          {
            "id": 3,
            "content": "Cảm ơn bạn!",
            "parentCommentId": 1
          }
        ],
        "createdAt": "2024-01-15T10:05:00Z"
      }
    ]
  }
}
```

### 10.3. Trả lời comment
- **Endpoint:** `POST /api/v1/comments/{id}/reply`
- **Authentication:** Required (JWT Token)
- **Path Parameters:**
  - `id`: Comment ID (parent comment)
- **Request Body:**
```json
{
  "content": "Cảm ơn bạn đã giúp đỡ!"
}
```
- **Response:** `DataResponse<CommentResponse>`

### 10.4. Vote comment
- **Endpoint:** `POST /api/v1/comments/{id}/vote`
- **Authentication:** Required (JWT Token)
- **Path Parameters:**
  - `id`: Comment ID
- **Request Body:**
```json
{
  "vote": 1
}
```
- **Response:** `DataResponse<VoteResponse>`

### 10.5. Cập nhật comment
- **Endpoint:** `PUT /api/v1/comments/{id}`
- **Authentication:** Required (JWT Token)
- **Path Parameters:**
  - `id`: Comment ID
- **Request Body:**
```json
{
  "content": "Updated comment content"
}
```
- **Response:** `DataResponse<CommentResponse>`

### 10.6. Xóa comment
- **Endpoint:** `DELETE /api/v1/comments/{id}`
- **Authentication:** Required (JWT Token)
- **Path Parameters:**
  - `id`: Comment ID
- **Response:** `DataResponse<String>`

---

## 11. FRIEND APIs

### 11.1. Gửi lời mời kết bạn
- **Endpoint:** `POST /api/v1/friends/request`
- **Authentication:** Required (JWT Token)
- **Request Body:**
```json
{
  "receiverId": 2
}
```
- **Response:** `DataResponse<FriendRequestResponse>`

**Response Example:**
```json
{
  "status": "success",
  "message": "",
  "data": {
    "id": 1,
    "senderId": 1,
    "senderName": "user1",
    "senderAvatar": "https://...",
    "receiverId": 2,
    "receiverName": "user2",
    "receiverAvatar": "https://...",
    "status": "PENDING",
    "createdAt": "2024-01-15T10:00:00Z"
  }
}
```

### 11.2. Lấy danh sách lời mời kết bạn
- **Endpoint:** `GET /api/v1/friends/requests`
- **Authentication:** Required (JWT Token)
- **Query Parameters:**
  - `type` (optional, default: "all"): "sent", "received", "all"
- **Response:** `DataResponse<List<FriendRequestResponse>>`

### 11.3. Chấp nhận lời mời kết bạn
- **Endpoint:** `POST /api/v1/friends/requests/{id}/accept`
- **Authentication:** Required (JWT Token)
- **Path Parameters:**
  - `id`: Friend Request ID
- **Response:** `DataResponse<FriendRequestResponse>`

### 11.4. Từ chối lời mời kết bạn
- **Endpoint:** `POST /api/v1/friends/requests/{id}/reject`
- **Authentication:** Required (JWT Token)
- **Path Parameters:**
  - `id`: Friend Request ID
- **Response:** `DataResponse<FriendRequestResponse>`

### 11.5. Hủy lời mời kết bạn
- **Endpoint:** `DELETE /api/v1/friends/requests/{id}`
- **Authentication:** Required (JWT Token)
- **Path Parameters:**
  - `id`: Friend Request ID
- **Response:** `DataResponse<String>`

### 11.6. Lấy danh sách bạn bè
- **Endpoint:** `GET /api/v1/friends`
- **Authentication:** Required (JWT Token)
- **Response:** `DataResponse<List<FriendResponse>>`

**Response Example:**
```json
{
  "status": "success",
  "message": "",
  "data": [
    {
      "userId": 2,
      "username": "user2",
      "avatar": "https://...",
      "friendSince": "2024-01-15T10:00:00Z"
    }
  ]
}
```

### 11.7. Hủy kết bạn
- **Endpoint:** `DELETE /api/v1/friends/{userId}`
- **Authentication:** Required (JWT Token)
- **Path Parameters:**
  - `userId`: User ID của bạn cần hủy
- **Response:** `DataResponse<String>`

---

## 12. CONVERSATION APIs (Nhắn tin)

### 12.1. Tạo conversation
- **Endpoint:** `POST /api/v1/conversations`
- **Authentication:** Required (JWT Token)
- **Request Body:**
```json
{
  "type": "DIRECT",
  "name": null,
  "avatar": null,
  "participantIds": [2]
}
```
**Hoặc cho GROUP:**
```json
{
  "type": "GROUP",
  "name": "Nhóm học tập",
  "avatar": "https://...",
  "participantIds": [2, 3, 4]
}
```
- **Response:** `DataResponse<ConversationResponse>`

**Response Example:**
```json
{
  "status": "success",
  "message": "",
  "data": {
    "id": 1,
    "type": "DIRECT",
    "name": null,
    "avatar": null,
    "createdById": 1,
    "createdByName": "user1",
    "participants": [
      {
        "userId": 1,
        "username": "user1",
        "avatar": "https://...",
        "role": "MEMBER",
        "joinedAt": "2024-01-15T10:00:00Z"
      },
      {
        "userId": 2,
        "username": "user2",
        "avatar": "https://...",
        "role": "MEMBER",
        "joinedAt": "2024-01-15T10:00:00Z"
      }
    ],
    "lastMessage": null,
    "unreadCount": 0,
    "createdAt": "2024-01-15T10:00:00Z"
  }
}
```

### 12.2. Lấy danh sách conversations
- **Endpoint:** `GET /api/v1/conversations`
- **Authentication:** Required (JWT Token)
- **Response:** `DataResponse<List<ConversationResponse>>`

### 12.3. Lấy chi tiết conversation
- **Endpoint:** `GET /api/v1/conversations/{id}`
- **Authentication:** Required (JWT Token)
- **Path Parameters:**
  - `id`: Conversation ID
- **Response:** `DataResponse<ConversationResponse>`

### 12.4. Cập nhật conversation (GROUP only)
- **Endpoint:** `PUT /api/v1/conversations/{id}`
- **Authentication:** Required (JWT Token)
- **Authorization:** ADMIN của conversation
- **Path Parameters:**
  - `id`: Conversation ID
- **Request Body:**
```json
{
  "name": "Updated group name",
  "avatar": "https://..."
}
```
- **Response:** `DataResponse<ConversationResponse>`

### 12.5. Thêm thành viên vào conversation (GROUP only)
- **Endpoint:** `POST /api/v1/conversations/{id}/members`
- **Authentication:** Required (JWT Token)
- **Authorization:** ADMIN của conversation
- **Path Parameters:**
  - `id`: Conversation ID
- **Request Body:**
```json
{
  "userIds": [5, 6]
}
```
- **Response:** `DataResponse<ConversationResponse>`

### 12.6. Xóa thành viên khỏi conversation (GROUP only)
- **Endpoint:** `DELETE /api/v1/conversations/{id}/members/{userId}`
- **Authentication:** Required (JWT Token)
- **Authorization:** ADMIN hoặc tự xóa chính mình
- **Path Parameters:**
  - `id`: Conversation ID
  - `userId`: User ID cần xóa
- **Response:** `DataResponse<String>`

---

## 13. MESSAGE APIs

### 13.1. Gửi tin nhắn
- **Endpoint:** `POST /api/v1/conversations/{conversationId}/messages`
- **Authentication:** Required (JWT Token)
- **Path Parameters:**
  - `conversationId`: Conversation ID
- **Request Body (TEXT):**
```json
{
  "content": "Xin chào!",
  "messageType": "TEXT",
  "imageUrl": null
}
```
**Request Body (IMAGE):**
```json
{
  "content": null,
  "messageType": "IMAGE",
  "imageUrl": "https://cloudinary.com/image.jpg"
}
```
- **Response:** `DataResponse<MessageResponse>`

**Response Example:**
```json
{
  "status": "success",
  "message": "",
  "data": {
    "id": 1,
    "conversationId": 1,
    "senderId": 1,
    "senderName": "user1",
    "senderAvatar": "https://...",
    "content": "Xin chào!",
    "messageType": "TEXT",
    "imageUrl": null,
    "isDeleted": false,
    "createdAt": "2024-01-15T10:00:00Z"
  }
}
```

### 13.2. Lấy danh sách tin nhắn
- **Endpoint:** `GET /api/v1/conversations/{conversationId}/messages`
- **Authentication:** Required (JWT Token)
- **Path Parameters:**
  - `conversationId`: Conversation ID
- **Query Parameters:**
  - `page` (default: 0): Số trang
  - `size` (default: 50): Số items mỗi trang
  - `sortBy` (default: "createdAt"): Trường sắp xếp
  - `sortDir` (default: "DESC"): Hướng sắp xếp (ASC/DESC)
- **Response:** `DataResponse<Page<MessageResponse>>`

### 13.3. Xóa tin nhắn
- **Endpoint:** `DELETE /api/v1/conversations/{conversationId}/messages/{id}`
- **Authentication:** Required (JWT Token)
- **Path Parameters:**
  - `conversationId`: Conversation ID
  - `id`: Message ID
- **Response:** `DataResponse<String>`

### 13.4. WebSocket cho real-time messaging
- **WebSocket Endpoint:** `/ws`
- **STOMP Destination để gửi:** `/app/chat.send`
- **STOMP Destination để nhận:** `/topic/conversation/{conversationId}`
- **Message Payload:**
```json
{
  "conversationId": 1,
  "userId": 1,
  "content": "Hello!",
  "messageType": "TEXT",
  "imageUrl": null
}
```

---

## 14. NOTIFICATION APIs

### 14.1. Lấy danh sách notifications
- **Endpoint:** `GET /api/v1/notifications`
- **Authentication:** Required (JWT Token)
- **Query Parameters:**
  - `type` (optional): FRIEND_REQUEST, FRIEND_ACCEPTED, POST_LIKE, POST_COMMENT, COMMENT_REPLY, MESSAGE
  - `isRead` (optional): true/false (null = tất cả)
  - `page` (default: 0): Số trang
  - `size` (default: 20): Số items mỗi trang
  - `sortBy` (default: "createdAt"): Trường sắp xếp
  - `sortDir` (default: "DESC"): Hướng sắp xếp (ASC/DESC)
- **Response:** `DataResponse<Page<NotificationResponse>>`

**Response Example:**
```json
{
  "status": "success",
  "message": "",
  "data": {
    "content": [
      {
        "id": 1,
        "type": "FRIEND_REQUEST",
        "title": "Lời mời kết bạn",
        "content": "user2 đã gửi lời mời kết bạn",
        "relatedUserId": 2,
        "relatedUserName": "user2",
        "relatedUserAvatar": "https://...",
        "isRead": false,
        "createdAt": "2024-01-15T10:00:00Z"
      }
    ]
  }
}
```

### 14.2. Lấy số lượng notifications chưa đọc
- **Endpoint:** `GET /api/v1/notifications/unread-count`
- **Authentication:** Required (JWT Token)
- **Response:** `DataResponse<Long>`

**Response Example:**
```json
{
  "status": "success",
  "message": "",
  "data": 5
}
```

### 14.3. Đánh dấu notification đã đọc
- **Endpoint:** `PUT /api/v1/notifications/{id}/read`
- **Authentication:** Required (JWT Token)
- **Path Parameters:**
  - `id`: Notification ID
- **Response:** `DataResponse<NotificationResponse>`

### 14.4. Đánh dấu tất cả notifications đã đọc
- **Endpoint:** `PUT /api/v1/notifications/read-all`
- **Authentication:** Required (JWT Token)
- **Response:** `DataResponse<String>`

### 14.5. Xóa notification
- **Endpoint:** `DELETE /api/v1/notifications/{id}`
- **Authentication:** Required (JWT Token)
- **Path Parameters:**
  - `id`: Notification ID
- **Response:** `DataResponse<String>`

### 14.6. WebSocket cho real-time notifications
- **WebSocket Endpoint:** `/ws`
- **STOMP Destination để nhận:** `/user/{userId}/queue/notifications`
- **Message Format:** `NotificationResponse`

---

## 15. USER/PROFILE APIs

### 15.1. Lấy profile của user hiện tại
- **Endpoint:** `GET /api/v1/users/me/profile`
- **Authentication:** Required (JWT Token)
- **Response:** `DataResponse<UserProfileResponse>`

**Response Example:**
```json
{
  "status": "success",
  "message": "",
  "data": {
    "userId": 1,
    "username": "user1",
    "email": "user1@example.com",
    "avatar": "https://...",
    "dob": "2000-01-01",
    "phoneNumber": "0123456789",
    "gender": "MALE",
    "status": true,
    "lastOnline": "2024-01-15T10:00:00Z",
    "role": "ROLE_USER",
    "authenWith": 0,
    "isBlocked": false,
    "createdAt": "2024-01-01T00:00:00Z"
  }
}
```

### 15.2. Lấy profile công khai của user khác
- **Endpoint:** `GET /api/v1/users/{id}/profile`
- **Authentication:** Không cần (optional để xem isFriend)
- **Path Parameters:**
  - `id`: User ID
- **Response:** `DataResponse<UserPublicProfileResponse>`

**Response Example:**
```json
{
  "status": "success",
  "message": "",
  "data": {
    "userId": 2,
    "username": "user2",
    "avatar": "https://...",
    "dob": "2000-02-02",
    "gender": "FEMALE",
    "lastOnline": "2024-01-15T10:00:00Z",
    "postCount": 10,
    "friendCount": 50,
    "isFriend": true,
    "createdAt": "2024-01-01T00:00:00Z"
  }
}
```

### 15.3. Cập nhật profile
- **Endpoint:** `PUT /api/v1/users/me/profile`
- **Authentication:** Required (JWT Token)
- **Request Body:**
```json
{
  "username": "newusername",
  "dob": "2000-01-01",
  "phoneNumber": "0123456789",
  "gender": "MALE"
}
```
- **Response:** `DataResponse<UserProfileResponse>`

### 15.4. Upload avatar
- **Endpoint:** `POST /api/v1/users/me/avatar`
- **Authentication:** Required (JWT Token)
- **Request:** `multipart/form-data`
  - `file`: Image file
- **Response:** `DataResponse<UserProfileResponse>`

### 15.5. Lấy danh sách posts của user
- **Endpoint:** `GET /api/v1/users/{id}/posts`
- **Authentication:** Không cần (optional để xem userVote)
- **Path Parameters:**
  - `id`: User ID
- **Query Parameters:**
  - `page` (default: 0): Số trang
  - `size` (default: 20): Số items mỗi trang
  - `sortBy` (default: "createdAt"): Trường sắp xếp
  - `sortDir` (default: "DESC"): Hướng sắp xếp (ASC/DESC)
- **Response:** `DataResponse<Page<PostResponse>>`

---

## 16. LEADERBOARD APIs

### 16.1. Lấy bảng xếp hạng của một problem
- **Endpoint:** `GET /api/v1/leaderboard`
- **Authentication:** Không cần
- **Query Parameters:**
  - `problemId` (required): ID của problem
- **Response:** `DataResponse<List<LeaderboardResponse>>`

**Response Example:**
```json
{
  "status": "success",
  "message": "",
  "data": [
    {
      "rank": 1,
      "userId": 1,
      "username": "user1",
      "bestScore": 100,
      "bestSubmissionId": 15,
      "totalSubmissions": 10,
      "bestSubmissionTime": "2024-01-15T10:30:00Z",
      "statusMsg": "Accepted",
      "statusRuntime": "50 ms",
      "statusMemory": "256 KB",
      "isAccepted": true
    }
  ]
}
```

### 16.2. Lấy xếp hạng của user hiện tại trong một problem
- **Endpoint:** `GET /api/v1/leaderboard/my-rank`
- **Authentication:** Required (JWT Token)
- **Query Parameters:**
  - `problemId` (required): ID của problem
- **Response:** `DataResponse<LeaderboardResponse>`

---

## 17. CODE VALIDATION APIs

### 17.1. Validate code syntax
- **Endpoint:** `POST /api/v1/code/validate`
- **Authentication:** Không cần
- **Request Body:**
```json
{
  "codeContent": "print('Hello World')",
  "languageCode": "python"
}
```
- **Response:** `DataResponse<ValidationResponse>`

---

## 18. JUDGE TEST APIs

### 18.1. Test Docker connection
- **Endpoint:** `GET /api/v1/judge/test`
- **Authentication:** Không cần
- **Query Parameters:**
  - `languageCode` (default: "python"): Mã ngôn ngữ (python/java/javascript/cpp)
- **Response:** `DataResponse<Map<String, Object>>`
- **Note:** Chỉ dùng cho testing, có thể xóa trong production

---

## 19. ADMIN APIs

### 19.1. Tạo category (Admin only)
- **Endpoint:** `POST /api/v1/admin/categories`
- **Authentication:** Required (JWT Token)
- **Authorization:** ROLE_ADMIN
- **Request Body:**
```json
{
  "name": "Data Structures",
  "slug": "data-structures",
  "parentId": null
}
```
- **Response:** `DataResponse<CategoryResponse>`

### 19.2. Tạo language (Admin only)
- **Endpoint:** `POST /api/v1/admin/languages`
- **Authentication:** Required (JWT Token)
- **Authorization:** ROLE_ADMIN
- **Request Body:**
```json
{
  "code": "python",
  "name": "Python",
  "version": "3.11"
}
```
- **Response:** `DataResponse<LanguageResponse>`

### 19.3. Tạo problem (Admin only)
- **Endpoint:** `POST /api/v1/admin/problems`
- **Authentication:** Required (JWT Token)
- **Authorization:** ROLE_ADMIN
- **Request Body:**
```json
{
  "code": "PROB001",
  "title": "Two Sum",
  "slug": "two-sum",
  "content": "Given an array of integers...",
  "level": "EASY",
  "sampleInput": "2 7 11 15\n9",
  "sampleOutput": "0 1",
  "timeLimitMs": 2000,
  "memoryLimitMb": 256,
  "categoryIds": [1, 2],
  "tagIds": [1, 2],
  "languageIds": [1, 2]
}
```
- **Response:** `DataResponse<ProblemDetailResponse>`

### 19.4. Tạo tag (Admin only)
- **Endpoint:** `POST /api/v1/admin/tags`
- **Authentication:** Required (JWT Token)
- **Authorization:** ROLE_ADMIN
- **Request Body:**
```json
{
  "name": "Array",
  "slug": "array"
}
```
- **Response:** `DataResponse<TagResponse>`

### 19.5. Tạo test case (Admin only)
- **Endpoint:** `POST /api/v1/admin/testcases`
- **Authentication:** Required (JWT Token)
- **Authorization:** ROLE_ADMIN
- **Request Body:**
```json
{
  "problemId": 1,
  "input": "2 7 11 15\n9",
  "expectedOutput": "0 1",
  "isSample": false,
  "isHidden": false,
  "weight": 1
}
```
- **Response:** `DataResponse<TestCaseResponse>`

---

## Response Format

Tất cả API đều trả về format chuẩn:
```json
{
  "status": "success" | "error",
  "message": "string",
  "data": {...} // Khi status = "success"
}
```

---

## Authentication

Các API yêu cầu authentication sẽ cần JWT token trong header:
```
Authorization: Bearer <token>
```

---

## Authorization

- **ROLE_ADMIN:** Các API admin chỉ dành cho admin
- **ROLE_USER:** Các API user dành cho user đã đăng nhập

---

## WebSocket

### Connection
- **Endpoint:** `ws://localhost:8080/ws` hoặc `http://localhost:8080/ws` (với SockJS)
- **Protocol:** STOMP over WebSocket

### Destinations

#### Messaging
- **Send:** `/app/chat.send`
- **Receive:** `/topic/conversation/{conversationId}`
- **Join:** `/app/chat.addUser`

#### Notifications
- **Receive:** `/user/{userId}/queue/notifications`

### Example (JavaScript)
```javascript
// Connect
const socket = new SockJS('http://localhost:8080/ws');
const stompClient = Stomp.over(socket);

stompClient.connect({}, function(frame) {
  // Subscribe to conversation messages
  stompClient.subscribe('/topic/conversation/1', function(message) {
    const messageData = JSON.parse(message.body);
    console.log('New message:', messageData);
  });
  
  // Subscribe to notifications
  stompClient.subscribe('/user/1/queue/notifications', function(notification) {
    const notificationData = JSON.parse(notification.body);
    console.log('New notification:', notificationData);
  });
  
  // Send message
  stompClient.send('/app/chat.send', {}, JSON.stringify({
    conversationId: 1,
    userId: 1,
    content: "Hello!",
    messageType: "TEXT",
    imageUrl: null
  }));
});
```

---

## Lưu ý

1. Base URL được cấu hình trong `application.properties`: `base.url=/api/v1`
2. Server port mặc định: `8080`
3. Tất cả request body phải có `Content-Type: application/json` (trừ upload file)
4. Upload file sử dụng `multipart/form-data`
5. Validation errors sẽ trả về với status code 400
6. Unauthorized requests sẽ trả về status code 401
7. Forbidden requests sẽ trả về status code 403
8. Pagination: `page` bắt đầu từ 0
9. Sort direction: `ASC` hoặc `DESC` (case-insensitive)
