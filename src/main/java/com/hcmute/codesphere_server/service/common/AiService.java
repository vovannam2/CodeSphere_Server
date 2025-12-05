package com.hcmute.codesphere_server.service.common;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.hcmute.codesphere_server.model.payload.request.ChatRequest;
import com.hcmute.codesphere_server.model.payload.request.RefactorCodeRequest;
import com.hcmute.codesphere_server.model.payload.request.ReviewCodeRequest;
import com.hcmute.codesphere_server.model.payload.response.RefactorCodeResponse;
import com.hcmute.codesphere_server.model.payload.response.ReviewCodeResponse;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpEntity;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpMethod;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Service;
import org.springframework.web.client.RestTemplate;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

@Slf4j
@Service
@RequiredArgsConstructor
public class AiService {

    private final ProblemService problemService;
    private final RestTemplate restTemplate = new RestTemplate();
    private final ObjectMapper objectMapper = new ObjectMapper();

    @Value("${openai.api.key}")
    private String openaiApiKey;

    @Value("${openai.api.url}")
    private String openaiApiUrl;

    @Value("${openai.api.model}")
    private String openaiApiModel;

    @Value("${openai.api.temperature:0.7}")
    private Double temperature;

    @Value("${openai.api.max-tokens:4096}")
    private Integer maxTokens;

    public RefactorCodeResponse refactorCode(RefactorCodeRequest request) {
        try {
            // Lấy thông tin problem để tạo context tốt hơn
            String problemContext = "";
            try {
                var problem = problemService.getProblemById(request.getProblemId());
                problemContext = String.format(
                    "Problem: %s\nDescription: %s\nConstraints: Time limit: %dms, Memory: %dMB",
                    problem.getTitle(),
                    problem.getContent() != null ? problem.getContent().substring(0, Math.min(500, problem.getContent().length())) : "",
                    problem.getTimeLimitMs(),
                    problem.getMemoryLimitMb()
                );
            } catch (Exception e) {
                log.warn("Could not fetch problem context: {}", e.getMessage());
            }

            // Tạo system prompt cho refactoring
            String systemPrompt;
            String userMessage;
            
            // Nếu có suggestions cụ thể, tối ưu prompt để chỉ refactor theo suggestions đó
            if (request.getSuggestions() != null && !request.getSuggestions().isEmpty()) {
                String suggestionsText = String.join("\n\n", request.getSuggestions());
                systemPrompt = String.format(
                    "Bạn là một trợ lý refactor code chuyên nghiệp. Nhiệm vụ của bạn là refactor code CHỈ theo các gợi ý cụ thể sau:\n\n" +
                    "Các gợi ý cần refactor:\n%s\n\n" +
                    "Yêu cầu:\n" +
                    "1. Chỉ refactor các phần code liên quan đến các gợi ý trên\n" +
                    "2. Giữ nguyên các phần code không liên quan\n" +
                    "3. Tuân thủ best practices của ngôn ngữ %s\n" +
                    "4. Giữ nguyên logic và functionality\n" +
                    "5. Thêm comments giải thích nếu cần\n\n" +
                    "Context về problem:\n%s\n\n" +
                    "Chỉ trả về code đã refactor, không giải thích thêm.",
                    suggestionsText,
                    request.getLanguage(),
                    problemContext
                );
                userMessage = String.format(
                    "Hãy refactor code sau đây (ngôn ngữ: %s) theo các gợi ý đã nêu:\n\n```%s\n%s\n```",
                    request.getLanguage(),
                    request.getLanguage(),
                    request.getCode()
                );
            } else {
                // Refactor toàn bộ như cũ
                systemPrompt = String.format(
                    "Bạn là một trợ lý refactor code chuyên nghiệp. Nhiệm vụ của bạn là refactor code đã cho để:\n" +
                    "1. Cải thiện khả năng đọc và maintainability\n" +
                    "2. Tối ưu hiệu suất nếu có thể\n" +
                    "3. Tuân thủ best practices của ngôn ngữ %s\n" +
                    "4. Giữ nguyên logic và functionality\n" +
                    "5. Thêm comments giải thích nếu cần\n\n" +
                    "Context về problem:\n%s\n\n" +
                    "Chỉ trả về code đã refactor, không giải thích thêm.",
                    request.getLanguage(),
                    problemContext
                );
                userMessage = String.format(
                    "Hãy refactor code sau đây (ngôn ngữ: %s):\n\n```%s\n%s\n```",
                    request.getLanguage(),
                    request.getLanguage(),
                    request.getCode()
                );
            }

            // Gọi OpenAI API
            String refactoredCode = callOpenAIAPI(systemPrompt, userMessage);

            return RefactorCodeResponse.builder()
                    .refactoredCode(refactoredCode)
                    .build();

        } catch (Exception e) {
            log.error("Error refactoring code: {}", e.getMessage(), e);
            throw new RuntimeException("Không thể refactor code: " + e.getMessage(), e);
        }
    }

