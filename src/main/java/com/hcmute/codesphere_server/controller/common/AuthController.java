package com.hcmute.codesphere_server.controller.common;

import com.hcmute.codesphere_server.model.payload.request.LoginRequest;
import com.hcmute.codesphere_server.model.payload.request.RegisterRequest;
import com.hcmute.codesphere_server.model.payload.response.AuthResponse;
import com.hcmute.codesphere_server.model.payload.response.DataResponse;
import com.hcmute.codesphere_server.service.common.AuthService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("${base.url}/auth")
@RequiredArgsConstructor
public class AuthController {

    private final AuthService authService;

    @PostMapping("/register")
    public ResponseEntity<DataResponse<AuthResponse>> register(
            @Valid @RequestBody RegisterRequest request) {
        try {
            AuthResponse response = authService.register(request);
            return ResponseEntity.ok(DataResponse.success(response));
        } catch (RuntimeException e) {
            return ResponseEntity.badRequest()
                .body(DataResponse.error(e.getMessage()));
        }
    }

    @PostMapping("/login")
    public ResponseEntity<DataResponse<AuthResponse>> login(
            @Valid @RequestBody LoginRequest request) {
        try {
            AuthResponse response = authService.login(request);
            return ResponseEntity.ok(DataResponse.success(response));
        } catch (RuntimeException e) {
            return ResponseEntity.badRequest()
                .body(DataResponse.error(e.getMessage()));
        }
    }

    // Endpoint cho Google OAuth2 - Spring Security tự xử lý
    // Frontend redirect đến: /oauth2/authorization/google
    @GetMapping("/google")
    public ResponseEntity<DataResponse<String>> googleAuth() {
        return ResponseEntity.ok(DataResponse.success(
            "Redirect to: /oauth2/authorization/google"
        ));
    }
}
