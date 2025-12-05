# 🐳 Giải thích: Docker trong CodeSphere Server

## ❌ KHÔNG CẦN docker-compose!

**Quan trọng:** Bạn **KHÔNG cần** tạo docker-compose.yml hay chạy Spring Boot trong Docker.

## ✅ Cách hoạt động thực tế:

### 1. Spring Boot chạy BÌNH THƯỜNG trên máy host

```
┌─────────────────────────────────┐
│  Máy tính của bạn (Host)        │
│                                  │
│  ┌──────────────────────────┐  │
│  │  Spring Boot Application  │  │ ← Chạy BÌNH THƯỜNG
│  │  (Port 8080)              │  │   Không cần Docker!
│  └──────────────────────────┘  │
│                                  │
│  ┌──────────────────────────┐  │
│  │  Docker Engine            │  │ ← Chỉ dùng để chạy code
│  │  (đang chạy)              │  │
│  └──────────────────────────┘  │
└─────────────────────────────────┘
```

### 2. Docker chỉ dùng để CHẠY CODE SUBMISSIONS

Khi user submit code, Spring Boot sẽ:

```
User Submit Code
    ↓
Spring Boot nhận request
    ↓
Tạo Docker container TẠM THỜI
    ↓
Copy code vào container
    ↓
Chạy code trong container
    ↓
Lấy kết quả (stdout, stderr)
    ↓
XÓA container (cleanup)
    ↓
Trả kết quả về user
```

## 📋 So sánh:

| Cách hiểu SAI ❌ | Cách đúng ✅ |
|------------------|-------------|
| Cần docker-compose.yml | **KHÔNG cần** docker-compose |
| Chạy Spring Boot trong Docker | Spring Boot chạy **BÌNH THƯỜNG** trên host |
| Tạo images trong docker-compose | Chỉ cần **pull images** từ Docker Hub |
| Start containers bằng docker-compose | Containers được tạo **TỰ ĐỘNG** khi submit code |

## 🚀 Cách chạy thực tế:

### Bước 1: Đảm bảo Docker đang chạy
```bash
# Kiểm tra Docker
docker ps
```

### Bước 2: Pull Docker images (chỉ cần 1 lần)
```bash
docker pull eclipse-temurin:17-jdk
docker pull python:3.11-alpine
docker pull gcc:latest
docker pull node:18-alpine
```

**Lưu ý:** Chỉ cần pull images này. Spring Boot sẽ tự động tạo containers từ các images này khi cần.

### Bước 3: Chạy Spring Boot BÌNH THƯỜNG
```bash
# Chạy như mọi khi, KHÔNG cần Docker!
mvn spring-boot:run
```

### Bước 4: Test submission
Khi user submit code qua API:
- Spring Boot tự động tạo container từ image đã pull
- Chạy code trong container
- Xóa container sau khi xong

## 🔍 Ví dụ cụ thể:

### Khi user submit code C++:

```java
// 1. User gửi code qua API
POST /api/v1/submissions
{
  "codeContent": "#include <iostream>...",
  "languageCode": "cpp",
  "problemId": 1
}

// 2. Spring Boot xử lý (trong JudgeService)
dockerExecutionHelper.runCode(...)
    ↓
// 3. Tạo container TẠM THỜI
docker create container from gcc:latest
    ↓
// 4. Copy code vào container
// 5. Chạy: g++ main.cpp && ./main
// 6. Lấy output
// 7. XÓA container
    ↓
// 8. Trả kết quả về user
```

## 📁 Cấu trúc thực tế:

```
CodeSphere_Server/
├── src/
│   └── main/
│       └── java/
│           └── JudgeService.java      ← Gọi Docker API
│           └── DockerExecutionHelper.java  ← Tạo containers
│
└── src/temp/                          ← Thư mục tạm cho code files
    └── main.cpp (tạo tự động)
    └── main.py (tạo tự động)
    └── Main.java (tạo tự động)
```

## 🎯 Tóm lại:

1. ✅ **Spring Boot chạy BÌNH THƯỜNG** trên máy host
2. ✅ **Docker chỉ là công cụ** để chạy code submissions
3. ✅ **Containers được tạo TỰ ĐỘNG** khi cần, rồi tự xóa
4. ❌ **KHÔNG cần** docker-compose
5. ❌ **KHÔNG cần** chạy Spring Boot trong Docker

## 💡 Tại sao dùng Docker?

- **An toàn:** Code chạy trong container cô lập
- **Kiểm soát:** Giới hạn memory, CPU, timeout
- **Đa ngôn ngữ:** Mỗi ngôn ngữ có image riêng
- **Dễ dọn dẹp:** Container tự xóa sau khi chạy xong

