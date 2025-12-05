# Judge Service & Code Validation Guide

## Tổng quan

Hệ thống đã được tích hợp **Judge Service** sử dụng **Judge0 API** để:
1. **Chạy code** và kiểm tra với test cases (tự động khi user nộp bài)
2. **Validate code syntax** real-time trong editor (khi user đang gõ code)

## Judge0 API

### Ưu điểm:
- ✅ **Miễn phí** (có giới hạn requests)
- ✅ **Hỗ trợ 60+ ngôn ngữ**: Java, Python, C++, C, JavaScript, Go, Rust, Kotlin, v.v.
- ✅ **Nhanh**: Response time ~1-3 giây
- ✅ **An toàn**: Code chạy trong sandbox, không ảnh hưởng server

### Cách sử dụng:

#### Option 1: Dùng Public API (Miễn phí, có giới hạn)
```properties
judge0.api.url=https://judge0-ce.p.rapidapi.com
judge0.api.key=  # Để trống
```

#### Option 2: Dùng RapidAPI (Cần đăng ký)
1. Đăng ký tại: https://rapidapi.com/judge0-official/api/judge0-ce
2. Lấy API key
3. Cập nhật `application.properties`:
```properties
judge0.api.url=https://judge0-ce.p.rapidapi.com
judge0.api.key=YOUR_RAPIDAPI_KEY
```

#### Option 3: Self-hosted (Khuyến nghị cho production)
1. Deploy Judge0: https://github.com/judge0/judge0
2. Cập nhật `application.properties`:
```properties
judge0.api.url=http://your-judge0-server:2358
judge0.api.key=  # Để trống nếu không cần auth
```

## Các ngôn ngữ được hỗ trợ

Hiện tại đã map các ngôn ngữ sau (có thể thêm trong `JudgeService.LANGUAGE_MAP`):

| Language Code | Judge0 ID | Ngôn ngữ |
|--------------|-----------|----------|
| java | 62 | Java (OpenJDK 13.0.1) |
| python | 71 | Python (3.8.1) |
| cpp | 54 | C++ (GCC 9.2.0) |
| c | 50 | C (GCC 9.2.0) |
| javascript | 63 | Node.js (12.14.0) |
| go | 60 | Go (1.13.5) |
| rust | 73 | Rust (1.40.0) |
| kotlin | 78 | Kotlin (1.3.70) |

Xem danh sách đầy đủ: https://ce.judge0.com/languages

## API Endpoints

### 1. Nộp bài (Submission)
```
POST /api/v1/submissions
```

**Request:**
```json
{
  "problemId": 1,
  "languageId": 1,
  "codeContent": "public class Main {\n    public static void main(String[] args) {\n        System.out.println(\"Hello\");\n    }\n}"
}
```

**Flow:**
1. Tạo submission với status `PENDING`
2. Judge Service tự động chạy code với tất cả test cases (async)
3. Cập nhật kết quả vào `SubmissionEntity` và `SubmissionTestcaseEntity`
4. User có thể poll hoặc dùng WebSocket để lấy kết quả real-time

### 2. Validate Code (Real-time trong editor)
```
POST /api/v1/code/validate
```

**Request:**
```json
{
  "codeContent": "public class Main {\n    public static void main(String[] args) {\n        System.out.println(\"Hello\");\n    }\n}",
  "languageCode": "java"
}
```

**Response:**
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

**Hoặc nếu có lỗi:**
```json
{
  "success": true,
  "data": {
    "valid": false,
    "message": "Compilation Error",
    "errors": [
      "Main.java:2: error: ';' expected\n    System.out.println(\"Hello\")\n                              ^"
    ]
  }
}
```

## Tích hợp với Frontend Editor

### 1. Real-time Code Validation

Trong editor (ví dụ: Monaco Editor, CodeMirror), gọi API khi user gõ code:

```javascript
// Debounce để không gọi quá nhiều
let validationTimeout;
editor.onDidChangeModelContent(() => {
  clearTimeout(validationTimeout);
  validationTimeout = setTimeout(() => {
    validateCode();
  }, 1000); // Chờ 1 giây sau khi user ngừng gõ
});

async function validateCode() {
  const code = editor.getValue();
  const languageCode = getSelectedLanguage(); // "java", "python", etc.
  
  try {
    const response = await fetch('/api/v1/code/validate', {
      method: 'POST',
      headers: {
        'Content-Type': 'application/json',
        'Authorization': `Bearer ${token}`
      },
      body: JSON.stringify({
        codeContent: code,
        languageCode: languageCode
      })
    });
    
    const result = await response.json();
    
    if (result.success && result.data) {
      if (result.data.valid) {
        // Hiển thị success indicator
        showSuccessMessage("Code compiles successfully");
      } else {
        // Hiển thị lỗi
        showErrors(result.data.errors);
      }
    }
  } catch (error) {
    console.error('Validation error:', error);
  }
}
```

### 2. Hiển thị lỗi trong editor

```javascript
// Monaco Editor example
function showErrors(errors) {
  const markers = errors.map((error, index) => {
    // Parse error message để lấy line number
    const match = error.match(/line (\d+)/);
    const line = match ? parseInt(match[1]) - 1 : 0;
    
    return {
      severity: monaco.MarkerSeverity.Error,
      startLineNumber: line,
      startColumn: 1,
      endLineNumber: line,
      endColumn: 100,
      message: error
    };
  });
  
  monaco.editor.setModelMarkers(editor.getModel(), 'validation', markers);
}
```

## Cấu trúc Database

### SubmissionTestcaseEntity
Lưu kết quả chi tiết của từng test case:

```sql
submission_testcases:
  - submission_id (FK)
  - test_case_id (FK)
  - status: "PASSED" | "FAILED"
  - runtime_ms: Thời gian chạy (ms)
  - memory_kb: Bộ nhớ sử dụng (KB)
  - stdout: Output của code
  - stderr: Lỗi (nếu có)
```

## Troubleshooting

### 1. Judge Service không chạy
- Kiểm tra `judge0.api.url` trong `application.properties`
- Kiểm tra network connection đến Judge0 API
- Xem logs trong console

### 2. Code không compile
- Kiểm tra language code có đúng không
- Kiểm tra code format (có thể cần thêm boilerplate code)
- Xem `compileOutput` trong response

### 3. Timeout
- Tăng `timeLimitMs` trong Problem
- Kiểm tra code có vòng lặp vô hạn không

## Performance

- **Async Processing**: Judge chạy async, không block request
- **Caching**: Có thể cache kết quả validation cho code giống nhau
- **Rate Limiting**: Judge0 có giới hạn requests, nên implement rate limiting

## Security

- ✅ Code chạy trong sandbox (Judge0)
- ✅ Time limit và memory limit được enforce
- ✅ Không có quyền truy cập file system
- ⚠️ Vẫn nên validate input trước khi gửi đến Judge0

