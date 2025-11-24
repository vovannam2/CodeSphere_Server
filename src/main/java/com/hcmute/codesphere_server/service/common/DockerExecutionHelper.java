package com.hcmute.codesphere_server.service.common;

import com.github.dockerjava.api.DockerClient;
import com.github.dockerjava.api.async.ResultCallback;
import com.github.dockerjava.api.command.CreateContainerResponse;
import com.github.dockerjava.api.command.WaitContainerResultCallback;
import com.github.dockerjava.api.model.*;
import lombok.Data;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.List;
import java.util.concurrent.TimeUnit;

/**
 * Helper class để chạy code trong Docker containers
 * Hỗ trợ: Java, Python, C++, C, JavaScript
 * Sử dụng volume mount để copy code vào container (giống ITenv-Server)
 */
@Slf4j
@Component
@RequiredArgsConstructor
public class DockerExecutionHelper {

    private final DockerClient dockerClient;

    @Value("${docker.execution.timeout:10}")
    private int executionTimeoutSeconds;

    @Value("${docker.execution.memory-limit:512}")
    private long memoryLimitMB;

    @Value("${docker.execution.cpu-shares:512}")
    private int cpuShares;

    // Mapping language code sang Docker image và command
    // Lưu ý: Khi mount thư mục temp vào /src, file sẽ ở /src/filename (không có /temp)
    private static final java.util.Map<String, LanguageConfig> LANGUAGE_CONFIGS = java.util.Map.of(
            "java", new LanguageConfig("eclipse-temurin:17-jdk", "Main.java", 
                    "javac /src/Main.java && java -cp /src Main"),
            "python", new LanguageConfig("python:3.11-alpine", "main.py", 
                    "python3 /src/main.py"),
            "cpp", new LanguageConfig("gcc:latest", "main.cpp", 
                    "g++ /src/main.cpp -o /src/main && /src/main"),
            "c", new LanguageConfig("gcc:latest", "main.c", 
                    "gcc /src/main.c -o /src/main && /src/main"),
            "javascript", new LanguageConfig("node:18-alpine", "main.js", 
                    "node /src/main.js")
    );

    @Data
    private static class LanguageConfig {
        final String image;
        final String filename;
        final String runCommand;
    }

    /**
     * Kết quả chạy code
     */
    @Data
    public static class ExecutionResult {
        private boolean success;
        private String stdout;
        private String stderr;
        private long runtimeMs;
        private long memoryKb;
        private String errorMessage;
        private int exitCode;

        public static ExecutionResult success(String stdout, long runtimeMs, long memoryKb) {
            ExecutionResult result = new ExecutionResult();
            result.success = true;
            result.stdout = stdout;
            result.runtimeMs = runtimeMs;
            result.memoryKb = memoryKb;
            result.exitCode = 0;
            return result;
        }

        public static ExecutionResult error(String stderr, String errorMessage, int exitCode) {
            ExecutionResult result = new ExecutionResult();
            result.success = false;
            result.stderr = stderr;
            result.errorMessage = errorMessage;
            result.exitCode = exitCode;
            return result;
        }
    }