    public ReviewCodeResponse reviewCode(ReviewCodeRequest request) {
        try {
            // Lấy thông tin problem để tạo context tốt hơn
            String problemContext = "";
            try {
                var problem = problemService.getProblemById(request.getProblemId());
                problemContext = String.format(
                    "Problem: %s\nDescription: %s\nConstraints: Time limit: %dms, Memory: %dMB",
                    problem.getTitle(),
                    problem.getContent() != null ? problem.getContent().substring(0, Math.min(500, problem.getContent().length())) : "",
                    problem.getTimeLimitMs(),
                    problem.getMemoryLimitMb()
                );
            } catch (Exception e) {
                log.warn("Could not fetch problem context: {}", e.getMessage());
            }

            // Tạo system prompt cho code review
            String systemPrompt = String.format(
                "Bạn là một chuyên gia code review chuyên nghiệp. Nhiệm vụ của bạn là đánh giá code đã cho và đưa ra nhận xét chi tiết.\n\n" +
                "QUAN TRỌNG: Bạn PHẢI trả về kết quả theo format sau (mỗi mục phải bắt đầu bằng số thứ tự và **bold title**):\n\n" +
                "1. **Cách đặt tên biến và hàm**\n" +
                "[Nội dung đánh giá về cách đặt tên biến và hàm]\n\n" +
                "2. **Cấu trúc code**\n" +
                "[Nội dung đánh giá về cấu trúc code]\n\n" +
                "3. **Best practices**\n" +
                "[Nội dung đánh giá về best practices của ngôn ngữ %s]\n\n" +
                "4. **Hiệu suất**\n" +
                "[Nội dung đánh giá về hiệu suất, nếu có vấn đề]\n\n" +
                "5. **Điểm mạnh**\n" +
                "[Những điểm tốt của code]\n\n" +
                "6. **Điểm cần cải thiện**\n" +
                "[Những điểm cần cải thiện và gợi ý cụ thể]\n\n" +
                "Context về problem:\n%s\n\n" +
                "Hãy đánh giá một cách chi tiết, khách quan và hữu ích. Trả lời bằng tiếng Việt. " +
                "Đảm bảo mỗi mục bắt đầu bằng số thứ tự và **bold title**.",
                request.getLanguage(),
                problemContext
            );

            // Tạo user message
            String userMessage = String.format(
                "Hãy đánh giá code sau đây (ngôn ngữ: %s) theo format đã yêu cầu:\n\n```%s\n%s\n```",
                request.getLanguage(),
                request.getLanguage(),
                request.getCode()
            );

            // Gọi OpenAI API
            String review = callOpenAIAPI(systemPrompt, userMessage);

            return ReviewCodeResponse.builder()
                    .review(review)
                    .build();

        } catch (Exception e) {
            log.error("Error reviewing code: {}", e.getMessage(), e);
            throw new RuntimeException("Không thể đánh giá code: " + e.getMessage(), e);
        }
    }

