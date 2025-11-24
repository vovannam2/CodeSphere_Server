# 🔄 Flow Nộp Bài và Xem Kết Quả

## ✅ Có, nó TỰ ĐỘNG chạy testcases!

Sau khi nộp submission, hệ thống sẽ **tự động**:
1. ✅ Tạo submission với state `PENDING`
2. ✅ Gọi Judge Service (async) để chạy code
3. ✅ Chạy code với **TẤT CẢ test cases** của problem
4. ✅ So sánh output với expected output
5. ✅ Lưu kết quả vào `SubmissionTestcaseEntity`
6. ✅ Cập nhật submission với kết quả cuối cùng

---

## 📊 Flow Chi Tiết

```
1. User nộp code
   POST /api/v1/submissions
   ↓
2. Tạo SubmissionEntity (state: PENDING)
   ↓
3. Judge Service chạy ASYNC (không block request)
   ↓
4. Với mỗi test case:
   - Chạy code với input
   - Lấy output
   - So sánh với expectedOutput
   - Lưu vào SubmissionTestcaseEntity
   ↓
5. Tính tổng kết:
   - totalCorrect / totalTestcases
   - isAccepted (true nếu tất cả pass)
   - score (phần trăm)
   ↓
6. Cập nhật SubmissionEntity
   state: ACCEPTED hoặc WRONG_ANSWER
```

---

## 👀 Xem Kết Quả Ở Đâu?

### 1. Xem chi tiết submission (Khuyến nghị)

**API:** `GET /api/v1/submissions/{id}`

```bash
curl -X GET "http://localhost:8080/api/v1/submissions/1" \
  -H "Authorization: Bearer YOUR_JWT_TOKEN"
```

**Response:**
```json
{
  "success": true,
  "data": {
    "id": 1,
    "userId": 1,
    "username": "user123",
    "problemId": 1,
    "problemTitle": "Tính tổng 2 số",
    "problemCode": "SUM",
    "languageId": 3,
    "languageName": "C++",
    "languageCode": "cpp",
    "codeContent": "#include <iostream>...",
    "isAccepted": true,              // ← Kết quả
    "score": 100,                    // ← Điểm
    "statusCode": 3,
    "statusRuntime": "50 ms",
    "statusMemory": "1024 KB",
    "statusMsg": "Accepted",         // ← Thông báo
    "state": "ACCEPTED",             // ← Trạng thái
    "totalCorrect": 5,               // ← Số test cases pass
    "totalTestcases": 5,             // ← Tổng số test cases
    "compileError": null,
    "fullCompileError": null,
    "createdAt": "2025-01-XX...",
    "updatedAt": "2025-01-XX..."
  }
}
```

**Các trạng thái:**
- `PENDING` → Đang chờ judge
- `ACCEPTED` → Pass tất cả test cases ✅
- `WRONG_ANSWER` → Fail một số test cases ❌
- `ERROR` → Lỗi khi judge

---

### 2. Xem danh sách submissions

**API:** `GET /api/v1/submissions`

**Query Parameters:**
- `problemId` (optional): Filter theo problem
- `userId` (optional): Filter theo user
- `status` (optional): "ACCEPTED" hoặc "REJECTED"
- `page`, `size`: Phân trang

```bash
curl -X GET "http://localhost:8080/api/v1/submissions?problemId=1&status=ACCEPTED" \
  -H "Authorization: Bearer YOUR_JWT_TOKEN"
```

**Response:**
```json
{
  "success": true,
  "data": {
    "content": [
      {
        "id": 1,
        "problemTitle": "Tính tổng 2 số",
        "isAccepted": true,
        "score": 100,
        "statusMsg": "Accepted",
        "state": "ACCEPTED",
        "totalCorrect": 5,
        "totalTestcases": 5,
        "createdAt": "..."
      }
    ],
    "totalElements": 1,
    "totalPages": 1
  }
}
```

---