    /**
     * Chỉ compile code (không chạy) - dùng cho validation
     * Chỉ hỗ trợ compiled languages: C++, C, Java
     */
    public ExecutionResult compileCode(String sourceCode, String languageCode,
                                       Integer timeLimitMs, Integer memoryLimitMb) {
        String lang = languageCode.toLowerCase();
        LanguageConfig config = LANGUAGE_CONFIGS.get(lang);

        if (config == null) {
            return ExecutionResult.error("", "Language not supported: " + languageCode, -1);
        }

        // Chỉ hỗ trợ compiled languages
        if (!lang.equals("cpp") && !lang.equals("c") && !lang.equals("java")) {
            return ExecutionResult.error("", "Compile-only validation only supports C++, C, and Java", -1);
        }

        log.debug("🔨 Compiling {} code in Docker container (validation)", lang);

        String containerId = null;
        Path codeFile = null;
        try {
            // 1. Đảm bảo image tồn tại
            ensureImageExists(config.image);

            // 2. Tạo file code tạm trên host
            Path tempDir = Paths.get(System.getProperty("user.dir"), "src", "temp");
            Files.createDirectories(tempDir);
            codeFile = tempDir.resolve(config.filename);
            Files.writeString(codeFile, sourceCode);
            String hostPath = codeFile.toAbsolutePath().toString();

            // 3. Tạo compile command (chỉ compile, không chạy)
            String compileCommand;
            if (lang.equals("java")) {
                compileCommand = "javac /src/Main.java";
            } else if (lang.equals("cpp")) {
                compileCommand = "g++ /src/main.cpp -o /src/main";
            } else { // c
                compileCommand = "gcc /src/main.c -o /src/main";
            }

            // 4. Tạo container với volume mount
            long memoryBytes = (memoryLimitMb != null ? memoryLimitMb : 256L) * 1024 * 1024;
            int timeoutSeconds = (timeLimitMs != null ? timeLimitMs : 5000) / 1000 + 1;

            containerId = createContainerWithMount(config.image, compileCommand, 
                    hostPath, memoryBytes, timeoutSeconds);

            // 5. Khởi động container và compile
            long startTime = System.currentTimeMillis();
            ExecutionResult result = executeInContainer(containerId, timeoutSeconds);
            long endTime = System.currentTimeMillis();
            result.runtimeMs = endTime - startTime;

            // 6. Dọn dẹp container và file tạm
            cleanupContainer(containerId);
            Files.deleteIfExists(codeFile);

            return result;

        } catch (Exception e) {
            log.error("❌ Error compiling code: {}", e.getMessage(), e);
            if (containerId != null) {
                cleanupContainer(containerId);
            }
            if (codeFile != null) {
                try {
                    Files.deleteIfExists(codeFile);
                } catch (IOException ex) {
                    log.warn("Failed to delete temp file: {}", codeFile);
                }
            }
            return ExecutionResult.error("", "Compilation error: " + e.getMessage(), -1);
        }
    }

    /**
     * Chạy code với input và trả về kết quả
     */
    public ExecutionResult runCode(String sourceCode, String languageCode, String input,
                                   Integer timeLimitMs, Integer memoryLimitMb) {
        String lang = languageCode.toLowerCase();
        LanguageConfig config = LANGUAGE_CONFIGS.get(lang);

        if (config == null) {
            return ExecutionResult.error("", "Language not supported: " + languageCode, -1);
        }

        log.debug("🐳 Running {} code in Docker container", lang);
        log.debug("Image: {}, File: {}", config.image, config.filename);

        String containerId = null;
        Path codeFile = null;
        Path inputFile = null;
        try {
            // 1. Đảm bảo image tồn tại
            ensureImageExists(config.image);

            // 2. Tạo file code và input tạm trên host
            Path tempDir = Paths.get(System.getProperty("user.dir"), "src", "temp");
            Files.createDirectories(tempDir);
            codeFile = tempDir.resolve(config.filename);
            Files.writeString(codeFile, sourceCode);
            String hostPath = codeFile.toAbsolutePath().toString();
            
            // Tạo file input nếu có và sửa command để đọc từ file
            String actualCommand = config.runCommand;
            if (input != null && !input.isEmpty()) {
                inputFile = tempDir.resolve("input.txt");
                Files.writeString(inputFile, input);
                // Redirect input chỉ cho phần chạy program (sau &&), không phải phần compile
                // Ví dụ: g++ /src/main.cpp -o /src/main && /src/main < /src/input.txt
                if (actualCommand.contains(" && ")) {
                    String[] parts = actualCommand.split(" && ", 2);
                    actualCommand = parts[0] + " && " + parts[1] + " < /src/input.txt";
                } else {
                    // Nếu không có &&, redirect cho toàn bộ command
                    actualCommand = actualCommand + " < /src/input.txt";
                }
            }

            // 3. Tạo container với volume mount
            long memoryBytes = (memoryLimitMb != null ? memoryLimitMb : 256L) * 1024 * 1024;
            int timeoutSeconds = (timeLimitMs != null ? timeLimitMs : 2000) / 1000 + 1;

            containerId = createContainerWithMount(config.image, actualCommand, 
                    hostPath, memoryBytes, timeoutSeconds);

            // 4. Khởi động container và chạy code
            long startTime = System.currentTimeMillis();
            ExecutionResult result = executeInContainer(containerId, timeoutSeconds);
            long endTime = System.currentTimeMillis();
            result.runtimeMs = endTime - startTime;

            // 5. Dọn dẹp container và file tạm
            cleanupContainer(containerId);
            Files.deleteIfExists(codeFile);
            if (inputFile != null) {
                Files.deleteIfExists(inputFile);
            }

            return result;

        } catch (Exception e) {
            log.error("❌ Error running code: {}", e.getMessage(), e);
            if (containerId != null) {
                cleanupContainer(containerId);
            }
            if (codeFile != null) {
                try {
                    Files.deleteIfExists(codeFile);
                } catch (IOException ex) {
                    log.warn("Failed to delete temp file: {}", codeFile);
                }
            }
            if (inputFile != null) {
                try {
                    Files.deleteIfExists(inputFile);
                } catch (IOException ex) {
                    log.warn("Failed to delete input file: {}", inputFile);
                }
            }
            return ExecutionResult.error("", "Execution error: " + e.getMessage(), -1);
        }
    }