    public String chatWithContext(ChatRequest request) {
        try {
            String systemPrompt;
            String userMessage = request.getMessage();

            if ("problem".equals(request.getContext()) && request.getProblemId() != null) {
                // Chat với context của problem
                try {
                    var problem = problemService.getProblemById(request.getProblemId());
                    systemPrompt = String.format(
                        "Bạn là một trợ lý coding chuyên giúp giải các bài competitive programming.\n\n" +
                        "Context về problem hiện tại:\n" +
                        "- Title: %s\n" +
                        "- Description: %s\n" +
                        "- Constraints: Time limit: %dms, Memory: %dMB\n" +
                        "- Language: %s\n" +
                        "- Current code: %s\n\n" +
                        "Hãy trả lời câu hỏi của user dựa trên context này. Nếu user hỏi về code, hãy phân tích code hiện tại.",
                        problem.getTitle(),
                        problem.getContent() != null ? problem.getContent().substring(0, Math.min(1000, problem.getContent().length())) : "",
                        problem.getTimeLimitMs(),
                        problem.getMemoryLimitMb(),
                        request.getLanguage() != null ? request.getLanguage() : "N/A",
                        request.getCode() != null ? request.getCode().substring(0, Math.min(500, request.getCode().length())) : "Chưa có code"
                    );
                } catch (Exception e) {
                    log.warn("Could not fetch problem context: {}", e.getMessage());
                    systemPrompt = "Bạn là một trợ lý coding chuyên giúp giải các bài competitive programming.";
                }
            } else {
                // Chat tổng quát
                systemPrompt = "Bạn là một trợ lý coding chuyên nghiệp. Hãy giúp user với các câu hỏi về lập trình, algorithms, data structures, và best practices.";
            }

            return callOpenAIAPI(systemPrompt, userMessage);

        } catch (Exception e) {
            log.error("Error in chat: {}", e.getMessage(), e);
            throw new RuntimeException("Không thể xử lý chat: " + e.getMessage(), e);
        }
    }