### 3. Xem submissions của mình

**API:** `GET /api/v1/submissions/my-submissions`

```bash
curl -X GET "http://localhost:8080/api/v1/submissions/my-submissions" \
  -H "Authorization: Bearer YOUR_JWT_TOKEN"
```

---

## ⏱️ Timing - Khi nào có kết quả?

### Ngay sau khi nộp:
```json
{
  "state": "PENDING",
  "statusMsg": "Đang chờ xử lý...",
  "totalTestcases": 0
}
```

### Sau 3-10 giây (tùy số lượng test cases):
```json
{
  "state": "ACCEPTED",  // hoặc "WRONG_ANSWER"
  "statusMsg": "Accepted",
  "totalTestcases": 5,
  "totalCorrect": 5
}
```

---

## 🔄 Polling để xem kết quả real-time

### Cách 1: Poll mỗi 2 giây

```javascript
async function checkSubmissionResult(submissionId) {
  const maxAttempts = 30; // Tối đa 60 giây
  let attempts = 0;
  
  while (attempts < maxAttempts) {
    const response = await fetch(`/api/v1/submissions/${submissionId}`, {
      headers: {
        'Authorization': `Bearer ${token}`
      }
    });
    
    const result = await response.json();
    const submission = result.data;
    
    // Nếu không còn PENDING, trả về kết quả
    if (submission.state !== 'PENDING') {
      return submission;
    }
    
    // Đợi 2 giây rồi check lại
    await new Promise(resolve => setTimeout(resolve, 2000));
    attempts++;
  }
  
  throw new Error('Timeout waiting for result');
}
```

### Cách 2: Đợi 5 giây rồi check

```bash
# Nộp bài
SUBMISSION_ID=$(curl -X POST ... | jq -r '.data.id')

# Đợi 5 giây
sleep 5

# Check kết quả
curl -X GET "/api/v1/submissions/$SUBMISSION_ID" | jq
```

---

## 📋 Checklist Kết Quả

Sau khi judge xong, kiểm tra:

- [ ] `state` không còn `PENDING`
- [ ] `totalTestcases > 0` (đã chạy test cases)
- [ ] `totalCorrect` = số test cases pass
- [ ] `isAccepted: true` nếu pass tất cả
- [ ] `statusMsg` có thông báo rõ ràng
- [ ] `score` = phần trăm (totalCorrect * 100 / totalTestcases)

---

## 🗄️ Xem Chi Tiết Từng Test Case (Database)

```sql
SELECT 
    st.submission_id,
    st.test_case_id,
    tc.input,
    tc.expected_output,
    st.status,           -- PASSED hoặc FAILED
    st.stdout,           -- Output của code
    st.runtime_ms,       -- Thời gian chạy
    st.memory_kb         -- Bộ nhớ sử dụng
FROM submission_testcases st
JOIN test_cases tc ON st.test_case_id = tc.id
WHERE st.submission_id = 1;
```

**Kết quả:**
```
submission_id | test_case_id | input | expected_output | status  | stdout | runtime_ms
--------------|--------------|-------|-----------------|---------|--------|------------
1             | 1            | 1 2   | 3               | PASSED  | 3      | 50
1             | 2            | 5 10  | 15              | PASSED  | 15     | 48
1             | 3            | -5 5  | 0               | PASSED  | 0      | 45
1             | 4            | 100 200 | 300          | PASSED  | 300    | 52
1             | 5            | 0 0   | 0               | PASSED  | 0      | 40
```

---

## 🎯 Tóm Tắt

1. ✅ **Tự động chạy**: Sau khi nộp, Judge Service tự động chạy tất cả test cases
2. ✅ **Xem kết quả**: `GET /api/v1/submissions/{id}`
3. ✅ **Polling**: Check mỗi 2-5 giây cho đến khi `state != PENDING`
4. ✅ **Kết quả**: `isAccepted`, `score`, `totalCorrect/totalTestcases`