    /**
     * Đảm bảo Docker image tồn tại, nếu không thì pull
     */
    private void ensureImageExists(String image) throws InterruptedException {
        try {
            List<Image> images = dockerClient.listImagesCmd()
                    .withImageNameFilter(image)
                    .exec();
            
            if (images.isEmpty()) {
                log.info("📥 Pulling Docker image: {} (this may take a while...)", image);
                dockerClient.pullImageCmd(image)
                        .start()
                        .awaitCompletion();
                log.info("✅ Image pulled successfully: {}", image);
            } else {
                log.debug("✅ Image already exists: {}", image);
            }
        } catch (Exception e) {
            log.error("❌ Error ensuring image exists: {}", e.getMessage());
            throw new RuntimeException("Failed to ensure image exists: " + image, e);
        }
    }

    /**
     * Convert Windows path sang format Docker hiểu được
     * Trên Windows, Docker Desktop có thể mount trực tiếp Windows path
     * Nếu không hoạt động, có thể cần convert sang format /d/... hoặc /mnt/d/...
     */
    private String convertToDockerPath(String path) {
        // Trên Windows, Docker Desktop có thể mount trực tiếp Windows path
        // Nếu path có dạng D:\... hoặc C:\..., giữ nguyên
        // Docker Desktop sẽ tự động xử lý
        if (path.contains(":") && path.contains("\\")) {
            // Windows path - Docker Desktop có thể mount trực tiếp
            // Hoặc convert sang format /d/... nếu cần
            // Tạm thời giữ nguyên để test
            return path;
        }
        // Unix path - giữ nguyên
        return path;
    }