    private String callOpenAIAPI(String systemPrompt, String userMessage) {
        try {
            log.info("Calling OpenAI API with model: {}", openaiApiModel);
            log.debug("System prompt length: {}, User message length: {}", 
                    systemPrompt != null ? systemPrompt.length() : 0,
                    userMessage != null ? userMessage.length() : 0);
            
            // Validate API key
            if (openaiApiKey == null || openaiApiKey.isEmpty()) {
                log.error("OpenAI API key is not configured!");
                throw new RuntimeException("OpenAI API key chưa được cấu hình. Vui lòng set biến môi trường OPENAI_API_KEY");
            }
            
            HttpHeaders headers = new HttpHeaders();
            headers.setContentType(MediaType.APPLICATION_JSON);
            headers.set("Authorization", "Bearer " + openaiApiKey);

            // Build messages array for OpenAI API
            List<Map<String, Object>> messagesList = new ArrayList<>();
            
            // Add system message if provided
            if (systemPrompt != null && !systemPrompt.isEmpty()) {
                Map<String, Object> systemMessage = new HashMap<>();
                systemMessage.put("role", "system");
                systemMessage.put("content", systemPrompt);
                messagesList.add(systemMessage);
            }
            
            // Add user message
            Map<String, Object> userMessageMap = new HashMap<>();
            userMessageMap.put("role", "user");
            userMessageMap.put("content", userMessage);
            messagesList.add(userMessageMap);
            
            // Build request body for OpenAI API
            Map<String, Object> requestBody = new HashMap<>();
            requestBody.put("model", openaiApiModel);
            requestBody.put("messages", messagesList);
            requestBody.put("temperature", temperature);
            requestBody.put("max_tokens", maxTokens);

            String requestBodyJson = objectMapper.writeValueAsString(requestBody);
            log.debug("OpenAI API request body: {}", requestBodyJson);

            HttpEntity<String> entity = new HttpEntity<>(requestBodyJson, headers);

            log.info("Sending request to OpenAI API: {}", openaiApiUrl);
            ResponseEntity<String> response = restTemplate.exchange(
                    openaiApiUrl,
                    HttpMethod.POST,
                    entity,
                    String.class
            );

            log.info("OpenAI API response status: {}", response.getStatusCode());
            
            if (!response.getStatusCode().is2xxSuccessful()) {
                log.error("OpenAI API error response: {}", response.getBody());
                throw new RuntimeException("OpenAI API returned error: " + response.getStatusCode() + " - " + response.getBody());
            }
            
            log.debug("OpenAI API response body: {}", response.getBody());

            // Parse response từ OpenAI API
            JsonNode jsonResponse = objectMapper.readTree(response.getBody());
            
            // Check for error in response
            if (jsonResponse.has("error")) {
                JsonNode error = jsonResponse.get("error");
                String errorMessage = error.has("message") ? error.get("message").asText() : error.toString();
                log.error("OpenAI API error: {}", errorMessage);
                throw new RuntimeException("OpenAI API error: " + errorMessage);
            }
            
            // Parse OpenAI response format: {choices: [{message: {content: "..."}}]}
            JsonNode choices = jsonResponse.path("choices");
            
            if (choices.isArray() && choices.size() > 0) {
                JsonNode firstChoice = choices.get(0);
                JsonNode message = firstChoice.path("message");
                if (message.has("content")) {
                    String responseText = message.get("content").asText();
                    log.info("OpenAI API response text length: {}", responseText.length());
                    return responseText;
                }
            }

            log.error("Invalid response from OpenAI API. Response: {}", response.getBody());
            throw new RuntimeException("Invalid response from OpenAI API: " + response.getBody());

        } catch (org.springframework.web.client.HttpClientErrorException e) {
            log.error("HTTP error calling OpenAI API: {} - {}", e.getStatusCode(), e.getResponseBodyAsString());
            
            // Try to parse error message from OpenAI API
            String errorMessage = "Lỗi khi gọi OpenAI API";
            try {
                String responseBody = e.getResponseBodyAsString();
                if (responseBody != null && responseBody.contains("error")) {
                    JsonNode errorJson = objectMapper.readTree(responseBody);
                    if (errorJson.has("error")) {
                        JsonNode error = errorJson.get("error");
                        if (error.has("message")) {
                            String apiErrorMessage = error.get("message").asText();
                            
                            // Translate common errors to Vietnamese
                            if (apiErrorMessage.contains("insufficient_quota") || apiErrorMessage.contains("You exceeded your current quota")) {
                                errorMessage = "Tài khoản OpenAI API không đủ credits. Vui lòng nạp thêm credits tại https://platform.openai.com/account/billing";
                            } else if (apiErrorMessage.contains("Invalid API key") || apiErrorMessage.contains("Incorrect API key")) {
                                errorMessage = "API key không hợp lệ. Vui lòng kiểm tra lại biến môi trường OPENAI_API_KEY";
                            } else if (apiErrorMessage.contains("rate limit")) {
                                errorMessage = "Đã vượt quá giới hạn requests. Vui lòng thử lại sau.";
                            } else {
                                errorMessage = "Lỗi từ OpenAI API: " + apiErrorMessage;
                            }
                        }
                    }
                }
            } catch (Exception parseEx) {
                log.warn("Could not parse error response: {}", parseEx.getMessage());
                errorMessage = "Lỗi khi gọi OpenAI API: " + e.getStatusCode() + " - " + e.getResponseBodyAsString();
            }
            
            throw new RuntimeException(errorMessage);
        } catch (Exception e) {
            log.error("Error calling OpenAI API: {}", e.getMessage(), e);
            if (e.getCause() != null) {
                log.error("Cause: {}", e.getCause().getMessage());
            }
            throw new RuntimeException("Lỗi khi gọi OpenAI API: " + e.getMessage(), e);
        }
    }
}

