# Hướng dẫn Test Judge Service & Submission API

## 📋 Mục lục
1. [Chuẩn bị](#chuẩn-bị)
2. [Test API Nộp Bài (Submission)](#test-api-nộp-bài-submission)
3. [Test API Xem Kết Quả](#test-api-xem-kết-quả)
4. [Test Judge0 API Connection](#test-judge0-api-connection)
5. [Test Code Validation](#test-code-validation)

---

## 🔧 Chuẩn bị

### 1. Kiểm tra Judge0 Config
Mở file `application.properties` và kiểm tra:
```properties
judge0.api.url=https://judge0-ce.p.rapidapi.com
judge0.api.key=
```

### 2. Cần có:
- ✅ Server đang chạy (port 8080)
- ✅ Database có ít nhất 1 Problem với test cases
- ✅ Đã đăng nhập và có JWT token
- ✅ Postman hoặc curl để test

---

## 📤 Test API Nộp Bài (Submission)

### Endpoint: `POST /api/v1/submissions`

### Request Headers:
```
Content-Type: application/json
Authorization: Bearer YOUR_JWT_TOKEN
```

### Request Body:
```json
{
  "problemId": 1,
  "languageId": 1,
  "codeContent": "import java.util.Scanner;\n\npublic class Main {\n    public static void main(String[] args) {\n        Scanner sc = new Scanner(System.in);\n        int n = sc.nextInt();\n        System.out.println(n * n);\n    }\n}"
}
```

### CURL Command:
```bash
curl -X POST http://localhost:8080/api/v1/submissions \
  -H "Content-Type: application/json" \
  -H "Authorization: Bearer YOUR_JWT_TOKEN" \
  -d '{
    "problemId": 1,
    "languageId": 1,
    "codeContent": "import java.util.Scanner;\n\npublic class Main {\n    public static void main(String[] args) {\n        Scanner sc = new Scanner(System.in);\n        int n = sc.nextInt();\n        System.out.println(n * n);\n    }\n}"
  }'
```

### Response (Ngay lập tức - trước khi judge):
```json
{
  "success": true,
  "data": {
    "id": 1,
    "userId": 1,
    "username": "user123",
    "problemId": 1,
    "problemTitle": "Tính bình phương",
    "problemCode": "SQUARE",
    "languageId": 1,
    "languageName": "Java",
    "languageCode": "java",
    "codeContent": "...",
    "isAccepted": false,
    "score": 0,
    "statusCode": 0,
    "statusRuntime": "0 ms",
    "statusMemory": "0 KB",
    "statusMsg": "Đang chờ xử lý...",
    "state": "PENDING",
    "totalCorrect": 0,
    "totalTestcases": 0,
    "createdAt": "2025-01-XX..."
  }
}
```

**Lưu ý:** Submission sẽ có state `PENDING` ngay sau khi tạo. Judge Service sẽ chạy async và cập nhật kết quả sau.

---

## 📊 Test API Xem Kết Quả

### 1. Xem chi tiết submission: `GET /api/v1/submissions/{id}`

### CURL Command:
```bash
curl -X GET http://localhost:8080/api/v1/submissions/1 \
  -H "Authorization: Bearer YOUR_JWT_TOKEN"
```

### Response (Sau khi judge xong):
```json
{
  "success": true,
  "data": {
    "id": 1,
    "userId": 1,
    "username": "user123",
    "problemId": 1,
    "problemTitle": "Tính bình phương",
    "problemCode": "SQUARE",
    "languageId": 1,
    "languageName": "Java",
    "languageCode": "java",
    "codeContent": "...",
    "isAccepted": true,
    "score": 100,
    "statusCode": 3,
    "statusRuntime": "50 ms",
    "statusMemory": "1024 KB",
    "statusMsg": "Accepted",
    "state": "ACCEPTED",
    "totalCorrect": 3,
    "totalTestcases": 3,
    "compileError": null,
    "fullCompileError": null,
    "createdAt": "2025-01-XX...",
    "updatedAt": "2025-01-XX..."
  }
}
```

### 2. Xem danh sách submissions: `GET /api/v1/submissions`

### Query Parameters:
- `userId` (optional): Filter theo user
- `problemId` (optional): Filter theo problem
- `status` (optional): "ACCEPTED" hoặc "REJECTED"
- `page` (default: 0): Số trang
- `size` (default: 20): Số items mỗi trang
- `sortBy` (default: "createdAt"): Sắp xếp theo field
- `sortDir` (default: "DESC"): "ASC" hoặc "DESC"

### CURL Command:
```bash
curl -X GET "http://localhost:8080/api/v1/submissions?problemId=1&status=ACCEPTED&page=0&size=10" \
  -H "Authorization: Bearer YOUR_JWT_TOKEN"
```

### 3. Xem submissions của mình: `GET /api/v1/submissions/my-submissions`

### CURL Command:
```bash
curl -X GET "http://localhost:8080/api/v1/submissions/my-submissions?page=0&size=10" \
  -H "Authorization: Bearer YOUR_JWT_TOKEN"
```

---

## 🔍 Test Judge0 API Connection

### Cách 1: Test trực tiếp Judge0 API

### Test với cURL:
```bash
curl -X POST "https://judge0-ce.p.rapidapi.com/submissions?base64_encoded=false&wait=true" \
  -H "Content-Type: application/json" \
  -H "X-RapidAPI-Key: YOUR_RAPIDAPI_KEY" \
  -H "X-RapidAPI-Host: judge0-ce.p.rapidapi.com" \
  -d '{
    "source_code": "print(\"Hello World\")",
    "language_id": 71,
    "stdin": ""
  }'
```

**Nếu không có RapidAPI key**, có thể test với public endpoint (có thể bị rate limit):
```bash
curl -X POST "https://ce.judge0.com/submissions?base64_encoded=false&wait=true" \
  -H "Content-Type: application/json" \
  -d '{
    "source_code": "print(\"Hello World\")",
    "language_id": 71,
    "stdin": ""
  }'
```

### Response từ Judge0:
```json
{
  "stdout": "Hello World\n",
  "stderr": null,
  "status": {
    "id": 3,
    "description": "Accepted"
  },
  "time": "0.001",
  "memory": 1234
}
```

### Cách 2: Test qua Code Validation API

### Endpoint: `POST /api/v1/code/validate`

### Request:
```json
{
  "codeContent": "print(\"Hello World\")",
  "languageCode": "python"
}
```

### CURL Command:
```bash
curl -X POST http://localhost:8080/api/v1/code/validate \
  -H "Content-Type: application/json" \
  -H "Authorization: Bearer YOUR_JWT_TOKEN" \
  -d '{
    "codeContent": "print(\"Hello World\")",
    "languageCode": "python"
  }'
```

### Response (Nếu Judge0 hoạt động):
```json
{
  "success": true,
  "data": {
    "valid": true,
    "message": "Code compiles successfully",
    "errors": null
  }
}
```

### Response (Nếu Judge0 không hoạt động):
```json
{
  "success": false,
  "message": "Validation error: Connection refused / Timeout"
}
```

---

## 🧪 Test Flow Hoàn Chỉnh

### Bước 1: Tạo Problem với Test Cases
```sql
-- Tạo problem
INSERT INTO problems (code, title, slug, content, level, time_limit_ms, memory_limit_mb, author_id, status)
VALUES ('SQUARE', 'Tính bình phương', 'tinh-binh-phuong', 'Nhập số n, in ra n²', 'EASY', 2000, 256, 1, true);

-- Tạo test cases
INSERT INTO test_cases (problem_id, input, expected_output, is_sample, is_hidden, weight)
VALUES 
  (1, '5', '25', true, false, 1),
  (1, '10', '100', false, false, 1),
  (1, '0', '0', false, false, 1);
```

### Bước 2: Nộp Code
```bash
curl -X POST http://localhost:8080/api/v1/submissions \
  -H "Content-Type: application/json" \
  -H "Authorization: Bearer YOUR_JWT_TOKEN" \
  -d '{
    "problemId": 1,
    "languageId": 1,
    "codeContent": "import java.util.Scanner;\n\npublic class Main {\n    public static void main(String[] args) {\n        Scanner sc = new Scanner(System.in);\n        int n = sc.nextInt();\n        System.out.println(n * n);\n    }\n}"
  }'
```

**Lưu submission ID từ response** (ví dụ: `id: 1`)

### Bước 3: Đợi vài giây (Judge chạy async)
```bash
sleep 5
```

### Bước 4: Kiểm tra kết quả
```bash
curl -X GET http://localhost:8080/api/v1/submissions/1 \
  -H "Authorization: Bearer YOUR_JWT_TOKEN"
```

**Kiểm tra:**
- ✅ `state`: Từ `PENDING` → `ACCEPTED` hoặc `WRONG_ANSWER`
- ✅ `isAccepted`: `true` nếu pass tất cả test cases
- ✅ `totalCorrect`: Số test cases pass
- ✅ `totalTestcases`: Tổng số test cases
- ✅ `statusMsg`: "Accepted" hoặc "Wrong Answer (2/3)"

### Bước 5: Kiểm tra SubmissionTestcaseEntity trong DB
```sql
SELECT * FROM submission_testcases WHERE submission_id = 1;
```

**Kết quả mong đợi:**
```
submission_id | test_case_id | status  | runtime_ms | memory_kb | stdout
--------------|--------------|---------|------------|-----------|--------
1             | 1            | PASSED  | 50         | 1024      | 25
1             | 2            | PASSED  | 48         | 1024      | 100
1             | 3            | PASSED  | 45         | 1024      | 0
```

---

## 🐛 Troubleshooting

### 1. Submission mãi ở trạng thái PENDING
**Nguyên nhân:**
- Judge Service không chạy được
- Judge0 API không kết nối được
- Language không được hỗ trợ

**Kiểm tra:**
```bash
# Xem logs của server
tail -f logs/application.log

# Tìm lỗi "Error judging submission"
grep "Error judging submission" logs/application.log
```

### 2. Judge0 API Connection Error
**Kiểm tra:**
```bash
# Test kết nối đến Judge0
curl -X GET "https://judge0-ce.p.rapidapi.com/languages" \
  -H "X-RapidAPI-Key: YOUR_KEY" \
  -H "X-RapidAPI-Host: judge0-ce.p.rapidapi.com"
```

**Nếu lỗi:**
- Kiểm tra `judge0.api.url` trong `application.properties`
- Kiểm tra network/firewall
- Thử dùng public endpoint: `https://ce.judge0.com`

### 3. Language không được hỗ trợ
**Kiểm tra:** Xem `JudgeService.LANGUAGE_MAP` có language code của bạn không.

**Thêm language mới:**
```java
// Trong JudgeService.java
private static final Map<String, Integer> LANGUAGE_MAP = Map.of(
    "java", 62,
    "python", 71,
    // Thêm language mới ở đây
    "your_lang", YOUR_JUDGE0_LANGUAGE_ID
);
```

### 4. Code không compile
**Kiểm tra:**
- Code format đúng chưa (ví dụ: Java cần class Main)
- Language code đúng chưa
- Xem `compileError` trong response

---

## 📝 Postman Collection

Tạo Postman Collection với các requests:

1. **Login** → Lấy JWT token
2. **Create Submission** → Nộp code
3. **Get Submission** → Xem kết quả (poll mỗi 2 giây)
4. **Validate Code** → Test syntax
5. **Get My Submissions** → Xem lịch sử

---

## ✅ Checklist Test

- [ ] Judge0 API connection OK
- [ ] Code validation API hoạt động
- [ ] Submission được tạo với state PENDING
- [ ] Judge Service chạy async và cập nhật kết quả
- [ ] SubmissionTestcaseEntity được tạo đúng
- [ ] Kết quả chính xác (PASSED/FAILED)
- [ ] Score và isAccepted được tính đúng

---

## 🚀 Quick Test Script

Tạo file `test_submission.sh`:

```bash
#!/bin/bash

TOKEN="YOUR_JWT_TOKEN"
BASE_URL="http://localhost:8080/api/v1"

echo "1. Testing Code Validation..."
curl -X POST "$BASE_URL/code/validate" \
  -H "Content-Type: application/json" \
  -H "Authorization: Bearer $TOKEN" \
  -d '{"codeContent": "print(\"Hello\")", "languageCode": "python"}'

echo -e "\n\n2. Creating Submission..."
SUBMISSION_RESPONSE=$(curl -s -X POST "$BASE_URL/submissions" \
  -H "Content-Type: application/json" \
  -H "Authorization: Bearer $TOKEN" \
  -d '{
    "problemId": 1,
    "languageId": 1,
    "codeContent": "import java.util.Scanner; public class Main { public static void main(String[] args) { Scanner sc = new Scanner(System.in); int n = sc.nextInt(); System.out.println(n * n); } }"
  }')

SUBMISSION_ID=$(echo $SUBMISSION_RESPONSE | jq -r '.data.id')
echo "Submission ID: $SUBMISSION_ID"

echo -e "\n3. Waiting 5 seconds for judge..."
sleep 5

echo -e "\n4. Checking result..."
curl -X GET "$BASE_URL/submissions/$SUBMISSION_ID" \
  -H "Authorization: Bearer $TOKEN" | jq
```

Chạy: `chmod +x test_submission.sh && ./test_submission.sh`