    /**
     * Tạo container với volume mount từ host
     */
    private String createContainerWithMount(String image, String runCommand, 
                                           String hostPath, long memoryBytes, int timeoutSeconds) {
        try {
            // Mount thư mục chứa code từ host vào /src trong container
            String containerPath = "/src";
            
            // Lấy thư mục chứa file (xử lý cả Windows và Unix path)
            Path filePath = Paths.get(hostPath);
            Path parentDir = filePath.getParent();
            if (parentDir == null) {
                throw new IllegalArgumentException("Invalid file path: " + hostPath);
            }
            String hostDir = convertToDockerPath(parentDir.toAbsolutePath().toString());
            
            log.debug("Mounting host directory: {} -> {}", hostDir, containerPath);
            Bind bind = new Bind(hostDir, new Volume(containerPath));

            CreateContainerResponse container = dockerClient.createContainerCmd(image)
                    .withCmd("sh", "-c", runCommand)
                    .withWorkingDir(containerPath)
                    .withAttachStdout(true)
                    .withAttachStderr(true)
                    .withTty(false)
                    .withHostConfig(HostConfig.newHostConfig()
                            .withBinds(bind)
                            .withMemory(memoryBytes)
                            .withCpuShares(cpuShares)
                            .withNetworkMode("none") // Mạng bị cô lập
                            .withAutoRemove(false)) // Xóa thủ công
                    .exec();

            log.debug("✅ Container created: {}", container.getId());
            return container.getId();
        } catch (Exception e) {
            log.error("❌ Error creating container: {}", e.getMessage());
            throw new RuntimeException("Failed to create container", e);
        }
    }

    /**
     * Chạy code trong container (input đã được mount vào file)
     */
    private ExecutionResult executeInContainer(String containerId, int timeoutSeconds) {
        ByteArrayOutputStream stdout = new ByteArrayOutputStream();
        ByteArrayOutputStream stderr = new ByteArrayOutputStream();
        
        try {
            // Khởi động container
            dockerClient.startContainerCmd(containerId).exec();

            // Tạo callback để đọc output
            ResultCallback.Adapter<Frame> attachCallback = new ResultCallback.Adapter<Frame>() {
                @Override
                public void onNext(Frame frame) {
                    try {
                        if (frame.getStreamType() == StreamType.STDOUT) {
                            stdout.write(frame.getPayload());
                        } else if (frame.getStreamType() == StreamType.STDERR) {
                            stderr.write(frame.getPayload());
                        }
                    } catch (IOException e) {
                        log.error("Lỗi khi đọc output", e);
                    }
                }
            };

            // Attach container để đọc output
            dockerClient.attachContainerCmd(containerId)
                    .withStdOut(true)
                    .withStdErr(true)
                    .withFollowStream(true)
                    .exec(attachCallback);

            // Chờ container hoàn thành (với timeout)
            WaitContainerResultCallback waitCallback = new WaitContainerResultCallback();
            dockerClient.waitContainerCmd(containerId).exec(waitCallback);
            
            // Chờ mã thoát với timeout
            Integer exitCode = waitCallback.awaitStatusCode(timeoutSeconds, TimeUnit.SECONDS);
            
            // Đợi một chút để đảm bảo đọc hết output
            Thread.sleep(200);

            String stdoutStr = stdout.toString().trim();
            String stderrStr = stderr.toString().trim();

            if (exitCode != null && exitCode == 0 && stderrStr.isEmpty()) {
                return ExecutionResult.success(stdoutStr, 0, 0);
            } else {
                int code = exitCode != null ? exitCode : -1;
                String errorMsg = code != 0 ? "Process exited with code " + code : (stderrStr.isEmpty() ? "Unknown error" : stderrStr);
                return ExecutionResult.error(stderrStr, errorMsg, code);
            }

        } catch (Exception e) {
            log.error("❌ Lỗi khi thực thi trong container: {}", e.getMessage(), e);
            String stderrStr = stderr.toString().trim();
            return ExecutionResult.error(stderrStr, "Execution failed: " + e.getMessage(), -1);
        }
    }

    /**
     * Dọn dẹp container
     */
    private void cleanupContainer(String containerId) {
        try {
            dockerClient.stopContainerCmd(containerId).exec();
            dockerClient.removeContainerCmd(containerId).exec();
            log.debug("🧹 Container cleaned up: {}", containerId);
        } catch (Exception e) {
            log.warn("⚠️ Error cleaning up container {}: {}", containerId, e.getMessage());
            try {
                dockerClient.removeContainerCmd(containerId).withForce(true).exec();
            } catch (Exception ex) {
                log.error("❌ Failed to force remove container: {}", containerId);
            }
        }
    }
}

